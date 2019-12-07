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

import android.app.Activity;
import android.support.annotation.NonNull;

/**
 * FrameGenerator provides a unified interface for receiving consecutive frames from a sequence.
 *
 * <p>Typically, consecutive frames will be different (as if generated from a video, camera, or
 * other time-dependent sequence), but this is not required by the interface.
 *
 * @author Vasu Agrawal
 */
interface FrameGenerator {

  CameraInformation getCameraInformation();

  /**
   * Get the next frame in the sequence, blocking if none are available.
   *
   * @return The next frame. Never null.
   */
  @NonNull YuvRgbFrame getFrame() throws InterruptedException;

  /** Method to serve as a destructor to release any important resources (e.g. camera). */
  void shutdown();
}
