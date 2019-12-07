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


import org.firstinspires.ftc.onbotjava.*;
import com.google.gson.JsonSyntaxException;

import org.firstinspires.ftc.onbotjava.OnBotJavaManager;
import org.firstinspires.ftc.robotcore.internal.webserver.WebHandler;

import java.io.File;

import fi.iki.elonen.NanoHTTPD;

import static org.firstinspires.ftc.onbotjava.StandardResponses.badRequest;

@RegisterWebHandler(uri = OnBotJavaProgrammingMode.URI_FILE_DELETE)
public class DeleteFile implements WebHandler {
    /**
     * <li>Delete
     * <p>Requires a "delete" entry in data map. This should be a JSON encoded array</p>
     * </li>
     */
    @Override
    public NanoHTTPD.Response getResponse(NanoHTTPD.IHTTPSession session) {
        if (RequestConditions.containsParameters(session, RequestConditions.REQUEST_KEY_DELETE)) {
            String[] deleteFiles;
            try {
                deleteFiles = OnBotJavaWebInterfaceManager.instance().gson().fromJson(RequestConditions.dataForParameter(session, RequestConditions.REQUEST_KEY_DELETE), String[].class);
            } catch (JsonSyntaxException ex) {
                return StandardResponses.badRequest("Invalid delete syntax - bad JSON");
            }
            for (String fileToDeletePath : deleteFiles) {
                final File fileToDelete = new File(OnBotJavaManager.javaRoot, fileToDeletePath);
                if (fileToDelete.exists()) {
                    recursiveDelete(fileToDelete);
                }
            }
            return StandardResponses.successfulRequest();
        }

        return StandardResponses.badRequest();
    }

    /**
     * Recursively delete a file or a folder
     *
     * @param file the starting point
     */
    private void recursiveDelete(File file) {
        if (file.isDirectory()) {
            final File[] files = file.listFiles();
            for (File file1 : files) {
                recursiveDelete(file1);
            }
        }
        file.delete();
    }
}
