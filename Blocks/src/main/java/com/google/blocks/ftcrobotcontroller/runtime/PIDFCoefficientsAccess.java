package com.google.blocks.ftcrobotcontroller.runtime;

import android.webkit.JavascriptInterface;
import com.qualcomm.robotcore.hardware.MotorControlAlgorithm;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;

/**
 * A class that provides JavaScript access to {@link PIDFCoefficients}.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class PIDFCoefficientsAccess extends Access {

  PIDFCoefficientsAccess(BlocksOpMode blocksOpMode, String identifier) {
    super(blocksOpMode, identifier, "PIDFCoefficients");
  }

  private PIDFCoefficients checkPIDFCoefficients(Object pidfCoefficientsArg) {
    return checkArg(pidfCoefficientsArg, PIDFCoefficients.class, "pidfCoefficients");
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public PIDFCoefficients create() {
    startBlockExecution(BlockType.CREATE, "");
    return new PIDFCoefficients();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public PIDFCoefficients create_withPIDFAlgorithm(double p, double i, double d, double f, String algorithmString) {
    startBlockExecution(BlockType.CREATE, "");
    MotorControlAlgorithm algorithm = checkArg(algorithmString, MotorControlAlgorithm.class, "algorithm");
    if (algorithm != null) {
      return new PIDFCoefficients(p, i, d, f, algorithm);
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public PIDFCoefficients create_withPIDF(double p, double i, double d, double f) {
    startBlockExecution(BlockType.CREATE, "");
    return new PIDFCoefficients(p, i, d, f);
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public PIDFCoefficients create_withPIDFCoefficients(Object pidfCoefficientsArg) {
    startBlockExecution(BlockType.CREATE, "");
    PIDFCoefficients pidfCoefficients = checkPIDFCoefficients(pidfCoefficientsArg);
    if (pidfCoefficients != null) {
      return new PIDFCoefficients(pidfCoefficients);
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void setP(Object pidfCoefficientsArg, double p) {
    startBlockExecution(BlockType.SETTER, ".P");
    PIDFCoefficients pidfCoefficients = checkPIDFCoefficients(pidfCoefficientsArg);
    if (pidfCoefficients != null) {
      pidfCoefficients.p = p;
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public double getP(Object pidfCoefficientsArg) {
    startBlockExecution(BlockType.GETTER, ".P");
    PIDFCoefficients pidfCoefficients = checkPIDFCoefficients(pidfCoefficientsArg);
    if (pidfCoefficients != null) {
      return pidfCoefficients.p;
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void setI(Object pidfCoefficientsArg, double i) {
    startBlockExecution(BlockType.SETTER, ".I");
    PIDFCoefficients pidfCoefficients = checkPIDFCoefficients(pidfCoefficientsArg);
    if (pidfCoefficients != null) {
      pidfCoefficients.i = i;
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public double getI(Object pidfCoefficientsArg) {
    startBlockExecution(BlockType.GETTER, ".I");
    PIDFCoefficients pidfCoefficients = checkPIDFCoefficients(pidfCoefficientsArg);
    if (pidfCoefficients != null) {
      return pidfCoefficients.i;
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void setD(Object pidfCoefficientsArg, double d) {
    startBlockExecution(BlockType.SETTER, ".D");
    PIDFCoefficients pidfCoefficients = checkPIDFCoefficients(pidfCoefficientsArg);
    if (pidfCoefficients != null) {
      pidfCoefficients.d = d;
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public double getD(Object pidfCoefficientsArg) {
    startBlockExecution(BlockType.GETTER, ".D");
    PIDFCoefficients pidfCoefficients = checkPIDFCoefficients(pidfCoefficientsArg);
    if (pidfCoefficients != null) {
      return pidfCoefficients.d;
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void setF(Object pidfCoefficientsArg, double f) {
    startBlockExecution(BlockType.SETTER, ".F");
    PIDFCoefficients pidfCoefficients = checkPIDFCoefficients(pidfCoefficientsArg);
    if (pidfCoefficients != null) {
      pidfCoefficients.f = f;
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public double getF(Object pidfCoefficientsArg) {
    startBlockExecution(BlockType.GETTER, ".F");
    PIDFCoefficients pidfCoefficients = checkPIDFCoefficients(pidfCoefficientsArg);
    if (pidfCoefficients != null) {
      return pidfCoefficients.f;
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void setAlgorithm(Object pidfCoefficientsArg, String algorithmString) {
    startBlockExecution(BlockType.SETTER, ".Algorithm");
    PIDFCoefficients pidfCoefficients = checkPIDFCoefficients(pidfCoefficientsArg);
    MotorControlAlgorithm algorithm = checkArg(algorithmString, MotorControlAlgorithm.class, "");
    if (pidfCoefficients != null && algorithm != null) {
      pidfCoefficients.algorithm = algorithm;
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public String getAlgorithm(Object pidfCoefficientsArg) {
    startBlockExecution(BlockType.GETTER, ".Algorithm");
    PIDFCoefficients pidfCoefficients = checkPIDFCoefficients(pidfCoefficientsArg);
    if (pidfCoefficients != null && pidfCoefficients.algorithm != null) {
      return pidfCoefficients.algorithm.toString();
    }
    return "";
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public String toText(Object pidfCoefficientsArg) {
    startBlockExecution(BlockType.FUNCTION, ".toText");
    PIDFCoefficients pidfCoefficients = checkPIDFCoefficients(pidfCoefficientsArg);
    if (pidfCoefficients != null) {
      return pidfCoefficients.toString();
    }
    return "";
  }
}
