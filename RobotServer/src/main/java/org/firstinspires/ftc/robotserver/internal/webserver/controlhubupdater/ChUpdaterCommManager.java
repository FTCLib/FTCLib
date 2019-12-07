/*
Copyright (c) 2019 Noah Andrews

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of Noah Andrews nor the names of his contributors may be used to
endorse or promote products derived from this software without specific prior
written permission.

NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package org.firstinspires.ftc.robotserver.internal.webserver.controlhubupdater;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.util.Log;
import android.widget.Toast;

import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.ThreadPool;

import org.firstinspires.ftc.robotcore.internal.collections.SimpleGson;
import org.firstinspires.ftc.robotcore.internal.network.NetworkConnectionHandler;
import org.firstinspires.ftc.robotcore.internal.network.PeerStatusCallback;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.ui.UILocation;
import org.firstinspires.ftc.robotcore.internal.webserver.websockets.FtcWebSocket;
import org.firstinspires.ftc.robotcore.internal.webserver.websockets.FtcWebSocketMessage;
import org.firstinspires.ftc.robotcore.internal.webserver.websockets.WebSocketManager;
import org.firstinspires.ftc.robotcore.internal.webserver.websockets.WebSocketMessageTypeHandler;
import org.firstinspires.ftc.robotcore.internal.webserver.websockets.WebSocketNamespaceHandler;
import org.firstinspires.ftc.robotserver.internal.webserver.controlhubupdater.result.Result;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Handles all communication between the Control Hub Updater system application built into the
 * REV Control Hub v1.0 and the user. The CH Updater application is used to do APK installations and
 * OS updates in OTA zip format.
 *
 * Results are  displayed on a web browser via the Manage page, and on the Driver Station via toast
 * messages. Toast messages are important for if the user closes the web browser that initiated the
 * update, and for actions that were not initiated through the web browser (such as putting an APK
 * into the appropriate update folder).
 *
 * This class is fully thread-safe.
 */
public final class ChUpdaterCommManager extends WebSocketNamespaceHandler {
    //----------------------------------------------------------------------------------------------
    // Constants
    //----------------------------------------------------------------------------------------------
    public  static final String WS_NAMESPACE = "ControlHubUpdater";
    private static final String BIND_MESSAGE_TYPE = "bind";
    private static final String CONFIRM_DANGEROUS_ACTION_MESSAGE_TYPE = "confirmDangerousAction";
    private static final String DELETE_UPLOADED_FILE_MESSAGE_TYPE = "deleteUploadedFile";
    private static final String NOTIFICATION_MESSAGE_TYPE = "notification";
    private static final String TAG = "ChUpdaterCommManager";

    //----------------------------------------------------------------------------------------------
    // State Variables
    //----------------------------------------------------------------------------------------------
    private final Map<UUID, ChUpdaterResultReceiver> uuidResultReceiverMap = new ConcurrentHashMap<>(4); // Maps update transaction UUIDs to their ResultReceivers
    private final WebSocketManager webSocketManager;

    private final Queue<String> toastQueue; // Guarded by toastQueueLock
    private final Object toastQueueLock;

    //----------------------------------------------------------------------------------------------
    // Constructor
    //----------------------------------------------------------------------------------------------

    public ChUpdaterCommManager(WebSocketManager webSocketManager) {
        super(WS_NAMESPACE);
        this.webSocketManager = webSocketManager;

        synchronized (ChUpdaterBroadcastReceiver.outerClassReferenceLock) {
            ChUpdaterBroadcastReceiver.outerClassReference = this;
        }

        // Setup toast queueing
        this.toastQueue = ChUpdaterBroadcastReceiver.toastQueue;
        this.toastQueueLock = ChUpdaterBroadcastReceiver.toastQueueLock;
        QueuedToastSender queuedToastSender = new QueuedToastSender();
        boolean alreadyConnectedToPeer = NetworkConnectionHandler.getInstance().registerPeerStatusCallback(queuedToastSender);
        if (alreadyConnectedToPeer) {
            // If we're already connected to the peer, we need to send any broadcast toasts that were queued
            queuedToastSender.onPeerConnected();
        }
    }

    //----------------------------------------------------------------------------------------------
    // WebSocketNamespaceHandler overrides
    //----------------------------------------------------------------------------------------------

    @Override
    public void onSubscribe(FtcWebSocket webSocket) {
        // Send any applicable broadcasts
        synchronized (ChUpdaterBroadcastReceiver.messagesForNewWsConnectionsLock) {
            FtcWebSocketMessage message;
            Iterator<FtcWebSocketMessage> iterator = ChUpdaterBroadcastReceiver.messagesForNewWsConnections.iterator();
            while (iterator.hasNext()) {
                message = iterator.next();
                webSocket.send(message);

                // Prevent the message from being sent outside of whatever window it's supposed to be sent in
                if (ChUpdaterBroadcastReceiver.withinReconnectionAllowancePeriod) {
                    ChUpdaterBroadcastReceiver.seenMessageSet.add(message);
                } else {
                    iterator.remove();
                }
            }
        }
    }

    @Override
    protected void registerMessageTypeHandlers(Map<String, WebSocketMessageTypeHandler> messageTypeHandlerMap) {
        messageTypeHandlerMap.put(BIND_MESSAGE_TYPE, new BindMessageHandler());
        messageTypeHandlerMap.put(CONFIRM_DANGEROUS_ACTION_MESSAGE_TYPE, new ConfirmDangerousActionMessageHandler());
        messageTypeHandlerMap.put(DELETE_UPLOADED_FILE_MESSAGE_TYPE, new DeleteUploadedFileMessageHandler());
    }


    //----------------------------------------------------------------------------------------------
    // Public methods
    //----------------------------------------------------------------------------------------------

    /**
     * Tells the CH Updater to initiate an update using a given OTA zip or APK file
     *
     * @return a UUID that identifies this particular update transaction
     */
    public UUID startUpdate(UpdateType updateType, File file) {
        ChUpdaterResultReceiver receiver = new ChUpdaterResultReceiver(file, new Handler(Looper.getMainLooper()));
        UUID uuid = UUID.randomUUID();
        uuidResultReceiverMap.put(uuid, receiver);

        String action = null;

        switch (updateType) {
            case APP:
                action = UpdaterConstants.ACTION_UPDATE_FTC_APP;
                break;
            case OTA:
                action = UpdaterConstants.ACTION_APPLY_OTA_UPDATE;
                break;
        }

        Intent intent = new Intent();
        ComponentName updateService = new ComponentName(UpdaterConstants.CONTROL_HUB_UPDATER_PACKAGE, UpdaterConstants.CONTROL_HUB_UPDATE_SERVICE);

        intent.setComponent(updateService);
        intent.setAction(action);
        intent.putExtra(UpdaterConstants.EXTRA_UPDATE_FILE_PATH, file.getAbsolutePath());
        intent.putExtra(UpdaterConstants.EXTRA_RESULT_RECEIVER, AppUtil.wrapResultReceiverForIpc(receiver));
        receiver.setSentIntent(intent);
        AppUtil.getDefContext().startService(intent);
        return uuid;
    }

    //----------------------------------------------------------------------------------------------
    // Static utility methods
    //----------------------------------------------------------------------------------------------

    /**
     * Convert a CH Updater Result into a WebSocket message ready to be sent to a browser
     */
    private static FtcWebSocketMessage createWsMessageFromResult(Result result, boolean isBroadcast) {
        String message = result.getMessage();
        String detailMessage = result.getDetailMessage();
        Result.DetailMessageType detailMessageType = result.getDetailMessageType();
        Result.PresentationType presentationType = result.getPresentationType();

        if (detailMessageType == Result.DetailMessageType.DISPLAYED && detailMessage != null) {
            message = (message + "\n\n" + detailMessage);
        }

        // Broadcast notifications should not be treated as prompts (we don't know what the command we sent was, so we can't resend it)
        if (isBroadcast && presentationType == Result.PresentationType.PROMPT) {
            presentationType = Result.PresentationType.ERROR;
        }

        NotificationPayload payload = new NotificationPayload(message, presentationType);
        return new FtcWebSocketMessage(WS_NAMESPACE, NOTIFICATION_MESSAGE_TYPE, payload.toJson());
    }

    private static void logResult(Result result) {
        String message = result.getMessage();

        int logPriority = result.getPresentationType() == Result.PresentationType.ERROR ? Log.ERROR : Log.VERBOSE;

        RobotLog.internalLog(logPriority, TAG, result.getCause(), String.format("%s result received: %s", result.getPresentationType().name(), message));

        if (result.getDetailMessage() != null) {
            RobotLog.internalLog(logPriority, TAG, null, "detail message: " + result.getDetailMessage());
        }
    }

    /**
     * We only want to show final successes and errors as toasts
     */
    private static boolean isResultEligibleForToast(Result result) {
        return (result.getPresentationType() == Result.PresentationType.SUCCESS) || (result.getPresentationType() == Result.PresentationType.ERROR);
    }

    /**
     * @return true if toast was shown to user, false otherwise
     */
    private static boolean sendResultAsToastIfPossible(Result result) {
        if (isResultEligibleForToast(result) && NetworkConnectionHandler.getInstance().isPeerConnected()) {
            AppUtil.getInstance().showToast(UILocation.BOTH, result.getMessage(), Toast.LENGTH_LONG);
            return true;
        }
        return false;
    }

    //----------------------------------------------------------------------------------------------
    // Static nested classes
    //----------------------------------------------------------------------------------------------

    /**
     * BroadcastReceiver (registered in manifest) that receives broadcast results from the CH Updater.
     *
     * Broadcast results usually indicate the end of a transaction. They should be sent via toast
     * messages, and should be sent to all browsers connected at the time of the event. If no
     * browsers were connected at the time of the event, then it should be shown to the next single
     * browser to connect.
     *
     * Browsers are given {@link ChUpdaterBroadcastReceiver#RECONNECTION_ALLOWANCE_SECONDS} to
     * reconnect after the app or device is restarted due to an update. Broadcasts received during
     * this window will be sent to all browsers that connect during this window.
     */
    public static class ChUpdaterBroadcastReceiver extends BroadcastReceiver {
        private static final int RECONNECTION_ALLOWANCE_SECONDS = 10;

        private static ChUpdaterCommManager outerClassReference; // Guarded by outerClassReferenceLock
        private static final Object outerClassReferenceLock = new Object();

        private static final List<FtcWebSocketMessage> messagesForNewWsConnections = new ArrayList<>(); // Guarded by messagesForNewWsConnectionsLock
        private static Set<FtcWebSocketMessage> seenMessageSet = new HashSet<>(); // Guarded by messagesForNewWsConnectionsLock
        private static boolean withinReconnectionAllowancePeriod = true; // Guarded by messagesForNewWsConnectionsLock
        private static final Object messagesForNewWsConnectionsLock = new Object();

        private static final Queue<String> toastQueue = new LinkedBlockingQueue<>(); // Guarded by toastQueueLock
        private static final Object toastQueueLock = new Object();

        static {
            ThreadPool.getDefaultScheduler().schedule(new AllowancePeriodExpiredRunnable(), RECONNECTION_ALLOWANCE_SECONDS, TimeUnit.SECONDS);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Result result = Result.fromBundle(intent.getBundleExtra(UpdaterConstants.RESULT_BROADCAST_BUNDLE_EXTRA));
            logResult(result);

            boolean eligibleForToast = isResultEligibleForToast(result);
            boolean sentToast = sendResultAsToastIfPossible(result);

            if (eligibleForToast && !sentToast) {
                synchronized (toastQueueLock) {
                    toastQueue.add(result.getMessage());
                }
            }

            FtcWebSocketMessage wsMessage = createWsMessageFromResult(result, true);
            int webSocketsThatReceivedMessage = 0;

            synchronized (outerClassReferenceLock) {
                if (outerClassReference != null) {
                    webSocketsThatReceivedMessage = outerClassReference.webSocketManager.broadcastToNamespace(WS_NAMESPACE, wsMessage);
                }
            }

            synchronized (messagesForNewWsConnectionsLock) {
                if (webSocketsThatReceivedMessage == 0 || withinReconnectionAllowancePeriod) {
                    messagesForNewWsConnections.add(wsMessage);
                    if (webSocketsThatReceivedMessage > 0) {
                        seenMessageSet.add(wsMessage);
                    }
                }
            }
        }

        private static class AllowancePeriodExpiredRunnable implements Runnable {
            @Override public void run() {
                synchronized (messagesForNewWsConnectionsLock) {
                    withinReconnectionAllowancePeriod = false;
                    for (FtcWebSocketMessage message : seenMessageSet) {
                        messagesForNewWsConnections.remove(message);
                    }
                    seenMessageSet = null;
                }
            }
        }
    }

    /**
     * Payload for notifications sent via WebSocket
     */
    private static class NotificationPayload {
        final String message;
        final Result.PresentationType presentationType;

        NotificationPayload(String message, Result.PresentationType presentationType) {
            this.message = message;
            this.presentationType = presentationType;
        }

        String toJson() {
            return SimpleGson.getInstance().toJson(this);
        }
    }

    public enum UpdateType { OTA, APP }

    //----------------------------------------------------------------------------------------------
    // Inner classes
    //----------------------------------------------------------------------------------------------

    /**
     * A connected browser sends this message with the UUID of an update transaction that it just
     * initiated.
     */
    private class BindMessageHandler extends UpdaterMessageTypeHandler {
        @Override
        void handleUpdaterMessage(FtcWebSocket webSocket, UUID receivedUuid, ChUpdaterResultReceiver associatedResultReceiver) {
            RobotLog.dd(TAG, "Binding WebSocket " + webSocket + " to UUID " + receivedUuid);
            associatedResultReceiver.setWebSocket(webSocket);
        }
    }

    /**
     * A connected browser sends this message after an update transaction sends back a result with
     * a PresentationType of PROMPT, and the user has accepted the prompt.
     */
    private class ConfirmDangerousActionMessageHandler extends UpdaterMessageTypeHandler {
        @Override
        void handleUpdaterMessage(FtcWebSocket webSocket, UUID receivedUuid, ChUpdaterResultReceiver associatedResultReceiver) {
            RobotLog.ww(TAG, "Confirming dangerous action for UUID " + receivedUuid);
            associatedResultReceiver.confirmDangerousAction();
        }
    }

    /**
     * A connected browser sends this message after an update transaction sends back a result with
     * a PresentationType of PROMPT, and the user has denied the prompt.
     */
    private class DeleteUploadedFileMessageHandler extends UpdaterMessageTypeHandler {
        @Override
        void handleUpdaterMessage(FtcWebSocket webSocket, UUID receivedUuid, ChUpdaterResultReceiver associatedResultReceiver) {
            RobotLog.ww(TAG, "Deleting uploaded file associated with UUID " + receivedUuid);
            associatedResultReceiver.deleteAssociatedUpload();
        }
    }

    /**
     * CH Updater WebSocket messages sent by the client use a common format. This abstract handler
     * parses it.
     */
    private abstract class UpdaterMessageTypeHandler implements WebSocketMessageTypeHandler {
        abstract void handleUpdaterMessage(FtcWebSocket webSocket, UUID receivedUuid, ChUpdaterResultReceiver associatedResultReceiver);

        @Override public final void handleMessage(FtcWebSocketMessage message, FtcWebSocket webSocket) {
            UUID receivedUuid = UUID.fromString(message.getPayload());
            ChUpdaterResultReceiver resultReceiver = uuidResultReceiverMap.get(receivedUuid);
            if (resultReceiver != null) {
                handleUpdaterMessage(webSocket, receivedUuid, resultReceiver);
            } else {
                RobotLog.ww(TAG, "Received message with unknown UUID " + receivedUuid);
            }
        }
    }

    private class QueuedToastSender implements PeerStatusCallback {
        @Override public void onPeerConnected() {
            synchronized (toastQueueLock) {
                String toastMessage = toastQueue.poll();
                while (toastMessage != null) {
                    AppUtil.getInstance().showToast(UILocation.BOTH, toastMessage, Toast.LENGTH_LONG);
                    toastMessage = toastQueue.poll();
                }
            }
        }
        @Override public void onPeerDisconnected() { }
    }

    /**
     * Receives results from the CH Updater that only need to be seen by the initiating webpage.
     * This includes interim status updates and most errors (errors that happen after the app is
     * killed during the update process are broadcast, by necessity).
     *
     * Toast messages should not be displayed/queued from here, and only the most recent result
     * needs to be shown.
     */
    private class ChUpdaterResultReceiver extends ResultReceiver {
        private FtcWebSocket webSocket;
        private Result lastResult;
        private Intent sentIntent;
        private final File uploadedFile;

        ChUpdaterResultReceiver(File uploadedFile, Handler handler) {
            super(handler);
            this.uploadedFile = uploadedFile;
        }

        /**
         * When we receive a Result from the CH Updater for our update transaction, we send it to our
         * associated WebSocket, if we have one. Otherwise, we save it, in case a WebSocket binds
         * itself to us later.
         */
        @Override protected synchronized void onReceiveResult(int resultCode, Bundle resultData) {
            Result result = Result.fromBundle(resultData);
            logResult(result);

            // Only errors and successes can ever be sent as toast messages.
            boolean eligibleForToast = isResultEligibleForToast(result);
            boolean sentToast = sendResultAsToastIfPossible(result);

            synchronized (toastQueueLock) {
                if (webSocket != null && webSocket.isOpen()) { // It's important to synchronize this null check too (or at least it was at one point)
                    webSocket.send(createWsMessageFromResult(result, false));
                } else { // The WebSocket is disconnected
                    lastResult = result;

                    // Queue toast if eligible and it wasn't already sent
                    if (!sentToast && eligibleForToast) {
                        toastQueue.add(result.getMessage());
                    }
                }
            }
        }

        /**
         * Associate a WebSocket with this ChUpdaterResultReceiver, such that it will receive ALL
         * updates for this receiver's update transaction, while other connected browsers will only
         * receive broadcast updates.
         */
        private synchronized void setWebSocket(FtcWebSocket webSocket) {
            this.webSocket = webSocket;
            if (lastResult != null) {
                webSocket.send(createWsMessageFromResult(lastResult, false));
            }
        }

        /**
         * Store the original Intent that initiated the update.
         */
        private void setSentIntent(Intent sentIntent) {
            this.sentIntent = sentIntent;
        }

        /**
         * Tells the CH Updater to go ahead with the update, doing whatever was specified by the
         * prompt that was sent previously (e.g. uninstall the previously installed app).
         */
        private void confirmDangerousAction() {
            sentIntent.putExtra(UpdaterConstants.EXTRA_DANGEROUS_ACTION_CONFIRMED, true);
            AppUtil.getDefContext().startService(sentIntent);
        }

        /**
         * Deletes the uploaded file to save space.
         */
        @SuppressWarnings("ResultOfMethodCallIgnored")
        private void deleteAssociatedUpload() {
            uploadedFile.delete();
        }
    }
}
