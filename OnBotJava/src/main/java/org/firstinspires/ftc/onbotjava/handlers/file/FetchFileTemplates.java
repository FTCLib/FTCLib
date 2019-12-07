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
import org.firstinspires.ftc.onbotjava.OnBotJavaWebInterfaceManager;
import org.firstinspires.ftc.onbotjava.RegisterWebHandler;
import org.firstinspires.ftc.onbotjava.StandardResponses;
import org.firstinspires.ftc.robotcore.internal.webserver.WebHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fi.iki.elonen.NanoHTTPD;

import static org.firstinspires.ftc.onbotjava.OnBotJavaFileSystemUtils.templatesDir;

@RegisterWebHandler(uri = OnBotJavaProgrammingMode.URI_FILE_TEMPLATES)
public class FetchFileTemplates implements WebHandler {
    private String response;
    private boolean templatesEnsured = true;

    /**
     * <p>Called with {@link OnBotJavaProgrammingMode#URI_FILE_TEMPLATES}</p>
     * <p>Makes a listing of the available project templates for use in JSON format</p>
     */
    private NanoHTTPD.Response projectTemplates() {
        if (response == null || !templatesEnsured) {
            List<String> templates = new ArrayList<>();
            //final File templatesFolder = templatesDir;
            templatesEnsured = OnBotJavaFileSystemUtils.ensureTemplates();
            String templatePath = templatesDir.getAbsolutePath();
            OnBotJavaFileSystemUtils.searchForFiles(templatePath, templatesDir, templates, false);
            for (int i = 0; i < templates.size(); i++) {
                String template = templates.get(i);

                // remove temp files and plain old directories
                if (template.endsWith(OnBotJavaFileSystemUtils.EXT_TEMP_FILE)) {
                    templates.remove(template);
                    // Preserve current loop index by subtracting 1 then continuing the loop, causing the loop to re-run the same index
                    // which now should be a different object
                    --i;
                    continue;
                }

                if (template.startsWith(OnBotJavaFileSystemUtils.PATH_SEPARATOR)) {
                    template = template.substring(1);
                    templates.set(i, template);
                }
            }

            Collections.sort(templates);
            response = OnBotJavaWebInterfaceManager.instance().gson().toJson(templates);
        }

        return StandardResponses.successfulJsonRequest(response);
    }

    @Override
    public NanoHTTPD.Response getResponse(NanoHTTPD.IHTTPSession session) {
        return projectTemplates();
    }
}
