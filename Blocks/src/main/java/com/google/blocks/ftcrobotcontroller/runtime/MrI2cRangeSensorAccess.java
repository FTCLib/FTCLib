// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import android.webkit.JavascriptInterface;
import com.google.blocks.ftcrobotcontroller.hardware.HardwareItem;
import com.qualcomm.hardware.modernrobotics.ModernRoboticsI2cRangeSensor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.I2cAddr;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

/**
 * A class that provides JavaScript access to a {@link ModernRoboticsI2cRangeSensor}.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class MrI2cRangeSensorAccess extends HardwareAccess<ModernRoboticsI2cRangeSensor> {
  private final ModernRoboticsI2cRangeSensor mrI2cRangeSensor;

  MrI2cRangeSensorAccess(BlocksOpMode blocksOpMode, HardwareItem hardwareItem, HardwareMap hardwareMap) {
    super(blocksOpMode, hardwareItem, hardwareMap, ModernRoboticsI2cRangeSensor.class);
    this.mrI2cRangeSensor = hardwareDevice;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {ModernRoboticsI2cRangeSensor.class}, methodName = "getLightDetected")
  public double getLightDetected() {
    startBlockExecution(BlockType.GETTER, ".LightDetected");
    return mrI2cRangeSensor.getLightDetected();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {ModernRoboticsI2cRangeSensor.class}, methodName = "getRawLightDetected")
  public double getRawLightDetected() {
    startBlockExecution(BlockType.GETTER, ".RawLightDetected");
    return mrI2cRangeSensor.getRawLightDetected();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {ModernRoboticsI2cRangeSensor.class}, methodName = "getRawLightDetectedMax")
  public double getRawLightDetectedMax() {
    startBlockExecution(BlockType.GETTER, ".RawLightDetectedMax");
    return mrI2cRangeSensor.getRawLightDetectedMax();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {ModernRoboticsI2cRangeSensor.class}, methodName = "rawUltrasonic")
  public double getRawUltrasonic() {
    startBlockExecution(BlockType.GETTER, ".RawUltrasonic");
    return mrI2cRangeSensor.rawUltrasonic();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {ModernRoboticsI2cRangeSensor.class}, methodName = "rawOptical")
  public double getRawOptical() {
    startBlockExecution(BlockType.GETTER, ".RawOptical");
    return mrI2cRangeSensor.rawOptical();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {ModernRoboticsI2cRangeSensor.class}, methodName = "cmUltrasonic")
  public double getCmUltrasonic() {
    startBlockExecution(BlockType.GETTER, ".CmUltrasonic");
    return mrI2cRangeSensor.cmUltrasonic();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {ModernRoboticsI2cRangeSensor.class}, methodName = "cmOptical")
  public double getCmOptical() {
    startBlockExecution(BlockType.GETTER, ".CmOptical");
    return mrI2cRangeSensor.cmOptical();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {ModernRoboticsI2cRangeSensor.class}, methodName = "getDistance")
  public double getDistance(String distanceUnitString) {
    startBlockExecution(BlockType.FUNCTION, ".getDistance");
    DistanceUnit distanceUnit = checkArg(distanceUnitString, DistanceUnit.class, "unit");
    if (distanceUnit != null) {
      return mrI2cRangeSensor.getDistance(distanceUnit);
    }
    return 0.0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {ModernRoboticsI2cRangeSensor.class}, methodName = "setI2cAddress")
  public void setI2cAddress7Bit(int i2cAddr7Bit) {
    startBlockExecution(BlockType.SETTER, ".I2cAddress7Bit");
    mrI2cRangeSensor.setI2cAddress(I2cAddr.create7bit(i2cAddr7Bit));
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {ModernRoboticsI2cRangeSensor.class}, methodName = "getI2cAddress")
  public int getI2cAddress7Bit() {
    startBlockExecution(BlockType.GETTER, ".I2cAddress7Bit");
    I2cAddr i2cAddr = mrI2cRangeSensor.getI2cAddress();
    if (i2cAddr != null) {
      return i2cAddr.get7Bit();
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {ModernRoboticsI2cRangeSensor.class}, methodName = "setI2cAddress")
  public void setI2cAddress8Bit(int i2cAddr8Bit) {
    startBlockExecution(BlockType.SETTER, ".I2cAddress8Bit");
    mrI2cRangeSensor.setI2cAddress(I2cAddr.create8bit(i2cAddr8Bit));
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {ModernRoboticsI2cRangeSensor.class}, methodName = "getI2cAddress")
  public int getI2cAddress8Bit() {
    startBlockExecution(BlockType.GETTER, ".I2cAddress8Bit");
    I2cAddr i2cAddr = mrI2cRangeSensor.getI2cAddress();
    if (i2cAddr != null) {
      return i2cAddr.get8Bit();
    }
    return 0;
  }
}
