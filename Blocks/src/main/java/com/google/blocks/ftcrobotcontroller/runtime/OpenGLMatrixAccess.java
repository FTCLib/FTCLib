// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import android.webkit.JavascriptInterface;
import org.firstinspires.ftc.robotcore.external.matrices.MatrixF;
import org.firstinspires.ftc.robotcore.external.matrices.OpenGLMatrix;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;

/**
 * A class that provides JavaScript access to {@link OpenGLMatrix}.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class OpenGLMatrixAccess extends Access {

  OpenGLMatrixAccess(BlocksOpMode blocksOpMode, String identifier) {
    super(blocksOpMode, identifier, "OpenGLMatrix");
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public OpenGLMatrix create() {
    startBlockExecution(BlockType.CREATE, "");
    return new OpenGLMatrix();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public OpenGLMatrix create_withMatrixF(Object matrixArg) {
    startBlockExecution(BlockType.CREATE, "");
    MatrixF matrix = checkMatrixF(matrixArg);
    if (matrix != null) {
      return new OpenGLMatrix(matrix);
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public OpenGLMatrix rotation(String angleUnitString, float angle, float dx, float dy, float dz) {
    startBlockExecution(BlockType.FUNCTION, ".rotation");
    AngleUnit angleUnit = checkAngleUnit(angleUnitString);
    if (angleUnit != null) {
      return OpenGLMatrix.rotation(angleUnit, angle, dx, dy, dz);
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public OpenGLMatrix rotation_withAxesArgs(
      String axesReferenceString, String axesOrderString, String angleUnitString,
      float firstAngle, float secondAngle, float thirdAngle) {
    startBlockExecution(BlockType.FUNCTION, ".rotation");
    AxesReference axesReference = checkAxesReference(axesReferenceString);
    AxesOrder axesOrder = checkAxesOrder(axesOrderString);
    AngleUnit angleUnit = checkAngleUnit(angleUnitString);
    if (axesReference != null && axesOrder != null && angleUnit != null) {
      return OpenGLMatrix.rotation(
          axesReference, axesOrder, angleUnit, firstAngle, secondAngle, thirdAngle);
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public OpenGLMatrix translation(float dx, float dy, float dz) {
    startBlockExecution(BlockType.FUNCTION, ".translation");
    return OpenGLMatrix.translation(dx, dy, dz);
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public OpenGLMatrix identityMatrix() {
    startBlockExecution(BlockType.FUNCTION, ".identityMatrix");
    return OpenGLMatrix.identityMatrix();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void scale_with3(Object matrixArg, float scaleX, float scaleY, float scaleZ) {
    startBlockExecution(BlockType.FUNCTION, ".scale");
    OpenGLMatrix matrix = checkOpenGLMatrix(matrixArg);
    if (matrix != null) {
      matrix.scale(scaleX, scaleY, scaleZ);
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void scale_with1(Object matrixArg, float scale) {
    startBlockExecution(BlockType.FUNCTION, ".scale");
    OpenGLMatrix matrix = checkOpenGLMatrix(matrixArg);
    if (matrix != null) {
      matrix.scale(scale);
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void translate(Object matrixArg, float dx, float dy, float dz) {
    startBlockExecution(BlockType.FUNCTION, ".translate");
    OpenGLMatrix matrix = checkOpenGLMatrix(matrixArg);
    if (matrix != null) {
      matrix.translate(dx, dy, dz);
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void rotate(Object matrixArg, String angleUnitString, float angle, float dx, float dy, float dz) {
    startBlockExecution(BlockType.FUNCTION, ".rotate");
    OpenGLMatrix matrix = checkOpenGLMatrix(matrixArg);
    AngleUnit angleUnit = checkAngleUnit(angleUnitString);
    if (matrix != null && angleUnit != null) {
      matrix.rotate(angleUnit, angle, dx, dy, dz);
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void rotate_withAxesArgs(
      Object matrixArg,
      String axesReferenceString, String axesOrderString, String angleUnitString,
      float firstAngle, float secondAngle, float thirdAngle) {
    startBlockExecution(BlockType.FUNCTION, ".rotate");
    OpenGLMatrix matrix = checkOpenGLMatrix(matrixArg);
    AxesReference axesReference = checkAxesReference(axesReferenceString);
    AxesOrder axesOrder = checkAxesOrder(axesOrderString);
    AngleUnit angleUnit = checkAngleUnit(angleUnitString);
    if (matrix != null && axesReference != null && axesOrder != null && angleUnit != null) {
      matrix.rotate(axesReference, axesOrder, angleUnit, firstAngle, secondAngle, thirdAngle);
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public OpenGLMatrix scaled_with3(Object matrixArg, float scaleX, float scaleY, float scaleZ) {
    startBlockExecution(BlockType.FUNCTION, ".scaled");
    OpenGLMatrix matrix = checkOpenGLMatrix(matrixArg);
    if (matrix != null) {
      return matrix.scaled(scaleX, scaleY, scaleZ);
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public OpenGLMatrix scaled_with1(Object matrixArg, float scale) {
    startBlockExecution(BlockType.FUNCTION, ".scaled");
    OpenGLMatrix matrix = checkOpenGLMatrix(matrixArg);
    if (matrix != null) {
      return matrix.scaled(scale);
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public OpenGLMatrix translated(Object matrixArg, float dx, float dy, float dz) {
    startBlockExecution(BlockType.FUNCTION, ".translated");
    OpenGLMatrix matrix = checkOpenGLMatrix(matrixArg);
    if (matrix != null) {
      return matrix.translated(dx, dy, dz);
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public OpenGLMatrix rotated(
      Object matrixArg, String angleUnitString, float angle, float dx, float dy, float dz) {
    startBlockExecution(BlockType.FUNCTION, ".rotated");
    OpenGLMatrix matrix = checkOpenGLMatrix(matrixArg);
    AngleUnit angleUnit = checkAngleUnit(angleUnitString);
    if (matrix != null && angleUnit != null) {
      return matrix.rotated(angleUnit, angle, dx, dy, dz);
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public OpenGLMatrix rotated_withAxesArgs(
      Object matrixArg, String axesReferenceString, String axesOrderString, String angleUnitString,
      float firstAngle, float secondAngle, float thirdAngle) {
    startBlockExecution(BlockType.FUNCTION, ".rotated");
    OpenGLMatrix matrix = checkOpenGLMatrix(matrixArg);
    AxesReference axesReference = checkAxesReference(axesReferenceString);
    AxesOrder axesOrder = checkAxesOrder(axesOrderString);
    AngleUnit angleUnit = checkAngleUnit(angleUnitString);
    if (matrix != null && axesReference != null && axesOrder != null && angleUnit != null) {
      return matrix.rotated(axesReference, axesOrder, angleUnit, firstAngle, secondAngle, thirdAngle);
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public OpenGLMatrix multiplied(Object matrix1Arg, Object matrix2Arg) {
    startBlockExecution(BlockType.FUNCTION, ".multiplied");
    OpenGLMatrix matrix1 = checkArg(matrix1Arg, OpenGLMatrix.class, "matrix1");
    OpenGLMatrix matrix2 = checkArg(matrix2Arg, OpenGLMatrix.class, "matrix2");
    if (matrix1 != null && matrix2 != null) {
      return matrix1.multiplied(matrix2);
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void multiply(Object matrix1Arg, Object matrix2Arg) {
    startBlockExecution(BlockType.FUNCTION, ".multiply");
    OpenGLMatrix matrix1 = checkArg(matrix1Arg, OpenGLMatrix.class, "matrix1");
    OpenGLMatrix matrix2 = checkArg(matrix2Arg, OpenGLMatrix.class, "matrix2");
    if (matrix1 != null && matrix2 != null) {
      matrix1.multiply(matrix2);
    }
  }
}
