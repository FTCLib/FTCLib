package org.firstinspires.ftc.onbotjava;

import org.firstinspires.ftc.robotcore.internal.collections.SimpleGson;
import org.firstinspires.ftc.robotcore.internal.webserver.websockets.FtcWebSocket;
import org.firstinspires.ftc.robotcore.internal.webserver.websockets.FtcWebSocketMessage;
import org.firstinspires.ftc.robotcore.internal.webserver.websockets.WebSocketManager;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Provides a delegate for OnBotJava handlers to message the OnBotJava namespace
 *
 */
public class OnBotJavaBroadcastManager {
    private static String NAMESPACE = OnBotJavaProgrammingMode.WS_NAMESPACE;
    private WebSocketManager webSocketManager;
    private final ConcurrentLinkedQueue<FtcWebSocketMessage> messageQueue;


    protected OnBotJavaBroadcastManager() {
        this.messageQueue = new ConcurrentLinkedQueue<>();
    }

    /**
     * Sends an message over the OnBotJava namespace
     *
     * @param type the message type
     * @param message the message to send, the clients receiving the message will see a JSON response
     */
    public void broadcast(String type, Object message) {
        FtcWebSocketMessage msg = new FtcWebSocketMessage(NAMESPACE, type, SimpleGson.getInstance().toJson(message));
        if (webSocketManager == null) {
            messageQueue.add(msg);
        } else {
            webSocketManager.broadcastToNamespace(NAMESPACE, msg);
        }
    }

    void registerWebSocketManager(WebSocketManager webSocketManager) {
        this.webSocketManager = webSocketManager;
        while (!messageQueue.isEmpty()) {
            FtcWebSocketMessage msg = messageQueue.poll();
            webSocketManager.broadcastToNamespace(NAMESPACE, msg);
        }
    }
}
