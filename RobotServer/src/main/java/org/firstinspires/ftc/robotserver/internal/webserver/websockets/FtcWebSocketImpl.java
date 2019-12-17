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

import com.google.gson.JsonSyntaxException;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.internal.webserver.websockets.CloseCode;
import org.firstinspires.ftc.robotcore.internal.webserver.websockets.FtcWebSocket;
import org.firstinspires.ftc.robotcore.internal.webserver.websockets.FtcWebSocketMessage;
import org.firstinspires.ftc.robotcore.internal.webserver.websockets.WebSocketManager;

import java.net.InetAddress;
import java.util.Locale;

public final class FtcWebSocketImpl implements FtcWebSocket {
    private static final String TAG = "FtcWebSocket";
    private static final boolean DEBUG = false;

    private final InetAddress remoteIpAddress;
    private final String remoteHostname;
    private final int port;
    private final RawWebSocket rawWebSocket;
    private final WebSocketManager manager;


    private FtcWebSocketImpl(int port, InetAddress remoteIpAddress, String remoteHostname, WebSocketManager webSocketManager, RawWebSocket rawWebSocket) {
        this.remoteIpAddress = remoteIpAddress;
        this.remoteHostname = remoteHostname;
        this.port = port;
        this.rawWebSocket = rawWebSocket;
        this.manager = webSocketManager;
    }


    //----------------------------------------------------------------------------------------------
    // Interface Method Overrides
    //----------------------------------------------------------------------------------------------

    @Override public void send(@NonNull FtcWebSocketMessage message) {
        String json = message.toJson();

        if (DEBUG) {
            RobotLog.vv(TAG, "sending message to %s: %s", this, json);
        }

        if (message.getNamespace().equals(WebSocketManager.SYSTEM_NAMESPACE)) {
            throw new IllegalArgumentException("System namespace messages should ONLY go from the client to the server.");
        }

        rawWebSocket.send(json);
    }

    @Override public InetAddress getRemoteIpAddress() {
        return remoteIpAddress;
    }

    @Override public String getRemoteHostname() {
        return remoteHostname;
    }

    @Override public int getPort() {
        return port;
    }

    @Override public boolean isOpen() {
        return rawWebSocket.isOpen();
    }

    @Override public void close(CloseCode closeCode, String reason) {
        rawWebSocket.close(closeCode.getValue(), reason);
    }

    @Override public String toString() {
        return String.format(Locale.ROOT, "websocket (ip=%s port=%d)", getRemoteIpAddress(), getPort());
    }

    //----------------------------------------------------------------------------------------------
    // Private methods to be called by RawWebSocket
    //----------------------------------------------------------------------------------------------
    private void onOpen() {
        RobotLog.vv(TAG, "Opening %s", this);
    }

    private void onClose(CloseCode closeCode, String reason, boolean initiatedByRemote) {
        RobotLog.vv(TAG, "%s has closed. closeCode:%s initiatedByRemote:%b Reason: %s", this, closeCode.toString(), initiatedByRemote, reason);
        ((WebSocketManagerImpl)manager).onWebSocketClose(this);
    }

    private void onMessage(String message) {
        if (DEBUG) {
            RobotLog.vv(TAG, "Message received from %s: %s", this, message);
        }

        try {
            FtcWebSocketMessage ftcMessage = FtcWebSocketMessage.fromJson(message);
            ((WebSocketManagerImpl)manager).onWebSocketMessage(ftcMessage, this);
        } catch (JsonSyntaxException e) {
            RobotLog.logExceptionHeader(TAG, e, "Malformed json received from %s", this);
        }
    }

    private void onException(Throwable exception) {
        RobotLog.ee(TAG, "%s experienced an exception:", this);
        RobotLog.logStackTrace(TAG, exception);
    }

    public static abstract class RawWebSocket {
        private FtcWebSocketImpl ftcWebSocket;

        public RawWebSocket(int port, InetAddress remoteIpAddress, String remoteHostname, WebSocketManager webSocketManager) {
            ftcWebSocket = new FtcWebSocketImpl(port, remoteIpAddress, remoteHostname, webSocketManager, this);
        }

        /**
         * Implementations of this method must be thread-safe
         */
        protected abstract boolean isOpen();
        protected abstract void send(String payload);

        /**
         * Implementations of this method must be thread-safe
         */
        protected abstract void close(int closeCode, String reason);

        public void onOpen() {
            ftcWebSocket.onOpen();
        }

        public void onMessage(String message) {
            ftcWebSocket.onMessage(message);
        }

        public void onClose(CloseCode closeCode, String reason, boolean initiatedByRemote) {
            ftcWebSocket.onClose(closeCode, reason, initiatedByRemote);
        }

        public void onException(Throwable exception) {
            ftcWebSocket.onException(exception);
        }
    }
}
