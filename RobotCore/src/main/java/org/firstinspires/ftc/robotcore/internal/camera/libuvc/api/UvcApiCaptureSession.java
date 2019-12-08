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

import androidx.annotation.NonNull;

import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.external.hardware.camera.Camera;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCaptureRequest;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCaptureSequenceId;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCaptureSession;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraException;
import org.firstinspires.ftc.robotcore.external.function.Continuation;
import org.firstinspires.ftc.robotcore.external.function.ContinuationResult;
import org.firstinspires.ftc.robotcore.internal.camera.libuvc.nativeobject.UvcDeviceHandle;
import org.firstinspires.ftc.robotcore.internal.system.CloseableDestructOnFinalize;
import org.firstinspires.ftc.robotcore.internal.system.Misc;
import org.firstinspires.ftc.robotcore.internal.system.Tracer;

/**
 * {@link UvcApiCaptureSession} is the internal implementation of {@link CameraCaptureSession}.
 */
@SuppressWarnings("WeakerAccess")
public class UvcApiCaptureSession extends CloseableDestructOnFinalize<UvcDeviceHandle> implements CameraCaptureSession
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public String getTag()
        {
        return UvcApiCaptureSession.class.getSimpleName();
        }
    public static boolean TRACE = true;
    protected final Tracer tracer = Tracer.create(getTag(), TRACE);

    protected final Continuation<? extends StateCallback> userContinuation;
    protected final int                     captureSessionId;
    protected int                           nextCaptureSequenceId = 1;
    protected boolean                       closeReported = false;
    protected UvcApiCameraCaptureSequence   uvcCaptureSequence = null;

    @Override public String toString()
        {
        return Misc.formatInvariant("%s(%s)", this.getClass().getSimpleName(), captureSessionId);
        }

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public UvcApiCaptureSession(UvcDeviceHandle uvcDeviceHandle, Continuation<? extends StateCallback> userContinuation, int captureSessionId)
        {
        setParent(uvcDeviceHandle);
        this.captureSessionId = captureSessionId;
        this.userContinuation = userContinuation;

        enableOnlyClose(); /** to support {@link CameraCaptureSession#close()} */
        reportConfigured();
        }

    public UvcDeviceHandle getDeviceHandle()
        {
        return getParent();
        }

    @Override public void doClose()
        {
        tracer.trace("doClose()", new Runnable()
            {
            @Override public void run()
                {
                shutdown();
                UvcApiCaptureSession.super.doClose();
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
        tracer.trace("destructor", new Runnable()
            {
            @Override public void run()
                {
                shutdown();
                getParent().onClosed(UvcApiCaptureSession.this);
                UvcApiCaptureSession.super.destructor();
                }
            });
        }

    //----------------------------------------------------------------------------------------------
    // Callbacks
    //----------------------------------------------------------------------------------------------

    protected void reportConfigured()
        {
        synchronized (lock)
            {
            addRef();
            userContinuation.dispatch(new ContinuationResult<StateCallback>()
                {
                @Override public void handle(StateCallback stateCallback)
                    {
                    stateCallback.onConfigured(UvcApiCaptureSession.this);
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
                userContinuation.dispatch(new ContinuationResult<StateCallback>()
                    {
                    @Override public void handle(StateCallback stateCallback)
                        {
                        stateCallback.onClosed(UvcApiCaptureSession.this);
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
        return getDeviceHandle().getSelfCamera();
        }

    @Override public CameraCaptureSequenceId startCapture(@NonNull final CameraCaptureRequest cameraCaptureRequest,
                                         @NonNull CaptureCallback captureCallback,
                                         @NonNull Continuation<? extends StatusCallback> statusContinuation) throws CameraException
        {
        return startCapture(cameraCaptureRequest, Continuation.createTrivial(captureCallback), statusContinuation);
        }

    public CameraCaptureSequenceId startCapture(@NonNull final CameraCaptureRequest cameraCaptureRequest,
            @NonNull final Continuation<? extends CaptureCallback> captureContinuation,
            @NonNull final Continuation<? extends StatusCallback> statusContinuation) throws CameraException
        {
        synchronized (lock)
            {
            if (!UvcApiCameraCaptureRequest.isForDeviceHandle(getDeviceHandle(), cameraCaptureRequest))
                {
                throw Misc.illegalArgumentException("capture request is not from this camera");
                }

            UvcApiCameraCaptureRequest uvcApiCameraCaptureRequest = (UvcApiCameraCaptureRequest)cameraCaptureRequest;

            stopCapture();
            try
                {
                uvcCaptureSequence = new UvcApiCameraCaptureSequence(this, new UvcApiCameraCaptureSequenceId(getParent(), nextCaptureSequenceId++), uvcApiCameraCaptureRequest, captureContinuation, statusContinuation); // lakllj987987
                uvcCaptureSequence.startStreaming();
                return uvcCaptureSequence.uvcCaptureSequenceId;
                }
            catch (CameraException | RuntimeException e)
                {
                stopCapture();
                throw e;
                }
            }
        }

    @Override public void stopCapture() // throws NOTHING
        {
        try {
            synchronized (lock)
                {
                if (uvcCaptureSequence != null)
                    {
                    tracer.trace("stopCapture()", new Runnable()
                        {
                        @Override public void run()
                            {
                            uvcCaptureSequence.stopStreamingAndReportClosedIfNeeded();
                            uvcCaptureSequence.releaseRef(); // lakllj987987
                            uvcCaptureSequence = null;
                            }
                        });
                    }
                }
            }
        catch (RuntimeException e)
            {
            RobotLog.ee(getTag(), e, "unexpected exception in stopCapture(); ignoring");
            }
        }
    }
