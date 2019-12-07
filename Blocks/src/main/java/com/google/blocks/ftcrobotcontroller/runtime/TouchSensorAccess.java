// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import android.webkit.JavascriptInterface;
import com.google.blocks.ftcrobotcontroller.hardware.HardwareItem;
import com.qualcomm.hardware.hitechnic.HiTechnicNxtTouchSensor;
import com.qualcomm.hardware.rev.RevTouchSensor;
import com.qualcomm.hardware.modernrobotics.ModernRoboticsTouchSensor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.TouchSensor;

/**
 * A class that provides JavaScript access to a {@link TouchSensor}.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class TouchSensorAccess extends HardwareAccess<TouchSensor> {
  private final TouchSensor touchSensor;

  TouchSensorAccess(BlocksOpMode blocksOpMode, HardwareItem hardwareItem, HardwareMap hardwareMap) {
    super(blocksOpMode, hardwareItem, hardwareMap, TouchSensor.class);
    this.touchSensor = hardwareDevice;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {HiTechnicNxtTouchSensor.class, ModernRoboticsTouchSensor.class, RevTouchSensor.class}, methodName = "isPressed")
  public boolean getIsPressed() {
    startBlockExecution(BlockType.GETTER, ".IsPressed");
    return touchSensor.isPressed();
  }
}
