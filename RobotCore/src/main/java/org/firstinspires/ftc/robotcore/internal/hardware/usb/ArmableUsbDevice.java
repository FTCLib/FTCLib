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
package org.firstinspires.ftc.robotcore.internal.hardware.usb;

import android.content.Context;
import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;

import com.qualcomm.robotcore.R;
import com.qualcomm.robotcore.eventloop.SyncdDevice;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.HardwareDeviceCloseOnTearDown;
import com.qualcomm.robotcore.hardware.usb.RobotUsbDevice;
import com.qualcomm.robotcore.hardware.usb.RobotUsbModule;
import com.qualcomm.robotcore.util.GlobalWarningSource;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.SerialNumber;
import com.qualcomm.robotcore.util.WeakReferenceSet;

import org.firstinspires.ftc.robotcore.external.function.InterruptableThrowingRunnable;
import org.firstinspires.ftc.robotcore.internal.system.Assert;
import org.firstinspires.ftc.robotcore.internal.system.Misc;
import org.firstinspires.ftc.robotcore.internal.system.Tracer;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link ArmableUsbDevice} is an abstract base implementation class that assists in managing
 * the state transitions that a USB device can sequence through. Originally designed for use
 * with {@link RobotUsbDevice}s, which represent devices communicated with through a serial
 * communications protocol, it has been adapted to work with other, more general usb devices.
 */
@SuppressWarnings("WeakerAccess")
public abstract class ArmableUsbDevice implements RobotUsbModule, GlobalWarningSource, HardwareDeviceCloseOnTearDown
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected abstract String getTag();
    public static boolean TRACE = false;
    protected Tracer tracer = Tracer.create(getTag(), TRACE);

    protected final Context                 context;
    protected final SerialNumber            serialNumber;
    protected       SyncdDevice.Manager     syncdDeviceManager;
    protected       OpenRobotUsbDevice      openRobotUsbDevice;
    protected final AtomicInteger           referenceCount;
    protected       RobotUsbDevice          robotUsbDevice;

    protected           ARMINGSTATE             armingState;
    protected final     Object                  armingLock                 = new Object();
    protected final  WeakReferenceSet<Callback> registeredCallbacks        = new WeakReferenceSet<>();
    protected final     Object                  warningMessageLock         = new Object();
    protected           int                     warningMessageSuppressionCount;

    /** The (first) warning message generating during an arm() attempt. This is auto-cleared when
     * arm() is called; if other issues can contribute to warnings, they should be dealt with separately */
    protected           String                  warningMessage;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public interface OpenRobotUsbDevice
        {
        @Nullable RobotUsbDevice open() throws RobotCoreException, InterruptedException;
        }

    public ArmableUsbDevice(Context context, SerialNumber serialNumber, SyncdDevice.Manager manager, OpenRobotUsbDevice openRobotUsbDevice)
        {
        this.context              = context;
        this.serialNumber         = serialNumber;
        this.syncdDeviceManager   = manager;
        this.openRobotUsbDevice   = openRobotUsbDevice;
        this.referenceCount       = new AtomicInteger(1);
        this.robotUsbDevice       = null;
        this.armingState          = ARMINGSTATE.DISARMED;
        this.warningMessageSuppressionCount = 0;
        this.warningMessage       = "";
        }

    @CallSuper
    protected void finishConstruction()
        {
        registerGlobalWarningSource();
        }

    protected void registerGlobalWarningSource()
        {
        RobotLog.registerGlobalWarningSource(this);
        }

    protected void unregisterGlobalWarningSource()
        {
        RobotLog.unregisterGlobalWarningSource(this);
        }

    //----------------------------------------------------------------------------------------------
    // Accessors
    //----------------------------------------------------------------------------------------------

    public Context getContext()
        {
        return this.context;
        }

    @Nullable public RobotUsbDevice getRobotUsbDevice()
        {
        return this.robotUsbDevice;
        }

    //----------------------------------------------------------------------------------------------
    // RobotUsbModule
    //----------------------------------------------------------------------------------------------

    @Override
    public SerialNumber getSerialNumber()
        {
        return serialNumber;
        }

    @Override
    public void registerCallback(Callback callback, boolean doInitialCallback)
        {
        registeredCallbacks.add(callback);
        if (doInitialCallback)
            {
            callback.onModuleStateChange(this, this.armingState);
            }
        }

    @Override
    public void unregisterCallback(Callback callback)
        {
        registeredCallbacks.remove(callback);
        }

    //----------------------------------------------------------------------------------------------
    // GlobalWarningSource
    //----------------------------------------------------------------------------------------------

    @Override
    public String getGlobalWarning()
        {
        synchronized (this.warningMessageLock)
            {
            return this.warningMessageSuppressionCount > 0 ? "" : composeGlobalWarning();
            }
        }

    /** subclass hook for more complicated warning message structure */
    protected String composeGlobalWarning()
        {
        return this.warningMessage;
        }

    @Override
    public void clearGlobalWarning()
        {
        synchronized (this.warningMessageLock)
            {
            internalClearGlobalWarning();
            this.warningMessageSuppressionCount = 0;
            }
        }

    protected void internalClearGlobalWarning()
        {
        synchronized (this.warningMessageLock)
            {
            if (!this.warningMessage.isEmpty()) RobotLog.vv(getTag(), "clearing extant global warning: \"%s\"", this.warningMessage);
            this.warningMessage = "";
            }
        }

    @Override
    public void suppressGlobalWarning(boolean suppress)
        {
        synchronized (this.warningMessageLock)
            {
            if (suppress)
                this.warningMessageSuppressionCount++;
            else
                this.warningMessageSuppressionCount--;
            }
        }

    @Override
    public void setGlobalWarning(String warning)
        {
        // We need to lock so we can atomically test-and-set. We can't lock on us, the whole
        // object, as that will cause deadlocks due to interactions with operational synchronized
        // methods (in subclasses). Thus, we need to use a leaf-level lock.
        synchronized (this.warningMessageLock)
            {
            if (warning != null && warningMessage.isEmpty())
                {
                this.warningMessage = warning;
                }
            }
        }

    //----------------------------------------------------------------------------------------------
    // Internal arming and disarming
    //----------------------------------------------------------------------------------------------

    protected void armDevice() throws RobotCoreException, InterruptedException
        {
        synchronized (armingLock)
            {
            // An arming attempt clears any extant warning
            internalClearGlobalWarning();

            Assert.assertTrue(this.robotUsbDevice == null);
            RobotUsbDevice device = null;
            try
                {
                // Open the USB device
                tracer.trace("opening %s...", serialNumber);
                device = this.openRobotUsbDevice.open();

                // Arm using that device
                tracer.trace("...opening, now arming %s...", serialNumber);
                armDevice(device);

                tracer.trace("...arming %s...", serialNumber);
                }
            catch (RobotCoreException|RuntimeException e)
                {
                RobotLog.logExceptionHeader(getTag(), e, "exception arming %s", serialNumber);
                //
                // NullPointerException(s) are, annoyingly (vs some non-runtime exception), thrown by the FTDI
                // layer on abnormal termination. Here, and elsewhere in this class, we catch those in order to
                // robustly recover from what is just a USB read or write error.
                if (device != null) device.close();
                setGlobalWarning(getUnableToOpenMessage());
                throw e;
                }
            catch (InterruptedException e)
                {
                if (device != null) device.close();
                throw e;
                }
            }
        }

    protected String getUnableToOpenMessage()
        {
        return Misc.formatForUser(R.string.warningUnableToOpen, SerialNumber.getDeviceDisplayName(serialNumber));
        }

    protected void pretendDevice() throws RobotCoreException, InterruptedException
        {
        synchronized (armingLock)
            {
            RobotUsbDevice device = this.getPretendDevice(this.serialNumber);
            pretendDevice(device);
            }
        }

    protected RobotUsbDevice getPretendDevice(SerialNumber serialNumber)
        {
        return null;
        }

    protected abstract void armDevice(RobotUsbDevice device) throws RobotCoreException, InterruptedException;

    protected void pretendDevice(RobotUsbDevice device) throws RobotCoreException, InterruptedException
        {
        // Default: arm using that device
        armDevice(device);
        }

    protected abstract void disarmDevice() throws InterruptedException;

    //----------------------------------------------------------------------------------------------
    // Arming and disarming
    //----------------------------------------------------------------------------------------------

    @Override
    public ARMINGSTATE getArmingState()
        {
        return this.armingState;
        }

    protected ARMINGSTATE setArmingState(ARMINGSTATE state)
        {
        ARMINGSTATE prev = this.armingState;
        this.armingState = state;
        for (Callback callback : registeredCallbacks)
            {
            callback.onModuleStateChange(this, state);
            }
        return prev;
        }

    protected boolean isArmed()
        {
        return this.armingState == ARMINGSTATE.ARMED;
        }
    protected boolean isArmedOrArming()
        {
        return this.armingState == ARMINGSTATE.ARMED || this.armingState == ARMINGSTATE.TO_ARMED;
        }

    protected boolean isPretending()
        {
        return this.armingState == ARMINGSTATE.PRETENDING;
        }


    @Override
    public void arm() throws RobotCoreException, InterruptedException
        {
        tracer.trace("arm()", new InterruptableThrowingRunnable<RobotCoreException>()
            {
            @Override public void run() throws RobotCoreException, InterruptedException
                {
                synchronized (armingLock)
                    {
                    try
                        {
                        switch (armingState)
                            {
                            case ARMED:
                                return;
                            case DISARMED:
                                ARMINGSTATE prev = setArmingState(ARMINGSTATE.TO_ARMED);
                                try {
                                    doArm();
                                    setArmingState(ARMINGSTATE.ARMED);
                                    }
                                catch (Exception e)
                                    {
                                    setArmingState(prev);
                                    throw e;
                                    }
                                break;
                            default:
                                throw new RobotCoreException("illegal state: can't arm() from state %s", armingState.toString());
                            }
                        }
                    catch (RobotCoreException|InterruptedException|NullPointerException e)
                        {
                        disarm();
                        throw e;
                        }
                    }
                }
            });
        }

    /** intended as subclass hook */
    protected void doArm() throws RobotCoreException, InterruptedException
        {
        this.armDevice();
        }

    @Override
    public void pretend() throws RobotCoreException, InterruptedException
        {
        tracer.trace("pretend()", new InterruptableThrowingRunnable<RobotCoreException>()
            {
            @Override public void run() throws RobotCoreException, InterruptedException
                {
                synchronized (armingLock)
                    {
                    try
                        {
                        switch (armingState)
                            {
                            case PRETENDING:
                                return;
                            case DISARMED:
                                ARMINGSTATE prev = setArmingState(ARMINGSTATE.TO_PRETENDING);
                                try {
                                    doPretend();
                                    setArmingState(ARMINGSTATE.PRETENDING);
                                    }
                                catch (Exception e)
                                    {
                                    RobotLog.logExceptionHeader(getTag(), e, "exception while pretending; reverting to %s", prev);
                                    setArmingState(prev);
                                    throw e;
                                    }
                                break;
                            default:
                                throw new RobotCoreException("illegal state: can't pretend() from state %s", armingState.toString());
                            }
                        }
                    catch (RobotCoreException|RuntimeException|InterruptedException e)
                        {
                        disarm();
                        throw e;
                        }
                    }
                }
            });
        }

    @Override
    public void armOrPretend() throws RobotCoreException, InterruptedException
        {
        synchronized (armingLock)
            {
            try
                {
                arm();
                }
            catch (RobotCoreException|RuntimeException e)
                {
                pretend();
                }
            catch (InterruptedException e)
                {
                pretend();
                Thread.currentThread().interrupt(); // TODO: we should just throw. That change requires solid testing though, esp of MR devices...
                }
            }
        }

    /** intended for subclasses to override */
    protected void doPretend() throws RobotCoreException, InterruptedException
        {
        this.pretendDevice();
        }

    @Override
    public void disarm() throws RobotCoreException, InterruptedException
        {
        tracer.trace("disarm()", new InterruptableThrowingRunnable<RobotCoreException>()
            {
            @Override public void run() throws RobotCoreException, InterruptedException
                {
                synchronized (armingLock)
                    {
                    switch (armingState)
                        {
                        case DISARMED:
                            return;
                        case TO_ARMED:
                        case ARMED:
                        case TO_PRETENDING:
                        case PRETENDING:
                            ARMINGSTATE prev = setArmingState(ARMINGSTATE.TO_DISARMED);
                            try {
                                doDisarm();
                                setArmingState(ARMINGSTATE.DISARMED);
                                }
                            catch (InterruptedException e)
                                {
                                setArmingState(prev);
                                Thread.currentThread().interrupt();
                                }
                            catch (Exception e)
                                {
                                RobotLog.ee(getTag(), e, "exception thrown during disarm()");
                                setArmingState(prev);
                                throw e;
                                }
                            break;
                        default:
                            throw new RobotCoreException("illegal state: can't disarm() from state %s", armingState.toString());
                        }
                    }
                }
            });
        }

    /** intended as subclass hook */
    protected void doDisarm() throws RobotCoreException, InterruptedException
        {
        this.disarmDevice();
        }

    /** Add a new reference to this object so as to increase the number of releaseRef() calls
     * that will be required to actually close the device. Avoid locks in the reference counting
     * so we don't complicate the lock hierarchy and cause deadlocks. */
    public void addRef()
        {
        for (;;)
            {
            int crefCur = referenceCount.get();
            Assert.assertTrue(crefCur > 0); // can't revive dead objects
            if (crefCur <= 0)
                break; // preserve sanity

            int crefNew = crefCur + 1;
            if (referenceCount.compareAndSet(crefCur, crefNew))
                {
                tracer.trace("0x%08x: addRef [%s]=%d", hashCode(), getSerialNumber(), crefNew);
                break;
                }
            }
        }

    public void releaseRef()
        {
        for (;;)
            {
            int crefCur = referenceCount.get();
            Assert.assertTrue(crefCur > 0); // because we believe we own a ref we can release!
            if (crefCur <= 0)
                break; // preserve sanity

            int crefNew = crefCur - 1;
            if (referenceCount.compareAndSet(crefCur, crefNew))
                {
                tracer.trace("0x%08x: releaseRef [%s]=%d", hashCode(), getSerialNumber(), crefNew);
                if (crefNew == 0)
                    {
                    doClose();
                    }
                break;
                }
            }
        }


    /** close(), proper, must be idempotent. Note: we don't expect clients to use BOTH ref counting AND explicit close() */
    @Override public void close()
        {
        for (;;)
            {
            int crefCur = referenceCount.get();
            if (crefCur <= 0)
                break; // be idempotent

            int crefNew = 0;
            if (referenceCount.compareAndSet(crefCur, crefNew))
                {
                doClose();
                break;
                }
            }
        }

    protected void doClose()
        {
        tracer.trace("doClose()", new Runnable()
            {
            @Override public void run()
                {
                synchronized (armingLock)
                    {
                    tracer.trace("doClose([%s])...", getSerialNumber());
                    try
                        {
                        switch (armingState)
                            {
                            case CLOSED:
                                return;
                            case ARMED:
                                doCloseFromArmed();
                                break;
                            default:
                                doCloseFromOther();
                                break;
                            }
                        }
                    catch (InterruptedException e)
                        {
                        Thread.currentThread().interrupt();
                        }
                    catch (RobotCoreException|RuntimeException e)
                        {
                        // Eat the exception: we are close()ing now, after all
                        }
                    finally
                        {
                        // Critically, double-plus ensure that our FTDI device (or whatever) gets closed, or we might not be able to re-open later
                        if (robotUsbDevice != null)
                            {
                            RobotLog.v("safety-closing USB device for %s", serialNumber);
                            robotUsbDevice.close();
                            robotUsbDevice = null;
                            }

                        // In all cases, note us as closed on exit. We tried to close down once; even
                        // if we got an error, there's nothing more we can reasonably do!
                        setArmingState(ARMINGSTATE.CLOSED);

                        // Having closed, we're functionally useless; our state is dead to the world,
                        // of no concern to anyone. Thus, we no longer should reasonably contribute to
                        // any global warnings.
                        unregisterGlobalWarningSource();

                        tracer.trace("...doClose([%s])", getSerialNumber());
                        }
                    }
                }
            });
        }

    /** intended as subclass hook */
    protected void doCloseFromArmed() throws RobotCoreException, InterruptedException
        {
        this.disarm();
        }

    /** intended as subclass hook */
    protected void doCloseFromOther() throws RobotCoreException, InterruptedException
        {
        this.disarm();
        }

    }
