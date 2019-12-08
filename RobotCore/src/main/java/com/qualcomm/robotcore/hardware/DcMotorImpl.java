/* Copyright (c) 2014, 2015 Qualcomm Technologies Inc

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of Qualcomm Technologies Inc nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. */

package com.qualcomm.robotcore.hardware;

import androidx.annotation.NonNull;

import com.qualcomm.robotcore.R;
import com.qualcomm.robotcore.hardware.configuration.typecontainers.MotorConfigurationType;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.external.navigation.Rotation;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

/**
 * Control a DC Motor attached to a DC Motor Controller
 *
 * @see com.qualcomm.robotcore.hardware.DcMotorController
 */
@SuppressWarnings("unused,WeakerAccess")
public class DcMotorImpl implements DcMotor {

  //------------------------------------------------------------------------------------------------
  // State
  //------------------------------------------------------------------------------------------------

  protected DcMotorController            controller = null;
  protected int                          portNumber = -1;
  protected Direction                    direction  = Direction.FORWARD;
  protected MotorConfigurationType       motorType  = null;

  //------------------------------------------------------------------------------------------------
  // Construction
  //------------------------------------------------------------------------------------------------

  /**
   * Constructor
   *
   * @param controller DC motor controller this motor is attached to
   * @param portNumber portNumber position on the controller
   */
  public DcMotorImpl(DcMotorController controller, int portNumber) {
    this(controller, portNumber, Direction.FORWARD);
  }

  /**
   * Constructor
   *
   * @param controller DC motor controller this motor is attached to
   * @param portNumber portNumber port number on the controller
   * @param direction direction this motor should spin
   */
  public DcMotorImpl(DcMotorController controller, int portNumber, Direction direction) {
    this(controller, portNumber, direction, MotorConfigurationType.getUnspecifiedMotorType());
  }

  /**
   * Constructor
   *
   * @param controller DC motor controller this motor is attached to
   * @param portNumber portNumber port number on the controller
   * @param direction direction this motor should spin
   * @param motorType the type we know this motor to be
   */
  public DcMotorImpl(DcMotorController controller, int portNumber, Direction direction, @NonNull MotorConfigurationType motorType) {
    this.controller = controller;
    this.portNumber = portNumber;
    this.direction = direction;
    this.motorType = motorType;
    RobotLog.v("DcMotorImpl(type=%s)", motorType.getXmlTag());

    // Clone the initial assigned motor type. This disconnects any subsequent modifications in the
    // fields of the type from the type used here at construction, which is usually / often the master
    // SDK instance for a given type, which ought not to be messed with.
    controller.setMotorType(portNumber, motorType.clone());
  }

  //------------------------------------------------------------------------------------------------
  // HardwareDevice interface
  //------------------------------------------------------------------------------------------------

  @Override public Manufacturer getManufacturer() {
    return controller.getManufacturer();
  }

  @Override
  public String getDeviceName() {
    return AppUtil.getDefContext().getString(R.string.configTypeMotor);
  }

  @Override
  public String getConnectionInfo() {
    return controller.getConnectionInfo() + "; port " + portNumber;
  }

  @Override
  public int getVersion() {
    return 1;
  }

  @Override
  public void resetDeviceConfigurationForOpMode() {
    this.setDirection(Direction.FORWARD);
    this.controller.resetDeviceConfigurationForOpMode(portNumber);
  }

  @Override
  public void close() {
    setPowerFloat();
  }

  //------------------------------------------------------------------------------------------------
  // DcMotor interface
  //------------------------------------------------------------------------------------------------

  @Override public MotorConfigurationType getMotorType() {
    return controller.getMotorType(portNumber);
  }

  @Override public void setMotorType(MotorConfigurationType motorType) {
    controller.setMotorType(portNumber, motorType);
  }

/**
   * Get DC motor controller
   *
   * @return controller
   */
  public DcMotorController getController() {
    return controller;
  }


  /**
   * Set the direction
   * @param direction direction
   */
  synchronized public void setDirection(Direction direction) {
    this.direction = direction;
  }

  /**
   * Get the direction
   * @return direction
   */
  public Direction getDirection() {
    return direction;
  }

  /**
   * Get port number
   *
   * @return portNumber
   */
  public int getPortNumber() {
    return portNumber;
  }

  /**
   * Set the current motor power
   *
   * @param power from -1.0 to 1.0
   */
  synchronized public void setPower(double power) {
    // Power must be positive when in RUN_TO_POSITION mode : in that mode, the
    // *direction* of rotation is controlled instead by the relative positioning
    // of the current and target positions.
    if (getMode() == RunMode.RUN_TO_POSITION) {
        power = Math.abs(power);
    } else {
        power = adjustPower(power);
    }
    internalSetPower(power);
  }

  protected void internalSetPower(double power) {
    controller.setMotorPower(portNumber, power);
  }

 /**
   * Get the current motor power
   *
   * @return scaled from -1.0 to 1.0
   */
  synchronized public double getPower() {
    double power = controller.getMotorPower(portNumber);
    if (getMode() == RunMode.RUN_TO_POSITION) {
        power = Math.abs(power);
    } else {
        power = adjustPower(power);
    }
    return power;
  }

  /**
   * Is the motor busy?
   *
   * @return true if the motor is busy
   */
  public boolean isBusy() {
    return controller.isBusy(portNumber);
  }

  @Override
  public synchronized void setZeroPowerBehavior(ZeroPowerBehavior zeroPowerBehavior) {
    controller.setMotorZeroPowerBehavior(portNumber, zeroPowerBehavior);
  }

  @Override
  public synchronized ZeroPowerBehavior getZeroPowerBehavior() {
    return controller.getMotorZeroPowerBehavior(portNumber);
  }

  /**
   * Allow motor to float
   */
  @Deprecated
  public synchronized void setPowerFloat() {
    setZeroPowerBehavior(ZeroPowerBehavior.FLOAT);
    setPower(0.0);
  }

  /**
   * Is motor power set to float?
   *
   * @return true of motor is set to float
   */
  public synchronized boolean getPowerFloat() {
    return getZeroPowerBehavior() == ZeroPowerBehavior.FLOAT && getPower() == 0.0;
  }

  /**
   * Set the motor target position, using an integer. If this motor has been set to REVERSE,
   * the passed-in "position" value will be multiplied by -1.
   *
   *  @param position range from Integer.MIN_VALUE to Integer.MAX_VALUE
   *
   */
  synchronized public void setTargetPosition(int position) {
    position = adjustPosition(position);
    internalSetTargetPosition(position);
  }

  protected void internalSetTargetPosition(int position) {
    controller.setMotorTargetPosition(portNumber, position);
  }

  /**
   * Get the current motor target position. If this motor has been set to REVERSE, the returned
   * "position" will be multiplied by -1.
   *
   * @return integer, unscaled
   */
  synchronized public int getTargetPosition() {
    int position = controller.getMotorTargetPosition(portNumber);
    return adjustPosition(position);
  }

  /**
   * Get the current encoder value, accommodating the configured directionality of the motor.
   *
   * @return double indicating current position
   */
  synchronized public int getCurrentPosition() {
    int position = controller.getMotorCurrentPosition(portNumber);
    return adjustPosition(position);
  }

  protected int adjustPosition(int position) {
    if (getOperationalDirection() == Direction.REVERSE) position = -position;
    return position;
  }

  protected double adjustPower(double power) {
    if (getOperationalDirection() == Direction.REVERSE) power = -power;
    return power;
  }

  protected Direction getOperationalDirection() {
    return motorType.getOrientation() == Rotation.CCW ? direction.inverted() : direction;
  }

  /**
   * Set the current mode
   *
   * @param mode run mode
   */
  synchronized public void setMode(RunMode mode) {
    mode = mode.migrate();
    internalSetMode(mode);
  }

  protected void internalSetMode(RunMode mode) {
    controller.setMotorMode(portNumber, mode);
  }

  /**
   * Get the current mode
   *
   * @return run mode
   */
  public RunMode getMode() {
    return controller.getMotorMode(portNumber);
  }
}
