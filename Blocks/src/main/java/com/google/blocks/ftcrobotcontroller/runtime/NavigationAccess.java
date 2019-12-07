// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import android.webkit.JavascriptInterface;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.UnnormalizedAngleUnit;

/**
 * A class that provides JavaScript access to various navigation enum methods.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class NavigationAccess extends Access {

  NavigationAccess(BlocksOpMode blocksOpMode, String identifier) {
    super(blocksOpMode, identifier, "");
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public double angleUnit_normalize(double angle, String angleUnitString) {
    startBlockExecution(BlockType.FUNCTION, "AngleUnit", ".normalize");
    AngleUnit angleUnit = checkAngleUnit(angleUnitString);
    if (angleUnit != null) {
      return angleUnit.normalize(angle);
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public double angleUnit_convert(double angle, String fromAngleUnitString, String toAngleUnitString) {
    startBlockExecution(BlockType.FUNCTION, "AngleUnit", ".convert");
    AngleUnit fromAngleUnit = checkArg(fromAngleUnitString, AngleUnit.class, "from");
    AngleUnit toAngleUnit = checkArg(toAngleUnitString, AngleUnit.class, "to");
    if (fromAngleUnit != null && toAngleUnit != null) {
      return toAngleUnit.fromUnit(fromAngleUnit, angle);
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public double unnormalizedAngleUnit_convert(double angle, String fromAngleUnitString, String toAngleUnitString) {
    startBlockExecution(BlockType.FUNCTION, "UnnormalizedAngleUnit", ".convert");
    AngleUnit fromAngleUnit = checkArg(fromAngleUnitString, AngleUnit.class, "from");
    AngleUnit toAngleUnit = checkArg(toAngleUnitString, AngleUnit.class, "to");
    if (fromAngleUnit != null && toAngleUnit != null) {
      return toAngleUnit.getUnnormalized().fromUnit(fromAngleUnit.getUnnormalized(), angle);
    }
    return 0;
  }
}
