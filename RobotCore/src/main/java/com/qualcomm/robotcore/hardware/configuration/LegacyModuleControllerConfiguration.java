/* Copyright (c) 2014, 2015 Qualcomm Technologies Inc

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of Qualcomm Technologies Inc nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. */

package com.qualcomm.robotcore.hardware.configuration;

import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.util.SerialNumber;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static com.qualcomm.robotcore.hardware.configuration.BuiltInConfigurationType.ACCELEROMETER;
import static com.qualcomm.robotcore.hardware.configuration.BuiltInConfigurationType.COLOR_SENSOR;
import static com.qualcomm.robotcore.hardware.configuration.BuiltInConfigurationType.COMPASS;
import static com.qualcomm.robotcore.hardware.configuration.BuiltInConfigurationType.GYRO;
import static com.qualcomm.robotcore.hardware.configuration.BuiltInConfigurationType.IR_SEEKER;
import static com.qualcomm.robotcore.hardware.configuration.BuiltInConfigurationType.LIGHT_SENSOR;
import static com.qualcomm.robotcore.hardware.configuration.BuiltInConfigurationType.MATRIX_CONTROLLER;
import static com.qualcomm.robotcore.hardware.configuration.BuiltInConfigurationType.MOTOR_CONTROLLER;
import static com.qualcomm.robotcore.hardware.configuration.BuiltInConfigurationType.SERVO_CONTROLLER;
import static com.qualcomm.robotcore.hardware.configuration.BuiltInConfigurationType.TOUCH_SENSOR;
import static com.qualcomm.robotcore.hardware.configuration.BuiltInConfigurationType.TOUCH_SENSOR_MULTIPLEXER;
import static com.qualcomm.robotcore.hardware.configuration.BuiltInConfigurationType.ULTRASONIC_SENSOR;

public class LegacyModuleControllerConfiguration extends ControllerConfiguration<DeviceConfiguration> {
  private static final ConfigurationType[] simpleLegacyTypes =
          { COMPASS, LIGHT_SENSOR, IR_SEEKER, ACCELEROMETER, GYRO, TOUCH_SENSOR,
                  TOUCH_SENSOR_MULTIPLEXER, ULTRASONIC_SENSOR, COLOR_SENSOR };

  public LegacyModuleControllerConfiguration() {
    super("", ConfigurationUtility.buildEmptyDevices(0, ModernRoboticsConstants.NUMBER_OF_LEGACY_MODULE_PORTS, BuiltInConfigurationType.NOTHING), SerialNumber.createFake(), BuiltInConfigurationType.LEGACY_MODULE_CONTROLLER);
  }

  public LegacyModuleControllerConfiguration(String name, List<DeviceConfiguration> modules, SerialNumber serialNumber) {
    super(name, modules, serialNumber, BuiltInConfigurationType.LEGACY_MODULE_CONTROLLER);
  }

  @Override
  protected void deserializeChildElement(ConfigurationType configurationType, XmlPullParser parser, ReadXMLFileHandler xmlReader) throws IOException, XmlPullParserException, RobotCoreException {
    super.deserializeChildElement(configurationType, parser, xmlReader); // Doesn't currently do anything, but leave for future-proofing
    DeviceConfiguration legacyDevice = null;

    if (Arrays.asList(simpleLegacyTypes).contains(configurationType)) {
      legacyDevice = new DeviceConfiguration();
    }

    else if (configurationType == MOTOR_CONTROLLER) {
      legacyDevice = new MotorControllerConfiguration();
    }

    else if (configurationType == SERVO_CONTROLLER) {
      legacyDevice = new ServoControllerConfiguration();
    }

    else if (configurationType == MATRIX_CONTROLLER) {
      legacyDevice = new MatrixControllerConfiguration();
    }

    if (legacyDevice != null) {
      legacyDevice.deserialize(parser, xmlReader);
      getDevices().set(legacyDevice.getPort(), legacyDevice);
    }
  }
}
