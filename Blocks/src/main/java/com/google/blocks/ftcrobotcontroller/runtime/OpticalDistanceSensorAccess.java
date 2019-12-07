// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import android.webkit.JavascriptInterface;
import com.google.blocks.ftcrobotcontroller.hardware.HardwareItem;
import com.qualcomm.hardware.modernrobotics.ModernRoboticsAnalogOpticalDistanceSensor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.OpticalDistanceSensor;

/**
 * A class that provides JavaScript access to a {@link OpticalDistanceSensor}.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class OpticalDistanceSensorAccess extends HardwareAccess<OpticalDistanceSensor> {
  private final OpticalDistanceSensor opticalDistanceSensor;

  OpticalDistanceSensorAccess(BlocksOpMode blocksOpMode, HardwareItem hardwareItem, HardwareMap hardwareMap) {
    super(blocksOpMode, hardwareItem, hardwareMap, OpticalDistanceSensor.class);
    this.opticalDistanceSensor = hardwareDevice;
  }

  // from com.qualcomm.robotcore.hardware.LightSensor

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {ModernRoboticsAnalogOpticalDistanceSensor.class}, methodName = "getLightDetected")
  public double getLightDetected() {
    startBlockExecution(BlockType.GETTER, ".LightDetected");
    return opticalDistanceSensor.getLightDetected();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {ModernRoboticsAnalogOpticalDistanceSensor.class}, methodName = "getRawLightDetected")
  public double getRawLightDetected() {
    startBlockExecution(BlockType.GETTER, ".RawLightDetected");
    return opticalDistanceSensor.getRawLightDetected();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {ModernRoboticsAnalogOpticalDistanceSensor.class}, methodName = "getRawLightDetectedMax")
  public double getRawLightDetectedMax() {
    startBlockExecution(BlockType.GETTER, ".RawLightDetectedMax");
    return opticalDistanceSensor.getRawLightDetectedMax();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {ModernRoboticsAnalogOpticalDistanceSensor.class}, methodName = "enableLed")
  public void enableLed(boolean enable) {
    startBlockExecution(BlockType.FUNCTION, ".enableLed");
    opticalDistanceSensor.enableLed(enable);
  }
}
