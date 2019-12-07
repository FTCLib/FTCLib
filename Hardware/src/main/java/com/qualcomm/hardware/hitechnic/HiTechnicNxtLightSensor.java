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
import com.qualcomm.robotcore.hardware.AnalogSensor;
import com.qualcomm.robotcore.hardware.LightSensor;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

public class HiTechnicNxtLightSensor extends LegacyModulePortDeviceImpl implements LightSensor, AnalogSensor
  {

  //------------------------------------------------------------------------------------------------
  // Constants
  //------------------------------------------------------------------------------------------------

  public static final byte LED_DIGITAL_LINE_NUMBER = 0;
  public static final double MIN_LIGHT_FRACTION = 120.0 / 1023.0;
  public static final double MAX_LIGHT_FRACTION = 870.0 / 1023.0;

  protected static final double apiLevelMin = 0.0;
  protected static final double apiLevelMax = 1.0;

  //------------------------------------------------------------------------------------------------
  // Construction
  //------------------------------------------------------------------------------------------------

  public HiTechnicNxtLightSensor(LegacyModule legacyModule, int physicalPort) {
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

  @Override
  public String toString() {
    return String.format("Light Level: %1.2f", getLightDetected());
  }

  /**
   * Read the scaled light level from an NXT Light sensor and return a scaled value<BR>
   * NOTE: Returned values INCREASE as the light energy INCREASES<BR>
   * Typical Scaled Light Levels:<BR>
   * LED ON (reflective): Black = 0.1  White  = 0.55 <BR>
   * LED OFF (ambient):   Dark  = 0.0  Bright = 1.0 <BR>
   * @return a double scaled to a value between 0.0 and 1.0, inclusive
   */
  @Override
  public double getLightDetected() {
    double max = getRawLightDetectedMax();
    return Range.clip(
            Range.scale(getRawLightDetected(),
                    MIN_LIGHT_FRACTION * max, MAX_LIGHT_FRACTION * max,
                    apiLevelMin, apiLevelMax),
            apiLevelMin, apiLevelMax);
  }

  /**
   * Read the raw light level from an NXT Light sensor<BR>
   * NOTE: Returned values INCREASE as the light energy INCREASES<BR>
   * @return the light level detected, in volts (here; interface doesn't specify the units)
   * Typical Scaled Light Levels:<BR>
   * LED ON (reflective): Black = 0.9  White  = 2.7 <BR>
   * LED OFF (ambient):   Dark  = 0.1  Bright = 5.0 <BR>
   */
  @Override
  public double getRawLightDetected() {
    // Note the raw voltage coming back from the sensor has the wrong sense of correlation
    // with intensity, so we invert the signal here
    double max = getRawLightDetectedMax();
    return Range.clip(max - readRawVoltage(), 0, max);  // paranoia
  }

  @Override
  public double getRawLightDetectedMax() {
    // The sensor is a five volt sensor, but we might have a level shifter between us and the
    // sensor (probably not, but this mirrors the other sensor's paths).
    final double sensorMaxVoltage = 5.0;
    return Math.min(sensorMaxVoltage, module.getMaxAnalogInputVoltage());
  }

  @Override
  public double readRawVoltage() {
    // Note the raw voltage coming back from the sensor has the wrong sense of correllation with intensity
    return module.readAnalogVoltage(physicalPort);
  }

  @Override
  public void enableLed(boolean enable) {
    module.setDigitalLine(physicalPort, LED_DIGITAL_LINE_NUMBER, enable);
  }

  @Override
  public String status() {
    return String.format("NXT Light Sensor, connected via device %s, port %d",
        module.getSerialNumber(), physicalPort);
  }

  @Override public Manufacturer getManufacturer() {
    return Manufacturer.HiTechnic;
  }

  @Override
  public String getDeviceName() {
    return AppUtil.getDefContext().getString(com.qualcomm.robotcore.R.string.configTypeHTLightSensor);
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
    // take no action
  }
}
