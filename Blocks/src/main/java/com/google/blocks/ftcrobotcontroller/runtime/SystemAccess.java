// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import android.webkit.JavascriptInterface;

/**
 * A class that provides JavaScript access to {@link System}.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class SystemAccess extends Access {

  SystemAccess(BlocksOpMode blocksOpMode, String identifier) {
    super(blocksOpMode, identifier, "System");
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public long nanoTime() {
    startBlockExecution(BlockType.FUNCTION, ".nanoTime");
    return System.nanoTime();
  }
}
