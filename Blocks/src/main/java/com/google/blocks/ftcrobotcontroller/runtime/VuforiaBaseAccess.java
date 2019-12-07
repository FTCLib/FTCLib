// Copyright 2018 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import android.util.Pair;
import android.webkit.JavascriptInterface;
import com.qualcomm.robotcore.hardware.HardwareMap;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraName;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer.CameraDirection;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer.Parameters.CameraMonitorFeedback;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaBase;

/**
 * An abstract class for classes that provides JavaScript access to a {@link VuforiaBase}.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
abstract class VuforiaBaseAccess<T extends VuforiaBase> extends Access {
  private final HardwareMap hardwareMap;
  private T vuforiaBase;

  VuforiaBaseAccess(BlocksOpMode blocksOpMode, String identifier, HardwareMap hardwareMap) {
    super(blocksOpMode, identifier, "Vuforia");
    this.hardwareMap = hardwareMap;
  }

  private boolean checkAndSetVuforiaBase() {
    if (vuforiaBase != null) {
      reportWarning("Vuforia.initialize has already been called!");
      return false;
    }
    vuforiaBase = createVuforia();
    return true;
  }

  protected abstract T createVuforia();

  T getVuforiaBase() {
    if (vuforiaBase == null) {
      reportWarning("You forgot to call Vuforia.initialize!");
      return null;
    }
    return vuforiaBase;
  }

  // Access methods

  @Override
  void close() {
    if (vuforiaBase != null) {
      vuforiaBase.close();
      vuforiaBase = null;
    }
  }

  // Javascript methods

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void initialize_withCameraDirection(String vuforiaLicenseKey, String cameraDirectionString,
      boolean useExtendedTracking, boolean enableCameraMonitoring, String cameraMonitorFeedbackString,
      float dx, float dy, float dz, float xAngle, float yAngle, float zAngle,
      boolean useCompetitionFieldTargetLocations) {
    startBlockExecution(BlockType.FUNCTION, ".initialize");
    CameraDirection cameraDirection = checkVuforiaLocalizerCameraDirection(cameraDirectionString);
    Pair<Boolean, CameraMonitorFeedback> cameraMonitorFeedback =
        checkCameraMonitorFeedback(cameraMonitorFeedbackString);
    if (cameraDirection != null && cameraMonitorFeedback.first && checkAndSetVuforiaBase()) {
      try {
        vuforiaBase.initialize(vuforiaLicenseKey, cameraDirection,
            useExtendedTracking, enableCameraMonitoring, cameraMonitorFeedback.second,
            dx, dy, dz, xAngle, yAngle, zAngle, useCompetitionFieldTargetLocations);
      } catch (Exception e) {
        blocksOpMode.throwException(e);
      }
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void initialize_withWebcam(
      String cameraNameString, String webcamCalibrationFilename, boolean useExtendedTracking,
      boolean enableCameraMonitoring, String cameraMonitorFeedbackString,
      float dx, float dy, float dz, float xAngle, float yAngle, float zAngle,
      boolean useCompetitionFieldTargetLocations) {
    startBlockExecution(BlockType.FUNCTION, ".initialize");
    CameraName cameraName = checkCameraNameFromString(hardwareMap, cameraNameString);
    Pair<Boolean, CameraMonitorFeedback> cameraMonitorFeedback =
        checkCameraMonitorFeedback(cameraMonitorFeedbackString);
    if (cameraName != null && cameraMonitorFeedback.first && checkAndSetVuforiaBase()) {
      String vuforiaLicenseKey = "";
      try {
        vuforiaBase.initialize(vuforiaLicenseKey, cameraName, webcamCalibrationFilename,
            useExtendedTracking, enableCameraMonitoring, cameraMonitorFeedback.second,
            dx, dy, dz, xAngle, yAngle, zAngle, useCompetitionFieldTargetLocations);
      } catch (Exception e) {
        blocksOpMode.throwException(e);
      }
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void activate() {
    startBlockExecution(BlockType.FUNCTION, ".activate");
    try {
      vuforiaBase.activate();
    } catch (IllegalStateException e) {
      reportWarning(e.getMessage());
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void deactivate() {
    startBlockExecution(BlockType.FUNCTION, ".deactivate");
    try {
      vuforiaBase.deactivate();
    } catch (IllegalStateException e) {
      reportWarning(e.getMessage());
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public String track(String name) {
    startBlockExecution(BlockType.FUNCTION, ".track");
    try {
      return vuforiaBase.track(name).toJson();
    } catch (IllegalStateException e) {
      reportWarning(e.getMessage());
    } catch (IllegalArgumentException e) {
      reportInvalidArg("name", vuforiaBase.printTrackableNames());
    }
    return vuforiaBase.emptyTrackingResults(name).toJson();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public String trackPose(String name) {
    startBlockExecution(BlockType.FUNCTION, ".trackPose");
    try {
      return vuforiaBase.trackPose(name).toJson();
    } catch (IllegalStateException e) {
      reportWarning(e.getMessage());
    } catch (IllegalArgumentException e) {
      reportInvalidArg("name", vuforiaBase.printTrackableNames());
    }
    return vuforiaBase.emptyTrackingResults(name).toJson();
  }
}
