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
package org.firstinspires.ftc.robotcore.internal.camera.libuvc.nativeobject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.firstinspires.ftc.robotcore.external.android.util.Size;
import org.firstinspires.ftc.robotcore.external.function.Continuation;
import org.firstinspires.ftc.robotcore.external.function.Supplier;
import org.firstinspires.ftc.robotcore.external.hardware.camera.Camera;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCaptureRequest;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCaptureSession;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraException;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.external.hardware.camera.controls.CameraControl;
import org.firstinspires.ftc.robotcore.internal.camera.CameraImpl;
import org.firstinspires.ftc.robotcore.internal.camera.CameraState;
import org.firstinspires.ftc.robotcore.internal.camera.RefCountedCamera;
import org.firstinspires.ftc.robotcore.internal.camera.RefCountedCameraHelper;
import org.firstinspires.ftc.robotcore.internal.camera.libuvc.api.UvcApiCameraCaptureRequest;
import org.firstinspires.ftc.robotcore.internal.camera.libuvc.api.UvcApiCaptureSession;
import org.firstinspires.ftc.robotcore.internal.camera.libuvc.api.UvcApiExposureControl;
import org.firstinspires.ftc.robotcore.internal.camera.libuvc.api.UvcApiFocusControl;
import org.firstinspires.ftc.robotcore.internal.camera.libuvc.constants.UvcAutoExposureMode;
import org.firstinspires.ftc.robotcore.internal.camera.libuvc.constants.UvcFrameFormat;
import org.firstinspires.ftc.robotcore.internal.system.Misc;
import org.firstinspires.ftc.robotcore.internal.system.NativeObject;
import org.firstinspires.ftc.robotcore.internal.system.Tracer;
import org.firstinspires.ftc.robotcore.internal.camera.calibration.CameraCalibration;
import org.firstinspires.ftc.robotcore.internal.camera.calibration.CameraCalibrationIdentity;
import org.firstinspires.ftc.robotcore.internal.camera.calibration.CameraCalibrationManager;
import org.firstinspires.ftc.robotcore.internal.vuforia.externalprovider.ExtendedExposureMode;
import org.firstinspires.ftc.robotcore.internal.vuforia.externalprovider.FocusMode;
import org.firstinspires.ftc.robotcore.internal.camera.calibration.VendorProductCalibrationIdentity;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link UvcDeviceHandle} is the manifestation of a uvc_device_handle_t
 */
@SuppressWarnings("WeakerAccess")
public class UvcDeviceHandle extends NativeObject<UvcDevice> implements RefCountedCamera
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = UvcDeviceHandle.class.getSimpleName();
    public String getTag() { return TAG; }
    public static boolean TRACE = true;
    protected Tracer tracer = Tracer.create(getTag(), TRACE);

    protected Camera.StateCallback  stateCallback = null;
    protected RefCountedCameraHelper refCountedCameraHelper;
    protected CameraCaptureSession  currentCaptureSession = null;
    protected int                   nextCaptureSessionId = 1;
    protected Camera                selfCamera;
    protected CameraState           selfState = CameraState.Nascent;
    protected boolean               reportClosedCalled = false;
    protected List<CameraControl>   cameraControls = new ArrayList<>();


    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public UvcDeviceHandle(long pointer, UvcDevice uvcDevice, Camera.StateCallback stateCallback)
        {
        super(pointer);
        setParent(uvcDevice);
        this.stateCallback = stateCallback;
        constructControls();
        refCountedCameraHelper = new RefCountedCameraHelper(this, lock, tracer, new Runnable()
            {
            @Override public void run()
                {
                shutdown();
                }
            });
        }

    protected void shutdown()
        {
        closeCaptureSession();
        reportSelfClosed();
        }

    @Override protected void destructor()
        {
        tracer.trace("destructor", new Runnable()
            {
            @Override public void run()
                {
                shutdown();
                if (pointer != 0)
                    {
                    nativeReleaseRefDeviceHandle(pointer);
                    clearPointer();
                    }
                UvcDeviceHandle.super.destructor();
                }
            });
        }

    @Override public Tracer getTracer()
        {
        return tracer;
        }

    @Override public void addRefExternal()
        {
        synchronized (lock)
            {
            refCountedCameraHelper.addRefExternal();;
            }
        }
    @Override public int releaseRefExternal()
        {
        synchronized (lock)
            {
            return refCountedCameraHelper.releaseRefExternal();
            }
        }
    @Override public String getExternalTraceIdentifier()
        {
        return "UvcDeviceHandle" + getTraceIdentifier();
        }

    public UvcContext getUvcContext()
        {
        return getParent().getUvcContext();
        }

    //----------------------------------------------------------------------------------------------
    // Metadata
    //----------------------------------------------------------------------------------------------

    public int getVendorId()
        {
        return getParent().getVendorId();
        }

    public int getProductId()
        {
        return getParent().getProductId();
        }

    //----------------------------------------------------------------------------------------------
    // CameraInternal interface
    //----------------------------------------------------------------------------------------------

    @Override public boolean hasCalibration(CameraCalibrationManager manager, Size size)
        {
        return manager.hasCalibration(getCalibrationIdentity(), size);
        }

    @Override public CameraCalibration getCalibration(CameraCalibrationManager manager, Size size)
        {
        return manager.getCalibration(getCalibrationIdentity(), size);
        }

    @Nullable @Override public CameraCalibrationIdentity getCalibrationIdentity()
        {
        return new VendorProductCalibrationIdentity(getVendorId(), getProductId());
        }

    //----------------------------------------------------------------------------------------------
    // CameraControls
    //----------------------------------------------------------------------------------------------

    void constructControls()
        {
        cameraControls.add(new UvcApiFocusControl(this));
        cameraControls.add(new UvcApiExposureControl(this));
        }

    @Override public @Nullable <T extends CameraControl> T getControl(Class<T> controlType)
        {
        synchronized (lock)
            {
            for (CameraControl cameraControl : cameraControls)
                {
                if (controlType.isInstance(cameraControl))
                    {
                    return controlType.cast(cameraControl);
                    }
                }
            }
        return null;
        }

    public FocusMode getVuforiaFocusMode()
        {
        synchronized (lock)
            {
            return FocusMode.from(nativeGetVuforiaFocusMode(pointer));
            }
        }

    public boolean setVuforiaFocusMode(FocusMode vuforia)
        {
        synchronized (lock)
            {
            return nativeSetVuforiaFocusMode(pointer, vuforia.ordinal());
            }
        }

    public boolean isVuforiaFocusModeSupported(FocusMode vuforia)
        {
        synchronized (lock)
            {
            return nativeIsVuforiaFocusModeSupported(pointer, vuforia.ordinal());
            }
        }

    public double getMinFocusLength()
        {
        synchronized (lock)
            {
            return nativeGetMinFocusLength(pointer);
            }
        }

    public double getMaxFocusLength()
        {
        synchronized (lock)
            {
            return nativeGetMaxFocusLength(pointer);
            }
        }

    public double getFocusLength()
        {
        synchronized (lock)
            {
            return nativeGetFocusLength(pointer);
            }
        }

    public boolean setFocusLength(double focusLength)
        {
        synchronized (lock)
            {
            return nativeSetFocusLength(pointer, focusLength);
            }
        }

    public boolean isFocusLengthSupported()
        {
        synchronized (lock)
            {
            return nativeIsFocusLengthSupported(pointer);
            }
        }

    //----------------------------------------------------------------------------------------------

    public int getVuforiaExposureMode()
        {
        synchronized (lock)
            {
            return nativeGetVuforiaExposureMode(pointer);
            }
        }

    public boolean setVuforiaExposureMode(ExtendedExposureMode vuforia)
        {
        synchronized (lock)
            {
            return nativeSetVuforiaExposureMode(pointer, vuforia.ordinal());
            }
        }

    public boolean isVuforiaExposureModeSupported(ExtendedExposureMode vuforia)
        {
        synchronized (lock)
            {
            return nativeIsVuforiaExposureModeSupported(pointer, vuforia.ordinal());
            }
        }

    public long getMinExposure()
        {
        synchronized (lock)
            {
            return nativeGetMinExposure(pointer);
            }
        }
    public long getMaxExposure()
        {
        synchronized (lock)
            {
            return nativeGetMaxExposure(pointer);
            }
        }
    public long getExposure()
        {
        synchronized (lock)
            {
            return nativeGetExposure(pointer);
            }
        }
    public boolean setExposure(long ns)
        {
        synchronized (lock)
            {
            return nativeSetExposure(pointer, ns);
            }
        }
    public boolean isExposureSupported()
        {
        synchronized (lock)
            {
            return nativeIsExposureSupported(pointer);
            }
        }

    //----------------------------------------------------------------------------------------------
    // Camera interface : state transitions
    //----------------------------------------------------------------------------------------------

    @Override @NonNull public WebcamName getCameraName()
        {
        return getParent().getWebcamName();
        }

    public final void openSelfAndReport() // idempotent
        {
        synchronized (lock)
            {
            switch (selfState)
                {
                case Nascent:
                    selfState = CameraState.OpenNotStarted;
                    createSelfCamera();
                    stateCallback.onOpened(selfCamera);
                    break;
                case OpenNotStarted:
                    // idempotent
                    break;
                default:
                    throw Misc.illegalStateException("openSelfCameraAndReport(): %s", selfState);
                }
            }
        }

    protected final void reportSelfClosed() // idempotent
        {
        synchronized (lock)
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
                    stateCallback.onClosed(selfCamera);
                    break;
                }
            }
        }

    protected void createSelfCamera()
        {
        /** This takes an additional reference on us. 'Will be passed to {@link Camera.StateCallback#onOpened(Camera)}
         * who then takes ownership and is responsible for closing, which releases same. */
        selfCamera = new CameraImpl(this);
        }

    protected void reportError(Camera.Error error)
        {
        synchronized (lock)
            {
            if (stateCallback != null)
                {
                stateCallback.onError(selfCamera, error);
                }
            }
        }

    //----------------------------------------------------------------------------------------------
    // Camera interface : capture requests
    //----------------------------------------------------------------------------------------------

    @Override public CameraCaptureRequest createCaptureRequest(int format, Size size, int fps) throws CameraException
        {
        return new UvcApiCameraCaptureRequest(this, format, size, fps);
        }

    //----------------------------------------------------------------------------------------------
    // Camera interface : capture sessions
    //----------------------------------------------------------------------------------------------

    @Override @NonNull public CameraCaptureSession createCaptureSession(Continuation<? extends CameraCaptureSession.StateCallback> continuation) throws CameraException
        {
        synchronized (lock)
            {
            closeCaptureSession();
            currentCaptureSession = new UvcApiCaptureSession(this, continuation, nextCaptureSessionId++);
            return currentCaptureSession;
            }
        }

    protected void closeCaptureSession()
        {
        CameraCaptureSession sessionToClose = null;
        synchronized (lock)
            {
            if (currentCaptureSession != null)
                {
                sessionToClose = currentCaptureSession; // paranoia about deadlocks
                currentCaptureSession = null;
                }
            }
        if (sessionToClose != null)
            {
            sessionToClose.close(); // idempotent
            }
        }

    public void onClosed(UvcApiCaptureSession closedSession)
        {
        // We thought to null out currentCaptureSession here, but that runs the risk of deadlocks,
        // just maybe. So we leave it alone. That may mean that we call currentCaptureSession.close()
        // more than once, but it's idempotent, so that's ok.
        }

    //----------------------------------------------------------------------------------------------
    // Accessing
    //----------------------------------------------------------------------------------------------

    public long getPointer()
        {
        return pointer;
        }

    public long getAddRefdPointer()
        {
        synchronized (lock)
            {
            if (pointer != 0)
                {
                nativeAddRefDeviceHandle(pointer);
                }
            return pointer;
            }
        }

    @Override public String toString()
        {
        return Misc.formatInvariant("%s(%s|%s)", this.getClass().getSimpleName(), getTraceIdentifier(), getParent().toString());
        }

    public String toStringVerbose()
        {
        return nativeGetDiagnostics(pointer);
        }

    public void setAutoExposure(final UvcAutoExposureMode autoExposure)
        {
        nativeSetAutoExposure(pointer, autoExposure.getValue());
        }

    public Camera getSelfCamera()
        {
        return selfCamera;
        }

    /**
     * @return null if the mode is not supported
     */
    public UvcStreamCtrl getStreamControl(final UvcFrameFormat uvcFrameFormat, final int width, final int height, final int fps)
        {
        synchronized (lock)
            {
            return tracer.trace(tracer.format("getStreamControl(%dx%d %d)", width, height, fps), new Supplier<UvcStreamCtrl>()
                {
                @Override public UvcStreamCtrl get()
                    {
                    if (uvcFrameFormat != null)
                        {
                        UvcStreamCtrl result = new UvcStreamCtrl(UvcDeviceHandle.this);
                        int rc = nativeGetStreamControlFormatSize(pointer, result.getPointer(), uvcFrameFormat.getValue(), width, height, fps);
                        if (rc == 0)
                            {
                            return result;
                            }
                        else
                            {
                            result.releaseRef();
                            return null;
                            }
                        }
                    else
                        return null;
                    }
                });
            }
        }

    public void stopAllStreaming()
        {
        synchronized (lock)
            {
            nativeStopAllStreaming(pointer);
            }
        }

    //----------------------------------------------------------------------------------------------
    // Native Methods
    //----------------------------------------------------------------------------------------------

    protected native static String nativeGetDiagnostics(long pointer);
    protected native static int nativeGetStreamControlFormatSize(long pointer, long pointerStreamControl, int uvcFrameFormat, int width, int height, int fps);
    protected native static void nativeSetAutoExposure(long pointer, byte mode);
    protected native static int nativeStopAllStreaming(long pointer);
    protected native static void nativeAddRefDeviceHandle(long pointer);
    protected native static void nativeReleaseRefDeviceHandle(long pointer);

    protected native static boolean nativeIsVuforiaFocusModeSupported(long pointer, int mode);
    protected native static int nativeGetVuforiaFocusMode(long pointer);
    protected native static boolean nativeSetVuforiaFocusMode(long pointer, int mode);

    protected native static double nativeGetMinFocusLength(long pointer);
    protected native static double nativeGetMaxFocusLength(long pointer);
    protected native static double nativeGetFocusLength(long pointer);
    protected native static boolean nativeSetFocusLength(long pointer, double focusLength);
    protected native static boolean nativeIsFocusLengthSupported(long pointer);

    protected native static boolean nativeIsVuforiaExposureModeSupported(long pointer, int mode);
    protected native static int nativeGetVuforiaExposureMode(long pointer);
    protected native static boolean nativeSetVuforiaExposureMode(long pointer, int mode);

    protected native static boolean nativeIsExposureSupported(long pointer);
    protected native static long nativeGetMinExposure(long pointer);
    protected native static long nativeGetMaxExposure(long pointer);
    protected native static long nativeGetExposure(long pointer);
    protected native static boolean nativeSetExposure(long pointer, long exposure);
    }
