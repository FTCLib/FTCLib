// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import android.webkit.JavascriptInterface;
import com.qualcomm.robotcore.hardware.HardwareMap;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraName;
import org.firstinspires.ftc.robotcore.external.matrices.OpenGLMatrix;
import org.firstinspires.ftc.robotcore.external.navigation.RelicRecoveryVuMark;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer.CameraDirection;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackableDefaultListener;

/**
 * A class that provides JavaScript access to {@link VuforiaTrackableDefaultListener}.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class VuforiaTrackableDefaultListenerAccess extends Access {
  private final HardwareMap hardwareMap;

  VuforiaTrackableDefaultListenerAccess(BlocksOpMode blocksOpMode, String identifier, HardwareMap hardwareMap) {
    super(blocksOpMode, identifier, "VuforiaTrackableDefaultListener");
    this.hardwareMap = hardwareMap;
  }

  private VuforiaTrackableDefaultListener checkVuforiaTrackableDefaultListener(
      Object vuforiaTrackableDefaultListenerArg) {
    return checkArg(vuforiaTrackableDefaultListenerArg, VuforiaTrackableDefaultListener.class,
        "vuforiaTrackableDefaultListener");
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void setPhoneInformation(
      Object vuforiaTrackableDefaultListenerArg, Object phoneLocationOnRobotArg,
      String cameraDirectionString) {
    startBlockExecution(BlockType.FUNCTION, ".setPhoneInformation");
    VuforiaTrackableDefaultListener vuforiaTrackableDefaultListener = checkVuforiaTrackableDefaultListener(
        vuforiaTrackableDefaultListenerArg);
    OpenGLMatrix phoneLocationOnRobot = checkOpenGLMatrix(phoneLocationOnRobotArg);
    CameraDirection cameraDirection = checkVuforiaLocalizerCameraDirection(cameraDirectionString);
    if (vuforiaTrackableDefaultListener != null && phoneLocationOnRobot != null && cameraDirection != null) {
      vuforiaTrackableDefaultListener.setPhoneInformation(phoneLocationOnRobot, cameraDirection);
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void setCameraLocationOnRobot(
      Object vuforiaTrackableDefaultListenerArg, String cameraNameString, Object cameraLocationOnRobotArg) {
    startBlockExecution(BlockType.FUNCTION, ".setCameraLocationOnRobot");
    VuforiaTrackableDefaultListener vuforiaTrackableDefaultListener = checkVuforiaTrackableDefaultListener(
        vuforiaTrackableDefaultListenerArg);
    CameraName cameraName = checkCameraNameFromString(hardwareMap, cameraNameString);
    OpenGLMatrix cameraLocationOnRobot = checkOpenGLMatrix(cameraLocationOnRobotArg);
    if (vuforiaTrackableDefaultListener != null && cameraName != null && cameraLocationOnRobot != null) {
      vuforiaTrackableDefaultListener.setCameraLocationOnRobot(cameraName, cameraLocationOnRobot);
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public boolean isVisible(Object vuforiaTrackableDefaultListenerArg) {
    startBlockExecution(BlockType.FUNCTION, ".isVisible");
    VuforiaTrackableDefaultListener vuforiaTrackableDefaultListener = checkVuforiaTrackableDefaultListener(
        vuforiaTrackableDefaultListenerArg);
    if (vuforiaTrackableDefaultListener != null) {
      return vuforiaTrackableDefaultListener.isVisible();
    }
    return false;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public OpenGLMatrix getUpdatedRobotLocation(Object vuforiaTrackableDefaultListenerArg) {
    startBlockExecution(BlockType.FUNCTION, ".getUpdatedRobotLocation");
    VuforiaTrackableDefaultListener vuforiaTrackableDefaultListener = checkVuforiaTrackableDefaultListener(
        vuforiaTrackableDefaultListenerArg);
    if (vuforiaTrackableDefaultListener != null) {
      return vuforiaTrackableDefaultListener.getUpdatedRobotLocation();
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public OpenGLMatrix getPose(Object vuforiaTrackableDefaultListenerArg) {
    startBlockExecution(BlockType.FUNCTION, ".getPose");
    VuforiaTrackableDefaultListener vuforiaTrackableDefaultListener = checkVuforiaTrackableDefaultListener(
        vuforiaTrackableDefaultListenerArg);
    if (vuforiaTrackableDefaultListener != null) {
      return vuforiaTrackableDefaultListener.getPose();
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public String getRelicRecoveryVuMark(Object vuforiaTrackableDefaultListenerArg) {
    startBlockExecution(BlockType.FUNCTION, ".getRelicRecoveryVuMark");
    VuforiaTrackableDefaultListener vuforiaTrackableDefaultListener = checkVuforiaTrackableDefaultListener(
        vuforiaTrackableDefaultListenerArg);
    if (vuforiaTrackableDefaultListener != null) {
      return RelicRecoveryVuMark.from(vuforiaTrackableDefaultListener).toString();
    }
    return RelicRecoveryVuMark.UNKNOWN.toString();
  }
}
