// Copyright 2018 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import android.webkit.JavascriptInterface;
import com.qualcomm.robotcore.hardware.HardwareMap;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaRelicRecovery;

/**
 * A class that provides JavaScript access to Vuforia for Relic Recovery (2017-2018).
 *
 * @author lizlooney@google.com (Liz Looney)
 */
final class VuforiaRelicRecoveryAccess extends VuforiaBaseAccess<VuforiaRelicRecovery> {
  VuforiaRelicRecoveryAccess(BlocksOpMode blocksOpMode, String identifier, HardwareMap hardwareMap) {
    super(blocksOpMode, identifier, hardwareMap);
  }

  protected VuforiaRelicRecovery createVuforia() {
    return new VuforiaRelicRecovery();
  }

  // We no longer generate javascript code to call this method, but it remains for backwards
  // compatibility.
  @SuppressWarnings("unused")
  @JavascriptInterface
  public void initialize(String vuforiaLicenseKey,
      String cameraDirectionString, boolean enableCameraMonitoring, String cameraMonitorFeedbackString,
      float dx, float dy, float dz, float xAngle, float yAngle, float zAngle) {
    initialize_withCameraDirection(vuforiaLicenseKey, cameraDirectionString, true /* useExtendedTracking */,
        enableCameraMonitoring, cameraMonitorFeedbackString, dx, dy, dz, xAngle, yAngle, zAngle,
        true /* useCompetitionFieldTargetLocations */);
  }

  // We no longer generate javascript code to call this method, but it remains for backwards
  // compatibility.
  @SuppressWarnings("unused")
  @JavascriptInterface
  public void initializeExtended(String vuforiaLicenseKey, String cameraDirectionString,
      boolean useExtendedTracking, boolean enableCameraMonitoring, String cameraMonitorFeedbackString,
      float dx, float dy, float dz, float xAngle, float yAngle, float zAngle,
      boolean useCompetitionFieldTargetLocations) {
    initialize_withCameraDirection(vuforiaLicenseKey, cameraDirectionString,
        useExtendedTracking, enableCameraMonitoring, cameraMonitorFeedbackString,
        dx, dy, dz, xAngle, yAngle, zAngle,
        useCompetitionFieldTargetLocations);
  }
}
