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

import android.hardware.usb.UsbDevice;
import androidx.annotation.NonNull;

import com.qualcomm.robotcore.util.SerialNumber;

import org.firstinspires.ftc.robotcore.external.function.Consumer;
import org.firstinspires.ftc.robotcore.external.function.Continuation;
import org.firstinspires.ftc.robotcore.external.hardware.camera.Camera;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraName;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.internal.camera.CameraManagerImpl;
import org.firstinspires.ftc.robotcore.internal.camera.CameraManagerInternal;
import org.firstinspires.ftc.robotcore.internal.camera.CameraState;
import org.firstinspires.ftc.robotcore.internal.camera.RefCountedCamera;
import org.firstinspires.ftc.robotcore.internal.camera.libuvc.nativeobject.UvcDevice;
import org.firstinspires.ftc.robotcore.internal.camera.libuvc.nativeobject.UvcDeviceHandle;
import org.firstinspires.ftc.robotcore.internal.collections.MutableReference;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.system.Assert;
import org.firstinspires.ftc.robotcore.internal.system.Deadline;
import org.firstinspires.ftc.robotcore.internal.system.Tracer;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * {@link UsbResiliantWebcam} wraps a webcam and allows it to disconnect / reconnect from the USB
 * bus while hiding that from the client.
 */
@SuppressWarnings("WeakerAccess")
public class UsbResiliantWebcam extends DelegatingCamera implements RefCountedCamera
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "UsbResiliantWebcam";
    public String getTag() { return TAG; }

    protected final WebcamName              selfWebcamName;
    protected       String                  selfUsbDeviceName = null;
    protected final InterveningStateCallback interveningStateCallback;
    protected final UsbAttachmentMonitor    usbAttachmentMonitor;

    protected long                          reopenDuration;
    protected TimeUnit                      reopenTimeUnit;
    protected Semaphore                     usbAttachmentSemaphore = new Semaphore(1);

    protected UsbMonitoringState            usbMonitoringState = UsbMonitoringState.Unknown;

    protected enum UsbMonitoringState { Unknown, Connected, Disconnected }

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public UsbResiliantWebcam(
            CameraManagerInternal cameraManager,
            WebcamName webcamName,
            @NonNull final Continuation<? extends Camera.StateCallback> userContinuation)
        {
        super(cameraManager, webcamName, userContinuation);
        selfWebcamName = webcamName;
        this.interveningStateCallback = new InterveningStateCallback(userContinuation);
        this.usbAttachmentMonitor = new UsbAttachmentMonitor();
        cameraManager.registerReceiver(usbAttachmentMonitor);
        }

    @Override protected void destructor()
        {
        tracer.trace("destructor()", new Runnable()
            {
            @Override public void run()
                {
                cameraManager.unregisterReceiver(usbAttachmentMonitor);
                UsbResiliantWebcam.super.destructor();
                }
            });
        }

    //----------------------------------------------------------------------------------------------
    // CameraControls
    //----------------------------------------------------------------------------------------------

    @Override protected void constructControls()
        {
        delegatingCameraControls.add(new CachingFocusControl());
        delegatingCameraControls.add(new CachingExposureControl());
        }

    //----------------------------------------------------------------------------------------------
    // Opening and closing
    //----------------------------------------------------------------------------------------------

    /**
     * This is the callback we actually pass to the camera we open. It intervenes between that
     * camera and the user's callback. This it does to preserve the illusion that the camera
     * is open even as it might disconnect and (re)connect.
     */
     class InterveningStateCallback implements Camera.StateCallback
        {
        //------------------------------------------------------------------------------------------
        // State
        //------------------------------------------------------------------------------------------

        protected final Tracer tracer = Tracer.create(getTag() + ".InterveningStateCallback", TRACE);
        protected final Continuation<? extends Camera.StateCallback>  interveningContinuation;

        //------------------------------------------------------------------------------------------
        // Construction
        //------------------------------------------------------------------------------------------

        InterveningStateCallback(@NonNull final Continuation<? extends Camera.StateCallback> userContinuation)
            {
            interveningContinuation = Continuation.create(serialThreadPool, this);

            }

        //------------------------------------------------------------------------------------------
        // Camera.StateCallback
        //------------------------------------------------------------------------------------------

        @Override public void onOpened(final @NonNull Camera cameraOpened)
            {
            tracer.trace("onOpened() camera=" + cameraOpened, new Runnable()
                {
                @Override public void run()
                    {
                    // Either this is the successful first open attempt, or a successful reopen attempt. On
                    // the former, we need to report to the user; on the latter, we just need to record our
                    // internal state.
                    synchronized (outerLock)
                        {
                        // If we successfully open, we can assume connected. This helps if
                        // we happened to have missed any original connection notification
                        usbMonitoringState = UsbMonitoringState.Connected;
                        //
                        delegatedCameraState = CameraState.OpenNotStarted;
                        changeDelegatedCamera(cameraOpened);
                        openSelfAndReport();
                        }
                    }
                });
            }

        @Override public void onOpenFailed(@NonNull CameraName cameraName, @NonNull Camera.OpenFailure failureReason)
            {
            // Either this is a failed first open attempt, or a failed reopen attempt.
            synchronized (outerLock)
                {
                Assert.assertTrue(selfWebcamName.equals(cameraName));
                delegatedCameraState = CameraState.FailedOpen;
                changeDelegatedCamera(null);
                reportOpenFailed(failureReason);
                }
            }

        @Override public void onClosed(@NonNull Camera cameraClosedIgnored)
            {
            synchronized (outerLock)
                {
                if (selfState == CameraState.Closed)
                    {
                    /** Note: this may run *after* the dtor of {@link UsbResiliantWebcam} */
                    }
                else if (delegatedCameraState != CameraState.Disconnected)
                    {
                    tracer.traceError("unexpected closing internal camera W/O going through disconnection path");
                    reportSelfClosed(); // safety
                    }
                delegatedCameraState = CameraState.Closed;
                changeDelegatedCamera(null);
                }
            }

        @Override public void onError(@NonNull Camera camera, Camera.Error error)
            {
            synchronized (outerLock)
                {
                dispatchingCallback.onError(selfCamera, error);
                }
            }

        }

    //----------------------------------------------------------------------------------------------
    // Opening and closing
    //----------------------------------------------------------------------------------------------

    class UsbAttachmentMonitor implements CameraManagerInternal.UsbAttachmentCallback
        {
        @Override public void onDetached(UsbDevice usbDevice)
            {
            synchronized (outerLock)
                {
                if (usbDevice.getDeviceName().equals(selfUsbDeviceName))
                    {
                    tracer.trace("ACTION_USB_DEVICE_DETACHED: camera detached: scheduling disconnect: %s(%s)", selfWebcamName, selfUsbDeviceName);
                    usbMonitoringState = UsbMonitoringState.Disconnected;

                    /** We could close immediately. But we might also have an open in flight, and
                     * we need to close that guy too should it become successful. So we serialize
                     * using {@link #usbAttachmentSemaphore} */
                    openClosePool.execute(new Runnable()
                        {
                        @Override public void run()
                            {
                            tracer.trace("doingDisconnect()", new Runnable()
                                {
                                @Override public void run()
                                    {
                                    boolean releaseSemaphore = false;
                                    try {
                                        usbAttachmentSemaphore.acquire(); releaseSemaphore = true;
                                        synchronized (outerLock)
                                            {
                                            if (usbMonitoringState==UsbMonitoringState.Disconnected)
                                                {
                                                if (delegatedCamera != null)
                                                    {
                                                    // Set delegatedCameraState to avoid reporting self as closed when we see delegated camera close
                                                    delegatedCameraState = CameraState.Disconnected;
                                                    closeDelegatedCameras();
                                                    }
                                                }
                                            }
                                        }
                                    catch (InterruptedException e)
                                        {
                                        Thread.currentThread().interrupt();
                                        }
                                    finally
                                        {
                                        if (releaseSemaphore)
                                            {
                                            usbAttachmentSemaphore.release();
                                            }
                                        }
                                    }
                                });
                            }
                        });
                    }
                }
            }

        @Override public void onAttached(UsbDevice usbDevice, SerialNumber serialNumberJustAttached, MutableReference<Boolean> claimed)
            {
            synchronized (outerLock)
                {
                boolean reclaim = !claimed.getValue() && serialNumberJustAttached.matches(selfWebcamName.getSerialNumber());
                tracer.trace("webcam=%s serialNumberJustAttached=%s reclaim=%s", selfWebcamName.getSerialNumber(), serialNumberJustAttached, reclaim);

                if (reclaim)
                    {
                    tracer.trace("ACTION_USB_DEVICE_ATTACHED: camera attached: scheduling reopen: %s", selfWebcamName);
                    claimed.setValue(true);
                    usbMonitoringState = UsbMonitoringState.Connected;
                    openClosePool.execute(new Runnable() // NOT the serial pool: we'd block too long, and we don
                        {
                        @Override public void run()
                            {
                            tracer.trace("doingReopen()", new Runnable()
                                {
                                @Override public void run()
                                    {
                                    boolean releaseSemaphore = false;
                                    try {
                                        usbAttachmentSemaphore.acquire(); releaseSemaphore = true;
                                        synchronized (outerLock)
                                            {
                                            if (usbMonitoringState==UsbMonitoringState.Connected)
                                                {
                                                if (delegatedCamera == null)
                                                    {
                                                    releaseSemaphore = false;
                                                    asyncRequestPermissionAndOpenCamera(new Runnable()
                                                        {
                                                        @Override public void run()
                                                            {
                                                            tracer.trace("doingReopen(): complete");
                                                            usbAttachmentSemaphore.release();
                                                            }
                                                        });
                                                    }
                                                }
                                            }
                                        }
                                    catch (InterruptedException e)
                                        {
                                        Thread.currentThread().interrupt();
                                        }
                                    finally
                                        {
                                        if (releaseSemaphore)
                                            {
                                            usbAttachmentSemaphore.release();
                                            }
                                        }
                                    }
                                });
                            }
                        });
                    }
                }
            }
        };

    /**
     * Called by {@link CameraManagerImpl}
     */
    public void openAssumingPermission(long reopenDuration, TimeUnit reopenTimeUnit)
        {
        synchronized (outerLock)
            {
            this.reopenDuration = reopenDuration;
            this.reopenTimeUnit = reopenTimeUnit;
            openAssumingPermission();
            }
        }

    protected void openAssumingPermission() // this call happens to be synchronous. huzzah
        {
        tracer.trace("openAssumingPermission()", new Runnable()
            {
            @Override public void run()
                {
                synchronized (outerLock)
                    {
                    switch (selfState)
                        {
                        case FailedOpen:
                            // We reported to the user that the open failed; that's a dead-end state
                            // that we can't get out of.
                            break;
                        case Closed:
                            // Can't come back from the dead
                            break;

                        case Nascent:
                        // We are *resiliant*, so we need the next three too!
                        case OpenNotStarted:
                        case OpenAndStarted:
                        case Disconnected:
                            Assert.assertNull(delegatedCamera);
                            boolean openOrFailedNotified = false;
                            Camera.OpenFailure failureReason = Camera.OpenFailure.InternalError;
                            try {
                                UvcDevice uvcDevice = cameraManager.findUvcDevice(selfWebcamName); // kkl3900773;k
                                if (uvcDevice != null)
                                    {
                                    try {
                                        // Remember his USB name while we can so we can compare correctly on detachment: can't compare by serial numbers then, too late
                                        selfUsbDeviceName = selfWebcamName.getUsbDeviceNameIfAttached();
                                        UvcDeviceHandle uvcDeviceHandle = uvcDevice.open(selfWebcamName, interveningStateCallback); //xyzkelkjear
                                        try {
                                            openOrFailedNotified = true;
                                            }
                                        finally
                                            {
                                            /** On successful open, the callback added a ref. See {@link UvcDeviceHandle#createSelfCamera()}  */
                                            if (uvcDeviceHandle != null)
                                                {
                                                uvcDeviceHandle.releaseRef(); //xyzkelkjear
                                                }
                                            }
                                        }
                                    finally
                                        {
                                        uvcDevice.releaseRef(); // kkl3900773;k
                                        }
                                    }
                                else
                                    {
                                    tracer.trace("can't find uvcDevice");
                                    failureReason = Camera.OpenFailure.Disconnected;
                                    }
                                }
                            finally
                                {
                                if (!openOrFailedNotified)
                                    {
                                    interveningStateCallback.onOpenFailed(selfCameraName, failureReason);
                                    }
                                }
                            break;
                        default:
                            throw AppUtil.getInstance().unreachable(TAG);
                        }
                    }
                }
            });
        }

    // Careful about calling this on a thread we can't block for a long, long time
    protected void asyncRequestPermissionAndOpenCamera(final Runnable runOnCompletion)
        {
        tracer.trace("asyncRequestPermissionAndOpenCamera()", new Runnable()
            {
            @Override public void run()
                {
                synchronized (outerLock)
                    {
                    Assert.assertNull(delegatedCamera);
                    selfWebcamName.asyncRequestCameraPermission(context, new Deadline(reopenDuration, reopenTimeUnit),
                                                                Continuation.create(serialThreadPool, new Consumer<Boolean>()
                        {
                        @Override public void accept(Boolean permissionGranted)
                            {
                            if (permissionGranted)
                                {
                                openAssumingPermission(); // synchronous
                                }
                            else
                                {
                                tracer.traceError("permission declined for cameara: %s", selfWebcamName);
                                }
                            if (runOnCompletion != null)
                                {
                                runOnCompletion.run();
                                }
                            }
                        }));
                    }
                }
            });
        }
    }

