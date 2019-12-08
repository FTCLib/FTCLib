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

import com.qualcomm.robotcore.util.ThreadPool;

import org.firstinspires.ftc.robotcore.external.function.ThrowingSupplier;
import org.firstinspires.ftc.robotcore.external.hardware.camera.Camera;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCaptureRequest;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCaptureSequenceId;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCaptureSession;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraException;
import org.firstinspires.ftc.robotcore.external.function.Continuation;
import org.firstinspires.ftc.robotcore.external.function.ContinuationResult;
import org.firstinspires.ftc.robotcore.internal.system.CloseableRefCounted;
import org.firstinspires.ftc.robotcore.internal.system.Misc;
import org.firstinspires.ftc.robotcore.internal.system.Tracer;

import java.util.concurrent.CountDownLatch;

@SuppressWarnings("WeakerAccess")
public class DelegatingCaptureSession extends CloseableRefCounted implements CameraCaptureSession
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "DelCaptureSession";
    public String getTag() { return delegatingCamera!=null ? delegatingCamera.getTag() + "|" + TAG : TAG; }
    protected final Tracer tracer;

    protected final Continuation<? extends CameraCaptureSession.StateCallback> userStateContinuation;
    protected final DelegatingCamera            delegatingCamera; // no ref
    protected final int                         captureSessionId;
    protected Camera                            camera = null;
    protected int                               nextCaptureSequenceId = 1;
    protected boolean                           closeReported = false;
    protected DelegatingCaptureSequence         delegatingCaptureSequence = null;

    @Override public String toString()
        {
        return Misc.formatInvariant("%s(%s)", this.getClass().getSimpleName(), captureSessionId);
        }

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public DelegatingCaptureSession(DelegatingCamera delegatingCamera, Continuation<? extends StateCallback> userStateContinuation, int captureSessionId)
        {
        this.delegatingCamera = delegatingCamera;
        this.captureSessionId = captureSessionId;
        this.userStateContinuation = userStateContinuation;
        this.tracer = Tracer.create(getTag(), DelegatingCamera.TRACE);

        enableOnlyClose();
        reportConfigured();
        }

    @Override protected void doClose()
        {
        tracer.trace("doClose()", new Runnable()
            {
            @Override public void run()
                {
                shutdown();
                DelegatingCaptureSession.super.doClose();
                }
            });
        }

    protected void shutdown()
        {
        stopCapture();
        reportClosedIfNeeded();
        }

    @Override protected void destructor()
        {
        tracer.trace("destructor()", new Runnable()
            {
            @Override public void run()
                {
                shutdown();
                delegatingCamera.onClosed(DelegatingCaptureSession.this);
                DelegatingCaptureSession.super.destructor();
                }
            });
        }

    public void onCameraChanged(final @Nullable Camera newCamera)
        {
        synchronized (lock)
            {
            if (camera != newCamera)
                {
                tracer.trace(tracer.format("onCameraChange(%s->%s)", camera, newCamera), new Runnable()
                    {
                    @Override public void run()
                        {
                        camera = newCamera;
                        updateCameraInSequence();
                        }
                    });
                }
            }
        }

    protected void updateCameraInSequence()
        {
        synchronized (lock)
            {
            if (delegatingCaptureSequence != null)
                {
                delegatingCaptureSequence.onCameraChanged(camera);
                }
            }
        }

    //----------------------------------------------------------------------------------------------
    // Callbacks
    //----------------------------------------------------------------------------------------------

    public Continuation<InterveningStateCallback> newInterveningStateCallback()
        {
        return Continuation.create(ThreadPool.getDefault(), new InterveningStateCallback());
        }

    protected class InterveningStateCallback implements CameraCaptureSession.StateCallback
        {
        protected CountDownLatch latch = new CountDownLatch(1);
        protected boolean configured = false;

        @Override public void onConfigured(@NonNull CameraCaptureSession session)
            {
            // Camera session has become configured. We largely ignore: the user's notion of the
            // session being configured is independent of the actual cameras that come and go.
            tracer.trace("camera session is configured: %s", session.getCamera().getCameraName());
            configured = true;
            latch.countDown();
            }

        @Override public void onClosed(@NonNull CameraCaptureSession session)
            {
            // Camera session has closed. We largely ignore: the user's notion of the session being
            // closed is independent of the actual cameras that come and go.
            tracer.trace("camera session is closed: %s", session.getCamera().getCameraName());
            configured = false;
            latch.countDown();
            }

        public boolean awaitConfiguredOrClosed()
            {
            try {
                latch.await();
                return configured;
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                return false;
                }
            }
        }

    protected void reportConfigured()
        {
        synchronized (lock)
            {
            addRef();
            userStateContinuation.dispatch(new ContinuationResult<StateCallback>()
                {
                @Override public void handle(StateCallback stateCallback)
                    {
                    stateCallback.onConfigured(DelegatingCaptureSession.this);
                    releaseRef();
                    }
                });
            }
        }

    protected void reportClosedIfNeeded() // idempotent
        {
        synchronized (lock)
            {
            if (!closeReported)
                {
                closeReported = true;
                addRef();
                userStateContinuation.dispatch(new ContinuationResult<StateCallback>()
                    {
                    @Override public void handle(StateCallback stateCallback)
                        {
                        stateCallback.onClosed(DelegatingCaptureSession.this);
                        releaseRef();
                        }
                    });
                }
            }
        }

    //----------------------------------------------------------------------------------------------
    // CameraCaptureSession
    //----------------------------------------------------------------------------------------------

    @NonNull @Override public Camera getCamera()
        {
        return delegatingCamera.selfCamera;
        }

    @Override public CameraCaptureSequenceId startCapture(@NonNull final CameraCaptureRequest cameraCaptureRequest,
                                         @NonNull CaptureCallback captureCallback,
                                         @NonNull Continuation<? extends StatusCallback> statusContinuation) throws CameraException
        {
        return startCapture(cameraCaptureRequest, Continuation.createTrivial(captureCallback), statusContinuation);
        }

    @Override public CameraCaptureSequenceId startCapture(final @NonNull CameraCaptureRequest captureRequest,
            final @NonNull Continuation<? extends CaptureCallback> userCaptureContinuation,
            final @NonNull Continuation<? extends StatusCallback> userStatusContinuation) throws CameraException
        {
        return tracer.trace("startCapture()", new ThrowingSupplier<CameraCaptureSequenceId, CameraException>()
            {
            @Override public CameraCaptureSequenceId get() throws CameraException
                {
                synchronized (lock)
                    {
                    if (!DelegatingCaptureRequest.isForCamera(delegatingCamera, captureRequest))
                        {
                        throw Misc.illegalArgumentException("capture request is not from this camera");
                        }
                    DelegatingCaptureRequest resiliantCameraCaptureRequest = (DelegatingCaptureRequest)captureRequest;

                    stopCapture();
                    try {
                        delegatingCaptureSequence = new DelegatingCaptureSequence(delegatingCamera, DelegatingCaptureSession.this,
                                                                                  new DelegatingCaptureSequenceId(delegatingCamera, nextCaptureSequenceId++),
                                                                                  resiliantCameraCaptureRequest, userCaptureContinuation, userStatusContinuation); // akklalk3ll3l3l
                        updateCameraInSequence();
                        delegatingCaptureSequence.startStreaming();
                        return delegatingCaptureSequence.delegatingSequenceId;
                        }
                    catch (CameraException|RuntimeException e)
                        {
                        stopCapture();
                        throw e;
                        }
                    }
                }
            });
        }

    @Override public void stopCapture()
        {
        try {
            synchronized (lock)
                {
                if (delegatingCaptureSequence != null)
                    {
                    tracer.trace("stopCapture()", new Runnable()
                        {
                        @Override public void run()
                            {
                            delegatingCaptureSequence.stopStreamingAndReportClosedIfNeeded();
                            delegatingCaptureSequence.releaseRef(); // akklalk3ll3l3l
                            delegatingCaptureSequence = null;
                            }
                        });
                    }
                }
            }
        catch (RuntimeException e)
            {
            tracer.traceError(e, "unexpected exception in stopCapture(); ignoring");
            }
        }
    }
