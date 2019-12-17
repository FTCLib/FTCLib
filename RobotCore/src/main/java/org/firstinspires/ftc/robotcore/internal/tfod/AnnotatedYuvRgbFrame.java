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

import android.support.annotation.NonNull;
import java.util.List;
import org.firstinspires.ftc.robotcore.external.tfod.Recognition;

/**
 * Keep frames, frame times, and recognitions together in a single class.
 *
 * This class is <b>not</b> immutable. A client is expected and able to modify the contained List
 * of Recognitions to add new annotations as necessary. It is unexpected, though possible, for a
 * client to modify the contents of the contained YuvRgbFrame. However, modifying the YuvRgbFrame
 * is not advised.
 *
 * @author Vasu Agrawal
 * @author lizlooney@google.com (Liz Looney)
 */
public class AnnotatedYuvRgbFrame {

  private final YuvRgbFrame frame;
  private final List<Recognition> recognitions;

  public AnnotatedYuvRgbFrame(@NonNull YuvRgbFrame frame, @NonNull List<Recognition> recognitions) {
    this.frame = frame;
    this.recognitions = recognitions;
  }

  public YuvRgbFrame getFrame() {
    return frame;
  }

  /** These must be in sorted order, decreasing by confidence. */
  public List<Recognition> getRecognitions() {
    return recognitions;
  }

  public long getFrameTimeNanos() {
    return frame.getFrameTimeNanos();
  }

  public String getTag() {
    return frame.getTag();
  }
}
