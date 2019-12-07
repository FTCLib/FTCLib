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

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;
import com.google.ftcresearch.tfod.util.ImageUtils;
import com.google.ftcresearch.tfod.util.Size;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.firstinspires.ftc.robotcore.external.tfod.Recognition;
import org.tensorflow.lite.Interpreter;

/**
 * Class to indefinitely read frames and return the most recent recognitions via a callback.
 *
 * <p>The TfodFrameManager constantly reads frames from the supplied {@link FrameGenerator}. These
 * frames are then periodically passed through a {@link RecognizeImageRunnable} to get the
 * recognitions for that frame asynchronously. The TfodFrameManager also passes all frames through a
 * {@link MultiBoxTracker}, if specified in the parameters. The resultant recognitions (from the
 * tracker or the runnable) are then passed into the {@link AnnotatedFrameCallback}.
 *
 * @author Vasu Agrawal
 * @author lizlooney@google.com (Liz Looney)
 */
class TfodFrameManager implements Runnable {

  private static final String TAG = "TfodFrameManager";
  private static final Paint paint = new Paint(); // Used to draw recognitions without tracker

  static {
    paint.setColor(Color.RED);
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(10);
  }

  // Parameters passed in to the constructor
  private final FrameGenerator frameGenerator;
  private final CameraInformation cameraInformation;
  private final List<Interpreter> interpreters;
  private final List<String> labels;
  private final TfodParameters params;
  private final AnnotatedFrameCallback tfodCallback;

  private final Size trackerFrameSize;
  private final Matrix originalToTrackerTransform;
  private final Matrix trackerToOriginalTransform;

  private final ExecutorService executor;
  private final RollingAverage averageInferenceTime;

  // Output locations for the interpreters, so we're not constantly reallocating
  private final List<float[][][]> outputLocations = new ArrayList<>();
  private final List<float[][]> outputClasses = new ArrayList<>();
  private final List<float[][]> outputScores = new ArrayList<>();
  private final List<float[]> numDetections = new ArrayList<>();

  private final Queue<Integer> availableIds = new ConcurrentLinkedQueue<>();
  private long lastSubmittedFrameTimeNanos;

  private final Object lastRecognizedFrameLock = new Object();
  private volatile AnnotatedYuvRgbFrame lastRecognizedFrame; // The most recent returned frame

  private final MultiBoxTracker tracker;
  private volatile boolean active;

  TfodFrameManager(
      FrameGenerator frameGenerator,
      List<Interpreter> interpreters,
      List<String> labels,
      TfodParameters params,
      AnnotatedFrameCallback tfodCallback) {
    this.frameGenerator = frameGenerator;
    this.cameraInformation = frameGenerator.getCameraInformation();
    this.interpreters = interpreters;
    this.labels = labels;
    this.params = params;
    this.tfodCallback = tfodCallback;

    trackerFrameSize = calculateTrackerFrameSize(params.inputSize, cameraInformation.size);
    originalToTrackerTransform = ImageUtils.transformBetweenImageSizes(
        cameraInformation.size, trackerFrameSize);
    trackerToOriginalTransform = ImageUtils.transformBetweenImageSizes(
        trackerFrameSize, cameraInformation.size);
    executor = Executors.newFixedThreadPool(params.numExecutorThreads);
    averageInferenceTime = new RollingAverage(params.timingBufferSize);
    tracker = params.trackerDisable ? null : new MultiBoxTracker(params);

    // Create the output arrays for the different interpreters
    for (int i = 0; i < params.numExecutorThreads; i++) {
      outputLocations.add(new float[1][params.maxNumDetections][4]);
      outputClasses.add(new float[1][params.maxNumDetections]);
      outputScores.add(new float[1][params.maxNumDetections]);
      numDetections.add(new float[1]);
    }

    // Mark all of the interpreters as available.
    for (int i = 0; i < params.numExecutorThreads; i++) {
      //Log.d(TAG, "Adding interpreter: " + i);
      availableIds.add(i);
    }

    // TODO(vasuagrawal): Do one inference task and get an inference time to use as a seed.
    // Make sure one inference task is done here before doing in the executor, so that we can
    // have a somewhat accurate estimate for the rollingAverage seed (which keeps all the
    // executors spaced evenly). The alternative is to just pick some reasonable value based on
    // experimental data (e.g. 300 ms), or just let it be 0 and let the system adjust automatically.
  }

  private static Size calculateTrackerFrameSize(int inputSize, Size cameraSize) {
    // The tracker frames should be smaller than the camera frames, but have the same aspect ratio
    // as the camera frames and each dimension should be larger than the input size.
    int smaller = Math.min(cameraSize.width, cameraSize.height);
    int larger = Math.max(cameraSize.width, cameraSize.height);
    long dim1 = inputSize + 1;
    while (dim1 < smaller) {
      if (dim1 * larger % smaller == 0) {
        break;
      }
      dim1++;
    }
    long dim2 = dim1 * larger / smaller;
    return (smaller == cameraSize.width)
        ? new Size((int) dim1, (int) dim2)
        : new Size((int) dim2, (int) dim1);
  }

  /** Transform all locations in the source list of recognitions by m, returning a new list */
  private List<Recognition> transformRecognitionLocations(List<Recognition> source, Matrix m) {
    List<Recognition> output = new ArrayList<>();
    for (Recognition recognition : source) {
      RecognitionImpl recognitionImpl = (RecognitionImpl) recognition;

      // It's fine to modify the location in place since getLocation() returns a copy.
      RectF location = recognitionImpl.getLocation();
      m.mapRect(location);

      output.add(new RecognitionImpl(recognitionImpl, location));
    }

    return output;
  }

  private void receiveNewRecognitions(AnnotatedYuvRgbFrame annotatedFrame) {
    synchronized (lastRecognizedFrameLock) {
      if (lastRecognizedFrame == null ||
          annotatedFrame.getFrameTimeNanos() > lastRecognizedFrame.getFrameTimeNanos()) {
        //Log.v(annotatedFrame.getTag(), "TfodFrameManager.receiveNewRecognitions - setting a new annotated frame.");
        lastRecognizedFrame = annotatedFrame;
      } else {
        Log.w(TAG, "Received an out of order recognition. Something is likely wrong!");
        return; // We don't want to process / send this frame anywhere.
      }
    }

    if (params.trackerDisable) {
      // No tracker. Directly calling tfod callback.
      tfodCallback.onResult(annotatedFrame);
    } else {
      // Sending received recognitions to the tracker.
      Timer timer = new Timer(annotatedFrame.getTag());
      timer.start("TfodFrameManager.receiveNewRecognitions - preprocessing for tracker update in receive");
      // To support tracker resizing, we need to get the resized frame and transform the
      // locations on the recognitions.
      byte[] luminosity = annotatedFrame.getFrame().getLuminosityArray(trackerFrameSize);

      // Convert the recognitions to the tracker frame coordinates
      List<Recognition> recognitionsInTrackerCoordinates =
          transformRecognitionLocations(annotatedFrame.getRecognitions(), originalToTrackerTransform);
      timer.end();

      tracker.trackResults(recognitionsInTrackerCoordinates, luminosity, annotatedFrame.getFrameTimeNanos());
    }
  }

  private void submitRecognitionTask(final AnnotatedYuvRgbFrame annotatedFrame) {
    // See if there's an interpreter available to handle this frame.
    final Integer interpreterId = availableIds.poll();

    if (interpreterId != null) { // There's actually an available interpreter, we will use it
      RecognizeImageRunnable task =
          new RecognizeImageRunnable(
              annotatedFrame,
              cameraInformation,
              interpreters.get(interpreterId),
              params,
              labels,
              outputLocations.get(interpreterId),
              outputClasses.get(interpreterId),
              outputScores.get(interpreterId),
              numDetections.get(interpreterId),
              new AnnotatedFrameCallback() {
                @Override
                public void onResult(AnnotatedYuvRgbFrame frame) {
                  // Note that frame and annotatedFrame are the same instance, but now it may have
                  // some recognitions.
                  long endTimeNanos = System.nanoTime();
                  long elapsedNanos = endTimeNanos - annotatedFrame.getFrameTimeNanos();
                  long elapsedMs = TimeUnit.MILLISECONDS.convert(elapsedNanos, TimeUnit.NANOSECONDS);
                  //Log.i(annotatedFrame.getTag(), "TfodFrameManager - interpreter [" + interpreterId + "] Ran for " + elapsedMs + " ms");

                  averageInferenceTime.add(elapsedNanos);
                  //Log.i(
                  //    annotatedFrame.getTag(),
                  //    "TfodFrameManager - average inference time is now "
                  //        + TimeUnit.MILLISECONDS.convert(
                  //            (long) averageInferenceTime.get(), TimeUnit.NANOSECONDS)
                  //        + "ms");

                  receiveNewRecognitions(annotatedFrame);

                  // Finally, mark the interpreter as available.
                  availableIds.add(interpreterId);
                }
              });

      //Log.i(annotatedFrame.getTag(), "TfodFrameManager.submitRecognitionTask - submitting recognition task with interpreter [" + interpreterId + "]");
      lastSubmittedFrameTimeNanos = annotatedFrame.getFrameTimeNanos();
      executor.submit(task);
    } else {
      // TODO(vasuagrawal): Add this to the statistics made available in the future (dropped frames)
      //Log.d(annotatedFrame.getTag(), "TfodFrameManager.submitRecognitionTask - no available interpreters");
    }
  }

  /**
   * Determine whether enough time has elapsed since we last submitted a frame.
   *
   * <p>The specified parameters for object detection may potentially allow more than a single
   * executor thread. In that event, it becomes possible to pipeline model evaluation (through
   * {@link RecognizeImageRunnable}) to minimize the latency between receiving recognitions. This
   * object maintains a RollingAverage of the compute time spent per frame. Dividing this average
   * latency by the number of executor threads yields a minimum time that should elapse between
   * frames submitted to the executor service to ensure that recognitions are being returned as
   * evenly spaced in time as possible, rather than in burst loads.
   *
   * @param frameTimeNanos Time (in nanoseconds) to determine if enough time has elapsed from.
   */
  private boolean enoughInterFrameTimeElapsed(final long frameTimeNanos) {

    final long elapsedNanos = frameTimeNanos - lastSubmittedFrameTimeNanos;
    final long minimumTimeNanos = (long) averageInferenceTime.get() / params.numExecutorThreads;

    return elapsedNanos >= minimumTimeNanos;
  }

  /**
   * Convert a frame to an appropriate format for the tracker (luminance) and submit it.
   *
   * @param annotatedFrame Annotated (with timestamp) input frame to send to tracker.
   */
  private void submitFrameToTracker(AnnotatedYuvRgbFrame annotatedFrame) {

    // Give the tracker a resized version of the current frame.
    byte[] luminosity = annotatedFrame.getFrame().getLuminosityArray(trackerFrameSize);

    final long frameTimeNanos = annotatedFrame.getFrameTimeNanos();

    tracker.onFrame(trackerFrameSize.width, trackerFrameSize.height, trackerFrameSize.width /* rowStride */,
        0 /* sensorOrientation */, luminosity, frameTimeNanos, cameraInformation);
    //Log.d(annotatedFrame.getTag(), "TfodFrameManager.submitFrameToTracker - Submitted frame to tracker");
  }

  /**
   * Constantly get frames, (maybe) pass them to an interpreter and through a tracker.
   *
   * <p>First, a frame is pulled from the frameGenerator. If enough time has elapsed since the last
   * time a frame was submitted {@see TfodFrameManager::enoughInterFrameTimeElapsed}, the current
   * frame gets submitted to an available interpreter (if any). Furthermore, every frame is passed
   * through the tracker, which is used to help interpolate recognitions between outputs from
   * interpreters, as well as to compensate for the latency of running the network. Finally, after
   * passing the frame through the tracker, the most recent list of recognitions (what the tracker
   * currently believes) is returned through the callback.
   */
  @Override
  public void run() {
    //Log.i(TAG, "Frame manager thread name: " + Thread.currentThread().getName());
    Timer timer = new Timer(TAG);

    while (!Thread.currentThread().isInterrupted()) {
      // First, grab the frame.
      timer.start("TfodFrameManager.run - Waiting for frame");
      final YuvRgbFrame frame;
      try {
        frame = frameGenerator.getFrame();
      } catch (IllegalStateException e) {
        Log.e(TAG, "TfodFrameManager.run - could not get image from frame generator");
        continue;
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }
      timer.end();

      //Log.d(frame.getTag(), "TfodFrameManager.run - got an image from frame generator");
      AnnotatedYuvRgbFrame annotatedFrame = new AnnotatedYuvRgbFrame(frame, new ArrayList<Recognition>());

      if (!active) {
        tfodCallback.onResult(annotatedFrame);
        continue;
      }

      // Then determine if we've waited long enough to submit the frame
      if (enoughInterFrameTimeElapsed(frame.getFrameTimeNanos())) {
        //Log.i(frame.getTag(), "TfodFrameManager.run - trying to submit recognition task
        //(pending interpreter)");
        Timer frameTimer = new Timer(frame.getTag());
        frameTimer.start("TfodFrameManager.run - submitting recognition task");
        submitRecognitionTask(annotatedFrame);
        frameTimer.end();
      } else {
        //Log.d(frame.getTag(), "TfodFrameManager.run - not enough time has elapsed");
      }

      // If the tracker isn't disabled, feed it the newest frame, and then pass the results back up.
      if (!params.trackerDisable) {
        submitFrameToTracker(annotatedFrame);
        tracker.printResults();

        Timer frameTimer = new Timer(annotatedFrame.getTag());
        frameTimer.start("TfodFrameManager.run - preprocessing for tracker in main loop");

        // Map the recognitions back to original coordinates.
        final List<Recognition> recognitions =
            transformRecognitionLocations(tracker.getRecognitions(), trackerToOriginalTransform);
        frameTimer.end();

        tfodCallback.onResult(new AnnotatedYuvRgbFrame(frame, recognitions));
      }
    }

    // Make sure we clean up executor before returning from this thread.
    if (!executor.isShutdown()) {
      executor.shutdown();
      try {
        executor.awaitTermination(100, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  /** Draw both normal and debug information to the canvas. */
  void drawDebug(Canvas canvas) {
    if (!active) {
      return;
    }

    // Draw first so debug information gets put on top.
    draw(canvas);

    if (!params.trackerDisable) {
      tracker.drawDebug(canvas);
    } // There's no debug information without the tracker
  }

  /**
   * Only draw recognitions to the canvas.
   *
   * If the tracker is enabled, this will pass through to the tracker's draw implementation,
   * which uses different colors for each tracked object (changing colors when a new association
   * is made). If the tracker is not enabled, each object will simply be boxed in red.
   * */
  void draw(Canvas canvas) {
    if (!active) {
      return;
    }

    if (!params.trackerDisable) {
      tracker.draw(canvas);
    } else {
      final AnnotatedYuvRgbFrame annotatedFrame = lastRecognizedFrame;

      if (annotatedFrame != null) {
        for (Recognition recognition : annotatedFrame.getRecognitions()) {
          RecognitionImpl recognitionImpl = (RecognitionImpl) recognition;
          canvas.drawRect(recognitionImpl.getLocation(), paint);
        }
      }
    }
  }

  void activate() {
    active = true;
  }

  void deactivate() {
    active = false;
  }
}
