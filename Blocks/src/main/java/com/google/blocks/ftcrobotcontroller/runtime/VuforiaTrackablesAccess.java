// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import android.webkit.JavascriptInterface;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackable;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackables;

/**
 * A class that provides JavaScript access to {@link VuforiaTrackables}.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class VuforiaTrackablesAccess extends Access {

  VuforiaTrackablesAccess(BlocksOpMode blocksOpMode, String identifier) {
    super(blocksOpMode, identifier, "VuforiaTrackables");
  }

  private VuforiaTrackables checkVuforiaTrackables(
      Object vuforiaTrackablesArg) {
    return checkArg(vuforiaTrackablesArg, VuforiaTrackables.class, "vuforiaTrackables");
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public int getSize(Object vuforiaTrackablesArg) {
    startBlockExecution(BlockType.GETTER, ".Size");
    VuforiaTrackables vuforiaTrackables = checkVuforiaTrackables(vuforiaTrackablesArg);
    if (vuforiaTrackables != null) {
      return vuforiaTrackables.size();
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public String getName(Object vuforiaTrackablesArg) {
    startBlockExecution(BlockType.GETTER, ".Name");
    VuforiaTrackables vuforiaTrackables = checkVuforiaTrackables(vuforiaTrackablesArg);
    if (vuforiaTrackables != null) {
      String name = vuforiaTrackables.getName();
      if (name != null) {
        return name;
      }
    }
    return "";
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public VuforiaLocalizer getLocalizer(Object vuforiaTrackablesArg) {
    startBlockExecution(BlockType.GETTER, ".Localizer");
    VuforiaTrackables vuforiaTrackables = checkVuforiaTrackables(vuforiaTrackablesArg);
    if (vuforiaTrackables != null) {
      return vuforiaTrackables.getLocalizer();
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public VuforiaTrackable get(Object vuforiaTrackablesArg, int index) {
    startBlockExecution(BlockType.FUNCTION, ".get");
    VuforiaTrackables vuforiaTrackables = checkVuforiaTrackables(vuforiaTrackablesArg);
    if (vuforiaTrackables != null) {
      return vuforiaTrackables.get(index);
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void setName(Object vuforiaTrackablesArg, String name) {
    startBlockExecution(BlockType.FUNCTION, ".setName");
    VuforiaTrackables vuforiaTrackables = checkVuforiaTrackables(vuforiaTrackablesArg);
    if (vuforiaTrackables != null) {
      vuforiaTrackables.setName(name);
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void activate(Object vuforiaTrackablesArg) {
    startBlockExecution(BlockType.FUNCTION, ".activate");
    VuforiaTrackables vuforiaTrackables = checkVuforiaTrackables(vuforiaTrackablesArg);
    if (vuforiaTrackables != null) {
      vuforiaTrackables.activate();
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void deactivate(Object vuforiaTrackablesArg) {
    startBlockExecution(BlockType.FUNCTION, ".deactivate");
    VuforiaTrackables vuforiaTrackables = checkVuforiaTrackables(vuforiaTrackablesArg);
    if (vuforiaTrackables != null) {
      vuforiaTrackables.deactivate();
    }
  }
}
