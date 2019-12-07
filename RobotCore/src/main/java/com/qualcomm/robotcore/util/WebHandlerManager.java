package com.qualcomm.robotcore.util;

import org.firstinspires.ftc.robotcore.internal.webserver.WebHandler;
import org.firstinspires.ftc.robotcore.internal.webserver.WebObserver;

public interface WebHandlerManager {
    WebServer getWebServer();

    /**
     * Register a key, value pair. Associate a String command with a {@link WebHandler}.
     *
     * @param command String a uri that is part of an IHTTPSession
     * @param webHandler RequestHandler
     */
    void register(String command, WebHandler webHandler);

    /**
     * Returns if a web handler is already associated with a command.
     *
     * @param command String a uri that is part of an IHTTPSession
     * @return if the command is already associated
     * @see #register(String, WebHandler)
     */
    WebHandler getRegistered(String command);

    /**
     * Registers a observer as {@link WebObserver} for client requests into the robot web server.
     *
     * This allows for the detection of some client requests, but this method does not allow for
     * {@code WebObserver}s to return a server response.
     *
     * Successive calls with the same key use the last {@code WebObserver} that was used in a call.
     * Therefore, different instances of active {@code WebObserver} require different keys.
     *
     * @param key name of the {@code WebObserver}, used to update the {@code WebObserver} with that key
     * @param webObserver an instance of an {@code WebObserver} for future traffic
     */
    void registerObserver(String key, WebObserver webObserver);
}
