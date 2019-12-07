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

import com.qualcomm.robotcore.R;
import com.qualcomm.robotcore.hardware.configuration.annotations.DeviceProperties;
import com.qualcomm.robotcore.hardware.configuration.annotations.DigitalIoDeviceType;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

/**
 * Control a single digital channel
 */
@DigitalIoDeviceType
@DeviceProperties(name = "@string/configTypeDigitalDevice", xmlTag = "DigitalDevice", builtIn = true)
public class DigitalChannelImpl implements DigitalChannel {

  private DigitalChannelController controller = null;
  private int channel = -1;

  /**
   * Constructor
   *
   * @param controller Digital channel controller this channel is attached to
   * @param channel channel on the digital channel controller
   */
  public DigitalChannelImpl(DigitalChannelController controller, int channel) {
    this.controller = controller;
    this.channel = channel;
  }

  /**
   * Get the channel mode
   *
   * @return channel mode
   */
  @Override
  public DigitalChannel.Mode getMode() {
    return controller.getDigitalChannelMode(channel);
  }

  /**
   * Set the channel mode
   *
   * @param mode
   */
  @Override
  public void setMode(DigitalChannel.Mode mode) {
    controller.setDigitalChannelMode(channel, mode);
  }

  @Override @Deprecated public void setMode(DigitalChannelController.Mode mode) {
    controller.setDigitalChannelMode(channel, mode);
  }

/**
   * Get the channel state
   *
   * @return state
   */
  @Override
  public boolean getState() {
    return controller.getDigitalChannelState(channel);
  }

  /**
   * Set the channel state
   * <p>
   * The behavior of this method is undefined for INPUT digital channels.
   *
   * @param state
   */
  @Override
  public void setState(boolean state) {
    controller.setDigitalChannelState(channel, state);
  }

  @Override public Manufacturer getManufacturer() {
    return controller.getManufacturer();
  }

  @Override
  public String getDeviceName() {
    return AppUtil.getDefContext().getString(R.string.configTypeDigitalDevice);
  }

  @Override
  public String getConnectionInfo() {
    return controller.getConnectionInfo() + "; digital port " + channel;
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
