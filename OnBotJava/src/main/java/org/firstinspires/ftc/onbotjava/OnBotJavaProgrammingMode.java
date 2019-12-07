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

package org.firstinspires.ftc.onbotjava;

import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.onbotjava.handlers.OnBotJavaWebSocketHandler;
import org.firstinspires.ftc.onbotjava.handlers.OnBotJavaWebSocketTypedMessageHandler;
import org.firstinspires.ftc.onbotjava.handlers.admin.Clean;
import org.firstinspires.ftc.onbotjava.handlers.admin.Rearm;
import org.firstinspires.ftc.onbotjava.handlers.admin.ResetOnBotJava;
import org.firstinspires.ftc.onbotjava.handlers.admin.Settings;
import org.firstinspires.ftc.onbotjava.handlers.admin.SettingsReset;
import org.firstinspires.ftc.onbotjava.handlers.file.CopyFile;
import org.firstinspires.ftc.onbotjava.handlers.file.DeleteFile;
import org.firstinspires.ftc.onbotjava.handlers.file.DownloadFile;
import org.firstinspires.ftc.onbotjava.handlers.file.FetchFileContents;
import org.firstinspires.ftc.onbotjava.handlers.file.FetchFileTemplates;
import org.firstinspires.ftc.onbotjava.handlers.file.FetchFileTree;
import org.firstinspires.ftc.onbotjava.handlers.file.NewFile;
import org.firstinspires.ftc.onbotjava.handlers.file.SaveFile;
import org.firstinspires.ftc.onbotjava.handlers.file.UploadFiles;
import org.firstinspires.ftc.onbotjava.handlers.objbuild.FetchBuildStatus;
import org.firstinspires.ftc.onbotjava.handlers.objbuild.FetchLog;
import org.firstinspires.ftc.onbotjava.handlers.objbuild.LaunchBuild;
import org.firstinspires.ftc.onbotjava.handlers.objbuild.WaitForBuild;
import org.firstinspires.ftc.onbotjava.handlers.javascript.FetchAutocompleteJavaScript;
import org.firstinspires.ftc.onbotjava.handlers.javascript.FetchJavaScriptSettings;
import org.firstinspires.ftc.onbotjava.handlers.websocket.LaunchBuildWs;
import org.firstinspires.ftc.robotcore.internal.webserver.WebHandler;
import org.firstinspires.ftc.robotcore.internal.webserver.websockets.FtcWebSocket;
import org.firstinspires.ftc.robotcore.internal.webserver.websockets.FtcWebSocketMessage;
import org.firstinspires.ftc.robotcore.internal.webserver.websockets.WebSocketManager;
import org.firstinspires.ftc.robotcore.internal.webserver.websockets.WebSocketMessageTypeHandler;
import org.firstinspires.ftc.robotserver.internal.programmingmode.ProgrammingMode;
import org.firstinspires.ftc.robotserver.internal.programmingmode.ProgrammingModeManager;
import org.firstinspires.ftc.robotserver.internal.webserver.websockets.WebSocketNamespaceHandlerRegistry;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class OnBotJavaProgrammingMode implements ProgrammingMode {
    public static final String URI_JAVA_PREFIX = "/java";
    @SuppressWarnings("unused") // required for the OnBotUI
    public static final String URI_JAVA_EDITOR = URI_JAVA_PREFIX + "/editor.html";
    @SuppressWarnings("unused")
    public static final String URI_JAVA_README_FILE = URI_JAVA_PREFIX + "/readme.md";
    // File Tasks
    public static final String URI_FILE_COPY = URI_JAVA_PREFIX + "/file/copy";
    public static final String URI_FILE_DELETE = URI_JAVA_PREFIX + "/file/delete";
    public static final String URI_FILE_DOWNLOAD = URI_JAVA_PREFIX + "/file/download";
    public static final String URI_FILE_GET = URI_JAVA_PREFIX + "/file/get";
    public static final String URI_FILE_NEW = URI_JAVA_PREFIX + "/file/new";
    public static final String URI_FILE_SAVE = URI_JAVA_PREFIX + "/file/save";
    public static final String URI_FILE_TEMPLATES = URI_JAVA_PREFIX + "/file/templates";
    public static final String URI_FILE_TREE = URI_JAVA_PREFIX + "/file/tree";
    public static final String URI_FILE_UPLOAD = URI_JAVA_PREFIX + "/file/upload";
    public static final String URI_JS_AUTOCOMPLETE = URI_JAVA_PREFIX + "/js/editor/autocomplete";
    // Build tasks
    public static final String URI_BUILD_LAUNCH = URI_JAVA_PREFIX + "/build/start";
    public static final String URI_BUILD_LOG = URI_JAVA_PREFIX + "/build/log";
    public static final String URI_BUILD_STATUS = URI_JAVA_PREFIX + "/build/status";
    public static final String URI_BUILD_WAIT = URI_JAVA_PREFIX + "/build/wait";
    public static final String URI_JS_SETTINGS = URI_JAVA_PREFIX + "/js/settings.js";
    // Admin tasks
    public static final String URI_ADMIN_CLEAN = URI_JAVA_PREFIX + "/admin/clean";
    public static final String URI_ADMIN_REARM = URI_JAVA_PREFIX + "/admin/rearm";
    public static final String URI_ADMIN_SETTINGS = URI_JAVA_PREFIX + "/admin/settings";
    public static final String URI_ADMIN_SETTINGS_RESET = URI_ADMIN_SETTINGS + "/reset";
    public static final String URI_ADMIN_RESET_ONBOTJAVA = URI_JAVA_PREFIX + "/admin/factory_reset";

    public static final String WS_NAMESPACE = "ONBOTJAVA";
    public static final String WS_BUILD_LAUNCH = "build:launch";
    public static final String WS_BUILD_STATUS = "build:status";

    private final String TAG = OnBotJavaProgrammingMode.class.getSimpleName();

    public void close() {
        OnBotJavaWebInterfaceManager.instance().buildMonitor().close();
    }

    public void register(ProgrammingModeManager manager) {
        List<WebHandler> webHandlerList = Arrays.asList(new Clean(), new Rearm(), new ResetOnBotJava(), new Settings(), new SettingsReset(), new FetchBuildStatus(), new FetchLog(), new LaunchBuild(), new WaitForBuild(),
                new CopyFile(), new DeleteFile(), new DownloadFile(), new FetchFileContents(), new FetchFileTemplates(), new FetchFileTree(), new NewFile(), new SaveFile(), new UploadFiles(), new FetchJavaScriptSettings(), new FetchAutocompleteJavaScript());

        try {
            for (WebHandler webHandler : webHandlerList) {
                if (!webHandler.getClass().isAnnotationPresent(RegisterWebHandler.class)) {
                    continue;
                }

                final RegisterWebHandler registerHandler = webHandler.getClass().getAnnotation(RegisterWebHandler.class);
                String uri = registerHandler.uri();
                boolean paramGenerator = registerHandler.usesParamGenerator();
                manager.register(uri, manager.decorate(paramGenerator, webHandler));
            }

            OnBotJavaWebInterfaceManager.instance().broadcastManager().registerWebSocketManager(manager.getWebServer().getWebSocketManager());
            OnBotJavaWebSocketTypedMessageHandler[] wsHandlers = {
                    new LaunchBuildWs()
            };
            Map<String, WebSocketMessageTypeHandler> webSocketHandlerMap = new ConcurrentHashMap<>();
            for (OnBotJavaWebSocketTypedMessageHandler handler : wsHandlers) {
                webSocketHandlerMap.put(handler.type(), handler);
            }
            manager.register(new OnBotJavaWebSocketHandler(webSocketHandlerMap));

            //RobotLog.dd(TAG, "It took %.3f s. to setup OnBotJava web handlers", (System.currentTimeMillis() - start) / 1000d);
        } catch (Exception ex) {
            RobotLog.ee(TAG, ex, "Failed to register OnBotJava web handlers");
        }
    }

}

