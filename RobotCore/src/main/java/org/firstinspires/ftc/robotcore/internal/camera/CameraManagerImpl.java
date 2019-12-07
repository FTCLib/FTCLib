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
package org.firstinspires.ftc.robotcore.internal.camera;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.qualcomm.robotcore.eventloop.SyncdDevice;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.SerialNumber;
import com.qualcomm.robotcore.util.ThreadPool;
import org.firstinspires.ftc.robotcore.internal.usb.VendorProductSerialNumber;

import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.function.Consumer;
import org.firstinspires.ftc.robotcore.external.function.Continuation;
import org.firstinspires.ftc.robotcore.external.function.ContinuationResult;
import org.firstinspires.ftc.robotcore.external.function.Function;
import org.firstinspires.ftc.robotcore.external.function.Supplier;
import org.firstinspires.ftc.robotcore.external.hardware.camera.BuiltinCameraName;
import org.firstinspires.ftc.robotcore.external.hardware.camera.Camera;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraManager;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraName;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.internal.camera.delegating.RefCountedSwitchableCameraImpl;
import org.firstinspires.ftc.robotcore.internal.camera.delegating.SwitchableCameraName;
import org.firstinspires.ftc.robotcore.internal.camera.delegating.SwitchableCameraNameImpl;
import org.firstinspires.ftc.robotcore.internal.camera.delegating.UsbResiliantWebcam;
import org.firstinspires.ftc.robotcore.internal.camera.libuvc.nativeobject.LibUsbDevice;
import org.firstinspires.ftc.robotcore.internal.camera.libuvc.nativeobject.UvcContext;
import org.firstinspires.ftc.robotcore.internal.camera.libuvc.nativeobject.UvcDevice;
import org.firstinspires.ftc.robotcore.internal.camera.names.BuiltinCameraNameImpl;
import org.firstinspires.ftc.robotcore.internal.camera.names.UnknownCameraNameImpl;
import org.firstinspires.ftc.robotcore.internal.camera.names.WebcamNameImpl;
import org.firstinspires.ftc.robotcore.internal.collections.MutableReference;
import org.firstinspires.ftc.robotcore.internal.hardware.usb.ArmableUsbDevice;
import org.firstinspires.ftc.robotcore.internal.network.CallbackLooper;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.system.ContinuationSynchronizer;
import org.firstinspires.ftc.robotcore.internal.system.Deadline;
import org.firstinspires.ftc.robotcore.internal.system.DestructOnFinalize;
import org.firstinspires.ftc.robotcore.internal.system.Misc;
import org.firstinspires.ftc.robotcore.internal.system.Tracer;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.firstinspires.ftc.robotcore.internal.camera.CameraImpl.addRefCamera;
import static org.firstinspires.ftc.robotcore.internal.camera.CameraImpl.releaseRefCamera;

/**
 * {@link CameraManagerImpl} is the main (only?) implementation of {@link CameraManager}
 *
 * @see ClassFactory#getCameraManager()
 */
@SuppressWarnings("WeakerAccess")
public class CameraManagerImpl extends DestructOnFinalize/*no parent*/ implements CameraManager, CameraManagerInternal, AppUtil.UsbFileSystemRootListener
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "CameraManager";
    public String getTag() { return TAG; }
    public static boolean TRACE = true;
    protected Tracer tracer = Tracer.create(TAG, TRACE);
    protected static final AtomicInteger instanceCounter = new AtomicInteger(0);

    protected final Executor serialThreadPool = ThreadPool.newSingleThreadExecutor(TAG); // single threaded so callbacks don't ge re-ordered
    protected final int instanceNumber = instanceCounter.getAndIncrement();
    protected UvcContext uvcContext = null;
    protected UsbManager usbManager = (UsbManager) AppUtil.getDefContext().getSystemService(Context.USB_SERVICE);
    protected final UsbAttachmentMonitor usbAttachmentMonitor;
    protected final List<CameraManagerInternal.UsbAttachmentCallback> callbacks = new ArrayList<>();

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    /** logically private */
    public CameraManagerImpl()
        {
        super(TraceLevel.None); // we'll trace ctor, thank you
        traceLevel = defaultTraceLevel;
        AppUtil.getInstance().addUsbfsListener(this);
        if (traceCtor()) RobotLog.vv(getTag(), "construct(%s)", getTraceIdentifier());
        getOrMakeUvcContext();

        this.usbAttachmentMonitor = new UsbAttachmentMonitor();
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        AppUtil.getDefContext().registerReceiver(usbAttachmentMonitor, filter, null, CallbackLooper.getDefault().getHandler());
        }

    @Override protected void destructor()
        {
        // Note: we probably would get by with extending from RefCounted instead of CloseOnFinalize,
        // since we don't ourselves need to call any native apis here, but making that change will
        // require some good testing.
        uvcContext.releaseRef(); // KSLDFLAK
        AppUtil.getInstance().removeUsbfsListener(this);
        AppUtil.getDefContext().getApplicationContext().unregisterReceiver(usbAttachmentMonitor);
        super.destructor();
        }

    @Override public String getTraceIdentifier()
        {
        return super.getTraceIdentifier() + Misc.formatInvariant("|inst#=%d", instanceNumber);
        }

    //----------------------------------------------------------------------------------------------
    // Usb
    //----------------------------------------------------------------------------------------------

    @Override public void registerReceiver(UsbAttachmentCallback receiver)
        {
        synchronized (callbacks)
            {
            if (!callbacks.contains(receiver))
                {
                callbacks.add(receiver);
                }
            }
        }

    @Override public void unregisterReceiver(UsbAttachmentCallback receiver)
        {
        synchronized (callbacks)
            {
            callbacks.remove(receiver);
            }
        }

    /** a main purpose we have here is avoiding races on attachment */
    class UsbAttachmentMonitor extends BroadcastReceiver
        {
        @Override public void onReceive(Context context, Intent intent)
            {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action))
                {
                tracer.trace("---------------------------------------------- ACTION_USB_DEVICE_DETACHED");
                UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                synchronized (callbacks)
                    {
                    for (UsbAttachmentCallback callback : callbacks)
                        {
                        callback.onDetached(usbDevice);
                        }
                    }
                }
            else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action))
                {
                tracer.trace("---------------------------------------------- ACTION_USB_DEVICE_ATTACHED");
                UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                synchronized (callbacks)
                    {
                    SerialNumber serialNumber = getRealOrVendorProductSerialNumber(usbDevice);
                    if (serialNumber != null)
                        {
                        MutableReference<Boolean> claimed = new MutableReference<>(false);
                        for (UsbAttachmentCallback callback : callbacks)
                            {
                            callback.onAttached(usbDevice, serialNumber, claimed);
                            }
                        }
                    else
                        tracer.traceError("unable to determine serial number of %s; ignoring", usbDevice.getDeviceName());
                    }
                }
            }
        }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    /** Keep trying to find the usbfs if we haven't already */
    protected UvcContext getOrMakeUvcContext()
        {
        synchronized (lock)
            {
            if (uvcContext == null)
                {
                uvcContext = new UvcContext(this, AppUtil.getInstance().getUsbFileSystemRoot()); // KSLDFLAK
                }
            else if (uvcContext.getUsbFileSystemRoot()==null)
                {
                String root = AppUtil.getInstance().getUsbFileSystemRoot();
                if (root != null)
                    {
                    uvcContext.releaseRef(); // KSLDFLAK
                    uvcContext = new UvcContext(this, root); // KSLDFLAK
                    }
                }
            return uvcContext;
            }
        }

    @Override public void onUsbFileSystemRootChanged(String usbFileSystemRoot)
        {
        if (usbFileSystemRoot != null) // paranoia
            {
            RobotLog.ii(TAG, "found USB file system root: %s", usbFileSystemRoot);
            getOrMakeUvcContext();
            }
        }

    //----------------------------------------------------------------------------------------------
    // Camera Names
    //----------------------------------------------------------------------------------------------

    @Override public boolean isWebcamAttached(@NonNull SerialNumber serialNumberPattern)
        {
        return WebcamNameImpl.isAttached(this, serialNumberPattern);
        }


    @Override public WebcamName webcamNameFromSerialNumber(@NonNull SerialNumber serialNumber, @NonNull ArmableUsbDevice.OpenRobotUsbDevice opener, @NonNull SyncdDevice.Manager manager)
        {
        return WebcamNameImpl.forSerialNumber(serialNumber, opener, manager);
        }

    @Override public @Nullable SerialNumber getRealOrVendorProductSerialNumber(UsbDevice usbDevice)
        {
        return getOrMakeUvcContext().getRealOrVendorProductSerialNumber(usbDevice);
        }

    @Override @NonNull public List<LibUsbDevice> getMatchingLibUsbDevices(Function<SerialNumber, Boolean> matcher)
        {
        if (CameraManagerInternal.avoidKitKatLegacyPaths || Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            {
            List<LibUsbDevice> result = new ArrayList<>();
            for (UsbDevice usbDevice : usbManager.getDeviceList().values())
                {
                SerialNumber candidate = getRealOrVendorProductSerialNumber(usbDevice);
                if (candidate != null && matcher.apply(candidate))
                    {
                    LibUsbDevice libUsbDevice = getOrMakeUvcContext().getLibUsbDeviceFromUsbDeviceName(usbDevice.getDeviceName(), true);
                    result.add(libUsbDevice);
                    }
                }
            return result;
            }
        else
            {
            return getOrMakeUvcContext().getMatchingLibUsbDevicesKitKat(matcher);
            }
        }

    @Override public void enumerateAttachedSerialNumbers(Consumer<SerialNumber> consumer)
        {
        if (CameraManagerInternal.avoidKitKatLegacyPaths || Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            {
            for (UsbDevice usbDevice : usbManager.getDeviceList().values())
                {
                SerialNumber candidate = getRealOrVendorProductSerialNumber(usbDevice);
                if (candidate != null)
                    {
                    consumer.accept(candidate);
                    }
                }
            }
        else
            {
            getOrMakeUvcContext().enumerateAttachedSerialNumbersKitKat(consumer);
            }
        }

    @Override public WebcamName webcamNameFromDevice(UsbDevice usbDevice)
        {
        SerialNumber serialNumber = getRealOrVendorProductSerialNumber(usbDevice);
        if (serialNumber != null)
            {
            return WebcamNameImpl.forSerialNumber(serialNumber);
            }
        tracer.traceError("unable to determine webcamName for %s", usbDevice.getDeviceName());
        return null;  // device might have detached, for example
        }

    @Override public WebcamName webcamNameFromDevice(LibUsbDevice libUsbDevice)
        {
        return WebcamNameImpl.forSerialNumber(libUsbDevice.getRealOrVendorProductSerialNumber());
        }

    @Override public BuiltinCameraName nameFromCameraDirection(VuforiaLocalizer.CameraDirection cameraDirection)
        {
        return BuiltinCameraNameImpl.forCameraDirection(cameraDirection);
        }

    @Override public CameraName nameForUnknownCamera()
        {
        return UnknownCameraNameImpl.forUnknown();
        }

    @Override public SwitchableCameraName nameForSwitchableCamera(CameraName... cameraNames)
        {
        return SwitchableCameraNameImpl.forSwitchable(cameraNames);
        }

    //----------------------------------------------------------------------------------------------
    // CameraManager
    //----------------------------------------------------------------------------------------------

    @Override public UsbManager getUsbManager()
        {
        return usbManager;
        }

    /** Not actually publicly exposed, since for the moment we wouldn't be able to successfully
     * open any {@link CameraName} that was not a webcam */
    public List<CameraName> getAllCameras()
        {
        List<CameraName> result = new ArrayList<>();

        for (WebcamName webcamName : new HashSet<>(getAllWebcams()))
            {
            result.add(webcamName);
            }

        // Add any builtin cameras too
        int cBuiltin = android.hardware.Camera.getNumberOfCameras();
        for (int iBuiltin = 0; iBuiltin < cBuiltin; iBuiltin++)
            {
            android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
            android.hardware.Camera.getCameraInfo(iBuiltin, info);
            switch (info.facing)
                {
                case android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK:
                    result.add(nameFromCameraDirection(VuforiaLocalizer.CameraDirection.BACK));
                    break;
                case android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT:
                    result.add(nameFromCameraDirection(VuforiaLocalizer.CameraDirection.FRONT));
                    break;
                }
            }

        return result;
        }

    /** Returns a list of all the currently attached webcams */
    @Override public List<WebcamName> getAllWebcams()
        {
        List<WebcamName> result = new ArrayList<>();
        for (UvcDevice device : getOrMakeUvcContext().getDeviceList())
            {
            WebcamName webcamName = device.getWebcamName();
            result.add(webcamName);
            device.releaseRef();
            }
        return result;
        }

    //----------------------------------------------------------------------------------------------
    // Opening cameras
    //----------------------------------------------------------------------------------------------

    @Override public void asyncOpenCameraAssumingPermission(@NonNull final CameraName cameraName,
            @NonNull final Continuation<? extends Camera.StateCallback> userContinuation,
            final long reopenDuration, final TimeUnit reopenTimeUnit)
        {
        if (userContinuation == null)
            {
            throw Misc.illegalArgumentException("Camera.StateCallback continuation must not be null");
            }

        // callback which will run wherever the continuation provided runs, does a little bit
        // of bookkeeping then forwards the notifications on to the caller.
        Camera.StateCallback tracingCallback = new Camera.StateCallbackDefault()
            {
            @Override public void onOpened(@NonNull final Camera camera)
                {
                tracer.trace("camera reports opened: %s", camera);
                userContinuation.getTarget().onOpened(camera);
                }
            @Override public void onOpenFailed(@NonNull CameraName cameraName, @NonNull Camera.OpenFailure failureReason)
                {
                tracer.trace("camera reports failed to open: %s:", cameraName);
                userContinuation.getTarget().onOpenFailed(cameraName, failureReason);
                }
            @Override public void onClosed(@NonNull Camera camera)
                {
                tracer.trace("camera reports closed: %s", camera);
                userContinuation.getTarget().onClosed(camera);
                }
            @Override public void onError(@NonNull Camera camera, Camera.Error error)
                {
                tracer.traceError("camera reports error: %s: %s", camera, error);
                userContinuation.getTarget().onError(camera, error);
                }
            };

        // run where the caller runs
        final Continuation<Camera.StateCallback> tracingContinuation = userContinuation.createForNewTarget(tracingCallback); // we'll run where they run

        tracer.trace(tracer.format("asyncOpenCamera(%s)", cameraName), new Runnable()
            {
            @Override public void run()
                {
                if (cameraName.isWebcam())
                    {
                    asyncOpenWebcamAssumingPermission((WebcamName) cameraName, tracingContinuation, reopenDuration, reopenTimeUnit);
                    }
                else if (cameraName instanceof SwitchableCameraName)
                    {
                    asyncOpenSwitchableAssumingPermission((SwitchableCameraName)cameraName, tracingContinuation, reopenDuration, reopenTimeUnit);
                    }
                else
                    {
                    tracer.traceError("asyncOpenCamera(): %s is not a kind of camera we can open", cameraName);
                    tracingContinuation.dispatch(new ContinuationResult<Camera.StateCallback>()
                        {
                        @Override public void handle(Camera.StateCallback callback)
                            {
                            callback.onOpenFailed(cameraName, Camera.OpenFailure.CameraTypeNotSupported);
                            }
                        });
                    }
                }
            });
        }

    protected void asyncOpenSwitchableAssumingPermission(final SwitchableCameraName switchableCameraName,
            @NonNull final Continuation<? extends Camera.StateCallback> userContinuation,
            final long reopenDuration, final TimeUnit reopenTimeUnit)
        {
        final CameraName[] cameraNames = switchableCameraName.getMembers();
        tracer.trace(tracer.format("asyncOpenSwitchable(%s)", switchableCameraName), new Runnable()
            {
            @Override public void run()
                {
                RefCountedSwitchableCameraImpl switchableCamera = new RefCountedSwitchableCameraImpl(CameraManagerImpl.this, switchableCameraName, cameraNames, userContinuation); // alajzk3k3k3
                try {
                    switchableCamera.openAssumingPermission(reopenDuration, reopenTimeUnit);
                    }
                finally
                    {
                    switchableCamera.releaseRef(); // alajzk3k3k3
                    }
                }
            });
        }

    protected void asyncOpenWebcamAssumingPermission(@NonNull final WebcamName webcamName,
            @NonNull final Continuation<? extends Camera.StateCallback> userContinuation,
            final long reopenDuration, final TimeUnit reopenTimeUnit)
        {
        tracer.trace(tracer.format("asyncOpenWebcam(%s)", webcamName), new Runnable()
            {
            @Override public void run()
                {
                UsbResiliantWebcam resliantWebcam = new UsbResiliantWebcam(CameraManagerImpl.this, webcamName, userContinuation); // alajzk3k3k3
                try {
                    resliantWebcam.openAssumingPermission(reopenDuration, reopenTimeUnit);
                    }
                finally
                    {
                    resliantWebcam.releaseRef(); // alajzk3k3k3
                    }
                }
            });
        }

    @Override public Camera requestPermissionAndOpenCamera(final Deadline deadline, final CameraName cameraName, final @Nullable Continuation<? extends Camera.StateCallback> userContinuation)
        {
        Camera camera = tracer.trace("doOpenCamera()", new Supplier<Camera>()
            {
            @Override public Camera get()
                {
                final ContinuationSynchronizer<Camera> synchronizer = new ContinuationSynchronizer<>(deadline, TRACE);
                /**
                 * We have to ask for permission to use the camera, which may involved interacting
                 * with the user and so may take a while. The result of that request will be delivered
                 * by calling 'accept(permissionGranted)' on a worker thread in the default thread pool.
                 */
                tracer.trace("requesting permission for camera: %s", cameraName);
                cameraName.asyncRequestCameraPermission(AppUtil.getDefContext(), synchronizer.getDeadline(), Continuation.create(ThreadPool.getDefault(), new Consumer<Boolean>()
                    {
                    @Override public void accept(Boolean permissionGranted)
                        {
                        if (permissionGranted)
                            {
                            tracer.trace("permission granted for camera: %s", cameraName);

                            Camera.StateCallback synchronizingCallback = new Camera.StateCallbackDefault()
                                {
                                @Override public void onOpened(@NonNull final Camera camera)
                                    {
                                    addRefCamera(camera);
                                    if (userContinuation != null)
                                        {
                                        userContinuation.getTarget().onOpened(camera);
                                        }
                                    else
                                        {
                                        // We do NOT here close the camera if there's no user callback, as the camera
                                        // is ALSO returned as the return value if successful; caller will take care
                                        // of the closing responsibility through that path.
                                        //      closeCamera("synchronizingCallback", camera); -- don't call
                                        }
                                    synchronizer.finish(tracer.format("camera reports opened: %s", cameraName), camera);
                                    releaseRefCamera(camera);
                                    }
                                @Override public void onOpenFailed(@NonNull CameraName cameraName, @NonNull Camera.OpenFailure failureReason)
                                    {
                                    if (userContinuation != null) userContinuation.getTarget().onOpenFailed(cameraName, failureReason);
                                    synchronizer.finish(Misc.formatInvariant("camera failed to open: %s", cameraName), null);
                                    }
                                @Override public void onClosed(@NonNull Camera camera)
                                    {
                                    addRefCamera(camera);
                                    if (userContinuation != null) userContinuation.getTarget().onClosed(camera);
                                    releaseRefCamera(camera);
                                    }
                                @Override public void onError(@NonNull Camera camera, Camera.Error error)
                                    {
                                    addRefCamera(camera);
                                    if (userContinuation != null) userContinuation.getTarget().onError(camera, error);
                                    releaseRefCamera(camera);
                                    }
                                };

                            // run where the caller runs (if there is one) or just a handy place (if not)
                            final Continuation<Camera.StateCallback> synchronizingContinuation = userContinuation==null
                                    ? Continuation.create(ThreadPool.getDefault(), synchronizingCallback)
                                    : userContinuation.createForNewTarget(synchronizingCallback); // we'll run where they run

                            try {
                                /* Open the camera. NB: We're are running a different thread (one in the thread pool) than the one on which doOpenCamera was called */
                                asyncOpenCameraAssumingPermission(cameraName, synchronizingContinuation, deadline.getDuration(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS);
                                }
                            catch (RuntimeException e)
                                {
                                tracer.traceError(e, "exception opening camera: %s", cameraName);
                                synchronizer.finish("exception opening camera: " + cameraName, null);
                                }
                            }
                        else
                            {
                            RobotLog.ee(TAG, "permission declined for camera: %s", cameraName);
                            synchronizer.finish("permission declined", null);
                            }
                        }
                    }));

                // Wait for the above to complete
                try {
                    synchronizer.await("camera open");
                    }
                catch (InterruptedException e)
                    {
                    Thread.currentThread().interrupt();
                    }

                return synchronizer.getValue();
                }
            });

        return camera;
        }

    //----------------------------------------------------------------------------------------------
    // Utility
    //----------------------------------------------------------------------------------------------

    @Override public Executor getSerialThreadPool()
        {
        return serialThreadPool;
        }

    public UsbDevice findUsbDevice(String usbDevicePath) throws FileNotFoundException
        {
        try {
            return usbManager.getDeviceList().get(usbDevicePath);
            }
        catch (RuntimeException e)
            {
            throw new FileNotFoundException(usbDevicePath);
            }
        }

    @Override public @Nullable UvcDevice findUvcDevice(@NonNull WebcamName webcamName)
        {
        UvcDevice result = null;
        boolean found = false;
        final SerialNumber patternSerialNumber = webcamName.getSerialNumber();
        for (UvcDevice device : getOrMakeUvcContext().getDeviceList()) // remember: we have to release the whole list: can't exit loop early
            {
            try {
                SerialNumber candidate = device.getWebcamName().getSerialNumber();
                if (candidate.matches(patternSerialNumber))
                    {
                    if (!found)
                        {
                        result = device;
                        result.addRef();
                        found = true;
                        }
                    else
                        {
                        /**
                         * Duplicate: we'll show none of them as available. This can happen if at
                         * configuration time there was a unique {@link VendorProductSerialNumber}
                         * attached, which got wildcarded, but now there are <em>two</em> such cameras
                         * attached. We don't know which to pick! See {@link WebcamNameImpl#isAttached()}.
                         */
                        tracer.traceError("more than one webcam attached matching serial number %s: ignoring them all", patternSerialNumber);
                        if (result != null)
                            {
                            result.releaseRef();
                            result = null;
                            }
                        }
                    }
                }
            finally
                {
                device.releaseRef();
                }
            }
        if (result != null)
            {
            result.cacheWebcamName(); // while we can
            }
        return result;
        }

    }
