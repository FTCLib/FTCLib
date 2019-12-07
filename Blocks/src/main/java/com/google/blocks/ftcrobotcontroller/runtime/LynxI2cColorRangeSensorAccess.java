// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import android.webkit.JavascriptInterface;
import com.google.blocks.ftcrobotcontroller.hardware.HardwareItem;
import com.qualcomm.hardware.lynx.LynxI2cColorRangeSensor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.I2cAddr;
import com.qualcomm.robotcore.hardware.NormalizedRGBA;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

/**
 * A class that provides JavaScript access to a {@link LynxI2cColorRangeSensor}.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class LynxI2cColorRangeSensorAccess extends HardwareAccess<LynxI2cColorRangeSensor> {
  private final LynxI2cColorRangeSensor lynxI2cColorRangeSensor;

  LynxI2cColorRangeSensorAccess(BlocksOpMode blocksOpMode, HardwareItem hardwareItem, HardwareMap hardwareMap) {
    super(blocksOpMode, hardwareItem, hardwareMap, LynxI2cColorRangeSensor.class);
    this.lynxI2cColorRangeSensor = hardwareDevice;
  }

  // Properties

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {LynxI2cColorRangeSensor.class}, methodName = "red")
  public int getRed() {
    startBlockExecution(BlockType.GETTER, ".Red");
    return lynxI2cColorRangeSensor.red();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {LynxI2cColorRangeSensor.class}, methodName = "green")
  public int getGreen() {
    startBlockExecution(BlockType.GETTER, ".Green");
    return lynxI2cColorRangeSensor.green();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {LynxI2cColorRangeSensor.class}, methodName = "blue")
  public int getBlue() {
    startBlockExecution(BlockType.GETTER, ".Blue");
    return lynxI2cColorRangeSensor.blue();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {LynxI2cColorRangeSensor.class}, methodName = "alpha")
  public int getAlpha() {
    startBlockExecution(BlockType.GETTER, ".Alpha");
    return lynxI2cColorRangeSensor.alpha();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {LynxI2cColorRangeSensor.class}, methodName = "argb")
  public int getArgb() {
    startBlockExecution(BlockType.GETTER, ".Argb");
    return lynxI2cColorRangeSensor.argb();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {LynxI2cColorRangeSensor.class}, methodName = "setI2cAddress")
  public void setI2cAddress7Bit(int i2cAddr7Bit) {
    startBlockExecution(BlockType.SETTER, ".I2cAddress7Bit");
    lynxI2cColorRangeSensor.setI2cAddress(I2cAddr.create7bit(i2cAddr7Bit));
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {LynxI2cColorRangeSensor.class}, methodName = "getI2cAddress")
  public int getI2cAddress7Bit() {
    startBlockExecution(BlockType.GETTER, ".I2cAddress7Bit");
    I2cAddr i2cAddr = lynxI2cColorRangeSensor.getI2cAddress();
    if (i2cAddr != null) {
      return i2cAddr.get7Bit();
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {LynxI2cColorRangeSensor.class}, methodName = "setI2cAddress")
  public void setI2cAddress8Bit(int i2cAddr8Bit) {
    startBlockExecution(BlockType.SETTER, ".I2cAddress8Bit");
    lynxI2cColorRangeSensor.setI2cAddress(I2cAddr.create8bit(i2cAddr8Bit));
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {LynxI2cColorRangeSensor.class}, methodName = "getI2cAddress")
  public int getI2cAddress8Bit() {
    startBlockExecution(BlockType.GETTER, ".I2cAddress8Bit");
    I2cAddr i2cAddr = lynxI2cColorRangeSensor.getI2cAddress();
    if (i2cAddr != null) {
      return i2cAddr.get8Bit();
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {LynxI2cColorRangeSensor.class}, methodName = "getLightDetected")
  public double getLightDetected() {
    startBlockExecution(BlockType.GETTER, ".LightDetected");
    return lynxI2cColorRangeSensor.getLightDetected();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {LynxI2cColorRangeSensor.class}, methodName = "getRawLightDetected")
  public double getRawLightDetected() {
    startBlockExecution(BlockType.GETTER, ".RawLightDetected");
    return lynxI2cColorRangeSensor.getRawLightDetected();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {LynxI2cColorRangeSensor.class}, methodName = "getRawLightDetectedMax")
  public double getRawLightDetectedMax() {
    startBlockExecution(BlockType.GETTER, ".RawLightDetectedMax");
    return lynxI2cColorRangeSensor.getRawLightDetectedMax();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {LynxI2cColorRangeSensor.class}, methodName = "getDistance")
  public double getDistance(String distanceUnitString) {
    startBlockExecution(BlockType.FUNCTION, ".getDistance");
    DistanceUnit distanceUnit = checkArg(distanceUnitString, DistanceUnit.class, "unit");
    if (distanceUnit != null) {
      return lynxI2cColorRangeSensor.getDistance(distanceUnit);
    }
    return 0.0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {LynxI2cColorRangeSensor.class}, methodName = "getNormalizedColors")
  public String getNormalizedColors() {
    startBlockExecution(BlockType.FUNCTION, ".getNormalizedColors");
    NormalizedRGBA color = lynxI2cColorRangeSensor.getNormalizedColors();
    return "{ \"Red\":" + color.red +
        ", \"Green\":" + color.green +
        ", \"Blue\":" + color.blue +
        ", \"Alpha\":" + color.alpha +
        ", \"Color\":" + color.toColor() + " }";
  }
}
