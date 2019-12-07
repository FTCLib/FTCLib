/*
 * Copyright (c) 2014 Qualcomm Technologies Inc
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

import com.qualcomm.robotcore.util.SerialNumber;

/**
 * Interface for working with Analog Controllers
 * <p>
 * Different analog input controllers will implement this interface.
 */
public interface AnalogOutputController extends HardwareDevice {

  /**
   * Serial Number
   *
   * @return return the USB serial number of this device
   */
  SerialNumber getSerialNumber();

  /**
   * Sets the channel output voltage.
   * If mode == 0: takes input from -1023-1023, output in the range -4 to +4 volts.
   * If mode == 1, 2, or 3: takes input from 0-1023, output in the range 0 to 8 volts.
   * @param port channel 0 or 1
   * @param voltage voltage value in the correct range.
   */
  void setAnalogOutputVoltage(int port, int voltage);

  /**
   * Sets the channel output frequency in the range 1-5,000 Hz in mode 1, 2 or 3.
   * If mode 0 is selected, this field will be over-written to 0.
   * @param port 1 or 0
   * @param freq output frequency in the range1-5,000Hz
   */
  void setAnalogOutputFrequency(int port, int freq);

  /**
   * Sets the channel operating mode.
   * Mode 0: Voltage output. Range: -4V - 4V
   * Mode 1: Sine wave output. Range: 0 - 8V
   * Mode 2: Square wave output. Range: 0 - 8V
   * Mode 3: Triangle wave output. Range: 0 - 8V
   * @param port 0 or 1
   * @param mode voltage, sine, square, or triangle
   */
  void setAnalogOutputMode(int port, byte mode);
}
