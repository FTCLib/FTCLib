// Copyright 2018 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import android.webkit.JavascriptInterface;
import com.qualcomm.robotcore.hardware.HardwareMap;
import java.util.List;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaBase;
import org.firstinspires.ftc.robotcore.external.tfod.Recognition;
import org.firstinspires.ftc.robotcore.external.tfod.TfodBase;

/**
 * An abstract class for classes that provides JavaScript access to a {@link TfodBase}.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
abstract class TfodBaseAccess<T extends TfodBase> extends Access {
  private final HardwareMap hardwareMap;
  private T tfodBase;

  TfodBaseAccess(BlocksOpMode blocksOpMode, String identifier, HardwareMap hardwareMap) {
    super(blocksOpMode, identifier, "Tfod");
    this.hardwareMap = hardwareMap;
  }

  private boolean checkAndSetTfodBase() {
    if (tfodBase != null) {
      reportWarning("Tfod.initialize has already been called!");
      return false;
    }
    tfodBase = createTfod();
    return true;
  }

  protected abstract T createTfod();

  // Access methods

  @Override
  void close() {
    if (tfodBase != null) {
      tfodBase.close();
      tfodBase = null;
    }
  }

  // Javascript methods

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void initialize(VuforiaBaseAccess vuforiaBaseAccess, float minimumConfidence,
      boolean useObjectTracker, boolean enableCameraMonitoring) {
    startBlockExecution(BlockType.FUNCTION, ".initialize");
    VuforiaBase vuforiaBase = vuforiaBaseAccess.getVuforiaBase();
    if (checkAndSetTfodBase() && vuforiaBase != null) {
      try {
        tfodBase.initialize(vuforiaBase, minimumConfidence, useObjectTracker, enableCameraMonitoring);
      } catch (IllegalStateException e) {
        reportWarning(e.getMessage());
      }
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void activate() {
    startBlockExecution(BlockType.FUNCTION, ".activate");
    try {
      tfodBase.activate();
    } catch (IllegalStateException e) {
      reportWarning(e.getMessage());
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void deactivate() {
    startBlockExecution(BlockType.FUNCTION, ".deactivate");
    try {
      tfodBase.deactivate();
    } catch (IllegalStateException e) {
      reportWarning(e.getMessage());
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void setClippingMargins(int left, int top, int right, int bottom) {
    startBlockExecution(BlockType.FUNCTION, ".setClippingMargins");
    try {
      tfodBase.setClippingMargins(left, top, right, bottom);
    } catch (IllegalStateException e) {
      reportWarning(e.getMessage());
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public String getRecognitions() {
    startBlockExecution(BlockType.FUNCTION, ".getRecognitions");
    try {
      return toJson(tfodBase.getRecognitions());
    } catch (IllegalStateException e) {
      reportWarning(e.getMessage());
    }
    return "[]";
  }

  private static String toJson(List<Recognition> recognitions) {
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    String delimiter = "";
    for (Recognition recognition : recognitions) {
      sb.append(delimiter).append(toJson(recognition));
      delimiter = ",";
    }
    sb.append("]");
    return sb.toString();
  }

  private static String toJson(Recognition recognition) {
    return "{ \"Label\":\"" + recognition.getLabel() + "\"" +
        ", \"Confidence\":" + recognition.getConfidence() +
        ", \"Left\":" + recognition.getLeft() +
        ", \"Right\":" + recognition.getRight() +
        ", \"Top\":" + recognition.getTop() +
        ", \"Bottom\":" + recognition.getBottom() +
        ", \"Width\":" + recognition.getWidth() +
        ", \"Height\":" + recognition.getHeight() +
        ", \"ImageWidth\":" + recognition.getImageWidth() +
        ", \"ImageHeight\":" + recognition.getImageHeight() +
        ", \"estimateAngleToObject\":" + recognition.estimateAngleToObject(AngleUnit.RADIANS) +
        " }";
  }
}
