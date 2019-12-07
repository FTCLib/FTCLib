// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import android.webkit.JavascriptInterface;
import com.google.blocks.ftcrobotcontroller.hardware.HardwareItem;
import com.qualcomm.hardware.hitechnic.HiTechnicNxtIrSeekerSensor;
import com.qualcomm.hardware.modernrobotics.ModernRoboticsI2cIrSeekerSensorV3;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.I2cAddr;
import com.qualcomm.robotcore.hardware.IrSeekerSensor;
import com.qualcomm.robotcore.hardware.IrSeekerSensor.Mode;

/**
 * A class that provides JavaScript access to a {@link IrSeekerSensor}.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class IrSeekerSensorAccess extends HardwareAccess<IrSeekerSensor> {
  private final IrSeekerSensor irSeekerSensor;

  IrSeekerSensorAccess(BlocksOpMode blocksOpMode, HardwareItem hardwareItem, HardwareMap hardwareMap) {
    super(blocksOpMode, hardwareItem, hardwareMap, IrSeekerSensor.class);
    this.irSeekerSensor = hardwareDevice;
  }

  // Properties

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {HiTechnicNxtIrSeekerSensor.class, ModernRoboticsI2cIrSeekerSensorV3.class}, methodName = "setSignalDetectedThreshold")
  public void setSignalDetectedThreshold(double threshold) {
    startBlockExecution(BlockType.SETTER, ".SignalDetectedThreshold");
    irSeekerSensor.setSignalDetectedThreshold(threshold);
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {HiTechnicNxtIrSeekerSensor.class, ModernRoboticsI2cIrSeekerSensorV3.class}, methodName = "getSignalDetectedThreshold")
  public double getSignalDetectedThreshold() {
    startBlockExecution(BlockType.GETTER, ".SignalDetectedThreshold");
    return irSeekerSensor.getSignalDetectedThreshold();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {HiTechnicNxtIrSeekerSensor.class, ModernRoboticsI2cIrSeekerSensorV3.class}, methodName = "setMode")
  public void setMode(String modeString) {
    startBlockExecution(BlockType.SETTER, ".Mode");
    Mode mode = checkArg(modeString, Mode.class, "");
    if (mode != null) {
      irSeekerSensor.setMode(mode);
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {HiTechnicNxtIrSeekerSensor.class, ModernRoboticsI2cIrSeekerSensorV3.class}, methodName = "getMode")
  public String getMode() {
    startBlockExecution(BlockType.GETTER, ".Mode");
    Mode mode = irSeekerSensor.getMode();
    if (mode != null) {
      return mode.toString();
    }
    return "";
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {HiTechnicNxtIrSeekerSensor.class, ModernRoboticsI2cIrSeekerSensorV3.class}, methodName = "signalDetected")
  public boolean getIsSignalDetected() {
    startBlockExecution(BlockType.GETTER, ".IsSignalDetected");
    return irSeekerSensor.signalDetected();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {HiTechnicNxtIrSeekerSensor.class, ModernRoboticsI2cIrSeekerSensorV3.class}, methodName = "getAngle")
  public double getAngle() {
    startBlockExecution(BlockType.GETTER, ".Angle");
    return irSeekerSensor.getAngle();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {HiTechnicNxtIrSeekerSensor.class, ModernRoboticsI2cIrSeekerSensorV3.class}, methodName = "getStrength")
  public double getStrength() {
    startBlockExecution(BlockType.GETTER, ".Strength");
    return irSeekerSensor.getStrength();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {HiTechnicNxtIrSeekerSensor.class, ModernRoboticsI2cIrSeekerSensorV3.class}, methodName = "setI2cAddress")
  public void setI2cAddress7Bit(int i2cAddr7Bit) {
    startBlockExecution(BlockType.SETTER, ".I2cAddress7Bit");
    irSeekerSensor.setI2cAddress(I2cAddr.create7bit(i2cAddr7Bit));
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {HiTechnicNxtIrSeekerSensor.class, ModernRoboticsI2cIrSeekerSensorV3.class}, methodName = "getI2cAddress")
  public int getI2cAddress7Bit() {
    startBlockExecution(BlockType.GETTER, ".I2cAddress7Bit");
    I2cAddr i2cAddr = irSeekerSensor.getI2cAddress();
    if (i2cAddr != null) {
      return i2cAddr.get7Bit();
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {HiTechnicNxtIrSeekerSensor.class, ModernRoboticsI2cIrSeekerSensorV3.class}, methodName = "setI2cAddress")
  public void setI2cAddress8Bit(int i2cAddr8Bit) {
    startBlockExecution(BlockType.SETTER, ".I2cAddress8Bit");
    irSeekerSensor.setI2cAddress(I2cAddr.create8bit(i2cAddr8Bit));
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {HiTechnicNxtIrSeekerSensor.class, ModernRoboticsI2cIrSeekerSensorV3.class}, methodName = "getI2cAddress")
  public int getI2cAddress8Bit() {
    startBlockExecution(BlockType.GETTER, ".I2cAddress8Bit");
    I2cAddr i2cAddr = irSeekerSensor.getI2cAddress();
    if (i2cAddr != null) {
      return i2cAddr.get8Bit();
    }
    return 0;
  }
}
