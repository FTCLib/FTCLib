// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import android.webkit.JavascriptInterface;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Velocity;

/**
 * A class that provides JavaScript access to {@link Velocity}.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class VelocityAccess extends Access {

  VelocityAccess(BlocksOpMode blocksOpMode, String identifier) {
    super(blocksOpMode, identifier, "Velocity");
  }

  private Velocity checkVelocity(Object velocityArg) {
    return checkArg(velocityArg, Velocity.class, "velocity");
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public String getDistanceUnit(Object velocityArg) {
    startBlockExecution(BlockType.GETTER, ".DistanceUnit");
    Velocity velocity = checkVelocity(velocityArg);
    if (velocity != null) {
      DistanceUnit distanceUnit = velocity.unit;
      if (distanceUnit != null) {
        return distanceUnit.toString();
      }
    }
    return "";
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public double getXVeloc(Object velocityArg) {
    startBlockExecution(BlockType.GETTER, ".XVeloc");
    Velocity velocity = checkVelocity(velocityArg);
    if (velocity != null) {
      return velocity.xVeloc;
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public double getYVeloc(Object velocityArg) {
    startBlockExecution(BlockType.GETTER, ".YVeloc");
    Velocity velocity = checkVelocity(velocityArg);
    if (velocity != null) {
      return velocity.yVeloc;
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public double getZVeloc(Object velocityArg) {
    startBlockExecution(BlockType.GETTER, ".ZVeloc");
    Velocity velocity = checkVelocity(velocityArg);
    if (velocity != null) {
      return velocity.zVeloc;
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public long getAcquisitionTime(Object velocityArg) {
    startBlockExecution(BlockType.GETTER, ".AcquisitionTime");
    Velocity velocity = checkVelocity(velocityArg);
    if (velocity != null) {
      return velocity.acquisitionTime;
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public Velocity create() {
    startBlockExecution(BlockType.CREATE, "");
    return new Velocity();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public Velocity create_withArgs(
      String distanceUnitString, double xVeloc, double yVeloc, double zVeloc,
      long acquisitionTime) {
    startBlockExecution(BlockType.CREATE, "");
    DistanceUnit distanceUnit = checkDistanceUnit(distanceUnitString);
    if (distanceUnit != null) {
      return new Velocity(distanceUnit, xVeloc, yVeloc, zVeloc, acquisitionTime);
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public Velocity toDistanceUnit(Object velocityArg, String distanceUnitString) {
    startBlockExecution(BlockType.FUNCTION, ".toDistanceUnit");
    Velocity velocity = checkVelocity(velocityArg);
    DistanceUnit distanceUnit = checkDistanceUnit(distanceUnitString);
    if (velocity != null && distanceUnit != null) {
      return velocity.toUnit(distanceUnit);
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public String toText(Object velocityArg) {
    startBlockExecution(BlockType.FUNCTION, ".toText");
    Velocity velocity = checkVelocity(velocityArg);
    if (velocity != null) {
      return velocity.toString();
    }
    return "";
  }
}
