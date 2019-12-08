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
package org.firstinspires.ftc.robotcore.internal.camera.delegating;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.firstinspires.ftc.robotcore.external.function.ThrowingRunnable;
import org.firstinspires.ftc.robotcore.external.hardware.camera.Camera;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCaptureRequest;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCaptureSequenceId;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCaptureSession;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraException;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraFrame;
import org.firstinspires.ftc.robotcore.external.function.Continuation;
import org.firstinspires.ftc.robotcore.external.function.ContinuationResult;
import org.firstinspires.ftc.robotcore.internal.camera.RenumberedCameraFrame;
import org.firstinspires.ftc.robotcore.internal.system.Assert;
import org.firstinspires.ftc.robotcore.internal.system.RefCounted;
import org.firstinspires.ftc.robotcore.internal.system.Tracer;

import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("WeakerAccess")
public class DelegatingCaptureSequence extends RefCounted
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "DelCaptureSequence";
    public String getTag() { return delegatingCamera!=null ? delegatingCamera.getTag() + "|" + TAG : TAG; }
    protected final Tracer tracer;

    protected final Continuation<? extends CameraCaptureSession.CaptureCallback> userCaptureContinuation;
    protected final Continuation<? extends CameraCaptureSession.StatusCallback> userStatusContinuation;

    protected final DelegatingCamera            delegatingCamera;
    protected final DelegatingCaptureSession    delegatingCaptureSession;
    protected final DelegatingCaptureSequenceId delegatingSequenceId;
    protected final DelegatingCaptureRequest    delegatingCaptureRequest;

    protected boolean                   reportOnClose = false;
    protected boolean                   closeReported = false;
    protected final static AtomicInteger nextFrameNumber = new AtomicInteger(1);
    protected long                      lastFrameNumber = CameraFrame.UnknownFrameNumber;

    protected StreamingState            streamingState = StreamingState.Stopped;
    protected Camera                    camera = null;
    protected CameraCaptureRequest      cameraCaptureRequest = null;
    protected CameraCaptureSession      cameraCaptureSession = null;
    protected CameraCaptureSequenceId   cameraCaptureSequenceId = null;

    protected enum StreamingState { Started, Paused, Stopped };

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    DelegatingCaptureSequence(DelegatingCamera delegatingCamera,
            DelegatingCaptureSession delegatingCaptureSession,
            DelegatingCaptureSequenceId delegatingSequenceId,
            DelegatingCaptureRequest delegatingCaptureRequest,
            Continuation<? extends CameraCaptureSession.CaptureCallback> userCaptureContinuation,
            @NonNull Continuation<? extends CameraCaptureSession.StatusCallback> userStatusContinuation)
        {
        this.delegatingCaptureSession = delegatingCaptureSession;
        this.delegatingCamera = delegatingCamera;
        this.delegatingSequenceId = delegatingSequenceId;
        this.delegatingCaptureRequest = delegatingCaptureRequest;
        this.userCaptureContinuation = userCaptureContinuation;
        this.userStatusContinuation = userStatusContinuation;
        this.tracer = Tracer.create(getTag(), DelegatingCamera.TRACE);
        }

    @Override protected void destructor()
        {
        stopStreamingAndReportClosedIfNeeded();
        super.destructor();
        }

    public void onCameraChanged(final @Nullable Camera newCamera) // idempotent
        {
        synchronized (lock)
            {
            if (camera != newCamera)
                {
                tracer.trace(tracer.format("onCameraChange(%s->%s)", camera, newCamera), new Runnable()
                    {
                    @Override public void run()
                        {
                        // Whatever else, we don't want streaming from the current camera, so pause
                        pauseStreaming();

                        // Switch the current camera
                        camera = newCamera;

                        // Resume streaming on the new camera if there is one
                        if (camera != null)
                            {
                            resumeStreaming();
                            }
                        }
                    });
                }
            }
        }

    //----------------------------------------------------------------------------------------------
    // Streaming
    //----------------------------------------------------------------------------------------------

    public void startStreaming() throws CameraException
        {
        synchronized (lock)
            {
            tracer.trace(tracer.format("startStreaming(%s)", streamingState), new ThrowingRunnable<CameraException>()
                {
                @Override public void run() throws CameraException
                    {
                    switch (streamingState)
                        {
                        case Started:
                            // idempotent
                            break;
                        case Paused:
                            tracer.traceError("starting paused stream");
                            break;
                        case Stopped:
                            Assert.assertFalse(reportOnClose);
                            reportOnClose = true;
                            try {
                                doStreaming();
                                }
                            catch (RuntimeException e)
                                {
                                reportError(e);
                                reportOnClose = false; // never started, don't need to notify later
                                throw e;
                                }
                            catch (CameraException e)
                                {
                                reportError(e);
                                reportOnClose = false; // never started, don't need to notify later
                                throw e;
                                }
                            streamingState = StreamingState.Started;
                            break;
                        }
                    }
                });
            }
        }

    // Careful of callback locking loops!
    protected void reportError(CameraException e)
        {
        delegatingCamera.reportError(e.error);
        }
    protected void reportError(RuntimeException e)
        {
        delegatingCamera.reportError(Camera.Error.InternalError);
        }


    public void pauseStreaming()
        {
        synchronized (lock)
            {
            tracer.trace(tracer.format("pauseStreaming(%s)", streamingState), new Runnable()
                {
                @Override public void run()
                    {
                    switch (streamingState)
                        {
                        case Started:
                            undoStreaming();
                            streamingState = StreamingState.Paused;
                            break;
                        case Paused:
                            // idempotent
                            break;
                        case Stopped:
                            // paused stronger than paused
                            break;
                        }

                    }
                });
            }
        }

    public void resumeStreaming()
        {
        synchronized (lock)
            {
            tracer.trace(tracer.format("resumeStreaming(%s)", streamingState), new Runnable()
                {
                @Override public void run()
                    {
                    switch (streamingState)
                        {
                        case Started:
                            // idempotent
                            break;
                        case Paused:
                            boolean localSuccess = false;
                            try
                                {
                                doStreaming();
                                localSuccess = true;
                                streamingState = StreamingState.Started;
                                }
                            catch (RuntimeException e)
                                {
                                reportError(e);
                                }
                            catch (CameraException e)
                                {
                                reportError(e);
                                }
                            finally
                                {
                                // We need to report closure if we *ever* successfully started
                                reportOnClose = reportOnClose || localSuccess;
                                }
                            break;
                        case Stopped:
                            // resuming a dead horse: ignored
                            break;
                        }
                    }
                });
            }
        }

    public void stopStreamingAndReportClosedIfNeeded()
        {
        synchronized (lock)
            {
            undoStreaming();
            reportClosedIfNeeded();
            streamingState = StreamingState.Stopped;
            }
        }

    class InterveningCaptureCallback implements CameraCaptureSession.CaptureCallback
        {
        @Override public void onNewFrame(@NonNull CameraCaptureSession session, final @NonNull CameraCaptureRequest request, final @NonNull CameraFrame cameraFrame)
            {
            /**
             * Using dispatchHere is important for CameraFrame copying reasons. And it's legit and ok because
             * we used {@link Continuation#createForNewTarget} in {@link #doStreaming()}
             */
            userCaptureContinuation.dispatchHere(new ContinuationResult<CameraCaptureSession.CaptureCallback>()
                {
                @Override public void handle(CameraCaptureSession.CaptureCallback captureCallback)
                    {
                    RenumberedCameraFrame renumberedCameraFrame = new RenumberedCameraFrame(delegatingCaptureRequest, delegatingSequenceId, cameraFrame, nextFrameNumber.getAndIncrement());
                    lastFrameNumber = renumberedCameraFrame.getFrameNumber();
                    captureCallback.onNewFrame(delegatingCaptureSession, request, renumberedCameraFrame);
                    renumberedCameraFrame.releaseRef();
                    }
                });
            }
        }

    class InterveningStatusCallback implements CameraCaptureSession.StatusCallback
        {
        @Override public void onCaptureSequenceCompleted(@NonNull CameraCaptureSession session, CameraCaptureSequenceId cameraCaptureSequenceId, long lastFrameNumberIgnored)
            {
            userStatusContinuation.dispatchHere(new ContinuationResult<CameraCaptureSession.StatusCallback>()
                {
                @Override public void handle(CameraCaptureSession.StatusCallback statusCallback)
                    {
                    statusCallback.onCaptureSequenceCompleted(delegatingCaptureSession, delegatingSequenceId, lastFrameNumber);
                    }
                });
            }
        }

    protected void doStreaming() throws CameraException
        {
        tracer.trace("doStreaming()", new ThrowingRunnable<CameraException>()
            {
            @Override public void run() throws CameraException
                {
                synchronized (lock)
                    {
                    Assert.assertNotNull(camera);
                    Assert.assertNull(cameraCaptureSession);
                    Assert.assertNull(cameraCaptureRequest);

                    Continuation<DelegatingCaptureSession.InterveningStateCallback> sessionContinuation = delegatingCaptureSession.newInterveningStateCallback();
                    cameraCaptureSession = camera.createCaptureSession(sessionContinuation); // Can throw!
                    if (sessionContinuation.getTarget().awaitConfiguredOrClosed())
                        {
                        cameraCaptureRequest = camera.createCaptureRequest(delegatingCaptureRequest.androidFormat, delegatingCaptureRequest.size, delegatingCaptureRequest.fps);
                        try {
                            cameraCaptureSequenceId = cameraCaptureSession.startCapture(cameraCaptureRequest,
                                userCaptureContinuation.createForNewTarget(new InterveningCaptureCallback()),
                                userStatusContinuation.createForNewTarget(new InterveningStatusCallback()));
                            }
                        catch (CameraException e)
                            {
                            tracer.traceError(e, "exception starting capture");
                            undoStreaming();
                            throw e;
                            }
                        catch (RuntimeException e)
                            {
                            tracer.traceError(e, "exception starting capture");
                            undoStreaming();
                            throw new CameraException(Camera.Error.InternalError, e);
                            }
                        }
                    else
                        {
                        tracer.traceError("awaitConfiguredOrClosed(): unable to open capture session");
                        cameraCaptureSession.close();
                        cameraCaptureSession = null;
                        throw new CameraException(Camera.Error.Timeout);
                        }
                    }
                }
            });
        }

    protected void undoStreaming() // idempotent
        {
        synchronized (lock)
            {
            if (cameraCaptureSession != null)
                {
                tracer.trace("undoStreaming()", new Runnable()
                    {
                    @Override public void run()
                        {
                        cameraCaptureSession.stopCapture();
                        cameraCaptureSession.close();
                        cameraCaptureSession = null;
                        cameraCaptureRequest = null;
                        }
                    });
                }
            }
        }

    protected void reportClosedIfNeeded()
        {
        synchronized (lock)
            {
            if (reportOnClose && !closeReported)
                {
                closeReported = true;
                userStatusContinuation.dispatch(new ContinuationResult<CameraCaptureSession.StatusCallback>()
                    {
                    @Override public void handle(CameraCaptureSession.StatusCallback captureCallback)
                        {
                        captureCallback.onCaptureSequenceCompleted(delegatingCaptureSession, delegatingSequenceId, lastFrameNumber);
                        }
                    });
                }
            }
        }
    }
