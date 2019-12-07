// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import android.webkit.JavascriptInterface;
import org.firstinspires.ftc.robotcore.external.JavaUtil;
import org.firstinspires.ftc.robotcore.external.matrices.OpenGLMatrix;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;

/**
 * A class that provides JavaScript access to miscellaneous functionality.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class MiscAccess extends Access {

  MiscAccess(BlocksOpMode blocksOpMode, String identifier) {
    super(blocksOpMode, identifier, "");
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public Object getNull() {
    startBlockExecution(BlockType.SPECIAL, "null");
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public boolean isNull(Object value) {
    startBlockExecution(BlockType.SPECIAL, "isNull");
    return (value == null);
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public boolean isNotNull(Object value) {
    startBlockExecution(BlockType.SPECIAL, "isNotNull");
    return (value != null);
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public String formatNumber(double number, int precision) {
    startBlockExecution(BlockType.SPECIAL, "formatNumber");
    return JavaUtil.formatNumber(number, precision);
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public double roundDecimal(double number, int precision) {
    startBlockExecution(BlockType.SPECIAL, "roundDecimal");
    return Double.parseDouble(JavaUtil.formatNumber(number, precision));
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public OpenGLMatrix getUpdatedRobotLocation(
      float x, float y, float z, float xAngle, float yAngle, float zAngle) {
    startBlockExecution(BlockType.FUNCTION, "VuforiaTrackingResults", ".getUpdatedRobotLocation");
    return OpenGLMatrix
        .translation(x, y, z)
        .multiplied(Orientation.getRotationMatrix(
            AxesReference.EXTRINSIC, AxesOrder.XYZ, AngleUnit.DEGREES, xAngle, yAngle, zAngle));
  }
}
