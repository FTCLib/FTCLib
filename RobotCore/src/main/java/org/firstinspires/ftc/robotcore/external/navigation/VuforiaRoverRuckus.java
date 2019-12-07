/*
Copyright 2018 Google LLC.

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

package org.firstinspires.ftc.robotcore.external.navigation;

import org.firstinspires.ftc.robotcore.external.matrices.OpenGLMatrix;

/**
 * A class that provides simplified access to Vuforia for the Rover Ruckus game (2018-2019).
 *
 * @author lizlooney@google.com (Liz Looney)
 */
public class VuforiaRoverRuckus extends VuforiaBase {
  private static final String ASSET_NAME = "RoverRuckus";
  public static final String[] TRACKABLE_NAMES = {
    "BluePerimeter",
    "RedPerimeter",
    "FrontPerimeter",
    "BackPerimeter",
  };
  private static final OpenGLMatrix[] LOCATIONS_ON_FIELD = {
    // Place the BluePerimeter target in the middle of the blue perimeter wall.
    OpenGLMatrix
        .translation(0, MM_FTC_FIELD_WIDTH/2, 0)
        .multiplied(Orientation.getRotationMatrix(
            AxesReference.EXTRINSIC, AxesOrder.XZX, AngleUnit.DEGREES, 90, 0, 0)),
    // Place the RedPerimeter target in the middle of the red perimeter wall.
    OpenGLMatrix
        .translation(0, -MM_FTC_FIELD_WIDTH / 2, 0)
        .multiplied(Orientation.getRotationMatrix(
            AxesReference.EXTRINSIC, AxesOrder.XZX, AngleUnit.DEGREES, 90, 180, 0)),
    // Place the FrontPerimeter target in the middle of the front perimeter wall.
    OpenGLMatrix
        .translation(-MM_FTC_FIELD_WIDTH / 2, 0, 0)
        .multiplied(Orientation.getRotationMatrix(
            AxesReference.EXTRINSIC, AxesOrder.XZX, AngleUnit.DEGREES, 90, 90, 0)),
    // Place the BackPerimeter target in the middle of the back perimeter wall.
    OpenGLMatrix
        .translation(MM_FTC_FIELD_WIDTH / 2, 0, 0)
        .multiplied(Orientation.getRotationMatrix(
            AxesReference.EXTRINSIC, AxesOrder.XZX, AngleUnit.DEGREES, 90, 270, 0)),
  };

  public VuforiaRoverRuckus() {
    super(ASSET_NAME, TRACKABLE_NAMES, LOCATIONS_ON_FIELD);
  }
}
