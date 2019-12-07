// Copyright 2018 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import com.qualcomm.robotcore.hardware.HardwareMap;
import org.firstinspires.ftc.robotcore.external.tfod.TfodRoverRuckus;

/**
 * A class that provides JavaScript access to TensorFlow Object Detection for Rover Ruckus (2018-2019).
 *
 * @author lizlooney@google.com (Liz Looney)
 */
final class TfodRoverRuckusAccess extends TfodBaseAccess<TfodRoverRuckus> {
  TfodRoverRuckusAccess(BlocksOpMode blocksOpMode, String identifier, HardwareMap hardwareMap) {
    super(blocksOpMode, identifier, hardwareMap);
  }

  protected TfodRoverRuckus createTfod() {
    return new TfodRoverRuckus();
  }
}
