// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import android.webkit.JavascriptInterface;
import com.google.blocks.ftcrobotcontroller.hardware.HardwareItem;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.CRServoImpl;
import com.qualcomm.robotcore.hardware.DcMotorSimple.Direction;
import com.qualcomm.robotcore.hardware.HardwareMap;

/**
 * A class that provides JavaScript access to a {@link CRServo}.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class CRServoAccess extends HardwareAccess<CRServo> {
  private final CRServo crServo;

  CRServoAccess(BlocksOpMode blocksOpMode, HardwareItem hardwareItem, HardwareMap hardwareMap) {
    super(blocksOpMode, hardwareItem, hardwareMap, CRServo.class);
    this.crServo = hardwareDevice;
  }

  // From com.qualcomm.robotcore.hardware.CRServo

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {CRServoImpl.class}, methodName = "setDirection")
  public void setDirection(String directionString) {
    startBlockExecution(BlockType.SETTER, ".Direction");
    Direction direction = checkArg(directionString, Direction.class, "");
    if (direction != null) {
      crServo.setDirection(direction);
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {CRServoImpl.class}, methodName = "getDirection")
  public String getDirection() {
    startBlockExecution(BlockType.GETTER, ".Direction");
    Direction direction = crServo.getDirection();
    if (direction != null) {
      return direction.toString();
    }
    return "";
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {CRServoImpl.class}, methodName = "setPower")
  public void setPower(double power) {
    startBlockExecution(BlockType.SETTER, ".Power");
    crServo.setPower(power);
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {CRServoImpl.class}, methodName = "getPower")
  public double getPower() {
    startBlockExecution(BlockType.GETTER, ".Power");
    return crServo.getPower();
  }
}
