// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import android.webkit.JavascriptInterface;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Position;

/**
 * A class that provides JavaScript access to {@link Position}.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class PositionAccess extends Access {

  PositionAccess(BlocksOpMode blocksOpMode, String identifier) {
    super(blocksOpMode, identifier, "Position");
  }

  private Position checkPosition(Object positionArg) {
    return checkArg(positionArg, Position.class, "position");
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public String getDistanceUnit(Object positionArg) {
    startBlockExecution(BlockType.GETTER, ".DistanceUnit");
    Position position = checkPosition(positionArg);
    if (position != null) {
      DistanceUnit distanceUnit = position.unit;
      if (distanceUnit != null) {
        return distanceUnit.toString();
      }
    }
    return "";
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public double getX(Object positionArg) {
    startBlockExecution(BlockType.GETTER, ".X");
    Position position = checkPosition(positionArg);
    if (position != null) {
      return position.x;
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public double getY(Object positionArg) {
    startBlockExecution(BlockType.GETTER, ".Y");
    Position position = checkPosition(positionArg);
    if (position != null) {
      return position.y;
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public double getZ(Object positionArg) {
    startBlockExecution(BlockType.GETTER, ".Z");
    Position position = checkPosition(positionArg);
    if (position != null) {
      return position.z;
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public long getAcquisitionTime(Object positionArg) {
    startBlockExecution(BlockType.GETTER, ".AcquisitionTime");
    Position position = checkPosition(positionArg);
    if (position != null) {
      return position.acquisitionTime;
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public Position create() {
    startBlockExecution(BlockType.CREATE, "");
    return new Position();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public Position create_withArgs(
      String distanceUnitString, double x, double y, double z, long acquisitionTime) {
    startBlockExecution(BlockType.CREATE, "");
    DistanceUnit distanceUnit = checkDistanceUnit(distanceUnitString);
    if (distanceUnit != null) {
      return new Position(distanceUnit, x, y, z, acquisitionTime);
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public Position toDistanceUnit(Object positionArg, String distanceUnitString) {
    startBlockExecution(BlockType.FUNCTION, ".toDistanceUnit");
    Position position = checkPosition(positionArg);
    DistanceUnit distanceUnit = checkDistanceUnit(distanceUnitString);
    if (position != null && distanceUnit != null) {
      return position.toUnit(distanceUnit);
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public String toText(Object positionArg) {
    startBlockExecution(BlockType.FUNCTION, ".toText");
    Position position = checkPosition(positionArg);
    if (position != null) {
      return position.toString();
    }
    return "";
  }
}
