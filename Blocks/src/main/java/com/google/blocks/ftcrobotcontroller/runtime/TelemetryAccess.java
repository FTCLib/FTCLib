// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import android.webkit.JavascriptInterface;
import org.firstinspires.ftc.robotcore.external.Telemetry;

/**
 * A class that provides JavaScript access to {@link Telemetry}.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class TelemetryAccess extends Access {
  private final Telemetry telemetry;

  TelemetryAccess(BlocksOpMode blocksOpMode, String identifier, Telemetry telemetry) {
    super(blocksOpMode, identifier, "Telemetry");
    this.telemetry = telemetry;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void addNumericData(String key, double data) {
    startBlockExecution(BlockType.FUNCTION, ".addData");
    telemetry.addData(key, data);
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void addTextData(String key, String data) {
    startBlockExecution(BlockType.FUNCTION, ".addData");
    if (data != null) {
      telemetry.addData(key, data);
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void addObjectData(String key, Object data) {
    startBlockExecution(BlockType.FUNCTION, ".addData");
    // Avoid calling data.toString() in case data is null.
    telemetry.addData(key, "" + data);
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void update() {
    startBlockExecution(BlockType.FUNCTION, ".update");
    telemetry.update();
  }
}
