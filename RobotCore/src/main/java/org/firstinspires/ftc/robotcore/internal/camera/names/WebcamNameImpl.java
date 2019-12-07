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
package org.firstinspires.ftc.robotcore.internal.camera.names;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.qualcomm.robotcore.R;
import com.qualcomm.robotcore.eventloop.SyncdDevice;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.HardwareDeviceCloseOnTearDown;
import com.qualcomm.robotcore.hardware.usb.RobotArmingStateNotifier;
import com.qualcomm.robotcore.hardware.usb.RobotUsbDevice;
import com.qualcomm.robotcore.hardware.usb.RobotUsbModule;
import com.qualcomm.robotcore.util.GlobalWarningSource;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.SerialNumber;
import com.qualcomm.robotcore.util.WeakReferenceSet;

import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.function.Consumer;
import org.firstinspires.ftc.robotcore.external.function.Continuation;
import org.firstinspires.ftc.robotcore.external.function.ContinuationResult;
import org.firstinspires.ftc.robotcore.external.function.Function;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCharacteristics;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.internal.camera.CameraManagerInternal;
import org.firstinspires.ftc.robotcore.internal.camera.libuvc.api.UvcApiCameraCharacteristics;
import org.firstinspires.ftc.robotcore.internal.camera.libuvc.api.UvcApiCameraCharacteristicsBuilder;
import org.firstinspires.ftc.robotcore.internal.camera.libuvc.nativeobject.LibUsbDevice;
import org.firstinspires.ftc.robotcore.internal.camera.libuvc.nativeobject.UvcDevice;
import org.firstinspires.ftc.robotcore.internal.camera.libuvc.nativeobject.UvcStreamingInterface;
import org.firstinspires.ftc.robotcore.internal.collections.MutableReference;
import org.firstinspires.ftc.robotcore.internal.hardware.UserNameable;
import org.firstinspires.ftc.robotcore.internal.hardware.usb.ArmableUsbDevice;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.system.Assert;
import org.firstinspires.ftc.robotcore.internal.system.Deadline;
import org.firstinspires.ftc.robotcore.internal.system.Misc;
import org.firstinspires.ftc.robotcore.internal.system.Tracer;
import org.firstinspires.ftc.robotcore.internal.usb.UsbConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

@SuppressWarnings("WeakerAccess")
public class WebcamNameImpl extends CameraNameImplBase implements WebcamNameInternal, RobotUsbModule, GlobalWarningSource, HardwareDeviceCloseOnTearDown, UserNameable
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "WebcamNameImpl";
    protected final Tracer tracer = Tracer.create(TAG, TRACE);

    protected static Semaphore requestPermissionSemaphore = new Semaphore(1);
    protected final Object                               lock = new Object();
    protected final CameraManagerInternal                cameraManagerInternal;
    protected final @NonNull SerialNumber                serialNumberPattern;
    protected final @Nullable ArmableDeviceHelper        helper;
    protected String                                     userName = null;
    protected final WeakReferenceSet<DelegatingCallback> delegatingCallbacks = new WeakReferenceSet<>();

    @Override public String toString()
        {
        return "Webcam(" + serialNumberPattern + ")";
        }

    //----------------------------------------------------------------------------------------------
    // Construction (all internal)
    //----------------------------------------------------------------------------------------------

    private WebcamNameImpl(@NonNull SerialNumber serialNumberPattern)
        {
        Assert.assertNotNull(serialNumberPattern);
        this.cameraManagerInternal = (CameraManagerInternal)(ClassFactory.getInstance().getCameraManager());
        this.serialNumberPattern = serialNumberPattern;
        this.helper = null;
        }

    private WebcamNameImpl(@NonNull SerialNumber serialNumberPattern, ArmableUsbDevice.OpenRobotUsbDevice opener, @NonNull SyncdDevice.Manager manager)
        {
        Assert.assertNotNull(serialNumberPattern);
        this.cameraManagerInternal = (CameraManagerInternal)(ClassFactory.getInstance().getCameraManager());
        this.serialNumberPattern = serialNumberPattern;
        this.helper = new ArmableDeviceHelper(serialNumberPattern, opener, manager);
        this.helper.finishConstruction();
        }

    public static WebcamName forSerialNumber(@NonNull SerialNumber serialNumber)
        {
        return new WebcamNameImpl(serialNumber);
        }

    public static WebcamName forSerialNumber(@NonNull SerialNumber serialNumber, ArmableUsbDevice.OpenRobotUsbDevice opener, @NonNull SyncdDevice.Manager manager)
        {
        return new WebcamNameImpl(serialNumber, opener, manager);
        }

    //----------------------------------------------------------------------------------------------
    // Equality
    //----------------------------------------------------------------------------------------------

    @Override public boolean equals(Object o)
        {
        if (o instanceof WebcamNameImpl)
            {
            WebcamNameImpl them = (WebcamNameImpl)o;
            return serialNumberPattern.equals(them.serialNumberPattern);
            }
        return super.equals(o);
        }

    @Override public int hashCode()
        {
        return serialNumberPattern.hashCode();
        }

    //----------------------------------------------------------------------------------------------
    // CameraName
    //----------------------------------------------------------------------------------------------

    @Override public boolean isWebcam()
        {
        return true;
        }

    //----------------------------------------------------------------------------------------------
    // WebcamName
    //----------------------------------------------------------------------------------------------

    /**
     * As in {@link CameraManagerInternal#findUvcDevice}, this will return {@code false} if we are
     * a wildcard and there's more than one thing we match against.
     */
    @Override public boolean isAttached()
        {
        return isAttached(null);
        }
    protected boolean isAttached(@Nullable MutableReference<Boolean> isDuplicate)
        {
        return getUsbDeviceNameIfAttached(cameraManagerInternal, serialNumberPattern, isDuplicate) != null;
        }
    public static boolean isAttached(CameraManagerInternal cameraManagerInternal, SerialNumber serialNumberPattern)
        {
        return getUsbDeviceNameIfAttached(cameraManagerInternal, serialNumberPattern, null) != null;
        }
    @Override public @Nullable String getUsbDeviceNameIfAttached()
        {
        return getUsbDeviceNameIfAttached(cameraManagerInternal, serialNumberPattern, null);
        }
    static String getUsbDeviceNameIfAttached(CameraManagerInternal cameraManagerInternal, final SerialNumber serialNumberPattern, @Nullable MutableReference<Boolean> isDuplicate)
        {
        String result = null;
        if (isDuplicate != null) isDuplicate.setValue(false);
        if (CameraManagerInternal.avoidKitKatLegacyPaths || Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            {
            boolean found = false;
            for (UsbDevice usbDevice : getUsbManager().getDeviceList().values())
                {
                SerialNumber candidate = cameraManagerInternal.getRealOrVendorProductSerialNumber(usbDevice);
                if (candidate != null && candidate.matches(serialNumberPattern))
                    {
                    if (!found)
                        {
                        result = usbDevice.getDeviceName();
                        found = true;
                        }
                    else
                        {
                        RobotLog.ee(TAG, "more than one webcam attached matching serial number %s: ignoring them all", serialNumberPattern);
                        result = null;
                        if (isDuplicate != null) isDuplicate.setValue(true);
                        }
                    }
                }
            }
        else
            {
            List<LibUsbDevice> libUsbDevices = cameraManagerInternal.getMatchingLibUsbDevices(new Function<SerialNumber, Boolean>()
                {
                @Override public Boolean apply(SerialNumber candidate)
                    {
                    return candidate.matches(serialNumberPattern);
                    }
                });
            try {
                if (libUsbDevices.size()==0)
                    {
                    result = null; // device is not found
                    }
                else if (libUsbDevices.size()==1)
                    {
                    result = libUsbDevices.get(0).getUsbDeviceName();
                    }
                else
                    {
                    RobotLog.ee(TAG, "more than one webcam attached matching serial number %s: ignoring them all", serialNumberPattern);
                    for (LibUsbDevice libUsbDevice : libUsbDevices)
                        {
                        RobotLog.ee(TAG, "libUsbDevice: name=%s connection=%s serial=%s", libUsbDevice.getUsbDeviceName(), libUsbDevice.getUsbConnectionPath(), libUsbDevice.getRealOrVendorProductSerialNumber());
                        }
                    result = null;
                    if (isDuplicate != null) isDuplicate.setValue(true);
                    }
                }
            finally
                {
                for (LibUsbDevice libUsbDevice : libUsbDevices)
                    {
                    libUsbDevice.releaseRef();
                    }
                }
            }
        return result;
        }

    protected static List<SerialNumber> getMatchingAttachedSerialNumbers(CameraManagerInternal cameraManagerInternal, final Function<SerialNumber, Boolean> matcher)
        {
        final List<SerialNumber> result = new ArrayList<>();
        cameraManagerInternal.enumerateAttachedSerialNumbers(new Consumer<SerialNumber>()
            {
            @Override public void accept(SerialNumber candidate)
                {
                if (matcher.apply(candidate))
                    {
                    result.add(candidate);
                    }
                }
            });
        return result;
        }

    @Override public @NonNull SerialNumber getSerialNumber()
        {
        return serialNumberPattern;
        }

    @Override public void asyncRequestCameraPermission(Context context, Deadline deadline, final Continuation<? extends Consumer<Boolean>> userContinuation)
        {
        final UsbDevice usbDevice = getUsbDevice();
        if (usbDevice != null)
            {
            try {
                // To reduce user confusion, we enforce the fact that only one camera can attempt to
                // request permission at any one time.
                if (deadline.tryAcquire(requestPermissionSemaphore))
                    {
                    tracer.trace("requesting permission for %s", usbDevice.getDeviceName());
                    AppUtil.getInstance().asyncRequestUsbPermission(TAG, context, usbDevice, deadline, userContinuation.createForNewTarget(new Consumer<Boolean>()
                        {
                        @Override public void accept(final Boolean permissionGranted)
                            {
                            tracer.trace("permission for %s=%s", usbDevice.getDeviceName(), permissionGranted);
                            requestPermissionSemaphore.release();   // let others go as soon as we know we're done
                            userContinuation.dispatchHere(new ContinuationResult<Consumer<Boolean>>()
                                {
                                @Override public void handle(Consumer<Boolean> booleanConsumer)
                                    {
                                    booleanConsumer.accept(permissionGranted);
                                    }
                                });
                            }
                        }));
                    }
                else
                    {
                    tracer.trace("requestPermission(): timed out waiting on semaphore");
                    reportFalse(userContinuation);
                    }
                }
            catch (InterruptedException e)  // interrupt waiting on semaphore
                {
                Thread.currentThread().interrupt();
                tracer.trace("requestPermission(): interrupted");
                reportFalse(userContinuation);
                }
            }
        else
            {
            tracer.trace("unable to find usbDevice: %s", getSerialNumber());
            reportFalse(userContinuation);
            }
        }

    protected static void reportFalse(final Continuation<? extends Consumer<Boolean>> userContinuation)
        {
        userContinuation.dispatch(new ContinuationResult<Consumer<Boolean>>()
            {
            @Override public void handle(Consumer<Boolean> booleanConsumer)
                {
                booleanConsumer.accept(false);
                }
            });
        }

    @Override public CameraCharacteristics getCameraCharacteristics()
        {
        UvcDevice uvcDevice = findUvcDevice();
        if (uvcDevice != null)
            {
            try {
                UvcApiCameraCharacteristicsBuilder builder = new UvcApiCameraCharacteristicsBuilder();
                try {
                    for (UvcStreamingInterface streamingInterface : uvcDevice.getStreamingInterfaces())
                        {
                        builder.addStream(streamingInterface);
                        streamingInterface.releaseRef();
                        }
                    return builder.build();
                    }
                finally
                    {
                    builder.releaseRef();
                    }
                }
            finally
                {
                uvcDevice.releaseRef();
                }
            }
        return new UvcApiCameraCharacteristics();
        }

    protected @Nullable UvcDevice findUvcDevice()
        {
        CameraManagerInternal cameraManagerInternal = (CameraManagerInternal)ClassFactory.getInstance().getCameraManager();
        return cameraManagerInternal.findUvcDevice(this);
        }

    //----------------------------------------------------------------------------------------------
    // UserNameable
    //----------------------------------------------------------------------------------------------

    @Nullable @Override public String getUserName()
        {
        return userName;
        }

    @Override public void setUserName(@Nullable String userName)
        {
        this.userName = userName;
        }

    //----------------------------------------------------------------------------------------------
    // HardwareDevice
    //----------------------------------------------------------------------------------------------

    protected static UsbManager getUsbManager()
        {
        return (UsbManager) AppUtil.getDefContext().getSystemService(Context.USB_SERVICE);
        }

    protected @Nullable UsbDevice getUsbDevice()
        {
        return getUsbManager().getDeviceList().get(getUsbDeviceNameIfAttached());
        }

    @Override public String getDeviceName()
        {
        UsbDevice usbDevice = getUsbDevice();
        if (usbDevice != null)
            {
            String manufacturer = null;
            String product = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) // getManufacturerName & getProductName are >= LOLLIPOP
                {
                manufacturer = usbDevice.getManufacturerName();
                product = usbDevice.getProductName();
                }
            manufacturer = UsbConstants.getManufacturerName(manufacturer, usbDevice.getVendorId());
            product = UsbConstants.getProductName(product, usbDevice.getVendorId(), usbDevice.getProductId());
            if (TextUtils.isEmpty(product))
                {
                product = AppUtil.getDefContext().getString(R.string.moduleDisplayNameWebcam);
                }
            return (TextUtils.isEmpty(manufacturer)) ? product : manufacturer + " " + product;
            }
        else
            {
            return AppUtil.getDefContext().getString(R.string.moduleDisplayNameWebcam);
            }
        }

    @Override public String getConnectionInfo()
        {
        return "USB (" + getSerialNumber() + ")";
        }

    @Override public Manufacturer getManufacturer()
        {
        return Manufacturer.Unknown;
        }

    @Override public int getVersion()
        {
        return 1;
        }

    @Override public void resetDeviceConfigurationForOpMode()
        {
        // Nothing to do
        }

    @Override public void close()
        {
        if (helper != null) helper.close();
        }

    //----------------------------------------------------------------------------------------------
    // Arming and disarming
    //----------------------------------------------------------------------------------------------

    /**
     * {@link ArmableDeviceHelper} provides our {@link RobotUsbModule} & {@link GlobalWarningSource}
     * implementations, mostly by delegating back to us. We can't inherit like other {@link RobotUsbModule}s
     * do since we already inherit from {@link CameraNameImplBase}.
     */
    class ArmableDeviceHelper extends ArmableUsbDevice implements SyncdDevice, CameraManagerInternal.UsbAttachmentCallback
        {
        //------------------------------------------------------------------------------------------
        // State
        //------------------------------------------------------------------------------------------

        protected volatile boolean hasDetached = false;

        //------------------------------------------------------------------------------------------
        // Construction
        //------------------------------------------------------------------------------------------

        public ArmableDeviceHelper(final SerialNumber serialNumber, OpenRobotUsbDevice opener, @NonNull SyncdDevice.Manager manager)
            {
            super(AppUtil.getDefContext(), serialNumber, manager, opener);
            }

        //------------------------------------------------------------------------------------------
        // Attachment monitoring
        //------------------------------------------------------------------------------------------

        @Override public void onAttached(UsbDevice usbDevice, SerialNumber serialNumberJustAttached, MutableReference<Boolean> claimed)
            {
            // When other devices attach, because of duplicates that might logically *de*tach us if we're a wildcard
            checkAttached();
            }

        @Override public void onDetached(UsbDevice usbDevice)
            {
            checkAttached();
            }

        protected void checkAttached()
            {
            synchronized (armingLock)
                {
                if (getArmingState()== ARMINGSTATE.ARMED)
                    {
                    if (!hasDetached)
                        {
                        MutableReference<Boolean> isDuplicate = new MutableReference<>(false);
                        if (!isAttached(isDuplicate))
                            {
                            tracer.trace("detach detected");
                            hasDetached = true;
                            setGlobalWarning(getUnableToFindMessage(isDuplicate.getValue()));
                            }
                        }
                    }
                }
            }

        //------------------------------------------------------------------------------------------
        // ArmbableUsbDevice
        //------------------------------------------------------------------------------------------

        @Override protected String getUnableToOpenMessage()
            {
            return getUnableToFindMessage(false);
            }
        protected String getUnableToFindMessage(boolean isDuplicate)
            {
            if (isDuplicate)
                {
                return userName==null
                        ? Misc.formatForUser(R.string.duplicateWebcam, serialNumber)
                        : Misc.formatForUser(R.string.duplicateWebcamWithName, userName, serialNumber);
                }
            else
                {
                return userName==null
                        ? Misc.formatForUser(R.string.webcamNotFound, serialNumber)
                        : Misc.formatForUser(R.string.webcamNotFoundWithName, userName, serialNumber);
                }
            }

        @Override public void finishConstruction()
            {
            super.finishConstruction();
            }
        @Override protected void registerGlobalWarningSource()
            {
            tracer.trace("registerGlobalWarningSource(%s)", WebcamNameImpl.this);
            RobotLog.registerGlobalWarningSource(WebcamNameImpl.this);
            }

        @Override protected void unregisterGlobalWarningSource()
            {
            RobotLog.unregisterGlobalWarningSource(WebcamNameImpl.this);
            tracer.trace("unregisterGlobalWarningSource(%s)", WebcamNameImpl.this);
            }

        @Override protected String getTag()
            {
            return TAG;
            }
        @Override protected void armDevice(RobotUsbDevice device) throws RobotCoreException, InterruptedException
            {
            tracer.trace("armDevice()", new Runnable()
                {
                @Override public void run()
                    {
                    hasDetached = false;
                    if (syncdDeviceManager !=null) syncdDeviceManager.registerSyncdDevice(ArmableDeviceHelper.this);
                    cameraManagerInternal.registerReceiver(ArmableDeviceHelper.this);

                    // Do this immediately since we might be a wildcard with more than one matching camera
                    checkAttached();
                    }
                });
            }

        @Override protected void pretendDevice(RobotUsbDevice device) throws RobotCoreException, InterruptedException
            {
            tracer.trace("pretendDevice()", new Runnable()
                {
                @Override public void run()
                    {
                    hasDetached = false;
                    if (syncdDeviceManager !=null) syncdDeviceManager.registerSyncdDevice(ArmableDeviceHelper.this);
                    cameraManagerInternal.registerReceiver(ArmableDeviceHelper.this);
                    }
                });
            }

        @Override protected void disarmDevice() throws InterruptedException
            {
            tracer.trace("disarmDevice()", new Runnable()
                {
                @Override public void run()
                    {
                    if (syncdDeviceManager !=null) syncdDeviceManager.unregisterSyncdDevice(ArmableDeviceHelper.this);
                    cameraManagerInternal.unregisterReceiver(ArmableDeviceHelper.this);
                    }
                });
            }

        //------------------------------------------------------------------------------------------
        // SyncdDevice
        //------------------------------------------------------------------------------------------

        @Override public ShutdownReason getShutdownReason()
            {
            return hasDetached
                ? ShutdownReason.ABNORMAL   // returning this eventually triggers us to pretend
                : ShutdownReason.NORMAL;
            }

        @Override public void setOwner(RobotUsbModule owner)
            {
            // Ignored
            }

        @Override public RobotUsbModule getOwner()
            {
            return WebcamNameImpl.this;
            }
        }

    //----------------------------------------------------------------------------------------------
    // Delegating methods
    //----------------------------------------------------------------------------------------------

    @Override public void arm() throws RobotCoreException, InterruptedException
        {
        if (helper!=null) helper.arm();
        }

    @Override public void pretend() throws RobotCoreException, InterruptedException
        {
        if (helper!=null) helper.pretend();
        }

    @Override public void armOrPretend() throws RobotCoreException, InterruptedException
        {
        if (helper!=null) helper.armOrPretend();
        }

    @Override public void disarm() throws RobotCoreException, InterruptedException
        {
        if (helper!=null) helper.disarm();
        }

    @Override public ARMINGSTATE getArmingState()
        {
        return helper!=null
            ? helper.getArmingState()
            : ARMINGSTATE.CLOSED;
        }

    class DelegatingCallback implements Callback
        {
        public final Callback userCallback;
        DelegatingCallback(Callback userCallback)
            {
            this.userCallback = userCallback;
            }
        @Override public void onModuleStateChange(RobotArmingStateNotifier module, ARMINGSTATE state)
            {
            userCallback.onModuleStateChange(WebcamNameImpl.this, state);
            }
        }

    @Override public void registerCallback(Callback userCallback, boolean doInitialCallback)
        {
        synchronized (delegatingCallbacks)
            {
            for (DelegatingCallback delegatingCallback : delegatingCallbacks)
                {
                if (delegatingCallback.userCallback == userCallback)
                    {
                    return; // already registered
                    }
                }
            DelegatingCallback delegatingCallback = new DelegatingCallback(userCallback);
            delegatingCallbacks.add(delegatingCallback);
            if (helper!=null) helper.registerCallback(delegatingCallback, doInitialCallback);
            }
        }

    @Override public void unregisterCallback(Callback userCallback)
        {
        synchronized (delegatingCallbacks)
            {
            DelegatingCallback toRemove = null;
            for (DelegatingCallback delegatingCallback : delegatingCallbacks)
                {
                if (delegatingCallback.userCallback == userCallback)
                    {
                    toRemove = delegatingCallback;
                    break;
                    }
                }
            if (toRemove != null)
                {
                delegatingCallbacks.remove(toRemove);
                if (helper!=null) helper.unregisterCallback(toRemove.userCallback);
                }
            }
        }

    @Override public String getGlobalWarning()
        {
        return helper != null
            ? helper.getGlobalWarning()
            : "";
        }

    @Override public void suppressGlobalWarning(boolean suppress)
        {
        if (helper!=null) helper.suppressGlobalWarning(suppress);
        }

    @Override public void setGlobalWarning(String warning)
        {
        if (helper!=null) helper.setGlobalWarning(warning);
        }

    @Override public void clearGlobalWarning()
        {
        if (helper!=null) helper.clearGlobalWarning();
        }
    }
