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
package com.qualcomm.robotcore.hardware.usb.serial;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.qualcomm.robotcore.hardware.usb.RobotUsbDevice;
import com.qualcomm.robotcore.hardware.usb.RobotUsbDeviceImplBase;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.SerialNumber;

import org.firstinspires.ftc.robotcore.internal.system.Assert;
import org.firstinspires.ftc.robotcore.internal.hardware.TimeWindow;
import org.firstinspires.ftc.robotcore.internal.usb.exception.RobotUsbException;
import org.firstinspires.ftc.robotcore.internal.usb.exception.RobotUsbUnspecifiedException;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * {@link RobotUsbDeviceTty} is an implementation of {@link RobotUsbDevice} that
 * sits on top of a serial file handle (such as /dev/tty) and presents the persona of
 * a (fake) USB device.
 */
@SuppressWarnings("WeakerAccess")
public class RobotUsbDeviceTty extends RobotUsbDeviceImplBase implements RobotUsbDevice
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "RobotUsbDeviceTTY";
    public static boolean DEBUG = false;
    @Override public String getTag() { return TAG; }

    protected final File                file;
    protected SerialPort                serialPort;
    protected int                       baudRate;
    protected int                       msDefaultTimeout = 100;
    protected USBIdentifiers            usbIdentifiers   = new USBIdentifiers();
    protected final Object              startStopLock    = new Object();
    protected final Object              readLock         = new Object();
    protected final Object              writeLock        = new Object();
    protected Queue<Byte>               readAhead        = new ArrayDeque<Byte>();
    protected boolean                   debugRetainBuffers = false;
    protected @NonNull String           productName      = "";

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public RobotUsbDeviceTty(SerialPort serialPort, SerialNumber serialNumber, File file)
        {
        super(serialNumber);
        RobotLog.vv(TAG, "opening serial=%s file=%s", serialNumber, file.getPath());
        this.file = file;
        this.serialPort = serialPort;
        this.baudRate = serialPort.getBaudRate();
        }

    //----------------------------------------------------------------------------------------------
    // RobotUsbDevice - construction
    //----------------------------------------------------------------------------------------------

    @Override public void close()
        {
        synchronized (this.startStopLock)
            {
            if (this.serialPort != null)
                {
                RobotLog.vv(TAG, "closing serial=%s file=%s", serialNumber, file.getPath());
                this.serialPort.close();
                this.serialPort = null;
                //
                removeFromExtantDevices();
                }
            }
        }

    @Override public boolean isOpen()
        {
        synchronized (this.startStopLock)
            {
            return this.serialPort != null;
            }
        }

    @Override public boolean isAttached()
        {
        return true;
        }

    //----------------------------------------------------------------------------------------------
    // RobotUsbDevice - core read & write
    //----------------------------------------------------------------------------------------------

    @Override public void resetAndFlushBuffers()
        {
        // TODO: Nothing we know how to do here, at the moment, but perhaps we can revisit this in future
        }

    @Override public void write(byte[] data) throws RobotUsbException
        {
        // Only one writer at a time: we're not *certain* the output stream is thread-safe
        synchronized (this.writeLock)
            {
            try {
                this.serialPort.getOutputStream().write(data);
                if (DEBUG)
                    {
                    dumpBytesSent(data);
                    }
                }
            catch (IOException e)
                {
                throw RobotUsbUnspecifiedException.createChained(e, "exception in %s.write()", TAG);
                }
            }
        }

    @Override
    public int read(byte[] data, final int ibFirst, final int cbToRead, final long msTimeout, @Nullable TimeWindow timeWindow) throws RobotUsbException, InterruptedException
        {
        // Only one reader at a time, thank you very much
        synchronized (this.readLock)
            {
            try {
                ElapsedTime timer = new ElapsedTime();
                int cbRead = 0;

                // Use up any readahead that we have from when we might have timed out previously
                while (cbRead < cbToRead && this.readAhead.size() > 0)
                    {
                    data[cbRead++] = this.readAhead.remove();
                    }

                // Get the remainder by actually reading
                while (isOpen() && cbRead < cbToRead)   // isOpen() call may not actually be needed: read() call might (?) prematurely return
                    {
                    // Attempt to read all that we still need
                    int cbReadOnce = this.serialPort.getInputStream().read(data, ibFirst + cbRead, cbToRead - cbRead);

                    // Adjust for Java's odd EOF behavior
                    if (cbReadOnce == -1) cbReadOnce = 0;

                    // Do our bookkeeping based on what we've got; return if we're done
                    Assert.assertTrue(cbReadOnce >= 0);
                    cbRead += cbReadOnce;
                    if (cbRead == cbToRead) break;

                    // Did we hit the timeout?
                    if (timer.milliseconds() > msTimeout) break;

                    // Behave ourselves, then go around the loop again
                    if (Thread.interrupted()) throw new InterruptedException();
                    Thread.yield();
                    }

                if (cbRead==cbToRead)
                    {
                    // All is well: we got what we came for
                    if (DEBUG)
                        {
                        dumpBytesReceived(data, ibFirst, cbRead);
                        }

                    // We don't support timestamps (we wish we could)!
                    if (timeWindow != null)
                        {
                        timeWindow.clear();
                        }

                    // Return data to our caller
                    return cbRead;
                    }
                else
                    {
                    // Timeout or close case. Push back data for next time.
                    for (int i = 0; i < cbRead; i++)
                        {
                        this.readAhead.add(data[i]);
                        }
                    RobotLog.ee(TAG, "didn't read enough data cbToRead=%d cbRead=%d msTimeout=%d", cbToRead, cbRead, msTimeout);
                    return 0;   // maybe an odd semantic to indicate timeout, but that's what we've got
                    }
                }
            catch (InterruptedIOException e)
                {
                // Our callers don't know about the IO flavor of InterruptedException well, so turn it into what they're expecting
                throw (e.getCause() instanceof InterruptedException)
                    ? (InterruptedException)e.getCause()
                    : new InterruptedException(e.getMessage());
                }
            catch (IOException e)
                {
                // Wrap anything else in what they're expecting
                throw RobotUsbUnspecifiedException.createChained(e, "exception in %s.read()", TAG);
                }
            }
        }

    @Override public boolean mightBeAtUsbPacketStart()
        {
        return true;
        }

    @Override public void skipToLikelyUsbPacketStart()
        {
        // Nothing we can do here of use
        }

    @Override public void requestReadInterrupt(boolean interruptRequested)
        {
        // We don't need to do anything here, since the actual threads on which IO occurs (never
        // this current thread) will also be interrupted, and the IO we do here will honor that
        // interrupt correctly, unlike the FTDI layer.
        }

    //----------------------------------------------------------------------------------------------
    // RobotUsbDevice - io configuration
    //----------------------------------------------------------------------------------------------

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
    @Override public void setBaudRate(int baudRate) throws RobotUsbException
        {
        // Ignored: we are passed in our open device, with baud rate already configured.
        // If we wanted to update it on the fly, we'd need to do more work in SerialPort.cpp.
        // We could do that in theory, but it's not worth it right at the moment.
        }

    @Override
    public void setDataCharacteristics(byte dataBits, byte stopBits, byte parity) throws RobotUsbException
        {
        // ignored
        }

    @Override public void setLatencyTimer(int latencyTimer) throws RobotUsbException
        {
        // ignored
        }

    @Override public void setBreak(boolean enable) throws RobotUsbException
        {
        // ignored // TODO fix later
        }

    public void setMsDefaultTimeout(int msDefaultTimeout)
        {
        this.msDefaultTimeout = msDefaultTimeout;
        }

    public int getMsDefaultTimeout()
        {
        return msDefaultTimeout;
        }

    //----------------------------------------------------------------------------------------------
    // RobotUsbDevice - meta data
    //----------------------------------------------------------------------------------------------

    @Override public USBIdentifiers getUsbIdentifiers()
        {
        return this.usbIdentifiers;
        }

    public void setUsbIdentifiers(USBIdentifiers usbIdentifiers)
        {
        this.usbIdentifiers = usbIdentifiers;
        }

    public void setProductName(@NonNull String productName)
        {
        this.productName = productName;
        }

    @Override @NonNull public String getProductName()
        {
        return productName;
        }
    }
