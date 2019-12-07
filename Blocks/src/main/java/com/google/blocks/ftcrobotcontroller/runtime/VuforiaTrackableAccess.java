// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import android.webkit.JavascriptInterface;
import org.firstinspires.ftc.robotcore.external.matrices.OpenGLMatrix;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackable;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackables;

/**
 * A class that provides JavaScript access to {@link VuforiaTrackable}.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class VuforiaTrackableAccess extends Access {

  VuforiaTrackableAccess(BlocksOpMode blocksOpMode, String identifier) {
    super(blocksOpMode, identifier, "VuforiaTrackable");
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void setLocation(Object vuforiaTrackableArg, Object matrixArg) {
    startBlockExecution(BlockType.FUNCTION, ".setLocation");
    VuforiaTrackable vuforiaTrackable = checkVuforiaTrackable(vuforiaTrackableArg);
    OpenGLMatrix matrix = checkOpenGLMatrix(matrixArg);
    if (vuforiaTrackable != null && matrix != null) {
      vuforiaTrackable.setLocation(matrix);
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public OpenGLMatrix getLocation(Object vuforiaTrackableArg) {
    startBlockExecution(BlockType.GETTER, ".Location");
    VuforiaTrackable vuforiaTrackable = checkVuforiaTrackable(vuforiaTrackableArg);
    if (vuforiaTrackable != null) {
      return vuforiaTrackable.getLocation();
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void setUserData(Object vuforiaTrackableArg, Object userData) {
    startBlockExecution(BlockType.FUNCTION, ".setUserData");
    VuforiaTrackable vuforiaTrackable = checkVuforiaTrackable(vuforiaTrackableArg);
    if (vuforiaTrackable != null) {
      vuforiaTrackable.setUserData(userData);
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public Object getUserData(Object vuforiaTrackableArg) {
    startBlockExecution(BlockType.GETTER, ".UserData");
    VuforiaTrackable vuforiaTrackable = checkVuforiaTrackable(vuforiaTrackableArg);
    if (vuforiaTrackable != null) {
      return vuforiaTrackable.getUserData();
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public VuforiaTrackables getTrackables(Object vuforiaTrackableArg) {
    startBlockExecution(BlockType.GETTER, ".Trackables");
    VuforiaTrackable vuforiaTrackable = checkVuforiaTrackable(vuforiaTrackableArg);
    if (vuforiaTrackable != null) {
      return vuforiaTrackable.getTrackables();
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void setName(Object vuforiaTrackableArg, String name) {
    startBlockExecution(BlockType.FUNCTION, ".setName");
    VuforiaTrackable vuforiaTrackable = checkVuforiaTrackable(vuforiaTrackableArg);
    if (vuforiaTrackable != null) {
      vuforiaTrackable.setName(name);
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public String getName(Object vuforiaTrackableArg) {
    startBlockExecution(BlockType.GETTER, ".Name");
    VuforiaTrackable vuforiaTrackable = checkVuforiaTrackable(vuforiaTrackableArg);
    if (vuforiaTrackable != null) {
      String name = vuforiaTrackable.getName();
      if (name != null) {
        return name;
      }
    }
    return "";
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public VuforiaTrackable.Listener getListener(Object vuforiaTrackableArg) {
    startBlockExecution(BlockType.GETTER, ".Listener");
    VuforiaTrackable vuforiaTrackable = checkVuforiaTrackable(vuforiaTrackableArg);
    if (vuforiaTrackable != null) {
      return vuforiaTrackable.getListener();
    }
    return null;
  }
}
