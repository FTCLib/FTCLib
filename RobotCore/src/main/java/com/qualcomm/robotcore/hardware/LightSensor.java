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
 * Light Sensor
 */
public interface LightSensor extends HardwareDevice {

  /**
   * Get the amount of light detected by the sensor, scaled and cliped to a range
   * which is a pragmatically useful sensitivity. Note that returned values INCREASE as
   * the light energy INCREASES.
   * @return amount of light, on a scale of 0.0 to 1.0
   */
  double getLightDetected();

  /**
   * Returns a signal whose strength is proportional to the intensity of the light measured.
   * Note that returned values INCREASE as the light energy INCREASES. The units in which
   * this signal is returned are unspecified.
   * @return a value proportional to the amount of light detected, in unspecified units
   */
  double getRawLightDetected();

  /**
   * Returns the maximum value that can be returned by {@link #getRawLightDetected}.
   * @return the maximum value that can be returned by getRawLightDetected
   * @see #getRawLightDetected
   */
  double getRawLightDetectedMax();

  /**
   * Enable the LED light
   * @param enable true to enable; false to disable
   */
  void enableLed(boolean enable);

  /**
   * Status of this sensor, in string form
   * @return status
   */
  String status();

}
