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
package org.firstinspires.ftc.robotcore.internal.webserver.websockets;

import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class WebSocketNamespaceHandler {
    private final Map<String, WebSocketMessageTypeHandler> messageTypeHandlerMap;
    private final String namespace;

    /**
     * Simple constructor
     *
     * @param namespace The namespace that this handler will handle the messages of
     */
    public WebSocketNamespaceHandler(String namespace) {
        this(namespace, null);
    }

    /**
     * Constructor with parameter for a map of message type handlers. Can be used as
     * an alternative to overriding {@link #registerMessageTypeHandlers(Map)}
     *
     * @param namespace The namespace that this handler will handle the messages of
     * @param prepopulatedMessageTypeHandlerMap A map of message type Strings to WebSocketMessageTypeHandler implementations
     */
    public WebSocketNamespaceHandler(String namespace, @Nullable Map<String, WebSocketMessageTypeHandler> prepopulatedMessageTypeHandlerMap) {
        this.namespace = namespace;
        this.messageTypeHandlerMap = new ConcurrentHashMap<>();
        if (prepopulatedMessageTypeHandlerMap != null) {
            this.messageTypeHandlerMap.putAll(prepopulatedMessageTypeHandlerMap);
        }
        registerMessageTypeHandlers(this.messageTypeHandlerMap);
    }

    /**
     * Register handlers for particular method types by overriding this method and adding to messageTypeHandlerMap.
     * Use the message type as the key, and the handler as the value.
     *
     * This should be called ONLY from the WebSocketNamespaceHandler constructor.
     */
    protected void registerMessageTypeHandlers(Map<String, WebSocketMessageTypeHandler> messageTypeHandlerMap) {

    }

    /**
     * This will be called when a client sends a message with the registered namespace.
     * It may be called from any thread.
     *
     * @param message The received message
     * @param webSocket The WebSocket connected to the client that sent the message
     */
    @CallSuper
    public void onMessage(FtcWebSocketMessage message, FtcWebSocket webSocket) {
       WebSocketMessageTypeHandler handler = messageTypeHandlerMap.get(message.getType());
       if (handler != null) {
           handler.handleMessage(message, webSocket);
       }
    }

    /**
     * This will be called when a client subscribes to the registered namespace.
     * It may be called from any thread.
     *
     * @param webSocket The WebSocket connected to the client that subscribed
     */
    public void onSubscribe(FtcWebSocket webSocket) {

    }

    /**
     * This will be called when a client unsubscribes from the registered namespace.
     * This includes when the WebSocket connected to a subscribed client closes.
     *
     * This may be called from any thread.
     *
     * @param webSocket The WebSocket connected to the client that unsubscribed
     */
    public void onUnsubscribe(FtcWebSocket webSocket) {

    }

    /**
     * Specifies the namespace that this handler will handle the messages of
     */
    public final String getNamespace() {
        return namespace;
    }
}
