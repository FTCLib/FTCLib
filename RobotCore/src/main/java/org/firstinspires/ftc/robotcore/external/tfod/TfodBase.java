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
package org.firstinspires.ftc.robotcore.external.tfod;

import android.content.Context;
import java.util.List;
import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaBase;
import org.firstinspires.ftc.robotcore.external.tfod.Recognition;
import org.firstinspires.ftc.robotcore.external.tfod.TFObjectDetector;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

/**
 * An abstract base class that provides simplified access to TensorFlow Object Detection.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
public abstract class TfodBase {
  private final String assetName;
  private final String[] labels;

  private TFObjectDetector tfod;

  protected TfodBase(String assetName, String[] labels) {
    this.assetName = assetName;
    this.labels = labels;
  }

  /**
   * Initializes TensorFlow Object Detection.
   */
  public void initialize(VuforiaBase vuforiaBase, float minimumConfidence, boolean useObjectTracker,
      boolean enableCameraMonitoring) {
    TFObjectDetector.Parameters parameters = new TFObjectDetector.Parameters();
    parameters.minimumConfidence = minimumConfidence;
    parameters.useObjectTracker = useObjectTracker;
    if (enableCameraMonitoring) {
      Context context = AppUtil.getInstance().getRootActivity();
      parameters.tfodMonitorViewIdParent = context.getResources().getIdentifier(
          "tfodMonitorViewId", "id", context.getPackageName());
    }
    tfod = ClassFactory.getInstance().createTFObjectDetector(parameters, vuforiaBase.getVuforiaLocalizer());

    tfod.loadModelFromAsset(assetName, labels);
  }

  /**
   * Activates object detection.
   *
   * @throws IllegalStateException if initialized has not been called yet.
   */
  public void activate() {
    if (tfod == null) {
      throw new IllegalStateException("You forgot to call Tfod.initialize!");
    }
    tfod.activate();
  }

  /**
   * Deactivates object detection.
   *
   * @throws IllegalStateException if initialized has not been called yet.
   */
  public void deactivate() {
    if (tfod == null) {
      throw new IllegalStateException("You forgot to call Tfod.initialize!");
    }
    tfod.deactivate();
  }

  /**
   * Sets the number of pixels to obscure on the left, top, right, and bottom edges of each image
   * passed to the TensorFlow object detector. The size of the images are not changed, but the
   * pixels in the margins are colored black.
   */
  public void setClippingMargins(int left, int top, int right, int bottom) {
    if (tfod == null) {
      throw new IllegalStateException("You forgot to call Tfod.initialize!");
    }
    tfod.setClippingMargins(left, top, right, bottom);
  }

  /**
   * Returns the list of recognitions, but only if they are different than the last call to {@link #getUpdatedRecognitions()}.
   */
  public List<Recognition> getUpdatedRecognitions() {
    if (tfod == null) {
      throw new IllegalStateException("You forgot to call Tfod.initialize!");
    }
    return tfod.getUpdatedRecognitions();
  }

  /**
   * Returns the list of recognitions.
   */
  public List<Recognition> getRecognitions() {
    if (tfod == null) {
      throw new IllegalStateException("You forgot to call Tfod.initialize!");
    }
    return tfod.getRecognitions();
  }

  /**
   * Deactivates object detection and cleans up.
   */
  public void close() {
    if (tfod != null) {
      tfod.deactivate();
      tfod.shutdown();
      tfod = null;
    }
  }
}
