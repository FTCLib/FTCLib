package org.firstinspires.ftc.onbotjava.handlers;

import org.firstinspires.ftc.onbotjava.OnBotJavaProgrammingMode;
import org.firstinspires.ftc.robotcore.internal.webserver.websockets.WebSocketMessageTypeHandler;
import org.firstinspires.ftc.robotcore.internal.webserver.websockets.WebSocketNamespaceHandler;

import java.util.Map;

public class OnBotJavaWebSocketHandler extends WebSocketNamespaceHandler {
    public OnBotJavaWebSocketHandler(Map<String, WebSocketMessageTypeHandler> messageTypeHandlerMap) {
        super(OnBotJavaProgrammingMode.WS_NAMESPACE, messageTypeHandlerMap);
    }
}
