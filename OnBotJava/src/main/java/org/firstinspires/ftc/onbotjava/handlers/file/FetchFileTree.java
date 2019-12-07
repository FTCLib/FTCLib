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
import org.firstinspires.ftc.onbotjava.StandardResponses;

import org.firstinspires.ftc.onbotjava.OnBotJavaManager;
import org.firstinspires.ftc.robotcore.internal.webserver.WebHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import fi.iki.elonen.NanoHTTPD;

@RegisterWebHandler(uri = OnBotJavaProgrammingMode.URI_FILE_TREE)
public class FetchFileTree implements WebHandler {
    /**
     * <p>Called with {@link OnBotJavaProgrammingMode#URI_FILE_TREE}</p>
     * <p>
     * <p>Generates a listing of files in the {@link OnBotJavaManager#srcDir} and
     * {@link OnBotJavaManager#jarDir}. </p>
     * {@link OnBotJavaManager#jarDir}
     *
     * @return This returns the file tree in JSON, with the src and jar directories separate fields
     */
    private NanoHTTPD.Response projectTree() {
    /*
    <li>src:  .java source code is placed here in package-appropriate subdirectories, in the
        *                 usual Java style</li>
     */
        final String srcPath = OnBotJavaManager.srcDir.getAbsolutePath();
        File srcDir = OnBotJavaManager.srcDir;
        if (!srcDir.isDirectory()) srcDir.mkdirs();
        ArrayList<String> srcList = new ArrayList<>();
    /*
    *      <li>jars: (optional) Any externally-compiled jar src can be placed in this
    *                directory. They will be installed in the system, much as the .java source
    *                src are after they have been compiled.</li>
     */
        final String jarPath = OnBotJavaManager.jarDir.getAbsolutePath();
        File jarDir = OnBotJavaManager.jarDir.getAbsoluteFile();
        if (!jarDir.isDirectory()) jarDir.mkdirs();
        ArrayList<String> jarList = new ArrayList<>();
        OnBotJavaFileSystemUtils.searchForFiles(srcPath, srcDir, srcList, true);
        OnBotJavaFileSystemUtils.searchForFiles(jarPath, jarDir, jarList, true);

        return StandardResponses.successfulRequest(new FileTree(srcList, jarList));
    }

    @Override
    public NanoHTTPD.Response getResponse(NanoHTTPD.IHTTPSession session) {
        return projectTree();
    }

    private static class FileTree {
        final List<String> src;
        final List<String> jars;

        private FileTree(List<String> src, List<String> jars) {
            this.src = src;
            this.jars = jars;
        }
    }
}
