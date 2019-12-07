// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import android.webkit.JavascriptInterface;
import org.firstinspires.ftc.robotcore.external.android.AndroidAccelerometer;
import org.firstinspires.ftc.robotcore.external.navigation.Acceleration;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

/**
 * A class that provides JavaScript access to the Android Accelerometer.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class AndroidAccelerometerAccess extends Access {
  private final AndroidAccelerometer androidAccelerometer;

  AndroidAccelerometerAccess(BlocksOpMode blocksOpMode, String identifier) {
    super(blocksOpMode, identifier, "AndroidAccelerometer");
    androidAccelerometer = new AndroidAccelerometer();
  }

  // Access methods

  @Override
  void close() {
    androidAccelerometer.stopListening();
  }

  // Javascript methods

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void setDistanceUnit(String distanceUnitString) {
    startBlockExecution(BlockType.SETTER, ".DistanceUnit");
    DistanceUnit distanceUnit = checkArg(distanceUnitString, DistanceUnit.class, "");
    if (distanceUnit != null) {
      androidAccelerometer.setDistanceUnit(distanceUnit);
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public double getX() {
    startBlockExecution(BlockType.GETTER, ".X");
    return androidAccelerometer.getX();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public double getY() {
    startBlockExecution(BlockType.GETTER, ".Y");
    return androidAccelerometer.getY();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public double getZ() {
    startBlockExecution(BlockType.GETTER, ".Z");
    return androidAccelerometer.getZ();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public Acceleration getAcceleration() {
    startBlockExecution(BlockType.GETTER, ".Acceleration");
    return androidAccelerometer.getAcceleration();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public String getDistanceUnit() {
    startBlockExecution(BlockType.GETTER, ".DistanceUnit");
    return androidAccelerometer.getDistanceUnit().toString();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public boolean isAvailable() {
    return androidAccelerometer.isAvailable();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void startListening() {
    startBlockExecution(BlockType.FUNCTION, ".startListening");
    androidAccelerometer.startListening();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void stopListening() {
    startBlockExecution(BlockType.FUNCTION, ".stopListening");
    androidAccelerometer.stopListening();
  }
}
