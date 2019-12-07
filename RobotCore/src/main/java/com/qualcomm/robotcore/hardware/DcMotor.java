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

import com.qualcomm.robotcore.hardware.configuration.typecontainers.MotorConfigurationType;

/**
 * DcMotor interface provides access to full-featured motor functionality.
 */
public interface DcMotor extends DcMotorSimple
    {
    /**
     * Returns the assigned type for this motor. If no particular motor type has been
     * configured, then {@link MotorConfigurationType#getUnspecifiedMotorType()} will be returned.
     * Note that the motor type for a given motor is initially assigned in the robot
     * configuration user interface, though it may subsequently be modified using methods herein.
     * @return the assigned type for this motor
     */
    MotorConfigurationType getMotorType();

    /**
     * Sets the assigned type of this motor. Usage of this method is very rare.
     * @param motorType the new assigned type for this motor
     * @see #getMotorType() 
     */
    void setMotorType(MotorConfigurationType motorType);

    /**
     * Returns the underlying motor controller on which this motor is situated.
     * @return the underlying motor controller on which this motor is situated.
     * @see #getPortNumber()
     */
    DcMotorController getController();

    /**
     * Returns the port number on the underlying motor controller on which this motor is situated.
     * @return the port number on the underlying motor controller on which this motor is situated.
     * @see #getController()
     */
    int getPortNumber();

    /**
     * ZeroPowerBehavior provides an indication as to a motor's behavior when a power level of zero
     * is applied.
     * @see #setZeroPowerBehavior(ZeroPowerBehavior) 
     * @see #setPower(double)
     */
    enum ZeroPowerBehavior
        {
        /** The behavior of the motor when zero power is applied is not currently known. This value
         * is mostly useful for your internal state variables. It may not be passed as a parameter
         * to {@link #setZeroPowerBehavior(ZeroPowerBehavior)} and will never be returned from
         * {@link #getZeroPowerBehavior()}*/
        UNKNOWN,
        /** The motor stops and then brakes, actively resisting any external force which attempts
         * to turn the motor. */
        BRAKE,
        /** The motor stops and then floats: an external force attempting to turn the motor is not
         * met with active resistence. */
        FLOAT
        }

    /**
     * Sets the behavior of the motor when a power level of zero is applied.
     * @param zeroPowerBehavior the new behavior of the motor when a power level of zero is applied.
     * @see ZeroPowerBehavior
     * @see #setPower(double)
     */
    void setZeroPowerBehavior(ZeroPowerBehavior zeroPowerBehavior);

    /**
     * Returns the current behavior of the motor were a power level of zero to be applied.
     * @return the current behavior of the motor were a power level of zero to be applied.
     */
    ZeroPowerBehavior getZeroPowerBehavior();

    /**
     * Sets the zero power behavior of the motor to {@link ZeroPowerBehavior#FLOAT FLOAT}, then
     * applies zero power to that motor.
     *
     * <p>Note that the change of the zero power behavior to {@link ZeroPowerBehavior#FLOAT FLOAT}
     * remains in effect even following the return of this method. <STRONG>This is a breaking
     * change</STRONG> in behavior from previous releases of the SDK. Consider, for example, the
     * following code sequence:</p>
     *
     * <pre>
     *     motor.setZeroPowerBehavior(ZeroPowerBehavior.BRAKE); // method not available in previous releases
     *     motor.setPowerFloat();
     *     motor.setPower(0.0);
     * </pre>
     *
     * <p>Starting from this release, this sequence of code will leave the motor floating. Previously,
     * the motor would have been left braked.</p>
     *
     * @see #setPower(double)
     * @see #getPowerFloat()
     * @see #setZeroPowerBehavior(ZeroPowerBehavior)
     * @deprecated This method is deprecated in favor of direct use of
     *       {@link #setZeroPowerBehavior(ZeroPowerBehavior) setZeroPowerBehavior()} and
     *       {@link #setPower(double) setPower()}.
     */
    @Deprecated void setPowerFloat();

    /**
     * Returns whether the motor is currently in a float power level.
     * @return whether the motor is currently in a float power level.
     * @see #setPowerFloat()
     */
    boolean getPowerFloat();

    /**
     * Sets the desired encoder target position to which the motor should advance or retreat
     * and then actively hold thereat. This behavior is similar to the operation of a servo.
     * The maximum speed at which this advance or retreat occurs is governed by the power level
     * currently set on the motor. While the motor is advancing or retreating to the desired
     * taget position, {@link #isBusy()} will return true.
     *
     * <p>Note that adjustment to a target position is only effective when the motor is in
     * {@link RunMode#RUN_TO_POSITION RUN_TO_POSITION}
     * RunMode. Note further that, clearly, the motor must be equipped with an encoder in order
     * for this mode to function properly.</p>
     *
     * @param position the desired encoder target position
     * @see #getCurrentPosition()
     * @see #setMode(RunMode)
     * @see RunMode#RUN_TO_POSITION
     * @see #getTargetPosition()
     * @see #isBusy()
     */
    void setTargetPosition(int position);

    /**
     * Returns the current target encoder position for this motor.
     * @return the current target encoder position for this motor.
     * @see #setTargetPosition(int)
     */
    int getTargetPosition();

    /**
     * Returns true if the motor is currently advancing or retreating to a target position.
     * @return true if the motor is currently advancing or retreating to a target position.
     * @see #setTargetPosition(int)
     */
    boolean isBusy();

    /**
     * Returns the current reading of the encoder for this motor. The units for this reading,
     * that is, the number of ticks per revolution, are specific to the motor/encoder in question,
     * and thus are not specified here.
     * @return the current reading of the encoder for this motor
     * @see #getTargetPosition()
     * @see RunMode#STOP_AND_RESET_ENCODER
     */
    int getCurrentPosition();

    /**
     * The run mode of a motor {@link RunMode} controls how the motor interprets the
     * it's parameter settings passed through power- and encoder-related methods.
     * Some of these modes internally use <a href="https://en.wikipedia.org/wiki/PID_controller">PID</a>
     * control to achieve their function, while others do not. Those that do are referred
     * to as "PID modes".
     */
    enum RunMode
        {
        /** The motor is simply to run at whatever velocity is achieved by apply a particular
         * power level to the motor.
         */
        RUN_WITHOUT_ENCODER,

        /** The motor is to do its best to run at targeted velocity. An encoder must be affixed
         * to the motor in order to use this mode. This is a PID mode.
         */
        RUN_USING_ENCODER,

        /** The motor is to attempt to rotate in whatever direction is necessary to cause the
         * encoder reading to advance or retreat from its current setting to the setting which
         * has been provided through the {@link #setTargetPosition(int) setTargetPosition()} method.
         * An encoder must be affixed to this motor in order to use this mode. This is a PID mode.
         */
        RUN_TO_POSITION,

        /** The motor is to set the current encoder position to zero. In contrast to
         * {@link com.qualcomm.robotcore.hardware.DcMotor.RunMode#RUN_TO_POSITION RUN_TO_POSITION},
         * the motor is not rotated in order to achieve this; rather, the current rotational
         * position of the motor is simply reinterpreted as the new zero value. However, as
         * a side effect of placing a motor in this mode, power is removed from the motor, causing
         * it to stop, though it is unspecified whether the motor enters brake or float mode.
         *
         * Further, it should be noted that setting a motor to{@link RunMode#STOP_AND_RESET_ENCODER
         * STOP_AND_RESET_ENCODER} may or may not be a transient state: motors connected to some motor
         * controllers will remain in this mode until explicitly transitioned to a different one, while
         * motors connected to other motor controllers will automatically transition to a different
         * mode after the reset of the encoder is complete.
         */
        STOP_AND_RESET_ENCODER,

        /** @deprecated Use {@link #RUN_WITHOUT_ENCODER} instead */
        @Deprecated RUN_WITHOUT_ENCODERS,

        /** @deprecated Use {@link #RUN_USING_ENCODER} instead */
        @Deprecated RUN_USING_ENCODERS,

        /** @deprecated Use {@link #STOP_AND_RESET_ENCODER} instead */
        @Deprecated RESET_ENCODERS;

        /** Returns the new new constant corresponding to old constant names.
         * @deprecated Replace use of old constants with new */
        @Deprecated
        public RunMode migrate()
            {
            switch (this)
                {
                case RUN_WITHOUT_ENCODERS: return RUN_WITHOUT_ENCODER;
                case RUN_USING_ENCODERS: return RUN_USING_ENCODER;
                case RESET_ENCODERS: return STOP_AND_RESET_ENCODER;
                default: return this;
                }
            }

        /**
         * Returns whether this RunMode is a PID-controlled mode or not
         * @return whether this RunMode is a PID-controlled mode or not
         */
        public boolean isPIDMode()
            {
            return this==RUN_USING_ENCODER || this==RUN_USING_ENCODERS || this==RUN_TO_POSITION;
            }
        }

    /**
     * Sets the current run mode for this motor
     * @param mode the new current run mode for this motor
     * @see RunMode
     * @see #getMode()
     */
    void setMode(RunMode mode);

    /**
     * Returns the current run mode for this motor
     * @return the current run mode for this motor
     * @see RunMode
     * @see #setMode(RunMode)
     */
    RunMode getMode();
    }
