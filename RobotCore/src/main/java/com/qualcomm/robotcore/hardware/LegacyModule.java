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

/**
 * Legacy Module for working with NXT devices.
 *
 * The Modern Robotics Core Legacy Module provides backward compatibility to enable LEGO NXT devices
 * to connect to an Android device or a PC via a USB connection. Each Legacy Module has six (6) ports and
 * virtually any combination of devices can be connected to the module.
 *
 * The Legacy Module firmware supports all NXT LEGO sensors (except the Lego Color Sensor),
 * HiTechnic sensors, Matrix controllers as well as the HiTechnic Tetrix motor and servo controllers.
 *
 * Each Legacy Module port can operate in digital as well as analog modes. In digital mode, legacy
 * connector pins 5 and 6 can be set to logic 0 or logic 1.
 *
 * In analog mode, the voltage on legacy connector pin 1 is measured using a 10 bit analog to digital
 * converter. Additionally, pins 5 and 6 can be set to logic 0 or logic 1 for control of the attached
 * device, such as a LEGO light sensor which uses pin 5 to turn the sensor LED on and off.
 *
 * In I2C mode, legacy connector pins 5 and 6 are used to communicate with I2C devices in accordance
 * with the LEGO interpretation of I2C. Ports 4 and 5 can additionally be switched into pin 1 9v
 * supply mode to permit LEGO ultrasonic range sensors to be used
 *
 * @see <a href="http://www.modernroboticsinc.com/core-legacy-module-3">Modern Robotics Legacy Module</a>
 */
public interface LegacyModule extends HardwareDevice, I2cController {

  /**
   * Enable a physical port in analog read mode
   * @param physicalPort physical port number on the device
   */
  void enableAnalogReadMode(int physicalPort);

  /**
   * Read an analog value from a device; only works in analog read mode
   * @param physicalPort physical port number on the device
   * @return byte[] containing the two analog values; low byte first, high byte second
   */
  byte[] readAnalogRaw(int physicalPort);

  /**
   * Reads the analog voltage from a device. The port indicated must currently
   * be in analog read mode.
   * @param physicalPort the port whose voltage is to be read
   * @return the voltage level read, in volts
   */
  double readAnalogVoltage(int physicalPort);

  /**
   * Returns the maximum voltage that can be read by our analog inputs
   * @return the maximum voltage that can be read by our analog inputs
   */
  double getMaxAnalogInputVoltage();

  /**
   * Enable or disable 9V power on a port
   * @param physicalPort physical port number on the device
   * @param enable true to enable; false to disable
   */
  void enable9v(int physicalPort, boolean enable);

  /**
   * Set the value of digital line 0 or 1 while in analog mode.
   * <p>
   * These are port pins 5 and 6.
   * @param physicalPort physical port number on the device
   * @param line line 0 or 1
   * @param set true to set; otherwise false
   */
  void setDigitalLine(int physicalPort, int line, boolean set);
}
