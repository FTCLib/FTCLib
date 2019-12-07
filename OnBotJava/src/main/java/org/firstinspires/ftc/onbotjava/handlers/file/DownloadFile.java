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

package org.firstinspires.ftc.onbotjava.handlers.file;


import org.firstinspires.ftc.onbotjava.OnBotJavaFileSystemUtils;
import org.firstinspires.ftc.onbotjava.OnBotJavaProgrammingMode;
import org.firstinspires.ftc.onbotjava.RegisterWebHandler;
import org.firstinspires.ftc.onbotjava.RequestConditions;

import org.firstinspires.ftc.onbotjava.OnBotJavaManager;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.webserver.WebHandler;

import java.nio.file.Path;
import java.util.Date;

import fi.iki.elonen.NanoHTTPD;

@RegisterWebHandler(uri = OnBotJavaProgrammingMode.URI_FILE_DOWNLOAD)
public class DownloadFile implements WebHandler {
    @Override
    public NanoHTTPD.Response getResponse(NanoHTTPD.IHTTPSession session) {
        final NanoHTTPD.Response file = OnBotJavaFileSystemUtils.getFile(session.getParameters(), true, OnBotJavaFileSystemUtils.LineEndings.WINDOWS.lineEnding);
        if (file.getStatus() != NanoHTTPD.Response.Status.OK) {
            return file;
        }

        String fileName = RequestConditions.dataForParameter(session, RequestConditions.REQUEST_KEY_FILE);
        if (fileName.equals(OnBotJavaFileSystemUtils.PATH_SEPARATOR + OnBotJavaManager.srcDir.getName() + OnBotJavaFileSystemUtils.PATH_SEPARATOR)) {
            fileName = "OnBotJava-" + AppUtil.getInstance().getIso8601DateFormat().format(new Date()) + OnBotJavaFileSystemUtils.EXT_ZIP_FILE;
        } else if (fileName.endsWith(OnBotJavaFileSystemUtils.PATH_SEPARATOR)) {
            // Check to see if this is a folder, if so add a ".zip" extension
            fileName = fileName.substring(0, fileName.length() - 1);
            fileName = fileName.substring(fileName.lastIndexOf(OnBotJavaFileSystemUtils.PATH_SEPARATOR) + 1);
            fileName +=  OnBotJavaFileSystemUtils.EXT_ZIP_FILE;
        } else if (fileName.contains(OnBotJavaFileSystemUtils.PATH_SEPARATOR)) {
            fileName = fileName.substring(fileName.lastIndexOf(OnBotJavaFileSystemUtils.PATH_SEPARATOR) + 1);
        }

        file.addHeader("Content-Disposition", "attachment; filename=" + fileName);
        file.addHeader("Pragma",  "no-cache");

        return file;
    }
}
