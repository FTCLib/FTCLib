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

package org.firstinspires.ftc.onbotjava.handlers.admin;

import android.annotation.SuppressLint;

import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.onbotjava.OnBotJavaFileSystemUtils;
import org.firstinspires.ftc.onbotjava.OnBotJavaManager;
import org.firstinspires.ftc.onbotjava.OnBotJavaProgrammingMode;
import org.firstinspires.ftc.onbotjava.OnBotJavaWebInterfaceManager;
import org.firstinspires.ftc.onbotjava.RegisterWebHandler;
import org.firstinspires.ftc.onbotjava.RequestConditions;
import org.firstinspires.ftc.onbotjava.StandardResponses;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.webserver.WebHandler;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import fi.iki.elonen.NanoHTTPD;

@RegisterWebHandler(uri = OnBotJavaProgrammingMode.URI_ADMIN_RESET_ONBOTJAVA)
public class ResetOnBotJava implements WebHandler {
    private static final String TAG = ResetOnBotJava.class.getName();
    private static final long INVALID_ID = -1;
    private long handshakeId = -1;

    @Override
    public NanoHTTPD.Response getResponse(NanoHTTPD.IHTTPSession session) {
        final Map<String, List<String>> parameters = session.getParameters();
        if (!parameters.containsKey(RequestConditions.REQUEST_KEY_ID)) {
            handshakeId = UUID.randomUUID().getMostSignificantBits();
            return StandardResponses.successfulRequest(Long.toString(handshakeId));
        } else {
            final String possibleHandshakeIdString = parameters.get(RequestConditions.REQUEST_KEY_ID).get(0);
            long possibleHandshakeId = INVALID_ID;
            try {
                possibleHandshakeId = Long.parseLong(possibleHandshakeIdString);
            } catch (NumberFormatException ignored) {

            }

            if (handshakeId != INVALID_ID && handshakeId == possibleHandshakeId) {
                try {
                    if (resetOnBotJava()) {
                        return StandardResponses.successfulJsonRequest("done");
                    } else {
                        return StandardResponses.serverError("user intervention required");
                    }
                } catch (RuntimeException ex) {
                    return StandardResponses.serverError("see logs");
                }
            }

            handshakeId = INVALID_ID;
            return StandardResponses.badRequest("invalid handshake");
        }
    }

    @SuppressLint({"SetWorldReadable", "SetWorldWritable"})
    private boolean resetOnBotJava() {
        final File javaRoot = OnBotJavaManager.javaRoot;

        // Set global R/W on our files in case permissions got messy
        javaRoot.setReadable(true, false);
        javaRoot.setWritable(true, false);
        javaRoot.setExecutable(true, false);
        for (File file : AppUtil.getInstance().filesIn(javaRoot)) {
            file.setWritable(true, false);
            file.setReadable(true, false);
            if (file.isDirectory()) { // allows us to read the folder listing
                file.setExecutable(true, false);
            }
        }

        AppUtil.getInstance().delete(javaRoot);
        if (javaRoot.exists()) { // delete failed
            if (javaRoot.isDirectory()) {
                for (File file : AppUtil.getInstance().filesIn(javaRoot)) {
                    RobotLog.e(TAG, "[RESET] Delete failed for %s", file.getAbsolutePath());
                }
            } else { // java root is not a directory, for some reason, but we have attempted to delete it, so we need user intervention
                RobotLog.e(TAG, "[RESET] Delete of javaRoot (\"%s\") failed, not a directory??? User required to delete \"%s\"", javaRoot, javaRoot);
            }

            return false;
        }

        OnBotJavaManager.initialize(); // resetup everything in OnBotJava
        OnBotJavaFileSystemUtils.ensureTemplates();
        OnBotJavaWebInterfaceManager.instance().editorSettings().resetToDefaults(); // reset editor defaults
        return true;
    }
}
