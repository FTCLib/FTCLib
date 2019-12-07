package com.qualcomm.robotcore.hardware.configuration;

import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.util.SerialNumber;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.List;

public class MatrixControllerConfiguration extends ControllerConfiguration<DeviceConfiguration> {

  private List<DeviceConfiguration> servos;
  private List<DeviceConfiguration> motors;

  public MatrixControllerConfiguration() {
    this("",
            ConfigurationUtility.buildEmptyMotors(MatrixConstants.INITIAL_MOTOR_PORT, MatrixConstants.NUMBER_OF_MOTORS),
            ConfigurationUtility.buildEmptyServos(MatrixConstants.INITIAL_SERVO_PORT, MatrixConstants.NUMBER_OF_SERVOS),
            SerialNumber.createFake());
  }

  public MatrixControllerConfiguration(String name, List<DeviceConfiguration> motors, List<DeviceConfiguration> servos, SerialNumber serialNumber) {
    super(name, serialNumber, BuiltInConfigurationType.MATRIX_CONTROLLER);
    this.servos = servos;
    this.motors = motors;
  }

  public List<DeviceConfiguration> getServos() {
    return servos;
  }

  public void setServos(List<DeviceConfiguration> servos) {
    this.servos = servos;
  }

  public List<DeviceConfiguration> getMotors() {
    return motors;
  }

  public void setMotors(List<DeviceConfiguration> motors){
    this.motors = motors;
  }

  @Override
  protected void deserializeChildElement(ConfigurationType configurationType, XmlPullParser parser, ReadXMLFileHandler xmlReader) throws IOException, XmlPullParserException, RobotCoreException {
    super.deserializeChildElement(configurationType, parser, xmlReader); // Doesn't currently do anything, but leave for future-proofing
    if (configurationType.isDeviceFlavor(ConfigurationType.DeviceFlavor.SERVO)) {
      DeviceConfiguration servo = new DeviceConfiguration();
      servo.deserialize(parser, xmlReader);

      // Matrix HW is indexed by 1, but internally this code indexes by 0.
      getServos().set(servo.getPort() - MatrixConstants.INITIAL_SERVO_PORT, servo);

    }

    else if (configurationType.isDeviceFlavor(ConfigurationType.DeviceFlavor.MOTOR)) {
      DeviceConfiguration motor = new DeviceConfiguration();
      motor.deserialize(parser, xmlReader);

      // Matrix HW is indexed by 1, but internally this code indexes by 0.
      getMotors().set(motor.getPort() - MatrixConstants.INITIAL_MOTOR_PORT, motor);
    }
  }
}
