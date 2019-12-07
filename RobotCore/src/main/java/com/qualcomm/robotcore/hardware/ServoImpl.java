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

import com.qualcomm.robotcore.R;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

/**
 * ServoImpl provides an implementation of the Servo interface that operates on top
 * of an instance of the ServoController interface.
 */
public class ServoImpl implements Servo {

  //------------------------------------------------------------------------------------------------
  // State
  //------------------------------------------------------------------------------------------------

  protected ServoController controller  = null;
  protected int             portNumber  = -1;

  protected Direction       direction        = Direction.FORWARD;
  protected double          limitPositionMin = MIN_POSITION;
  protected double          limitPositionMax = MAX_POSITION;

  //------------------------------------------------------------------------------------------------
  // Construction
  //------------------------------------------------------------------------------------------------

  /**
   * Constructor
   * @param controller Servo controller that this servo is attached to
   * @param portNumber physical port number on the servo controller
   */
  public ServoImpl(ServoController controller, int portNumber) {
    this(controller, portNumber, Direction.FORWARD);
  }

  /**
   * COnstructor
   * @param controller Servo controller that this servo is attached to
   * @param portNumber physical port number on the servo controller
   * @param direction FORWARD for normal operation, REVERSE to reverse operation
   */
  public ServoImpl(ServoController controller, int portNumber, Direction direction) {
    this.direction = direction;
    this.controller = controller;
    this.portNumber = portNumber;
  }

  //------------------------------------------------------------------------------------------------
  // HardwareDevice interface
  //------------------------------------------------------------------------------------------------

  @Override public Manufacturer getManufacturer() {
    return controller.getManufacturer();
  }

  @Override
  public String getDeviceName() {
    return AppUtil.getDefContext().getString(R.string.configTypeServo);
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
  public synchronized void resetDeviceConfigurationForOpMode() {
    this.limitPositionMin = MIN_POSITION;
    this.limitPositionMax = MAX_POSITION;
    this.direction = Direction.FORWARD;
  }

  @Override
  public void close() {
    // take no action
  }

  //------------------------------------------------------------------------------------------------
  // Servo operations
  //------------------------------------------------------------------------------------------------

  /**
   * Get Servo Controller
   * @return servo controller
   */
  public ServoController getController() {
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
   * Get Channel
   * @return channel
   */
  public int getPortNumber() {
    return portNumber;
  }

  /**
   * Commands the servo to move to a designated position. This method initiates the movement;
   * the servo will arrive at the commanded position at some later time.
   *
   * @param position the commanded servo position. Should be in the range [0.0, 1.0].
   * @see #getPosition()
   */
  synchronized public void setPosition(double position) {
    position = Range.clip(position, MIN_POSITION, MAX_POSITION);
    if (direction == Direction.REVERSE) position = reverse(position);
    double scaled = Range.scale(position, MIN_POSITION, MAX_POSITION, limitPositionMin, limitPositionMax);
    internalSetPosition(scaled);
  }

  protected void internalSetPosition(double position) {
    controller.setServoPosition(portNumber, position);
  }

  /**
   * Returns the position to which the servo was last commanded, or Double.NaN if that is
   * unavailable.
   * @return the last commanded position
   * @see #setPosition(double)
   * @see Double#isNaN(double)
   */
  synchronized public double getPosition() {
    double position = controller.getServoPosition(portNumber);
    if (direction == Direction.REVERSE) position = reverse(position);
    double scaled = Range.scale(position, limitPositionMin, limitPositionMax, MIN_POSITION, MAX_POSITION);
    return Range.clip(scaled, MIN_POSITION, MAX_POSITION);
  }

  /**
   * Automatically scales the position of the servo.
   */
  synchronized public void scaleRange(double min, double max) {
    min = Range.clip(min, MIN_POSITION, MAX_POSITION);
    max = Range.clip(max, MIN_POSITION, MAX_POSITION);

    if (min >= max) {
      throw new IllegalArgumentException("min must be less than max");
    }

    limitPositionMin = min;
    limitPositionMax = max;
  }

  private double reverse(double position) {
    return MAX_POSITION - position + MIN_POSITION;
  }
}
