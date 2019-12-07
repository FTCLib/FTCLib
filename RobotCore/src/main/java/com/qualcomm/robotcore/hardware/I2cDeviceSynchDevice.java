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

import com.qualcomm.robotcore.hardware.usb.RobotArmingStateNotifier;

/**
 * {@link I2cDeviceSynchDevice} instances are I2c devices which are built on top of
 * I2cDeviceSynchSimple instances or subclasses thereof. The class provides common and
 * handy utility services for such devices.
 */
@SuppressWarnings("WeakerAccess")
public abstract class I2cDeviceSynchDevice<DEVICE_CLIENT extends I2cDeviceSynchSimple> implements RobotArmingStateNotifier.Callback, HardwareDevice
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected DEVICE_CLIENT deviceClient;
    protected boolean       deviceClientIsOwned;
    protected boolean       isInitialized;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    protected I2cDeviceSynchDevice(DEVICE_CLIENT deviceClient, boolean deviceClientIsOwned)
        {
        this.deviceClient        = deviceClient;
        this.deviceClientIsOwned = deviceClientIsOwned;
        this.isInitialized       = false;
        this.deviceClient.enableWriteCoalescing(false);
        }

    protected void registerArmingStateCallback(boolean doInitialCallback)
        {
        if (deviceClient instanceof RobotArmingStateNotifier)
            {
            ((RobotArmingStateNotifier)deviceClient).registerCallback(this, doInitialCallback);
            }
        }

    protected void engage()
        {
        if (this.deviceClient instanceof Engagable)
            {
            ((Engagable)this.deviceClient).engage();
            }
        }

    protected void disengage()
        {
        if (this.deviceClient instanceof Engagable)
            {
            ((Engagable)this.deviceClient).disengage();
            }
        }

    //----------------------------------------------------------------------------------------------
    // Accessing
    //----------------------------------------------------------------------------------------------

    public DEVICE_CLIENT getDeviceClient()
        {
        return this.deviceClient;
        }

    //----------------------------------------------------------------------------------------------
    // RobotArmingStateNotifier.Callback
    //----------------------------------------------------------------------------------------------

    @Override
    public void onModuleStateChange(RobotArmingStateNotifier module, RobotArmingStateNotifier.ARMINGSTATE state)
        {
        // We need to make sure that the actual hardware gets initialized at least once
        if (state == RobotArmingStateNotifier.ARMINGSTATE.ARMED)
            {
            initializeIfNecessary();
            }
        else if (state == RobotArmingStateNotifier.ARMINGSTATE.PRETENDING)
            {
            // At least make things basically sane
            initializeIfNecessary();
            // Next time we arm, we will re-init, as we don't know what's been up with that sensor hw
            this.isInitialized = false;
            }
        }

    protected synchronized void initializeIfNecessary()
        {
        if (!this.isInitialized)
            {
            this.initialize();
            }
        }

    public synchronized boolean initialize()
        {
        this.isInitialized = this.doInitialize();
        return this.isInitialized;
        }

    /**
     * Actually carries out the initialization of the instance.
     * @return Whether the initialization was successful or not
     */
    protected abstract boolean doInitialize();

    //----------------------------------------------------------------------------------------------
    // HardwareDevice
    //----------------------------------------------------------------------------------------------

    @Override
    public void resetDeviceConfigurationForOpMode()
        {
        this.deviceClient.resetDeviceConfigurationForOpMode();
        this.initialize();
        }

    @Override public void close()
        {
        if (this.deviceClientIsOwned)
            {
            this.deviceClient.close();
            }
        }

    @Override
    public int getVersion()
        {
        return 1;
        }

    @Override
    public String getConnectionInfo()
        {
        return this.deviceClient.getConnectionInfo();
        }
    }
