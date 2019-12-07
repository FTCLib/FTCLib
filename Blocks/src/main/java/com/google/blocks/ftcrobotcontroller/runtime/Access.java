/*
Copyright 2016 Google LLC.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.google.blocks.ftcrobotcontroller.runtime;

import android.util.Pair;
import android.webkit.JavascriptInterface;
import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.hardware.rev.RevBlinkinLedDriver.BlinkinPattern;
import com.qualcomm.robotcore.hardware.HardwareDevice;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.RobotLog;
import java.util.Locale;
import java.util.Set;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraName;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.external.matrices.MatrixF;
import org.firstinspires.ftc.robotcore.external.matrices.OpenGLMatrix;
import org.firstinspires.ftc.robotcore.external.matrices.VectorF;
import org.firstinspires.ftc.robotcore.external.navigation.Acceleration;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer.Parameters.CameraMonitorFeedback;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackable;

/**
 * An abstract class for classes that provides JavaScript access to an object.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
abstract class Access {
  protected final static String DEFAULT_CAMERA_MONTIOR_FEEDBACK_STRING = "DEFAULT";

  protected final BlocksOpMode blocksOpMode;
  private final String identifier;
  private final String blockFirstName;

  protected Access(BlocksOpMode blocksOpMode, String identifier, String blockFirstName) {
    this.blocksOpMode = blocksOpMode;
    this.identifier = identifier;
    this.blockFirstName = blockFirstName;
  }

  /**
   * The close method should be overridden in classes that need to clean up when the OpMode has
   * finished running.
   */
  void close() {
  }

  protected final void startBlockExecution(BlockType blockType, String blockLastName) {
    blocksOpMode.startBlockExecution(blockType, blockFirstName, blockLastName);
  }

  protected final void startBlockExecution(
      BlockType blockType, String blockFirstNameOverride, String blockLastName) {
    blocksOpMode.startBlockExecution(blockType, blockFirstNameOverride, blockLastName);
  }

  protected AngleUnit checkAngleUnit(String angleUnitString) {
    return checkArg(angleUnitString, AngleUnit.class, "angleUnit");
  }

  protected AxesOrder checkAxesOrder(String axesOrderString) {
    return checkArg(axesOrderString, AxesOrder.class, "axesOrder");
  }

  protected AxesReference checkAxesReference(String axesReferenceString) {
    return checkArg(axesReferenceString, AxesReference.class, "axesReference");
  }

  protected DistanceUnit checkDistanceUnit(String distanceUnitString) {
    return checkArg(distanceUnitString, DistanceUnit.class, "distanceUnit");
  }

  protected BlinkinPattern checkBlinkinPattern(String blinkinPatternString) {
    return checkArg(blinkinPatternString, BlinkinPattern.class, "blinkinPattern");
  }

  protected BNO055IMU.Parameters checkBNO055IMUParameters(Object parametersArg) {
    return checkArg(parametersArg, BNO055IMU.Parameters.class, "parameters");
  }

  protected MatrixF checkMatrixF(Object matrixArg) {
    return checkArg(matrixArg, MatrixF.class, "matrix");
  }

  protected OpenGLMatrix checkOpenGLMatrix(Object matrixArg) {
    return checkArg(matrixArg, OpenGLMatrix.class, "matrix");
  }

  protected VectorF checkVectorF(Object vectorArg) {
    return checkArg(vectorArg, VectorF.class, "vector");
  }

  protected VuforiaLocalizer.Parameters checkVuforiaLocalizerParameters(Object parametersArg) {
    return checkArg(parametersArg, VuforiaLocalizer.Parameters.class, "vuforiaLocalizerParameters");
  }

  protected Pair<Boolean, CameraMonitorFeedback> checkCameraMonitorFeedback(String cameraMonitorFeedbackString) {
    boolean cameraMonitorFeedbackIsValid;
    CameraMonitorFeedback cameraMonitorFeedback;
    if (cameraMonitorFeedbackString.equalsIgnoreCase(DEFAULT_CAMERA_MONTIOR_FEEDBACK_STRING)) {
      cameraMonitorFeedback = null;
      cameraMonitorFeedbackIsValid = true;
    } else {
      cameraMonitorFeedback = checkArg(cameraMonitorFeedbackString, CameraMonitorFeedback.class, "cameraMonitorFeedback");
      cameraMonitorFeedbackIsValid = cameraMonitorFeedback != null;
    }
    return Pair.create(cameraMonitorFeedbackIsValid, cameraMonitorFeedback);
  }

  protected VuforiaLocalizer.CameraDirection checkVuforiaLocalizerCameraDirection(String cameraDirectionString) {
    return checkArg(cameraDirectionString, VuforiaLocalizer.CameraDirection.class, "cameraDirection");
  }

  protected CameraName checkCameraName(Object cameraNameArg) {
    return checkArg(cameraNameArg, CameraName.class, "cameraName");
  }

  protected CameraName checkCameraNameFromString(HardwareMap hardwareMap, String cameraNameString) {
    return checkCameraName(cameraNameFromString(hardwareMap, cameraNameString));
  }

  protected CameraName cameraNameFromString(HardwareMap hardwareMap, String cameraNameString) {
    return hardwareMap.tryGet(WebcamName.class, cameraNameString);
  }

  protected String cameraNameToString(HardwareMap hardwareMap, CameraName cameraName) {
    if (cameraName instanceof HardwareDevice) {
      Set<String> deviceNames = hardwareMap.getNamesOf((HardwareDevice) cameraName);
      if (!deviceNames.isEmpty()) {
        return deviceNames.iterator().next();
      }
    }

    return "";
  }

  protected VuforiaTrackable checkVuforiaTrackable(Object vuforiaTrackableArg) {
    return checkArg(vuforiaTrackableArg, VuforiaTrackable.class, "vuforiaTrackable");
  }

  /*
   * Reports an error if the given arg is not an instance of the specified clazz.
   */
  protected final <T extends Object> T checkArg(Object arg, Class<T> clazz, String socketName) {
    if (!clazz.isInstance(arg)) {
      reportInvalidArg(socketName, getTypeFromClass(clazz));
      return null;
    }
    return clazz.cast(arg);
  }

  /*
   * Reports an error if the given arg is not compatible with the specified enumClass.
   */
  protected final <T extends Enum<T>> T checkArg(String arg, Class<T> enumClass, String socketName) {
    if (arg == null) {
      reportInvalidArg(socketName, enumClass.getSimpleName());
      return null;
    }
    // First, try the arg as is.
    try {
      return Enum.valueOf(enumClass, arg);
    } catch (IllegalArgumentException e) {
    }
    // Second, try the arg converted to upper case.
    try {
      return Enum.valueOf(enumClass, arg.toUpperCase(Locale.ENGLISH));
    } catch (IllegalArgumentException e) {
      reportInvalidArg(socketName, enumClass.getSimpleName());
      return null;
    }
  }

  protected final void reportInvalidArg(String socketName, String expectedType) {
    if (socketName != null && !socketName.isEmpty()) {
      reportWarning("Incorrect block plugged into the %s socket of the block labeled \"%s\". Expected %s.",
          socketName, blocksOpMode.getFullBlockLabel(), expectedType);
    } else {
      reportWarning("Incorrect block plugged into a socket of the block labeled \"%s\". Expected %s.",
          blocksOpMode.getFullBlockLabel(), expectedType);
    }
  }

  protected final void reportWarning(String message) {
    reportWarning("Warning while executing the block labeled \"%s\". %s",
        blocksOpMode.getFullBlockLabel(), message);
  }

  protected final void reportHardwareError(String message) {
    reportWarning("Error while initializing hardware items. %s",
        blocksOpMode.getFullBlockLabel(), message);
  }

  private final void reportWarning(String format, Object... args) {
    String message = String.format(format, args);
    RobotLog.ww("Blocks", message);
    RobotLog.setGlobalWarningMessage(message);
  }

  private final String getTypeFromClass(Class clazz) {
    // In blocks, the output/check is "BNO055IMU.Parameters", but from the user's point of view,
    // it is "IMU-BNO055.Parameters".
    if (BNO055IMU.Parameters.class.isAssignableFrom(clazz)) {
      return "IMU-BNO055.Parameters";
    }
    if (VuforiaLocalizer.Parameters.class.isAssignableFrom(clazz)) {
      return "VuforiaLocalizer.Parameters";
    }

    return clazz.getSimpleName();
  }
}
