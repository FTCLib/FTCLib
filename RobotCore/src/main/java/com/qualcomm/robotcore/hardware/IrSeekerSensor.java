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


/**
 * IR Seeker Sensor
 * <p>
 * Determine the location of an IR source
 */
public interface IrSeekerSensor extends HardwareDevice {

  /**
   * Enumeration of device modes
   */
  enum Mode {
    MODE_600HZ, MODE_1200HZ
  }

  /**
   * IR Sensor attached to an IR Seeker
   * <p>
   * Get the angle of this sensor, along with signal strength
   */
  class IrSeekerIndividualSensor {
    private double angle = 0;
    private double strength = 0;

    /**
     * Constructor
     */
    public IrSeekerIndividualSensor() {
      this(0, 0);
    }

    /**
     * Constructor
     */
    public IrSeekerIndividualSensor(double angle, double strength) {
      this.angle = angle;
      this.strength = strength;
    }

    /**
     * Get the angle at which this sensor is mounted
     * @return sensor angle
     */
    public double getSensorAngle() {
      return angle;
    }

    /**
     * Get the strength of the IR signal detected by this sensor
     * @return IR strength, scaled from 0 to 1
     */
    public double getSensorStrength() {
      return strength;
    }

    @Override
    public String toString() {
      return String.format("IR Sensor: %3.1f degrees at %3.1f%% power", angle, strength * 100.0);
    }
  }

  /**
   * Set the minimum threshold for a signal to be considered detected
   * @param threshold minimum threshold
   */
  void setSignalDetectedThreshold(double threshold);

  /**
   * Get the minimum threshold for a signal to be considered detected
   * @return threshold
   */
  double getSignalDetectedThreshold();

  /**
   * Set the device mode
   * @param mode sample rate
   */
  void setMode(Mode mode);

  /**
   * Get the device mode
   * @return device mode
   */
  Mode getMode();

  /**
   * Returns true if an IR signal is detected
   * @return true if signal is detected; otherwise false
   */
  boolean signalDetected();

  /**
   * Estimated angle in which the signal is coming from
   * <p>
   * If the signal is estimated to be directly ahead, 0 will be returned. If the signal is to the
   * left a negative angle will be returned. If the signal is to the right a positive angle will be
   * returned. If no signal is detected, a 0 will be returned.
   * <p>
   * NOTE: not all sensors give an accurate angle.
   * @return angle to IR signal
   */
  double getAngle();

  /**
   * IR Signal strength
   * <p>
   * Detected IR signal strength, on a scale of 0.0 to 1.0, where 0 is no signal detected and 1 is
   * max IR signal detected.
   * @return signal strength, scaled from 0 to 1
   */
  double getStrength();

  /**
   * Get a list of all IR sensors attached to this seeker. The list will include the angle at which
   * the sensor is mounted, and the signal strength.
   * @return array of IrSensors
   */
  IrSeekerIndividualSensor[] getIndividualSensors();

  /**
   * Set the I2C address to a new value.
   *
   */
  void setI2cAddress(I2cAddr newAddress);

  /**
   * Get the current I2C Address of this object.
   * Not necessarily the same as the I2C address of the actual device.
   *
   * Return the current I2C address.
   * @return current I2C address
   */
  I2cAddr getI2cAddress();


}
