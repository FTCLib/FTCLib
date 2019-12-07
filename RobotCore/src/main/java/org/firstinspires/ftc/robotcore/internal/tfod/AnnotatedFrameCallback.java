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

/**
 * Callback from a function which produces an AnnotatedYuvRgbFrame
 *
 * Note that this interface makes no guarantees about the ownership or mutability of the
 * annotatedFrame. Each worker (the object calling this callback) is free to determine its own
 * contract for usage. Consult the appropriate documentation for more details.
 *
 * @author Vasu Agrawal
 */
interface AnnotatedFrameCallback {

  /** Return AnnotatedYuvRgbFrame on upon successful completion of computation. */
  void onResult(AnnotatedYuvRgbFrame annotatedFrame);
}
