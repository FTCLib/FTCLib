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

package com.qualcomm.hardware.modernrobotics;

import com.qualcomm.robotcore.hardware.AnalogInputController;
import com.qualcomm.robotcore.hardware.DigitalChannel;
import com.qualcomm.robotcore.hardware.DigitalChannelController;
import com.qualcomm.robotcore.hardware.TouchSensor;
import com.qualcomm.robotcore.hardware.configuration.annotations.AnalogSensorType;
import com.qualcomm.robotcore.hardware.configuration.annotations.DeviceProperties;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

/**
 * {@link ModernRoboticsTouchSensor} is the driver for a Modern Robotics Touch Sensor.
 * This can operate on either a DigitalChannelController or on an AnalogInputController.
 */
@SuppressWarnings("WeakerAccess")
// These annotations ONLY describe this touch sensor in ANALOG mode.
// Its digital mode is still defined in the BuiltInConfigurationType enum.
@AnalogSensorType
@DeviceProperties(name = "@string/configTypeMRTouchSensor", xmlTag = "ModernRoboticsAnalogTouchSensor", builtIn = true, description = "@string/mr_touch_sensor_description")
public class ModernRoboticsTouchSensor implements TouchSensor {

  private DigitalChannelController digitalController = null;
  private AnalogInputController analogInputController = null;
  private int physicalPort = -1;
  private double analogThreshold;

  public ModernRoboticsTouchSensor(DigitalChannelController digitalController, int physicalPort) {
    this.digitalController = digitalController;
    this.physicalPort = physicalPort;
    this.digitalController.setDigitalChannelMode(physicalPort, DigitalChannel.Mode.INPUT);
  }

  public ModernRoboticsTouchSensor(AnalogInputController analogController, int physicalPort) {
    this.analogInputController = analogController;
    this.physicalPort = physicalPort;
    this.analogThreshold = analogController.getMaxAnalogInputVoltage() / 2;
  }

  public boolean isDigital() {
    return digitalController != null;
  }

  public boolean isAnalog() {
    return !isDigital();
  }

  public double getAnalogVoltageThreshold() {
    return analogThreshold;
  }

  public void setAnalogVoltageThreshold(double threshold) {
    this.analogThreshold = threshold;
  }

  @Override
  public String toString() {
    return String.format("Touch Sensor: %1.2f", getValue());
  }

  @Override
  public double getValue() {
    return isPressed() ? 1.0 : 0.0;
  }

  @Override
  public boolean isPressed() {
    return isDigital()
            ? digitalController.getDigitalChannelState(physicalPort)
            : analogInputController.getAnalogInputVoltage(physicalPort) > getAnalogVoltageThreshold();
  }

  @Override public Manufacturer getManufacturer() {
    return Manufacturer.ModernRobotics;
  }

  @Override
  public String getDeviceName() {
    return AppUtil.getDefContext().getString(com.qualcomm.robotcore.R.string.configTypeMRTouchSensor);
  }

  @Override
  public String getConnectionInfo() {
    return isDigital()
            ? digitalController.getConnectionInfo()+ "; digital port " + physicalPort
            : analogInputController.getConnectionInfo() + "; analog port " + physicalPort;
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

  }
}
