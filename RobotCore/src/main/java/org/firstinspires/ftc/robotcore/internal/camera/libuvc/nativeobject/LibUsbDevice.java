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

import android.os.Build;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.SerialNumber;
import com.qualcomm.robotcore.util.TypeConversion;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.system.Misc;
import org.firstinspires.ftc.robotcore.internal.system.NativeObject;
import org.firstinspires.ftc.robotcore.internal.usb.UsbSerialNumber;

import java.io.File;

/**
 * {@link LibUsbDevice} is the Java manifestation of a native libusb_device*
 */
@SuppressWarnings("WeakerAccess")
public class LibUsbDevice extends NativeObject
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = LibUsbDevice.class.getSimpleName();
    public String getTag() { return TAG; }

    protected final boolean traceEnabled;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public LibUsbDevice(long pointer)
        {
        this(pointer, true);
        }

    public LibUsbDevice(long pointer, boolean traceEnabled)
        {
		super(pointer, traceEnabled ? defaultTraceLevel : TraceLevel.None); // We assume ownership of the (native) ref count present in pointer
        this.traceEnabled = traceEnabled;
        }

    @Override protected void destructor()
        {
        if (pointer != 0)
            {
            nativeReleaseRefDevice(pointer, traceEnabled);
            clearPointer();
            }
        super.destructor();
        }

    //----------------------------------------------------------------------------------------------
    // Accessing
    //----------------------------------------------------------------------------------------------

    public String getUsbDeviceName()
        {
        String usbfsRoot = AppUtil.getInstance().getUsbFileSystemRoot();
        if (usbfsRoot != null)
            {
            int busNumber = getBusNumber();
            int deviceAddress = getDeviceAddress();
            return Misc.formatInvariant("%s/%03d/%03d", usbfsRoot, busNumber, deviceAddress);
            }
        throw new IllegalStateException("root of usbfs not known");
        }

    public long getPointer()
        {
        return pointer;
        }

    public int getBusNumber()
        {
        return TypeConversion.unsignedByteToInt(nativeGetBusNumber(pointer));
        }

    public int getPortNumber()
        {
        return TypeConversion.unsignedByteToInt(nativeGetPortNumber(pointer));
        }

    public int getDeviceAddress()
        {
        return TypeConversion.unsignedByteToInt(nativeGetDeviceAddress(pointer));
        }

    /**
     * Returns either the real serial number or one we make up from vid, pid, and connection path
     */
    public SerialNumber getRealOrVendorProductSerialNumber()
        {
        String string = nativeGetSerialNumber(pointer, traceEnabled);
        if (UsbSerialNumber.isValidUsbSerialNumber(string))
            {
            return SerialNumber.fromString(string);
            }
        else
            {
            return SerialNumber.fromVidPid(nativeGetVendorId(pointer), nativeGetProductId(pointer), getUsbConnectionPath());
            }
        }

    // http://www.linux-usb.org/FAQ.html#i6
    public @NonNull String getUsbConnectionPath()
        {
        String sysfsPath = nativeGetSysfs(pointer);
        if (!TextUtils.isEmpty(sysfsPath))
            {
            return new File(sysfsPath).getName(); // just the name in the syfsdir
            }
        else
            {
            // We always fail on Nougat and beyond, as sysfs has been perm-restricted, so don't bother logging there
            // https://source.android.com/setup/start/build-numbers
            if (Build.VERSION.SDK_INT < 24) 
                {
                RobotLog.ee(TAG, "unable to find USB connection path for %s", getUsbDeviceName());
                }
            return "";
            }
        }

    //----------------------------------------------------------------------------------------------
    // Native Methods
    //----------------------------------------------------------------------------------------------

    protected native static void nativeAddRefDevice(long pointer);
    protected native static void nativeReleaseRefDevice(long pointer, boolean traceEnabled);
    protected native static byte nativeGetBusNumber(long pointer);
    protected native static byte nativeGetPortNumber(long pointer);
    protected native static int  nativeGetVendorId(long pointer);
    protected native static int  nativeGetProductId(long pointer);
    protected native static byte nativeGetDeviceAddress(long pointer);
    protected native static String nativeGetSerialNumber(long pointer, boolean traceEnabled);
    protected native static String nativeGetSysfs(long pointer);
    }
