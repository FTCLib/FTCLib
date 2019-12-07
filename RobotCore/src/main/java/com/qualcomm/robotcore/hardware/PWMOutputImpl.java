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
 * Control a single digital port
 */
public class PWMOutputImpl implements PWMOutput
  {
  protected PWMOutputController controller = null;
  protected int port = -1;

  /**
   * Constructor
   *
   * @param controller Digital port controller this port is attached to
   * @param port port on the digital port controller
   */
  public PWMOutputImpl(PWMOutputController controller, int port) {
    this.controller = controller;
    this.port = port;
  }

  /**
   * Set the pulse width output time for this port. Typically set to a value between
   * 750 and 2,250 to control a servo.
   * @param time pulse width for the port in microseconds.
   */
  public void setPulseWidthOutputTime(int time) {
    controller.setPulseWidthOutputTime(port, time);
  }

  /**
   * Get the pulse width output time for this port
   */
  public int getPulseWidthOutputTime() {
    return controller.getPulseWidthOutputTime(port);
  }

  /**
   * Set the pulse width output period. Typically set to 20,000 to control servo.
   * @param period pulse repetition period in microseconds.
   */
  public void setPulseWidthPeriod(int period) {
    controller.setPulseWidthPeriod(port, period);
  }

  /**
   * Get the pulse width output
   */
  public int getPulseWidthPeriod() {
    return controller.getPulseWidthPeriod(port);
  }

  @Override public Manufacturer getManufacturer() {
    return this.controller.getManufacturer();
  }

  @Override
  public String getDeviceName() {
    return AppUtil.getDefContext().getString(R.string.configTypePulseWidthDevice);
  }

  @Override
  public String getConnectionInfo() {
    return controller.getConnectionInfo() + "; port " + port;
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
