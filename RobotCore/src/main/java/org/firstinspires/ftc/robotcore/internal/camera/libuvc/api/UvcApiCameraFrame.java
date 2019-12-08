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
package org.firstinspires.ftc.robotcore.internal.camera.libuvc.api;

import android.graphics.Bitmap;
import androidx.annotation.NonNull;

import org.firstinspires.ftc.robotcore.external.android.util.Size;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCaptureRequest;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCaptureSequenceId;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraFrame;
import org.firstinspires.ftc.robotcore.internal.camera.CameraFrameInternal;
import org.firstinspires.ftc.robotcore.internal.camera.libuvc.constants.UvcFrameFormat;
import org.firstinspires.ftc.robotcore.internal.camera.libuvc.nativeobject.UvcFrame;
import org.firstinspires.ftc.robotcore.internal.system.DestructOnFinalize;

/**
 * {@link UvcApiCameraFrame} is the internal implementation of {@link CameraFrame}
 */
@SuppressWarnings("WeakerAccess")
public class UvcApiCameraFrame extends DestructOnFinalize/*no parent*/ implements CameraFrame, CameraFrameInternal
    {
    /**
     * Note: we do NOT addref our capture request (by setting it as our parent, for example).
     * We can get away with this because our only uses of it return data that is NOT backed
     * by any native objects, and so the normal Java memory management applies. And this is
     * important because captured frames are likely more than anything else in the API to be
     * relatively long lived / manipulated and stored by the user. So avoiding unnecessary
     * backing state is worthwhile.
     */
    protected UvcApiCameraCaptureSequence captureSequence;
    protected UvcFrame uvcFrame;

    public UvcApiCameraFrame(UvcApiCameraCaptureSequence captureSequence, UvcFrame uvcFrame, boolean copyFrame)
        {
        super(TraceLevel.VeryVerbose);
        this.captureSequence = captureSequence;
        if (copyFrame)
            {
            this.uvcFrame = uvcFrame.copy();
            }
        else
            {
            this.uvcFrame = uvcFrame;
            this.uvcFrame.addRef();
            }
        }

    public long getPointer()
        {
        return uvcFrame.getPointer();
        }

    @Override protected void destructor()
        {
        if (this.uvcFrame != null)
            {
            this.uvcFrame.releaseRef();
            this.uvcFrame = null;
            }
        super.destructor();
        }

    @Override public UvcApiCameraFrame getUvcApiCameraFrame()
        {
        return this;
        }

    @NonNull @Override public CameraCaptureRequest getRequest()
        {
        return captureSequence.uvcCameraCaptureRequest;
        }

    @Override public long getFrameNumber()
        {
        return uvcFrame.getFrameNumber();
        }

    @Override public UvcFrameFormat getUvcFrameFormat()
        {
        return uvcFrame.getFrameFormat();
        }

    @Override public int getStride()
        {
        return uvcFrame.getStride();
        }

    @Override public Size getSize()
        {
        return getRequest().getSize();
        }

    @Override public int getImageSize()
        {
        return uvcFrame.getImageSize();
        }

    @Override public long getImageBuffer()
        {
        return uvcFrame.getImageBuffer();
        }

    @Override public long getCaptureTime()
        {
        return uvcFrame.getCaptureTime();
        }

    @Override public CameraCaptureSequenceId getCaptureSequenceId()
        {
        return captureSequence.uvcCaptureSequenceId;
        }

    @Override public void copyToBitmap(Bitmap bitmap)
        {
        uvcFrame.copyToBitmap(bitmap);
        }

    @Override public CameraFrame copy()
        {
        return new UvcApiCameraFrame(captureSequence, uvcFrame, true);
        }
    }
