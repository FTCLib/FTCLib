/*
 * Copyright (C) 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.firstinspires.ftc.robotcore.external.tfod;

/**
 * A class that provides simplified access to TensorFlow Object Detection for the Rover Ruckus game (2018-2019).
 *
 * @author lizlooney@google.com (Liz Looney)
 */
public class TfodRoverRuckus extends TfodBase {
  public static final String TFOD_MODEL_ASSET = "RoverRuckus.tflite";

  public static final String LABEL_GOLD_MINERAL = "Gold Mineral";
  public static final String LABEL_SILVER_MINERAL = "Silver Mineral";

  public static final String[] LABELS = {
    LABEL_GOLD_MINERAL,
    LABEL_SILVER_MINERAL,
  };

  public TfodRoverRuckus() {
    super(TFOD_MODEL_ASSET, LABELS);
  }
}
