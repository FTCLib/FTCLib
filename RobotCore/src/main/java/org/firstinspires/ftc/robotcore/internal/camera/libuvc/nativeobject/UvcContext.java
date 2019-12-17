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

import android.content.Context;
import android.hardware.usb.UsbConfiguration;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.renderscript.RenderScript;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.SerialNumber;

import org.firstinspires.ftc.robotcore.external.function.Consumer;
import org.firstinspires.ftc.robotcore.external.function.Function;
import org.firstinspires.ftc.robotcore.internal.camera.CameraManagerImpl;
import org.firstinspires.ftc.robotcore.internal.camera.CameraManagerInternal;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.system.ClassFactoryImpl;
import org.firstinspires.ftc.robotcore.internal.system.Misc;
import org.firstinspires.ftc.robotcore.internal.system.NativeObject;
import org.firstinspires.ftc.robotcore.internal.system.SystemProperties;
import org.firstinspires.ftc.robotcore.internal.usb.UsbConstants;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * {@link UvcContext} is the Java manifestation of a native uvc_context_t, together with
 * some additional Java-level state that should be maintained throughout a UVC session.
 */
@SuppressWarnings("WeakerAccess")
public class UvcContext extends NativeObject
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = UvcContext.class.getSimpleName();
    public String getTag() { return TAG; }
    public static boolean DEBUG_RS = false;
    protected static final AtomicInteger instanceCounter = new AtomicInteger(0);

    protected final int instanceNumber = instanceCounter.getAndIncrement();
    protected final Lock renderscriptAccessLock = new ReentrantLock();
    protected RenderScript renderScript = null;
    protected final RenderScript.ContextType renderScriptContextType =
            DEBUG_RS
                ? RenderScript.ContextType.DEBUG
                : RenderScript.ContextType.NORMAL;

    /* From the NDK:
     *  enum RSInitFlags {
     *      RS_INIT_SYNCHRONOUS = 1,        ///< All RenderScript calls will be synchronous. May reduce latency.
     *      RS_INIT_LOW_LATENCY = 2,        ///< Prefer low latency devices over potentially higher throughput devices.
     *      // Bitflag 4 is reserved for the context flag low power
     *      RS_INIT_WAIT_FOR_ATTACH = 8,    ///< Kernel execution will hold to give time for a debugger to be attached
     *      RS_INIT_MAX = 16
     *  };
     */
    protected final int renderScriptCreateFlags = 0; // RenderScript.CREATE_FLAG_NONE

    /**
     * We hold on to a {@link CameraManagerImpl}, but we DON'T ref count it. All we're trying to do is keep
     * same from being GC'd so long as we are alive; this will keep the instance management in
     * {@link ClassFactoryImpl#getCameraManager()} working better. To wit: whenever the UvcDeviceManager
     * ultimately gets GC'd and finalized, we'll be release()d and all will be well.
     */
    protected CameraManagerImpl cameraManagerImpl = null;

    protected @Nullable final String usbFileSystemRoot;
    protected boolean renderScriptInitialized = false;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public UvcContext(CameraManagerImpl cameraManagerImpl, @Nullable String usbFileSystemRoot)
        {
        super(nativeInitContext(usbFileSystemRoot, Build.VERSION.SDK_INT, AppUtil.FIRST_FOLDER.getAbsolutePath(), CameraManagerInternal.forceJavaUsbEnumerationKitKat), TraceLevel.None /*we'll trace ctor*/);
        traceLevel = defaultTraceLevel;
        if (usbFileSystemRoot == null)
            {
            RobotLog.ww(TAG, "creating UvcContext with null usbFileSystemRoot");
            }
        this.usbFileSystemRoot = usbFileSystemRoot;
        this.cameraManagerImpl = cameraManagerImpl;
        if (traceCtor()) RobotLog.vv(getTag(), "construct(%s)", getTraceIdentifier());
        }

    @Override protected void destructor()
        {
        if (renderScript != null)
            {
            renderScript.destroy();
            renderScript = null;
            }
        if (pointer != 0)
            {
            nativeExitContext(pointer);
            clearPointer();
            }
        cameraManagerImpl = null;
        super.destructor();
        }

    @Override public String getTraceIdentifier()
        {
        return super.getTraceIdentifier() + Misc.formatInvariant("|inst#=%d", instanceNumber);
        }

    //----------------------------------------------------------------------------------------------
    // Accessing
    //----------------------------------------------------------------------------------------------

    public long getPointer()
        {
        return pointer;
        }

    public @Nullable String getUsbFileSystemRoot()
        {
        return usbFileSystemRoot;
        }

    public CameraManagerImpl getCameraManagerImpl()
        {
        return cameraManagerImpl;
        }

    /** Note that we make up a serial number if the device doesn't actually have one */
    public @Nullable SerialNumber getRealOrVendorProductSerialNumber(UsbDevice usbDevice)
        {
        SerialNumber serialNumber = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) // useNonKitKatPaths isn't worth it here: serial number is so readily accessible
            {
            serialNumber = SerialNumber.fromUsbOrNull(usbDevice.getSerialNumber());
            }
        else
            {
            serialNumber = SerialNumber.fromUsbOrNull(nativeGetSerialNumberFromUsbPath(pointer, usbDevice.getDeviceName()));
            }
        if (serialNumber==null) // Device lacks real serial number: go the long route so we make up a VendorProductSerialNumber
            {
            LibUsbDevice libUsbDevice = getLibUsbDeviceFromUsbDeviceName(usbDevice.getDeviceName(), false);
            if (libUsbDevice != null)
                {
                serialNumber = libUsbDevice.getRealOrVendorProductSerialNumber();
                libUsbDevice.releaseRef();
                }
            }
        return serialNumber;
        }

    public @Nullable LibUsbDevice getLibUsbDeviceFromUsbDeviceName(String usbDeviceName, boolean traceEnabled)
        {
        synchronized (lock)
            {
            long libUsbDevicePointer = nativeGetLibUsbDeviceFromUsbDeviceName(pointer, usbDeviceName);
            if (libUsbDevicePointer != 0)
                {
                return new LibUsbDevice(libUsbDevicePointer, traceEnabled);
                }
            return null;
            }
        }

    //----------------------------------------------------------------------------------------------
    // Renderscript
    //----------------------------------------------------------------------------------------------

    protected void initRenderScriptParametersIfNeeded()
        {
        synchronized (lock)
            {
            if (!renderScriptInitialized)
                {
                File file = getRenderscriptCacheDir();
                AppUtil.getInstance().ensureDirectoryExists(file, false);
                //
                String dir    = file.getAbsolutePath();
                int targetApi = AppUtil.getDefContext().getApplicationInfo().targetSdkVersion;
                nativeInitRenderScriptParameters(pointer, dir, renderScriptCreateFlags, targetApi);

                /* see C:\Android\410c\build\frameworks\rs\rsContext.cpp:
                 *     if (getProp("debug.rs.debug") != 0) {
                 *         ALOGD("Forcing debug context due to debug.rs.debug.");
                 *         ...
                 *     }
                 */
                try {
                    SystemProperties.set("debug.rs.debug", renderScriptContextType== RenderScript.ContextType.DEBUG ? "1" : "0");
                    }
                catch (Throwable throwable)
                    {
                    // Ignore possible security issue or whatever
                    }

                renderScriptInitialized = true;
                }
            }
        }

    protected File getRenderscriptCacheDir()
        {
        // Internally, Java Renderscript uses the following. We just mirror.
        // final String CACHE_PATH = "com.android.renderscript.cache";
        // File f = new File(RenderScriptCacheDir.mCacheDir, CACHE_PATH);
        return new File(AppUtil.getDefContext().getCacheDir(), "org.firstinspires.ftc.renderscript.cache");
        }

    public @NonNull RenderScript getRenderScript()
        {
        synchronized (lock)
            {
            initRenderScriptParametersIfNeeded();
            if (null == renderScript)
                {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    {
                    renderScript = RenderScript.create(AppUtil.getDefContext(), renderScriptContextType, renderScriptCreateFlags);
                    }
                else
                    {
                    renderScript = RenderScript.create(AppUtil.getDefContext(), renderScriptContextType);
                    }
                }
            renderScript.setErrorHandler(new RenderScript.RSErrorHandler()
                {
                @Override public void run()
                    {
                    RobotLog.ee(getTag(), "RenderScript error(%d): %s", mErrorNum, mErrorMessage);
                    }
                });
            return renderScript;
            }
        }

    /** We only keep single instances of renderscript, so we need to serialize access thereto */
    public boolean lockRenderScriptWhile(long time, TimeUnit timeUnit, Runnable runnable)
        {
        try {
            initRenderScriptParametersIfNeeded();
            if (renderscriptAccessLock.tryLock(time, timeUnit))
                {
                try
                    {
                    runnable.run();
                    }
                finally
                    {
                    renderscriptAccessLock.unlock();
                    }
                return true;
                }
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            }
        return false;
        }

    //----------------------------------------------------------------------------------------------
    // LibUsb device enumeration
    //----------------------------------------------------------------------------------------------

    protected interface LongConsumer
        {
        void accept(long value);
        }

    public @NonNull List<LibUsbDevice> getMatchingLibUsbDevicesKitKat(final Function<SerialNumber, Boolean> matcher)
        {
        synchronized (lock)
            {
            final List<LibUsbDevice> result = new ArrayList<>();

            nativeEnumerateAttachedLibUsbDevicesKitKat(pointer, new LongConsumer()
                {
                @Override public void accept(long libusbPointer)
                    {
                    LibUsbDevice libUsbDevice = new LibUsbDevice(libusbPointer, false); // takes ownership of the pointer
                    try {
                        SerialNumber candidate = libUsbDevice.getRealOrVendorProductSerialNumber();
                        if (candidate != null && matcher.apply(candidate))
                            {
                            libUsbDevice.addRef();
                            result.add(libUsbDevice);
                            }
                        }
                    finally
                        {
                        libUsbDevice.releaseRef();
                        }
                    }
                });

            return result;
            }
        }

    /**
     * We're forced to do more work on KitKat. The world is much more efficient
     * on Lollipop and beyond: see the callers of this method.
     */
    public void enumerateAttachedSerialNumbersKitKat(final Consumer<SerialNumber> consumer)
        {
        synchronized (lock)
            {
            nativeEnumerateAttachedLibUsbDevicesKitKat(pointer, new LongConsumer()
                {
                @Override public void accept(long libusbPointer)
                    {
                    LibUsbDevice libUsbDevice = new LibUsbDevice(libusbPointer, false); // takes ownership of the pointer
                    try {
                        SerialNumber candidate = libUsbDevice.getRealOrVendorProductSerialNumber();
                        consumer.accept(candidate);
                        }
                    finally
                        {
                        libUsbDevice.releaseRef();
                        }
                    }
                });
            }
        }

    //----------------------------------------------------------------------------------------------
    // UVC device enumeration
    //----------------------------------------------------------------------------------------------

    /**
     * See uvc_is_usb_device_compatible()
     */
    protected boolean isUvcCompatible(UsbDevice usbDevice) // throws what?
        {
        // We *could* always use the native APIs, even when we don't have to. But we've got a lot
        // more testing done right now using the Java path on post KitKat, so we leave it (for now?).
        if (CameraManagerInternal.useNonKitKatPaths && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            {
            // We can use java-level APIs
            UsbConfiguration usbConfiguration = usbDevice.getConfiguration(0);
            int interfaceCount = usbConfiguration.getInterfaceCount();
            for (int i = 0; i < interfaceCount; i++)
                {
                // In the Java model, the USB interface collection is flattened. UsbInterface's
                // are to be distinguished from each other using getId() and getAlternateSetting(),
                // which taken as a pair together uniquely identify the interface.
                UsbInterface usbInterface = usbConfiguration.getInterface(i);
                if (usbInterface.getInterfaceClass() == UsbConstants.USB_CLASS_VIDEO && usbInterface.getInterfaceSubclass() == UsbConstants.USB_VIDEO_INTERFACE_SUBCLASS_STREAMING)
                    {
                    return true;
                    }
                }
            return false;
            }
        else
            {
            // We need to use native code. Note: we might *always* want to do that, perhaps, just for consistency
            UvcDevice uvcDevice = uvcDeviceFrom(usbDevice);
            if (uvcDevice != null)
                {
                try {
                    return uvcDevice.isUvcCompatible();
                    }
                finally
                    {
                    uvcDevice.releaseRef();
                    }
                }
            }
        return false;
        }

    protected UvcDevice uvcDeviceFrom(UsbDevice usbDevice)
        {
        UvcDevice result = null;
        long pointerUvcDevice = nativeCreateUvcDevice(this.pointer, usbDevice.getDeviceName());
        if (pointerUvcDevice != 0)
            {
            try {
                result = new UvcDevice(pointerUvcDevice, this, usbDevice);
                }
            catch (IOException e)
                {
                RobotLog.ee(TAG, e, "exception processing %s; ignoring", usbDevice.getDeviceName());
                }
            }
        else
            RobotLog.ee(TAG, "nativeCreateUvcDevice() failed");
        return result;
        }

    /**
     * Returns a list of currently attached USB Video class (UVC) cameras. To avoid security
     * issues on Android post KitKat, we do the USB device enumeration here in Java
     *
     * @return a list of currently attached USB Video class (UVC) cameras.
     */
    protected List<UvcDevice> getUvcDeviceListUsingJava() // throws NOTHING
        {
        synchronized (lock)
            {
            final ArrayList<UvcDevice> result = new ArrayList<>();

            final UsbManager usbManager = (UsbManager) AppUtil.getDefContext().getSystemService(Context.USB_SERVICE);
            for (final UsbDevice usbDevice : usbManager.getDeviceList().values())
                {
                try {
                    // Check for UVC compatibility using java
                    if (isUvcCompatible(usbDevice))
                        {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                            {
                            RobotLog.dd(TAG, "found webcam: usbPath=%s vid=%d pid=%d serial=%s product=%s",
                                    usbDevice.getDeviceName(),
                                    usbDevice.getVendorId(),
                                    usbDevice.getProductId(),
                                    usbDevice.getSerialNumber(),
                                    usbDevice.getProductName());
                            }
                        else
                            {
                            RobotLog.dd(TAG, "found webcam: usbPath=%s vid=%d pid=%d", usbDevice.getDeviceName(), usbDevice.getVendorId(), usbDevice.getProductId());
                            }
                        UvcDevice uvcDevice = uvcDeviceFrom(usbDevice);
                        if (uvcDevice != null)
                            {
                            result.add(uvcDevice); // transfer ownership of the reference
                            }
                        }
                    else
                        RobotLog.dd(TAG, "usb device is *not* UVC compatible, %s", usbDevice.getDeviceName());
                    }
                catch (RuntimeException e)
                    {
                    RobotLog.ee(TAG, e, "exception processing %s; ignoring", usbDevice.getDeviceName());
                    }
                }
            return result;
            }
        }

    /**
     * This returns the list of current UVC cameras using the classic approach of enumerating
     * usb devices in native code inside of the libuvc library itself. That, however, has issues
     * and troubles on Android post KitKat.
     */
    protected List<UvcDevice> getUvcDeviceListKitKat()  // throws NOTHING
        {
        synchronized (lock)
            {
            ArrayList<UvcDevice> result = new ArrayList<>();
            long[] nativeList = nativeGetUvcDeviceListKitKat(pointer);
            RobotLog.vv(TAG, "nativeGetDeviceList(): %d devices", nativeList.length);

            for (int i = 0; i < nativeList.length; i++)
                {
                try {
                    result.add(new UvcDevice(nativeList[i], this, null));
                    }
                catch (IOException|RuntimeException e)
                    {
                    RobotLog.ee(TAG, e, "internal error: failed opening UvcDevice: i=%d; ignoring", i);
                    }
                }
            return result;
            }
        }

    public List<UvcDevice> getDeviceList() // throws NOTHING
        {
        if (CameraManagerInternal.avoidKitKatLegacyPaths || Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            {
            return getUvcDeviceListUsingJava();
            }
        else
            {
            return getUvcDeviceListKitKat();
            }
        }

    //----------------------------------------------------------------------------------------------
    // Native Methods
    //----------------------------------------------------------------------------------------------

    protected native static long nativeInitContext(@Nullable String usbfs, int buildVersionSDKInt, @NonNull String tempFolder, boolean forceJavaUsbEnumerationKitKat);
    protected native static void nativeExitContext(long pointer);

    /** Creates a UvcDevice from whole cloth. */
    protected native static long nativeCreateUvcDevice(long pointer, String usbPath);

    /** Utility for finding serial number from usb path; necessary on KitKat */
    protected native static String nativeGetSerialNumberFromUsbPath(long pointer, String usbPath);
    protected native static void nativeEnumerateAttachedLibUsbDevicesKitKat(long pointer, LongConsumer consumer);
    protected native static long nativeGetLibUsbDeviceFromUsbDeviceName(long pointer, String usbDeviceName);

    /** Returns a java object full of longs which are actually uvc_device* pointers that own a ref count.*/
    protected static native long[] nativeGetUvcDeviceListKitKat(long pointer);

    protected static native void nativeInitRenderScriptParameters(long pointer, String cacheDir, int flags, int targetApi);
    }
