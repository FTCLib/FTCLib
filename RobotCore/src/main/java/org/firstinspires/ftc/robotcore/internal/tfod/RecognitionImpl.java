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

package org.firstinspires.ftc.robotcore.internal.tfod;

import android.graphics.RectF;
import android.support.annotation.NonNull;
import java.util.Objects;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.tfod.Recognition;


/**
 * An immutable result describing a single instance of an object recognized by the model.
 *
 * <p>A given frame may have multiple recognitions, with each recognition corresponding to a
 * different instance of an object.
 *
 * @author Vasu Agrawal
 * @author lizlooney@google.com (Liz Looney)
 */
class RecognitionImpl implements Recognition {

  private final @NonNull CameraInformation cameraInformation;
  private final @NonNull String label;
  private final float confidence;
  private final @NonNull RectF location;

  // The following fields are adjusted based on the rotation of the camera.
  private final @NonNull RectF updatedLocation;
  private final float frameHorizontalFocalLength;
  private final int frameWidth;
  private final int frameHeight;

  RecognitionImpl(@NonNull RecognitionImpl recognition, @NonNull RectF location) {
    this(recognition.cameraInformation, recognition.label, recognition.confidence, location);
  }

  RecognitionImpl(@NonNull CameraInformation cameraInformation,
      @NonNull String label, float confidence, @NonNull RectF location) {
    this.cameraInformation = cameraInformation;
    this.label = label;
    this.confidence = confidence;
    this.location = location;

    updatedLocation = new RectF(location);
    switch (cameraInformation.rotation) {
      default:
        throw new IllegalArgumentException("CameraInformation.rotation must be 0, 90, 180, or 270.");
      case 0:
        frameHorizontalFocalLength = cameraInformation.horizontalFocalLength;
        frameWidth = cameraInformation.size.width;
        frameHeight = cameraInformation.size.height;
        break;
      case 90:
        frameHorizontalFocalLength = cameraInformation.verticalFocalLength;
        frameWidth = cameraInformation.size.height;
        frameHeight = cameraInformation.size.width;
        updatedLocation.left = location.top;
        updatedLocation.right = location.bottom;
        updatedLocation.top = cameraInformation.size.width - location.right;
        updatedLocation.bottom = cameraInformation.size.width - location.left;
        break;
      case 180:
        frameHorizontalFocalLength = cameraInformation.horizontalFocalLength;
        frameWidth = cameraInformation.size.width;
        frameHeight = cameraInformation.size.height;
        updatedLocation.left = cameraInformation.size.width - location.right;
        updatedLocation.right = cameraInformation.size.width - location.left;
        updatedLocation.top = cameraInformation.size.height - location.bottom;
        updatedLocation.bottom = cameraInformation.size.height - location.top;
        break;
      case 270:
        frameHorizontalFocalLength = cameraInformation.verticalFocalLength;
        frameWidth = cameraInformation.size.height;
        frameHeight = cameraInformation.size.width;
        updatedLocation.left = cameraInformation.size.height - location.bottom;
        updatedLocation.right = cameraInformation.size.height - location.top;
        updatedLocation.top = location.left;
        updatedLocation.bottom = location.right;
        break;
    }
  }

  RectF getLocation() {
    return location;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof RecognitionImpl)) {
      return false;
    }

    RecognitionImpl other = (RecognitionImpl) o;
    return confidence == other.confidence
        && label.equals(other.label)
        && location.equals(other.location);
  }

  @Override
  public int hashCode() {
    return Objects.hash(confidence, label, location);
  }

  @Override
  public String toString() {
    return String.format(
        "Recognition(label=%s, confidence=%.3f, left=%.0f, right=%.0f, top=%.0f, bottom=%.0f",
        label,
        confidence,
        updatedLocation.left, updatedLocation.right, updatedLocation.top, updatedLocation.bottom);
  }

  @Override
  public String getLabel() {
    return label;
  }

  @Override
  public float getConfidence() {
    return confidence;
  }

  @Override
  public float getLeft() {
    return updatedLocation.left;
  }

  @Override
  public float getRight() {
    return updatedLocation.right;
  }

  @Override
  public float getTop() {
    return updatedLocation.top;
  }

  @Override
  public float getBottom() {
    return updatedLocation.bottom;
  }

  @Override
  public float getWidth() {
    return updatedLocation.width();
  }

  @Override
  public float getHeight() {
    return updatedLocation.height();
  }

  @Override
  public int getImageWidth() {
    return frameWidth;
  }

  @Override
  public int getImageHeight() {
    return frameHeight;
  }

  @Override
  public double estimateAngleToObject(AngleUnit angleUnit) {
    /**
     * The distance from the camera to the center of the image, in pixels.
     */
    double adjacentSideLength = frameHorizontalFocalLength;

    /**
     * The horizonal distance, from the object to the center of the image, in pixels.
     * This will be negative for objects on the left, positive for objects on the right.
     */
    double oppositeSideLength = updatedLocation.centerX() - 0.5f * frameWidth;

    double tangent = oppositeSideLength / adjacentSideLength;
    double angle = angleUnit.fromRadians(Math.atan(tangent));
    return angle;
  }
}
