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
 * Gyro Sensor
 */
public interface GyroSensor extends HardwareDevice {

  /**
   * Calibrate the gyro.  For the Modern Robotics device this will null, or reset,
   * the Z axis heading.
   * @throws UnsupportedOperationException if unsupported; not all gyro devices support this feature
   */
  void calibrate();

  /**
   * Is the gyro performing a calibration operation?
   * @return true if yes, false otherwise
   * @throws UnsupportedOperationException if unsupported; not all gyro devices support this feature
   */
  boolean isCalibrating();

  /**
   * Return the integrated Z axis as a cartesian heading.
   * @return heading between 0-360.
   * @throws UnsupportedOperationException if unsupported; not all gyro devices support this feature
   */
  int getHeading();

  /**
   * Return the rotation of this sensor expressed as a fraction of the maximum possible reportable rotation
   * @return the current fractional rotation of this gyro (a value between 0.0 and 1.0)
   * @throws UnsupportedOperationException if unsupported; not all gyro devices support this feature
   */
  double getRotationFraction();

  /**
   * Return the gyro's raw X value.
   * @return X value
   * @throws UnsupportedOperationException if unsupported; not all gyro devices support this feature
   */
  int rawX();

  /**
   * Return the gyro's raw Y value.
   * @return Y value
   * @throws UnsupportedOperationException if unsupported; not all gyro devices support this feature
   */
  int rawY();

  /**
   * Return the gyro's raw Z value.
   * @return Z value
   * @throws UnsupportedOperationException if unsupported; not all gyro devices support this feature
   */
  int rawZ();

  /**
   * Set the integrated Z axis to zero.
   * @throws UnsupportedOperationException if unsupported; not all gyro devices support this feature
   */
  void resetZAxisIntegrator();

  /**
   * Status of this sensor, in string form
   * @return status
   */
  String status();
}
