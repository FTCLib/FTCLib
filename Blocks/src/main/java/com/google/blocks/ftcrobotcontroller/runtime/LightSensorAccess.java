// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import android.webkit.JavascriptInterface;
import com.google.blocks.ftcrobotcontroller.hardware.HardwareItem;
import com.qualcomm.hardware.hitechnic.HiTechnicNxtLightSensor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.LightSensor;

/**
 * A class that provides JavaScript access to a {@link LightSensor}.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class LightSensorAccess extends HardwareAccess<LightSensor> {
  private final LightSensor lightSensor;

  LightSensorAccess(BlocksOpMode blocksOpMode, HardwareItem hardwareItem, HardwareMap hardwareMap) {
    super(blocksOpMode, hardwareItem, hardwareMap, LightSensor.class);
    this.lightSensor = hardwareDevice;
  }

  // from com.qualcomm.robotcore.hardware.LightSensor

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {HiTechnicNxtLightSensor.class}, methodName = "getLightDetected")
  public double getLightDetected() {
    startBlockExecution(BlockType.GETTER, ".LightDetected");
    return lightSensor.getLightDetected();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {HiTechnicNxtLightSensor.class}, methodName = "getRawLightDetected")
  public double getRawLightDetected() {
    startBlockExecution(BlockType.GETTER, ".RawLightDetected");
    return lightSensor.getRawLightDetected();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {HiTechnicNxtLightSensor.class}, methodName = "getRawLightDetectedMax")
  public double getRawLightDetectedMax() {
    startBlockExecution(BlockType.GETTER, ".RawLightDetectedMax");
    return lightSensor.getRawLightDetectedMax();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {HiTechnicNxtLightSensor.class}, methodName = "enableLed")
  public void enableLed(boolean enable) {
    startBlockExecution(BlockType.FUNCTION, ".enableLed");
    lightSensor.enableLed(enable);
  }
}
