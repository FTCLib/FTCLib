// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import android.webkit.JavascriptInterface;
import com.google.blocks.ftcrobotcontroller.hardware.HardwareItem;
import com.qualcomm.hardware.adafruit.AdafruitI2cColorSensor;
import com.qualcomm.hardware.hitechnic.HiTechnicNxtColorSensor;
import com.qualcomm.hardware.modernrobotics.ModernRoboticsI2cColorSensor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.I2cAddr;
import com.qualcomm.robotcore.hardware.ColorSensor;
import com.qualcomm.robotcore.hardware.Light;
import com.qualcomm.robotcore.hardware.NormalizedColorSensor;
import com.qualcomm.robotcore.hardware.NormalizedRGBA;
import com.qualcomm.robotcore.hardware.SwitchableLight;

/**
 * A class that provides JavaScript access to a {@link ColorSensor}.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class ColorSensorAccess extends HardwareAccess<ColorSensor> {
  private final ColorSensor colorSensor;

  ColorSensorAccess(BlocksOpMode blocksOpMode, HardwareItem hardwareItem, HardwareMap hardwareMap) {
    super(blocksOpMode, hardwareItem, hardwareMap, ColorSensor.class);
    this.colorSensor = hardwareDevice;
  }

  // Properties

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {AdafruitI2cColorSensor.class, HiTechnicNxtColorSensor.class, ModernRoboticsI2cColorSensor.class},
      methodName = "red")
  public int getRed() {
    startBlockExecution(BlockType.GETTER, ".Red");
    return colorSensor.red();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {AdafruitI2cColorSensor.class, HiTechnicNxtColorSensor.class, ModernRoboticsI2cColorSensor.class},
      methodName = "green")
  public int getGreen() {
    startBlockExecution(BlockType.GETTER, ".Green");
    return colorSensor.green();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {AdafruitI2cColorSensor.class, HiTechnicNxtColorSensor.class, ModernRoboticsI2cColorSensor.class},
      methodName = "blue")
  public int getBlue() {
    startBlockExecution(BlockType.GETTER, ".Blue");
    return colorSensor.blue();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {AdafruitI2cColorSensor.class, HiTechnicNxtColorSensor.class, ModernRoboticsI2cColorSensor.class},
      methodName = "alpha")
  public int getAlpha() {
    startBlockExecution(BlockType.GETTER, ".Alpha");
    return colorSensor.alpha();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {AdafruitI2cColorSensor.class, HiTechnicNxtColorSensor.class, ModernRoboticsI2cColorSensor.class},
      methodName = "argb")
  public int getArgb() {
    startBlockExecution(BlockType.GETTER, ".Argb");
    return colorSensor.argb();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {AdafruitI2cColorSensor.class, HiTechnicNxtColorSensor.class, ModernRoboticsI2cColorSensor.class},
      methodName = "enableLed")
  public void enableLed(boolean enable) {
    startBlockExecution(BlockType.FUNCTION, ".enableLed");
    colorSensor.enableLed(enable);
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {HiTechnicNxtColorSensor.class, ModernRoboticsI2cColorSensor.class},
      methodName = "enableLight")
  public void enableLight(boolean enable) {
    startBlockExecution(BlockType.FUNCTION, ".enableLight");
    if (colorSensor instanceof SwitchableLight) {
      ((SwitchableLight) colorSensor).enableLight(enable);
    } else {
      reportWarning("This ColorSensor is not a SwitchableLight.");
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {AdafruitI2cColorSensor.class, HiTechnicNxtColorSensor.class, ModernRoboticsI2cColorSensor.class},
      methodName = "isLightOn")
  public boolean isLightOn() {
    startBlockExecution(BlockType.FUNCTION, ".isLightOn");
    if (colorSensor instanceof Light) {
      return ((Light) colorSensor).isLightOn();
    } else {
      reportWarning("This ColorSensor is not a Light.");
    }
    return false;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {AdafruitI2cColorSensor.class, HiTechnicNxtColorSensor.class, ModernRoboticsI2cColorSensor.class},
      methodName = "setI2cAddress")
  public void setI2cAddress7Bit(int i2cAddr7Bit) {
    startBlockExecution(BlockType.SETTER, ".I2cAddress7Bit");
    colorSensor.setI2cAddress(I2cAddr.create7bit(i2cAddr7Bit));
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {AdafruitI2cColorSensor.class, HiTechnicNxtColorSensor.class, ModernRoboticsI2cColorSensor.class},
      methodName = "getI2cAddress")
  public int getI2cAddress7Bit() {
    startBlockExecution(BlockType.GETTER, ".I2cAddress7Bit");
    I2cAddr i2cAddr = colorSensor.getI2cAddress();
    if (i2cAddr != null) {
      return i2cAddr.get7Bit();
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {AdafruitI2cColorSensor.class, HiTechnicNxtColorSensor.class, ModernRoboticsI2cColorSensor.class},
      methodName = "setI2cAddress")
  public void setI2cAddress8Bit(int i2cAddr8Bit) {
    startBlockExecution(BlockType.SETTER, ".I2cAddress8Bit");
    colorSensor.setI2cAddress(I2cAddr.create8bit(i2cAddr8Bit));
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {AdafruitI2cColorSensor.class, HiTechnicNxtColorSensor.class, ModernRoboticsI2cColorSensor.class},
      methodName = "getI2cAddress")
  public int getI2cAddress8Bit() {
    startBlockExecution(BlockType.GETTER, ".I2cAddress8Bit");
    I2cAddr i2cAddr = colorSensor.getI2cAddress();
    if (i2cAddr != null) {
      return i2cAddr.get8Bit();
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {AdafruitI2cColorSensor.class, HiTechnicNxtColorSensor.class, ModernRoboticsI2cColorSensor.class},
      methodName = "toString")
  public String toText() {
    startBlockExecution(BlockType.FUNCTION, ".toText");
    return colorSensor.toString();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {AdafruitI2cColorSensor.class, HiTechnicNxtColorSensor.class, ModernRoboticsI2cColorSensor.class},
      methodName = "getNormalizedColors")
  public String getNormalizedColors() {
    startBlockExecution(BlockType.FUNCTION, ".getNormalizedColors");
    if (colorSensor instanceof NormalizedColorSensor) {
      NormalizedRGBA color = ((NormalizedColorSensor) colorSensor).getNormalizedColors();
      return "{ \"Red\":" + color.red +
          ", \"Green\":" + color.green +
          ", \"Blue\":" + color.blue +
          ", \"Alpha\":" + color.alpha +
          ", \"Color\":" + color.toColor() + " }";
    }
    return "{ \"Red\":0" +
        ", \"Green\":0" +
        ", \"Blue\":0" +
        ", \"Alpha\":0" +
        ", \"Color\":0 }";
  }
}
