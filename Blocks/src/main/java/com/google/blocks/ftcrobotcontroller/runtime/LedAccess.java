// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import android.webkit.JavascriptInterface;
import com.google.blocks.ftcrobotcontroller.hardware.HardwareItem;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.LED;

/**
 * A class that provides JavaScript access to a {@link LED}.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class LedAccess extends HardwareAccess<LED> {
  private final LED led;

  LedAccess(BlocksOpMode blocksOpMode, HardwareItem hardwareItem, HardwareMap hardwareMap) {
    super(blocksOpMode, hardwareItem, hardwareMap, LED.class);
    this.led = hardwareDevice;
  }

  // from com.qualcomm.robotcore.hardware.LED

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {LED.class}, methodName = "enable")
  public void enableLed(boolean enable) {
    startBlockExecution(BlockType.FUNCTION, ".enableLed");
    led.enable(enable);
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {LED.class}, methodName = "isLightOn")
  public boolean isLightOn() {
    startBlockExecution(BlockType.FUNCTION, ".isLightOn");
    return led.isLightOn();
  }
}
