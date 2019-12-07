// Copyright 2018 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import com.qualcomm.robotcore.hardware.HardwareMap;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaRoverRuckus;

/**
 * A class that provides JavaScript access to Vuforia for Rover Ruckus (2018-2019).
 *
 * @author lizlooney@google.com (Liz Looney)
 */
final class VuforiaRoverRuckusAccess extends VuforiaBaseAccess<VuforiaRoverRuckus> {
  VuforiaRoverRuckusAccess(BlocksOpMode blocksOpMode, String identifier, HardwareMap hardwareMap) {
    super(blocksOpMode, identifier, hardwareMap);
  }

  protected VuforiaRoverRuckus createVuforia() {
    return new VuforiaRoverRuckus();
  }
}
