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
package org.firstinspires.ftc.robotcore.internal.ftdi;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import androidx.annotation.NonNull;

import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.ThreadPool;

import org.firstinspires.ftc.robotcore.internal.system.DebuggableReentrantLock;
import org.firstinspires.ftc.robotcore.internal.system.WatchdogMonitor;
import org.firstinspires.ftc.robotcore.internal.usb.exception.RobotUsbException;
import org.firstinspires.ftc.robotcore.internal.usb.exception.RobotUsbFTDIException;
import org.firstinspires.ftc.robotcore.internal.usb.exception.RobotUsbStuckUsbWriteException;
import org.firstinspires.ftc.robotcore.internal.usb.exception.RobotUsbUnspecifiedException;
import org.firstinspires.ftc.robotcore.internal.usb.exception.RobotUsbWriteLockException;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * {@link MonitoredUsbDeviceConnection} is a delegator to UsbDeviceConnection that performs
 * additional timeout monitoring to detect stuck calls that we observed being caused by
 * electrostatic discharge events.
 *
 * Note that we only implement the subset of the {@link UsbDeviceConnection} API that we actually use.
 */
@SuppressWarnings({"WeakerAccess", "UnnecessaryLocalVariable"})
public class MonitoredUsbDeviceConnection
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "MonitoredUsbDeviceConnection";

    protected final @NonNull FtDevice               ftDevice;
    protected final @NonNull UsbDeviceConnection    delegate;
    protected final @NonNull String                 serialNumber;

    // protected    Set<UsbInterface>               interfacesClaimed = new HashSet<UsbInterface>();

    protected final WatchdogMonitor                 monitor = new WatchdogMonitor();
    protected       int                             msUsbWriteDurationMax = 200;
    protected       int                             msUsbWriteLockAcquire = 250;  // finger in the wind
    protected final static DebuggableReentrantLock  usbWriteLock = new DebuggableReentrantLock();

    protected enum FailureType { UNKNOWN, WRITE, CONTROL_TRANSFER };
    protected       Callable<RobotUsbException>     failureAction;
    protected       FailureType                     failureType = FailureType.UNKNOWN;

    protected       Callable<RobotUsbException>     bulkTransferAction;
    protected       UsbEndpoint                     endpoint;
    protected       byte[]                          buffer;
    protected       int                             offset;
    protected       int                             length;
    protected       int                             timeout;

    protected       Callable<RobotUsbException>     controlTransferAction;
    protected       int                             requestType;
    protected       int                             request;
    protected       int                             value;
    protected       int                             index;
    protected       int                             callResult;


    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public MonitoredUsbDeviceConnection(@NonNull FtDevice ftDevice, @NonNull UsbDeviceConnection delegate)
        {
        this.ftDevice = ftDevice;
        this.delegate = delegate;
        this.serialNumber = delegate.getSerial();
        initializeMonitoring();
        }

    public void close()
        {
        // Assert.assertTrue(interfacesClaimed.isEmpty());
        RobotLog.vv(TAG, "closing UsbDeviceConnection(%s)", serialNumber);
        delegate.close();
        monitor.close(false);   // false since we might be closing the FtDevice due while on failureAction (see XYZZY below): it will soon terminate anyway
        }

    //----------------------------------------------------------------------------------------------
    // Monitoring
    //----------------------------------------------------------------------------------------------

    private boolean acquireUsbWriteLock() throws InterruptedException
        {
        return (usbWriteLock.tryLock() || usbWriteLock.tryLock(msUsbWriteLockAcquire, TimeUnit.MILLISECONDS));
        }

    private void releaseUsbWriteLock()
        {
        usbWriteLock.unlock();
        }

    protected void initializeMonitoring()
        {
        // We allocate ahead of time rather than on each call in order to ease
        // pressure on the garbage collector.

        bulkTransferAction = new Callable<RobotUsbException>()
            {
            @Override public RobotUsbException call()
                {
                callResult = delegate.bulkTransfer(endpoint, buffer, offset, length, timeout);
                return null;
                }
            };

        controlTransferAction = new Callable<RobotUsbException>()
            {
            @Override public RobotUsbException call()
                {
                callResult = delegate.controlTransfer(requestType, request, value, index, buffer, offset, length, timeout);;
                return null;
                }
            };

        failureAction = new Callable<RobotUsbException>()
            {
            @Override public RobotUsbException call()
                {
                // We close the device in the hopes of waking up the call. We return a
                // distinguished exception so that folks can in fact KNOW we closed the device. We
                // also set up so that a read that detects the closed device will return the same sort
                // of 'please try again' error rather than its usual 'i give up'.
                //
                // Note: closing in this way *does* seem to unstick the call. However, we've rarely/never
                // been able to successfully re-open after that, even after waiting huge amounts of
                // time (like a second). Rather, we get stuck in trying to set the baud rate, inside of
                // a different native call (UsbDeviceConnection.native_control_request).
                //
                Thread monitoredThread = monitor.getMonitoredThread();
                String threadMessage = monitoredThread == null ? "" : String.format(" threadId=%d TID=%d:", monitoredThread.getId(), ThreadPool.getTID(monitoredThread));
                String failureMessage;
                switch (failureType)
                    {
                    default:                failureMessage = "unknown failure"; break;
                    case WRITE:             failureMessage = String.format("write(%d bytes)", length); break;
                    case CONTROL_TRANSFER:  failureMessage = String.format("control(%d bytes)", length); break;
                    }
                RobotLog.ee(TAG, "watchdog: stuck USB %s%s: serial=%s closing device", failureMessage, threadMessage, serialNumber, threadMessage);
                RobotUsbException deviceClosedReason = new RobotUsbStuckUsbWriteException(delegate, "watchdog: stuck USB %s: closed %s", failureMessage, serialNumber);
                ftDevice.setDeviceClosedReason(deviceClosedReason);
                ftDevice.close();           // XYZZY
                return deviceClosedReason;
                }
            };
        }

    private RobotUsbException unableToAcquireWriteLockException()
        {
        Thread owner = usbWriteLock.getOwner();
        String threadMessage = owner == null ? "" : String.format(": owner: id=%d TID=%d name=%s", owner.getId(), ThreadPool.getTID(owner), owner.getName());
        return new RobotUsbWriteLockException("unable to acquire usb write lock after %d ms%s", msUsbWriteLockAcquire, threadMessage);
        }

    //----------------------------------------------------------------------------------------------
    // Delegation
    //----------------------------------------------------------------------------------------------

    public boolean claimInterface(UsbInterface intf, boolean force)
        {
        // Assert.assertFalse(interfacesClaimed.contains(intf));
        boolean result = delegate.claimInterface(intf, force);
        // if (result) interfacesClaimed.add(intf);
        return result;
        }

    public boolean releaseInterface(UsbInterface intf)
        {
        // Assert.assertTrue(interfacesClaimed.contains(intf));
        boolean result = delegate.releaseInterface(intf);
        // interfacesClaimed.remove(intf);
        return result;
        }

    public byte[] getRawDescriptors()
        {
        return delegate.getRawDescriptors();
        }

    public int controlTransfer(int requestType, int request, int value, int index, byte[] buffer, int length, int timeout) throws RobotUsbException
        {
        return controlTransfer(requestType, request, value, index, buffer, 0, length, timeout);
        }

    public int controlTransfer(int requestType, int request, int value, int index, byte[] buffer, int offset, int length, int timeout) throws RobotUsbException
        {
        synchronized (monitor) // concurrency paranoia
            {
            this.callResult = FtDevice.RC_PARANOIA;

            try {
                if (acquireUsbWriteLock())
                    {
                    try {
                        this.requestType = requestType;
                        this.request     = request;
                        this.value       = value;
                        this.index       = index;
                        this.buffer      = buffer;
                        this.offset      = offset;
                        this.length      = length;
                        this.timeout     = timeout;

                        failureType = FailureType.CONTROL_TRANSFER;

                        RobotUsbException timedOutException = monitor.monitor(
                                    controlTransferAction,
                                    failureAction,
                                    msUsbWriteDurationMax, TimeUnit.MILLISECONDS);

                        if (timedOutException != null)
                            {
                            throw timedOutException;
                            }
                        }
                    catch (ExecutionException | CancellationException e)
                        {
                        // We should have ruled out all of these cases with the above logic
                        throw RobotUsbUnspecifiedException.createChained(e, "control transfer: internal error: unexpected exception from future");
                        }
                    finally
                        {
                        releaseUsbWriteLock();
                        }
                    }
                else
                    {
                    throw unableToAcquireWriteLockException();
                    }

                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                }
            catch (RuntimeException e)
                {
                throw RobotUsbFTDIException.createChained(e, "runtime exception %s during controlTransfer() of %d bytes on %s", e.getClass().getSimpleName(), length, serialNumber);
                }

            return callResult;
            }
        }

    public int bulkTransfer(final UsbEndpoint endpoint, final byte[] buffer, final int offset, final int length, final int timeout) throws InterruptedException, RobotUsbException
        {
        if (endpoint.getDirection()==UsbConstants.USB_DIR_IN)
            {
            // read : we don't monitor these
            return delegate.bulkTransfer(endpoint, buffer, offset, length, timeout);
            }
        else
            {
            // write
            try {
                synchronized (monitor) // concurrency paranoia
                    {
                    this.callResult = FtDevice.RC_PARANOIA;

                    if (acquireUsbWriteLock())
                        {
                        try {
                            this.endpoint = endpoint;
                            this.buffer = buffer;
                            this.offset = offset;
                            this.length = length;
                            this.timeout = timeout;
                            //
                            failureType = FailureType.WRITE;
                            RobotUsbException timedOutException = monitor.monitor(
                                    bulkTransferAction,
                                    failureAction,
                                    msUsbWriteDurationMax, TimeUnit.MILLISECONDS);

                            if (timedOutException != null)
                                {
                                throw timedOutException;
                                }
                            }
                        catch (ExecutionException | CancellationException e)
                            {
                            // We should have ruled out all of these cases with the above logic
                            throw RobotUsbUnspecifiedException.createChained(e, "write: internal error: unexpected exception from future");
                            }
                        finally
                            {
                            releaseUsbWriteLock();
                            }
                        }
                    else
                        {
                        throw unableToAcquireWriteLockException();
                        }
                    //
                    return callResult;
                    }
                }
            catch (RuntimeException e)
                {
                throw RobotUsbFTDIException.createChained(e, "runtime exception %s during write() of %d bytes on %s", e.getClass().getSimpleName(), length, serialNumber);
                }
            }
        }
    }
