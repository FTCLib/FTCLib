/*
Copyright (c) 2018 Robert Atkinson

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
package org.firstinspires.ftc.robotcore.internal.vuforia.externalprovider;

import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.android.util.Size;
import org.firstinspires.ftc.robotcore.internal.camera.ImageFormatMapper;
import org.firstinspires.ftc.robotcore.internal.system.Misc;

/** see ExternalProvider.h */
@SuppressWarnings("WeakerAccess")
public class CameraMode
    {
    public final int width;
    public final int height;
    public final int fps;
    public final FrameFormat format;

    public CameraMode(int width, int height, int fps, FrameFormat format)
        {
        this.width = width;
        this.height = height;
        this.fps = fps;
        this.format = format;
        }

    public CameraMode(int[] data)
        {
        width = data[0];
        height = data[1];
        fps = data[2];
        format = FrameFormat.from(data[3]);
        }

    @Override public String toString()
        {
        return Misc.formatInvariant("CameraMode(format=%s:%d w=%d h=%d fps=%d)", format, format.ordinal(), width, height, fps);
        }

    public int[] toArray()
        {
        int[] result = new int[4];
        result[0] = width;
        result[1] = height;
        result[2] = fps;
        result[3] = format.ordinal();
        return result;
        }

    public int getAndroidFormat()
        {
        return ImageFormatMapper.androidFromVuforiaWebcam(format);
        }

    public Size getSize()
        {
        return new Size(width, height);
        }

    public long getNsFrameDuration()
        {
        return (long)Math.round((double) ElapsedTime.SECOND_IN_NANO / fps); // ie: billion * secondsPerFrame
        }

    public int getFramesPerSecond()
        {
        return fps;
        }
    }
