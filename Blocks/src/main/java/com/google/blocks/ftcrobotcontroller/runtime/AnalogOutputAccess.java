// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import android.webkit.JavascriptInterface;
import com.google.blocks.ftcrobotcontroller.hardware.HardwareItem;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.AnalogOutput;

/**
 * A class that provides JavaScript access to a {@link AnalogOutput}.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class AnalogOutputAccess extends HardwareAccess<AnalogOutput> {
  private final AnalogOutput analogOutput;

  AnalogOutputAccess(BlocksOpMode blocksOpMode, HardwareItem hardwareItem, HardwareMap hardwareMap) {
    super(blocksOpMode, hardwareItem, hardwareMap, AnalogOutput.class);
    this.analogOutput = hardwareDevice;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = AnalogOutput.class, methodName = "setAnalogOutputVoltage")
  public void setAnalogOutputVoltage(int voltage) {
    startBlockExecution(BlockType.FUNCTION, ".setAnalogOutputVoltage");
    analogOutput.setAnalogOutputVoltage(voltage);
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = AnalogOutput.class, methodName = "setAnalogOutputFrequency")
  public void setAnalogOutputFrequency(int frequency) {
    startBlockExecution(BlockType.FUNCTION, ".setAnalogOutputFrequency");
    analogOutput.setAnalogOutputFrequency(frequency);
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = AnalogOutput.class, methodName = "setAnalogOutputMode")
  public void setAnalogOutputMode(int mode) {
    startBlockExecution(BlockType.FUNCTION, ".setAnalogOutputMode");
    analogOutput.setAnalogOutputMode((byte) mode);
  }
}
