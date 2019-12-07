// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import android.webkit.JavascriptInterface;
import com.qualcomm.hardware.bosch.BNO055IMU.AccelUnit;
import com.qualcomm.hardware.bosch.BNO055IMU.AccelerationIntegrator;
import com.qualcomm.hardware.bosch.BNO055IMU.AngleUnit;
import com.qualcomm.hardware.bosch.BNO055IMU.Parameters;
import com.qualcomm.hardware.bosch.BNO055IMU.SensorMode;
import com.qualcomm.hardware.bosch.BNO055IMU.TempUnit;
import com.qualcomm.hardware.bosch.JustLoggingAccelerationIntegrator;
import com.qualcomm.hardware.bosch.NaiveAccelerationIntegrator;
import com.qualcomm.robotcore.hardware.I2cAddr;

/**
 * A class that provides JavaScript access to {@link BNO055IMU#Parameters}.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class BNO055IMUParametersAccess extends Access {

  BNO055IMUParametersAccess(BlocksOpMode blocksOpMode, String identifier) {
    super(blocksOpMode, identifier, "IMU-BNO055.Parameters");
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = Parameters.class, constructor = true)
  public Parameters create() {
    startBlockExecution(BlockType.CREATE, "");
    return new Parameters();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = Parameters.class, fieldName = "accelUnit")
  public void setAccelUnit(Object parametersArg, String accelUnitString) {
    startBlockExecution(BlockType.FUNCTION, ".setAccelUnit");
    Parameters parameters = checkBNO055IMUParameters(parametersArg);
    AccelUnit accelUnit = checkArg(accelUnitString, AccelUnit.class, "accelUnit");
    if (parameters != null && accelUnit != null) {
      parameters.accelUnit = accelUnit;
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = Parameters.class, fieldName = "accelUnit")
  public String getAccelUnit(Object parametersArg) {
    startBlockExecution(BlockType.GETTER, ".AccelUnit");
    Parameters parameters = checkBNO055IMUParameters(parametersArg);
    if (parameters != null) {
      AccelUnit accelUnit = parameters.accelUnit;
      if (accelUnit != null) {
        return accelUnit.toString();
      }
    }
    return "";
  }

  enum Algorithm {
    NAIVE,
    JUST_LOGGING
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = Parameters.class, fieldName = "accelerationIntegrationAlgorithm")
  public void setAccelerationIntegrationAlgorithm(Object parametersArg, String algorithmString) {
    startBlockExecution(BlockType.FUNCTION, ".setAccelerationIntegrationAlgorithm");
    Parameters parameters = checkBNO055IMUParameters(parametersArg);
    Algorithm algorithm = checkArg(algorithmString, Algorithm.class, "accelerationIntegrationAlgorithm");
    if (parameters != null && algorithm != null) {
      switch (algorithm) {
        case NAIVE:
          parameters.accelerationIntegrationAlgorithm = null;
          break;
        case JUST_LOGGING:
          parameters.accelerationIntegrationAlgorithm = new JustLoggingAccelerationIntegrator();
          break;
      }
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = Parameters.class, fieldName = "accelerationIntegrationAlgorithm")
  public String getAccelerationIntegrationAlgorithm(Object parametersArg) {
    startBlockExecution(BlockType.GETTER, ".AccelerationIntegrationAlgorithm");
    Parameters parameters = checkBNO055IMUParameters(parametersArg);
    if (parameters != null) {
      AccelerationIntegrator accelerationIntegrator = parameters.accelerationIntegrationAlgorithm;
      if (accelerationIntegrator == null ||
          accelerationIntegrator instanceof NaiveAccelerationIntegrator) {
        return "NAIVE";
      } else if (accelerationIntegrator instanceof JustLoggingAccelerationIntegrator) {
        return "JUST_LOGGING";
      }
    }
    return "";
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = Parameters.class, fieldName = "angleUnit")
  public void setAngleUnit(Object parametersArg, String angleUnitString) {
    startBlockExecution(BlockType.FUNCTION, ".setAngleUnit");
    Parameters parameters = checkBNO055IMUParameters(parametersArg);
    AngleUnit angleUnit = checkArg(angleUnitString, AngleUnit.class, "angleUnit");
    if (parameters != null && angleUnit != null) {
      parameters.angleUnit = angleUnit;
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = Parameters.class, fieldName = "angleUnit")
  public String getAngleUnit(Object parametersArg) {
    startBlockExecution(BlockType.GETTER, ".AngleUnit");
    Parameters parameters = checkBNO055IMUParameters(parametersArg);
    if (parameters != null) {
      AngleUnit angleUnit = parameters.angleUnit;
      if (angleUnit != null) {
        return angleUnit.toString();
      }
    }
    return "";
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = Parameters.class, fieldName = "calibrationDataFile")
  public void setCalibrationDataFile(Object parametersArg, String calibrationDataFile) {
    startBlockExecution(BlockType.FUNCTION, ".setCalibrationDataFile");
    Parameters parameters = checkBNO055IMUParameters(parametersArg);
    if (parameters != null) {
      parameters.calibrationDataFile = calibrationDataFile;
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = Parameters.class, fieldName = "calibrationDataFile")
  public String getCalibrationDataFile(Object parametersArg) {
    startBlockExecution(BlockType.GETTER, ".CalibrationDataFile");
    Parameters parameters = checkBNO055IMUParameters(parametersArg);
    if (parameters != null) {
      String calibrationDataFile = parameters.calibrationDataFile;
      if (calibrationDataFile != null) {
        return calibrationDataFile;
      }
    }
    return "";
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = Parameters.class, fieldName = "i2cAddr")
  public void setI2cAddress7Bit(Object parametersArg, int i2cAddr7Bit) {
    startBlockExecution(BlockType.FUNCTION, ".setI2cAddress7Bit");
    Parameters parameters = checkBNO055IMUParameters(parametersArg);
    if (parameters != null) {
      parameters.i2cAddr = I2cAddr.create7bit(i2cAddr7Bit);
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = Parameters.class, fieldName = "i2cAddr")
  public int getI2cAddress7Bit(Object parametersArg) {
    startBlockExecution(BlockType.GETTER, ".I2cAddress7Bit");
    Parameters parameters = checkBNO055IMUParameters(parametersArg);
    if (parameters != null) {
      I2cAddr i2cAddr = parameters.i2cAddr;
      if (i2cAddr != null) {
        return i2cAddr.get7Bit();
      }
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = Parameters.class, fieldName = "i2cAddr")
  public void setI2cAddress8Bit(Object parametersArg, int i2cAddr8Bit) {
    startBlockExecution(BlockType.FUNCTION, ".setI2cAddress8Bit");
    Parameters parameters = checkBNO055IMUParameters(parametersArg);
    if (parameters != null) {
      parameters.i2cAddr = I2cAddr.create8bit(i2cAddr8Bit);
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = Parameters.class, fieldName = "i2cAddr")
  public int getI2cAddress8Bit(Object parametersArg) {
    startBlockExecution(BlockType.GETTER, ".I2cAddress8Bit");
    Parameters parameters = checkBNO055IMUParameters(parametersArg);
    if (parameters != null) {
      I2cAddr i2cAddr = parameters.i2cAddr;
      if (i2cAddr != null) {
        return i2cAddr.get8Bit();
      }
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = Parameters.class, fieldName = "loggingEnabled")
  public void setLoggingEnabled(Object parametersArg, boolean loggingEnabled) {
    startBlockExecution(BlockType.FUNCTION, ".setLoggingEnabled");
    Parameters parameters = checkBNO055IMUParameters(parametersArg);
    if (parameters != null) {
      parameters.loggingEnabled = loggingEnabled;
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = Parameters.class, fieldName = "loggingEnabled")
  public boolean getLoggingEnabled(Object parametersArg) {
    startBlockExecution(BlockType.GETTER, ".LoggingEnabled");
    Parameters parameters = checkBNO055IMUParameters(parametersArg);
    if (parameters != null) {
      return parameters.loggingEnabled;
    }
    return false;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = Parameters.class, fieldName = "loggingTag")
  public void setLoggingTag(Object parametersArg, String loggingTag) {
    startBlockExecution(BlockType.FUNCTION, ".setLoggingTag");
    Parameters parameters = checkBNO055IMUParameters(parametersArg);
    if (parameters != null) {
      parameters.loggingTag = loggingTag;
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = Parameters.class, fieldName = "loggingTag")
  public String getLoggingTag(Object parametersArg) {
    startBlockExecution(BlockType.GETTER, ".LoggingTag");
    Parameters parameters = checkBNO055IMUParameters(parametersArg);
    if (parameters != null) {
      String loggingTag = parameters.loggingTag;
      if (loggingTag != null) {
        return loggingTag;
      }
    }
    return "";
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = Parameters.class, fieldName = "mode")
  public void setSensorMode(Object parametersArg, String sensorModeString) {
    startBlockExecution(BlockType.FUNCTION, ".setSensorMode");
    Parameters parameters = checkBNO055IMUParameters(parametersArg);
    SensorMode sensorMode = checkArg(sensorModeString, SensorMode.class, "sensorMode");
    if (parameters != null && sensorMode != null) {
      parameters.mode = sensorMode;
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = Parameters.class, fieldName = "mode")
  public String getSensorMode(Object parametersArg) {
    startBlockExecution(BlockType.GETTER, ".SensorMode");
    Parameters parameters = checkBNO055IMUParameters(parametersArg);
    if (parameters != null) {
      SensorMode sensorMode = parameters.mode;
      if (sensorMode != null) {
        return sensorMode.toString();
      }
    }
    return "";
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = Parameters.class, fieldName = "temperatureUnit")
  public void setTempUnit(Object parametersArg, String tempUnitString) {
    startBlockExecution(BlockType.FUNCTION, ".setTempUnit");
    Parameters parameters = checkBNO055IMUParameters(parametersArg);
    TempUnit tempUnit = checkArg(tempUnitString, TempUnit.class, "tempUnit");
    if (parameters != null && tempUnit != null) {
      parameters.temperatureUnit = tempUnit;
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = Parameters.class, fieldName = "temperatureUnit")
  public String getTempUnit(Object parametersArg) {
    startBlockExecution(BlockType.GETTER, ".TempUnit");
    Parameters parameters = checkBNO055IMUParameters(parametersArg);
    if (parameters != null) {
      TempUnit tempUnit = parameters.temperatureUnit;
      if (tempUnit != null) {
        return tempUnit.toString();
      }
    }
    return "";
  }
}
