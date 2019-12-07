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
import android.support.annotation.Nullable;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.OpModeManager;
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerNotifier;
import com.qualcomm.robotcore.robocol.Command;

import org.firstinspires.ftc.robotcore.external.function.Consumer;
import org.firstinspires.ftc.robotcore.external.function.Continuation;
import org.firstinspires.ftc.robotcore.internal.network.CallbackResult;
import org.firstinspires.ftc.robotcore.internal.network.NetworkConnectionHandler;
import org.firstinspires.ftc.robotcore.internal.network.RobotCoreCommandList;

import java.io.ByteArrayOutputStream;

import org.firstinspires.ftc.robotcore.internal.opmode.OpModeManagerImpl;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

/**
 * Server for sending camera frames from the RC to the corresponding client on the DS. Frames are
 * sent one at a time and can only be triggered with a DS request.
 *
 * @see CameraStreamClient
 */
public class CameraStreamServer implements OpModeManagerNotifier.Notifications {
    public static final int CHUNK_SIZE = 4096;
    private static final int DEFAULT_JPEG_QUALITY = 75;

    private static int frameNum;

    private static final CameraStreamServer INSTANCE = new CameraStreamServer();

    /**
     * Returns the application's stream server.
     */
    public static CameraStreamServer getInstance() {
        return INSTANCE;
    }

    private OpModeManagerImpl opModeManager;

    @Nullable private CameraStreamSource source;

    private int jpegQuality = DEFAULT_JPEG_QUALITY;

    private CameraStreamServer() {

    }

    public synchronized void setSource(@Nullable CameraStreamSource source) {
        this.source = source;

        RobotCoreCommandList.CmdStreamChange cmd = new RobotCoreCommandList.CmdStreamChange();
        cmd.available = source != null;
        NetworkConnectionHandler.getInstance().sendCommand(
                new Command(RobotCoreCommandList.CMD_STREAM_CHANGE, cmd.serialize()));

        if (source != null) {
            opModeManager = OpModeManagerImpl.getOpModeManagerOfActivity(AppUtil.getInstance().getActivity());
            if (opModeManager != null) {
                opModeManager.registerListener(this);
            }
        }
    }

    public int getJpegQuality() {
        return jpegQuality;
    }

    public void setJpegQuality(int quality) {
        jpegQuality = quality;
    }

    public CallbackResult handleRequestFrame() {
        if (source != null) {
            synchronized (this) {
                source.getFrameBitmap(Continuation.createTrivial(new Consumer<Bitmap>() {
                    @Override
                    public void accept(Bitmap bitmap) {
                        sendFrame(bitmap);
                    }
                }));
            }
        }

        return CallbackResult.HANDLED;
    }

    private void sendFrame(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, outputStream);
        byte[] data = outputStream.toByteArray();

        NetworkConnectionHandler handler = NetworkConnectionHandler.getInstance();
        RobotCoreCommandList.CmdReceiveFrameBegin receiveFrameBegin =
                new RobotCoreCommandList.CmdReceiveFrameBegin(frameNum, data.length);
        handler.sendCommand(new Command(RobotCoreCommandList.CMD_RECEIVE_FRAME_BEGIN, receiveFrameBegin.serialize()));

        for (int chunkNum = 0; chunkNum < Math.ceil((double) data.length / CHUNK_SIZE); chunkNum++) {
            int offset = chunkNum * CHUNK_SIZE;
            int length = Math.min(CHUNK_SIZE, data.length - offset);
            RobotCoreCommandList.CmdReceiveFrameChunk receiveFrameChunk =
                    new RobotCoreCommandList.CmdReceiveFrameChunk(frameNum, chunkNum, data, offset, length);
            handler.sendCommand(new Command(RobotCoreCommandList.CMD_RECEIVE_FRAME_CHUNK, receiveFrameChunk.serialize()));
        }

        frameNum++;
    }

    @Override
    public void onOpModePreInit(OpMode opMode) {

    }

    @Override
    public void onOpModePreStart(OpMode opMode) {

    }

    @Override
    public void onOpModePostStop(OpMode opMode) {
        setSource(null);

        if (opModeManager != null) {
            opModeManager.unregisterListener(this);
            opModeManager = null;
        }
    }
}
