package org.firstinspires.ftc.onbotjava.handlers;

import org.firstinspires.ftc.robotcore.internal.webserver.websockets.WebSocketMessageTypeHandler;

public interface OnBotJavaWebSocketTypedMessageHandler extends WebSocketMessageTypeHandler {
    String type();
}
