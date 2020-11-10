package com.arcrobotics.ftclib.hardware;

import com.arcrobotics.ftclib.geometry.Rotation2d;
import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.hardware.bosch.JustLoggingAccelerationIntegrator;
import com.qualcomm.robotcore.hardware.HardwareMap;

public class RevIMU extends GyroEx {

  private BNO055IMU revIMU;
  /***
   * Heading relative to starting position
   */
  double globalHeading;
  /** Heading relative to last offset */
  double relativeHeading;

  /** Offset between global heading and relative heading */
  double offset;

  private int multiplier;

  /**
   * Create a new object for the built-in gyro/imu in the Rev Expansion Hub
   *
   * @param hw Hardware map
   * @param imuName Name of sensor in configuration
   */
  public RevIMU(HardwareMap hw, String imuName) {
    revIMU = hw.get(BNO055IMU.class, imuName);
    multiplier = 1;
  }

  /**
   * Create a new object for the built-in gyro/imu in the Rev Expansion Hub with the default
   * configuration name of "imu"
   *
   * @param hw Hardware map
   */
  public RevIMU(HardwareMap hw) {
    this(hw, "imu");
  }

  /** Initializes gyro with default parameters. */
  public void init() {
    BNO055IMU.Parameters parameters = new BNO055IMU.Parameters();
    parameters.angleUnit = BNO055IMU.AngleUnit.DEGREES;
    parameters.accelUnit = BNO055IMU.AccelUnit.METERS_PERSEC_PERSEC;
    parameters.calibrationDataFile =
        "BNO055IMUCalibration.json"; // see the calibration sample opmode
    parameters.loggingEnabled = true;
    parameters.loggingTag = "IMU";
    parameters.accelerationIntegrationAlgorithm = new JustLoggingAccelerationIntegrator();

    init(parameters);
  }

  /** Initializes gyro with custom parameters. */
  public void init(BNO055IMU.Parameters parameters) {
    revIMU.initialize(parameters);

    globalHeading = 0;
    relativeHeading = 0;
    offset = 0;
  }

  /** Inverts the ouptut of gyro */
  public void invertGyro() {
    multiplier *= -1;
  }

  /** @return Relative heading of the robot */
  public double getHeading() {
    globalHeading = revIMU.getAngularOrientation().firstAngle;
    relativeHeading = globalHeading + offset;
    // Return yaw
    return relativeHeading * multiplier;
  }

  /** @return Absolute heading of the robot */
  @Override
  public double getAbsoluteHeading() {
    return revIMU.getAngularOrientation().firstAngle * multiplier;
  }

  /** @return X, Y, Z angles of gyro */
  public double[] getAngles() {

    double[] angles = new double[4];

    angles[0] = (double) revIMU.getAngularOrientation().firstAngle;
    angles[1] = (double) revIMU.getAngularOrientation().secondAngle;
    angles[2] = (double) revIMU.getAngularOrientation().thirdAngle;

    return angles;
  }

  /** @return Transforms heading into rotation2d */
  @Override
  public Rotation2d getRotation2d() {
    return Rotation2d.fromDegrees(getHeading());
  }

  @Override
  /** Don't need to call this function hardly ever. */
  public void disable() {
    revIMU.close();
  }

  @Override
  public void reset() {
    offset = -getHeading();
  }

  @Override
  public String getDeviceType() {
    return "Rev Expansion Hub IMU";
  }

  /** Get the underlying sensor * */
  public BNO055IMU getRevIMU() {
    return revIMU;
  }
}
