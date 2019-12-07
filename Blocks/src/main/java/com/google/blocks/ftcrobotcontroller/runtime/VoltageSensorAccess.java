// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import android.webkit.JavascriptInterface;
import com.google.blocks.ftcrobotcontroller.hardware.HardwareItem;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.VoltageSensor;

/**
 * A class that provides JavaScript access to an {@link VoltageSensor}.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class VoltageSensorAccess extends HardwareAccess<VoltageSensor> {
  private final VoltageSensor voltageSensor;

  VoltageSensorAccess(BlocksOpMode blocksOpMode, HardwareItem hardwareItem, HardwareMap hardwareMap) {
    super(blocksOpMode, hardwareItem, hardwareMap, VoltageSensor.class);
    this.voltageSensor = hardwareDevice;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public double getVoltage() {
    startBlockExecution(BlockType.GETTER, ".Voltage");
    return voltageSensor.getVoltage();
  }
}
