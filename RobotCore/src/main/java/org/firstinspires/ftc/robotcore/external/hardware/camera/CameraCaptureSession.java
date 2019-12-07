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

import android.hardware.camera2.CaptureFailure;
import android.support.annotation.NonNull;

import org.firstinspires.ftc.robotcore.external.function.Continuation;

/**
 * THIS INTERFACE IS EXPERIMENTAL. Its form and function may change in whole or in part
 * before being finalized for production use. Caveat emptor.
 *
 * {@link CameraCaptureSession} provides the means by which streaming data can be captured
 * from the camera.
 *
 * @see Camera#createCaptureSession
 */
@SuppressWarnings("WeakerAccess")
public interface CameraCaptureSession
    {
    /**
     * Get the camera device that this session is created for.
     */
    @NonNull Camera getCamera();

    /**
     * Close this capture session
     */
    void close();

    /**
     * Stream data from the camera: request endlessly repeating capture of images
     * by this capture session. {@link CameraFrame}s are provided through
     * {@link CaptureCallback#onNewFrame} as they become available.
     *
     * <p>With this method, the camera device will continually capture images
     * using the settings in the provided {@link CameraCaptureRequest}, at the maximum
     * rate possible.</p>
     *
     * <p>If the capture fails to start, then an exception is thrown. </p>
     *
     * <p>To stop the repeating capture, call {@link #stopCapture}</p>
     *
     * <p>Calling this method will replace any earlier repeating request or
     * burst set up by this method.</p>
     *
     * @param cameraCaptureRequest the request to repeat indefinitely
     * @param captureCallback The callback object to notify every time the
     * request finishes processing. If null, no metadata will be
     * produced for this stream of requests, although image data will
     * still be produced.
     * @param statusContinuation the callback to notify regarding status events of the capture
     *
     * @return CameraCaptureSequenceId A unique capture sequence ID used by
     *                               {@link StatusCallback#onCaptureSequenceCompleted}.
     *
     * @throws CameraException      if the camera device is no longer connected or has
     *                               encountered a fatal error
     * @throws IllegalStateException if this session is no longer active, either because the session
     *                               was explicitly closed, a new session has been created
     *                               or the camera device has been closed.
     *
     * @see #stopCapture
     * @see #startCapture(CameraCaptureRequest, Continuation, Continuation)
     */
    CameraCaptureSequenceId startCapture(@NonNull final CameraCaptureRequest cameraCaptureRequest,
                                         @NonNull CaptureCallback captureCallback,
                                         @NonNull Continuation<? extends StatusCallback> statusContinuation) throws CameraException;

    /**
     * As in {@link #startCapture(CameraCaptureRequest, CaptureCallback, Continuation)}, but supports
     * the generality of a {@link Continuation} to handle the capture. Note that unless this continuation
     * synchronously dispatches, or indicates that it can run on a guest worker thread, a copy of
     * each frame will be made before invoking {@link CaptureCallback#onNewFrame}.
     * @see Continuation#isDispatchSynchronous()
     * @see #startCapture(CameraCaptureRequest, CaptureCallback, Continuation)
     */
    CameraCaptureSequenceId startCapture(@NonNull final CameraCaptureRequest cameraCaptureRequest,
                                         @NonNull Continuation<? extends CaptureCallback> captureContinuation,
                                         @NonNull Continuation<? extends StatusCallback> statusContinuation) throws CameraException;


    /**
     * <p>Cancel any ongoing repeating capture set by {@link #startCapture}. This method
     * is idempotent.
     *
     * <p>Any currently in-flight captures will still complete.</p>
     *
     * @see #startCapture
     */
    void stopCapture();

    /**
     * <p>A callback object for tracking the progress of a {@link CameraCaptureRequest} submitted to the
     * camera device.</p>
     *
     * @see android.hardware.camera2.CameraCaptureSession.CaptureCallback
     */
    interface CaptureCallback
        {
        /**
         * This method is called when an image capture has fully completed and a newly captured
         * frame is available.
         *
         * @param session     the session returned by {@link Camera#createCaptureSession}
         * @param request     the request that was given to the Camera
         * @param cameraFrame the newly available frame
         *
         * @see #startCapture
         */
        void onNewFrame(@NonNull CameraCaptureSession session, @NonNull CameraCaptureRequest request, @NonNull CameraFrame cameraFrame);
        }

    interface StatusCallback
        {
        /**
         * This method is called when a capture sequence is completed. Once this is invoked, no
         * further calls to {@link CaptureCallback#onNewFrame} will be made.
         *
         * @param session                   The session returned by {@link Camera#createCaptureSession}
         * @param cameraCaptureSequenceId   A sequence ID returned by {@link #startCapture}
         * @param lastFrameNumber           The last frame number (returned by {@link CameraFrame#getFrameNumber}
         *                                  in the capture sequence, or {@link CameraFrame#UnknownFrameNumber}
         *                                  if no frames have been observed.
         *
         * @see CameraFrame#getFrameNumber()
         * @see CaptureFailure#getFrameNumber()
         * @see CameraFrame#getCaptureSequenceId()
         * @see CaptureFailure#getSequenceId()
         */
        void onCaptureSequenceCompleted(@NonNull CameraCaptureSession session, CameraCaptureSequenceId cameraCaptureSequenceId, long lastFrameNumber);
        }

    /**
     * A callback object for receiving updates about the state of a camera capture session.
     * @see android.hardware.camera2.CameraCaptureSession.StateCallback
     */
    interface StateCallback
        {
        /**
         * This method is called when the camera device has finished configuring itself, and the
         * session can start processing capture requests.
         *
         * @param session the session returned by {@link Camera#createCaptureSession}
         */
        void onConfigured(@NonNull CameraCaptureSession session);

        /**
         * This method is called when the session is closed.
         *
         * <p>A session is closed when a new session is created by the parent camera device,
         * or when the parent camera device is closed (either by the user closing the device,
         * or due to a camera device disconnection or fatal error).</p>
         *
         * <p>This method will not be called unless {@link #onConfigured} is called first. </p>
         *
         * @param session the session returned by {@link Camera#createCaptureSession}
         */
        void onClosed(@NonNull CameraCaptureSession session);
        }

    abstract class StateCallbackDefault implements StateCallback
        {
        @Override public void onConfigured(@NonNull CameraCaptureSession session)
            {
            }
        @Override public void onClosed(@NonNull CameraCaptureSession session)
            {
            }
        }
    }
