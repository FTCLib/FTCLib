/* Copyright (c) 2014, 2015 Qualcomm Technologies Inc, Noah Andrews

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
import java.io.Serializable;
import java.util.List;

/**
 * Represents a configured MR or HiTechnic standalone motor controller
 *
 * Hardware ports are indexed by 1.
 */
public class MotorControllerConfiguration extends ControllerConfiguration<DeviceConfiguration> implements Serializable{

  public MotorControllerConfiguration(){
    super("", ConfigurationUtility.buildEmptyMotors(ModernRoboticsConstants.INITIAL_MOTOR_PORT, ModernRoboticsConstants.NUMBER_OF_MOTORS), SerialNumber.createFake(), BuiltInConfigurationType.MOTOR_CONTROLLER);
  }

  public MotorControllerConfiguration(String name, List<DeviceConfiguration> motors, SerialNumber serialNumber) {
    super(name, motors, serialNumber, BuiltInConfigurationType.MOTOR_CONTROLLER);
  }

  public List<DeviceConfiguration> getMotors() {
    return super.getDevices();
  }

  public void setMotors(List<DeviceConfiguration> motors) {
    super.setDevices(motors);
  }

  @Override
  protected void deserializeChildElement(ConfigurationType configurationType, XmlPullParser parser, ReadXMLFileHandler xmlReader) throws IOException, XmlPullParserException, RobotCoreException {
    super.deserializeChildElement(configurationType, parser, xmlReader); // Doesn't currently do anything, but leave for future-proofing
    if (configurationType.isDeviceFlavor(ConfigurationType.DeviceFlavor.MOTOR)) {
      DeviceConfiguration motor = new DeviceConfiguration();
      motor.deserialize(parser, xmlReader);

      // ModernRobotics HW is indexed by 1, but internally this code indexes by 0.
      getMotors().set(motor.getPort() - ModernRoboticsConstants.INITIAL_MOTOR_PORT, motor);
    }
  }
}
