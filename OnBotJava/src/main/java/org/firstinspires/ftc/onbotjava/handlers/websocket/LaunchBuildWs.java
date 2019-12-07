package org.firstinspires.ftc.onbotjava.handlers.websocket;

import org.firstinspires.ftc.onbotjava.OnBotJavaProgrammingMode;
import org.firstinspires.ftc.onbotjava.OnBotJavaWebInterfaceManager;
import org.firstinspires.ftc.onbotjava.handlers.OnBotJavaWebSocketTypedMessageHandler;
import org.firstinspires.ftc.robotcore.internal.webserver.websockets.FtcWebSocket;
import org.firstinspires.ftc.robotcore.internal.webserver.websockets.FtcWebSocketMessage;

public class LaunchBuildWs implements OnBotJavaWebSocketTypedMessageHandler {
    @Override
    public void handleMessage(FtcWebSocketMessage message, FtcWebSocket webSocket) {
        OnBotJavaWebInterfaceManager.instance().buildMonitor().launchBuild();
    }

    @Override
    public String type() {
        return OnBotJavaProgrammingMode.WS_BUILD_LAUNCH;
    }
}
