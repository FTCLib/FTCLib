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

import android.support.annotation.NonNull;

import com.qualcomm.robotcore.hardware.configuration.LynxConstants;
import com.qualcomm.robotcore.hardware.configuration.typecontainers.MotorConfigurationType;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

/**
 * {@link DcMotorImplEx} is a motor that supports the {@link DcMotorEx} interface in addition
 * to simply {@link DcMotor}.
 */
@SuppressWarnings("WeakerAccess")
public class DcMotorImplEx extends DcMotorImpl implements DcMotorEx
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    DcMotorControllerEx controllerEx;
    int                 targetPositionTolerance = LynxConstants.DEFAULT_TARGET_POSITION_TOLERANCE;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public DcMotorImplEx(DcMotorController controller, int portNumber)
        {
        this(controller, portNumber, Direction.FORWARD);
        }

    public DcMotorImplEx(DcMotorController controller, int portNumber, Direction direction)
        {
        this(controller, portNumber, direction, MotorConfigurationType.getUnspecifiedMotorType());
        }

    public DcMotorImplEx(DcMotorController controller, int portNumber, Direction direction, @NonNull MotorConfigurationType motorType)
        {
        super(controller, portNumber, direction, motorType);
        this.controllerEx = (DcMotorControllerEx)controller;
        }

    //----------------------------------------------------------------------------------------------
    // DcMotorEx interface
    //----------------------------------------------------------------------------------------------

    @Override
    public void setMotorEnable()
        {
        controllerEx.setMotorEnable(this.getPortNumber());
        }

    @Override
    public void setMotorDisable()
        {
        controllerEx.setMotorDisable(this.getPortNumber());
        }

    @Override
    public boolean isMotorEnabled()
        {
        return controllerEx.isMotorEnabled(this.getPortNumber());
        }

    @Override public synchronized void setVelocity(double angularRate)
        {
        angularRate = adjustAngularRate(angularRate);
        controllerEx.setMotorVelocity(getPortNumber(), angularRate);
        }

    @Override public synchronized void setVelocity(double angularRate, AngleUnit unit)
        {
        angularRate = adjustAngularRate(angularRate);
        controllerEx.setMotorVelocity(getPortNumber(), angularRate, unit);
        }

    @Override public synchronized double getVelocity()
        {
        double angularRate = controllerEx.getMotorVelocity(this.getPortNumber());
        angularRate = adjustAngularRate(angularRate);
        return angularRate;
        }

    @Override
    public synchronized double getVelocity(AngleUnit unit)
        {
        double angularRate = controllerEx.getMotorVelocity(this.getPortNumber(), unit);
        angularRate = adjustAngularRate(angularRate);
        return angularRate;
        }

    protected double adjustAngularRate(double angularRate)
        {
        if (getOperationalDirection() == Direction.REVERSE) angularRate = -angularRate;
        return angularRate;
        }

    @Override public void setPIDCoefficients(RunMode mode, PIDCoefficients pidCoefficients)
        {
        controllerEx.setPIDCoefficients(this.getPortNumber(), mode, pidCoefficients);
        }

    @Override public void setPIDFCoefficients(RunMode mode, PIDFCoefficients pidfCoefficients)
        {
        controllerEx.setPIDFCoefficients(this.getPortNumber(), mode, pidfCoefficients);
        }

    @Override public void setVelocityPIDFCoefficients(double p, double i, double d, double f)
        {
        setPIDFCoefficients(RunMode.RUN_USING_ENCODER, new PIDFCoefficients(p, i, d, f, MotorControlAlgorithm.PIDF));
        }

    @Override public void setPositionPIDFCoefficients(double p)
        {
        setPIDFCoefficients(RunMode.RUN_TO_POSITION, new PIDFCoefficients(p, 0, 0, 0, MotorControlAlgorithm.PIDF));
        }

    @Override public PIDCoefficients getPIDCoefficients(RunMode mode)
        {
        return controllerEx.getPIDCoefficients(this.getPortNumber(), mode);
        }

    @Override public PIDFCoefficients getPIDFCoefficients(RunMode mode)
        {
        return controllerEx.getPIDFCoefficients(this.getPortNumber(), mode);
        }

    @Override public int getTargetPositionTolerance()
        {
        return this.targetPositionTolerance;
        }

    @Override synchronized public void setTargetPositionTolerance(int tolerance)
        {
        this.targetPositionTolerance = tolerance;
        }

    @Override protected void internalSetTargetPosition(int position)
        {
        this.controllerEx.setMotorTargetPosition(portNumber, position, this.targetPositionTolerance);
        }
    }
