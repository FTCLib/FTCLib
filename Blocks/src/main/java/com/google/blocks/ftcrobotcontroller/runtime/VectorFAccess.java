// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import android.webkit.JavascriptInterface;
import org.firstinspires.ftc.robotcore.external.matrices.MatrixF;
import org.firstinspires.ftc.robotcore.external.matrices.VectorF;

/**
 * A class that provides JavaScript access to {@link VectorF}.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class VectorFAccess extends Access {

  VectorFAccess(BlocksOpMode blocksOpMode, String identifier) {
    super(blocksOpMode, identifier, "VectorF");
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public int getLength(Object vectorArg) {
    startBlockExecution(BlockType.GETTER, ".Length");
    VectorF vector = checkVectorF(vectorArg);
    if (vector != null) {
      return vector.length();
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public float getMagnitude(Object vectorArg) {
    startBlockExecution(BlockType.GETTER, ".Magnitude");
    VectorF vector = checkVectorF(vectorArg);
    if (vector != null) {
      return vector.magnitude();
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public VectorF create(int length) {
    startBlockExecution(BlockType.CREATE, "");
    return VectorF.length(length);
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public float get(Object vectorArg, int index) {
    startBlockExecution(BlockType.FUNCTION, ".get");
    VectorF vector = checkVectorF(vectorArg);
    if (vector != null) {
      return vector.get(index);
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void put(Object vectorArg, int index, float value) {
    startBlockExecution(BlockType.FUNCTION, ".put");
    VectorF vector = checkVectorF(vectorArg);
    if (vector != null) {
      vector.put(index, value);
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public String toText(Object vectorArg) {
    startBlockExecution(BlockType.FUNCTION, ".toText");
    VectorF vector = checkVectorF(vectorArg);
    if (vector != null) {
      return vector.toString();
    }
    return "";
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public VectorF normalized3D(Object vectorArg) {
    startBlockExecution(BlockType.FUNCTION, ".normalized3D");
    VectorF vector = checkVectorF(vectorArg);
    if (vector != null) {
      return vector.normalized3D();
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public float dotProduct(Object vector1Arg, Object vector2Arg) {
    startBlockExecution(BlockType.FUNCTION, ".dotProduct");
    VectorF vector1 = checkArg(vector1Arg, VectorF.class, "vector1");
    VectorF vector2 = checkArg(vector2Arg, VectorF.class, "vector2");
    if (vector1 != null && vector2 != null) {
      return vector1.dotProduct(vector2);
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public MatrixF multiplied(Object vectorArg, Object matrixArg) {
    startBlockExecution(BlockType.FUNCTION, ".multiplied");
    VectorF vector = checkVectorF(vectorArg);
    MatrixF matrix = checkMatrixF(matrixArg);
    if (vector != null && matrix != null) {
      return vector.multiplied(matrix);
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public MatrixF added_withMatrix(Object vectorArg, Object matrixArg) {
    startBlockExecution(BlockType.FUNCTION, ".added");
    VectorF vector = checkVectorF(vectorArg);
    MatrixF matrix = checkMatrixF(matrixArg);
    if (vector != null && matrix != null) {
      return vector.added(matrix);
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public VectorF added_withVector(Object vector1Arg, Object vector2Arg) {
    startBlockExecution(BlockType.FUNCTION, ".added");
    VectorF vector1 = checkArg(vector1Arg, VectorF.class, "vector1");
    VectorF vector2 = checkArg(vector2Arg, VectorF.class, "vector2");
    if (vector1 != null && vector2 != null) {
      return vector1.added(vector2);
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void add_withVector(Object vector1Arg, Object vector2Arg) {
    startBlockExecution(BlockType.FUNCTION, ".add");
    VectorF vector1 = checkArg(vector1Arg, VectorF.class, "vector1");
    VectorF vector2 = checkArg(vector2Arg, VectorF.class, "vector2");
    if (vector1 != null && vector2 != null) {
      vector1.add(vector2);
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public MatrixF subtracted_withMatrix(Object vectorArg, Object matrixArg) {
    startBlockExecution(BlockType.FUNCTION, ".subtracted");
    VectorF vector = checkVectorF(vectorArg);
    MatrixF matrix = checkMatrixF(matrixArg);
    if (vector != null && matrix != null) {
      return vector.subtracted(matrix);
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public VectorF subtracted_withVector(Object vector1Arg, Object vector2Arg) {
    startBlockExecution(BlockType.FUNCTION, ".subtracted");
    VectorF vector1 = checkArg(vector1Arg, VectorF.class, "vector1");
    VectorF vector2 = checkArg(vector2Arg, VectorF.class, "vector2");
    if (vector1 != null && vector2 != null) {
      return vector1.subtracted(vector2);
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void subtract_withVector(Object vector1Arg, Object vector2Arg) {
    startBlockExecution(BlockType.FUNCTION, ".subtract");
    VectorF vector1 = checkArg(vector1Arg, VectorF.class, "vector1");
    VectorF vector2 = checkArg(vector2Arg, VectorF.class, "vector2");
    if (vector1 != null && vector2 != null) {
      vector1.subtract(vector2);
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public VectorF multiplied_withScale(Object vectorArg, float scale) {
    startBlockExecution(BlockType.FUNCTION, ".multiplied");
    VectorF vector = checkVectorF(vectorArg);
    if (vector != null) {
      return vector.multiplied(scale);
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void multiply_withScale(Object vectorArg, float scale) {
    startBlockExecution(BlockType.FUNCTION, ".multiply");
    VectorF vector = checkVectorF(vectorArg);
    if (vector != null) {
      vector.multiply(scale);
    }
  }
}