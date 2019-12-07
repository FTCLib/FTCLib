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
 * DeviceInterfaceModule for working with various devices
 */
@SuppressWarnings("unused")
public interface DeviceInterfaceModule extends DigitalChannelController, AnalogInputController, PWMOutputController, I2cController, AnalogOutputController {

  /**
   * A byte containing the current logic levels present in the D7-D0 channel pins.
   * If a particular pin is in output mode, the current output state will be reported.
   */
  int getDigitalInputStateByte();

  /**
   * If a particular bit is set to one, the corresponding channel pin will be in output mode.
   * Else it will be in input mode.
   * @param input - the desired setting for each channel pin.
   */
  void setDigitalIOControlByte(byte input);

  /**
   * Get the digital IO control byte
   * @return control byte
   */
  byte getDigitalIOControlByte();

  /**
   * If a a particular control field bit is set to one, the channel pin will be in output mode and
   * will reflect the value of the corresponding field bit.
   * @param input with output state of the digital pins.
   */
  void setDigitalOutputByte(byte input);

  /**
   * The D7-D0 output set field is a byte containing the required I/O output of the D7-D0
   * channel pins. If the corresponding Dy-D0 I/O control field bit is set to one, the channel pin
   * will be in output mode and will reflect the value of the corresponding D7-D0 output set field bit.
   * @return D7-D0 output set field.
   */
  byte getDigitalOutputStateByte();

  /**
   * Indicates whether the LED on the given channel is on or not
   * @return true for ON, false for OFF
   */
  boolean getLEDState(int channel);

  /**
   * Turn on or off a particular LED
   * @param channel - int indicating the ID of the LED.
   * @param state - byte containing the desired setting.
   */
  void setLED(int channel, boolean state);
}
