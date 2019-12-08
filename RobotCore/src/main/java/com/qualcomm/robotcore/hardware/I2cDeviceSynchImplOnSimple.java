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
package com.qualcomm.robotcore.hardware;

import androidx.annotation.Nullable;

import com.qualcomm.robotcore.util.ThreadPool;

import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * I2cDeviceSynchImplOnSimple takes an I2cDeviceSynchSimple and adds to it heartbeat and
 * readwindow functionality.
 */
@SuppressWarnings("WeakerAccess")
public class I2cDeviceSynchImplOnSimple extends I2cDeviceSynchReadHistoryImpl implements I2cDeviceSynch
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected I2cDeviceSynchSimple      i2cDeviceSynchSimple;
    protected I2cDeviceSynchReadHistory i2cDeviceSynchSimpleHistory;
    protected boolean                   isSimpleOwned;

    protected int                   iregReadLast, cregReadLast;
    protected int                   iregWriteLast;
    protected byte[]                rgbWriteLast;

    protected boolean               isHooked;                   // whether we are connected to the underling device or not
    protected boolean               isEngaged;                  // user's hooking *intent*
    protected boolean               isClosing;

    protected int                   msHeartbeatInterval;        // time between heartbeats; zero is 'none necessary'
    protected HeartbeatAction       heartbeatAction;            // the action to take when a heartbeat is needed. May be null.
    protected ScheduledExecutorService heartbeatExecutor;       // used to schedule heartbeats when we need to read from the outside

    protected final Object          engagementLock = new Object();
    protected final Object          concurrentClientLock = new Object(); // the lock we use to serialize against concurrent clients of us.

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public I2cDeviceSynchImplOnSimple(I2cDeviceSynchSimple simple, boolean isSimpleOwned)
        {
        this.i2cDeviceSynchSimple        = simple;
        this.i2cDeviceSynchSimpleHistory = (simple instanceof I2cDeviceSynchReadHistory) ? ((I2cDeviceSynchReadHistory)simple) : null;
        this.isSimpleOwned          = isSimpleOwned;

        this.msHeartbeatInterval    = 0;
        this.heartbeatAction        = null;
        this.heartbeatExecutor      = null;
        this.cregReadLast           = 0;
        this.rgbWriteLast           = null;
        this.isEngaged              = false;
        this.isHooked               = false;
        this.isClosing              = false;
        }

    //----------------------------------------------------------------------------------------------
    // Debugging
    //----------------------------------------------------------------------------------------------

    @Override
    public void setUserConfiguredName(@Nullable String name)
        {
        this.i2cDeviceSynchSimple.setUserConfiguredName(name);
        }

    @Override
    @Nullable public String getUserConfiguredName()
        {
        return this.i2cDeviceSynchSimple.getUserConfiguredName();
        }

    @Override
    public void setLogging(boolean enabled)
        {
        this.i2cDeviceSynchSimple.setLogging(enabled);
        }

    @Override public boolean getLogging()
        {
        return this.i2cDeviceSynchSimple.getLogging();
        }

    @Override
    public void setLoggingTag(String loggingTag)
        {
        this.i2cDeviceSynchSimple.setLoggingTag(loggingTag);
        }

    @Override public String getLoggingTag()
        {
        return this.i2cDeviceSynchSimple.getLoggingTag();
        }

    //----------------------------------------------------------------------------------------------
    // Engagable
    //----------------------------------------------------------------------------------------------

    @Override public void engage()
        {
        // The engagement lock is distinct from the concurrentClientLock because we need to be
        // able to drain heartbeats while disarming, so can't own the concurrentClientLock then,
        // but we still need to be able to lock out engage() and disengage() against each other.
        // Locking order: armingLock > concurrentClientLock > callbackLock
        //
        synchronized (this.engagementLock)
            {
            this.isEngaged = true;
            adjustHooking();
            }
        }

    protected void hook()
        {
        // engagementLock is distinct from the concurrentClientLock because we need to be
        // able to drain heartbeats while disarming, so can't own the concurrentClientLock then,
        // but we still need to be able to lock out engage() and disengage() against each other.
        // Locking order: engagementLock > concurrentClientLock
        //
        synchronized (this.engagementLock)
            {
            if (!this.isHooked)
                {
                startHeartBeat();
                this.isHooked = true;
                }
            }
        }

    /* adjust the hooking state to reflect the user's engagement intent */
    protected void adjustHooking()
        {
        synchronized (this.engagementLock)
            {
            if (!this.isHooked && this.isEngaged)
                this.hook();
            else if (this.isHooked && !this.isEngaged)
                this.unhook();
            }
        }

    @Override public boolean isEngaged()
        {
        return this.isEngaged;
        }

    @Override public boolean isArmed()
        {
        synchronized (this.engagementLock)
            {
            if (this.isHooked)
                {
                return this.i2cDeviceSynchSimple.isArmed();
                }
            return false;
            }
        }

    @Override public void disengage()
        {
        synchronized (this.engagementLock)
            {
            this.isEngaged = false;
            this.adjustHooking();
            }
        }

    protected void unhook()
        {
        synchronized (this.engagementLock)
            {
            if (this.isHooked)
                {
                stopHeartBeat();
                synchronized (concurrentClientLock)
                    {
                    waitForWriteCompletions(I2cWaitControl.ATOMIC);
                    this.isHooked = false;
                    }
                }
            }
        }

    //----------------------------------------------------------------------------------------------
    // Heartbeats
    //----------------------------------------------------------------------------------------------

    @Override
    public void setHeartbeatInterval(int ms)
        {
        synchronized (concurrentClientLock)
            {
            this.msHeartbeatInterval = Math.max(0, msHeartbeatInterval);
            stopHeartBeat();
            startHeartBeat();
            }
        }

    @Override
    public int getHeartbeatInterval()
        {
        return this.msHeartbeatInterval;
        }

    @Override
    public void setHeartbeatAction(HeartbeatAction action)
        {
        this.heartbeatAction = action;
        }

    @Override
    public HeartbeatAction getHeartbeatAction()
        {
        return this.heartbeatAction;
        }

    void startHeartBeat()
        {
        if (this.msHeartbeatInterval > 0)
            {
            this.heartbeatExecutor = ThreadPool.newScheduledExecutor(1, "I2cDeviceSyncImplOnSimple heartbeat");
            this.heartbeatExecutor.scheduleAtFixedRate(new Runnable()
                {
                @Override public void run()
                    {
                    HeartbeatAction action = getHeartbeatAction();
                    if (action != null)
                        {
                        synchronized (concurrentClientLock)
                            {
                            if (action.rereadLastRead && cregReadLast != 0)
                                {
                                read(iregReadLast, cregReadLast);
                                return;
                                }
                            if (action.rewriteLastWritten && rgbWriteLast != null)
                                {
                                write(iregWriteLast, rgbWriteLast);
                                return;
                                }
                            if (action.heartbeatReadWindow != null)
                                {
                                read(action.heartbeatReadWindow.getRegisterFirst(), action.heartbeatReadWindow.getRegisterCount());
                                }
                            }
                        }
                    }
                }, 0, this.msHeartbeatInterval, TimeUnit.MILLISECONDS);
            }
        }

    void stopHeartBeat()
        {
        if (this.heartbeatExecutor != null)
            {
            this.heartbeatExecutor.shutdownNow();
            ThreadPool.awaitTerminationOrExitApplication(this.heartbeatExecutor, 2, TimeUnit.SECONDS, "heartbeat executor", "internal error");
            this.heartbeatExecutor = null;
            }
        }

    //----------------------------------------------------------------------------------------------
    // Read window
    //----------------------------------------------------------------------------------------------

    @Override
    public void setReadWindow(ReadWindow window)
        {

        }

    @Override
    public ReadWindow getReadWindow()
        {
            return null;
        }

    @Override
    public void ensureReadWindow(ReadWindow windowNeeded, ReadWindow windowToSet)
        {

        }

    @Override
    public TimestampedData readTimeStamped(int ireg, int creg, ReadWindow readWindowNeeded, ReadWindow readWindowSet)
        {
        return readTimeStamped(ireg, creg);
        }

    //----------------------------------------------------------------------------------------------
    // HardwareDevice
    //----------------------------------------------------------------------------------------------

    @Override
    public Manufacturer getManufacturer()
        {
        return this.i2cDeviceSynchSimple.getManufacturer();
        }

    @Override
    public String getDeviceName()
        {
        return this.i2cDeviceSynchSimple.getDeviceName();
        }

    @Override
    public String getConnectionInfo()
        {
        return this.i2cDeviceSynchSimple.getConnectionInfo();
        }

    @Override
    public int getVersion()
        {
        return this.i2cDeviceSynchSimple.getVersion();
        }

    @Override
    public void resetDeviceConfigurationForOpMode()
        {
        this.i2cDeviceSynchSimple.resetDeviceConfigurationForOpMode();
        // TODO: more to come
        }

    @Override
    public void close()
        {
        this.isClosing = true;
        disengage();
        if (this.isSimpleOwned)
            {
            this.i2cDeviceSynchSimple.close();
            }
        }

    //----------------------------------------------------------------------------------------------
    // HardwareDeviceHealth
    //----------------------------------------------------------------------------------------------

    @Override public void setHealthStatus(HealthStatus status)
        {
        i2cDeviceSynchSimple.setHealthStatus(status);
        }

    @Override public HealthStatus getHealthStatus()
        {
        return i2cDeviceSynchSimple.getHealthStatus();
        }

    //----------------------------------------------------------------------------------------------
    // I2cDeviceSyncReadHistory implementation: we delegate to our wrappee if we can, as he
    // can more accurately track what was really read, but if not, we'll do it ourselves.
    //----------------------------------------------------------------------------------------------

    @Override public BlockingQueue<TimestampedI2cData> getHistoryQueue()
        {
        return i2cDeviceSynchSimpleHistory==null
                ? super.getHistoryQueue()
                : i2cDeviceSynchSimpleHistory.getHistoryQueue();
        }

    @Override public void setHistoryQueueCapacity(int capacity)
        {
        if (i2cDeviceSynchSimpleHistory==null)
            super.setHistoryQueueCapacity(capacity);
        else
            i2cDeviceSynchSimpleHistory.setHistoryQueueCapacity(capacity);
        }

    @Override public int getHistoryQueueCapacity()
        {
        return i2cDeviceSynchSimpleHistory==null
                ? super.getHistoryQueueCapacity()
                : i2cDeviceSynchSimpleHistory.getHistoryQueueCapacity();
        }

    @Override public void addToHistoryQueue(TimestampedI2cData data)
        {
        if (i2cDeviceSynchSimpleHistory==null)
            super.addToHistoryQueue(data);
        else
            {
            // We don't keep our own history: our wrappee does it for us instead
            }
        }

    //----------------------------------------------------------------------------------------------
    // I2cDeviceSynch pass through
    //----------------------------------------------------------------------------------------------

    protected boolean isOpenForReading()
        {
        return this.isHooked && this.newReadsAndWritesAllowed();
        }
    protected boolean isOpenForWriting()
        {
        return this.isHooked && this.newReadsAndWritesAllowed();
        }
    protected boolean newReadsAndWritesAllowed()
        {
        return !this.isClosing;
        }

    @Override public void setI2cAddress(I2cAddr newAddress)
        {
        setI2cAddr(newAddress);
        }

    @Override public I2cAddr getI2cAddress()
        {
        return getI2cAddr();
        }

    @Override
    public void setI2cAddr(I2cAddr i2cAddr)
        {
        this.i2cDeviceSynchSimple.setI2cAddress(i2cAddr);
        }

    @Override
    public I2cAddr getI2cAddr()
        {
        return this.i2cDeviceSynchSimple.getI2cAddress();
        }

    @Override
    public synchronized byte read8(int ireg)
        {
        return read(ireg, 1)[0];
        }

    @Override
    public byte[] read(int ireg, int creg)
        {
        return readTimeStamped(ireg,creg).data;
        }

    @Override
    public TimestampedData readTimeStamped(int ireg, int creg)
        {
        synchronized (concurrentClientLock)
            {
            if (!this.isOpenForReading())
                return TimestampedI2cData.makeFakeData(null, getI2cAddress(), ireg, creg);

            this.iregReadLast = ireg;
            this.cregReadLast = creg;

            TimestampedI2cData result = new TimestampedI2cData();
            result.i2cAddr  = this.getI2cAddress();
            result.register = ireg;

            TimestampedData justData = this.i2cDeviceSynchSimple.readTimeStamped(ireg, creg);
            result.data       = justData.data;
            result.nanoTime   = justData.nanoTime;

            addToHistoryQueue(result);
            return result;
            }
        }

    @Override
    public void write8(int ireg, int bVal)
        {
        write8(ireg, bVal, I2cWaitControl.ATOMIC);
        }

    @Override
    public void write8(int ireg, int bVal, I2cWaitControl waitControl)
        {
        write(ireg, new byte[] { (byte)bVal }, waitControl);
        }

    @Override
    public void write(int ireg, byte[] data)
        {
        write(ireg, data, I2cWaitControl.ATOMIC);
        }

    @Override
    public void write(int ireg, byte[] data, I2cWaitControl waitControl)
        {
        synchronized (concurrentClientLock)
            {
            if (!isOpenForWriting())
                return; // Ignore the write

            this.iregWriteLast = ireg;
            this.rgbWriteLast = Arrays.copyOf(data, data.length);
            this.i2cDeviceSynchSimple.write(ireg, data, waitControl);
            }
        }

    @Override
    public void waitForWriteCompletions(I2cWaitControl waitControl)
        {
        this.i2cDeviceSynchSimple.waitForWriteCompletions(waitControl);
        }

    @Override
    public void enableWriteCoalescing(boolean enable)
        {
        this.i2cDeviceSynchSimple.enableWriteCoalescing(enable);
        }

    @Override
    public boolean isWriteCoalescingEnabled()
        {
        return this.i2cDeviceSynchSimple.isWriteCoalescingEnabled();
        }
    }
