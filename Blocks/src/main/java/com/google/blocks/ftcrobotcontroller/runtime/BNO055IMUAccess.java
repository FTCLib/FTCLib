// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import android.webkit.JavascriptInterface;
import com.google.blocks.ftcrobotcontroller.hardware.HardwareItem;
import com.qualcomm.hardware.adafruit.AdafruitBNO055IMU;
import com.qualcomm.hardware.bosch.BNO055IMU.CalibrationStatus;
import com.qualcomm.hardware.bosch.BNO055IMU.Parameters;
import com.qualcomm.hardware.bosch.BNO055IMU.SystemError;
import com.qualcomm.hardware.bosch.BNO055IMU.SystemStatus;
import com.qualcomm.hardware.bosch.BNO055IMUImpl;
import com.qualcomm.hardware.lynx.LynxEmbeddedIMU;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.I2cAddr;
import com.qualcomm.robotcore.util.ReadWriteFile;
import java.util.Set;
import org.firstinspires.ftc.robotcore.external.navigation.Acceleration;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AngularVelocity;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.Axis;
import org.firstinspires.ftc.robotcore.external.navigation.MagneticFlux;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;
import org.firstinspires.ftc.robotcore.external.navigation.Position;
import org.firstinspires.ftc.robotcore.external.navigation.Quaternion;
import org.firstinspires.ftc.robotcore.external.navigation.Temperature;
import org.firstinspires.ftc.robotcore.external.navigation.Velocity;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

/**
 * A class that provides JavaScript access to {@link BNO055IMUImpl}.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class BNO055IMUAccess extends HardwareAccess<BNO055IMUImpl> {
  private final BNO055IMUImpl imu;

  BNO055IMUAccess(BlocksOpMode blocksOpMode, HardwareItem hardwareItem, HardwareMap hardwareMap) {
    super(blocksOpMode, hardwareItem, hardwareMap, BNO055IMUImpl.class);
    this.imu = hardwareDevice;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {AdafruitBNO055IMU.class, LynxEmbeddedIMU.class}, methodName = "getAcceleration")
  public Acceleration getAcceleration() {
    startBlockExecution(BlockType.GETTER, ".Acceleration");
    return imu.getAcceleration();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {AdafruitBNO055IMU.class, LynxEmbeddedIMU.class}, methodName = "getAngularOrientation")
  public Orientation getAngularOrientation() {
    startBlockExecution(BlockType.GETTER, ".AngularOrientation");
    return imu.getAngularOrientation();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {AdafruitBNO055IMU.class, LynxEmbeddedIMU.class}, methodName = "getAngularVelocity")
  public AngularVelocity getAngularVelocity() {
    startBlockExecution(BlockType.GETTER, ".AngularVelocity");
    return imu.getAngularVelocity();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {AdafruitBNO055IMU.class, LynxEmbeddedIMU.class}, methodName = "getCalibrationStatus")
  public String getCalibrationStatus() {
    startBlockExecution(BlockType.GETTER, ".CalibrationStatus");
    CalibrationStatus calibrationStatus = imu.getCalibrationStatus();
    if (calibrationStatus != null) {
      return calibrationStatus.toString();
    }
    return "";
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {AdafruitBNO055IMU.class, LynxEmbeddedIMU.class}, methodName = "getGravity")
  public Acceleration getGravity() {
    startBlockExecution(BlockType.GETTER, ".Gravity");
    return imu.getGravity();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {AdafruitBNO055IMU.class, LynxEmbeddedIMU.class}, methodName = "getLinearAcceleration")
  public Acceleration getLinearAcceleration() {
    startBlockExecution(BlockType.GETTER, ".LinearAcceleration");
    return imu.getLinearAcceleration();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {AdafruitBNO055IMU.class, LynxEmbeddedIMU.class}, methodName = "getMagneticFieldStrength")
  public MagneticFlux getMagneticFieldStrength() {
    startBlockExecution(BlockType.GETTER, ".MagneticFieldStrength");
    return imu.getMagneticFieldStrength();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {AdafruitBNO055IMU.class, LynxEmbeddedIMU.class}, methodName = "getOverallAcceleration")
  public Acceleration getOverallAcceleration() {
    startBlockExecution(BlockType.GETTER, ".OverallAcceleration");
    return imu.getOverallAcceleration();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {AdafruitBNO055IMU.class, LynxEmbeddedIMU.class}, methodName = "getParameters")
  public Parameters getParameters() {
    startBlockExecution(BlockType.GETTER, ".Parameters");
    return imu.getParameters();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {AdafruitBNO055IMU.class, LynxEmbeddedIMU.class}, methodName = "getPosition")
  public Position getPosition() {
    startBlockExecution(BlockType.GETTER, ".Position");
    return imu.getPosition();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {AdafruitBNO055IMU.class, LynxEmbeddedIMU.class}, methodName = "getQuaternionOrientation")
  public Quaternion getQuaternionOrientation() {
    startBlockExecution(BlockType.GETTER, ".QuaternionOrientation");
    return imu.getQuaternionOrientation();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {AdafruitBNO055IMU.class, LynxEmbeddedIMU.class}, methodName = "getSystemError")
  public String getSystemError() {
    startBlockExecution(BlockType.GETTER, ".SystemError");
    SystemError systemError = imu.getSystemError();
    if (systemError != null) {
      return systemError.toString();
    }
    return "";
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {AdafruitBNO055IMU.class, LynxEmbeddedIMU.class}, methodName = "getSystemStatus")
  public String getSystemStatus() {
    startBlockExecution(BlockType.GETTER, ".SystemStatus");
    SystemStatus systemStatus = imu.getSystemStatus();
    if (systemStatus != null) {
      return systemStatus.toString();
    }
    return "";
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {AdafruitBNO055IMU.class, LynxEmbeddedIMU.class}, methodName = "getTemperature")
  public Temperature getTemperature() {
    startBlockExecution(BlockType.GETTER, ".Temperature");
    return imu.getTemperature();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {AdafruitBNO055IMU.class, LynxEmbeddedIMU.class}, methodName = "getVelocity")
  public Velocity getVelocity() {
    startBlockExecution(BlockType.GETTER, ".Velocity");
    return imu.getVelocity();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {AdafruitBNO055IMU.class, LynxEmbeddedIMU.class}, methodName = "initialize")
  public void initialize(Object parametersArg) {
    startBlockExecution(BlockType.FUNCTION, ".initialize");
    Parameters parameters = checkBNO055IMUParameters(parametersArg);
    if (parameters != null) {
      imu.initialize(parameters);
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {AdafruitBNO055IMU.class, LynxEmbeddedIMU.class}, methodName = "startAccelerationIntegration")
  public void startAccelerationIntegration_with1(int msPollInterval) {
    startBlockExecution(BlockType.FUNCTION, ".startAccelerationIntegration");
    imu.startAccelerationIntegration(
        null /* initialPosition */, null /* initialVelocity */, msPollInterval);
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {AdafruitBNO055IMU.class, LynxEmbeddedIMU.class}, methodName = "startAccelerationIntegration")
  public void startAccelerationIntegration_with3(
      Object initialPositionArg, Object initialVelocityArg, int msPollInterval) {
    startBlockExecution(BlockType.FUNCTION, ".startAccelerationIntegration");
    Position initialPosition = checkArg(initialPositionArg, Position.class, "initialPosition");
    Velocity initialVelocity = checkArg(initialVelocityArg, Velocity.class, "initialVelocity");
    if (initialPosition != null && initialVelocity != null) {
      imu.startAccelerationIntegration(initialPosition, initialVelocity, msPollInterval);
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {AdafruitBNO055IMU.class, LynxEmbeddedIMU.class}, methodName = "stopAccelerationIntegration")
  public void stopAccelerationIntegration() {
    startBlockExecution(BlockType.FUNCTION, ".stopAccelerationIntegration");
    imu.stopAccelerationIntegration();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {AdafruitBNO055IMU.class, LynxEmbeddedIMU.class}, methodName = "isSystemCalibrated")
  public boolean isSystemCalibrated() {
    startBlockExecution(BlockType.FUNCTION, ".isSystemCalibrated");
    return imu.isSystemCalibrated();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {AdafruitBNO055IMU.class, LynxEmbeddedIMU.class}, methodName = "isGyroCalibrated")
  public boolean isGyroCalibrated() {
    startBlockExecution(BlockType.FUNCTION, ".isGyroCalibrated");
    return imu.isGyroCalibrated();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {AdafruitBNO055IMU.class, LynxEmbeddedIMU.class}, methodName = "isAccelerometerCalibrated")
  public boolean isAccelerometerCalibrated() {
    startBlockExecution(BlockType.FUNCTION, ".isAccelerometerCalibrated");
    return imu.isAccelerometerCalibrated();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {AdafruitBNO055IMU.class, LynxEmbeddedIMU.class}, methodName = "isMagnetometerCalibrated")
  public boolean isMagnetometerCalibrated() {
    startBlockExecution(BlockType.FUNCTION, ".isMagnetometerCalibrated");
    return imu.isMagnetometerCalibrated();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {AdafruitBNO055IMU.class, LynxEmbeddedIMU.class}, methodName = "readCalibrationData")
  public void saveCalibrationData(String absoluteFileName) {
    startBlockExecution(BlockType.FUNCTION, ".saveCalibrationData");
    ReadWriteFile.writeFile(
        AppUtil.getInstance().getSettingsFile(absoluteFileName),
        imu.readCalibrationData().serialize());
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {AdafruitBNO055IMU.class, LynxEmbeddedIMU.class}, methodName = "setI2cAddress")
  public void setI2cAddress7Bit(int i2cAddr7Bit) {
    startBlockExecution(BlockType.SETTER, ".I2cAddress7Bit");
    imu.setI2cAddress(I2cAddr.create7bit(i2cAddr7Bit));
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {AdafruitBNO055IMU.class, LynxEmbeddedIMU.class}, methodName = "getI2cAddress")
  public int getI2cAddress7Bit() {
    startBlockExecution(BlockType.GETTER, ".I2cAddress7Bit");
    I2cAddr i2cAddr = imu.getI2cAddress();
    if (i2cAddr != null) {
      return i2cAddr.get7Bit();
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {AdafruitBNO055IMU.class, LynxEmbeddedIMU.class}, methodName = "setI2cAddress")
  public void setI2cAddress8Bit(int i2cAddr8Bit) {
    startBlockExecution(BlockType.SETTER, ".I2cAddress8Bit");
    imu.setI2cAddress(I2cAddr.create8bit(i2cAddr8Bit));
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {AdafruitBNO055IMU.class, LynxEmbeddedIMU.class}, methodName = "getI2cAddress")
  public int getI2cAddress8Bit() {
    startBlockExecution(BlockType.GETTER, ".I2cAddress8Bit");
    I2cAddr i2cAddr = imu.getI2cAddress();
    if (i2cAddr != null) {
      return i2cAddr.get8Bit();
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {AdafruitBNO055IMU.class, LynxEmbeddedIMU.class}, methodName = "getAngularVelocityAxes")
  public String getAngularVelocityAxes() {
    startBlockExecution(BlockType.GETTER, ".AngularVelocityAxes");
    Set<Axis> axes = imu.getAngularVelocityAxes();
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    String delimiter = "";
    for (Axis axis : axes) {
      sb.append(delimiter).append("\"").append(axis.toString()).append("\"");
      delimiter = ",";
    }
    sb.append("]");
    return sb.toString();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {AdafruitBNO055IMU.class, LynxEmbeddedIMU.class}, methodName = "getAngularVelocity")
  public AngularVelocity getAngularVelocity(String angleUnitString) {
    startBlockExecution(BlockType.FUNCTION, ".getAngularVelocity");
    AngleUnit angleUnit = checkAngleUnit(angleUnitString);
    if (angleUnit != null) {
      return imu.getAngularVelocity(angleUnit);
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {AdafruitBNO055IMU.class, LynxEmbeddedIMU.class}, methodName = "getAngularOrientationAxes")
  public String getAngularOrientationAxes() {
    startBlockExecution(BlockType.GETTER, ".AngularOrientationAxes");
    Set<Axis> axes = imu.getAngularOrientationAxes();
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    String delimiter = "";
    for (Axis axis : axes) {
      sb.append(delimiter).append("\"").append(axis.toString()).append("\"");
      delimiter = ",";
    }
    sb.append("]");
    return sb.toString();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {AdafruitBNO055IMU.class, LynxEmbeddedIMU.class}, methodName = "getAngularOrientation")
  public Orientation getAngularOrientation(String axesReferenceString, String axesOrderString, String angleUnitString) {
    startBlockExecution(BlockType.FUNCTION, ".getAngularOrientation");
    AxesReference axesReference = checkAxesReference(axesReferenceString);
    AxesOrder axesOrder = checkAxesOrder(axesOrderString);
    AngleUnit angleUnit = checkAngleUnit(angleUnitString);
    if (axesReference != null && axesOrder != null && angleUnit != null) {
      return imu.getAngularOrientation(axesReference, axesOrder, angleUnit);
    }
    return null;
  }
}
