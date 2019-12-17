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

package com.google.blocks.ftcrobotcontroller.util;

import android.support.annotation.Nullable;

/**
 * An enum to represent the various identifiers that are used in the generate javascript code.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
public enum Identifier {
  ACCELERATION("accelerationAccess", "accelerationIdentifierForJavaScript",
      null, null),
  ANDROID_ACCELEROMETER("androidAccelerometerAccess", "androidAccelerometerIdentifierForJavaScript",
      "androidAccelerometer", "androidAccelerometerIdentifierForFtcJava"),
  ANDROID_GYROSCOPE("androidGyroscopeAccess", "androidGyroscopeIdentifierForJavaScript",
      "androidGyroscope", "androidGyroscopeIdentifierForFtcJava"),
  ANDROID_ORIENTATION("androidOrientationAccess", "androidOrientationIdentifierForJavaScript",
      "androidOrientation", "androidOrientationIdentifierForFtcJava"),
  ANDROID_SOUND_POOL("androidSoundPoolAccess", "androidSoundPoolIdentifierForJavaScript",
      "androidSoundPool", "androidSoundPoolIdentifierForFtcJava"),
  ANDROID_TEXT_TO_SPEECH("androidTextToSpeechAccess", "androidTextToSpeechIdentifierForJavaScript",
      "androidTextToSpeech", "androidTextToSpeechIdentifierForFtcJava"),
  ANGULAR_VELOCITY("angularVelocityAccess", "angularVelocityIdentifierForJavaScript",
      null, null),
  BLINKIN_PATTERN("blinkinPatternAccess", "blinkinPatternIdentifierForJavaScript",
      null, null),
  BLOCKS_OP_MODE("blocksOpMode", null,
      null, null),
  BNO055IMU_PARAMETERS("bno055imuParametersAccess", "bno055imuParametersIdentifierForJavaScript",
      null, null),
  COLOR("colorAccess", "colorIdentifierForJavaScript",
      null, null),
  DBG_LOG("dbgLogAccess", "dbgLogIdentifierForJavaScript",
      null, null),
  ELAPSED_TIME("elapsedTimeAccess", "elapsedTimeIdentifierForJavaScript",
      null, null),
  GAMEPAD_1("gamepad1", null,
      "gamepad1", null),
  GAMEPAD_2("gamepad2", null,
      "gamepad2", null),
  LINEAR_OP_MODE("linearOpMode", "linearOpModeIdentifierForJavaScript",
      null, null),
  MAGNETIC_FLUX("magneticFluxAccess", "magneticFluxIdentifierForJavaScript",
      null, null),
  MATRIX_F("matrixFAccess", "matrixFIdentifierForJavaScript",
      null, null),
  MISC("miscAccess", "miscIdentifierForJavaScript",
      null, null),
  NAVIGATION("navigationAccess", "navigationIdentifierForJavaScript",
      null, null),
  OPEN_GL_MATRIX("openGLMatrixAccess", "openGLMatrixIdentifierForJavaScript",
      null, null),
  ORIENTATION("orientationAccess", "orientationIdentifierForJavaScript",
      null, null),
  PIDF_COEFFICIENTS("pidfCoefficientsAccess", "pidfCoefficientsIdentifierForJavaScript",
      null, null),
  POSITION("positionAccess", "positionIdentifierForJavaScript",
      null, null),
  QUATERNION("quaternionAccess", "quaternionIdentifierForJavaScript",
      null, null),
  RANGE("rangeAccess", "rangeIdentifierForJavaScript",
      null, null),
  SYSTEM("systemAccess", "systemIdentifierForJavaScript",
      null, null),
  TELEMETRY("telemetry", "telemetryIdentifierForJavaScript",
      null, null),
  TEMPERATURE("temperatureAccess", "temperatureIdentifierForJavaScript",
      null, null),
  TFOD_ROVER_RUCKUS("tfodRoverRuckusAccess", "tfodRoverRuckusIdentifierForJavaScript",
      "tfodRoverRuckus", "tfodRoverRuckusIdentifierForFtcJava"),
  TFOD_SKY_STONE("tfodSkyStoneAccess", "tfodSkyStoneIdentifierForJavaScript",
      "tfodSkyStone", "tfodSkyStoneIdentifierForFtcJava"),
  VECTOR_F("vectorFAccess", "vectorFIdentifierForJavaScript",
      null, null),
  VELOCITY("velocityAccess", "velocityIdentifierForJavaScript",
      null, null),
  VUFORIA_RELIC_RECOVERY("vuforiaAccess", "vuforiaIdentifierForJavaScript", // For backwards compatibility
      "vuforiaRelicRecovery", "vuforiaRelicRecoveryIdentifierForFtcJava"),
  VUFORIA_ROVER_RUCKUS("vuforiaRoverRuckusAccess", "vuforiaRoverRuckusIdentifierForJavaScript",
      "vuforiaRoverRuckus", "vuforiaRoverRuckusIdentifierForFtcJava"),
  VUFORIA_SKY_STONE("vuforiaSkyStoneAccess", "vuforiaSkyStoneIdentifierForJavaScript",
      "vuforiaSkyStone", "vuforiaSkyStoneIdentifierForFtcJava"),
  VUFORIA_LOCALIZER("vuforiaLocalizerAccess", "vuforiaLocalizerIdentifierForJavaScript",
      null, null),
  VUFORIA_LOCALIZER_PARAMETERS("vuforiaLocalizerParametersAccess", "vuforiaLocalizerParametersIdentifierForJavaScript",
      null, null),
  VUFORIA_TRACKABLE("vuforiaTrackableAccess", "vuforiaTrackableIdentifierForJavaScript",
      null, null),
  VUFORIA_TRACKABLE_DEFAULT_LISTENER("vuforiaTrackableDefaultListenerAccess", "vuforiaTrackableDefaultListenerIdentifierForJavaScript",
      null, null),
  VUFORIA_TRACKABLES("vuforiaTrackablesAccess", "vuforiaTrackablesIdentifierForJavaScript",
      null, null);

  /**
   * The identifier used in the generated javascript code.
   */
  public final String identifierForJavaScript;
  /**
   * The identifier used in the generated java code.
   */
  public final String identifierForFtcJava;
  /**
   * The name of the variable used in the JavaScript generators.
   */
  @Nullable
  public final String variableForJavaScript;
  /**
   * The name of the variable used in the FtcJava generators.
   */
  @Nullable
  public final String variableForFtcJava;

  Identifier(String identifierForJavaScript, String variableForJavaScript,
      String identifierForFtcJava, String variableForFtcJava) {
    this.identifierForJavaScript = identifierForJavaScript;
    this.variableForJavaScript = variableForJavaScript;
    this.identifierForFtcJava = identifierForFtcJava;
    this.variableForFtcJava = variableForFtcJava;
  }
}
