// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import android.webkit.JavascriptInterface;
import com.google.blocks.ftcrobotcontroller.hardware.HardwareItem;
import com.qualcomm.hardware.modernrobotics.ModernRoboticsI2cCompassSensor;
import com.qualcomm.robotcore.hardware.CompassSensor.CompassMode;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.I2cAddr;
import org.firstinspires.ftc.robotcore.external.navigation.Acceleration;
import org.firstinspires.ftc.robotcore.external.navigation.MagneticFlux;

/**
 * A class that provides JavaScript access to a {@link ModernRoboticsI2cCompassSensor}.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class MrI2cCompassSensorAccess extends HardwareAccess<ModernRoboticsI2cCompassSensor> {
  private final ModernRoboticsI2cCompassSensor mrI2cCompassSensor;

  MrI2cCompassSensorAccess(BlocksOpMode blocksOpMode, HardwareItem hardwareItem, HardwareMap hardwareMap) {
    super(blocksOpMode, hardwareItem, hardwareMap, ModernRoboticsI2cCompassSensor.class);
    this.mrI2cCompassSensor = hardwareDevice;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {ModernRoboticsI2cCompassSensor.class}, methodName = "getDirection")
  public double getDirection() {
    startBlockExecution(BlockType.GETTER, ".Direction");
    return mrI2cCompassSensor.getDirection();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {ModernRoboticsI2cCompassSensor.class}, methodName = "setI2cAddress")
  public void setI2cAddress7Bit(int i2cAddr7Bit) {
    startBlockExecution(BlockType.SETTER, ".I2cAddress7Bit");
    mrI2cCompassSensor.setI2cAddress(I2cAddr.create7bit(i2cAddr7Bit));
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {ModernRoboticsI2cCompassSensor.class}, methodName = "getI2cAddress")
  public int getI2cAddress7Bit() {
    startBlockExecution(BlockType.GETTER, ".I2cAddress7Bit");
    I2cAddr i2cAddr = mrI2cCompassSensor.getI2cAddress();
    if (i2cAddr != null) {
      return i2cAddr.get7Bit();
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {ModernRoboticsI2cCompassSensor.class}, methodName = "setI2cAddress")
  public void setI2cAddress8Bit(int i2cAddr8Bit) {
    startBlockExecution(BlockType.SETTER, ".I2cAddress8Bit");
    mrI2cCompassSensor.setI2cAddress(I2cAddr.create8bit(i2cAddr8Bit));
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {ModernRoboticsI2cCompassSensor.class}, methodName = "getI2cAddress")
  public int getI2cAddress8Bit() {
    startBlockExecution(BlockType.GETTER, ".I2cAddress8Bit");
    I2cAddr i2cAddr = mrI2cCompassSensor.getI2cAddress();
    if (i2cAddr != null) {
      return i2cAddr.get8Bit();
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {ModernRoboticsI2cCompassSensor.class}, methodName = "getAcceleration")
  public double getXAccel() {
    startBlockExecution(BlockType.GETTER, ".XAccel");
    Acceleration acceleration = mrI2cCompassSensor.getAcceleration();
    if (acceleration != null) {
      return acceleration.xAccel;
    }
    return 0.0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {ModernRoboticsI2cCompassSensor.class}, methodName = "getAcceleration")
  public double getYAccel() {
    startBlockExecution(BlockType.GETTER, ".YAccel");
    Acceleration acceleration = mrI2cCompassSensor.getAcceleration();
    if (acceleration != null) {
      return acceleration.yAccel;
    }
    return 0.0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {ModernRoboticsI2cCompassSensor.class}, methodName = "getAcceleration")
  public double getZAccel() {
    startBlockExecution(BlockType.GETTER, ".ZAccel");
    Acceleration acceleration = mrI2cCompassSensor.getAcceleration();
    if (acceleration != null) {
      return acceleration.zAccel;
    }
    return 0.0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {ModernRoboticsI2cCompassSensor.class}, methodName = "getMagneticFlux")
  public double getXMagneticFlux() {
    startBlockExecution(BlockType.GETTER, ".XMagneticFlux");
    MagneticFlux magneticFlux = mrI2cCompassSensor.getMagneticFlux();
    if (magneticFlux != null) {
      return magneticFlux.x;
    }
    return 0.0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {ModernRoboticsI2cCompassSensor.class}, methodName = "getMagneticFlux")
  public double getYMagneticFlux() {
    startBlockExecution(BlockType.GETTER, ".YMagneticFlux");
    MagneticFlux magneticFlux = mrI2cCompassSensor.getMagneticFlux();
    if (magneticFlux != null) {
      return magneticFlux.y;
    }
    return 0.0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {ModernRoboticsI2cCompassSensor.class}, methodName = "getMagneticFlux")
  public double getZMagneticFlux() {
    startBlockExecution(BlockType.GETTER, ".ZMagneticFlux");
    MagneticFlux magneticFlux = mrI2cCompassSensor.getMagneticFlux();
    if (magneticFlux != null) {
      return magneticFlux.z;
    }
    return 0.0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {ModernRoboticsI2cCompassSensor.class}, methodName = "setMode")
  public void setMode(String compassModeString) {
    startBlockExecution(BlockType.FUNCTION, ".setMode");
    CompassMode compassMode = checkArg(compassModeString, CompassMode.class, "compassMode");
    if (compassMode != null) {
      mrI2cCompassSensor.setMode(compassMode);
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {ModernRoboticsI2cCompassSensor.class}, methodName = "isCalibrating")
  public boolean isCalibrating() {
    startBlockExecution(BlockType.FUNCTION, ".isCalibrating");
    return mrI2cCompassSensor.isCalibrating();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {ModernRoboticsI2cCompassSensor.class}, methodName = "calibrationFailed")
  public boolean calibrationFailed() {
    startBlockExecution(BlockType.FUNCTION, ".calibrationFailed");
    return mrI2cCompassSensor.calibrationFailed();
  }
}
