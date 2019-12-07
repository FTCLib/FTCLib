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

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.firstinspires.ftc.robotcore.external.function.Consumer;
import org.firstinspires.ftc.robotcore.external.function.Continuation;
import org.firstinspires.ftc.robotcore.external.function.ContinuationResult;
import org.firstinspires.ftc.robotcore.external.hardware.camera.BuiltinCameraName;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraName;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer.CameraDirection;
import org.firstinspires.ftc.robotcore.external.stream.CameraStreamServer;
import org.firstinspires.ftc.robotcore.external.tfod.Recognition;
import org.firstinspires.ftc.robotcore.external.tfod.TFObjectDetector;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.tensorflow.lite.Interpreter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to convert object detection and tracking system into a simple interface.
 *
 * <p>TFObjectDetector makes it easy to detect and track objects in real time. After initialization,
 * clients simply call getRecognitions() as often as they wish to get the recognitions corresponding
 * to the most recent frame which has been processed. Recognitions contain information about the
 * location, class, and detection confidence of each particular object.
 *
 * <p>Advanced users may wish to tune the performance of the TFObjectDetector by changing parameters
 * away from the defaults in {@link Parameters}. Not all parameters will make a measurable impact
 * on performance.
 *
 * @author Vasu Agrawal
 * @author lizlooney@google.com (Liz Looney)
 */
public class TFObjectDetectorImpl implements TFObjectDetector {

  private static final String TAG = "TFObjectDetector";

  private final AppUtil appUtil = AppUtil.getInstance();
  private final List<Interpreter> interpreters = new ArrayList<>();
  private final List<String> labels = new ArrayList<>();

  private final ClippingMargins clippingMargins = new ClippingMargins();

  // Parameters passed in through the constructor.
  private TfodParameters params;
  private VuforiaLocalizer vuforiaLocalizer;

  private Rate rate;
  private final int rotation;
  private FrameGenerator frameGenerator;

  private ViewGroup imageViewParent;
  private ImageView imageView;
  private FrameLayout.LayoutParams imageViewLayoutParams;

  // Parameters created during initialization.
  private TfodFrameManager frameManager;
  private Thread frameManagerThread;

  // Store all of the data relevant to a set of recognitions together in an AnnotatedYuvRgbFrame,
  // guarded by annotatedFrameLock for when multiple callbacks attempt to update the
  // annotatedFrame, or multiple clients attempt to access it.
  private final Object annotatedFrameLock = new Object();
  private AnnotatedYuvRgbFrame annotatedFrame;
  private long lastReturnedFrameTime = 0;

  private final Object bitmapFrameLock = new Object();
  private Continuation<? extends Consumer<Bitmap>> bitmapContinuation;

  /**
   * Return true if this device is compatible with TensorFlow Object Detection, false otherwise.
   */
  public static boolean isDeviceCompatible() {
    // Requires Android 6.0+
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
  }

  public TFObjectDetectorImpl(Parameters parameters, VuforiaLocalizer vuforiaLocalizer) {

    this.params = makeTfodParameters(parameters);
    this.vuforiaLocalizer = vuforiaLocalizer;

    rate = new Rate(params.maxFrameRate);

    Activity activity = (parameters.activity != null)
        ? parameters.activity
        : appUtil.getRootActivity();

    rotation = getRotation(activity, vuforiaLocalizer.getCameraName());
    frameGenerator = new VuforiaFrameGenerator(vuforiaLocalizer, rotation, clippingMargins);

    createImageViewIfRequested(activity, parameters);

    // Initialize the stored frame to something non-null. This also ensures that any asynchronous
    // setup being done in the frameGenerator gets done before the frame manager starts, so there's
    // no unexpected delays there.
    for (int i = 10; i >= 0; i--) {
      try {
        YuvRgbFrame frame = frameGenerator.getFrame();
        annotatedFrame = new AnnotatedYuvRgbFrame(frame, new ArrayList<Recognition>());
        break;
      } catch (IllegalStateException e) {
        Log.e(TAG, "TFObjectDetectorImpl.<init> - could not get image from frame generator");
        if (i == 0) {
          throw e;
        }
        continue;
      } catch (InterruptedException e) {
        // TODO(vasuagrawal): Figure out if this is the right exception / behavior.
        throw new RuntimeException("TFObjectDetector constructor interrupted while getting first frame!");
      }
    }

    if (imageView != null) {
      updateImageView(annotatedFrame);
    }

    CameraStreamServer.getInstance().setSource(this);
  }

  private static TfodParameters makeTfodParameters(Parameters parameters) {
    return new TfodParameters.Builder()
        .minResultConfidence((float) parameters.minimumConfidence)
        .trackerDisable(!parameters.useObjectTracker)
        .build();
  }

  private static int getRotation(Activity activity, CameraName cameraName) {
    int rotation = 0;

    if (cameraName instanceof BuiltinCameraName) {
      int displayRotation = 0;
      switch (activity.getWindowManager().getDefaultDisplay().getRotation()) {
        case Surface.ROTATION_0: displayRotation = 0; break;
        case Surface.ROTATION_90: displayRotation = 90; break;
        case Surface.ROTATION_180: displayRotation = 180; break;
        case Surface.ROTATION_270: displayRotation = 270; break;
      }

      CameraDirection cameraDirection = ((BuiltinCameraName) cameraName).getCameraDirection();

      for (int cameraId = 0; cameraId < Camera.getNumberOfCameras(); cameraId++) {
        CameraInfo cameraInfo = new CameraInfo();
        Camera.getCameraInfo(cameraId, cameraInfo);

        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT && cameraDirection == CameraDirection.FRONT) {
          rotation = - displayRotation - cameraInfo.orientation;
          break;
        }
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK && cameraDirection == CameraDirection.BACK) {
          rotation = displayRotation - cameraInfo.orientation;
          break;
        }
      }
    }

    while (rotation < 0) {
      rotation += 360;
    }
    rotation %= 360;
    return rotation;
  }

  private void createImageViewIfRequested(final Activity activity, Parameters parameters) {
    if (parameters.tfodMonitorViewParent != null) {
      imageViewParent = parameters.tfodMonitorViewParent;
    } else if (parameters.tfodMonitorViewIdParent != 0) {
      imageViewParent = (ViewGroup) activity.findViewById(parameters.tfodMonitorViewIdParent);
    }

    if (imageViewParent != null) {
      appUtil.synchronousRunOnUiThread(new Runnable() {
        @Override
        public void run() {
          imageView = new ImageView(activity);
          imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
          imageView.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
          imageViewLayoutParams = null;
          if (rotation != 0) {
            imageView.setRotation(360 - rotation);
          }
          imageViewParent.addView(imageView);
          imageViewParent.setVisibility(VISIBLE);
        }
      });
    }
  }

  @Override
  public void loadModelFromAsset(String assetName, String... labels) {
    try {
      AssetManager assetManager = AppUtil.getDefContext().getAssets();
      AssetFileDescriptor afd = assetManager.openFd(assetName);
      try (FileInputStream fis = afd.createInputStream()) {
        initialize(fis, afd.getStartOffset(), afd.getDeclaredLength(), labels);
      }
    } catch (IOException e) {
      throw new RuntimeException("TFObjectDetector loadModelFromAsset failed", e);
    }
  }

  @Override
  public void loadModelFromFile(String absoluteFileName, String... labels) {
    try {
      File file = new File(absoluteFileName);
      try (FileInputStream fis = new FileInputStream(absoluteFileName)) {
        initialize(fis, 0, file.length(), labels);
      }
    } catch (IOException e) {
      throw new RuntimeException("TFObjectDetector loadModelFromFile failed", e);
    }
  }

  private void initialize(FileInputStream fileInputStream, long startOffset, long declaredLength,
      String... labels) throws IOException {
    // Load the model.
    MappedByteBuffer modelData = fileInputStream.getChannel()
        .map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);

    // Add the given labels to our List.
    for (String label : labels) {
      this.labels.add(label);
    }

    // Create the interpreters.
    for (int i = 0; i < params.numExecutorThreads; i++) {
      interpreters.add(new Interpreter(modelData, params.numInterpreterThreads));
    }

    // Create a TfodFrameManager, which handles feeding tasks to the executor. Each task consists
    // of processing a single camera frame, passing it through the model (via the interpreter),
    // and returning a list of recognitions.
    frameManager = new TfodFrameManager(
        frameGenerator,
        interpreters,
        this.labels,
        params,
        new AnnotatedFrameCallback() {
          @Override
          public void onResult(AnnotatedYuvRgbFrame receivedAnnotatedFrame) {
            synchronized (annotatedFrameLock) {
              //Log.v(receivedAnnotatedFrame.getTag(), "TFObjectDetectorImpl callback - frame change: setting a new annotatedFrame");
              annotatedFrame = receivedAnnotatedFrame;
            }

            if (imageView != null) {
              updateImageView(receivedAnnotatedFrame);
            }
          }
        });
    //Log.i(TAG, "Starting frame manager thread");
    frameManagerThread = new Thread(frameManager, "FrameManager");
    frameManagerThread.start();
  }

  private void updateImageView(final AnnotatedYuvRgbFrame receivedAnnotatedFrame) {
    final Bitmap bitmap = receivedAnnotatedFrame.getFrame().getCopiedBitmap();
    Canvas canvas = new Canvas(bitmap);
    if (frameManager != null) {
      // Draw recognitions onto the screen.
      frameManager.drawDebug(canvas);
    }

    synchronized (bitmapFrameLock) {
      if (bitmapContinuation != null) {
        bitmapContinuation.dispatch(new ContinuationResult<Consumer<Bitmap>>() {
          @Override
          public void handle(Consumer<Bitmap> bitmapConsumer) {
            bitmapConsumer.accept(bitmap);
          }
        });
        bitmapContinuation = null;
      }
    }

    appUtil.synchronousRunOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (imageView != null) {
          if (imageViewLayoutParams == null) {
            double width = bitmap.getWidth();
            double height = bitmap.getHeight();
            if (rotation % 180 != 0) {
              double swap = width;
              width = height;
              height = swap;
            }
            double scale = Math.min(imageView.getWidth() / width, imageView.getHeight() / height);
            width *= scale;
            height *= scale;

            if (rotation % 180 != 0) {
              double swap = width;
              width = height;
              height = swap;
            }

            imageViewLayoutParams = new FrameLayout.LayoutParams((int) width, (int) height, Gravity.CENTER);
            imageView.setLayoutParams(imageViewLayoutParams);
          }
          imageView.setImageBitmap(bitmap);
          imageView.invalidate();
        }
      }
    });
  }

  /**
   * Activates this TFObjectDetector so it starts recognizing objects.
   */
  @Override
  public void activate() {
    if (frameManager != null) {
      frameManager.activate();
    }
  }

  /**
   * Deactivates this TFObjectDetector so it stops recognizing objects.
   */
  @Override
  public void deactivate() {
    if (frameManager != null) {
      frameManager.deactivate();
    }
  }

  @Override
  public void setClippingMargins(int left, int top, int right, int bottom) {
    synchronized (clippingMargins) {
      switch (rotation) {
        default:
          throw new IllegalStateException("rotation must be 0, 90, 180, or 270.");
        case 0:
          clippingMargins.left = left;
          clippingMargins.top = top;
          clippingMargins.right = right;
          clippingMargins.bottom = bottom;
          break;
        case 90:
          clippingMargins.left = bottom;
          clippingMargins.top = left;
          clippingMargins.right = top;
          clippingMargins.bottom = right;
          break;
        case 180:
          clippingMargins.left = right;
          clippingMargins.top = bottom;
          clippingMargins.right = left;
          clippingMargins.bottom = top;
          break;
        case 270:
          clippingMargins.left = top;
          clippingMargins.top = right;
          clippingMargins.right = bottom;
          clippingMargins.bottom = left;
          break;
      }
    }
  }

  /**
   * Get the most recent AnnotatedYuvRgbFrame available, at a maximum predetermined frame rate.
   *
   * Internally, the library gets frames asynchronously. To help clients behave more predictibly,
   * this function makes the most recent frame received by the library available at a specified
   * frame rate. If the requested frame rate is higher than the rate at which the library is
   * receiving frames, the same frame will be returned multiple times.
   *
   * The client is free to modify the contents of the AnnotatedYuvRgbFrame. However, note that
   * any changes will persist if the same frame is returned multiple times by this method.
   *
   * This method will never return a null frame, since a frame is acquired during initialization.
   *
   * @return Newest available AnnotatedYuvRgbFrame.
   */
  private @NonNull AnnotatedYuvRgbFrame getAnnotatedFrameAtRate() {
    rate.sleep();

    synchronized (annotatedFrameLock) {
      return annotatedFrame;
    }
  }

  private @NonNull AnnotatedYuvRgbFrame getAnnotatedFrame() {
    synchronized (annotatedFrameLock) {
      return annotatedFrame;
    }
  }

  /**
   * Return a new AnnotatedYuvRgbFrame or null if a new one isn't available.
   *
   * If a new frame has arrived since the last time this method was called, it will be returned.
   * Otherwise, null will be returned.
   *
   * Note that this method still takes a lock internally, and thus calling this method too
   * frequently may degrade performance of the detector.
   *
   * @return A new frame if one is available, null otherwise.
   */
  private AnnotatedYuvRgbFrame getUpdatedAnnotatedFrame() {
    synchronized (annotatedFrameLock) {
      // Can only do this safely because we know the annotatedFrame can never be null after the
      // constructor has happened.
      if (annotatedFrame.getFrameTimeNanos() > lastReturnedFrameTime) {
        lastReturnedFrameTime = annotatedFrame.getFrameTimeNanos();
        return annotatedFrame;
      }
    }

    return null;
  }

  @Override
  public void getFrameBitmap(Continuation<? extends Consumer<Bitmap>> continuation) {
    synchronized (bitmapFrameLock) {
      bitmapContinuation = continuation;
    }
  }

  private static List<Recognition> makeRecognitionsList(@NonNull AnnotatedYuvRgbFrame frame) {
    return new ArrayList<Recognition>(frame.getRecognitions());
  }

  @Override
  public List<Recognition> getUpdatedRecognitions() {
    AnnotatedYuvRgbFrame frame = getUpdatedAnnotatedFrame();
    if (frame == null) {
      return null;
    }
    return makeRecognitionsList(frame);
  }

  @Override
  public List<Recognition> getRecognitions() {
    return makeRecognitionsList(getAnnotatedFrame());
  }

  /**
   * Perform whatever cleanup is necessary to release all acquired resources.
   */
  @Override
  public void shutdown() {
    Thread currentThread = Thread.currentThread();
    boolean interrupted = currentThread.interrupted();

    deactivate();

    frameManagerThread.interrupt();
    try {
      frameManagerThread.join();
    } catch (InterruptedException e) {
      interrupted = true;
    }

    // If we've been asked to draw to the screen, remove the image view.
    if (imageView != null) {
      appUtil.synchronousRunOnUiThread(new Runnable() {
        @Override
        public void run() {
          imageViewParent.removeView(imageView);
          imageViewParent.setVisibility(GONE);
        }
      });
    }

    frameGenerator.shutdown();

    if (interrupted) {
      currentThread.interrupt();
    }
  }
}
