/*
Copyright 2019 Google LLC.

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

import static org.firstinspires.ftc.robotcore.external.navigation.AngleUnit.DEGREES;
import static org.firstinspires.ftc.robotcore.external.navigation.AxesOrder.XYZ;
import static org.firstinspires.ftc.robotcore.external.navigation.AxesOrder.YZX;
import static org.firstinspires.ftc.robotcore.external.navigation.AxesReference.EXTRINSIC;

import java.util.Map;
import java.util.HashMap;
import org.firstinspires.ftc.robotcore.external.matrices.OpenGLMatrix;

/**
 * A class that provides simplified access to Vuforia for the SKYSTONE game (2019-2020).
 *
 * @author lizlooney@google.com (Liz Looney)
 */
public class VuforiaSkyStone extends VuforiaBase {
  private static final String ASSET_NAME = "Skystone";
  private static final String STONE_TARGET = "Stone Target";
  private static final String BLUE_REAR_BRIDGE = "Blue Rear Bridge";
  private static final String RED_REAR_BRIDGE = "Red Rear Bridge";
  private static final String RED_FRONT_BRIDGE = "Red Front Bridge";
  private static final String BLUE_FRONT_BRIDGE = "Blue Front Bridge";
  private static final String RED_PERIMETER_1 = "Red Perimeter 1";
  private static final String RED_PERIMETER_2 = "Red Perimeter 2";
  private static final String FRONT_PERIMETER_1 = "Front Perimeter 1";
  private static final String FRONT_PERIMETER_2 = "Front Perimeter 2";
  private static final String BLUE_PERIMETER_1 = "Blue Perimeter 1";
  private static final String BLUE_PERIMETER_2 = "Blue Perimeter 2";
  private static final String REAR_PERIMETER_1 = "Rear Perimeter 1";
  private static final String REAR_PERIMETER_2 = "Rear Perimeter 2";

  // The height of the center of the target image above the floor.
  private static final float MM_TARGET_HEIGHT = 6 * MM_PER_INCH;

  // Constants for the center support targets
  private static final float STONE_Z = 2.00f * MM_PER_INCH;
  private static final float BRIDGE_Z = 6.42f * MM_PER_INCH;
  private static final float BRIDGE_Y = 23 * MM_PER_INCH;
  private static final float BRIDGE_X = 5.18f * MM_PER_INCH;
  private static final float BRIDGE_ROTATION_Y = 59; // degrees
  private static final float BRIDGE_ROTATION_Z = 180; // degrees

  // Constants for perimeter targets
  private static final float HALF_FIELD = 72 * MM_PER_INCH;
  private static final float QUAD_FIELD  = 36 * MM_PER_INCH;

  public static final String[] TRACKABLE_NAMES = {
    STONE_TARGET,
    BLUE_REAR_BRIDGE,
    RED_REAR_BRIDGE,
    RED_FRONT_BRIDGE,
    BLUE_FRONT_BRIDGE,
    RED_PERIMETER_1,
    RED_PERIMETER_2,
    FRONT_PERIMETER_1,
    FRONT_PERIMETER_2,
    BLUE_PERIMETER_1,
    BLUE_PERIMETER_2,
    REAR_PERIMETER_1,
    REAR_PERIMETER_2,
  };
  private static final Map<String, OpenGLMatrix> LOCATIONS_ON_FIELD = new HashMap<>();
  static {
    LOCATIONS_ON_FIELD.put(STONE_TARGET,
        OpenGLMatrix
        .translation(0, 0, STONE_Z)
        .multiplied(Orientation.getRotationMatrix(EXTRINSIC, XYZ, DEGREES, 90, 0, -90)));
    // Set the position of the bridge support targets with relation to origin (center of field).
    LOCATIONS_ON_FIELD.put(BLUE_FRONT_BRIDGE,
        OpenGLMatrix
            .translation(-BRIDGE_X, BRIDGE_Y, BRIDGE_Z)
            .multiplied(Orientation.getRotationMatrix(EXTRINSIC, XYZ, DEGREES, 0, BRIDGE_ROTATION_Y, BRIDGE_ROTATION_Z)));

    LOCATIONS_ON_FIELD.put(BLUE_REAR_BRIDGE,
        OpenGLMatrix
            .translation(-BRIDGE_X, BRIDGE_Y, BRIDGE_Z)
            .multiplied(Orientation.getRotationMatrix(EXTRINSIC, XYZ, DEGREES, 0, -BRIDGE_ROTATION_Y, BRIDGE_ROTATION_Z)));

    LOCATIONS_ON_FIELD.put(RED_FRONT_BRIDGE,
        OpenGLMatrix
            .translation(-BRIDGE_X, -BRIDGE_Y, BRIDGE_Z)
            .multiplied(Orientation.getRotationMatrix(EXTRINSIC, XYZ, DEGREES, 0, -BRIDGE_ROTATION_Y, 0)));

    LOCATIONS_ON_FIELD.put(RED_REAR_BRIDGE,
        OpenGLMatrix
            .translation(BRIDGE_X, -BRIDGE_Y, BRIDGE_Z)
            .multiplied(Orientation.getRotationMatrix(EXTRINSIC, XYZ, DEGREES, 0, BRIDGE_ROTATION_Y, 0)));

    // Set the position of the perimeter targets with relation to origin (center of field).
    LOCATIONS_ON_FIELD.put(RED_PERIMETER_1,
        OpenGLMatrix
            .translation(QUAD_FIELD, -HALF_FIELD, MM_TARGET_HEIGHT)
            .multiplied(Orientation.getRotationMatrix(EXTRINSIC, XYZ, DEGREES, 90, 0, 180)));

    LOCATIONS_ON_FIELD.put(RED_PERIMETER_2,
        OpenGLMatrix
            .translation(-QUAD_FIELD, -HALF_FIELD, MM_TARGET_HEIGHT)
            .multiplied(Orientation.getRotationMatrix(EXTRINSIC, XYZ, DEGREES, 90, 0, 180)));

    LOCATIONS_ON_FIELD.put(FRONT_PERIMETER_1,
        OpenGLMatrix
            .translation(-HALF_FIELD, -QUAD_FIELD, MM_TARGET_HEIGHT)
            .multiplied(Orientation.getRotationMatrix(EXTRINSIC, XYZ, DEGREES, 90, 0 , 90)));

    LOCATIONS_ON_FIELD.put(FRONT_PERIMETER_2,
        OpenGLMatrix
            .translation(-HALF_FIELD, QUAD_FIELD, MM_TARGET_HEIGHT)
            .multiplied(Orientation.getRotationMatrix(EXTRINSIC, XYZ, DEGREES, 90, 0, 90)));

    LOCATIONS_ON_FIELD.put(BLUE_PERIMETER_1,
        OpenGLMatrix
            .translation(-QUAD_FIELD, HALF_FIELD, MM_TARGET_HEIGHT)
            .multiplied(Orientation.getRotationMatrix(EXTRINSIC, XYZ, DEGREES, 90, 0, 0)));

    LOCATIONS_ON_FIELD.put(BLUE_PERIMETER_2,
        OpenGLMatrix
            .translation(QUAD_FIELD, HALF_FIELD, MM_TARGET_HEIGHT)
            .multiplied(Orientation.getRotationMatrix(EXTRINSIC, XYZ, DEGREES, 90, 0, 0)));

    LOCATIONS_ON_FIELD.put(REAR_PERIMETER_1,
        OpenGLMatrix
            .translation(HALF_FIELD, QUAD_FIELD, MM_TARGET_HEIGHT)
            .multiplied(Orientation.getRotationMatrix(EXTRINSIC, XYZ, DEGREES, 90, 0 , -90)));

    LOCATIONS_ON_FIELD.put(REAR_PERIMETER_2,
        OpenGLMatrix
            .translation(HALF_FIELD, -QUAD_FIELD, MM_TARGET_HEIGHT)
            .multiplied(Orientation.getRotationMatrix(EXTRINSIC, XYZ, DEGREES, 90, 0, -90)));
  };

  public VuforiaSkyStone() {
    super(ASSET_NAME, TRACKABLE_NAMES, LOCATIONS_ON_FIELD);
  }
}
