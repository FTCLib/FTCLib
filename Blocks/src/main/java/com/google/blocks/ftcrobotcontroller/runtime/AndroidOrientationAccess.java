// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import android.webkit.JavascriptInterface;
import org.firstinspires.ftc.robotcore.external.android.AndroidOrientation;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

/**
 * A class that provides JavaScript access to the Android sensors for Orientation.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class AndroidOrientationAccess extends Access {
  private final AndroidOrientation androidOrientation;

  AndroidOrientationAccess(BlocksOpMode blocksOpMode, String identifier) {
    super(blocksOpMode, identifier, "AndroidOrientation");
    androidOrientation = new AndroidOrientation();
  }

  // Access methods

  @Override
  void close() {
    androidOrientation.stopListening();
  }

  // Javascript methods

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void setAngleUnit(String angleUnitString) {
    startBlockExecution(BlockType.SETTER, ".AngleUnit");
    AngleUnit angleUnit = checkArg(angleUnitString, AngleUnit.class, "");
    if (angleUnit != null) {
      androidOrientation.setAngleUnit(angleUnit);
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public double getAzimuth() {
    startBlockExecution(BlockType.GETTER, ".Azimuth");
    return androidOrientation.getAzimuth();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public double getPitch() {
    startBlockExecution(BlockType.GETTER, ".Pitch");
    return androidOrientation.getPitch();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public double getRoll() {
    startBlockExecution(BlockType.GETTER, ".Roll");
    return androidOrientation.getRoll();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public double getAngle() {
    startBlockExecution(BlockType.GETTER, ".Angle");
    return androidOrientation.getAngle();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public double getMagnitude() {
    startBlockExecution(BlockType.GETTER, ".Magnitude");
    return androidOrientation.getMagnitude();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public String getAngleUnit() {
    startBlockExecution(BlockType.GETTER, ".AngleUnit");
    return androidOrientation.getAngleUnit().toString();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public boolean isAvailable() {
    startBlockExecution(BlockType.FUNCTION, ".isAvailable");
    return androidOrientation.isAvailable();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void startListening() {
    startBlockExecution(BlockType.FUNCTION, ".startListening");
    androidOrientation.startListening();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void stopListening() {
    startBlockExecution(BlockType.FUNCTION, ".stopListening");
    androidOrientation.stopListening();
  }
}
