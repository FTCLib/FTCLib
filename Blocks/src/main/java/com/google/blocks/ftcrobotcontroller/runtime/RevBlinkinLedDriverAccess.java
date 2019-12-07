/*
Copyright 2018 Google LLC.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.google.blocks.ftcrobotcontroller.runtime;

import android.webkit.JavascriptInterface;
import com.google.blocks.ftcrobotcontroller.hardware.HardwareItem;
import com.qualcomm.hardware.rev.RevBlinkinLedDriver;
import com.qualcomm.hardware.rev.RevBlinkinLedDriver.BlinkinPattern;
import com.qualcomm.robotcore.hardware.HardwareMap;

/**
 * A class that provides JavaScript access to a {@link RevBlinkinLedDriver}.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class RevBlinkinLedDriverAccess extends HardwareAccess<RevBlinkinLedDriver> {
  private final RevBlinkinLedDriver revBlinkinLedDriver;

  RevBlinkinLedDriverAccess(BlocksOpMode blocksOpMode, HardwareItem hardwareItem, HardwareMap hardwareMap) {
    super(blocksOpMode, hardwareItem, hardwareMap, RevBlinkinLedDriver.class);
    revBlinkinLedDriver = hardwareDevice;
  }

  // From com.qualcomm.hardware.rev.RevBlinkinLedDriver

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void setPattern(String blinkinPatternString) {
    startBlockExecution(BlockType.SETTER, ".Pattern");
    BlinkinPattern blinkinPattern = checkBlinkinPattern(blinkinPatternString);
    if (blinkinPattern != null) {
      revBlinkinLedDriver.setPattern(blinkinPattern);
    }
  }
}
