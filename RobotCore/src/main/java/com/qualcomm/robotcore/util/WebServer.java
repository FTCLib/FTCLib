package com.qualcomm.robotcore.util;

import org.firstinspires.ftc.robotcore.internal.webserver.RobotControllerWebInfo;
import org.firstinspires.ftc.robotcore.internal.webserver.websockets.WebSocketManager;

public interface WebServer {
    WebHandlerManager getWebHandlerManager();

    /**
     * Check if the WebServer has been started.
     *
     * @return true if the server was started and false otherwise.
     */
    boolean wasStarted();

    /**
     * start the WebServer.
     */
    void start();

    /**
     * stop the WebServer.
     */
    void stop();

    /**
     * Get {@link RobotControllerWebInfo}
     *
     * @return an instance of {@link RobotControllerWebInfo}
     */
    RobotControllerWebInfo getConnectionInformation();

    /**
     * Get the manager for the WebSockets associated with this WebServer
     */
    WebSocketManager getWebSocketManager();
}
