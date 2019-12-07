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
package com.qualcomm.hardware.hitechnic;

import android.content.Context;

import com.qualcomm.robotcore.hardware.Engagable;
import com.qualcomm.robotcore.hardware.HardwareDevice;
import com.qualcomm.robotcore.hardware.I2cAddr;
import com.qualcomm.robotcore.hardware.I2cController;
import com.qualcomm.robotcore.hardware.I2cControllerPortDeviceImpl;
import com.qualcomm.robotcore.hardware.I2cDevice;
import com.qualcomm.robotcore.hardware.I2cDeviceImpl;
import com.qualcomm.robotcore.hardware.I2cDeviceSynch;
import com.qualcomm.robotcore.hardware.I2cDeviceSynchImpl;
import com.qualcomm.robotcore.hardware.I2cWaitControl;

/**
 * HiTechnicNxtController serves as a common implementation base for legacy motor and servo
 * controllers. Its primary functionality is to keep track of user's intent as to whether or
 * not a controller should be engaged with its underlying I2cDevice, and manage the state transitions
 * associated with doing so.
 */
public abstract class HiTechnicNxtController extends I2cControllerPortDeviceImpl implements Engagable, HardwareDevice
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected Context        context;
    protected boolean        isEngaged;    // does the user want us to connect to the underlying device?
    protected boolean        isHooked;     // are we presently connected to the underlying device?
    protected I2cDevice      i2cDevice;
    protected I2cDeviceSynch i2cDeviceSynch;
    protected boolean        isHardwareInitialized;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public HiTechnicNxtController(Context context, I2cController module, int physicalPort, I2cAddr i2cAddr)
        {
        super(module, physicalPort);
        this.context   = context;
        this.isEngaged = true;      // for historical compatibility
        this.isHooked  = false;
        this.isHardwareInitialized = false;

        this.i2cDevice      = new I2cDeviceImpl(module, physicalPort);
        this.i2cDeviceSynch = new I2cDeviceSynchImpl(i2cDevice, i2cAddr, true); // we own the i2cDevice
        }

    //----------------------------------------------------------------------------------------------
    // HardwareDevice
    //----------------------------------------------------------------------------------------------

    @Override public Manufacturer getManufacturer()
        {
        return Manufacturer.HiTechnic;
        }

    @Override public synchronized void close()
        {
        if (this.isEngaged())
            {
            this.floatHardware();
            this.disengage();
            }
        this.i2cDeviceSynch.close();
        }

    void initializeHardwareIfNecessary()
        {
        if (!this.isHardwareInitialized && this.isArmed())
            {
            this.isHardwareInitialized = true;
            initializeHardware();
            }
        }

    protected abstract void initializeHardware();
    protected abstract void floatHardware();

    //----------------------------------------------------------------------------------------------
    // Engagable
    //----------------------------------------------------------------------------------------------

    @Override
    public synchronized void engage()
        {
        this.isEngaged = true;
        adjustHookingToMatchEngagement();
        }

    @Override
    public synchronized void disengage()
        {
        this.isEngaged = true;
        adjustHookingToMatchEngagement();
        }

    @Override
    public synchronized boolean isEngaged()
        {
        return this.isEngaged;
        }

    //----------------------------------------------------------------------------------------------
    // Controller comings and goings
    //----------------------------------------------------------------------------------------------

    @Override
    protected void controllerNowDisarmed()
        {
        if (this.isHooked)
            {
            this.unhook();
            }
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
        this.initializeHardwareIfNecessary();
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
        return this.i2cDeviceSynch.isArmed();
        }

    protected void write8(int ireg, byte data)
        {
        if (this.isEngaged())
            {
            this.i2cDeviceSynch.write8(ireg, data, I2cWaitControl.NONE);
            }
        }

    protected void write(int ireg, byte[] data)
        {
        if (this.isEngaged())
            {
            this.i2cDeviceSynch.write(ireg, data, I2cWaitControl.NONE);
            }
        }

    protected byte read8(int ireg)
        {
        return this.isEngaged() ? this.i2cDeviceSynch.read8(ireg) : 0;
        }
    }
