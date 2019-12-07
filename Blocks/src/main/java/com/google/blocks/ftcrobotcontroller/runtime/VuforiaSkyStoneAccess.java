// Copyright 2019 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import com.qualcomm.robotcore.hardware.HardwareMap;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaSkyStone;

/**
 * A class that provides JavaScript access to Vuforia for SKYSTONE (2019-2020).
 *
 * @author lizlooney@google.com (Liz Looney)
 */
final class VuforiaSkyStoneAccess extends VuforiaBaseAccess<VuforiaSkyStone> {
  VuforiaSkyStoneAccess(BlocksOpMode blocksOpMode, String identifier, HardwareMap hardwareMap) {
    super(blocksOpMode, identifier, hardwareMap);
  }

  protected VuforiaSkyStone createVuforia() {
    return new VuforiaSkyStone();
  }
}
