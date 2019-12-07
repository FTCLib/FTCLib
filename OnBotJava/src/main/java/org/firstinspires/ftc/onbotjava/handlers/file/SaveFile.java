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

import com.qualcomm.robotcore.util.ReadWriteFile;

import org.firstinspires.ftc.onbotjava.OnBotJavaManager;
import org.firstinspires.ftc.onbotjava.OnBotJavaProgrammingMode;
import org.firstinspires.ftc.onbotjava.OnBotJavaSecurityManager;
import org.firstinspires.ftc.onbotjava.RegisterWebHandler;
import org.firstinspires.ftc.onbotjava.RequestConditions;
import org.firstinspires.ftc.onbotjava.StandardResponses;
import org.firstinspires.ftc.robotcore.internal.webserver.WebHandler;

import java.io.File;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

import static org.firstinspires.ftc.onbotjava.StandardResponses.badRequest;

@RegisterWebHandler(uri = OnBotJavaProgrammingMode.URI_FILE_SAVE)
public class SaveFile implements WebHandler {
    /**
     * <p>Called with {@link OnBotJavaProgrammingMode#URI_FILE_SAVE}</p>
     * <p>
     * <p>Handle a file operation (not limited to saving), however only one major operation
     * per request is supported.</p>
     * Currently supported operations:
     * <ul>
     * <li>Save
     * <p>Requires a "code" entry in data map</p>
     * </li>
     * <p>
     * </ul>
     *
     * @param method HTTP request method
     * @param data   POSTed data, represented in a map form
     */
    private NanoHTTPD.Response saveFile(NanoHTTPD.Method method, Map<String, List<String>> data) {
        if (!data.containsKey(RequestConditions.REQUEST_KEY_FILE)) return StandardResponses.badRequest();
        String uri = data.get(RequestConditions.REQUEST_KEY_FILE).get(0);
        if (!OnBotJavaSecurityManager.isValidSourceFileLocation(uri) || !NanoHTTPD.Method.POST.equals(method)) {
            return StandardResponses.badRequest();
        }

        final String KEY_SAVE = RequestConditions.REQUEST_KEY_SAVE;
        if (!data.containsKey(KEY_SAVE)) {
            return StandardResponses.badRequest();
        }

        String code;
        code = data.get(KEY_SAVE).get(0);
        if (code == null) return StandardResponses.badRequest();
        File codeFile = new File(OnBotJavaManager.javaRoot, uri);
        ReadWriteFile.writeFile(codeFile, code);
        return StandardResponses.successfulRequest(data);

        // default response
    }

    @Override
    public NanoHTTPD.Response getResponse(NanoHTTPD.IHTTPSession session) {
        return saveFile(session.getMethod(), session.getParameters());
    }
}
