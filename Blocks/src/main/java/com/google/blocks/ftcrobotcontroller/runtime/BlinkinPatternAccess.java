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
import com.qualcomm.hardware.rev.RevBlinkinLedDriver.BlinkinPattern;

/**
 * A class that provides JavaScript access to various RevBlinkinLedDriver.BlinkinPattern enum methods.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class BlinkinPatternAccess extends Access {

  BlinkinPatternAccess(BlocksOpMode blocksOpMode, String identifier) {
    super(blocksOpMode, identifier, "BlinkinPattern");
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public String fromNumber(int number) {
    startBlockExecution(BlockType.FUNCTION, ".fromNumber");
    return BlinkinPattern.fromNumber(number).toString();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public int toNumber(String blinkinPatternString) {
    startBlockExecution(BlockType.FUNCTION, ".toNumber");
    BlinkinPattern blinkinPattern = checkBlinkinPattern(blinkinPatternString);
    if (blinkinPattern != null) {
      return blinkinPattern.ordinal();
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public String fromText(String text) {
    startBlockExecution(BlockType.FUNCTION, ".fromText");
    BlinkinPattern blinkinPattern = checkBlinkinPattern(text);
    if (blinkinPattern != null) {
      return blinkinPattern.toString();
    }
    return "";
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public String toText(String blinkinPatternString) {
    startBlockExecution(BlockType.FUNCTION, ".toText");
    BlinkinPattern blinkinPattern = checkBlinkinPattern(blinkinPatternString);
    if (blinkinPattern != null) {
      return blinkinPattern.toString();
    }
    return "";
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public String next(String blinkinPatternString) {
    startBlockExecution(BlockType.FUNCTION, ".next");
    BlinkinPattern blinkinPattern = checkBlinkinPattern(blinkinPatternString);
    if (blinkinPattern != null) {
      return blinkinPattern.next().toString();
    }
    return "";
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public String previous(String blinkinPatternString) {
    startBlockExecution(BlockType.FUNCTION, ".previous");
    BlinkinPattern blinkinPattern = checkBlinkinPattern(blinkinPatternString);
    if (blinkinPattern != null) {
      return blinkinPattern.previous().toString();
    }
    return "";
  }
}
