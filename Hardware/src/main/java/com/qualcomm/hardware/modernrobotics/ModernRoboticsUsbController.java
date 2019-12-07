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

package com.qualcomm.hardware.modernrobotics;

import android.content.Context;

import com.qualcomm.robotcore.eventloop.SyncdDevice;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.util.SerialNumber;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class provides services common to both Modern Robotics motor and servo controllers
 */
@SuppressWarnings("WeakerAccess")
public abstract class ModernRoboticsUsbController extends ModernRoboticsUsbDevice
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected enum WRITE_STATUS { IDLE, DIRTY, READ };

    protected       WRITE_STATUS            writeStatus;
    protected       boolean                 readWriteRunnableIsRunning;
    protected final AtomicInteger           callbackWaiterCount = new AtomicInteger();
    protected final AtomicLong              readCompletionCount = new AtomicLong();
    // Locking hierarchy is in the order listed
    protected final Object                  concurrentClientLock = new Object();
    protected final Object                  callbackLock         = new Object();

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public ModernRoboticsUsbController(Context context, SerialNumber serialNumber, SyncdDevice.Manager manager, OpenRobotUsbDevice openRobotUsbDevice, CreateReadWriteRunnable createReadWriteRunnable)
            throws RobotCoreException, InterruptedException
        {
        super(context, serialNumber, manager, openRobotUsbDevice, createReadWriteRunnable);
        this.writeStatus                = WRITE_STATUS.IDLE;
        this.readWriteRunnableIsRunning = false;
        }

    //----------------------------------------------------------------------------------------------
    // Reading and writing
    //
    // The key thing here is that we will block reads to wait until any pending writes have
    // completed before issuing, as that guarantees that they will see the effect of the writes.
    // Second, we block *writes* on any pending writes so that we know they've issued.
    //----------------------------------------------------------------------------------------------

    @Override public void write(int address, byte[] data)
        {
        synchronized (this.concurrentClientLock)
            {
            synchronized (this.callbackLock)
                {
                // If there's another write ahead of us, then wait till it
                // has been sent to the USB module before we do our write (which
                // might, for example, set the same registers he was writing to a
                // now different value. Think 'motor mode' as an example).
                boolean abandoned = false;
                while (this.writeStatus == WRITE_STATUS.DIRTY)
                    {
                    if (!this.isArmed() || !waitForCallback())
                        {
                        abandoned = true;
                        break;
                        }
                    }

                if (!abandoned && isOkToReadOrWrite())
                    {
                    // Write the data to the buffer and put off reads and writes until it gets out
                    this.writeStatus = WRITE_STATUS.DIRTY;
                    super.write(address, data);
                    }
                else
                    {
                    // Send write to the bit bucket
                    }
                }
            }
        }

    @Override public byte[] read(int address, int size)
        {
        synchronized (this.concurrentClientLock)
            {
            synchronized (this.callbackLock)
                {
                // Make sure that any read we issue happens *after* any writes
                // that have been send and then *after* we complete a read cycle
                // following thereafter. This is the essence of causality.
                boolean abandoned = false;
                while (this.writeStatus != WRITE_STATUS.IDLE)
                    {
                    if (!this.isArmed() || !waitForCallback())
                        {
                        abandoned = true;
                        break;
                        }
                    }

                if (!abandoned && isOkToReadOrWrite())
                    return super.read(address, size);
                else
                    return new byte[size];
                }
            }
        }

    @Override public void writeComplete() throws InterruptedException
        {
        // Any previously issued writes are now in the hands of the USB module
        synchronized (this.callbackLock)
            {
            super.writeComplete();
            // Make sure we really read after we write before read()s can continue
            if (this.writeStatus == WRITE_STATUS.DIRTY)
                this.writeStatus = WRITE_STATUS.READ;
            this.callbackLock.notifyAll();
            }
        }

    @Override public void readComplete() throws InterruptedException
        {
        synchronized (this.callbackLock)
            {
            super.readComplete();
            if (this.writeStatus==WRITE_STATUS.READ)
                this.writeStatus = WRITE_STATUS.IDLE;
            readCompletionCount.incrementAndGet();
            this.callbackLock.notifyAll();
            }
        }

    // Waits for the next read complete and returns true, or returns false if that's not possible.
    boolean waitForNextReadComplete()
        {
        synchronized (this.concurrentClientLock)
            {
            synchronized (this.callbackLock)
                {
                long cur = this.readCompletionCount.get();
                long target = cur + 1;
                while (this.readCompletionCount.get() < target)
                    {
                    if (!this.isArmed())
                        return false;
                    if (!waitForCallback())
                        return false;     // interrupted or readWriteRunnable is dead, deem us to have completed
                    }
                }
            }
        return true;
        }

    protected boolean isOkToReadOrWrite()
        {
        return this.isArmed() && this.readWriteRunnableIsRunning;
        }

    @Override public void startupComplete()
        {
        this.readWriteRunnableIsRunning = true;
        }

    @Override public void shutdownComplete()
        {
        this.readWriteRunnableIsRunning = false;
        synchronized (this.callbackLock)
            {
            this.writeStatus = WRITE_STATUS.IDLE;
            this.callbackLock.notifyAll();  // wake up any waiter
            }

        // It's important that by here there be no more waiters: that is, everyone
        // who needs to see that readWriteRunnableIsRunning is false will have noted same.
        while (this.callbackWaiterCount.get() > 0)
            Thread.yield();
        }

    /**
     * Waits for a next callback or until interruption or shutdown
     * @return true if a callback occurred, false otherwise
     */
    boolean waitForCallback()
        {
        this.callbackWaiterCount.incrementAndGet();
        boolean interrupted = false;
        if (this.readWriteRunnableIsRunning)
            {
            try {
                callbackLock.wait();
                }
            catch (InterruptedException e)
                {
                interrupted = true;
                Thread.currentThread().interrupt();
                }
            }
        boolean result = !interrupted && this.readWriteRunnableIsRunning;
        this.callbackWaiterCount.decrementAndGet();
        return result;
        }

    }
