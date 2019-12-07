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
package org.firstinspires.ftc.robotcore.internal.tfod;

import android.app.Activity;
import com.google.ftcresearch.tfod.util.Size;
import com.vuforia.Image;
import com.vuforia.PIXEL_FORMAT;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;

/**
 * An Implementation of FrameGenerator where the frames are retrieved from the Vuforia frame queue.
 *
 * @author Vasu Agrawal
 * @author lizlooney@google.com (Liz Looney)
 */
public class VuforiaFrameGenerator implements FrameGenerator {

  private static final String TAG = "VuforiaFrameGenerator";

  private final BlockingQueue<VuforiaLocalizer.CloseableFrame> frameQueue;
  private final CameraInformation cameraInformation;
  private final ClippingMargins clippingMargins;

  public VuforiaFrameGenerator(VuforiaLocalizer vuforia, int rotation, ClippingMargins clippingMargins) {
    // We only use RGB565, but if I don't include YUV, the Vuforia camera monitor looks crazy.
    boolean[] results = vuforia.enableConvertFrameToFormat(PIXEL_FORMAT.RGB565, PIXEL_FORMAT.YUV);
    if (!results[0]) { // Failed to get Vuforia to convert to RGB565.
      throw new RuntimeException("Unable to convince Vuforia to generate RGB565 frames!");
    }

    vuforia.setFrameQueueCapacity(1);
    frameQueue = vuforia.getFrameQueue();

    // Vuforia returns the focal length in pixels, which is exactly what we need!
    float[] focalLength = vuforia.getCameraCalibration().getFocalLength().getData();

    float[] size = vuforia.getCameraCalibration().getSize().getData();

    // NOTE(lizlooney): If the focal length is not available, we can calculate it from the size and
    // field of view, like this:
    /*
    float[] fieldOfView = vuforia.getCameraCalibration().getFieldOfViewRads().getData();
    float[] focalLength = new float[2];
    focalLength[0] = 0.5 * size[0] / Math.tan(0.5 * fieldOfView[0]);
    focalLength[1] = 0.5 * size[1] / Math.tan(0.5 * fieldOfView[1]);
    */

    cameraInformation = new CameraInformation(rotation, focalLength[0], focalLength[1], (int) size[0], (int) size[1]);

    this.clippingMargins = clippingMargins;
  }

  @Override
  public CameraInformation getCameraInformation() {
    return cameraInformation;
  }

  @Override
  public YuvRgbFrame getFrame() throws InterruptedException {

    VuforiaLocalizer.CloseableFrame vuforiaFrame = frameQueue.take();
    long frameTimeNanos = System.nanoTime();

    for (int i = 0; i < vuforiaFrame.getNumImages(); i++) {
      Image image = vuforiaFrame.getImage(i);
      if (image.getFormat() == PIXEL_FORMAT.RGB565) {
        return new YuvRgbFrame(frameTimeNanos, cameraInformation.size, image.getPixels(),
            clippingMargins);
      }
    }

    // We can't return a null frame, so this is the responsible thing to do.
    throw new IllegalStateException("Didn't find an RGB565 image from Vuforia!");
  }

  @Override
  public void shutdown() {
  }
}
