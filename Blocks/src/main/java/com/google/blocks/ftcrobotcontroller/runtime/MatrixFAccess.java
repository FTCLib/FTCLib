// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import android.webkit.JavascriptInterface;
import org.firstinspires.ftc.robotcore.external.matrices.MatrixF;
import org.firstinspires.ftc.robotcore.external.matrices.VectorF;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;

/**
 * A class that provides JavaScript access to {@link MatrixF}.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class MatrixFAccess extends Access {

  MatrixFAccess(BlocksOpMode blocksOpMode, String identifier) {
    super(blocksOpMode, identifier, "MatrixF");
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public int getNumRows(Object matrixArg) {
    startBlockExecution(BlockType.GETTER, ".NumRows");
    MatrixF matrix = checkMatrixF(matrixArg);
    if (matrix != null) {
      return matrix.numRows();
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public int getNumCols(Object matrixArg) {
    startBlockExecution(BlockType.GETTER, ".NumCols");
    MatrixF matrix = checkMatrixF(matrixArg);
    if (matrix != null) {
      return matrix.numCols();
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public MatrixF slice(Object matrixArg, int row, int col, int numRows, int numCols) {
    startBlockExecution(BlockType.FUNCTION, ".slice");
    MatrixF matrix = checkMatrixF(matrixArg);
    if (matrix != null) {
      return matrix.slice(row, col, numRows, numCols);
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public MatrixF identityMatrix(int dim) {
    startBlockExecution(BlockType.FUNCTION, ".identityMatrix");
    return MatrixF.identityMatrix(dim);
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public MatrixF diagonalMatrix(int dim, int scale) {
    startBlockExecution(BlockType.FUNCTION, ".diagonalMatrix");
    return MatrixF.diagonalMatrix(dim, scale);
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public MatrixF diagonalMatrix_withVector(Object vectorArg) {
    startBlockExecution(BlockType.FUNCTION, ".diagonalMatrix");
    VectorF vector = checkVectorF(vectorArg);
    if (vector != null) {
      return MatrixF.diagonalMatrix(vector);
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public float get(Object matrixArg, int row, int col) {
    startBlockExecution(BlockType.FUNCTION, ".get");
    MatrixF matrix = checkMatrixF(matrixArg);
    if (matrix != null) {
      return matrix.get(row, col);
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void put(Object matrixArg, int row, int col, float value) {
    startBlockExecution(BlockType.FUNCTION, ".put");
    MatrixF matrix = checkMatrixF(matrixArg);
    if (matrix != null) {
      matrix.put(row, col, value);
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public VectorF getRow(Object matrixArg, int row) {
    startBlockExecution(BlockType.FUNCTION, ".getRow");
    MatrixF matrix = checkMatrixF(matrixArg);
    if (matrix != null) {
      return matrix.getRow(row);
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public VectorF getColumn(Object matrixArg, int col) {
    startBlockExecution(BlockType.FUNCTION, ".getColumn");
    MatrixF matrix = checkMatrixF(matrixArg);
    if (matrix != null) {
      return matrix.getColumn(col);
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public String toText(Object matrixArg) {
    startBlockExecution(BlockType.FUNCTION, ".toText");
    MatrixF matrix = checkMatrixF(matrixArg);
    if (matrix != null) {
      return matrix.toString();
    }
    return "";
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public VectorF transform(Object matrixArg, Object vectorArg) {
    startBlockExecution(BlockType.FUNCTION, ".transform");
    MatrixF matrix = checkMatrixF(matrixArg);
    VectorF vector = checkVectorF(vectorArg);
    if (matrix != null && vector != null) {
      return matrix.transform(vector);
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public String formatAsTransform(Object matrixArg) {
    startBlockExecution(BlockType.FUNCTION, ".formatAsTransform");
    MatrixF matrix = checkMatrixF(matrixArg);
    if (matrix != null) {
      return matrix.formatAsTransform();
    }
    return "";
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public String formatAsTransform_withArgs(Object matrixArg, String axesReferenceString, String axesOrderString, String angleUnitString) {
    startBlockExecution(BlockType.FUNCTION, ".formatAsTransform");
    MatrixF matrix = checkMatrixF(matrixArg);
    AxesReference axesReference = checkAxesReference(axesReferenceString);
    AxesOrder axesOrder = checkAxesOrder(axesOrderString);
    AngleUnit angleUnit = checkAngleUnit(angleUnitString);
    if (matrix != null && axesReference != null && axesOrder != null && angleUnit != null) {
      return matrix.formatAsTransform(axesReference, axesOrder, angleUnit);
    }
    return "";
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public MatrixF transposed(Object matrixArg) {
    startBlockExecution(BlockType.FUNCTION, ".transposed");
    MatrixF matrix = checkMatrixF(matrixArg);
    if (matrix != null) {
      return matrix.transposed();
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public MatrixF multiplied_withMatrix(Object matrix1Arg, Object matrix2Arg) {
    startBlockExecution(BlockType.FUNCTION, ".multiplied");
    MatrixF matrix1 = checkArg(matrix1Arg, MatrixF.class, "matrix1");
    MatrixF matrix2 = checkArg(matrix2Arg, MatrixF.class, "matrix2");
    if (matrix1 != null && matrix2 != null) {
      return matrix1.multiplied(matrix2);
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public MatrixF multiplied_withScale(Object matrixArg, float scale) {
    startBlockExecution(BlockType.FUNCTION, ".multiplied");
    MatrixF matrix = checkMatrixF(matrixArg);
    if (matrix != null) {
      return matrix.multiplied(scale);
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public VectorF multiplied_withVector(Object matrixArg, Object vectorArg) {
    startBlockExecution(BlockType.FUNCTION, ".multiplied");
    MatrixF matrix = checkMatrixF(matrixArg);
    VectorF vector = checkVectorF(vectorArg);
    if (matrix != null && vector != null) {
      return matrix.multiplied(vector);
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void multiply_withMatrix(Object matrix1Arg, Object matrix2Arg) {
    startBlockExecution(BlockType.FUNCTION, ".multiply");
    MatrixF matrix1 = checkArg(matrix1Arg, MatrixF.class, "matrix1");
    MatrixF matrix2 = checkArg(matrix2Arg, MatrixF.class, "matrix2");
    if (matrix1 != null && matrix2 != null) {
      matrix1.multiply(matrix2);
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void multiply_withScale(Object matrixArg, float scale) {
    startBlockExecution(BlockType.FUNCTION, ".multiply");
    MatrixF matrix = checkMatrixF(matrixArg);
    if (matrix != null) {
      matrix.multiply(scale);
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void multiply_withVector(Object matrixArg, Object vectorArg) {
    startBlockExecution(BlockType.FUNCTION, ".multiply");
    MatrixF matrix = checkMatrixF(matrixArg);
    VectorF vector = checkVectorF(vectorArg);
    if (matrix != null && vector != null) {
      matrix.multiply(vector);
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public VectorF toVector(Object matrixArg) {
    startBlockExecution(BlockType.FUNCTION, ".toVector");
    MatrixF matrix = checkMatrixF(matrixArg);
    if (matrix != null) {
      return matrix.toVector();
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public MatrixF added_withMatrix(Object matrix1Arg, Object matrix2Arg) {
    startBlockExecution(BlockType.FUNCTION, ".added");
    MatrixF matrix1 = checkArg(matrix1Arg, MatrixF.class, "matrix1");
    MatrixF matrix2 = checkArg(matrix2Arg, MatrixF.class, "matrix2");
    if (matrix1 != null && matrix2 != null) {
      return matrix1.added(matrix2);
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public MatrixF added_withVector(Object matrixArg, Object vectorArg) {
    startBlockExecution(BlockType.FUNCTION, ".added");
    MatrixF matrix = checkMatrixF(matrixArg);
    VectorF vector = checkVectorF(vectorArg);
    if (matrix != null && vector != null) {
      return matrix.added(vector);
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void add_withMatrix(Object matrix1Arg, Object matrix2Arg) {
    startBlockExecution(BlockType.FUNCTION, ".add");
    MatrixF matrix1 = checkArg(matrix1Arg, MatrixF.class, "matrix1");
    MatrixF matrix2 = checkArg(matrix2Arg, MatrixF.class, "matrix2");
    if (matrix1 != null && matrix2 != null) {
      matrix1.add(matrix2);
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void add_withVector(Object matrixArg, Object vectorArg) {
    startBlockExecution(BlockType.FUNCTION, ".add");
    MatrixF matrix = checkMatrixF(matrixArg);
    VectorF vector = checkVectorF(vectorArg);
    if (matrix != null && vector != null) {
       matrix.add(vector);
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public MatrixF subtracted_withMatrix(Object matrix1Arg, Object matrix2Arg) {
    startBlockExecution(BlockType.FUNCTION, ".subtracted");
    MatrixF matrix1 = checkArg(matrix1Arg, MatrixF.class, "matrix1");
    MatrixF matrix2 = checkArg(matrix2Arg, MatrixF.class, "matrix2");
    if (matrix1 != null && matrix2 != null) {
      return matrix1.subtracted(matrix2);
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public MatrixF subtracted_withVector(Object matrixArg, Object vectorArg) {
    startBlockExecution(BlockType.FUNCTION, ".subtracted");
    MatrixF matrix = checkMatrixF(matrixArg);
    VectorF vector = checkVectorF(vectorArg);
    if (matrix != null && vector != null) {
      return matrix.subtracted(vector);
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void subtract_withMatrix(Object matrix1Arg, Object matrix2Arg) {
    startBlockExecution(BlockType.FUNCTION, ".subtract");
    MatrixF matrix1 = checkArg(matrix1Arg, MatrixF.class, "matrix1");
    MatrixF matrix2 = checkArg(matrix2Arg, MatrixF.class, "matrix2");
    if (matrix1 != null && matrix2 != null) {
      matrix1.subtract(matrix2);
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void subtract_withVector(Object matrixArg, Object vectorArg) {
    startBlockExecution(BlockType.FUNCTION, ".subtract");
    MatrixF matrix = checkMatrixF(matrixArg);
    VectorF vector = checkVectorF(vectorArg);
    if (matrix != null && vector != null) {
      matrix.subtract(vector);
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public VectorF getTranslation(Object matrixArg) {
    startBlockExecution(BlockType.FUNCTION, ".getTranslation");
    MatrixF matrix = checkMatrixF(matrixArg);
    if (matrix != null) {
      return matrix.getTranslation();
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public MatrixF inverted(Object matrixArg) {
    startBlockExecution(BlockType.FUNCTION, ".inverted");
    MatrixF matrix = checkMatrixF(matrixArg);
    if (matrix != null) {
      return matrix.inverted();
    }
    return null;
  }
}
