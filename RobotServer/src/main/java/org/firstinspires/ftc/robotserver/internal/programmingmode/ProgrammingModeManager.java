/*
 * Copyright (c) 2018 David Sargent
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted (subject to the limitations in the disclaimer below) provided that
 * the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * Neither the name of David Sargent nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior
 * written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
 * LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.firstinspires.ftc.robotserver.internal.programmingmode;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.qualcomm.robotcore.eventloop.opmode.FtcRobotControllerServiceState;
import com.qualcomm.robotcore.util.WebHandlerManager;
import com.qualcomm.robotcore.util.WebServer;
import org.firstinspires.ftc.robotcore.internal.webserver.WebHandler;
import org.firstinspires.ftc.robotcore.internal.webserver.websockets.WebSocketManager;
import org.firstinspires.ftc.robotcore.internal.webserver.websockets.WebSocketNamespaceHandler;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Controls and registers the various programming mode implementations
 *
 */
public class ProgrammingModeManager {
    public static final String TAG = ProgrammingModeManager.class.getSimpleName();
    private volatile WebServer webServer;
    private volatile WebSocketManager webSocketManager;
    private volatile WebHandlerManager webHandlerManager;
    private final List<ProgrammingMode> registeredProgrammingModes;
    private final ProgrammingModeWebHandlerDecorator webHandlerDecorator;

    public ProgrammingModeManager() {
        webHandlerDecorator = new ProgrammingModeWebHandlerDecorator(this);
        registeredProgrammingModes = Collections.synchronizedList(new LinkedList<ProgrammingMode>());
    }

    /**
     * Returns the web server instance, if any, associated with the available programming modes
     *
     * @return the currently running {@link WebServer}, or {@code null} if there is none
     */
    @Nullable
    public WebServer getWebServer() {
        return webServer;
    }

    /**
     * Registers a {@link WebHandler} to be associated with the given URI
     *
     * This overwrites any previously associated handler for the URI
     *
     * @param uri the URI that handler should be accessible from
     * @param webHandler
     */
    public void register(String uri, WebHandler webHandler) {
        webHandlerManager.register(uri, webHandler);
    }

    /**
     * Registers a {@link WebSocketNamespaceHandler} to be associated with a given namespace
     *
     * This overwrites any previously associated handler for the given namespace
     *
     * @param webHandler a websocket namespace handler for a given namespace
     */
    public void register(@NonNull WebSocketNamespaceHandler webHandler) {
        webSocketManager.registerNamespaceHandler(webHandler);
    }

    /**
     * Decorates the given web handler with additional operations
     * related to programming mode
     *
     * @param decorateWithParams should the WebHandler have the parameters pre-verified
     * @param handler the handler to decorate
     * @return the decorated handler
     */
    public WebHandler decorate(boolean decorateWithParams, WebHandler handler) {
        return webHandlerDecorator.decorate(handler, decorateWithParams);
    }

    /**
     * Returns the handler associated with URI
     *
     * @param uri the URI associated with a handler
     * @return the corresponding {@code WebHandler}, if any
     */
    public WebHandler getRegisteredHandler(String uri) {
        return webHandlerManager.getRegistered(uri);
    }

    /**
     * Updates the manager with the new {@link WebServer} and formally
     * registers {@link ProgrammingMode}s with the current {@code WebServer}
     *
     * @param rcServiceState the current robot controller service state
     */
    public void setState(FtcRobotControllerServiceState rcServiceState) {
        webServer = rcServiceState.getWebServer();
        webHandlerManager = webServer.getWebHandlerManager();
        webSocketManager = webServer.getWebSocketManager();
        for (ProgrammingMode programmingMode : registeredProgrammingModes) {
            programmingMode.register(this);
        }
    }

    /**
     * Registers an {@link ProgrammingMode} with the manager
     *
     * The method {@link #setState(FtcRobotControllerServiceState)} must be called
     * in order for any registered {@code ProgrammingModes} to be available
     *
     * @param programmingModeHandler the programming mode to register
     */
    public void register(ProgrammingMode programmingModeHandler) {
        registeredProgrammingModes.add(programmingModeHandler);

        if (webHandlerManager != null) {
            programmingModeHandler.register(this);
        }
    }
}
