// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import android.webkit.JavascriptInterface;
import org.firstinspires.ftc.robotcore.external.android.AndroidGyroscope;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AngularVelocity;

/**
 * A class that provides JavaScript access to the Android Gyroscope.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class AndroidGyroscopeAccess extends Access {
  private final AndroidGyroscope androidGyroscope;

  AndroidGyroscopeAccess(BlocksOpMode blocksOpMode, String identifier) {
    super(blocksOpMode, identifier, "AndroidGyroscope");
    androidGyroscope = new AndroidGyroscope();
  }

  // Access methods

  @Override
  void close() {
    androidGyroscope.stopListening();
  }

  // Javascript methods

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void setAngleUnit(String angleUnitString) {
    startBlockExecution(BlockType.SETTER, ".AngleUnit");
    AngleUnit angleUnit = checkArg(angleUnitString, AngleUnit.class, "");
    if (angleUnit != null) {
      androidGyroscope.setAngleUnit(angleUnit);
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public float getX() {
    startBlockExecution(BlockType.GETTER, ".X");
    return androidGyroscope.getX();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public float getY() {
    startBlockExecution(BlockType.GETTER, ".Y");
    return androidGyroscope.getY();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public float getZ() {
    startBlockExecution(BlockType.GETTER, ".Z");
    return androidGyroscope.getZ();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public AngularVelocity getAngularVelocity() {
    startBlockExecution(BlockType.GETTER, ".AngularVelocity");
    return androidGyroscope.getAngularVelocity();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public String getAngleUnit() {
    startBlockExecution(BlockType.GETTER, ".AngleUnit");
    return androidGyroscope.getAngleUnit().toString();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public boolean isAvailable() {
    startBlockExecution(BlockType.FUNCTION, ".isAvailable");
    return androidGyroscope.isAvailable();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void startListening() {
    startBlockExecution(BlockType.FUNCTION, ".startListening");
    androidGyroscope.startListening();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void stopListening() {
    startBlockExecution(BlockType.FUNCTION, ".stopListening");
    androidGyroscope.stopListening();
  }
}
