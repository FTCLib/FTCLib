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

import static org.firstinspires.ftc.robotcore.internal.system.Assert.assertFalse;
import static org.firstinspires.ftc.robotcore.internal.system.Assert.assertTrue;

import androidx.annotation.Nullable;
import android.util.Log;

import com.qualcomm.robotcore.hardware.usb.RobotArmingStateNotifier;
import com.qualcomm.robotcore.hardware.usb.RobotUsbModule;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.ThreadPool;

import org.firstinspires.ftc.robotcore.internal.hardware.TimeWindow;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * {@link I2cDeviceSynchImpl} is a utility class that makes it easy to read or write data to
 * an instance of {@link I2cDevice}. Its functionality is exposed through the {@link I2cDeviceSynch}
 * interface. Please see that interface, and the {@link I2cDeviceSynchImpl} constructor here, for
 * further information.
 *
 * @see I2cDeviceSynchImpl#I2cDeviceSynchImpl(I2cDevice, I2cAddr, boolean)
 * @see I2cDeviceSynch
 * @see I2cDevice
 */
@SuppressWarnings("WeakerAccess")
public final class I2cDeviceSynchImpl extends I2cDeviceSynchReadHistoryImpl implements I2cDeviceSynch, Engagable
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected       I2cAddr        i2cAddr;                   // the address we communicate with on the I2C bus
    protected       I2cDevice      i2cDevice;                 // the device we are talking to
    protected       boolean        isI2cDeviceOwned;          // do we own the i2cDevice, or are we simply its client
    protected       I2cController  controller;                // that device's controller
    protected       boolean        isControllerLegacy;        // is the controller a legacy module?
    protected       HardwareDeviceHealthImpl hardwareDeviceHealth; // keeps track of whether we are healthy or not
    protected       RobotUsbModule robotUsbModule;            // if the controller is a RobotUsbModule, then then him cast to this type

    protected       boolean       isHooked;                   // whether we are connected to the underling device or not
    protected       boolean       isEngaged;                  // user's hooking *intent*
    protected       AtomicInteger readerWriterPreventionCount;// used to be able to prevent new readers and writers
    protected       ReadWriteLock readerWriterGate;           // used to to drain extant readers and writers. We ought not to need this (concurrentClientLock ought to suffice), but it certainly works.
    protected       AtomicInteger readerWriterCount;          // for debugging
    protected       boolean       isClosing;                  // are we in the process of closing this device synch

    protected       Callback      callback;                   // the callback object on which we actually receive callbacks
    protected       boolean       loggingEnabled;             // whether we are to log to Logcat or not
    protected       String        loggingTag;                 // what we annotate our logging with
    protected @Nullable String    name;                       // optional user recognizable name of this device
    protected       ElapsedTime   timeSinceLastHeartbeat;     // keeps track of our need for doing heartbeats

    protected       TimeWindow    readCacheTimeWindow;        // timestamps written here when read cache segment is updated
    protected       byte[]        readCache;                  // the buffer into which reads are retrieved
    protected       byte[]        writeCache;                 // the buffer that we write from
    protected static final int    dibCacheOverhead = 4;       // this many bytes at start of writeCache are system overhead
    protected       Lock          readCacheLock;              // lock we must hold to look at readCache
    protected       Lock          writeCacheLock;             // lock we must old to look at writeCache
    protected static final int    msCallbackLockWaitQuantum = 60;      // arbitrary, but long enough that we don't hit it in common usage, only in shutdown situations
    protected static final int    msCallbackLockAbandon = 500;         // if we don't see a callback in this amount of time, then something seriously has gone wrong
    protected       boolean       isWriteCoalescingEnabled;            // are we allowed to coalesce adjacent writes, or must we issue them in order separately?

    protected final Object        engagementLock       = new Object();
    protected final Object        concurrentClientLock = new Object(); // the lock we use to serialize against concurrent clients of us. Can't acquire this AFTER the callback lock.
    protected final Object        callbackLock         = new Object(); // the lock we use to synchronize with our callback.

    protected          boolean             disableReadWindows;         // if true, then setReadWindow is effectively disabled. this is an internal debugging aid only.
    protected volatile ReadWindow          readWindow;                 // the set of registers to look at when we are in read mode. May be null, indicating no read needed
    protected volatile ReadWindow          readWindowActuallyRead;     // the read window that was really read. readWindow will be a (possibly non-proper) subset of this
    protected volatile ReadWindow          readWindowSentToController; // the read window we last issued to the controller module. May disappear before read() returns
    protected volatile boolean             isReadWindowSentToControllerInitialized; // whether readWindowSentToController has valid data or not
    protected volatile boolean             hasReadWindowChanged;       // whether regWindow has changed since the hw cycle loop last took note
    protected volatile long                nanoTimeReadCacheValid;     // the time on the System.nanoTime() clock at which the read cache was last set as valid
    protected volatile READ_CACHE_STATUS   readCacheStatus;            // what we know about the contents of readCache
    protected final    WriteCacheStatus    writeCacheStatus;           // what we know about the (payload) contents of writeCache
    protected volatile CONTROLLER_PORT_MODE controllerPortMode;        // what we know about the controller's read vs write status on the port we use
    protected volatile int                 iregWriteFirst;             // when writeCacheStatus is DIRTY, this is where we want to write
    protected volatile int                 cregWrite;
    protected volatile int                 msHeartbeatInterval;        // time between heartbeats; zero is 'none necessary'
    protected volatile HeartbeatAction     heartbeatAction;            // the action to take when a heartbeat is needed. May be null.
    protected volatile ExecutorService     heartbeatExecutor;          // used to schedule heartbeats when we need to read from the outside

    protected class WriteCacheStatus
        {
        private volatile WRITE_CACHE_STATUS status       = WRITE_CACHE_STATUS.IDLE;
        private volatile long               nanoTimeIdle = 0;

        public void setStatus(WRITE_CACHE_STATUS status)
            {
            synchronized (callback)
                {
                boolean wasIdle = this.status == WRITE_CACHE_STATUS.IDLE;
                this.status = status;
                boolean isIdle = this.status == WRITE_CACHE_STATUS.IDLE;
                if (!wasIdle && isIdle)
                    {
                    this.nanoTimeIdle = System.nanoTime();
                    }
                }
            }

        public void initStatus(WRITE_CACHE_STATUS status)
            {
            synchronized (callback)
                {
                this.status = status;
                this.nanoTimeIdle = 0;
                }
            }

        public WRITE_CACHE_STATUS getStatus()
            {
            return status;
            }

        /**
         * Waits for the write cache to become idle. But that doesn't come within a totally unreasonable
         * amount of time, we're going to assume that our ReadWriteRunnable thread is either stuck or
         * is dead, and we're going to get out of here.
         *
         * @return the time on the nanotime clock clock at which the idle transition occurred */
        public long waitForIdle() throws InterruptedException   // TODO: TimeoutException would be a better choice
            {
            synchronized (callbackLock)
                {
                ElapsedTime timer = null;
                while (getWriteCacheStatus() != WRITE_CACHE_STATUS.IDLE)
                    {
                    if (timer == null) timer = new ElapsedTime();
                    if (timer.milliseconds() > msCallbackLockAbandon)
                        throw new InterruptedException();
                    callbackLock.wait(msCallbackLockWaitQuantum);
                    }
                return nanoTimeIdle;
                }
            }

        }

    /* Keeps track of what we know about about the state of 'readCache' */
    protected enum READ_CACHE_STATUS
        {
        IDLE,                 // the read cache is quiescent; it doesn't contain valid data
        SWITCHINGTOREADMODE,  // a request to switch to read mode has been made (used in Legacy Module only)
        QUEUED,               // an I2C read has been queued, but we've not yet seen valid data
        QUEUE_COMPLETED,      // a transient state only ever seen within the callback
        VALID_ONLYONCE,       // read cache data has valid data but can only be read once
        VALID_QUEUED;         // read cache has valid data AND a read has been queued

        boolean isValid()
            {
            return this==VALID_QUEUED || this==VALID_ONLYONCE;
            }
        boolean isQueued()
            {
            return this==QUEUED || this==VALID_QUEUED;
            }
        }

    /* Keeps track about what we know about the state of 'writeCache' */
    protected enum WRITE_CACHE_STATUS
        {
        IDLE,               // write cache is quiescent
        DIRTY,              // write cache has changed bits that need to be pushed to module
        QUEUED,             // write cache is currently being written to module, not yet returned
        }

    /* Keeps track of what we know about the state of the controller's read vs write modality on our port */
    protected enum CONTROLLER_PORT_MODE
        {
        UNKNOWN,             // we don't know anything about the controller
        WRITE,               // the controller is in write mode
        SWITCHINGTOREADMODE, // the controller is transitioning to read mode: at the next
                             // portIsReady() callback, it will be there (used in Legacy Module only)
        READ                 // the port is in read mode, and can accept reads on the port data
        };

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    /**
     * Instantiate an {@link I2cDeviceSynchImpl} instance on the indicated {@link I2cDevice}
     * using the indicated I2C address.
     *
     * @param i2cDevice         the {@link I2cDevice} that the new {@link I2cDeviceSynchImpl} is
     *                          to be a client of
     * @param i2cAddr           the I2C address to which communications will be targeted
     * @param isI2cDeviceOwned  If true, then when this {@link I2cDeviceSynchImpl} closes, the
     *                          underlying {@link I2cDevice} is closed as well; otherwise, it is
     *                          not. Typically, if the provided {@link I2cDevice} is retrieved from
     *                          an OpMode's hardware map, one passes false to {@link #isI2cDeviceOwned},
     *                          as such @link I2cDevice}s should remain functional across multiple
     *                          OpMode invocations.
     * @see I2cDeviceSynch#close()
     * @see I2cDevice#close()
     */
    public I2cDeviceSynchImpl(I2cDevice i2cDevice, I2cAddr i2cAddr, boolean isI2cDeviceOwned)
        {
        this.loggingTag             = String.format("%s:i2cSynch(%s)", RobotLog.TAG, i2cDevice.getConnectionInfo());;
        this.i2cAddr                = i2cAddr;

        this.i2cDevice              = i2cDevice;
        this.isI2cDeviceOwned       = isI2cDeviceOwned;
        this.controller             = i2cDevice.getI2cController();
        this.isControllerLegacy     = this.controller instanceof LegacyModule;
        this.hardwareDeviceHealth   = new HardwareDeviceHealthImpl(loggingTag);
        this.isEngaged              = false;
        this.isClosing              = false;
        this.isHooked               = false;
        this.readerWriterPreventionCount = new AtomicInteger(0);
        this.readerWriterGate       = new ReentrantReadWriteLock();
        this.readerWriterCount      = new AtomicInteger(0);
        this.callback               = new Callback();
        this.timeSinceLastHeartbeat = new ElapsedTime();
        this.timeSinceLastHeartbeat.reset();
        this.msHeartbeatInterval    = 0;
        this.heartbeatAction        = null;
        this.heartbeatExecutor      = null;
        this.isWriteCoalescingEnabled = false;
        this.loggingEnabled         = false;
        this.disableReadWindows     = false;
        this.readWindow             = null;
        this.writeCacheStatus       = new WriteCacheStatus();

        if (this.controller instanceof RobotUsbModule)
            {
            this.robotUsbModule = (RobotUsbModule)this.controller;
            this.robotUsbModule.registerCallback(this.callback, false);
            }
        else
            throw new IllegalArgumentException("I2cController must also be a RobotUsbModule");

        this.i2cDevice.registerForPortReadyBeginEndCallback(this.callback);
        }

    /**
     * Instantiates an {@link I2cDeviceSynchImpl} instance on the indicated {@link I2cDevice}.
     * When this constructor is used, {@link #setI2cAddress(I2cAddr)} must be called later in order
     * for the instance to be functional.
     * @see #I2cDeviceSynchImpl(I2cDevice, I2cAddr, boolean)
     */
    public I2cDeviceSynchImpl(I2cDevice i2cDevice, boolean isI2cDeviceOwned)
        {
        this(i2cDevice, I2cAddr.zero(), isI2cDeviceOwned);
        }

    void attachToController()
    // All the state that we maintain that is tied to the state of our controller
        {
        this.readCacheTimeWindow = this.i2cDevice.getI2cReadCacheTimeWindow();
        this.readCache = this.i2cDevice.getI2cReadCache();
        this.readCacheLock = this.i2cDevice.getI2cReadCacheLock();
        this.writeCache = this.i2cDevice.getI2cWriteCache();
        this.writeCacheLock = this.i2cDevice.getI2cWriteCacheLock();

        resetControllerState();
        }

    void resetControllerState()
        {
        this.nanoTimeReadCacheValid = 0;
        this.readCacheStatus = READ_CACHE_STATUS.IDLE;
        this.initWriteCacheStatus(WRITE_CACHE_STATUS.IDLE);
        this.controllerPortMode = CONTROLLER_PORT_MODE.UNKNOWN;

        this.readWindowActuallyRead = null;
        this.readWindowSentToController = null;
        this.isReadWindowSentToControllerInitialized = false;

        // So the callback will do it's thing to refresh based on the now-current window
        this.hasReadWindowChanged = true;
        }

    @Override @Deprecated public void setI2cAddr(I2cAddr newAddress)
        {
        setI2cAddress(newAddress);
        }

    @Override public void setI2cAddress(I2cAddr i2cAddr)
        {
        synchronized (this.engagementLock)
            {
            if (this.i2cAddr.get7Bit() != i2cAddr.get7Bit())
                {
                boolean wasHooked = this.isHooked;
                this.disengage();
                //
                this.i2cAddr = i2cAddr;
                //
                if (wasHooked) this.engage();
                }
            }
        }

    @Override public I2cAddr getI2cAddress()
        {
        // Note: locking on engagementLock here is tempting, but will cause deadlock in shutdowns
        return this.i2cAddr;
        }

    @Override @Deprecated public I2cAddr getI2cAddr()
        {
        return getI2cAddress();
        }

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
        // Locking order: engagementLock > concurrentClientLock > callbackLock
        //
        synchronized (this.engagementLock)
            {
            if (!this.isHooked)
                {
                log(Log.VERBOSE, "hooking ...");
                //
                synchronized (this.callbackLock)
                    {
                    this.heartbeatExecutor = ThreadPool.newSingleThreadExecutor("I2cDeviceSyncImpl heartbeat");
                    this.i2cDevice.registerForI2cPortReadyCallback(this.callback);
                    }
                this.isHooked = true;
                //
                log(Log.VERBOSE, "... hooking complete");
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
                return this.i2cDevice.isArmed();
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
        try {
            synchronized (this.engagementLock)
                {
                if (this.isHooked)
                    {
                    log(Log.VERBOSE, "unhooking ...");

                    // We can't hold the concurrent client lock while we drain the heartbeat
                    // as that might be doing an external top-level read. But the semantic of
                    // Executors guarantees us this call returns any actions we've scheduled
                    // have in fact been completed.
                    this.heartbeatExecutor.shutdown();
                    ThreadPool.awaitTerminationOrExitApplication(this.heartbeatExecutor, 10, TimeUnit.SECONDS, "I2c Heartbeat", "internal error");

                    // Drain extant readers and writers
                    this.disableReadsAndWrites();
                    this.gracefullyDrainReadersAndWriters();

                    synchronized (this.callbackLock)
                        {
                        // There may be still data that needs to get out to the controller.
                        // Wait until that happens.
                        waitForWriteCompletionInternal(I2cWaitControl.ATOMIC);

                        // Now we know that the callback isn't executing, we can pull the
                        // rug out from under his use of the heartbeater
                        this.heartbeatExecutor = null;

                        // Finally, disconnect us from our I2cDevice
                        this.i2cDevice.deregisterForPortReadyCallback();
                        }

                    this.isHooked = false;
                    // Reset so that subsequent hooks see clean state. We no longer have a callback
                    // hook now, so any state here we have now isn't relevant to any subsequent rehook.
                    resetControllerState();
                    this.enableReadsAndWrites();

                    log(Log.VERBOSE, "...unhooking complete");
                    }
                }
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            }
        }

    protected void disableReadsAndWrites()
        {
        this.readerWriterPreventionCount.incrementAndGet();
        }
    protected void enableReadsAndWrites()
        {
        this.readerWriterPreventionCount.decrementAndGet();
        }
    protected boolean newReadsAndWritesAllowed()
        {
        return this.readerWriterPreventionCount.get() == 0;
        }

    protected void gracefullyDrainReadersAndWriters()
        {
        boolean interrupted = false;
        disableReadsAndWrites();

        for (;;)
            {
            try {
                // Note: we can't hold the concurrentClientLock or the callbackLock as
                // we attempt to take the readerWriterGate here, lest deadlock.
                if (readerWriterGate.writeLock().tryLock(20, TimeUnit.MILLISECONDS))
                    {
                    // gate acquired exclusively, so no extant readers or writers exist
                    readerWriterGate.writeLock().unlock();
                    break;
                    }
                else
                    {
                    // We timed out before hitting the lock. Run around the block again.
                    }
                }
            catch (InterruptedException e)
                {
                interrupted = true;
                }
            }

        if (interrupted)
            Thread.currentThread().interrupt();

        assertTrue(readerWriterCount.get()==0);
        enableReadsAndWrites();
        }

    protected void forceDrainReadersAndWriters()
        {
        boolean interrupted = false;
        boolean exitLoop = false;
        disableReadsAndWrites();

        for (;;)
            {
            synchronized (callbackLock)
                {
                // Set the write cache status to idle to kick anyone out of waiting for it to idle
                setWriteCacheStatus(WRITE_CACHE_STATUS.IDLE);

                // Lie and say that the data in the read cache is valid
                readCacheStatus = READ_CACHE_STATUS.VALID_QUEUED;
                hasReadWindowChanged = false;
                assertTrue(readCacheIsValid());

                // Actually wake folk up
                callbackLock.notifyAll();
                }

            if (exitLoop)
                break;

            try {
                // Note: we can't hold the concurrentClientLock or the callbackLock as
                // we attempt to take the readerWriterGate here, lest deadlock.
                if (readerWriterGate.writeLock().tryLock(20, TimeUnit.MILLISECONDS))
                    {
                    // gate acquired exclusively, so no extant readers or writers exist
                    readerWriterGate.writeLock().unlock();
                    exitLoop = true;
                    }
                else
                    {
                    // We timed out before hitting the lock. Repoke the cache statuses
                    // and try again.
                    }
                }
            catch (InterruptedException e)
                {
                interrupted = true;
                }
            }

        if (interrupted)
            Thread.currentThread().interrupt();

        // This assert has been observed to (inexplicably) fail. The circumstance at the time
        // involved the unplugging of a CDIM from the phone.
        assertTrue(readerWriterCount.get()==0);
        enableReadsAndWrites();
        }

    //----------------------------------------------------------------------------------------------
    // HardwareDevice
    //----------------------------------------------------------------------------------------------

    @Override public Manufacturer getManufacturer()
        {
        return this.controller.getManufacturer();
        }

    public String getDeviceName()
        {
        return this.i2cDevice.getDeviceName();
        }

    public String getConnectionInfo()
        {
        return this.i2cDevice.getConnectionInfo();
        }

    public int getVersion()
        {
        return this.i2cDevice.getVersion();
        }

    @Override
    public void resetDeviceConfigurationForOpMode()
        {
        }

    public void close()
        {
        // Since we're closing, we don't need to know if we're not going to get any more
        // notifications about ReadWriteRunnable coming and going: we're out of here!
        this.hardwareDeviceHealth.close();
        this.isClosing = true;
        this.i2cDevice.deregisterForPortReadyBeginEndCallback();

        this.disengage();

        if (this.isI2cDeviceOwned)
            this.i2cDevice.close();
        }

    //----------------------------------------------------------------------------------------------
    // HardwareDeviceHealth
    //----------------------------------------------------------------------------------------------

    @Override public void setHealthStatus(HealthStatus status)
        {
        hardwareDeviceHealth.setHealthStatus(status);
        }

    @Override public HealthStatus getHealthStatus()
        {
        return hardwareDeviceHealth.getHealthStatus();
        }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    /*
     * Sets the set of I2C device registers that we wish to read.
     */
    @Override public void setReadWindow(ReadWindow newWindow)
        {
        synchronized (this.concurrentClientLock)
            {
            if (!this.disableReadWindows)
                {
                setReadWindowInternal(newWindow);
                }
            }
        }

    protected void setReadWindowInternal(ReadWindow newWindow)
        {
        synchronized (this.concurrentClientLock)
            {
            synchronized (this.callbackLock)
                {
                if (this.readWindow != null && this.readWindow.canBeUsedToRead() && this.readWindow.mayInitiateSwitchToReadMode() && this.readWindow.sameAsIncludingMode(newWindow))
                    {
                    // What's there is good; we don't need to change anything
                    }
                else
                    {
                    // Remember the new window, but get a fresh copy so we can implement the read mode policy
                    assignReadWindow(newWindow.readableCopy());
                    assertTrue(this.readWindow.canBeUsedToRead() && this.readWindow.mayInitiateSwitchToReadMode());
                    }
                }
            }
        }

    /* locks must be externally taken */
    protected void assignReadWindow(ReadWindow newWindow)
        {
        this.readWindow = newWindow;

        // Let others (specifically, the callback) know of the update
        this.hasReadWindowChanged = true;
        }

    /*
     * Return the current register window.
     */
    @Override public ReadWindow getReadWindow()
        {
        synchronized (this.concurrentClientLock)
            {
            synchronized (this.callbackLock)
                {
                return this.readWindow;
                }
            }
        }

    /*
     * Ensure that the current register window covers the indicated set of registers.
     */
    @Override public void ensureReadWindow(ReadWindow windowNeeded, ReadWindow windowToSet)
        {
        synchronized (this.concurrentClientLock)
            {
            synchronized (this.callbackLock)
                {
                if (this.readWindow == null || !this.readWindow.containsWithSameMode(windowNeeded))
                    {
                    setReadWindow(windowToSet);
                    }
                }
            }
        }

    /*
     * Read the byte at the indicated register.
     */
    @Override public byte read8(int ireg)
        {
        return this.read(ireg, 1)[0];
        }

    /*
     * Read a contiguous set of registers
     */
    @Override public byte[] read(int ireg, int creg)
        {
        return this.readTimeStamped(ireg, creg).data;
        }

    /*
     * Read a contiguous set of registers.
     *
     * This is the core read routine. Note that the current read window is never
     * adjusted or invalidated by the execution of this function; that helps support
     * concurrent clients.
     */
    @Override public TimestampedData readTimeStamped(int ireg, int creg)
        {
        try
            {
            // Take the readerWriterLock so that others will be able to track when reads and writes have drained
            acquireReaderLockShared();
            try {
                if (!this.isOpenForReading())
                    return TimestampedI2cData.makeFakeData(null, getI2cAddress(), ireg, creg);

                synchronized (this.concurrentClientLock)
                    {
                    synchronized (this.callbackLock)
                        {
                        // Wait until the write cache isn't busy. This honors the visibility semantic
                        // we intend to portray, namely that issuing a read after a write has been
                        // issued will see the state AFTER the write has had a chance to take effect.
                        while (this.getWriteCacheStatus() != WRITE_CACHE_STATUS.IDLE)    // TODO: why don't we call waitForIdleWriteCache() here?
                            {
                            this.callbackLock.wait(msCallbackLockWaitQuantum);
                            }

                        // Remember what the read window was on entry so we can restore it later if needed
                        ReadWindow prevReadWindow = this.readWindow;

                        // Is what's in the read cache right now or shortly will be have what we want?
                        if (readCacheValidityCurrentOrImminent() && readWindowActuallyRead != null && readWindowActuallyRead.contains(ireg, creg))
                            {
                            // Ok, we don't have to issue a read, but we may have to wait for validity,
                            // which we we do in a moment down below
                            // log(Log.VERBOSE, String.format("read from cache: (0x%02x,%d)", ireg, creg));
                            }
                        else
                            {
                            // We have to issue a new read. We do so by setting the read window to something
                            // that is readable; this is noticed by the callback which then services the read.

                            // If there's no read window given or what's there either can't service any
                            // more reads or it doesn't contain the required registers, auto-make a new window.
                            boolean readWindowRangeOk = this.readWindow != null && this.readWindow.contains(ireg, creg);

                            if (!readWindowRangeOk || !this.readWindow.canBeUsedToRead() || !this.readWindow.mayInitiateSwitchToReadMode())
                                {
                                // If we can re-use the window that was there before that will help increase
                                // the chance that we don't need to take the time to switch the controller to
                                // read mode (with a different window) and thus can respond faster.
                                if (readWindowRangeOk)
                                    {
                                    // log(Log.VERBOSE, String.format("reuse window: (0x%02x,%d)", ireg, creg));
                                    setReadWindowInternal(this.readWindow);     // will make a readable copy
                                    }
                                else
                                    {
                                    // Make a one-shot that just covers the data we need right now
                                    // log(Log.VERBOSE, String.format("make one shot: (0x%02x,%d)", ireg, creg));
                                    setReadWindowInternal(new ReadWindow(ireg, creg, ReadMode.ONLY_ONCE));
                                    }
                                }
                            }

                        // Wait until the read cache is valid
                        waitForValidReadCache();

                        // Extract the data and return!
                        this.readCacheLock.lockInterruptibly();
                        try
                            {
                            assertTrue(this.readWindowActuallyRead.contains(this.readWindow));

                            synchronized (historyQueueLock)
                                {
                                if (getHistoryQueueCapacity() > 0)
                                    {
                                    // Remember what was actually read
                                    TimestampedI2cData readData = new TimestampedI2cData();
                                    int ibReadFirst     = /* this.readWindowActuallyRead.getRegisterFirst() - this.readWindowActuallyRead.getRegisterFirst() */ + dibCacheOverhead;
                                    readData.data       = Arrays.copyOfRange(this.readCache, ibReadFirst, ibReadFirst + this.readWindowActuallyRead.getRegisterCount());
                                    readData.nanoTime   = this.nanoTimeReadCacheValid;
                                    readData.i2cAddr    = this.getI2cAddress();
                                    readData.register   = this.readWindowActuallyRead.getRegisterFirst();
                                    addToHistoryQueue(readData);
                                    }
                                }

                            // The data of interest is somewhere in the read window, but not necessarily at the start.
                            int ibFirst            = ireg - this.readWindowActuallyRead.getRegisterFirst() + dibCacheOverhead;
                            TimestampedI2cData result = new TimestampedI2cData();
                            result.data            = Arrays.copyOfRange(this.readCache, ibFirst, ibFirst + creg);
                            result.nanoTime        = this.nanoTimeReadCacheValid;
                            result.i2cAddr         = this.getI2cAddress();
                            result.register        = ireg;
                            return result;
                            }
                        finally
                            {
                            this.readCacheLock.unlock();

                            // If that was a one-time read, invalidate the data so we won't read it again a second time.
                            // Note that this is the only place outside of the callback that we ever update
                            // readCacheStatus or writeCacheStatus
                            if (this.readCacheStatus==READ_CACHE_STATUS.VALID_ONLYONCE)
                                this.readCacheStatus=READ_CACHE_STATUS.IDLE;

                            // Restore any read window that we may have disturbed
                            if (this.readWindow != prevReadWindow)
                                {
                                assignReadWindow(prevReadWindow);
                                }
                            }
                        }
                    }
                }
            finally
                {
                releaseReaderLockShared();
                }
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            return TimestampedI2cData.makeFakeData(null, getI2cAddress(), ireg, creg);
            }
        }

    protected boolean isOpenForReading()
        {
        return this.isHooked && this.newReadsAndWritesAllowed();
        }
    protected boolean isOpenForWriting()
        {
        return this.isHooked && this.newReadsAndWritesAllowed();
        }
    protected void acquireReaderLockShared() throws InterruptedException
        {
        this.readerWriterGate.readLock().lockInterruptibly();
        this.readerWriterCount.incrementAndGet();   // for debugging
        }
    protected void releaseReaderLockShared()
        {
        this.readerWriterCount.decrementAndGet();       // for debugging
        this.readerWriterGate.readLock().unlock();
        }

    @Override public TimestampedData readTimeStamped(final int ireg, final int creg, final ReadWindow readWindowNeeded, final ReadWindow readWindowSet)
        {
        ensureReadWindow(readWindowNeeded, readWindowSet);
        return readTimeStamped(ireg, creg);
        }

    protected boolean readCacheValidityCurrentOrImminent()
        {
        return this.readCacheStatus != READ_CACHE_STATUS.IDLE && !this.hasReadWindowChanged;
        }
    protected boolean readCacheIsValid()
        {
        return this.readCacheStatus.isValid() && !this.hasReadWindowChanged;
        }

    /*
     * Write a byte to the indicated register
     */
    @Override public void write8(int ireg, int data)
        {
        this.write(ireg, new byte[]{(byte) data});
        }
    @Override public void write8(int ireg, int data, I2cWaitControl waitControl)
        {
        this.write(ireg, new byte[]{(byte) data}, waitControl);
        }

    /*
     * Write data to a set of registers, beginning with the one indicated. The data will be
     * written to the I2C device in an expeditious manner. Once data is accepted by this API,
     * it is guaranteed that (barring catastrophic failure) the data will be transmitted to the
     * USB controller module before the I2cDeviceSyncImpl is closed.
     */
    @Override public void write(int ireg, byte[] data)
        {
        write(ireg, data, I2cWaitControl.ATOMIC);
        }
    @Override public void write(int ireg, byte[] data, I2cWaitControl waitControl)
        {
        try
            {
            // Take the readerWriterLock so that others will be able to track when reads and writes have drained
            acquireReaderLockShared();
            try {
                if (!isOpenForWriting())
                    return; // Ignore the write

                synchronized (this.concurrentClientLock)
                    {
                    if (data.length > ReadWindow.WRITE_REGISTER_COUNT_MAX)
                        throw new IllegalArgumentException(String.format("write request of %d bytes is too large; max is %d", data.length, ReadWindow.WRITE_REGISTER_COUNT_MAX));

                    synchronized (this.callbackLock)
                        {
                        // If there's already a pending write, can we coalesce?
                        boolean doCoalesce = false;
                        if (this.isWriteCoalescingEnabled
                                && this.getWriteCacheStatus() == WRITE_CACHE_STATUS.DIRTY
                                && this.cregWrite + data.length <= ReadWindow.WRITE_REGISTER_COUNT_MAX)
                            {
                            if (ireg + data.length == this.iregWriteFirst)
                                {
                                // New data is immediately before the old data.
                                // leave ireg is unchanged
                                data = concatenateByteArrays(data, readWriteCache());
                                doCoalesce = true;
                                }
                            else if (this.iregWriteFirst + this.cregWrite == ireg)
                                {
                                // New data is immediately after the new data.
                                ireg = this.iregWriteFirst;
                                data = concatenateByteArrays(readWriteCache(), data);
                                doCoalesce = true;
                                }
                            }

                        // if (doCoalesce) this.log(Log.VERBOSE, "coalesced write");

                        // Wait until we can write to the write cache. If we are coalescing, then
                        // we don't ever wait, as we're just modifying what's there
                        if (!doCoalesce)
                            {
                            waitForIdleWriteCache();
                            }

                        // Indicate where we want to write
                        this.iregWriteFirst = ireg;
                        this.cregWrite      = data.length;

                        // Indicate we are dirty so the callback will write us out
                        setWriteCacheStatusIfHooked(WRITE_CACHE_STATUS.DIRTY);

                        // Provide the data we want to write
                        this.writeCacheLock.lock();
                        try
                            {
                            System.arraycopy(data, 0, this.writeCache, dibCacheOverhead, data.length);
                            }
                        finally
                            {
                            this.writeCacheLock.unlock();
                            }

                        waitForWriteCompletionInternal(waitControl);
                        }
                    }
                }
            finally
                {
                releaseReaderLockShared();
                }
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            }
        }

    protected static byte[] concatenateByteArrays(byte[] left, byte[] right)
        {
        byte[] result = new byte[left.length + right.length];
        System.arraycopy(left, 0, result, 0, left.length);
        System.arraycopy(right, 0, result, left.length, right.length);
        return result;
        }

    @Override public void waitForWriteCompletions(I2cWaitControl waitControl)
        {
        try {
            synchronized (this.concurrentClientLock)
                {
                synchronized (this.callbackLock)
                    {
                    waitForWriteCompletionInternal(waitControl);
                    }
                }
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            }
        }

    /* Returns a copy of the user data currently sitting in the write cache */
    protected byte[] readWriteCache()
        {
        this.writeCacheLock.lock();
        try {
            return Arrays.copyOfRange(this.writeCache, dibCacheOverhead, dibCacheOverhead + this.cregWrite);
            }
        finally
            {
            this.writeCacheLock.unlock();
            }
        }

    protected void waitForWriteCompletionInternal(I2cWaitControl writeControl) throws InterruptedException
        {
        if (writeControl != I2cWaitControl.NONE)
            {
            long nanoTimeIdle = waitForIdleWriteCache();
            if (writeControl == I2cWaitControl.WRITTEN)
                {
                long nanoDeadline = nanoTimeIdle + i2cDevice.getMaxI2cWriteLatency();
                long nanoWait     = Math.max(0, nanoDeadline - System.nanoTime());
                if (nanoWait > 0)
                    {
                    long msWait = nanoWait / ElapsedTime.MILLIS_IN_NANO;
                    Thread.sleep(msWait, (int)(nanoWait - msWait * ElapsedTime.MILLIS_IN_NANO));
                    }
                }
            }
        }

    protected void setWriteCacheStatus(WRITE_CACHE_STATUS status)
        {
        this.writeCacheStatus.setStatus(status);
        }

    protected WRITE_CACHE_STATUS getWriteCacheStatus()
        {
        return this.writeCacheStatus.getStatus();
        }

    protected void initWriteCacheStatus(WRITE_CACHE_STATUS status)
        {
        this.writeCacheStatus.initStatus(status);
        }

    protected long waitForIdleWriteCache() throws InterruptedException
        {
        return writeCacheStatus.waitForIdle();
        }

    /** @see WriteCacheStatus#waitForIdle() */
    protected void waitForValidReadCache() throws InterruptedException
        {
        ElapsedTime timer = null;
        while (!readCacheIsValid())
            {
            if (timer == null) timer = new ElapsedTime();
            if (timer.milliseconds() > msCallbackLockAbandon)
                throw new InterruptedException();
            this.callbackLock.wait(msCallbackLockWaitQuantum);
            }
        }

    /* set the write cache status, but don't disturb (from idle) if we're not open for business */
    void setWriteCacheStatusIfHooked(WRITE_CACHE_STATUS status)
        {
        if (this.isHooked && this.newReadsAndWritesAllowed())
            {
            setWriteCacheStatus(status);
            }
        }

    @Override
    public void enableWriteCoalescing(boolean enable)
        {
        synchronized (this.concurrentClientLock)
            {
            this.isWriteCoalescingEnabled = enable;
            }
        }

    @Override
    public boolean isWriteCoalescingEnabled()
        {
        synchronized (this.concurrentClientLock)
            {
            return this.isWriteCoalescingEnabled;
            }
        }

    @Override
    public void setUserConfiguredName(@Nullable String name)
        {
        synchronized (this.concurrentClientLock)
            {
            synchronized (this.callbackLock)
                {
                this.name = name;
                }
            }
        }

    @Override @Nullable public String getUserConfiguredName()
        {
        synchronized (this.concurrentClientLock)
            {
            synchronized (this.callbackLock)
                {
                return this.name;
                }
            }
        }

    @Override public void setLogging(boolean enabled)
        {
        synchronized (this.concurrentClientLock)
            {
            synchronized (this.callbackLock)
                {
                this.loggingEnabled = enabled;
                }
            }
        }

    @Override public boolean getLogging()
        {
        synchronized (this.concurrentClientLock)
            {
            synchronized (this.callbackLock)
                {
                return this.loggingEnabled;
                }
            }
        }

    @Override public void setLoggingTag(String loggingTag)
        {
        synchronized (this.concurrentClientLock)
            {
            synchronized (this.callbackLock)
                {
                this.loggingTag = loggingTag + "I2C";
                }
            }
        }

    @Override public String getLoggingTag()
        {
        synchronized (this.concurrentClientLock)
            {
            synchronized (this.callbackLock)
                {
                return this.loggingTag;
                }
            }
        }

    @Override public int getHeartbeatInterval()
        {
        synchronized (this.concurrentClientLock)
            {
            synchronized (this.callbackLock)
                {
                return this.msHeartbeatInterval;
                }
            }
        }

    @Override public void setHeartbeatInterval(int msHeartbeatInterval)
        {
        synchronized (this.concurrentClientLock)
            {
            synchronized (this.callbackLock)
                {
                this.msHeartbeatInterval = Math.max(0, msHeartbeatInterval);
                }
            }
        }

    @Override public void setHeartbeatAction(HeartbeatAction action)
        {
        synchronized (this.concurrentClientLock)
            {
            synchronized (this.callbackLock)
                {
                this.heartbeatAction = action;
                }
            }
        }

    @Override public HeartbeatAction getHeartbeatAction()
        {
        synchronized (this.concurrentClientLock)
            {
            synchronized (this.callbackLock)
                {
                return this.heartbeatAction;
                }
            }
        }

    protected void log(int verbosity, String message)
        {
        switch (verbosity)
            {
        case Log.VERBOSE:   Log.v(loggingTag, message); break;
        case Log.DEBUG:     Log.d(loggingTag, message); break;
        case Log.INFO:      Log.i(loggingTag, message); break;
        case Log.WARN:      Log.w(loggingTag, message); break;
        case Log.ERROR:     Log.e(loggingTag, message); break;
        case Log.ASSERT:    Log.wtf(loggingTag, message); break;
            }
        }
    protected void log(int verbosity, String format, Object... args)
        {
        log(verbosity, String.format(format, args));
        }

    protected class Callback implements I2cController.I2cPortReadyCallback, I2cController.I2cPortReadyBeginEndNotifications, RobotArmingStateNotifier.Callback
        {
        //------------------------------------------------------------------------------------------
        // State, kept in member variables so we can divvy the updateStateMachines() logic
        // across multiple function
        //------------------------------------------------------------------------------------------

        protected boolean setActionFlag     = false;
        protected boolean queueFullWrite    = false;
        protected boolean queueRead         = false;
        protected boolean heartbeatRequired = false;
        protected boolean enabledReadMode   = false;
        protected boolean enabledWriteMode  = false;

        protected READ_CACHE_STATUS  prevReadCacheStatus  = READ_CACHE_STATUS.IDLE;
        protected WRITE_CACHE_STATUS prevWriteCacheStatus = WRITE_CACHE_STATUS.IDLE;

        protected boolean doModuleIsArmedWorkEnabledWrites = false;
        protected boolean haveSeenModuleIsArmedWork        = false;

        //------------------------------------------------------------------------------------------
        // I2cController.I2cPortReadyCallback
        //------------------------------------------------------------------------------------------

        @Override public void portIsReady(int port)
        // This is the callback from the device module indicating completion of previously requested work.
        // At the moment we are called, we are assured that the read buffer / write buffer for our port in the
        // USB device is not currently busy.
            {
            updateStateMachines();
            }

        //------------------------------------------------------------------------------------------
        // RobotUsbModule.ArmingStateCallback
        //------------------------------------------------------------------------------------------

        @Override
        public void onModuleStateChange(RobotArmingStateNotifier robotUsbModule, RobotUsbModule.ARMINGSTATE armingstate)
            {
            switch (armingstate)
                {
                case ARMED:
                    log(Log.VERBOSE, "onArmed ...");
                    doModuleIsArmedWork(true);
                    log(Log.VERBOSE, "... onArmed");
                    break;
                case PRETENDING:
                    log(Log.VERBOSE, "onPretending ...");
                    doModuleIsArmedWork(false);
                    log(Log.VERBOSE, "... onPretending");
                    break;
                case DISARMED:
                    // Unnecessary: we WILL get the onPortIsReadyCallbacksEnd() notification;
                    break;
                }
            }

        //------------------------------------------------------------------------------------------
        // I2cController.I2cPortReadyBeginEndNotifications
        //------------------------------------------------------------------------------------------

        @Override public void onPortIsReadyCallbacksBegin(int port)
            {
            // We get this callback when we register for portIsReadyCallbacks and our module
            // is either armed or pretending. If it's armed already, we're not going to get
            // a notification that it changes into the armed state. So hook up now! Otherwise,
            // we'll wait until the
            log(Log.VERBOSE, "doPortIsReadyCallbackBeginWork ...");
            try {
                switch (robotUsbModule.getArmingState())
                    {
                    case ARMED:
                        doModuleIsArmedWork(true);
                        break;
                    case PRETENDING:
                        doModuleIsArmedWork(false);
                        break;
                    }
                }
            finally
                {
                log(Log.VERBOSE, "... doPortIsReadyCallbackBeginWork complete");
                }
            }

        protected void doModuleIsArmedWork(boolean arming)
            {
            try {
                log(Log.VERBOSE, "doModuleIsArmedWork ...");
                synchronized (engagementLock)
                    {
                    disableReadsAndWrites();
                    forceDrainReadersAndWriters();
                    unhook();

                    // Further locking appears unnecessary to invoke attachToController
                    I2cDeviceSynchImpl.this.attachToController();

                    adjustHooking();

                    // We are a little paranoid here. In theory, we ought to be able to work perfectly
                    // fine against a module that is pretending. But it's more robust of us to just leave
                    // reads and writes disabled at our upper surface rather than relying on that emulation
                    // to work. Moreover, during development, we hit a number of deadlocks when we tried,
                    // THOUGH those deadlocks might actually have been due to use not using onModuleStateChange
                    // to do the work but rather ONLY onPortIsReadyCallbacksBegin. That bug MIGHT have been
                    // the whole story, but it might not, and we're just too worn out to find out. So we take
                    // the robust, easy way out. For now, at least.
                    if (arming)
                        {
                        enableReadsAndWrites();
                        doModuleIsArmedWorkEnabledWrites = true;
                        }
                    else
                        doModuleIsArmedWorkEnabledWrites = false;

                    haveSeenModuleIsArmedWork = true;
                    }
                }
            finally
                {
                log(Log.VERBOSE, "... doModuleIsArmedWork complete");
                }
            }

        @Override public void onPortIsReadyCallbacksEnd(int port)
        // We're being told that we're not going to get any more portIsReady callbacks.
            {
            try {
                log(Log.VERBOSE, "onPortIsReadyCallbacksEnd ...");

                if (isClosing)
                    return; // ignore

                if (!haveSeenModuleIsArmedWork)
                    return; // ReadWriteRunnable started then suddenly stopped before owner finished arming

                synchronized (engagementLock)
                    {
                    if (doModuleIsArmedWorkEnabledWrites)
                        {
                        disableReadsAndWrites();
                        }

                    forceDrainReadersAndWriters();
                    unhook();
                    assertTrue(!isOpenForReading() && !isOpenForWriting());
                    enableReadsAndWrites();

                    haveSeenModuleIsArmedWork = false;
                    }
                }
            finally
                {
                log(Log.VERBOSE, "... onPortIsReadyCallbacksEnd complete");
                }
            }

        //------------------------------------------------------------------------------------------
        // Update logic
        //------------------------------------------------------------------------------------------

        protected void startSwitchingToReadMode(ReadWindow window)
            {
            readCacheStatus = isControllerLegacy ? READ_CACHE_STATUS.SWITCHINGTOREADMODE : READ_CACHE_STATUS.QUEUED;
            // calling enableI2cReadMode() will set the mode, i2cAddr, and register range bytes in the write cache.
            i2cDevice.enableI2cReadMode(i2cAddr, window.getRegisterFirst(), window.getRegisterCount());
            enabledReadMode = true;

            // Remember what we actually told the controller
            readWindowSentToController = window;
            isReadWindowSentToControllerInitialized = true;

            setActionFlag = true;      // causes an I2C read to happen
            queueFullWrite = true;     // write the bytes that enableI2cReadMode() set
            if (!isControllerLegacy) queueRead = true; // read the results of the I2C read
            }

        protected void issueWrite()
            {
            setWriteCacheStatusIfHooked(WRITE_CACHE_STATUS.QUEUED);
            // calling enableI2cWriteMode() will set the mode, i2cAddr, and register range bytes in the write cache.
            i2cDevice.enableI2cWriteMode(i2cAddr, iregWriteFirst, cregWrite);
            enabledWriteMode = true;

            // This might be only paranoia, but we're not certain. In any case, it's safe.
            readWindowSentToController = null;
            isReadWindowSentToControllerInitialized = true;

            setActionFlag  = true;      // causes the I2C write to happen
            queueFullWrite = true;      // write the bytes that enableI2cWriteMode() set
            }

        protected void updateStateMachines()
        // We've got quite the little state machine here!
            {
            synchronized (callbackLock)
                {
                //----------------------------------------------------------------------------------
                // Initialize state for managing state transition

                setActionFlag     = false;
                queueFullWrite    = false;
                queueRead         = false;
                heartbeatRequired = (msHeartbeatInterval > 0 && timeSinceLastHeartbeat.milliseconds() >= msHeartbeatInterval);
                enabledReadMode   = false;
                enabledWriteMode  = false;

                prevReadCacheStatus  = readCacheStatus;
                prevWriteCacheStatus = getWriteCacheStatus();

                //----------------------------------------------------------------------------------
                // Keep track of controller port mode states. The rule, we understand, is that after we request
                // a switch to read mode, once we get the 'port is ready' callback, then read mode is in fact
                // enabled. Note that SWITCHINGTOREADMODE is only ever used on Legacy Modules.
                if (controllerPortMode == CONTROLLER_PORT_MODE.SWITCHINGTOREADMODE)
                    controllerPortMode = CONTROLLER_PORT_MODE.READ;

                //----------------------------------------------------------------------------------
                // Deal with the fact that we've completed any previous queueing operation

                if (readCacheStatus == READ_CACHE_STATUS.QUEUED || readCacheStatus == READ_CACHE_STATUS.VALID_QUEUED)
                    {
                    readCacheStatus = READ_CACHE_STATUS.QUEUE_COMPLETED;
                    if (!readCacheTimeWindow.isCleared())
                        {
                        nanoTimeReadCacheValid = readCacheTimeWindow.getNanosecondsLast();
                        }
                    else
                        {
                        nanoTimeReadCacheValid = System.nanoTime();
                        }
                    }

                if (getWriteCacheStatus() == WRITE_CACHE_STATUS.QUEUED)
                    {
                    setWriteCacheStatus(WRITE_CACHE_STATUS.IDLE);
                    // Our write mode status should have been reported back to us. And it
                    // always will, so long as our module remains operational.
                    //
                    // But that might not happen *right* at the moment our module disconnects:
                    // we've lost communication; we're not going to see the write mode status
                    // reported back, but the hole module-disconnect-handing sequence has yet to
                    // tear everything down in response.
                    //
                    // There doesn't seem to be any way to reliably do an assert, as that loss
                    // of connection might have not yet got through to *anybody*.
                    //
                    // assertTrue(!isHooked || !newReadsAndWritesAllowed() || i2cDevice.isI2cPortInWriteMode());
                    }

                //--------------------------------------------------------------------------
                // That limits the number of states the caches can now be in

                assertTrue(readCacheStatus==READ_CACHE_STATUS.IDLE
                         ||(readCacheStatus==READ_CACHE_STATUS.SWITCHINGTOREADMODE && isControllerLegacy)
                         ||readCacheStatus==READ_CACHE_STATUS.VALID_ONLYONCE
                         ||readCacheStatus==READ_CACHE_STATUS.QUEUE_COMPLETED);
                assertTrue(getWriteCacheStatus() == WRITE_CACHE_STATUS.IDLE || getWriteCacheStatus() == WRITE_CACHE_STATUS.DIRTY);
                assertTrue(controllerPortMode==CONTROLLER_PORT_MODE.READ || controllerPortMode==CONTROLLER_PORT_MODE.WRITE || controllerPortMode==CONTROLLER_PORT_MODE.UNKNOWN);

                //--------------------------------------------------------------------------
                // Complete any read mode switch if there is one

                if (readCacheStatus == READ_CACHE_STATUS.SWITCHINGTOREADMODE)
                    {
                    assertTrue(isControllerLegacy);

                    // We're trying to switch into read mode. Are we there yet?
                    if (controllerPortMode == CONTROLLER_PORT_MODE.READ)
                        {
                        // See also below XYZZY
                        readCacheStatus = READ_CACHE_STATUS.QUEUED;
                        setActionFlag   = true;     // actually do an I2C read
                        queueRead       = true;     // read the I2C read results
                        }
                    else
                        {
                        queueRead = true;           // read the mode byte
                        }
                    }

                //--------------------------------------------------------------------------
                // If there's a write request pending, and it's ok to issue the write, do so

                else if (getWriteCacheStatus() == WRITE_CACHE_STATUS.DIRTY)
                    {
                    issueWrite();

                    // Our ordering rules are that any reads after a write have to wait until
                    // the write is actually sent to the hardware, so anything we've read before is junk.
                    // Note that there's an analogous check in read().
                    readCacheStatus = READ_CACHE_STATUS.IDLE;
                    }

                //--------------------------------------------------------------------------
                // Initiate reading if we should. Be sure to honor the policy of the read mode.

                else if (readCacheStatus == READ_CACHE_STATUS.IDLE || hasReadWindowChanged)
                    {
                    boolean issuedRead = false;
                    if (readWindow != null)
                        {
                        // Is the controller already set up to read the data we're now interested
                        // in, so that we can get at it without having to incur the cost of
                        // switching to read mode?
                        boolean readSwitchUnnecessary = (isReadWindowSentToControllerInitialized
                                && readWindowSentToController != null
                                && readWindowSentToController.contains(readWindow)
                                && controllerPortMode==CONTROLLER_PORT_MODE.READ);

                        if (readWindow.canBeUsedToRead() && (readSwitchUnnecessary || readWindow.mayInitiateSwitchToReadMode()))
                            {
                            if (readSwitchUnnecessary)
                                {
                                // Lucky us! We can go ahead and queue the read right now!
                                // See also above XYZZY
                                readWindowActuallyRead = readWindowSentToController;
                                readCacheStatus = READ_CACHE_STATUS.QUEUED;
                                setActionFlag   = true;         // actually do an I2C read
                                queueRead       = true;         // read the results of the read
                                }
                            else
                                {
                                // We'll start switching now, and queue the read later
                                readWindowActuallyRead = readWindow;
                                startSwitchingToReadMode(readWindow);
                                }

                            issuedRead = true;
                            }
                        }

                    if (issuedRead)
                        {
                        // Remember that we've used this window in a read operation. This doesn't
                        // matter for REPEATs, but does for the other modes
                        readWindow.noteWindowUsedForRead();
                        }
                    else
                        {
                        // Make *sure* that we don't appear to have valid data
                        readCacheStatus = READ_CACHE_STATUS.IDLE;
                        }

                    hasReadWindowChanged = false;
                    }

                //--------------------------------------------------------------------------
                // Reissue any previous read if we should. The only way we are here and
                // see READ_CACHE_STATUS.QUEUE_COMPLETED is if we completed a queuing operation
                // above.

                else if (readCacheStatus == READ_CACHE_STATUS.QUEUE_COMPLETED)
                    {
                    if (readWindow != null && readWindow.canBeUsedToRead())
                        {
                        readCacheStatus = READ_CACHE_STATUS.VALID_QUEUED;
                        setActionFlag = true;           // actually do an I2C read
                        queueRead     = true;           // read the results of the read
                        }
                    else
                        {
                        readCacheStatus = READ_CACHE_STATUS.VALID_ONLYONCE;
                        }
                    }

                //--------------------------------------------------------------------------
                // Completing the possibilities:

                else if (readCacheStatus == READ_CACHE_STATUS.VALID_ONLYONCE)
                    {
                    // Just leave it there until someone reads it
                    }

                //----------------------------------------------------------------------------------
                // Ok, after all that we finally know what how we're required to
                // interact with the device controller according to what we've been
                // asked to read or write. But what, now, about heartbeats?

                if (!setActionFlag && heartbeatRequired)
                    {
                    if (heartbeatAction != null)
                        {
                        if (isReadWindowSentToControllerInitialized && readWindowSentToController != null && heartbeatAction.rereadLastRead)
                            {
                            // Controller is in or is switching to read mode. If he's there
                            // yet, then issue an I2C read; if he's not, then he soon will be.
                            if (controllerPortMode == CONTROLLER_PORT_MODE.READ)
                                {
                                setActionFlag = true;       // issue an I2C read
                                }
                            else
                                {
                                assertTrue(readCacheStatus==READ_CACHE_STATUS.SWITCHINGTOREADMODE && isControllerLegacy);
                                }
                            }

                        else if (isReadWindowSentToControllerInitialized && readWindowSentToController == null && heartbeatAction.rewriteLastWritten)
                            {
                            // Controller is in write mode, and the write cache has what we last wrote
                            queueFullWrite = true;
                            setActionFlag = true;           // issue an I2C write
                            }

                        else if (heartbeatAction.heartbeatReadWindow != null)
                            {
                            // The simplest way to do this is just to do a new read from the outside, as that
                            // means it has literally zero impact here on our state machine. That unfortunately
                            // introduces concurrency where otherwise none might exist, but that's ONLY if you
                            // choose this flavor of heartbeat, so that's a reasonable tradeoff.
                            final ReadWindow window = heartbeatAction.heartbeatReadWindow;   // capture here while we still have the lock
                            try {
                                if (heartbeatExecutor != null)
                                    {
                                    heartbeatExecutor.submit(new java.lang.Runnable()
                                        {
                                        @Override public void run()
                                            {
                                            try {
                                                I2cDeviceSynchImpl.this.read(window.getRegisterFirst(), window.getRegisterCount());
                                                }
                                            catch (Exception e) // paranoia
                                                {
                                                // ignored
                                                }
                                            }
                                        });
                                    }
                                }
                            catch (RejectedExecutionException e)
                                {
                                // ignore: maybe we're racing with disarm
                                }
                            }
                        }
                    }

                if (setActionFlag)
                    {
                    // We're about to communicate on I2C right now, so reset the heartbeat.
                    // Note that we reset() *before* we talk to the device so as to do
                    // conservative timing accounting.
                    timeSinceLastHeartbeat.reset();
                    }

                //----------------------------------------------------------------------------------
                // Keep track about what we know about our port on the controller

                if (enabledReadMode || enabledWriteMode)
                    {
                    assertTrue(queueFullWrite);                       // they're useless without actually writing them
                    assertFalse(enabledReadMode && enabledWriteMode); // that would be silly

                    if (enabledWriteMode)
                        {
                        controllerPortMode = CONTROLLER_PORT_MODE.WRITE;
                        }
                    else
                        {
                        controllerPortMode = isControllerLegacy
                                ? CONTROLLER_PORT_MODE.SWITCHINGTOREADMODE
                                : CONTROLLER_PORT_MODE.READ;
                        }
                    }

                //----------------------------------------------------------------------------------
                // Read, set action flag and / or queue to module as requested

                if (setActionFlag)
                    i2cDevice.setI2cPortActionFlag();
                else
                    i2cDevice.clearI2cPortActionFlag();

                if (setActionFlag && !queueFullWrite)
                    {
                    i2cDevice.writeI2cPortFlagOnlyToController();
                    }
                else if (queueFullWrite)
                    {
                    i2cDevice.writeI2cCacheToController();
                    }

                // Queue a read after queuing any write for a bit of paranoia: if we're mode switching
                // to write, we want that write to go out first, THEN read the mode status. It probably
                // would anyway, but why not...
                if (queueRead)
                    {
                    i2cDevice.readI2cCacheFromController();
                    }

                //----------------------------------------------------------------------------------
                // Do logging

                if (loggingEnabled)
                    {
                    boolean trivial = true;

                    StringBuilder message = new StringBuilder();
                    message.append(String.format("cyc %d", i2cDevice.getCallbackCount()));
                    if (setActionFlag || queueFullWrite || queueRead)
                        {
                        trivial = false;
                        message.append("|");
                        if (setActionFlag)  message.append("f");
                        if (queueFullWrite) message.append("w");
                        if (queueRead)      message.append("r");
                        }

                    if (readCacheStatus != prevReadCacheStatus)   { trivial = false; message.append("| R." + prevReadCacheStatus.toString() + "->" + readCacheStatus.toString()); }
                    if (getWriteCacheStatus() != prevWriteCacheStatus) { trivial = false; message.append("| W." + prevWriteCacheStatus.toString() + "->" + writeCacheStatus.toString()); }
                    if (enabledWriteMode)                         { trivial = false; message.append(String.format("| write(0x%02x,%d)", iregWriteFirst, cregWrite)); }
                    if (enabledReadMode)                          { trivial = false; message.append(String.format("| read(0x%02x,%d)", readWindow.getRegisterFirst(), readWindow.getRegisterCount())); }

                    if (!trivial) message.append(String.format("| port=%s", controllerPortMode.toString()));
                    if (!trivial) log(Log.DEBUG, message.toString());
                    }

                //----------------------------------------------------------------------------------
                // Notify anyone blocked in read() or write()
                callbackLock.notifyAll();
                }
            }
        }
    }
