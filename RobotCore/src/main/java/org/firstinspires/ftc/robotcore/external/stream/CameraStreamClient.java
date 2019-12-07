/*
Copyright (c) 2019 Ryan Brott

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of Ryan Brott nor the names of his contributors may be used to
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
package org.firstinspires.ftc.robotcore.external.stream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.Nullable;
import android.util.SparseArray;
import android.util.SparseIntArray;

import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.internal.network.CallbackResult;
import org.firstinspires.ftc.robotcore.internal.network.RobotCoreCommandList;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Client for receiving camera frames sent from the RC on the DS. Frames are only sent when the DS
 * triggers the appropriate request.
 *
 * @see CameraStreamServer
 */
public class CameraStreamClient {
    private static final int MAX_CONCURRENT_FRAMES = 5;

    private static final CameraStreamClient INSTANCE = new CameraStreamClient();

    /**
     * Returns the application's stream server.
     */
    public static CameraStreamClient getInstance() {
        return INSTANCE;
    }

    private boolean available;

    private SortedMap<Integer, PartialFrame> partialFrames = new TreeMap<>();

    @Nullable private Listener listener;

    private CameraStreamClient() {

    }

    /**
     * Sets the frame receive listener.
     */
    public synchronized void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    /**
     * Returns true if the corresponding server is ready to stream frames.
     */
    public boolean isStreamAvailable() {
        return available;
    }

    public CallbackResult handleStreamChange(String extra) {
        RobotCoreCommandList.CmdStreamChange cmd = RobotCoreCommandList.CmdStreamChange.deserialize(extra);
        available = cmd.available;
        synchronized (this) {
            if (listener != null) {
                listener.onStreamAvailableChange(available);
            }
        }
        return CallbackResult.HANDLED;
    }

    public CallbackResult handleReceiveFrameBegin(String extra) {
        RobotCoreCommandList.CmdReceiveFrameBegin cmd = RobotCoreCommandList.CmdReceiveFrameBegin.deserialize(extra);
        PartialFrame partialFrame = new PartialFrame();
        partialFrame.length = cmd.getLength();
        partialFrame.data = new byte[cmd.getLength()];
        partialFrames.put(cmd.getFrameNum(), partialFrame);

        if (partialFrames.size() > MAX_CONCURRENT_FRAMES) {
            partialFrames.remove(partialFrames.firstKey());
        }

        return CallbackResult.HANDLED;
    }

    public CallbackResult handleReceiveFrameChunk(String extra) {
        try {
            RobotCoreCommandList.CmdReceiveFrameChunk cmd = RobotCoreCommandList.CmdReceiveFrameChunk.deserialize(extra);

            if (!partialFrames.containsKey(cmd.getFrameNum())) {
                return CallbackResult.HANDLED;
            }

            PartialFrame partialFrame = partialFrames.get(cmd.getFrameNum());
            int length = cmd.getData().length;
            System.arraycopy(cmd.getData(), 0, partialFrame.data,
                    cmd.getChunkNum() * CameraStreamServer.CHUNK_SIZE, length);
            partialFrame.bytesRead += length;

            if (partialFrame.bytesRead == partialFrame.length) {
                partialFrames.remove(cmd.getFrameNum());

                partialFrames.headMap(cmd.getFrameNum()).clear();

                Bitmap bitmap = BitmapFactory.decodeByteArray(partialFrame.data, 0, partialFrame.length);
                if (bitmap == null) {
                    RobotLog.e("Received invalid frame bitmap");
                    return CallbackResult.HANDLED;
                }

                synchronized (this) {
                    if (listener != null) {
                        listener.onFrameBitmap(bitmap);
                    }
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            RobotLog.e("Received too many frame bytes");
        }

        return CallbackResult.HANDLED;
    }

    /**
     * Interface for notifying the DS activity when new frames arrive.
     */
    public interface Listener {

        /**
         * Called when server signals stream availability.
         */
        void onStreamAvailableChange(boolean available);

        /**
         * Called on receipt of every frame.
         */
        void onFrameBitmap(Bitmap frameBitmap);
    }

    private static class PartialFrame {
        private int length, bytesRead;
        private byte[] data;
    }
}
