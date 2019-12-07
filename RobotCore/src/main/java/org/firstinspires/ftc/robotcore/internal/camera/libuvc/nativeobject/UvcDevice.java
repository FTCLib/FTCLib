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

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.firstinspires.ftc.robotcore.external.function.Supplier;
import org.firstinspires.ftc.robotcore.external.hardware.camera.Camera;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.internal.camera.CameraManagerImpl;
import org.firstinspires.ftc.robotcore.internal.camera.libuvc.constants.UvcError;
import org.firstinspires.ftc.robotcore.internal.system.Assert;
import org.firstinspires.ftc.robotcore.internal.system.Misc;
import org.firstinspires.ftc.robotcore.internal.system.NativeObject;
import org.firstinspires.ftc.robotcore.internal.system.Tracer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

/**
 * {@link UvcDevice} is the java manifestation of a native uvc_device_t.
 */
@SuppressWarnings("WeakerAccess")
public class UvcDevice extends NativeObject<UvcContext>
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = UvcDevice.class.getSimpleName();
    public String getTag() { return TAG; }
    public static boolean TRACE = true;
    protected Tracer tracer = Tracer.create(TAG, TRACE);

    protected       LibUsbDevice        libUsbDevice;
    protected final UsbDevice           usbDevice;
    protected       UsbDeviceConnection usbDeviceConnection;
    protected       WebcamName          webcamName;
    protected       UsbInterfaceManager usbInterfaceManager = new UsbInterfaceMangerImpl();

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    /** Note: in ALL cases, this takes ownership of thisPointer, even on error */
    public UvcDevice(long thisPointer, @NonNull UvcContext uvcContext, @Nullable UsbDevice usbDevice) throws IOException
        {
        super(thisPointer);
        try {
            setParent(uvcContext);
            this.libUsbDevice = new LibUsbDevice(nativeGetLibUsbDevice(pointer));
            this.usbDevice = usbDevice==null ? findUsbDevice() : usbDevice;
            this.webcamName = null;
            this.usbDeviceConnection = null;
            }
        catch (IOException|RuntimeException e)
            {
            this.releaseRef();  // exceptions in ctors need careful handling, as clients are blind
            throw e;
            }
        }

    @Override protected void destructor()
        {
        if (usbDeviceConnection != null)
            {
            usbDeviceConnection.close();
            usbDeviceConnection = null;
            }
        if (pointer != 0)
            {
            nativeReleaseRefDevice(pointer);
            clearPointer();
            }
        if (libUsbDevice != null)
            {
            libUsbDevice.releaseRef();
            libUsbDevice = null;
            }
        super.destructor();
        }

    //----------------------------------------------------------------------------------------------
    // Accessing
    //----------------------------------------------------------------------------------------------

    public int getVendorId()
        {
        return usbDevice.getVendorId();
        }

    public int getProductId()
        {
        return usbDevice.getProductId();
        }

    public UvcContext getUvcContext()
        {
        return getParent();
        }

    public CameraManagerImpl getCameraManagerImpl()
        {
        return getParent().getCameraManagerImpl();
        }

    protected UsbManager getUsbManager()
        {
        return getCameraManagerImpl().getUsbManager();
        }

    public UsbDevice getUsbDevice()
        {
        return usbDevice;
        }

    public boolean isUvcCompatible()
        {
        return nativeIsUvcCompatible(pointer);
        }

    @NonNull public WebcamName getWebcamName()
        {
        synchronized (lock)
            {
            cacheWebcamName();
            return webcamName;
            }
        }

    public void cacheWebcamName()
        {
        synchronized (lock)
            {
            if (webcamName==null)
                {
                webcamName = internalGetWebcamName();
                }
            }
        }

    protected WebcamName internalGetWebcamName() // throws nothing
        {
        try {
            synchronized (lock)
                {
                if (usbDevice != null)
                    {
                    return getCameraManagerImpl().webcamNameFromDevice(usbDevice);
                    }
                if (libUsbDevice != null)
                    {
                    return getCameraManagerImpl().webcamNameFromDevice(libUsbDevice);
                    }
                }
            }
        catch (RuntimeException e)
            {
            // ignore
            }
        return null;
        }

    protected @Nullable String internalGetUsbDeviceName()
        {
        synchronized (lock)
            {
            if (usbDevice != null)
                {
                return usbDevice.getDeviceName();
                }
            if (libUsbDevice != null)
                {
                return libUsbDevice.getUsbDeviceName();
                }
            return null;
            }
        }

    protected @NonNull String getUsbDeviceName()
        {
        String result = internalGetUsbDeviceName();
        if (result == null)
            {
            throw Misc.internalError("internal error: getUsbDeviceName with both usbDevice and libUsbDevice null");
            }
        return result;
        }

    protected UsbDevice findUsbDevice() throws FileNotFoundException
        {
        return getCameraManagerImpl().findUsbDevice(getUsbDeviceName());
        }

    @Override public String toString()
        {
        return Misc.formatInvariant("%s(%s)", getTag(), internalGetUsbDeviceName());
        }

    @Override public String getTraceIdentifier()
        {
        return super.getTraceIdentifier() + "|" + internalGetUsbDeviceName();
        }

    public UvcDeviceDescriptor getDeviceDescriptor()
        {
        return new UvcDeviceDescriptor(nativeGetDeviceDescriptor(pointer));
        }

    public UvcDeviceInfo getDeviceInfo()
        {
        return new UvcDeviceInfo(nativeGetDeviceInfo(pointer));
        }

    public List<UvcStreamingInterface> getStreamingInterfaces()
        {
        UvcDeviceInfo deviceInfo = getDeviceInfo();
        try {
            return deviceInfo.getStreamingInterfaces();
            }
        finally
            {
            deviceInfo.releaseRef();
            }
        }

    //----------------------------------------------------------------------------------------------
    // Native Helper
    //----------------------------------------------------------------------------------------------

    interface UsbInterfaceManager
        {
        int claimInterface(int idx);
        int releaseInterface(int idx);
        boolean isSetInterfaceAltSettingSupported();
        int setInterfaceAltSetting(int bInterfaceNumber, int bAlternateSetting);
        }

    protected class UsbInterfaceMangerImpl implements UsbInterfaceManager
        {
        @Override public int claimInterface(int idx)
            {
            tracer.trace("claimInterface(%d)", idx);
            int interfaceCount = usbDevice.getInterfaceCount();
            for (int i = 0; i < interfaceCount; i++)
                {
                // In the Java model, the USB interface collection is flattened. UsbInterface's
                // are to be distinguished from each other using getId() and getAlternateSetting(),
                // which taken as a pair together uniquely identify the interface.
                UsbInterface usbInterface = usbDevice.getInterface(i);
                if (usbInterface.getId() == idx)
                    {
                    if (usbDeviceConnection.claimInterface(usbInterface, true))
                        {
                        tracer.trace("claimInterface(%d) succeeded", idx);
                        return UvcError.SUCCESS.getValue();
                        }
                    else
                        {
                        tracer.traceError("claimInterface(%d) failed", idx);
                        return UvcError.IO.getValue();
                        }
                    }
                }
            tracer.traceError("claimInterface(%d) failed: not found", idx);
            return UvcError.NOT_FOUND.getValue();
            }

        @Override public int releaseInterface(int idx)
            {
            tracer.trace("releaseInterface(%d)", idx);
            int interfaceCount = usbDevice.getInterfaceCount();
            for (int i = 0; i < interfaceCount; i++)
                {
                UsbInterface usbInterface = usbDevice.getInterface(i);
                if (usbInterface.getId() == idx)
                    {
                    if (usbDeviceConnection.releaseInterface(usbInterface))
                        {
                        tracer.trace("releaseInterface(%d) succeeded", idx);
                        return UvcError.SUCCESS.getValue();
                        }
                    else
                        {
                        tracer.traceError("releaseInterface(%d) failed", idx);
                        return UvcError.IO.getValue();
                        }
                    }
                }
            tracer.traceError("releaseInterface(%d) failed: not found", idx);
            return UvcError.NOT_FOUND.getValue();
            }

        @Override public boolean isSetInterfaceAltSettingSupported()
            {
            return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
            }

        @Override public int setInterfaceAltSetting(int idx, int alternateSetting)
            {
            tracer.trace("setInterfaceAltSetting(%d,%d)", idx, alternateSetting);
            int interfaceCount = usbDevice.getInterfaceCount();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                {
                for (int i = 0; i < interfaceCount; i++)
                    {
                    UsbInterface usbInterface = usbDevice.getInterface(i);
                    if (usbInterface.getId() == idx && usbInterface.getAlternateSetting() == alternateSetting)
                        {
                        if (usbDeviceConnection.setInterface(usbInterface))
                            {
                            tracer.trace("setInterfaceAltSetting(%d,%d) succeeded", idx, alternateSetting);
                            return UvcError.SUCCESS.getValue();
                            }
                        else
                            {
                            tracer.traceError("setInterfaceAltSetting(%d, %d) failed", idx, alternateSetting);
                            return UvcError.IO.getValue();
                            }
                        }
                    }
                }
            tracer.traceError("setInterfaceAltSetting(%d, %d) failed: not found", idx, alternateSetting);
            return UvcError.NOT_FOUND.getValue();
            }
        }

    //----------------------------------------------------------------------------------------------
    // Opening and closing
    //----------------------------------------------------------------------------------------------

    /**
     * Calls either onOpened or onOpenFailed before returning. If successful, returns a ref for
     * caller to own on the UvcDeviceHandle. Returns null on failure.
     */
    public @Nullable UvcDeviceHandle open(final WebcamName cameraName, final Camera.StateCallback stateCallback) // throws NOTHING
        {
        return tracer.trace("open()", new Supplier<UvcDeviceHandle>()
            {
            @Override public UvcDeviceHandle get()
                {
                synchronized (lock)
                    {
                    Camera.OpenFailure failureReason = Camera.OpenFailure.InternalError;
                    try {
                        Assert.assertNull(usbDeviceConnection);
                        usbDeviceConnection = getUsbManager().openDevice(usbDevice);
                        if (usbDeviceConnection != null)
                            {
                            // Callee will dup the handle & copy the name
                            if (nativeSetUsbDeviceInfo(pointer, usbDeviceConnection.getFileDescriptor(), usbDevice.getDeviceName()))
                                {
                                long pointerHandle = nativeOpenDeviceHandle(pointer, usbInterfaceManager);
                                if (pointerHandle != 0)
                                    {
                                    UvcDeviceHandle uvcDeviceHandle = new UvcDeviceHandle(pointerHandle, UvcDevice.this, stateCallback);
                                    uvcDeviceHandle.openSelfAndReport();
                                    return uvcDeviceHandle;
                                    }
                                else
                                    failureReason = Camera.OpenFailure.OtherFailure;
                                }
                            else
                                failureReason = Camera.OpenFailure.InternalError;
                            }
                        else
                            failureReason = Camera.OpenFailure.InUseOrAccessDenied;
                        }
                    catch (RuntimeException e)
                        {
                        tracer.traceError(e, "exception opening UvcDevice %s", cameraName);
                        failureReason = Camera.OpenFailure.InternalError;
                        }

                    stateCallback.onOpenFailed(cameraName, failureReason);
                    return null;
                    }
                }
            });
        }

    //----------------------------------------------------------------------------------------------
    // Native Methods
    //----------------------------------------------------------------------------------------------

    protected native static long nativeGetDeviceDescriptor(long pointer);
    protected native static long nativeGetContext(long pointer);
    protected native static long nativeGetDeviceInfo(long pointer);
    protected native static long nativeGetLibUsbDevice(long pointer);
    protected native static long nativeOpenDeviceHandle(long pointer, UsbInterfaceManager usbInterfaceManager);
    protected native static void nativeReleaseRefDevice(long pointer);
    protected native static boolean nativeSetUsbDeviceInfo(long pointer, int fd, String usbPath);
    protected native static boolean nativeIsUvcCompatible(long pointer);
    }
