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

public abstract class I2cControllerPortDeviceImpl implements RobotArmingStateNotifier.Callback, I2cControllerPortDevice
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected final I2cController controller;
    protected final int           physicalPort;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    protected I2cControllerPortDeviceImpl(I2cController controller, int physicalPort)
        {
        this.controller = controller;
        this.physicalPort = physicalPort;
        }

    protected void finishConstruction()
        {
        controllerNowArmedOrPretending();

        if (controller instanceof RobotArmingStateNotifier)
            ((RobotArmingStateNotifier) controller).registerCallback((RobotArmingStateNotifier.Callback) this, false);
        }

    /** intended as a subclass hook */
    protected void controllerNowArmedOrPretending() { }

    /** intended as a subclass hook */
    protected void controllerNowDisarmed() { }

    @Override
    public synchronized void onModuleStateChange(RobotArmingStateNotifier module, RobotArmingStateNotifier.ARMINGSTATE state) {
    // Each time our module creates a new ReadWriteRunnable, we need to refresh our connection to same
    switch (state)
        {
        case ARMED:
        case PRETENDING:
            controllerNowArmedOrPretending();
            break;
        case DISARMED:
            controllerNowDisarmed();
            break;
        }
    }

    //----------------------------------------------------------------------------------------------
    // I2cControllerPortDevice
    //----------------------------------------------------------------------------------------------

    public I2cController getI2cController()
        {
        return this.controller;
        }

    public int getPort()
        {
        return this.physicalPort;
        }
    }
