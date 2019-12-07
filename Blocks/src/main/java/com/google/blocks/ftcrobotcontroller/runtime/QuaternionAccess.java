// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import android.webkit.JavascriptInterface;
import org.firstinspires.ftc.robotcore.external.navigation.Quaternion;

/**
 * A class that provides JavaScript access to {@link Quaternion}.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class QuaternionAccess extends Access {

  QuaternionAccess(BlocksOpMode blocksOpMode, String identifier) {
    super(blocksOpMode, identifier, "Quaternion");
  }

  private Quaternion checkQuaternion(Object quaternionArg) {
    return checkArg(quaternionArg, Quaternion.class, "quaternion");
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public float getW(Object quaternionArg) {
    startBlockExecution(BlockType.GETTER, ".W");
    Quaternion quaternion = checkQuaternion(quaternionArg);
    if (quaternion != null) {
      return quaternion.w;
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public float getX(Object quaternionArg) {
    startBlockExecution(BlockType.GETTER, ".X");
    Quaternion quaternion = checkQuaternion(quaternionArg);
    if (quaternion != null) {
      return quaternion.x;
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public float getY(Object quaternionArg) {
    startBlockExecution(BlockType.GETTER, ".Y");
    Quaternion quaternion = checkQuaternion(quaternionArg);
    if (quaternion != null) {
      return quaternion.y;
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public float getZ(Object quaternionArg) {
    startBlockExecution(BlockType.GETTER, ".Z");
    Quaternion quaternion = checkQuaternion(quaternionArg);
    if (quaternion != null) {
      return quaternion.z;
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public long getAcquisitionTime(Object quaternionArg) {
    startBlockExecution(BlockType.GETTER, ".AcquisitionTime");
    Quaternion quaternion = checkQuaternion(quaternionArg);
    if (quaternion != null) {
      return quaternion.acquisitionTime;
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public float getMagnitude(Object quaternionArg) {
    startBlockExecution(BlockType.GETTER, ".Magnitude");
    Quaternion quaternion = checkQuaternion(quaternionArg);
    if (quaternion != null) {
      return quaternion.magnitude();
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public Quaternion create() {
    startBlockExecution(BlockType.CREATE, "");
    return new Quaternion();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public Quaternion create_withArgs(float w, float x, float y, float z, long acquisitionTime) {
    startBlockExecution(BlockType.CREATE, "");
    return new Quaternion(w, x, y, z, acquisitionTime);
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public Quaternion normalized(Object quaternionArg) {
    startBlockExecution(BlockType.FUNCTION, ".normalized");
    Quaternion quaternion = checkQuaternion(quaternionArg);
    if (quaternion != null) {
      return quaternion.normalized();
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public Quaternion congugate(Object quaternionArg) {
    startBlockExecution(BlockType.FUNCTION, ".congugate");
    Quaternion quaternion = checkQuaternion(quaternionArg);
    if (quaternion != null) {
      return quaternion.congugate();
    }
    return null;
  }
}
