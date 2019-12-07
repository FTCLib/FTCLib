/*
Copyright (c) 2018 Noah Andrews

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of Noah Andrews nor the names of his contributors may be used to
endorse or promote products derived from this software without specific prior
written permission.

NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package org.firstinspires.ftc.robotserver.internal.webserver.tempfile;

import android.os.Environment;

import com.qualcomm.robotcore.util.RobotLog;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import fi.iki.elonen.NanoHTTPD;

public class UploadedTempFileManager implements NanoHTTPD.TempFileManager {
    private static final String TAG = "UploadedTempFileManager";
    private static final File tempDir = new File(Environment.getExternalStorageDirectory(), "tmp");

    private final List<NanoHTTPD.TempFile> tempFileList = new CopyOnWriteArrayList<>();

    // Should only be instantiated by UploadedTempFileManagerFactory
    UploadedTempFileManager() {
        if (!tempDir.exists()) {
            if (!tempDir.mkdirs()) {
                RobotLog.ee(TAG, "Failed to create temp directory");
            }
        }
    }

    @Override public void clear() {
        for (NanoHTTPD.TempFile tempFile: tempFileList) {
            try {
                tempFile.delete();
            } catch (Exception e) {
                RobotLog.ee(TAG, e,"Failed to delete temp file");
            }
        }
        tempFileList.clear();
    }

    @Override public NanoHTTPD.TempFile createTempFile(String filenameHint) throws Exception {
       UploadedTempFile newTempFile =  new UploadedTempFile(tempDir);
       tempFileList.add(newTempFile);
       return newTempFile;
    }
}
