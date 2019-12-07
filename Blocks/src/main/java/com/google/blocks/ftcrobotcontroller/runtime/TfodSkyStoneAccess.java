// Copyright 2019 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import com.qualcomm.robotcore.hardware.HardwareMap;
import org.firstinspires.ftc.robotcore.external.tfod.TfodSkyStone;

/**
 * A class that provides JavaScript access to TensorFlow Object Detection for SKYSTONE (2019-2020).
 *
 * @author lizlooney@google.com (Liz Looney)
 */
final class TfodSkyStoneAccess extends TfodBaseAccess<TfodSkyStone> {
  TfodSkyStoneAccess(BlocksOpMode blocksOpMode, String identifier, HardwareMap hardwareMap) {
    super(blocksOpMode, identifier, hardwareMap);
  }

  protected TfodSkyStone createTfod() {
    return new TfodSkyStone();
  }
}
