// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import android.webkit.JavascriptInterface;
import com.google.blocks.ftcrobotcontroller.hardware.HardwareItem;
import com.qualcomm.robotcore.hardware.AnalogInput;
import com.qualcomm.robotcore.hardware.HardwareMap;

/**
 * A class that provides JavaScript access to a {@link AnalogInput}.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class AnalogInputAccess extends HardwareAccess<AnalogInput> {
  private final AnalogInput analogInput;

  AnalogInputAccess(BlocksOpMode blocksOpMode, HardwareItem hardwareItem, HardwareMap hardwareMap) {
    super(blocksOpMode, hardwareItem, hardwareMap, AnalogInput.class);
    this.analogInput = hardwareDevice;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = AnalogInput.class, methodName = "getVoltage")
  public double getVoltage() {
    startBlockExecution(BlockType.GETTER, ".Voltage");
    return analogInput.getVoltage();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = AnalogInput.class, methodName = "getMaxVoltage")
  public double getMaxVoltage() {
    startBlockExecution(BlockType.GETTER, ".MaxVoltage");
    return analogInput.getMaxVoltage();
  }
}
