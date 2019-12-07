// Copyright 2017 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import android.webkit.JavascriptInterface;
import com.qualcomm.robotcore.util.Range;

/**
 * A class that provides JavaScript access to {@link Range}.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class RangeAccess extends Access {

  RangeAccess(BlocksOpMode blocksOpMode, String identifier) {
    super(blocksOpMode, identifier, "Range");
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public double clip(double number, double min, double max) {
    startBlockExecution(BlockType.FUNCTION, ".clip");
    return Range.clip(number, min, max);
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public double scale(double number, double x1, double x2, double y1, double y2) {
    startBlockExecution(BlockType.FUNCTION, ".scale");
    return Range.scale(number, x1, x2, y1, y2);
  }
}
