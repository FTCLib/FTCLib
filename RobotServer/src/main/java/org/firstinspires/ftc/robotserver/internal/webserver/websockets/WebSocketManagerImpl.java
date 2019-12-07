/*
Copyright (c) 2018 Noah Andrews

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
package org.firstinspires.ftc.robotserver.internal.webserver.websockets;

import android.support.annotation.NonNull;

import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.Util;

import org.firstinspires.ftc.robotcore.internal.webserver.websockets.FtcWebSocket;
import org.firstinspires.ftc.robotcore.internal.webserver.websockets.FtcWebSocketMessage;
import org.firstinspires.ftc.robotcore.internal.webserver.websockets.WebSocketManager;
import org.firstinspires.ftc.robotcore.internal.webserver.websockets.WebSocketNamespaceHandler;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Class that manages namespace handlers and namespace subscribers associated with a particular
 * {@link com.qualcomm.robotcore.util.WebServer} instance
 */
public final class WebSocketManagerImpl implements WebSocketManager {
    // Functions that change this object's state are synchronized.

    private static final String SUBSCRIBE_TO_NAMESPACE_MESSAGE_TYPE = "subscribeToNamespace";
    private static final String UNSUBSCRIBE_FROM_NAMESPACE_MESSAGE_TYPE = "unsubscribeFromNamespace";

    private static final String TAG = "WebSocketManager";

    //----------------------------------------------------------------------------------------------
    // Fields
    //----------------------------------------------------------------------------------------------
    private final ConcurrentMap<String, WebSocketNamespaceHandler> namespaceHandlerMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Set<FtcWebSocket>> namespaceSubscribersMap = new ConcurrentHashMap<>();

    //----------------------------------------------------------------------------------------------
    // WebSocketManager Method Overrides
    //----------------------------------------------------------------------------------------------

    @Override public void registerNamespaceHandler(@NonNull WebSocketNamespaceHandler handler) {
        internalRegisterNamespaceHandler(handler);
        RobotLog.vv(TAG, "Registered handler for namespace %s", handler.getNamespace());
    }

    @Override public void registerNamespaceAsBroadcastOnly(@NonNull String namespace) {
        internalRegisterNamespaceHandler(new BroadcastOnlyNamespaceHandler(namespace));
        RobotLog.vv(TAG, "Registered broadcast-only namespace %s", namespace);
    }

    @Override public int broadcastToNamespace(@NonNull String namespace, @NonNull FtcWebSocketMessage message) {
        if (!namespace.equals(message.getNamespace())) {
            throw new IllegalArgumentException("Cannot broadcast to a different namespace than is listed in the message");
        }
        // This check can be done non-atomically because we never remove anything from the namespace maps
        if (!namespaceSubscribersMap.containsKey(namespace)) {
            throw new IllegalStateException("You must register a namespace before broadcasting to it.");
        }
        int numberOfConnections = 0;
        //noinspection ConstantConditions
        for (FtcWebSocket webSocket: namespaceSubscribersMap.get(namespace)) {
            webSocket.send(message);
            ++numberOfConnections;
        }
        return numberOfConnections;
    }

    //----------------------------------------------------------------------------------------------
    // FtcWebSocket Interfacing Methods
    //----------------------------------------------------------------------------------------------

    /**
     * This method is idempotent
     */
    synchronized void onWebSocketClose(FtcWebSocket webSocket) {
        for (String namespace : namespaceSubscribersMap.keySet()) {
            unsubscribeWebSocketFromNamespace(webSocket, namespace);
        }
    }

    void onWebSocketMessage(FtcWebSocketMessage message, FtcWebSocketImpl webSocket) {
        if (message.getNamespace().equals(SYSTEM_NAMESPACE)) {
            handleSystemNamespace(message, webSocket);
            return;
        }
        // We can do this check non-atomically because we never remove items from namespaceHandlerMap
        if (!namespaceHandlerMap.containsKey(message.getNamespace())) {
            RobotLog.ww(TAG, "Received message to unregistered namespace %s", message.getNamespace());
            return;
        }
        //noinspection ConstantConditions
        namespaceHandlerMap.get(message.getNamespace()).onMessage(message, webSocket);
    }

    //----------------------------------------------------------------------------------------------
    // Private Methods
    //----------------------------------------------------------------------------------------------

    private synchronized void internalRegisterNamespaceHandler(@NonNull WebSocketNamespaceHandler handler) {
        String namespace = handler.getNamespace();
        if (!Util.isGoodString(namespace)) {
            throw new IllegalArgumentException("namespace must not be null, empty, or in need of trimming");
        }
        if (namespace.equals(SYSTEM_NAMESPACE)) {
            throw new IllegalArgumentException("namespace system is reserved.");
        }
        if (namespaceHandlerMap.containsKey(namespace) && !(namespaceHandlerMap.get(namespace) instanceof BroadcastOnlyNamespaceHandler)) {
            throw new IllegalArgumentException("namespace " + namespace + " is already registered with a handler");
        }

        namespaceHandlerMap.put(namespace, handler);
        namespaceSubscribersMap.putIfAbsent(namespace, new CopyOnWriteArraySet<FtcWebSocket>());
    }

    /**
     * This method is idempotent
     */
    private synchronized void unsubscribeWebSocketFromNamespace(FtcWebSocket webSocket, String namespace) {
        if (!namespaceHandlerMap.containsKey(namespace)) {
            RobotLog.ee(TAG, "Cannot unsubscribe %s from namespace (%s) because there is no corresponding namespace handler registered", webSocket, namespace);
            return;
        }
        //noinspection ConstantConditions
        if (namespaceSubscribersMap.get(namespace).remove(webSocket)) { // if the WebSocket was just removed from the set
            //noinspection ConstantConditions
            namespaceHandlerMap.get(namespace).onUnsubscribe(webSocket);
            RobotLog.vv(TAG, "Unsubscribed %s from namespace (%s)", webSocket, namespace);
        }
    }

    /**
     * This method is idempotent
     */
    private synchronized void subscribeWebSocketToNamespace(FtcWebSocketImpl webSocket, String namespace) {
        if (!namespaceHandlerMap.containsKey(namespace)) {
            RobotLog.ee(TAG, "Cannot subscribe %s to namespace (%s) because there is no corresponding namespace handler registered", webSocket, namespace);
            return;
        }
        //noinspection ConstantConditions
        if (namespaceSubscribersMap.get(namespace).add(webSocket)) { // if the WebSocket was just added to the set
            //noinspection ConstantConditions
            namespaceHandlerMap.get(namespace).onSubscribe(webSocket);
            RobotLog.vv(TAG, "Subscribed %s to namespace (%s)", webSocket, namespace);
        }
    }

    private void handleSystemNamespace(FtcWebSocketMessage message, FtcWebSocketImpl webSocket) {
        if (message.getType().equals(SUBSCRIBE_TO_NAMESPACE_MESSAGE_TYPE)) {
            // Idempotent, safe to repeat
            // The payload for this message type is a String containing the namespace to subscribe to
            String namespace = message.getPayload();
            subscribeWebSocketToNamespace(webSocket, namespace);
        } else if (message.getType().equals(UNSUBSCRIBE_FROM_NAMESPACE_MESSAGE_TYPE)) {
            // The payload for this message type is a String containing the namespace to unsubscribe from
            String namespace = message.getPayload();
            unsubscribeWebSocketFromNamespace(webSocket, namespace);
        }
    }

    private static final class BroadcastOnlyNamespaceHandler extends WebSocketNamespaceHandler {
        private BroadcastOnlyNamespaceHandler(String namespace) {
            super(namespace);
        }

        @Override public void onMessage(FtcWebSocketMessage message, FtcWebSocket webSocket) {
            super.onMessage(message, webSocket);
            RobotLog.ww(getTag(), "Message received on broadcast-only namespace " + getNamespace() + ":");
            RobotLog.ww(getTag(), message.toString());
        }

        private String getTag() {
            return "BroadcastOnlyNamespace-" + getNamespace();
        }
    }
}
