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

package org.firstinspires.ftc.robotserver.internal.webserver.websockets.tootallnate;

import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.internal.webserver.websockets.CloseCode;
import org.firstinspires.ftc.robotcore.internal.webserver.websockets.WebSocketManager;
import org.firstinspires.ftc.robotserver.internal.webserver.websockets.WebSocketManagerImpl;
import org.firstinspires.ftc.robotserver.internal.webserver.websockets.FtcWebSocketImpl.RawWebSocket;
import org.firstinspires.ftc.robotserver.internal.webserver.websockets.FtcWebSocketServer;
import org.firstinspires.ftc.robotserver.internal.webserver.websockets.WebSocketNamespaceHandlerRegistry;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TooTallWebSocketServer extends WebSocketServer implements FtcWebSocketServer {
    private static final String TAG = "TooTallWebSocketServer";
    private static final int DECODER_THREAD_COUNT = 1;

    private final WebSocketManagerImpl manager = new WebSocketManagerImpl();
    private final Map<WebSocket, RawWebSocket> wsMap = new ConcurrentHashMap<>();

    public TooTallWebSocketServer(InetSocketAddress address) {
        super(address, DECODER_THREAD_COUNT);
        setReuseAddr(true);
        setConnectionLostTimeout(5);
        WebSocketNamespaceHandlerRegistry.onWebSocketServerCreation(manager);
    }

    @Override public void onOpen(WebSocket conn, ClientHandshake handshake) {
        TooTallWebSocket webSocket = new TooTallWebSocket(conn, getPort(), conn.getRemoteSocketAddress().getAddress(), conn.getRemoteSocketAddress().getHostName(), manager);
        wsMap.put(conn, webSocket);
        webSocket.onOpen();
    }

    @Override public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        wsMap.get(conn).onClose(CloseCode.find(code), reason, remote);
    }

    @Override public void onMessage(WebSocket conn, String message) {
        wsMap.get(conn).onMessage(message);
    }

    @Override public void onError(WebSocket conn, Exception ex) {
        if (conn != null) {
            wsMap.get(conn).onException(ex);
        } else {
            RobotLog.ee(TAG, ex, "WebSocket server error");
        }
    }

    @Override public void onStart() {
        RobotLog.vv(TAG, "Started WebSocket server on port %d", getPort());
    }

    @Override
    public WebSocketManager getWebSocketManager() {
        return manager;
    }
}
