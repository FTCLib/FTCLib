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

import com.qualcomm.robotcore.util.SerialNumber;


/**
 * Interface for working with Digital Channel Controllers
 * <p>
 * Different digital channel controllers will implement this interface.
 */
public interface DigitalChannelController extends HardwareDevice {

  /**
   * Digital channel mode - input or output
   * @deprecated use {@link DigitalChannel.Mode} instead
   */
  @Deprecated
  enum Mode { INPUT, OUTPUT;
    public DigitalChannel.Mode migrate() {
      switch (this) {
        case INPUT: return DigitalChannel.Mode.INPUT;
        default:    return DigitalChannel.Mode.OUTPUT;
      }
    }
  }

 /**
   * Serial Number
   *
   * @return return the USB serial number of this device
   */
  SerialNumber getSerialNumber();

  /**
   * Get the mode of a digital channel
   *
   * @param channel channel
   * @return INPUT or OUTPUT
   */
  DigitalChannel.Mode getDigitalChannelMode(int channel);

  /**
   * Set the mode of a digital channel
   *
   * @param channel channel
   * @param mode INPUT or OUTPUT
   */
  void setDigitalChannelMode(int channel, DigitalChannel.Mode mode);

  /** @deprecated use {@link #setDigitalChannelMode(int, DigitalChannel.Mode)} instead */
  @Deprecated void setDigitalChannelMode(int channel, DigitalChannelController.Mode mode);

  /**
   * Get the state of a digital channel
   * If it's in OUTPUT mode, this will return the output bit.
   * If the channel is in INPUT mode, this will return the input bit.
   *
   * @param channel channel
   * @return true if set; otherwise false
   */
  boolean getDigitalChannelState(int channel);

  /**
   * Set the state of a digital channel
   * <p>
   * The behavior of this method is undefined for digital channels in INPUT mode.
   *
   * @param channel channel
   * @param state true to set; false to unset
   */
  void setDigitalChannelState(int channel, boolean state);
}
