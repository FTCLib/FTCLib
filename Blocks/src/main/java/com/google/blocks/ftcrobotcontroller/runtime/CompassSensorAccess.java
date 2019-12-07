// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import android.webkit.JavascriptInterface;
import com.google.blocks.ftcrobotcontroller.hardware.HardwareItem;
import com.qualcomm.hardware.hitechnic.HiTechnicNxtCompassSensor;
import com.qualcomm.robotcore.hardware.CompassSensor;
import com.qualcomm.robotcore.hardware.CompassSensor.CompassMode;
import com.qualcomm.robotcore.hardware.HardwareMap;

/**
 * A class that provides JavaScript access to a {@link CompassSensor}.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class CompassSensorAccess extends HardwareAccess<CompassSensor> {
  private final CompassSensor compassSensor;

  CompassSensorAccess(BlocksOpMode blocksOpMode, HardwareItem hardwareItem, HardwareMap hardwareMap) {
    super(blocksOpMode, hardwareItem, hardwareMap, CompassSensor.class);
    this.compassSensor = hardwareDevice;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {HiTechnicNxtCompassSensor.class}, methodName = "getDirection")
  public double getDirection() {
    startBlockExecution(BlockType.GETTER, ".Direction");
    return compassSensor.getDirection();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {HiTechnicNxtCompassSensor.class}, methodName = "calibrationFailed")
  public boolean getCalibrationFailed() {
    startBlockExecution(BlockType.GETTER, ".CalibrationFailed");
    return compassSensor.calibrationFailed();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {HiTechnicNxtCompassSensor.class}, methodName = "setMode")
  public void setMode(String compassModeString) {
    startBlockExecution(BlockType.FUNCTION, ".Mode");
    CompassMode compassMode = checkArg(compassModeString, CompassMode.class, "compassMode");
    if (compassMode != null) {
      compassSensor.setMode(compassMode);
    }
  }
}
