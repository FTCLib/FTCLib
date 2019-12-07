// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import android.webkit.JavascriptInterface;
import com.google.blocks.ftcrobotcontroller.hardware.HardwareItem;
import com.qualcomm.hardware.hitechnic.HiTechnicNxtGyroSensor;
import com.qualcomm.hardware.modernrobotics.ModernRoboticsI2cGyro;
import com.qualcomm.hardware.modernrobotics.ModernRoboticsI2cGyro.HeadingMode;
import com.qualcomm.robotcore.hardware.GyroSensor;
import com.qualcomm.robotcore.hardware.Gyroscope;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.I2cAddr;
import com.qualcomm.robotcore.hardware.OrientationSensor;
import java.util.Set;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AngularVelocity;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.Axis;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;

/**
 * A class that provides JavaScript access to a {@link GyroSensor}.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class GyroSensorAccess extends HardwareAccess<GyroSensor> {
  private final GyroSensor gyroSensor;

  GyroSensorAccess(BlocksOpMode blocksOpMode, HardwareItem hardwareItem, HardwareMap hardwareMap) {
    super(blocksOpMode, hardwareItem, hardwareMap, GyroSensor.class);
    this.gyroSensor = hardwareDevice;
  }

  // Properties

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {HiTechnicNxtGyroSensor.class, ModernRoboticsI2cGyro.class}, methodName = "getHeading")
  public int getHeading() {
    startBlockExecution(BlockType.GETTER, ".Heading");
    return gyroSensor.getHeading();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = ModernRoboticsI2cGyro.class, methodName = "setHeadingMode")
  public void setHeadingMode(String headingModeString) {
    startBlockExecution(BlockType.SETTER, ".HeadingMode");
    HeadingMode headingMode = checkArg(headingModeString, HeadingMode.class, "");
    if (headingMode != null) {
      if (gyroSensor instanceof ModernRoboticsI2cGyro) {
        ((ModernRoboticsI2cGyro) gyroSensor).setHeadingMode(headingMode);
      } else {
        reportWarning("This GyroSensor is not a ModernRoboticsI2cGyro.");
      }
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = ModernRoboticsI2cGyro.class, methodName = "getHeadingMode")
  public String getHeadingMode() {
    startBlockExecution(BlockType.GETTER, ".HeadingMode");
    if (gyroSensor instanceof ModernRoboticsI2cGyro) {
      HeadingMode headingMode = ((ModernRoboticsI2cGyro) gyroSensor).getHeadingMode();
      if (headingMode != null) {
        return headingMode.toString();
      }
    } else {
      reportWarning("This GyroSensor is not a ModernRoboticsI2cGyro.");
    }
    return "";
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = ModernRoboticsI2cGyro.class, methodName = "setI2cAddress")
  public void setI2cAddress7Bit(int i2cAddr7Bit) {
    startBlockExecution(BlockType.SETTER, ".I2cAddress7Bit");
    if (gyroSensor instanceof ModernRoboticsI2cGyro) {
      ((ModernRoboticsI2cGyro) gyroSensor).setI2cAddress(I2cAddr.create7bit(i2cAddr7Bit));
    } else {
      reportWarning("This GyroSensor is not a ModernRoboticsI2cGyro.");
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = ModernRoboticsI2cGyro.class, methodName = "getI2cAddress")
  public int getI2cAddress7Bit() {
    startBlockExecution(BlockType.GETTER, ".I2cAddress7Bit");
    if (gyroSensor instanceof ModernRoboticsI2cGyro) {
      I2cAddr i2cAddr = ((ModernRoboticsI2cGyro) gyroSensor).getI2cAddress();
      if (i2cAddr != null) {
        return i2cAddr.get7Bit();
      }
    } else {
      reportWarning("This GyroSensor is not a ModernRoboticsI2cGyro.");
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = ModernRoboticsI2cGyro.class, methodName = "setI2cAddress")
  public void setI2cAddress8Bit(int i2cAddr8Bit) {
    startBlockExecution(BlockType.SETTER, ".I2cAddress8Bit");
    if (gyroSensor instanceof ModernRoboticsI2cGyro) {
      ((ModernRoboticsI2cGyro) gyroSensor).setI2cAddress(I2cAddr.create8bit(i2cAddr8Bit));
    } else {
      reportWarning("This GyroSensor is not a ModernRoboticsI2cGyro.");
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = ModernRoboticsI2cGyro.class, methodName = "getI2cAddress")
  public int getI2cAddress8Bit() {
    startBlockExecution(BlockType.GETTER, ".I2cAddress8Bit");
    if (gyroSensor instanceof ModernRoboticsI2cGyro) {
      I2cAddr i2cAddr = ((ModernRoboticsI2cGyro) gyroSensor).getI2cAddress();
      if (i2cAddr != null) {
        return i2cAddr.get8Bit();
      }
    } else {
      reportWarning("This GyroSensor is not a ModernRoboticsI2cGyro.");
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = ModernRoboticsI2cGyro.class, methodName = "getIntegratedZValue")
  public int getIntegratedZValue() {
    startBlockExecution(BlockType.GETTER, ".IntegratedZValue");
    if (gyroSensor instanceof ModernRoboticsI2cGyro) {
      return ((ModernRoboticsI2cGyro) gyroSensor).getIntegratedZValue();
    } else {
      reportWarning("This GyroSensor is not a ModernRoboticsI2cGyro.");
    }
    return 0;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {HiTechnicNxtGyroSensor.class, ModernRoboticsI2cGyro.class}, methodName = "rawX")
  public int getRawX() {
    startBlockExecution(BlockType.GETTER, ".RawX");
    return gyroSensor.rawX();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {HiTechnicNxtGyroSensor.class, ModernRoboticsI2cGyro.class}, methodName = "rawY")
  public int getRawY() {
    startBlockExecution(BlockType.GETTER, ".RawY");
    return gyroSensor.rawY();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {HiTechnicNxtGyroSensor.class, ModernRoboticsI2cGyro.class}, methodName = "rawZ")
  public int getRawZ() {
    startBlockExecution(BlockType.GETTER, ".RawZ");
    return gyroSensor.rawZ();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {HiTechnicNxtGyroSensor.class}, methodName = "getRotationFraction")
  public double getRotationFraction() {
    startBlockExecution(BlockType.GETTER, ".RotationFraction");
    return gyroSensor.getRotationFraction();
  }

  // Functions

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {HiTechnicNxtGyroSensor.class, ModernRoboticsI2cGyro.class}, methodName = "calibrate")
  public void calibrate() {
    startBlockExecution(BlockType.FUNCTION, ".calibrate");
    gyroSensor.calibrate();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {HiTechnicNxtGyroSensor.class, ModernRoboticsI2cGyro.class}, methodName = "isCalibrating")
  public boolean isCalibrating() {
    startBlockExecution(BlockType.FUNCTION, ".isCalibrating");
    return gyroSensor.isCalibrating();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {HiTechnicNxtGyroSensor.class, ModernRoboticsI2cGyro.class}, methodName = "resetZAxisIntegrator")
  public void resetZAxisIntegrator() {
    startBlockExecution(BlockType.FUNCTION, ".resetZAxisIntegrator");
    gyroSensor.resetZAxisIntegrator();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {HiTechnicNxtGyroSensor.class, ModernRoboticsI2cGyro.class}, methodName = "getAngularVelocityAxes")
  public String getAngularVelocityAxes() {
    startBlockExecution(BlockType.GETTER, ".AngularVelocityAxes");
    if (gyroSensor instanceof Gyroscope) {
      Set<Axis> axes = ((Gyroscope) gyroSensor).getAngularVelocityAxes();
      StringBuilder sb = new StringBuilder();
      sb.append("[");
      String delimiter = "";
      for (Axis axis : axes) {
        sb.append(delimiter).append("\"").append(axis.toString()).append("\"");
        delimiter = ",";
      }
      sb.append("]");
      return sb.toString();
    } else {
      reportWarning("This GyroSensor is not a Gyroscope.");
    }
    return "[]";
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {HiTechnicNxtGyroSensor.class, ModernRoboticsI2cGyro.class}, methodName = "getAngularVelocity")
  public AngularVelocity getAngularVelocity(String angleUnitString) {
    startBlockExecution(BlockType.FUNCTION, ".getAngularVelocity");
    AngleUnit angleUnit = checkAngleUnit(angleUnitString);
    if (angleUnit != null) {
      if (gyroSensor instanceof Gyroscope) {
        return ((Gyroscope) gyroSensor).getAngularVelocity(angleUnit);
      } else {
        reportWarning("This GyroSensor is not a Gyroscope.");
        return new AngularVelocity(angleUnit, 0, 0, 0, 0L);
      }
    }
    return null;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {ModernRoboticsI2cGyro.class}, methodName = "getAngularOrientationAxes")
  public String getAngularOrientationAxes() {
    startBlockExecution(BlockType.GETTER, ".AngularOrientationAxes");
    if (gyroSensor instanceof OrientationSensor) {
      Set<Axis> axes = ((OrientationSensor) gyroSensor).getAngularOrientationAxes();
      StringBuilder sb = new StringBuilder();
      sb.append("[");
      String delimiter = "";
      for (Axis axis : axes) {
        sb.append(delimiter).append("\"").append(axis.toString()).append("\"");
        delimiter = ",";
      }
      sb.append("]");
      return sb.toString();
    } else {
      reportWarning("This GyroSensor is not a OrientationSensor.");
    }
    return "[]";
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  @Block(classes = {ModernRoboticsI2cGyro.class}, methodName = "getAngularOrientation")
  public Orientation getAngularOrientation(String axesReferenceString, String axesOrderString, String angleUnitString) {
    startBlockExecution(BlockType.FUNCTION, ".getAngularOrientation");
    AxesReference axesReference = checkAxesReference(axesReferenceString);
    AxesOrder axesOrder = checkAxesOrder(axesOrderString);
    AngleUnit angleUnit = checkAngleUnit(angleUnitString);
    if (axesReference != null && axesOrder != null && angleUnit != null) {
      if (gyroSensor instanceof OrientationSensor) {
        return ((OrientationSensor) gyroSensor).getAngularOrientation(axesReference, axesOrder, angleUnit);
      } else {
        reportWarning("This GyroSensor is not a OrientationSensor.");
        return new Orientation(axesReference, axesOrder, angleUnit, 0, 0, 0, 0L);
      }
    }
    return null;
  }
}
