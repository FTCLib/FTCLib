/*
Copyright (c) 2017 Robert Atkinson

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of Robert Atkinson nor the names of his contributors may be used to
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
package org.firstinspires.ftc.robotcore.internal.camera;

import android.graphics.Bitmap;
import androidx.annotation.NonNull;

import org.firstinspires.ftc.robotcore.external.android.util.Size;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCaptureRequest;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCaptureSequenceId;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraFrame;
import org.firstinspires.ftc.robotcore.internal.camera.libuvc.api.UvcApiCameraFrame;
import org.firstinspires.ftc.robotcore.internal.camera.libuvc.constants.UvcFrameFormat;
import org.firstinspires.ftc.robotcore.internal.system.RefCounted;

/**
 * A {@link CameraFrame} that maintains its own notion of frame number. This allows frames
 * that might in fact come from independent actual sources to be unified into one overall capture
 * sequence, for example
 */
@SuppressWarnings("WeakerAccess")
public class RenumberedCameraFrame extends RefCounted implements CameraFrame, CameraFrameInternal
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "RenumberedCameraFrame";
    public String getTag() { return TAG; }

    protected final CameraCaptureRequest captureRequest;
    protected final CameraCaptureSequenceId captureSequenceId;
    protected final CameraFrame innerFrame;
    protected final long frameNumber;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public RenumberedCameraFrame(CameraCaptureRequest captureRequest, CameraCaptureSequenceId captureSequenceId, CameraFrame innerFrame, long frameNumber)
        {
        super(TraceLevel.VeryVerbose);
        this.captureRequest = captureRequest;
        this.captureSequenceId = captureSequenceId;
        this.innerFrame = innerFrame;
        this.frameNumber = frameNumber;

        innerFrame.addRef(); // DKLJKKLJKLLKJkllk
        }

    @Override protected void destructor()
        {
        innerFrame.releaseRef(); // DKLJKKLJKLLKJkllk
        super.destructor();
        }

    //----------------------------------------------------------------------------------------------
    // CameraFrame
    //----------------------------------------------------------------------------------------------

    @Override public UvcApiCameraFrame getUvcApiCameraFrame()
        {
        if (innerFrame instanceof CameraFrameInternal)
            {
            return ((CameraFrameInternal)innerFrame).getUvcApiCameraFrame();
            }
        return null;
        }

    //----------------------------------------------------------------------------------------------
    // CameraFrame
    //----------------------------------------------------------------------------------------------

    @NonNull @Override public CameraCaptureRequest getRequest()
        {
        return captureRequest;
        }

    @Override public long getFrameNumber()
        {
        return frameNumber;
        }

    @Override public Size getSize()
        {
        return innerFrame.getSize();
        }

    @Override public int getImageSize()
        {
        return innerFrame.getImageSize();
        }

    @Override public long getImageBuffer()
        {
        return innerFrame.getImageBuffer();
        }

    @Override public long getCaptureTime()
        {
        return innerFrame.getCaptureTime();
        }

    @Override public UvcFrameFormat getUvcFrameFormat()
        {
        return innerFrame.getUvcFrameFormat();
        }

    @Override public int getStride()
        {
        return innerFrame.getStride();
        }

    @Override public CameraCaptureSequenceId getCaptureSequenceId()
        {
        return captureSequenceId;
        }

    @Override public void copyToBitmap(Bitmap bitmap)
        {
        innerFrame.copyToBitmap(bitmap);
        }

    @Override public CameraFrame copy()
        {
        CameraFrame innerCopy = innerFrame.copy();
        CameraFrame result = new RenumberedCameraFrame(captureRequest, captureSequenceId, innerCopy, frameNumber);
        innerCopy.releaseRef();
        return result;
        }
    }
