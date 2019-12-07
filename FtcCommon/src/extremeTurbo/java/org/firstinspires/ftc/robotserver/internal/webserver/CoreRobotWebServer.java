package org.firstinspires.ftc.robotserver.internal.webserver;

import com.qualcomm.robotcore.util.WebHandlerManager;
import com.qualcomm.robotcore.util.WebServer;
import com.qualcomm.robotcore.wifi.NetworkType;

import org.firstinspires.ftc.robotcore.internal.webserver.RobotControllerWebInfo;
import org.firstinspires.ftc.robotcore.internal.webserver.websockets.WebSocketManager;

public class CoreRobotWebServer implements WebServer {
    public CoreRobotWebServer(NetworkType networkType) {

    }

    @Override
    public WebHandlerManager getWebHandlerManager() {
        return null;
    }

    @Override
    public boolean wasStarted() {
        return false;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public RobotControllerWebInfo getConnectionInformation() {
        return null;
    }

    @Override
    public WebSocketManager getWebSocketManager() {
        return null;
    }
}
