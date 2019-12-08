/*
Copyright (c) 2017 Robert Atkinson

All rights reserved.

Derived in part from information in various resources, including FTDI, the
Android Linux implementation, FreeBsc, UsbSerial, and others.

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
package org.firstinspires.ftc.robotcore.internal.ftdi;

import android.hardware.usb.UsbEndpoint;
import androidx.annotation.NonNull;

import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.internal.system.FrequentErrorReporter;
import org.firstinspires.ftc.robotcore.internal.usb.exception.RobotUsbException;

import java.util.concurrent.TimeUnit;

/**
 * Thread implementation for receiving bulk data from Usb. Runs at elevated priority.
 */
@SuppressWarnings("WeakerAccess")
public class BulkPacketInWorker extends FtConstants implements Runnable
    {
    public static final String TAG = "BulkPacketInWorker";

    final MonitoredUsbDeviceConnection   usbDeviceConnection;
    final UsbEndpoint                    endpoint;
    final ReadBufferManager              readBufferManager;
    final FtDevice                       ftDevice;
    final int                            msReadTimeout;
    final FrequentErrorReporter<Integer> errorReporter;
    final Object                         trivialInput;

    BulkPacketInWorker(FtDevice ftDevice, ReadBufferManager readBufferManager, MonitoredUsbDeviceConnection usbDeviceConnection, UsbEndpoint endpoint)
        {
        this.ftDevice            = ftDevice;
        this.endpoint            = endpoint;
        this.usbDeviceConnection = usbDeviceConnection;
        this.readBufferManager   = readBufferManager;
        this.msReadTimeout       = this.ftDevice.getDriverParameters().getBulkInReadTimeout();
        this.errorReporter       = new FrequentErrorReporter<Integer>();
        this.trivialInput        = new Object();
        }

    public void awaitTrivialInput(long time, @NonNull TimeUnit unit)
        {
        long ms = unit.toMillis(time);
        synchronized (trivialInput)
            {
            try {
                trivialInput.wait(ms);
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                }
            }
        }

    protected void noteTrivialInput()
        {
        synchronized (trivialInput)
            {
            trivialInput.notifyAll();
            }
        }

    public void run()
        {
        try
            {
            do  {
                // Get a buffer into which to receive some data
                BulkPacketBufferIn packetBuffer = this.readBufferManager.acquireWritableInputBuffer();

                // Try to read some incoming data
                // TODO: this call is NOT interruptable ??!?
                int cbRead = this.usbDeviceConnection.bulkTransfer(this.endpoint, packetBuffer.array(), 0, packetBuffer.capacity(), this.msReadTimeout);
                if (cbRead > 0)
                    {
                    // Got some data : pass it along to our (lower-priority) processor
                    packetBuffer.setCurrentLength(cbRead);
                    this.readBufferManager.releaseReadableBuffer(packetBuffer);
                    if (cbRead <= MODEM_STATUS_SIZE)
                        {
                        noteTrivialInput();
                        }
                    }
                else
                    {
                    // No data received, so put buffer back into the pool
                    packetBuffer.setCurrentLength(0);       // be consistent, helps debugging
                    this.readBufferManager.releaseWritableInputBuffer(packetBuffer);

                    // Log any errors
                    if (cbRead < 0)
                        {
                        this.errorReporter.ee(cbRead, TAG, "%s: bulkTransfer() error: %d", ftDevice.getSerialNumber(), cbRead);
                        }
                    else
                        this.errorReporter.reset();
                    }
                }
            while (!Thread.interrupted());

            throw new InterruptedException();
            }
        catch (InterruptedException interrupt)
            {
            this.readBufferManager.purgeInputData();
            Thread.currentThread().interrupt();
            }
        catch (RuntimeException|RobotUsbException e)
            {
            // RuntimeExceptions are bugs
            // RobotUsbExceptions shouldn't be thrown on reads, as we are doing here
            RobotLog.ee(TAG, e, "unexpected exception");
            }
        }
    }
