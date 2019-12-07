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

import com.google.ftcresearch.tfod.util.Size;
import java.util.Objects;

/**
 * Class representing information about the camera needed for object detection.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class CameraInformation {
  final int rotation; // in degrees, must be 0, 90, 180, or 270
  final float horizontalFocalLength; // in pixels
  final float verticalFocalLength; // in pixels
  final Size size; // in pixels

  CameraInformation(int rotation, float horizontalFocalLength, float verticalFocalLength,
      int width, int height) {
    this.rotation = rotation;
    this.horizontalFocalLength = horizontalFocalLength;
    this.verticalFocalLength = verticalFocalLength;
    this.size = new Size(width, height);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CameraInformation ci = (CameraInformation) o;
    return rotation == ci.rotation
        && horizontalFocalLength == ci.horizontalFocalLength
        && verticalFocalLength == ci.verticalFocalLength
        && size.equals(ci.size);
  }

  @Override
  public int hashCode() {
    return Objects.hash(rotation, horizontalFocalLength, verticalFocalLength, size);
  }
}
