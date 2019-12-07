// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import android.webkit.JavascriptInterface;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AngularVelocity;
import org.firstinspires.ftc.robotcore.external.navigation.Axis;

/**
 * A class that provides JavaScript access to {@link AngularVelocity}.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class AngularVelocityAccess extends Access {

  AngularVelocityAccess(BlocksOpMode blocksOpMode, String identifier) {
    super(blocksOpMode, identifier, "AngularVelocity");
  }

  private AngularVelocity checkAngularVelocity(Object angularVelocityArg) {
    return checkArg(angularVelocityArg, AngularVelocity.class, "angularVelocity");
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = AngularVelocity.class, fieldName = "unit")
  public String getAngleUnit(Object angularVelocityArg) {
    startBlockExecution(BlockType.GETTER, ".AngleUnit");
    AngularVelocity angularVelocity = checkAngularVelocity(angularVelocityArg);
    if (angularVelocity != null) {
      AngleUnit angleUnit = angularVelocity.unit;
      if (angleUnit != null) {
        return angleUnit.toString();
      }
    }
    return "";
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = AngularVelocity.class, fieldName = "xRotationRate")
  public float getXRotationRate(Object angularVelocityArg) {
    startBlockExecution(BlockType.GETTER, ".XRotationRate");
    AngularVelocity angularVelocity = checkAngularVelocity(angularVelocityArg);
    if (angularVelocity != null) {
      return angularVelocity.xRotationRate;
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = AngularVelocity.class, fieldName = "yRotationRate")
  public float getYRotationRate(Object angularVelocityArg) {
    startBlockExecution(BlockType.GETTER, ".YRotationRate");
    AngularVelocity angularVelocity = checkAngularVelocity(angularVelocityArg);
    if (angularVelocity != null) {
      return angularVelocity.yRotationRate;
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = AngularVelocity.class, fieldName = "zRotationRate")
  public float getZRotationRate(Object angularVelocityArg) {
    startBlockExecution(BlockType.GETTER, ".ZRotationRate");
    AngularVelocity angularVelocity = checkAngularVelocity(angularVelocityArg);
    if (angularVelocity != null) {
      return angularVelocity.zRotationRate;
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = AngularVelocity.class, fieldName = "acquisitionTime")
  public long getAcquisitionTime(Object angularVelocityArg) {
    startBlockExecution(BlockType.GETTER, ".AcquisitionTime");
    AngularVelocity angularVelocity = checkAngularVelocity(angularVelocityArg);
    if (angularVelocity != null) {
      return angularVelocity.acquisitionTime;
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = AngularVelocity.class, constructor = true)
  public AngularVelocity create() {
    startBlockExecution(BlockType.CREATE, "");
    return new AngularVelocity();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = AngularVelocity.class, constructor = true)
  public AngularVelocity create_withArgs(
      String angleUnitString, float xRotationRate, float yRotationRate,
      float zRotationRate, long acquisitionTime) {
    startBlockExecution(BlockType.CREATE, "");
    AngleUnit angleUnit = checkAngleUnit(angleUnitString);
    if (angleUnit != null) {
      return new AngularVelocity(
          angleUnit, xRotationRate, yRotationRate, zRotationRate, acquisitionTime);
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = AngularVelocity.class, methodName = "toAngleUnit")
  public AngularVelocity toAngleUnit(Object angularVelocityArg, String angleUnitString) {
    startBlockExecution(BlockType.FUNCTION, ".toAngleUnit");
    AngularVelocity angularVelocity = checkAngularVelocity(angularVelocityArg);
    AngleUnit angleUnit = checkAngleUnit(angleUnitString);
    if (angularVelocity != null && angleUnit != null) {
      return angularVelocity.toAngleUnit(angleUnit);
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = AngularVelocity.class, fieldName = {"xRotationRate", "yRotationRate", "zRotationRate"})
  public float getRotationRate(Object angularVelocityArg, String axisString) {
    startBlockExecution(BlockType.FUNCTION, ".getRotationRate");
    AngularVelocity angularVelocity = checkAngularVelocity(angularVelocityArg);
    Axis axis = checkArg(axisString, Axis.class, "axis");
    if (angularVelocity != null && axis != null) {
      switch (axis) {
        case X:
          return angularVelocity.xRotationRate;
        case Y:
          return angularVelocity.yRotationRate;
        case Z:
          return angularVelocity.zRotationRate;
        case UNKNOWN:
          reportInvalidArg("axis", "Axis.X, Axis.Y, or Axis.Z");
      }
    }
    return 0;
  }
}
