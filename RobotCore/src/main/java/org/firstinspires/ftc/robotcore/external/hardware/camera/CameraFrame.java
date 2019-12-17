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

import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import org.firstinspires.ftc.robotcore.external.android.util.Size;
import org.firstinspires.ftc.robotcore.internal.camera.libuvc.constants.UvcFrameFormat;

import java.util.concurrent.Executor;

/**
 * THIS INTERFACE IS EXPERIMENTAL. Its form and function may change in whole or in part
 * before being finalized for production use. Caveat emptor.
 *
 * {@link CameraFrame} represents one frame of captured video.
 */
@SuppressWarnings("WeakerAccess")
public interface CameraFrame
    {
    /**
     * Returns the request associated with this result.
     *
     * @return The request associated with this result.
     */
    @NonNull CameraCaptureRequest getRequest();

    /**
     * Get the frame number associated with this result.
     *
     * <p>Whenever a request has been processed, regardless of failure or success,
     * it gets a unique frame number assigned to its future result/failure.</p>
     *
     * <p>For the same type of request (capturing from the camera device or reprocessing), this
     * value monotonically increments, starting with 0, for every new result or failure and the
     * scope is the lifetime of the {@link Camera}. Between different types of requests,
     * the frame number may not monotonically increment. For example, the frame number of a newer
     * reprocess result may be smaller than the frame number of an older result of capturing new
     * images from the camera device, but the frame number of a newer reprocess result will never be
     * smaller than the frame number of an older reprocess result.</p>
     *
     * @return The frame number. This will always be >=0.
     *
     * @see Camera#createCaptureRequest
     */
    long getFrameNumber();

    /**
     * A frame number that never appears in a real camera frame.
     */
    long UnknownFrameNumber = -1;

    /**
     * Returns the dimensions of the image
     */
    Size getSize();

    /**
     * Returns the number of bytes in the image
     */
    int getImageSize();

    /**
     * Returns access to the data of the image. This will be {@link #getImageSize()} in length.
     */
    long getImageBuffer();

    /**
     * Returns the time on the System.nanoTime() clock at which this frame was captured.
     */
    long getCaptureTime();

    /**
     * Returns the format of this frame using the {@link UvcFrameFormat} enumeration.
     */
    UvcFrameFormat getUvcFrameFormat();

    /**
     * Number of bytes per horizontal line (undefined/zero for compressed format)
     */
    int getStride();

    /**
     * The sequence ID for this frame that was returned by the
     * {@link CameraCaptureSession#startCapture} family of functions.
     *
     * <p>The sequence ID is a unique monotonically-increasing value starting from 0,
     * incremented every time a new group of requests is submitted to the Camera.</p>
     *
     * @return int The ID for the sequence of requests that this capture result is a part of
     *
     * @see CameraCaptureSession.CaptureCallback#onCaptureSequenceCompleted
     */
    CameraCaptureSequenceId getCaptureSequenceId();

    /**
     * Copies the contents of the {@link CameraFrame} into the indicated bitmap. The size
     * of the bitmap must be compatible with this result.
     *
     * @see CameraCaptureRequest#createEmptyBitmap()
     */
    void copyToBitmap(Bitmap bitmap);

    /**
     * Returns a copy of this frame, one whose data is guaranteed to be accessible as
     * long as the instance is extant. It is recommended that a copied frame be {@link #releaseRef()}'d
     * when no longer needed in order to help improve memory usage (this is not <em>required</em>).
     *
     * @return a copy of this frame
     * @see #releaseRef()
     */
    CameraFrame copy();

    /**
     * Adds a counted reference to the camera frame to facilitate it's deterministic reclamation
     * when no longer needed.
     * @see #releaseRef()
     */
    void addRef();

    /**
     * @see #copy()
     * @see #addRef()
     * @return used for debugging <em>only</em>
     */
    int releaseRef();
    }
