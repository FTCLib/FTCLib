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

import androidx.annotation.NonNull;

import com.qualcomm.robotcore.hardware.configuration.typecontainers.ServoConfigurationType;

/**
 * CRServoEx provides access to extended functionality on continuous rotation
 * servos. Implementations support both the {@link CRServo} and {@link PwmControl} interfaces.
 */
public class CRServoImplEx extends CRServoImpl implements PwmControl
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected ServoControllerEx controllerEx;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public CRServoImplEx(ServoControllerEx controller, int portNumber, @NonNull ServoConfigurationType servoType)
        {
        this(controller, portNumber, DcMotor.Direction.FORWARD, servoType);
        }

    public CRServoImplEx(ServoControllerEx controller, int portNumber, DcMotor.Direction direction, @NonNull ServoConfigurationType servoType)
        {
        super(controller, portNumber, direction);
        this.controllerEx = controller;
        controllerEx.setServoType(portNumber, servoType);
        }

    //----------------------------------------------------------------------------------------------
    // PwmControl
    //----------------------------------------------------------------------------------------------

    @Override
    public void setPwmRange(PwmRange range)
        {
        controllerEx.setServoPwmRange(this.getPortNumber(), range);
        }

    @Override
    public PwmRange getPwmRange()
        {
        return controllerEx.getServoPwmRange(this.getPortNumber());
        }

    @Override
    public void setPwmEnable()
        {
        controllerEx.setServoPwmEnable(this.getPortNumber());
        }

    @Override
    public void setPwmDisable()
        {
        controllerEx.setServoPwmDisable(this.getPortNumber());
        }

    @Override
    public boolean isPwmEnabled()
        {
        return controllerEx.isServoPwmEnabled(this.getPortNumber());
        }
    }
