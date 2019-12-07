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

import com.qualcomm.robotcore.util.ClassUtil;
import org.firstinspires.ftc.onbotjava.JavaSourceFile;
import org.firstinspires.ftc.onbotjava.OnBotJavaFileSystemUtils;
import org.firstinspires.ftc.onbotjava.OnBotJavaManager;
import org.firstinspires.ftc.onbotjava.OnBotJavaProgrammingMode;
import org.firstinspires.ftc.onbotjava.OnBotJavaSecurityManager;
import org.firstinspires.ftc.onbotjava.RegisterWebHandler;
import org.firstinspires.ftc.onbotjava.RequestConditions;
import org.firstinspires.ftc.onbotjava.StandardResponses;
import org.firstinspires.ftc.robotcore.internal.webserver.WebHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import fi.iki.elonen.NanoHTTPD;

import static org.firstinspires.ftc.onbotjava.StandardResponses.badRequest;

@RegisterWebHandler(uri = OnBotJavaProgrammingMode.URI_FILE_COPY)
public class CopyFile implements WebHandler {
    private static class RecursiveCopyOperation {
        private File src;
        private File dest;

        private RecursiveCopyOperation(File src, File dest) {
            this.src = src;
            this.dest = dest;
        }
    }

    @Override
    public NanoHTTPD.Response getResponse(NanoHTTPD.IHTTPSession session) {
        if (!RequestConditions.containsParameters(session, RequestConditions.REQUEST_KEY_COPY_TO, RequestConditions.REQUEST_KEY_COPY_FROM)) {
            return StandardResponses.badRequest();
        }

        final String fromFileName = RequestConditions.dataForParameter(session, RequestConditions.REQUEST_KEY_COPY_FROM);
        final String destFileName = RequestConditions.dataForParameter(session, RequestConditions.REQUEST_KEY_COPY_TO);
        if (!OnBotJavaSecurityManager.isValidSourceFileOrFolder(fromFileName) ||
                !OnBotJavaSecurityManager.isValidSourceFileOrFolder(destFileName))
            return StandardResponses.badRequest();

        File origin = new File(OnBotJavaManager.javaRoot, fromFileName);
        File dest = new File(OnBotJavaManager.javaRoot, destFileName);
        try {
            List<RecursiveCopyOperation> opsList = new ArrayList<>();
            generateRecursiveCopyList(origin, dest, opsList);
            executeRecursiveCopy(opsList);
        } catch (IOException ex) {
            return StandardResponses.badRequest("cannot copy files");
        }

        return StandardResponses.successfulRequest();
    }

    private void generateRecursiveCopyList(File origin, File dest, List<? super RecursiveCopyOperation> filesToCopy) throws IOException {
        if (origin.isDirectory()) {
            //if (dest.exists() && !dest.isDirectory()) throw new IOException("Cannot merge origin and destination");
            dest = checkForSameNameConflicts(dest);
            filesToCopy.add(new RecursiveCopyOperation(origin, dest));
            final String[] files = origin.list();
            for (String file : files) {
                File src = new File(origin, file);
                File destFile = new File(dest, file);
                destFile = checkForSameNameConflicts(destFile);
                // Prevent a file from being copied endlessly, if we are copying the parent folder to the inside of itself
                if (src.getAbsolutePath().equals(dest.getAbsolutePath())) {
                    continue;
                }

                if (src.isDirectory()) {
                    filesToCopy.add(new RecursiveCopyOperation(src, destFile));
                }

                generateRecursiveCopyList(src, destFile, filesToCopy);
            }
        } else {
            filesToCopy.add(new RecursiveCopyOperation(origin, dest));
        }
    }

    private void executeRecursiveCopy(List<? extends RecursiveCopyOperation> ops) throws IOException {
        for (RecursiveCopyOperation op : ops) {
            File src = op.src;
            File dst = op.dest;
            if (src.isDirectory()) {
                dst.mkdirs();
            } else {
                copyFile(src, dst);
            }
        }
    }

    /**
     * Copies the given source File to the given dest File.
     */
    private void copyFile(File source, File dest) throws IOException {
        dest = checkForSameNameConflicts(dest);

        if (source.getPath().endsWith(OnBotJavaFileSystemUtils.EXT_JAVA_FILE)) {
            JavaSourceFile.forFile(source).copyTo(dest);
        } else {
            try (FileChannel sourceChannel = new FileInputStream(source).getChannel();
                 FileChannel destChannel = new FileOutputStream(dest).getChannel()) {
                destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
            }
        }
    }

    private File checkForSameNameConflicts(File dest) {
        if (dest.exists()) {
            String originalName = dest.getName();
            String ext = "";
            if (originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf('.'));
                originalName = originalName.substring(0, originalName.lastIndexOf('.'));
            }
            String suffix = "_Copy";
            if (originalName.endsWith(suffix)) {
                suffix = "";
            }
            dest = new File(dest.getParentFile(), originalName + suffix + ext);
            for (int i = 2; i < 1000 && dest.exists(); i++) {
                dest = new File(dest.getParentFile(), originalName + suffix + i + ext);
            }
        }
        return dest;
    }
}
