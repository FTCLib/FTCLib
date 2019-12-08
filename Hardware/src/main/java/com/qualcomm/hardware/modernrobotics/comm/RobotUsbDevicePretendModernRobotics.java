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
package com.qualcomm.hardware.modernrobotics.comm;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.qualcomm.robotcore.hardware.DeviceManager;
import com.qualcomm.robotcore.hardware.usb.RobotUsbDevice;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.SerialNumber;

import org.firstinspires.ftc.robotcore.internal.collections.CircularByteBuffer;
import org.firstinspires.ftc.robotcore.internal.collections.MarkedItemQueue;
import org.firstinspires.ftc.robotcore.internal.hardware.TimeWindow;
import org.firstinspires.ftc.robotcore.internal.system.Misc;

/**
 * This class implements a dummy RobotUsbDevice that will apparently successfully do reads and
 * writes but doesn't actually do anything.
 */
@SuppressWarnings("WeakerAccess")
public class RobotUsbDevicePretendModernRobotics implements RobotUsbDevice
    {
    protected FirmwareVersion           firmwareVersion = new FirmwareVersion();
    protected CircularByteBuffer        circularByteBuffer = new CircularByteBuffer(0);
    protected MarkedItemQueue           markedItemQueue = new MarkedItemQueue();
    protected boolean                   interruptRequested = false;
    protected SerialNumber              serialNumber;
    protected DeviceManager.UsbDeviceType deviceType = DeviceManager.UsbDeviceType.FTDI_USB_UNKNOWN_DEVICE;
    protected boolean                   debugRetainBuffers = false;
    protected ModernRoboticsDatagram.AllocationContext<ModernRoboticsRequest>  requestAllocationContext  = new ModernRoboticsDatagram.AllocationContext<ModernRoboticsRequest>();
    protected ModernRoboticsDatagram.AllocationContext<ModernRoboticsResponse> responseAllocationContext = new ModernRoboticsDatagram.AllocationContext<ModernRoboticsResponse>();

    public RobotUsbDevicePretendModernRobotics(SerialNumber serialNumber)
        {
        this.serialNumber = serialNumber;
        }
    @Override @NonNull public SerialNumber getSerialNumber()
        {
        return this.serialNumber;
        }

    @NonNull @Override public String getProductName()
        {
        return Misc.formatForUser("pretend %s", deviceType);
        }

    @Override public void setDeviceType(@NonNull DeviceManager.UsbDeviceType deviceType)
        {
        this.deviceType = deviceType;
        }
    @Override @NonNull public DeviceManager.UsbDeviceType getDeviceType()
        {
        return this.deviceType;
        }
    @Override public void close()
        {
        }
    @Override public boolean isOpen()
        {
        return true;
        }
    @Override public boolean isAttached()
        {
        return true;
        }
    @Override public void setDebugRetainBuffers(boolean retain)
        {
        this.debugRetainBuffers = retain;
        }
    @Override public boolean getDebugRetainBuffers()
        {
        return this.debugRetainBuffers;
        }
    @Override public void logRetainedBuffers(long nsOrigin, long nsTimerExpire, String tag, String format, Object...args)
        {
        RobotLog.ee(tag, format, args);
        }
    @Override public void setBaudRate(int i)
        {
        }
    @Override public void setDataCharacteristics(byte b, byte b1, byte b2)
        {
        }
    @Override public void setLatencyTimer(int i)
        {
        }
    @Override public void setBreak(boolean enable)
        {
        }
    @Override public void skipToLikelyUsbPacketStart()
        {
        int cbUnmarked = markedItemQueue.removeUpToNextMarkedItemOrEnd();
        circularByteBuffer.skip(cbUnmarked);
        }
    @Override public boolean mightBeAtUsbPacketStart()
        {
        return markedItemQueue.isAtMarkedItem() || markedItemQueue.isEmpty();
        }
    @Override public void write(byte[] bytes)
        {
        // Interpret the byte data. Note: this only works because higher levels only ever write
        // whole requests in one fell swoop
        ModernRoboticsRequest request = null;
        ModernRoboticsResponse response = null;
        try {
            request = ModernRoboticsRequest.from(requestAllocationContext, bytes);
            if (0 != request.getFunction()) throw new IllegalArgumentException("undefined function: " + request.getFunction());

            // Generate an appropriate response
            if (request.isWrite())
                {
                response = ModernRoboticsResponse.newInstance(responseAllocationContext, 0);
                response.setWrite(request.getFunction());
                response.setAddress(request.getAddress());
                }
            else
                {
                response = ModernRoboticsResponse.newInstance(responseAllocationContext, request.getPayloadLength());
                response.setRead(request.getFunction());
                response.setAddress(request.getAddress());
                response.setPayloadLength(request.getPayloadLength());
                response.clearPayload();
                }

            // Remember the response for a subsequent read
            circularByteBuffer.write(response.data);
            markedItemQueue.addMarkedItem();
            markedItemQueue.addUnmarkedItems(response.data.length-1);

            // We have a bit of a dilemma: we don't actually need to take any time to, e.g.,
            // communicate with an actual piece of hardware like a non-pretend device does.
            // As a result, were we to do nothing, our ReadWriteRunnable would spin through its
            // run() loop lickity-split, much faster than a real device. That just pointlessly eats
            // up cycles.
            //
            // One way around that would be to have that run() loop actually *block* if there's
            // nothing for it to do. For the moment though we just slow things down here to
            // better match real devices.
            //
            // The constant used here (3.5ms) has been observed in a non-scientific study to roughly
            // approximate the time taken for a RobotUsbDevice write() followed by a read(). But only
            // roughly. We could break that into two sleep()s, one in write() and the other in
            // read(), but we don't care enough about verisimilitude to be bothered.
            try {
                Thread.sleep(3, 500000);
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                }
            }
        finally
            {
            if (response != null) response.close();
            if (request != null) request.close();
            }
        }

    @Override public int read(byte[] bytes, int ibFirst, int cbToRead, long timeout, @Nullable TimeWindow timeWindow)
        {
        // Return what we can
        int cbRead = circularByteBuffer.read(bytes, ibFirst, cbToRead);
        markedItemQueue.removeItems(cbRead);
        if (timeWindow != null)
            {
            // We don't provide timestamps on data read
            timeWindow.clear();
            }
        return cbRead;
        }

    @Override public void resetAndFlushBuffers()
        {
        circularByteBuffer.clear();
        markedItemQueue.clear();
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

    @Override
    public void requestReadInterrupt(boolean interruptRequested)
        {
        this.interruptRequested = interruptRequested;
        }

    @Override
    public USBIdentifiers getUsbIdentifiers()
        {
        USBIdentifiers result = new USBIdentifiers();
        result.vendorId = 0x0403;   // FTDI
        result.productId = 0;       // fake
        result.bcdDevice = 0;       // fake
        return result;
        }
    }
