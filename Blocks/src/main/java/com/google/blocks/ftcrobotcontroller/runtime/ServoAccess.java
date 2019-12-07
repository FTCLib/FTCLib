// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import android.webkit.JavascriptInterface;
import com.google.blocks.ftcrobotcontroller.hardware.HardwareItem;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.Servo.Direction;

/**
 * A class that provides JavaScript access to a {@link Servo}.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class ServoAccess extends HardwareAccess<Servo> {
  private final Servo servo;

  ServoAccess(BlocksOpMode blocksOpMode, HardwareItem hardwareItem, HardwareMap hardwareMap) {
    super(blocksOpMode, hardwareItem, hardwareMap, Servo.class);
    this.servo = hardwareDevice;
  }

  // From com.qualcomm.robotcore.hardware.Servo

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void setDirection(String directionString) {
    startBlockExecution(BlockType.SETTER, ".Direction");
    Direction direction = checkArg(directionString, Direction.class, "");
    if (direction != null) {
      servo.setDirection(direction);
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public String getDirection() {
    startBlockExecution(BlockType.GETTER, ".Direction");
    Direction direction = servo.getDirection();
    if (direction != null) {
      return direction.toString();
    }
    return "";
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void setPosition(double position) {
    startBlockExecution(BlockType.SETTER, ".Position");
    servo.setPosition(position);
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public double getPosition() {
    startBlockExecution(BlockType.GETTER, ".Position");
    return servo.getPosition();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void scaleRange(double min, double max) {
    startBlockExecution(BlockType.FUNCTION, ".scaleRange");
    servo.scaleRange(min, max);
  }
}
