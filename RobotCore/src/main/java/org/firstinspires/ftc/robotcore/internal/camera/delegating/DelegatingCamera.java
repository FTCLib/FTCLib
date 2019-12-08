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

import android.content.Context;
import android.hardware.usb.UsbManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.qualcomm.robotcore.util.ThreadPool;

import org.firstinspires.ftc.robotcore.external.android.util.Size;
import org.firstinspires.ftc.robotcore.external.function.Continuation;
import org.firstinspires.ftc.robotcore.external.function.ContinuationResult;
import org.firstinspires.ftc.robotcore.external.function.ThrowingSupplier;
import org.firstinspires.ftc.robotcore.external.hardware.camera.Camera;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCaptureRequest;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCaptureSession;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraException;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraName;
import org.firstinspires.ftc.robotcore.external.hardware.camera.controls.CameraControl;
import org.firstinspires.ftc.robotcore.internal.camera.CameraImpl;
import org.firstinspires.ftc.robotcore.internal.camera.CameraInternal;
import org.firstinspires.ftc.robotcore.internal.camera.CameraManagerInternal;
import org.firstinspires.ftc.robotcore.internal.camera.CameraState;
import org.firstinspires.ftc.robotcore.internal.camera.RefCountedCamera;
import org.firstinspires.ftc.robotcore.internal.camera.RefCountedCameraHelper;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.system.Assert;
import org.firstinspires.ftc.robotcore.internal.system.RefCounted;
import org.firstinspires.ftc.robotcore.internal.system.Tracer;
import org.firstinspires.ftc.robotcore.internal.camera.calibration.CameraCalibration;
import org.firstinspires.ftc.robotcore.internal.camera.calibration.CameraCalibrationIdentity;
import org.firstinspires.ftc.robotcore.internal.camera.calibration.CameraCalibrationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import static org.firstinspires.ftc.robotcore.internal.camera.CameraImpl.addRefCamera;
import static org.firstinspires.ftc.robotcore.internal.camera.CameraImpl.releaseRefCamera;

/**
 * A {@link Camera} that delegates to another camera. Said camera can be dynamically changed
 */
@SuppressWarnings("WeakerAccess")
public abstract class DelegatingCamera extends RefCounted implements RefCountedCamera
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "DelegatingCamera";
    public String getTag() { return TAG; }
    public static boolean TRACE = true;
    protected Tracer tracer = Tracer.create(getTag(), TRACE);

    protected final Object                  outerLock = lock; // a renaming to avoid confusion
    protected final Context                 context;
    protected final CameraManagerInternal   cameraManager;
    protected final UsbManager              usbManager;
    protected final Executor                openClosePool = ThreadPool.newSingleThreadExecutor(TAG);
    protected final Executor                serialThreadPool;
    protected final RefCountedCameraHelper  refCountedCameraHelper;
    protected final DispatchingCallback     dispatchingCallback;
    protected final CameraName              selfCameraName;

    protected int                           nextCaptureSessionId = 1;
    protected DelegatingCaptureSession      delegatingCaptureSession = null;
    protected Camera                        delegatedCamera = null;
    protected CameraState                   delegatedCameraState = CameraState.Nascent;
    protected List<CameraControl>           delegatingCameraControls = new ArrayList<>();

    protected Camera                        selfCamera = null;
    protected CameraState                   selfState = CameraState.Nascent;

    @Override public String toString()
        {
        return getTag() + "(" + selfCameraName + ")";
        }

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    protected DelegatingCamera(
            CameraManagerInternal cameraManager,
            CameraName selfCameraName,
            @NonNull final Continuation<? extends Camera.StateCallback> userContinuation)
        {
        this.context = AppUtil.getDefContext();
        this.usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        this.cameraManager = cameraManager;
        this.serialThreadPool = cameraManager.getSerialThreadPool();
        this.selfCameraName = selfCameraName;
        this.refCountedCameraHelper = new RefCountedCameraHelper(this, outerLock, tracer, new Runnable()
            {
            @Override public void run()
                {
                shutdown();
                }
            });
        this.dispatchingCallback = new DispatchingCallback(userContinuation);
        constructControls();
        }

    protected void shutdown()
        {
        closeCaptureSession();
        reportSelfClosed();
        closeDelegatedCameras(); // here? or only in dtor? here seems right, for consistency
        }

    @Override protected void destructor()
        {
        shutdown();
        super.destructor();
        }

    @Override public Tracer getTracer()
        {
        return tracer;
        }

    @Override public void addRefExternal()
        {
        synchronized (outerLock)
            {
            refCountedCameraHelper.addRefExternal();
            }
        }
    @Override public int releaseRefExternal()
        {
        synchronized (outerLock)
            {
            return refCountedCameraHelper.releaseRefExternal();
            }
        }
    @Override public String getExternalTraceIdentifier()
        {
        return getTag() + getTraceIdentifier();
        }

    //----------------------------------------------------------------------------------------------
    // Reporting
    //----------------------------------------------------------------------------------------------

    public final void openSelfAndReport() // idempotent
        {
        tracer.trace("openSelfAndReport()", new Runnable()
            {
            @Override public void run()
                {
                synchronized (outerLock)
                    {
                    switch (selfState)
                        {
                        case Nascent:
                            selfState = CameraState.OpenNotStarted;
                            createSelfCamera();
                            dispatchingCallback.onOpened(selfCamera);
                            break;
                        case OpenNotStarted:
                            // idempotent
                            break;
                        default:
                            // Shouldn't have opened self from any other state
                            throw new InternalError("openSelfAndReport");
                        }
                    }
                }
            });
        }


    /** a separate method so subclasses can override */
    protected void createSelfCamera()
        {
        selfCamera = new CameraImpl(DelegatingCamera.this);
        }

    public final void reportOpenFailed(Camera.OpenFailure failureReason) // idempotent
        {
        synchronized (outerLock)
            {
            switch (selfState)
                {
                case Nascent:
                    selfState = CameraState.FailedOpen;
                    dispatchingCallback.onOpenFailed(selfCameraName, failureReason);
                    break;
                case FailedOpen:
                    // idempotent
                    break;
                default:
                    throw new InternalError("reportOpenFailed");
                }
            }
        }

    public final void reportSelfClosed() // idempotent
        {
        synchronized (outerLock)
            {
            switch (selfState)
                {
                case Closed:
                    // idempotent
                    break;
                case FailedOpen:
                    // Only get close notifications if you successfully opened
                    break;
                default:
                    selfState = CameraState.Closed;
                    dispatchingCallback.onClosed(selfCamera);
                    break;
                }
            }
        }

    public final void reportError(Camera.Error error)
        {
        Assert.assertNotNull(selfCamera);
        dispatchingCallback.onError(selfCamera, error);
        }

    //----------------------------------------------------------------------------------------------
    // Active Camera
    //----------------------------------------------------------------------------------------------

    /** Default is to close the one delegatedCamera. Subclases may have grander ideas */
    protected void closeDelegatedCameras()
        {
        synchronized (outerLock)
            {
            if (delegatedCamera != null)
                {
                delegatedCamera.close(); // callbacks come later!
                changeDelegatedCamera(null); // was just assigning null; more testing needed on this change?
                }
            }
        }

    protected final void changeDelegatedCamera(final @Nullable Camera newCamera)
        {
        synchronized (outerLock)
            {
            if (delegatedCamera != newCamera)
                {
                tracer.trace(tracer.format("changeDelegatedCamera(%s->%s)", delegatedCamera, newCamera), new Runnable()
                    {
                    @Override public void run()
                        {
                        delegatedCamera = newCamera;
                        updateCameraHolders();
                        }
                    });
                }
            }
        }

    protected final void updateCameraHolders()
        {
        synchronized (outerLock)
            {
            if (delegatingCaptureSession != null)
                {
                delegatingCaptureSession.onCameraChanged(delegatedCamera);
                }
            for (CameraControl cameraControl : delegatingCameraControls)
                {
                if (cameraControl instanceof DelegatingCameraControl)
                    {
                    ((DelegatingCameraControl)cameraControl).onCameraChanged(delegatedCamera);
                    }
                }
            }
        }

    //----------------------------------------------------------------------------------------------
    // CameraControls
    //----------------------------------------------------------------------------------------------

    protected abstract void constructControls();

    @Override public @Nullable <T extends CameraControl> T getControl(Class<T> controlType)
        {
        synchronized (outerLock)
            {
            for (CameraControl cameraControl : delegatingCameraControls)
                {
                if (controlType.isInstance(cameraControl))
                    {
                    return controlType.cast(cameraControl);
                    }
                }
            return null;
            }
        }

    //----------------------------------------------------------------------------------------------
    // RefCountedCamera
    //----------------------------------------------------------------------------------------------

    @NonNull @Override public CameraName getCameraName()
        {
        return selfCameraName;
        }

    @Override public CameraCaptureRequest createCaptureRequest(int format, Size size, int fps) throws CameraException
        {
        return new DelegatingCaptureRequest(this, format, size, fps);
        }

    @NonNull @Override public CameraCaptureSession createCaptureSession(final Continuation<? extends CameraCaptureSession.StateCallback> userContinuation) throws CameraException
        {
        return tracer.trace("createCaptureSession()", new ThrowingSupplier<CameraCaptureSession, CameraException>()
            {
            @Override public CameraCaptureSession get() throws CameraException
                {
                synchronized (outerLock)
                    {
                    closeCaptureSession();
                    delegatingCaptureSession = new DelegatingCaptureSession(DelegatingCamera.this, userContinuation, nextCaptureSessionId++);
                    updateCameraHolders();
                    return delegatingCaptureSession;
                    }
                }
            });
        }

    protected void closeCaptureSession()
        {
        CameraCaptureSession sessionToClose = null;
        synchronized (outerLock)
            {
            if (delegatingCaptureSession != null)
                {
                sessionToClose = delegatingCaptureSession; // paranoia about deadlocks
                delegatingCaptureSession = null;
                }
            }
        if (sessionToClose != null)
            {
            sessionToClose.close(); // idempotent
            }
        }

    public void onClosed(DelegatingCaptureSession closedSession)
        {
        // We thought to null out currentCaptureSession here, but that runs the risk of deadlocks,
        // just maybe. So we leave it alone. That may mean that we call currentCaptureSession.close()
        // more than once, but it's idempotent, so that's ok.
        }

    //----------------------------------------------------------------------------------------------
    // CameraInternal
    //----------------------------------------------------------------------------------------------

    @Override public boolean hasCalibration(CameraCalibrationManager manager, Size size)
        {
        synchronized (lock)
            {
            if (delegatedCamera != null && delegatedCamera instanceof CameraInternal)
                {
                return ((CameraInternal) delegatedCamera).hasCalibration(manager, size);
                }
            return false;
            }
        }

    @Override public CameraCalibration getCalibration(CameraCalibrationManager manager, Size size)
        {
        synchronized (lock)
            {
            if (delegatedCamera != null && delegatedCamera instanceof CameraInternal)
                {
                return ((CameraInternal) delegatedCamera).getCalibration(manager, size);
                }
            return null;
            }
        }

    @Override public CameraCalibrationIdentity getCalibrationIdentity()
        {
        synchronized (lock)
            {
            if (delegatedCamera != null && delegatedCamera instanceof CameraInternal)
                {
                return ((CameraInternal) delegatedCamera).getCalibrationIdentity();
                }
            return null;
            }
        }

    //----------------------------------------------------------------------------------------------
    // Types
    //----------------------------------------------------------------------------------------------

    /**
     * Calls client continuation where the wants to run
     */
    protected class DispatchingCallback implements Camera.StateCallback
        {
        public @NonNull final Continuation<? extends Camera.StateCallback> userContinuation;

        public DispatchingCallback(@NonNull final Continuation<? extends Camera.StateCallback> userContinuation)
            {
            this.userContinuation = userContinuation;
            }

        @Override public void onOpened(@NonNull final Camera camera)
            {
            addRefCamera(camera);
            userContinuation.dispatch(new ContinuationResult<Camera.StateCallback>()
                {
                @Override public void handle(Camera.StateCallback stateCallback)
                    {
                    stateCallback.onOpened(camera);
                    releaseRefCamera(camera);
                    }
                });
            }
        @Override public void onOpenFailed(@NonNull final CameraName cameraName, @NonNull final Camera.OpenFailure failureReason)
            {
            userContinuation.dispatch(new ContinuationResult<Camera.StateCallback>()
                {
                @Override public void handle(Camera.StateCallback stateCallback)
                    {
                    stateCallback.onOpenFailed(cameraName, failureReason);
                    }
                });
            }
        @Override public void onClosed(@NonNull final Camera camera)
            {
            addRefCamera(camera);
            userContinuation.dispatch(new ContinuationResult<Camera.StateCallback>()
                {
                @Override public void handle(Camera.StateCallback stateCallback)
                    {
                    stateCallback.onClosed(camera);
                    releaseRefCamera(camera);
                    }
                });
            }
        @Override public void onError(@NonNull final Camera camera, final Camera.Error error)
            {
            addRefCamera(camera);
            userContinuation.dispatch(new ContinuationResult<Camera.StateCallback>()
                {
                @Override public void handle(Camera.StateCallback stateCallback)
                    {
                    stateCallback.onError(camera, error);
                    releaseRefCamera(camera);
                    }
                });
            }
        }


    }
