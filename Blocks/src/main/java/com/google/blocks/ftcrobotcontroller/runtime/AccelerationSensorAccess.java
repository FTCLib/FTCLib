// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import android.webkit.JavascriptInterface;
import com.google.blocks.ftcrobotcontroller.hardware.HardwareItem;
import com.qualcomm.hardware.hitechnic.HiTechnicNxtAccelerationSensor;
import com.qualcomm.robotcore.hardware.AccelerationSensor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import org.firstinspires.ftc.robotcore.external.navigation.Acceleration;

/**
 * A class that provides JavaScript access to a {@link AccelerationSensor}.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class AccelerationSensorAccess extends HardwareAccess<AccelerationSensor> {
  private final AccelerationSensor accelerationSensor;

  AccelerationSensorAccess(BlocksOpMode blocksOpMode, HardwareItem hardwareItem, HardwareMap hardwareMap) {
    super(blocksOpMode, hardwareItem, hardwareMap, AccelerationSensor.class);
    this.accelerationSensor = hardwareDevice;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(exclusiveToBlocks = true)
  public double getXAccel() {
    startBlockExecution(BlockType.GETTER, ".XAccel");
    Acceleration acceleration = accelerationSensor.getAcceleration();
    if (acceleration != null) {
      return acceleration.xAccel;
    }
    return 0.0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(exclusiveToBlocks = true)
  public double getYAccel() {
    startBlockExecution(BlockType.GETTER, ".YAccel");
    Acceleration acceleration = accelerationSensor.getAcceleration();
    if (acceleration != null) {
      return acceleration.yAccel;
    }
    return 0.0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(exclusiveToBlocks = true)
  public double getZAccel() {
    startBlockExecution(BlockType.GETTER, ".ZAccel");
    Acceleration acceleration = accelerationSensor.getAcceleration();
    if (acceleration != null) {
      return acceleration.zAccel;
    }
    return 0.0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {HiTechnicNxtAccelerationSensor.class}, methodName = "getAcceleration")
  public Acceleration getAcceleration() {
    startBlockExecution(BlockType.GETTER, ".Acceleration");
    return accelerationSensor.getAcceleration();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {HiTechnicNxtAccelerationSensor.class}, methodName = "toString")
  public String toText() {
    startBlockExecution(BlockType.FUNCTION, ".toText");
    return accelerationSensor.toString();
  }
}
