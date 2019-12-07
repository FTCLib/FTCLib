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
package org.firstinspires.ftc.robotcore.external.hardware.camera;

import android.graphics.ImageFormat;
import android.graphics.PixelFormat;

import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.android.util.Size;
import org.firstinspires.ftc.robotcore.internal.system.Misc;

import java.util.List;

/**
 * THIS INTERFACE IS EXPERIMENTAL. Its form and function may change in whole or in part
 * before being finalized for production use. Caveat emptor.
 *
 * Metadata regarding the configuration of video streams that a camera might produce.
 * Modelled after {@link android.hardware.camera2.params.StreamConfigurationMap}, though
 * significantly simplified here.
 */
@SuppressWarnings("WeakerAccess")
public interface CameraCharacteristics
    {
    //----------------------------------------------------------------------------------------------
    // Accessing
    //----------------------------------------------------------------------------------------------

    /**
     * Get the image {@code format} output formats in this camera
     *
     * <p>All image formats returned by this function will be defined in either {@link ImageFormat}
     * or in {@link PixelFormat} (and there is no possibility of collision).</p>
     *
     * @return an array of integer format. The formats are as identified by {@link ImageFormat}
     * and {@link PixelFormat}.
     *
     * @see ImageFormat
     * @see PixelFormat
     */
    int[] getAndroidFormats();

    /**
     * Get a list of sizes compatible with the requested image {@code format}.
     *
     * <p>The {@code format} should be a supported format (one of the formats returned by
     * {@link #getAndroidFormats}).</p>
     *
     * @param androidFormat an image format from {@link ImageFormat} or {@link PixelFormat}
     * @return
     *          an array of supported sizes,
     *          or {@code null} if the {@code format} is not a supported output
     *
     * @see ImageFormat
     * @see PixelFormat
     * @see #getAndroidFormats
     */
    Size[] getSizes(int androidFormat);

    /** Gets the device-recommended optimum size for the indicated format */
    Size getDefaultSize(int androidFormat);

    /**
     * Get the minimum frame duration for the format/size combination (in nanoseconds).
     *
     * <p>{@code format} should be one of the ones returned by {@link #getAndroidFormats()}.</p>
     * <p>{@code size} should be one of the ones returned by {@link #getSizes(int)}.</p>
     *
     * @param androidFormat an image format from {@link ImageFormat} or {@link PixelFormat}
     * @param size an output-compatible size
     * @return a minimum frame duration {@code >} 0 in nanoseconds, or
     *          0 if the minimum frame duration is not available.
     *
     * @throws IllegalArgumentException if {@code format} or {@code size} was not supported
     * @throws NullPointerException if {@code size} was {@code null}
     *
     * @see ImageFormat
     * @see PixelFormat
     */
    long getMinFrameDuration(int androidFormat, Size size);

    /**
     * Returns the maximum fps rate supported for the given format.
     * 
     * @return the maximum fps rate supported for the given format.
     */
    int getMaxFramesPerSecond(int androidFormat, Size size);



    class CameraMode
        {
        public final int  androidFormat;
        public final Size size;
        public final long nsFrameDuration;
        public final int  fps;
        public final boolean isDefaultSize; // not used in equalitor

        public CameraMode(int androidFormat, Size size, long nsFrameDuration, boolean isDefaultSize)
            {
            this.androidFormat = androidFormat;
            this.size = size;
            this.nsFrameDuration = nsFrameDuration;
            this.fps = (int) (ElapsedTime.SECOND_IN_NANO / nsFrameDuration);
            this.isDefaultSize = isDefaultSize;
            }

        @Override public String toString()
            {
            return Misc.formatInvariant("CameraMode(format=%d %dx%d fps=%d)", androidFormat, size.getWidth(), size.getHeight(), fps);
            }

        @Override public boolean equals(Object o)
            {
            if (o instanceof CameraMode)
                {
                CameraMode them = (CameraMode)o;
                return androidFormat == them.androidFormat
                        && size.equals(them.size)
                        && nsFrameDuration == them.nsFrameDuration
                        && fps == them.fps;
                }
            else
                return super.equals(o);
            }

        @Override public int hashCode()
            {
            return Integer.valueOf(androidFormat).hashCode() ^ size.hashCode() ^ Integer.valueOf(fps).hashCode();
            }
        }

    /**
     * Returns all the combinatorial format, size, and fps camera modes supported.
     *
     * @return all the combinatorial format, size, and fps camera modes supported
     */
    List<CameraMode> getAllCameraModes();
    }
