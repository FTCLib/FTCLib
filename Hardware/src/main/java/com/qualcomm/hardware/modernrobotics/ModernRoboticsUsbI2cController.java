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
import com.qualcomm.robotcore.hardware.I2cController;
import com.qualcomm.robotcore.util.SerialNumber;

/**
 * This class provides a common implementation point for functionality shared amongst
 * Modern Robotics USB I2cControllers. Not all possible shared functionality has yet been
 * lifted here; more sharing is possible.
 */
public abstract class ModernRoboticsUsbI2cController extends ModernRoboticsUsbDevice implements I2cController
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected final int                                 numberOfI2cPorts;
    protected final I2cPortReadyBeginEndNotifications[] i2cPortReadyBeginEndCallbacks;
    protected       boolean                             notificationsActive;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public ModernRoboticsUsbI2cController(int numberOfI2cPorts, Context context, SerialNumber serialNumber, SyncdDevice.Manager manager, OpenRobotUsbDevice openRobotUsbDevice, CreateReadWriteRunnable createReadWriteRunnable)
        throws RobotCoreException, InterruptedException
        {
        super(context, serialNumber, manager, openRobotUsbDevice, createReadWriteRunnable);
        this.numberOfI2cPorts              = numberOfI2cPorts;
        this.i2cPortReadyBeginEndCallbacks = new I2cPortReadyBeginEndNotifications[numberOfI2cPorts];
        this.notificationsActive           = false;
        }

    //----------------------------------------------------------------------------------------------
    // Utility
    //----------------------------------------------------------------------------------------------

    protected void throwIfI2cPortIsInvalid(int port)
        {
        if (port < 0 || port >= numberOfI2cPorts)
            {
            throw new IllegalArgumentException(String.format("port %d is invalid; valid ports are %d..%d", port, 0, numberOfI2cPorts - 1));
            }
        }

    //----------------------------------------------------------------------------------------------
    // Arming
    //----------------------------------------------------------------------------------------------

    @Override
    public synchronized boolean isArmed()
        {
        return super.isArmed();
        }

    //----------------------------------------------------------------------------------------------
    // Notifications management
    //----------------------------------------------------------------------------------------------

    @Override
    public synchronized void registerForPortReadyBeginEndCallback(I2cPortReadyBeginEndNotifications callback, int port)
        {
        // Validate arguments
        throwIfI2cPortIsInvalid(port);
        if (callback==null)
            throw new IllegalArgumentException(String.format("illegal null: registerForI2cNotificationsCallback(null,%d)", port));

        // Tear down anyone who's there
        this.deregisterForPortReadyBeginEndCallback(port);

        // Remember this guy
        i2cPortReadyBeginEndCallbacks[port] = callback;

        // Tell him things are starting
        if (this.notificationsActive)
            {
            try {
                callback.onPortIsReadyCallbacksBegin(port);
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                }
            }
        }

    @Override
    public synchronized I2cPortReadyBeginEndNotifications getPortReadyBeginEndCallback(int port)
        {
        throwIfI2cPortIsInvalid(port);
        return i2cPortReadyBeginEndCallbacks[port];
        }

    @Override
    public synchronized void deregisterForPortReadyBeginEndCallback(int port)
        {
        throwIfI2cPortIsInvalid(port);

        // Tell any existing guy he won't be notified any more. Note that we send this
        // even if notifications aren't currently active
        if (i2cPortReadyBeginEndCallbacks[port] != null)
            {
            try {
                i2cPortReadyBeginEndCallbacks[port].onPortIsReadyCallbacksEnd(port);
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                }
            }

        // forget anyone who's there
        i2cPortReadyBeginEndCallbacks[port] = null;
        }

    @Override
    public void startupComplete() throws InterruptedException
        {
        this.notificationsActive = true;

        if (i2cPortReadyBeginEndCallbacks != null)
            {
            for (int port = 0; port < numberOfI2cPorts; port++)
                {
                I2cPortReadyBeginEndNotifications callback = i2cPortReadyBeginEndCallbacks[port];
                if (callback != null)
                    {
                    callback.onPortIsReadyCallbacksBegin(port);
                    }
                }
            }
        }

    @Override
    public void shutdownComplete() throws InterruptedException
        {
        if (i2cPortReadyBeginEndCallbacks != null)
            {
            for (int port = 0; port < numberOfI2cPorts; port++)
                {
                I2cPortReadyBeginEndNotifications callback = i2cPortReadyBeginEndCallbacks[port];
                if (callback != null)
                    {
                    callback.onPortIsReadyCallbacksEnd(port);
                    }
                }
            }
        this.notificationsActive = false;
        }
    }
