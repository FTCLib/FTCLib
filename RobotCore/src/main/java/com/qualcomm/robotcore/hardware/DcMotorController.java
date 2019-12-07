/*
 * Copyright (c) 2014, 2015 Qualcomm Technologies Inc
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * (subject to the limitations in the disclaimer below) provided that the following conditions are
 * met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the
 * distribution.
 *
 * Neither the name of Qualcomm Technologies Inc nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS LICENSE. THIS
 * SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.qualcomm.robotcore.hardware;

import com.qualcomm.robotcore.hardware.configuration.typecontainers.MotorConfigurationType;

/**
 * Interface for working with DC Motor Controllers
 * <p>
 * Different DC motor controllers will implement this interface.
 */
@SuppressWarnings("unused")
public interface DcMotorController extends HardwareDevice {

  /**
   * Informs the motor controller of the type of a particular motor. This is normally used
   * only as part of the initialization of a motor.
   *
   * @param motor port of the motor in question
   * @param motorType the motor type.
   */
  void setMotorType(int motor, MotorConfigurationType motorType);

  /**
   * Retrieves the motor type configured for this motor
   * @param motor the motor in question
   * @return the configured type for that motor, or {@link MotorConfigurationType#getUnspecifiedMotorType()}
   * if no type has been configured
   */
  MotorConfigurationType getMotorType(int motor);

  /**
   * Set the current motor mode. {@link DcMotor.RunMode}
   *
   * @param motor port of motor
   * @param mode run mode
   */
  void setMotorMode(int motor, DcMotor.RunMode mode);

  /**
   * Get the current motor mode. Returns the current "run mode".
   *
   * @param motor port of motor
   * @return run mode
   */
  DcMotor.RunMode getMotorMode(int motor);

  /**
   * Set the current motor power
   *
   * @param motor port of motor
   * @param power from -1.0 to 1.0
   */
  void setMotorPower(int motor, double power);

  /**
   * Get the current motor power
   *
   * @param motor port of motor
   * @return scaled from -1.0 to 1.0
   */
  double getMotorPower(int motor);

  /**
   * Is the motor busy?
   *
   * @param motor port of motor
   * @return true if the motor is busy
   */
  boolean isBusy(int motor);

  /**
   * Sets the behavior of the motor when zero power is applied.
   * @param zeroPowerBehavior the behavior of the motor when zero power is applied.
   */
  void setMotorZeroPowerBehavior(int motor, DcMotor.ZeroPowerBehavior zeroPowerBehavior);

  /**
   * Returns the current zero power behavior of the motor.
   * @return the current zero power behavior of the motor.
   */
  DcMotor.ZeroPowerBehavior getMotorZeroPowerBehavior(int motor);

  /**
   * Is motor power set to float?
   *
   * @param motor port of motor
   * @return true of motor is set to float
   */
  boolean getMotorPowerFloat(int motor);

  /**
   * Set the motor target position. This takes in an integer, which is not scaled.
   *
   * Motor power should be positive if using run to position
   *  @param motor port of motor
   * @param position range from Integer.MIN_VALUE to Integer.MAX_VALUE
   */
  void setMotorTargetPosition(int motor, int position);

  /**
   * Get the current motor target position
   *
   * @param motor port of motor
   * @return integer, unscaled
   */
  int getMotorTargetPosition(int motor);

  /**
   * Get the current motor position
   *
   * @param motor port of motor
   * @return integer, unscaled
   */
  int getMotorCurrentPosition(int motor);

  /**
   * Reset the state we hold for the given motor so that it's clean at the start of an opmode
   * @param motor
   */
  void resetDeviceConfigurationForOpMode(int motor);
}
