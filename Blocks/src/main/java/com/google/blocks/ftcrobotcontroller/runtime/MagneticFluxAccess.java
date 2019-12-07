// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import android.webkit.JavascriptInterface;
import org.firstinspires.ftc.robotcore.external.navigation.MagneticFlux;

/**
 * A class that provides JavaScript access to {@link MagneticFlux}.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class MagneticFluxAccess extends Access {

  MagneticFluxAccess(BlocksOpMode blocksOpMode, String identifier) {
    super(blocksOpMode, identifier, "MagneticFlux");
  }

  private MagneticFlux checkMagneticFlux(Object magneticFluxArg) {
    return checkArg(magneticFluxArg, MagneticFlux.class, "magneticFlux");
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public double getX(Object magneticFluxArg) {
    startBlockExecution(BlockType.GETTER, ".X");
    MagneticFlux magneticFlux = checkMagneticFlux(magneticFluxArg);
    if (magneticFlux != null) {
      return magneticFlux.x;
    }
    return 0;
  }


  @SuppressWarnings("unused")
  @JavascriptInterface
  public double getY(Object magneticFluxArg) {
    startBlockExecution(BlockType.GETTER, ".Y");
    MagneticFlux magneticFlux = checkMagneticFlux(magneticFluxArg);
    if (magneticFlux != null) {
      return magneticFlux.y;
    }
    return 0;
  }


  @SuppressWarnings("unused")
  @JavascriptInterface
  public double getZ(Object magneticFluxArg) {
    startBlockExecution(BlockType.GETTER, ".Z");
    MagneticFlux magneticFlux = checkMagneticFlux(magneticFluxArg);
    if (magneticFlux != null) {
      return magneticFlux.z;
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public long getAcquisitionTime(Object magneticFluxArg) {
    startBlockExecution(BlockType.GETTER, ".AcquisitionTime");
    MagneticFlux magneticFlux = checkMagneticFlux(magneticFluxArg);
    if (magneticFlux != null) {
      return magneticFlux.acquisitionTime;
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public MagneticFlux create() {
    startBlockExecution(BlockType.CREATE, "");
    return new MagneticFlux();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public MagneticFlux create_withArgs(double x, double y, double z, long acquisitionTime) {
    startBlockExecution(BlockType.CREATE, "");
    return new MagneticFlux(x, y, z, acquisitionTime);
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public String toText(Object magneticFluxArg) {
    startBlockExecution(BlockType.FUNCTION, ".toText");
    MagneticFlux magneticFlux = checkMagneticFlux(magneticFluxArg);
    if (magneticFlux != null) {
      return magneticFlux.toString();
    }
    return "";
  }
}
