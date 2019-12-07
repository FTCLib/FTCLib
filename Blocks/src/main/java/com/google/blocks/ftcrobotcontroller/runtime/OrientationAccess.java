// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import android.webkit.JavascriptInterface;
import org.firstinspires.ftc.robotcore.external.matrices.MatrixF;
import org.firstinspires.ftc.robotcore.external.matrices.OpenGLMatrix;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;

/**
 * A class that provides JavaScript access to {@link Orientation}.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class OrientationAccess extends Access {

  OrientationAccess(BlocksOpMode blocksOpMode, String identifier) {
    super(blocksOpMode, identifier, "Orientation");
  }

  private Orientation checkOrientation(Object orientationArg) {
    return checkArg(orientationArg, Orientation.class, "orientation");
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public String getAxesReference(Object orientationArg) {
    startBlockExecution(BlockType.GETTER, ".AxesReference");
    Orientation orientation = checkOrientation(orientationArg);
    if (orientation != null) {
      AxesReference axesReference = orientation.axesReference;
      if (axesReference != null) {
        return axesReference.toString();
      }
    }
    return "";
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public String getAxesOrder(Object orientationArg) {
    startBlockExecution(BlockType.GETTER, ".AxesOrder");
    Orientation orientation = checkOrientation(orientationArg);
    if (orientation != null) {
      AxesOrder axesOrder = orientation.axesOrder;
      if (axesOrder != null) {
        return axesOrder.toString();
      }
    }
    return "";
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public String getAngleUnit(Object orientationArg) {
    startBlockExecution(BlockType.GETTER, ".AngleUnit");
    Orientation orientation = checkOrientation(orientationArg);
    if (orientation != null) {
      AngleUnit angleUnit = orientation.angleUnit;
      if (angleUnit != null) {
        return angleUnit.toString();
      }
    }
    return "";
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public float getFirstAngle(Object orientationArg) {
    startBlockExecution(BlockType.GETTER, ".FirstAngle");
    Orientation orientation = checkOrientation(orientationArg);
    if (orientation != null) {
      return orientation.firstAngle;
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public float getSecondAngle(Object orientationArg) {
    startBlockExecution(BlockType.GETTER, ".SecondAngle");
    Orientation orientation = checkOrientation(orientationArg);
    if (orientation != null) {
      return orientation.secondAngle;
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public float getThirdAngle(Object orientationArg) {
    startBlockExecution(BlockType.GETTER, ".ThirdAngle");
    Orientation orientation = checkOrientation(orientationArg);
    if (orientation != null) {
      return orientation.thirdAngle;
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public long getAcquisitionTime(Object orientationArg) {
    startBlockExecution(BlockType.GETTER, ".AcquisitionTime");
    Orientation orientation = checkOrientation(orientationArg);
    if (orientation != null) {
      return orientation.acquisitionTime;
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public Orientation create() {
    startBlockExecution(BlockType.CREATE, "");
    return new Orientation();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public Orientation create_withArgs(
      String axesReferenceString, String axesOrderString, String angleUnitString, float firstAngle,
      float secondAngle, float thirdAngle, long acquisitionTime) {
    startBlockExecution(BlockType.CREATE, "");
    AxesReference axesReference = checkAxesReference(axesReferenceString);
    AxesOrder axesOrder = checkAxesOrder(axesOrderString);
    AngleUnit angleUnit = checkAngleUnit(angleUnitString);
    if (axesReference != null && axesOrder != null && angleUnit != null) {
      return new Orientation(
          axesReference, axesOrder, angleUnit, firstAngle, secondAngle, thirdAngle,
          acquisitionTime);
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public Orientation toAngleUnit(Object orientationArg, String angleUnitString) {
    startBlockExecution(BlockType.FUNCTION, ".toAngleUnit");
    Orientation orientation = checkOrientation(orientationArg);
    AngleUnit angleUnit = checkAngleUnit(angleUnitString);
    if (orientation != null && angleUnit != null) {
      return orientation.toAngleUnit(angleUnit);
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public Orientation toAxesReference(Object orientationArg, String axesReferenceString) {
    startBlockExecution(BlockType.FUNCTION, ".toAxesReference");
    Orientation orientation = checkOrientation(orientationArg);
    AxesReference axesReference = checkAxesReference(axesReferenceString);
    if (orientation != null && axesReference != null) {
      return orientation.toAxesReference(axesReference);
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public Orientation toAxesOrder(Object orientationArg, String axesOrderString) {
    startBlockExecution(BlockType.FUNCTION, ".toAxesOrder");
    Orientation orientation = checkOrientation(orientationArg);
    AxesOrder axesOrder = checkAxesOrder(axesOrderString);
    if (orientation != null && axesOrder != null) {
      return orientation.toAxesOrder(axesOrder);
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public String toText(Object orientationArg) {
    startBlockExecution(BlockType.FUNCTION, ".toText");
    Orientation orientation = checkOrientation(orientationArg);
    if (orientation != null) {
      return orientation.toString();
    }
    return "";
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public OpenGLMatrix getRotationMatrix(Object orientationArg) {
    startBlockExecution(BlockType.FUNCTION, ".getRotationMatrix");
    Orientation orientation = checkOrientation(orientationArg);
    if (orientation != null) {
      return orientation.getRotationMatrix();
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public OpenGLMatrix getRotationMatrix_withArgs(
      String axesReferenceString, String axesOrderString, String angleUnitString, float firstAngle,
      float secondAngle, float thirdAngle) {
    startBlockExecution(BlockType.FUNCTION, ".getRotationMatrix");
    AxesReference axesReference = checkAxesReference(axesReferenceString);
    AxesOrder axesOrder = checkAxesOrder(axesOrderString);
    AngleUnit angleUnit = checkAngleUnit(angleUnitString);
    if (axesReference != null && axesOrder != null && angleUnit != null) {
      return Orientation.getRotationMatrix(
          axesReference, axesOrder, angleUnit, firstAngle, secondAngle, thirdAngle);
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public Orientation getOrientation(
      Object matrixArg, String axesReferenceString, String axesOrderString, String angleUnitString) {
    startBlockExecution(BlockType.FUNCTION, ".getOrientation");
    MatrixF matrix = checkMatrixF(matrixArg);
    AxesReference axesReference = checkAxesReference(axesReferenceString);
    AxesOrder axesOrder = checkAxesOrder(axesOrderString);
    AngleUnit angleUnit = checkAngleUnit(angleUnitString);
    if (matrix != null && axesReference != null && axesOrder != null && angleUnit != null) {
      return Orientation.getOrientation(matrix, axesReference, axesOrder, angleUnit);
    }
    return null;
  }
}
