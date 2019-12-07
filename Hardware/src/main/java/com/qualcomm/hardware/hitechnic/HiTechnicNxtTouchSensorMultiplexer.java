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

package com.qualcomm.hardware.hitechnic;

import com.qualcomm.robotcore.hardware.LegacyModule;
import com.qualcomm.robotcore.hardware.LegacyModulePortDeviceImpl;
import com.qualcomm.robotcore.hardware.TouchSensorMultiplexer;
import com.qualcomm.robotcore.util.TypeConversion;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.nio.ByteOrder;

/**
 * NXT Touch Sensor Multiplexer
 */
public class HiTechnicNxtTouchSensorMultiplexer extends LegacyModulePortDeviceImpl implements TouchSensorMultiplexer {

  //------------------------------------------------------------------------------------------------
  // Constants
  //------------------------------------------------------------------------------------------------

  int NUM_TOUCH_SENSORS = 4;

  public static final int MASK_TOUCH_SENSOR_1 = 0x1;
  public static final int MASK_TOUCH_SENSOR_2 = 0x2;
  public static final int MASK_TOUCH_SENSOR_3 = 0x4;
  public static final int MASK_TOUCH_SENSOR_4 = 0x8;

  public static final int INVALID = -1;

  public static final int[] MASK_MAP = {
      INVALID,
      MASK_TOUCH_SENSOR_1,
      MASK_TOUCH_SENSOR_2,
      MASK_TOUCH_SENSOR_3,
      MASK_TOUCH_SENSOR_4
  };

  //------------------------------------------------------------------------------------------------
  // Construction
  //------------------------------------------------------------------------------------------------

  public HiTechnicNxtTouchSensorMultiplexer(LegacyModule legacyModule, int physicalPort) {
    super(legacyModule, physicalPort);
    finishConstruction();
  }

  @Override
  protected void moduleNowArmedOrPretending() {
    module.enableAnalogReadMode(physicalPort);
  }

  //------------------------------------------------------------------------------------------------
  // Operations
  //------------------------------------------------------------------------------------------------

  public String status() {
    return String.format("NXT Touch Sensor Multiplexer, connected via device %s, port %d",
        module.getSerialNumber(), physicalPort);  }

  @Override public Manufacturer getManufacturer() {
    return Manufacturer.HiTechnic;
  }

  @Override
  public String getDeviceName() {
    return AppUtil.getDefContext().getString(com.qualcomm.robotcore.R.string.configTypeHTTouchSensorMultiplexer);
  }

  @Override
  public String getConnectionInfo() {
    return module.getConnectionInfo() + "; port " + physicalPort;
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
    // take no action.
  }

  @Override
  public boolean isTouchSensorPressed(int channel) {
    throwIfChannelInvalid(channel);
    int switches = getAllSwitches();

    int touchSensor = switches & MASK_MAP[channel];
    return (touchSensor > 0);
  }

  @Override
  public int getSwitches() {
    return getAllSwitches();
  }

  private int getAllSwitches() {
    byte[] analogBuffer = module.readAnalogRaw(3);
    int analogValue = TypeConversion.byteArrayToShort(analogBuffer, ByteOrder.LITTLE_ENDIAN);

    int svalue=1023-analogValue;
    int switches=339*svalue;
    switches/=1023-svalue;
    switches+=5;
    switches/=10;

    return switches;
  }

  private void throwIfChannelInvalid(int channel) {
    if (channel <= 0 || channel > NUM_TOUCH_SENSORS) {
      throw new IllegalArgumentException(String.format( "Channel %d is invalid; " +
          "valid channels are 1..%d", channel, NUM_TOUCH_SENSORS));
    }
  }
}
