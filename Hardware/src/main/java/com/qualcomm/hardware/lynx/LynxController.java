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
package com.qualcomm.hardware.lynx;

import android.content.Context;
import android.support.annotation.NonNull;

import com.qualcomm.hardware.lynx.commands.LynxCommand;
import com.qualcomm.hardware.lynx.commands.LynxMessage;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.Engagable;
import com.qualcomm.robotcore.hardware.HardwareDevice;
import com.qualcomm.robotcore.hardware.HardwareDeviceHealth;
import com.qualcomm.robotcore.hardware.HardwareDeviceHealthImpl;
import com.qualcomm.robotcore.hardware.usb.RobotArmingStateNotifier;
import com.qualcomm.robotcore.hardware.usb.RobotUsbModule;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.SerialNumber;
import com.qualcomm.robotcore.util.WeakReferenceSet;

import java.util.concurrent.Callable;

/**
 * Created by bob on 2016-03-07.
 */
@SuppressWarnings("WeakerAccess")
public abstract class LynxController extends LynxCommExceptionHandler implements Engagable, HardwareDevice, HardwareDeviceHealth, RobotArmingStateNotifier.Callback, RobotArmingStateNotifier
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected abstract String getTag();

    protected Context       context;
    private   LynxModule    module;
    protected boolean       isHardwareInitialized;
    protected boolean       isEngaged;    // does the user want us to connect to the underlying device?
    protected boolean       isHooked;     // are we presently connected to the underlying device?
    private   LynxModuleIntf pretendModule;
    protected final WeakReferenceSet<Callback> registeredCallbacks = new WeakReferenceSet<Callback>();
    protected final HardwareDeviceHealthImpl hardwareDeviceHealth;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public LynxController(Context context, LynxModule module)
        {
        this.context = context;
        this.module  = module;
        this.isEngaged = true;
        this.isHooked  = false;
        this.isHardwareInitialized = false;
        this.pretendModule = new PretendLynxModule();
        this.hardwareDeviceHealth = new HardwareDeviceHealthImpl(getTag(), getHealthStatusOverride());
        //
        this.module.noteController(this);
        }

    protected void finishConstruction()
        {
        moduleNowArmedOrPretending();
        this.module.registerCallback(this, false);
        }

    @Override
    public synchronized void onModuleStateChange(RobotArmingStateNotifier module, RobotUsbModule.ARMINGSTATE state)
        {
        // Adjust ourselves first
        switch (state)
            {
            case ARMED:
            case PRETENDING:
                moduleNowArmedOrPretending();
                break;
            case DISARMED:
                moduleNowDisarmed();
                break;
            }

        // THEN tell any of our clients so that they see our status
        for (Callback callback : registeredCallbacks)
            {
            callback.onModuleStateChange(this, state);
            }
        }

    protected void moduleNowArmedOrPretending()
        {
        adjustHookingToMatchEngagement();
        }

    protected void moduleNowDisarmed()
        {
        if (this.isHooked)
            {
            this.unhook();
            }
        }

    //----------------------------------------------------------------------------------------------
    // RobotArmingStateNotifier
    //----------------------------------------------------------------------------------------------

    @Override
    public SerialNumber getSerialNumber()
        {
        return this.module.getSerialNumber();
        }

    @Override
    public ARMINGSTATE getArmingState()
        {
        return this.module.getArmingState();
        }

    @Override
    public void registerCallback(Callback callback, boolean doInitialCallback)
        {
        registeredCallbacks.add(callback);
        if (doInitialCallback)
            {
            callback.onModuleStateChange(this, this.getArmingState());
            }
        }

    @Override
    public void unregisterCallback(Callback callback)
        {
        registeredCallbacks.remove(callback);
        }

    //----------------------------------------------------------------------------------------------
    // HardwareDevice
    //----------------------------------------------------------------------------------------------

    @Override public Manufacturer getManufacturer()
        {
        return Manufacturer.Lynx;
        }

    @Override public synchronized void close()
        {
        if (this.isEngaged())
            {
            // Float so as to, e.g., de-energize motors and servos
            this.floatHardware();
            this.disengage();
            }
        setHealthStatus(HealthStatus.CLOSED);
        }

    @Override public String getConnectionInfo()
        {
        return this.getModule().getConnectionInfo();
        }

    @Override public int getVersion()
        {
        return 1;
        }

    @Override public abstract String getDeviceName();

    @Override public void resetDeviceConfigurationForOpMode()
        {
        // Before every opmode, we put the device into an expected state so that user
        // level initialization logic should always see the same thing and thus need only
        // explicitly initialize that which is different than same.
        try {
            initializeHardware();
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            }
        catch (RobotCoreException e)
            {
            RobotLog.vv(getTag(), e, "exception initializing hardware; ignored");
            }
        }

    void initializeHardwareIfNecessary() throws RobotCoreException, InterruptedException
        {
        // Make sure we initialize the hardware at least once: we do so the first
        // time we successfully connect (and thus arm()).
        //
        if (!this.isHardwareInitialized)
            {
            RobotLog.vv(getTag(), "initializeHardware() mod#=%d", getModule().getModuleAddress());
            initializeHardware();
            this.isHardwareInitialized = this.isArmed();    // do this again if only pretending
            }
        }

    protected void initializeHardware() throws RobotCoreException, InterruptedException
        {
        // Subclass hook
        }
    protected void floatHardware()
        {
        // Subclass hook
        }
    public void forgetLastKnown()
        {
        // Subclass hook
        }

    protected void setHealthyIfArmed()
        {
        if (isArmed())
            {
            setHealthStatus(HealthStatus.HEALTHY);
            }
        }

    @Override
    public void setHealthStatus(HealthStatus status)
        {
        hardwareDeviceHealth.setHealthStatus(status);
        }

    protected Callable<HealthStatus> getHealthStatusOverride()
        {
        return new Callable<HealthStatus>()
            {
            @Override public HealthStatus call() throws Exception
                {
                if (LynxController.this.module.getArmingState() == ARMINGSTATE.PRETENDING)
                    {
                    return HealthStatus.UNHEALTHY;
                    }
                return HealthStatus.UNKNOWN;
                }
            };
        }

    @Override
    public HealthStatus getHealthStatus()
        {
        return hardwareDeviceHealth.getHealthStatus();
        }

    //----------------------------------------------------------------------------------------------
    // Engagable
    //----------------------------------------------------------------------------------------------

    @Override
    public void engage()
        {
        synchronized (this)
            {
            if (!isEngaged)
                {
                RobotLog.vv(getTag(), "engaging mod#=%d", getModule().getModuleAddress());
                isEngaged = true;
                adjustHookingToMatchEngagement();
                }
            }
        }

    @Override
    public void disengage()
        {
        synchronized (this)
            {
            if (!isEngaged)
                {
                RobotLog.vv(getTag(), "disengage mod#=%d", getModule().getModuleAddress());
                isEngaged = false;
                adjustHookingToMatchEngagement();
                }
            }
        }

    @Override
    public boolean isEngaged()
        {
        synchronized (this)
            {
            return this.isEngaged;
            }
        }

    //----------------------------------------------------------------------------------------------
    // Module comings and goings
    //----------------------------------------------------------------------------------------------

    protected LynxModuleIntf getModule()
        {
        // If we're not actually hooked, then don't actually communicate with
        // our underlying module.
        //
        // Note that the present implementation is such that hooking and unhooking should be
        // done very carefully if at all while commands are in flight.
        return this.isHooked ? this.module : this.pretendModule;
        }

    protected void adjustHookingToMatchEngagement()
    // Make our hook status match our intended hook status (aka engagement status)
        {
        if (!this.isHooked && this.isEngaged)
            {
            this.hook();
            }
        else if (this.isHooked && !this.isEngaged)
            {
            this.unhook();
            }
        }

    protected void hook()
        {
        this.doHook();
        this.isHooked = true;
        try {
            this.initializeHardwareIfNecessary();
            }
        catch (InterruptedException ignored)
            {
            Thread.currentThread().interrupt();
            }
        catch (RobotCoreException e)
            {
            RobotLog.ee(getTag(), e, "exception thrown in LynxController.hook()");
            }
        }

    protected void unhook()
        {
        this.doUnhook();
        this.isHooked = false;
        }

    protected void doHook()
        {
        // subclass responsibility
        }

    protected void doUnhook()
        {
        // subclass responsibility
        }

    //------------------------------------------------------------------------------------------------
    // Utility
    //------------------------------------------------------------------------------------------------

    protected boolean isArmed()
        {
        return this.module.getArmingState()== RobotArmingStateNotifier.ARMINGSTATE.ARMED;
        }

    //------------------------------------------------------------------------------------------------
    // Types
    //------------------------------------------------------------------------------------------------

    public class PretendLynxModule implements LynxModuleIntf
        {
        boolean isEngaged = true;

        @Override public Manufacturer getManufacturer()
            {
            return Manufacturer.Lynx;
            }

        @Override
        public String getFirmwareVersionString()
            {
            return getDeviceName();
            }

        @Override
        public String getDeviceName()
            {
            return LynxController.this.module.getDeviceName() + " (pretend)";
            }

        @Override
        public String getConnectionInfo()
            {
            return LynxController.this.module.getConnectionInfo();
            }

        @Override
        public int getVersion()
            {
            return 1;
            }

        @Override
        public void resetDeviceConfigurationForOpMode()
            {
            }

        @Override
        public void close()
            {
            }

        @Override public SerialNumber getSerialNumber()
            {
            return LynxController.this.module.getSerialNumber();
            }

        @Override
        public <T> T acquireI2cLockWhile(Supplier<T> supplier) throws InterruptedException, RobotCoreException, LynxNackException
            {
            return supplier.get();
            }

        @Override
        public void acquireNetworkTransmissionLock(LynxMessage message) throws InterruptedException
            {
            // do nothing
            }

        @Override
        public void releaseNetworkTransmissionLock(LynxMessage message) throws InterruptedException
            {
            // do nothing
            }

        @Override
        public void sendCommand(LynxMessage command) throws InterruptedException, LynxUnsupportedCommandException
            {
            // do nothing
            }

        @Override
        public void retransmit(LynxMessage message) throws InterruptedException
            {
            // do nothing
            }

        @Override
        public void finishedWithMessage(LynxMessage message) throws InterruptedException
            {
            // do nothing
            }

        @Override public void resetPingTimer(@NonNull LynxMessage message)
            {
            // do nothing
            }

        @Override
        public int getModuleAddress()
            {
            return LynxController.this.module.getModuleAddress();
            }

        @Override
        public void noteAttentionRequired()
            {
            // do nothing
            }

        @Override
        public int getInterfaceBaseCommandNumber(String interfaceName)
            {
            return LynxController.this.module.getInterfaceBaseCommandNumber(interfaceName);
            }

        @Override
        public boolean isParent()
            {
            return true;    // pretty arbitrary
            }

        @Override
        public void validateCommand(LynxMessage lynxMessage) throws LynxUnsupportedCommandException
            {
            }

        @Override public boolean isCommandSupported(Class<? extends LynxCommand> command)
            {
            return false;
            }

        @Override public boolean isEngaged()
            {
            return this.isEngaged;
            }

        @Override public void engage()
            {
            this.isEngaged = true;
            }

        @Override public void disengage()
            {
            this.isEngaged = false;
            }
        }
    }
