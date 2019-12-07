/*
 * Copyright (c) 2018 David Sargent, Noah Andrews
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
 * Neither the name of NAME nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior
 * written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
 * LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESSFOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package org.firstinspires.ftc.onbotjava.handlers.file;

import com.qualcomm.robotcore.util.ReadWriteFile;

import org.firstinspires.ftc.onbotjava.JavaSourceFile;
import org.firstinspires.ftc.onbotjava.OnBotJavaManager;
import org.firstinspires.ftc.onbotjava.OnBotJavaProgrammingMode;
import org.firstinspires.ftc.onbotjava.RegisterWebHandler;
import org.firstinspires.ftc.onbotjava.StandardResponses;
import org.firstinspires.ftc.robotserver.internal.webserver.RobotControllerWebHandlers;

import java.io.File;

import fi.iki.elonen.NanoHTTPD;

import static org.firstinspires.ftc.onbotjava.OnBotJavaFileSystemUtils.EXT_JAVA_FILE;
import static org.firstinspires.ftc.onbotjava.OnBotJavaFileSystemUtils.PATH_SEPARATOR;

@RegisterWebHandler(uri = OnBotJavaProgrammingMode.URI_FILE_UPLOAD, usesParamGenerator = false)
public class UploadFiles extends RobotControllerWebHandlers.FileUpload {

    @Override public NanoHTTPD.Response hook(File uploadedFile) {
        return StandardResponses.successfulJsonRequest(uploadedFile.getName());
    }

    @Override public File provideDestinationDirectory(String fileName, File tempFile) {
        File destDirectory;

        if (fileName.endsWith(EXT_JAVA_FILE)) {
            destDirectory = OnBotJavaManager.srcDir;
            final String code = ReadWriteFile.readFile(tempFile);
            String packageName = JavaSourceFile.extractPackageNameFromContents(code);

            if (packageName != null) {
                String folder = packageName.replaceAll("\\.", PATH_SEPARATOR);
                destDirectory = new File(destDirectory, folder);
            }
        } else if (fileName.endsWith(".jar")) {
            destDirectory = OnBotJavaManager.jarDir;
        } else {
            destDirectory = OnBotJavaManager.srcDir;
        }

        return destDirectory;
    }

    @Override
    protected String getFileName(String uploadedFileName) {
        return uploadedFileName.replaceAll("(?:\\.\\.|\\\\|/)", ""); // Make the uploaded file's name safe for Java by removing illegal characters
    }
}