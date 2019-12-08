/*
Copyright (c) 2016 Robert Atkinson

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
package com.qualcomm.robotcore.hardware.usb;

import androidx.annotation.NonNull;

import com.qualcomm.robotcore.hardware.DeviceManager;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.SerialNumber;

import org.firstinspires.ftc.robotcore.internal.system.Assert;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import static org.firstinspires.ftc.robotcore.internal.system.Assert.assertFalse;

/**
 * Code that is usefully common to various {@link RobotUsbDevice} implementations
 */
@SuppressWarnings("WeakerAccess")
public abstract class RobotUsbDeviceImplBase implements RobotUsbDevice
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public abstract String getTag();

    /** contains all the devices currently opened for ALL different kinds of {@link RobotUsbDevice}s */
    protected static final ConcurrentHashMap<SerialNumber, RobotUsbDevice> extantDevices = new ConcurrentHashMap<SerialNumber, RobotUsbDevice>();
    protected static final ConcurrentHashMap<SerialNumber, DeviceManager.UsbDeviceType> deviceTypes = new ConcurrentHashMap<SerialNumber, DeviceManager.UsbDeviceType>();

    protected final SerialNumber        serialNumber;
    protected DeviceManager.UsbDeviceType deviceType;
    protected FirmwareVersion           firmwareVersion = new FirmwareVersion();

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    protected RobotUsbDeviceImplBase(SerialNumber serialNumber)
        {
        this.serialNumber = serialNumber;

        this.deviceType = deviceTypes.get(this.serialNumber);
        if (this.deviceType == null) this.deviceType = DeviceManager.UsbDeviceType.UNKNOWN_DEVICE;

        Assert.assertFalse(extantDevices.contains(serialNumber));
        extantDevices.put(serialNumber, this);
        }

    protected void removeFromExtantDevices()
        {
        extantDevices.remove(this.serialNumber);
        }

    //----------------------------------------------------------------------------------------------
    // Accessing
    //----------------------------------------------------------------------------------------------

    public static Collection<RobotUsbDevice> getExtantDevices()
        {
        return extantDevices.values();
        }

    public static boolean isOpen(SerialNumber serialNumber)
        {
        return extantDevices.containsKey(serialNumber);
        }

    public static @NonNull DeviceManager.UsbDeviceType getDeviceType(SerialNumber serialNumber)
        {
        DeviceManager.UsbDeviceType result = deviceTypes.get(serialNumber);
        return result == null ? DeviceManager.UsbDeviceType.UNKNOWN_DEVICE : result;
        }

    //----------------------------------------------------------------------------------------------
    // RobotUsbDevice
    //----------------------------------------------------------------------------------------------

    @Override public synchronized void setDeviceType(@NonNull DeviceManager.UsbDeviceType deviceType)
        {
        // RobotLog.vv(getTag(), "setDeviceType(%s,%s)", serialNumber, deviceType);
        this.deviceType = deviceType;
        deviceTypes.put(this.serialNumber, deviceType);
        }

    @NonNull @Override public synchronized DeviceManager.UsbDeviceType getDeviceType()
        {
        return this.deviceType;
        }

    @NonNull @Override public SerialNumber getSerialNumber()
        {
        return this.serialNumber;
        }

    @Override @NonNull
    public FirmwareVersion getFirmwareVersion()
        {
        return firmwareVersion;
        }

    @Override
    public void setFirmwareVersion(FirmwareVersion version)
        {
        this.firmwareVersion = version;
        }

    //----------------------------------------------------------------------------------------------
    // Utility
    //----------------------------------------------------------------------------------------------

    protected void dumpBytesReceived(byte[] data, int ibFirst, int cbRead)
        {
        RobotLog.logBytes(getTag(), "received", data, ibFirst, cbRead);
        }

    protected void dumpBytesSent(byte[] data)
        {
        RobotLog.logBytes(getTag(), "sent", data, data.length);
        }

    }
