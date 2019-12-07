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

import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.external.Consumer;
import org.firstinspires.ftc.robotcore.external.android.util.Size;
import org.firstinspires.ftc.robotcore.external.hardware.camera.Camera;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCaptureSession;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraException;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraFrame;
import org.firstinspires.ftc.robotcore.external.function.Continuation;
import org.firstinspires.ftc.robotcore.external.function.ContinuationResult;
import org.firstinspires.ftc.robotcore.internal.camera.ImageFormatMapper;
import org.firstinspires.ftc.robotcore.internal.camera.libuvc.nativeobject.UvcDeviceHandle;
import org.firstinspires.ftc.robotcore.internal.camera.libuvc.nativeobject.UvcFrame;
import org.firstinspires.ftc.robotcore.internal.camera.libuvc.constants.UvcFrameFormat;
import org.firstinspires.ftc.robotcore.internal.camera.libuvc.nativeobject.UvcStreamCtrl;
import org.firstinspires.ftc.robotcore.internal.camera.libuvc.nativeobject.UvcStreamHandle;
import org.firstinspires.ftc.robotcore.internal.system.DestructOnFinalize;

import java.io.IOException;

/**
 * {link UvcApiCameraCaptureSequence} is the result of a request to initiate streaming.
 * @see CameraCaptureSession#startCapture
 */
@SuppressWarnings("WeakerAccess")
class UvcApiCameraCaptureSequence extends DestructOnFinalize<UvcApiCaptureSession>
    {
    protected final Continuation<? extends CameraCaptureSession.CaptureCallback> userCaptureContinuation;
    protected final Continuation<? extends CameraCaptureSession.StatusCallback> userStatusContinuation;

    protected final UvcApiCameraCaptureSequenceId   uvcCaptureSequenceId;
    protected final UvcApiCameraCaptureRequest      uvcCameraCaptureRequest;

    protected boolean               reportOnClose = false;
    protected boolean               closeReported = false;
    protected long                  lastFrameNumber = CameraFrame.UnknownFrameNumber;

    protected UvcStreamHandle       uvcStreamHandle = null;

    public UvcApiCameraCaptureSequence(UvcApiCaptureSession captureSession,
            UvcApiCameraCaptureSequenceId uvcCaptureSequenceId,
            UvcApiCameraCaptureRequest uvcCameraCaptureRequest,
            Continuation<? extends CameraCaptureSession.CaptureCallback> userCaptureContinuation,
            Continuation<? extends CameraCaptureSession.StatusCallback> userStatusContinuation)
        {
        this.setParent(captureSession);
        this.uvcCaptureSequenceId = uvcCaptureSequenceId;
        this.uvcCameraCaptureRequest = uvcCameraCaptureRequest;
        this.userCaptureContinuation = userCaptureContinuation;
        this.userStatusContinuation = userStatusContinuation;
        }

    @Override protected void destructor()
        {
        stopStreamingAndReportClosedIfNeeded();
        super.destructor();
        }

    protected UvcDeviceHandle getDeviceHandle()
        {
        return getCaptureSession().getDeviceHandle();
        }

    protected UvcApiCaptureSession getCaptureSession()
        {
        return getParent();
        }

    protected void startStreaming() throws CameraException
        {
        synchronized (lock)
            {
            reportOnClose = true;
            try
                {
                UvcFrameFormat format = ImageFormatMapper.uvcFromAndroid(uvcCameraCaptureRequest.getAndroidFormat());
                Size size = uvcCameraCaptureRequest.getSize();
                UvcStreamCtrl uvcStreamCtrl = getDeviceHandle().getStreamControl(format, size.getWidth(), size.getHeight(), uvcCameraCaptureRequest.getFramesPerSecond());
                if (uvcStreamCtrl != null)
                    {
                    try
                        {
                        uvcStreamHandle = uvcStreamCtrl.open();
                        if (uvcStreamHandle != null)
                            {
                            try
                                {
                                uvcStreamHandle.startStreaming(new Consumer<UvcFrame>()
                                    {
                                    @Override public void accept(final UvcFrame uvcFrame)
                                        {
                                        // We are currently on a UvcLib worker thread. Do we have to copy the frame, or can we be assured
                                        // that the dispatch below will be synchronous and so we don't need to.
                                        boolean runHere = userCaptureContinuation.isDispatchSynchronous() || userCaptureContinuation.canBorrowThread(Thread.currentThread());
                                        final UvcApiCameraFrame capturedFrame = new UvcApiCameraFrame(UvcApiCameraCaptureSequence.this, uvcFrame, !runHere);

                                        ContinuationResult<CameraCaptureSession.CaptureCallback> callOnNewFrame = new ContinuationResult<CameraCaptureSession.CaptureCallback>()
                                            {
                                            @Override public void handle(CameraCaptureSession.CaptureCallback captureCallback)
                                                {
                                                lastFrameNumber = capturedFrame.getFrameNumber();
                                                captureCallback.onNewFrame(getCaptureSession(), uvcCameraCaptureRequest, capturedFrame);
                                                capturedFrame.releaseRef();
                                                }
                                            };

                                        if (runHere)
                                            {
                                            // We avoid a pointless thread switch by invoking on the UVC worker
                                            // thread instead of an actual thread pool worker thread. This also
                                            // avoids an unnecessary copy of the frame data itself.
                                            userCaptureContinuation.dispatchHere(callOnNewFrame);
                                            }
                                        else
                                            {
                                            userCaptureContinuation.dispatch(callOnNewFrame);
                                            }
                                        }
                                    });
                                }
                            catch (IOException | RuntimeException e)
                                {
                                RobotLog.ee(UvcDeviceHandle.TAG, e, "uvcStreamHandle.startStreaming() failed");
                                uvcStreamHandle.releaseRef();
                                uvcStreamHandle = null;
                                throw new CameraException(Camera.Error.OtherError, e);
                                }
                            }
                        else
                            {
                            RobotLog.ee(UvcDeviceHandle.TAG, "uvcStreamCtrl.open() failed");
                            throw new CameraException(Camera.Error.OtherError);
                            }
                        }
                    finally
                        {
                        uvcStreamCtrl.releaseRef();
                        }
                    }
                else
                    {
                    RobotLog.ee(UvcDeviceHandle.TAG, "getStreamControl() failed");
                    throw new CameraException(Camera.Error.StreamingRequestNotSupported);
                    }
                }
            catch (RuntimeException e)
                {
                reportOnClose = false;
                throw new CameraException(Camera.Error.InternalError, e);
                }
            catch (CameraException e)
                {
                reportOnClose = false;
                throw e;
                }
            }
        }

    protected void stopStreamingAndReportClosedIfNeeded()
        {
        synchronized (lock)
            {
            // Shut down the actual streaming first, before we report closure: more deterministic,
            // and users can't resurrect anyway.
            if (uvcStreamHandle != null)
                {
                uvcStreamHandle.stopStreaming();
                uvcStreamHandle.releaseRef();
                uvcStreamHandle = null;
                }
            reportClosedIfNeeded();
            }
        }

    protected void reportClosedIfNeeded()
        {
        synchronized (lock)
            {
            if (reportOnClose && !closeReported)
                {
                closeReported = true;
                final UvcApiCaptureSession uvcCaptureSession = getCaptureSession();
                uvcCaptureSession.addRef();
                userStatusContinuation.dispatch(new ContinuationResult<CameraCaptureSession.StatusCallback>()
                    {
                    @Override public void handle(CameraCaptureSession.StatusCallback captureCallback)
                        {
                        captureCallback.onCaptureSequenceCompleted(uvcCaptureSession, uvcCaptureSequenceId, lastFrameNumber);
                        uvcCaptureSession.releaseRef();
                        }
                    });
                }
            }
        }
    }
