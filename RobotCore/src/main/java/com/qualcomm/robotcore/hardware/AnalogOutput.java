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

import com.qualcomm.robotcore.R;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

/**
 * Control a single analog device
 */
public class AnalogOutput implements HardwareDevice {

  private AnalogOutputController controller = null;
  private int channel = -1;

  /**
   * Constructor
   *
   * @param controller AnalogOutput controller this channel is attached to
   * @param channel channel on the analog output controller
   */
  public AnalogOutput(AnalogOutputController controller, int channel) {
    this.controller = controller;
    this.channel = channel;
  }

  /**
   * Sets the channel output voltage.
   * If mode == 0: takes input from -1023-1023, output in the range -4 to +4 volts.
   * If mode == 1, 2, or 3: takes input from 0-1023, output in the range 0 to 8 volts.
   * @param voltage voltage value in the correct range.
   */
  public void setAnalogOutputVoltage(int voltage) {
    controller.setAnalogOutputVoltage(channel, voltage);
  }

  /**
   * Sets the channel output frequency in the range 1-5,000 Hz in mode 1, 2 or 3.
   * If mode 0 is selected, this field will be over-written to 0.
   * @param freq output frequency in the range1-5,000Hz
   */
  public void setAnalogOutputFrequency(int freq) {
    controller.setAnalogOutputFrequency(channel, freq);
  }

  /**
   * Sets the channel operating mode.
   * Mode 0: Voltage output. Range: -4V - 4V
   * Mode 1: Sine wave output. Range: 0 - 8V
   * Mode 2: Square wave output. Range: 0 - 8V
   * Mode 3: Triangle wave output. Range: 0 - 8V
   * @param mode voltage, sine, square, or triangle
   */
  public void setAnalogOutputMode(byte mode) {
    controller.setAnalogOutputMode(channel, mode);
  }

  @Override public Manufacturer getManufacturer() {
    return controller.getManufacturer();
  }


  @Override
  public String getDeviceName() {
    return AppUtil.getDefContext().getString(R.string.configTypeAnalogOutput);
  }

  @Override
  public String getConnectionInfo() {
    return controller.getConnectionInfo() + "; analog port " + channel;
  }

  @Override
  public int getVersion() {
    return 1;
  }

  @Override
  public void resetDeviceConfigurationForOpMode() {
  }

  @Override
  public void close() {
    // take no action
  }
}
