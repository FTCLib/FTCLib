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

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

/**
 * DcMotorControllerEx is an optional motor controller interface supported by some hardware
 * that provides enhanced motor functionality.
 * @see DcMotorEx
 */
public interface DcMotorControllerEx extends DcMotorController
    {
    /**
     * Individually energizes a particular motor
     * @param motor the port number of the motor on this controller
     * @see #setMotorDisable(int)
     * @see #isMotorEnabled(int)
     */
    void setMotorEnable(int motor);

    /**
     * Individually denergizes a particular motor
     * @param motor the port number of the motor on this controller
     * @see #setMotorEnable(int)
     * @see #isMotorEnabled(int)
     */
    void setMotorDisable(int motor);

    /**
     * Returns whether a particular motor on the controller is energized
     * @param motor the port number of the motor on this controller
     * @see #setMotorEnable(int)
     * @see #setMotorDisable(int)
     */
    boolean isMotorEnabled(int motor);

    /**
     * Sets the target velocity of the indicated motor.
     * @param motor the port number of the motor on this controller
     * @param ticksPerSecond the new target rate for that motor, in ticks per second
     */
    void setMotorVelocity(int motor, double ticksPerSecond);

    /**
     * Sets the target velocity of the indicated motor.
     * @param motor         motor whose velocity is to be adjusted
     * @param angularRate   the new target rate for that motor, in 'unit's per second
     * @param unit          the unit inw which angularRate is expressed.
     * @see DcMotorEx#setVelocity(double, AngleUnit)
     */
    void setMotorVelocity(int motor, double angularRate, AngleUnit unit);

    /**
     * Returns the velocity of the indicated motor in ticks per second.
     * @param motor         the motor whose velocity is desired
     * @return              the current target velocity of the motor in ticks per second
     */
    double getMotorVelocity(int motor);

    /**
     * Returns the velocity of the indicated motor.
     * @param motor         the motor whose velocity is desired
     * @param unit          the angular unit in which the velocity is to be expressed
     * @return              the current velocity of the motor
     * @see DcMotorEx#getVelocity(AngleUnit)
     */
    double getMotorVelocity(int motor, AngleUnit unit);

    /**
     * Sets the coefficients used for PID control on the indicated motor when in the indicated mode
     * @param motor the motor whose PID coefficients are to be set
     * @param mode the mode on that motor whose coefficients are to be set
     * @param pidCoefficients the new coefficients to set
     *
     * @see DcMotorEx#setPIDCoefficients(DcMotor.RunMode, PIDCoefficients)
     * @see #getPIDCoefficients(int, DcMotor.RunMode)
     *
     * @deprecated Use {@link #setPIDFCoefficients(int, DcMotor.RunMode, PIDFCoefficients)} instead
     */
    @Deprecated
    void setPIDCoefficients(int motor, DcMotor.RunMode mode, PIDCoefficients pidCoefficients);

    /**
     * Sets the coefficients used for PIDF control on the indicated motor when in the indicated mode
     * @param motor the motor whose PIDF coefficients are to be set
     * @param mode the mode on that motor whose coefficients are to be set
     * @param pidfCoefficients the new coefficients to set
     *
     * @see DcMotorEx#setPIDFCoefficients(DcMotor.RunMode, PIDFCoefficients)
     * @see #getPIDFCoefficients(int, DcMotor.RunMode)
     */
    void setPIDFCoefficients(int motor, DcMotor.RunMode mode, PIDFCoefficients pidfCoefficients) throws UnsupportedOperationException;

    /**
     * Returns the coefficients used for PID control on the indicated motor when in the indicated mode
     * @param motor the motor whose PID coefficients are desired
     * @param mode the mode on that motor whose coefficients are to be queried
     * @return the coefficients used for PID control on the indicated motor when in the indicated mode
     *
     * @see DcMotorEx#getPIDCoefficients(DcMotor.RunMode)
     * @see #setPIDCoefficients(int, DcMotor.RunMode, PIDCoefficients)
     *
     * @deprecated Use {@link #getPIDFCoefficients(int, DcMotor.RunMode)} instead
     */
    @Deprecated
    PIDCoefficients getPIDCoefficients(int motor, DcMotor.RunMode mode);

    /**
     * Returns the coefficients used for PIDF control on the indicated motor when in the indicated mode
     * @param motor the motor whose PIDF coefficients are desired
     * @param mode the mode on that motor whose coefficients are to be queried
     * @return the coefficients used for PIDF control on the indicated motor when in the indicated mode
     *
     * @see DcMotorEx#getPIDCoefficients(DcMotor.RunMode)
     * @see #setPIDFCoefficients(int, DcMotor.RunMode, PIDFCoefficients)
     */
    PIDFCoefficients getPIDFCoefficients(int motor, DcMotor.RunMode mode);

    /**
     * Sets the target position and tolerance for a 'run to position' operation.
     *
     * @param motor     the motor number to be affected
     * @param position  the desired target position, in encoder ticks
     * @param tolerance the tolerance of the desired target position, in encoder ticks
     */
    void setMotorTargetPosition(int motor, int position, int tolerance);
    }
