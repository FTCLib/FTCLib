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
package org.firstinspires.ftc.robotcore.external.navigation;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Looper;
import android.text.TextUtils;

import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraName;
import org.firstinspires.ftc.robotcore.external.matrices.OpenGLMatrix;
import org.firstinspires.ftc.robotcore.external.matrices.VectorF;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer.CameraDirection;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer.Parameters;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer.Parameters.CameraMonitorFeedback;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An abstract base class that provides simplified access to Vuforia.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
public abstract class VuforiaBase {
  public static final float MM_PER_INCH = 25.4f;
  public static final float MM_FTC_FIELD_WIDTH = (12 * 12 - 2) * MM_PER_INCH;

  private static final String LICENSE_KEY_FILE = "CzechWolf";

  private final String assetName;
  private final String[] trackableNames;
  private final Map<String, OpenGLMatrix> locationsOnField = new HashMap<>();
  private volatile VuforiaLocalizer vuforiaLocalizer;
  private volatile VuforiaTrackables vuforiaTrackables;
  private final Map<String, VuforiaTrackableDefaultListener> listenerMap = new HashMap<>();
  private final Map<String, OpenGLMatrix> locationMap = new HashMap<>();
  private final Map<String, OpenGLMatrix> poseMap = new HashMap<>();

  public static class TrackingResults {
    public String name;
    public boolean isVisible;
    public boolean isUpdatedRobotLocation;
    public OpenGLMatrix matrix;
    public float x;
    public float y;
    public float z;
    public float xAngle;
    public float yAngle;
    public float zAngle;

    TrackingResults(String name) {
      this.name = name;
    }

    TrackingResults(String name, boolean isVisible, boolean isUpdatedRobotLocation, OpenGLMatrix matrix) {
      this.name = name;
      this.isVisible = isVisible;
      this.isUpdatedRobotLocation = isUpdatedRobotLocation;
      this.matrix = matrix;
      if (matrix != null) {
        VectorF translation = matrix.getTranslation();
        Orientation orientation = Orientation.getOrientation(
            matrix, AxesReference.EXTRINSIC, AxesOrder.XYZ, AngleUnit.DEGREES);
        x = translation.get(0);
        y = translation.get(1);
        z = translation.get(2);
        xAngle = orientation.firstAngle;
        yAngle = orientation.secondAngle;
        zAngle = orientation.thirdAngle;
      }
    }

    protected TrackingResults(TrackingResults other) {
      this(other.name, other.isVisible, other.isUpdatedRobotLocation, other.matrix);
    }

    public String formatAsTransform() {
      if (matrix != null) {
        return matrix.formatAsTransform();
      }
      return "";
    }

    public String toJson() {
      return "{ \"Name\":\"" + name + "\"" +
          ", \"IsVisible\":" + isVisible +
          ", \"IsUpdatedRobotLocation\":" + isUpdatedRobotLocation +
          ", \"X\":" + x +
          ", \"Y\":" + y +
          ", \"Z\":" + z +
          ", \"XAngle\":" + xAngle +
          ", \"YAngle\":" + yAngle +
          ", \"ZAngle\":" + zAngle + " }";
    }
  }

  protected VuforiaBase(String assetName, String[] trackableNames, OpenGLMatrix[] locationsOnField) {
    this.assetName = assetName;
    this.trackableNames = trackableNames;
    for (int i = 0; i < trackableNames.length; i++) {
      String name = trackableNames[i];
      this.locationsOnField.put(name, locationsOnField[i]);
    }
  }

  protected VuforiaBase(String assetName, String[] trackableNames, Map<String, OpenGLMatrix> locationsOnField) {
    this.assetName = assetName;
    this.trackableNames = trackableNames;
    this.locationsOnField.putAll(locationsOnField);
  }

  /**
   * Initializes Vuforia, with a CameraDirection.
   */
  public void initialize(String vuforiaLicenseKey, CameraDirection cameraDirection,
      boolean useExtendedTracking, boolean enableCameraMonitoring,
      CameraMonitorFeedback cameraMonitorFeedback,
      float dx, float dy, float dz, float xAngle, float yAngle, float zAngle,
      boolean useCompetitionFieldTargetLocations) {
    Parameters parameters = createParametersWithoutCamera(vuforiaLicenseKey,
        useExtendedTracking, enableCameraMonitoring, cameraMonitorFeedback);
    parameters.cameraDirection = cameraDirection;
    initialize(parameters, dx, dy, dz, xAngle, yAngle, zAngle, useCompetitionFieldTargetLocations);
  }

  /**
   * Initializes Vuforia, with a CameraName.
   */
  public void initialize(String vuforiaLicenseKey, CameraName cameraName, String webcamCalibrationFilename,
      boolean useExtendedTracking, boolean enableCameraMonitoring, CameraMonitorFeedback cameraMonitorFeedback,
      float dx, float dy, float dz, float xAngle, float yAngle, float zAngle, boolean useCompetitionFieldTargetLocations) {
    Parameters parameters = createParametersWithoutCamera(vuforiaLicenseKey,
        useExtendedTracking, enableCameraMonitoring, cameraMonitorFeedback);
    parameters.cameraName = cameraName;
    if (!TextUtils.isEmpty(webcamCalibrationFilename)) {
      parameters.addWebcamCalibrationFile(webcamCalibrationFilename);
    }
    initialize(parameters, dx, dy, dz, xAngle, yAngle, zAngle, useCompetitionFieldTargetLocations);
  }

  private void initialize(final Parameters parameters,
      float dx, float dy, float dz, float xAngle, float yAngle, float zAngle,
      boolean useCompetitionFieldTargetLocations) {
    OpenGLMatrix locationOnRobot = OpenGLMatrix
        .translation(dx, dy, dz)
        .multiplied(Orientation.getRotationMatrix(
            AxesReference.EXTRINSIC, AxesOrder.XYZ, AngleUnit.DEGREES, xAngle, yAngle, zAngle));

    Looper looper = Looper.myLooper();
    if (looper != null && !looper.equals(Looper.getMainLooper())) {
      // This method (and all methods in this class) can be called from Blocks on the JavaBridge
      // thread. The JavaBridge thread is used when javascript code running in an Android WebView
      // widget calls into java. That's how blocks works. For each block in a blocks OpMode, a bit
      // of javascript is generated. When a blocks opmode is executed, it runs in the context of a
      // tiny WebView widget.
      // Because the JavaBridge thread is a looper, but not the main looper, we need to create
      // another thread to call ClassFactory.createVuforiaLocalizer(parameters). Otherwise the
      // Vuforia.UpdateCallbackInterface.Vuforia_onUpdate method is called on the JavaBridge
      // thread and the camera monitor view won't update until after waitForStart is finished.
      final AtomicReference<VuforiaLocalizer> vuforiaLocalizerReference = new AtomicReference<>();
      final AtomicReference<RuntimeException> exceptionReference = new AtomicReference<>();
      Thread thread = new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            vuforiaLocalizerReference.set(ClassFactory.getInstance().createVuforia(parameters));
          } catch (RuntimeException e) {
            exceptionReference.set(e);
          }
        }
      });
      thread.start();
      try {
        thread.join();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }

      // If an exception was caught on the other thread, rethrow it now.
      RuntimeException e = exceptionReference.get();
      if (e != null) {
        e.fillInStackTrace();
        throw e;
      }

      vuforiaLocalizer = vuforiaLocalizerReference.get();
    } else {
      vuforiaLocalizer = ClassFactory.getInstance().createVuforia(parameters);
    }

    vuforiaTrackables = vuforiaLocalizer.loadTrackablesFromAsset(assetName);

    for (int i = 0; i < trackableNames.length; i++) {
      VuforiaTrackable trackable = vuforiaTrackables.get(i);
      String name = trackableNames[i];
      OpenGLMatrix locationOnField;
      if (useCompetitionFieldTargetLocations) {
        locationOnField = locationsOnField.get(name);
      } else {
        // Create an image translation/rotation matrix to be used for all images.
        // Essentially put all the image centers at the 0:0:0 origin,
        // but rotate them so they along the -X axis.
        locationOnField =
            OpenGLMatrix.rotation(AxesReference.EXTRINSIC, AxesOrder.XYZ, AngleUnit.DEGREES, 90, 0, -90);
      }
      initTrackable(trackable, name, locationOnField, locationOnRobot, parameters);
    }
  }

  public static Parameters createParameters() {
    Parameters parameters = new Parameters();
    Context context = AppUtil.getInstance().getRootActivity();
    AssetManager assetManager = context.getAssets();
    try {
      try (BufferedReader reader =
          new BufferedReader(new InputStreamReader(assetManager.open(LICENSE_KEY_FILE)))) {
        parameters.vuforiaLicenseKey = reader.readLine();
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to read vuforia license key from asset.", e);
    }
    return parameters;
  }

  private Parameters createParametersWithoutCamera(String vuforiaLicenseKey,
      boolean useExtendedTracking, boolean enableCameraMonitoring, CameraMonitorFeedback cameraMonitorFeedback) {
    Parameters parameters = createParameters();
    if (vuforiaLicenseKey.length() >= 217) {
      parameters.vuforiaLicenseKey = vuforiaLicenseKey;
    }
    parameters.useExtendedTracking = useExtendedTracking;
    parameters.cameraMonitorFeedback = cameraMonitorFeedback;
    if (enableCameraMonitoring) {
      Context context = AppUtil.getInstance().getRootActivity();
      parameters.cameraMonitorViewIdParent = context.getResources().getIdentifier(
          "cameraMonitorViewId", "id", context.getPackageName());
    }
    return parameters;
  }

  private void initTrackable(
      VuforiaTrackable vuforiaTrackable, String name, OpenGLMatrix locationOnField,
      OpenGLMatrix locationOnRobot, Parameters parameters) {
    vuforiaTrackable.setName(name);
    if (locationOnField != null) {
      vuforiaTrackable.setLocation(locationOnField);
    }

    VuforiaTrackableDefaultListener listener =
        (VuforiaTrackableDefaultListener) vuforiaTrackable.getListener();
    if (parameters.cameraName != null && !parameters.cameraName.isUnknown()) {
      listener.setCameraLocationOnRobot(parameters.cameraName, locationOnRobot);
    } else {
      listener.setPhoneInformation(locationOnRobot, parameters.cameraDirection);
    }
    listenerMap.put(name.toUpperCase(Locale.ENGLISH), listener);
  }

  public VuforiaLocalizer getVuforiaLocalizer() {
    if (vuforiaLocalizer == null) {
      throw new IllegalStateException("You forgot to call Vuforia.initialize!");
    }
    return vuforiaLocalizer;
  }

  /**
   * Activates all trackables, so that it is actively seeking their presence.
   *
   * @throws IllegalStateException if initialized has not been called yet.
   */
  public void activate() {
    if (vuforiaTrackables == null) {
      throw new IllegalStateException("You forgot to call Vuforia.initialize!");
    }
    vuforiaTrackables.activate();
  }

  /**
   * Deactivates all trackables, causing it to no longer see their presence.
   *
   * @throws IllegalStateException if initialized has not been called yet.
   */
  public void deactivate() {
    if (vuforiaTrackables == null) {
      throw new IllegalStateException("You forgot to call Vuforia.initialize!");
    }
    vuforiaTrackables.deactivate();
  }

  /**
   * Returns the TrackingResults of the trackable with the given name.
   *
   * @throws IllegalStateException if initialized has not been called yet.
   * @throws IllegalArgumentException if name is not the name of a trackable.
   */
  public TrackingResults track(String name) {
    if (vuforiaTrackables == null) {
      throw new IllegalStateException("You forgot to call Vuforia.initialize!");
    }
    VuforiaTrackableDefaultListener listener = getListener(name);
    if (listener == null) {
      throw new IllegalArgumentException("name");
    }
    boolean isUpdatedRobotLocation;
    OpenGLMatrix matrix = listener.getUpdatedRobotLocation();
    if (matrix != null) {
      isUpdatedRobotLocation = true;
      locationMap.put(name, matrix);
    } else {
      isUpdatedRobotLocation = false;
      matrix = locationMap.get(name);
    }
    return new TrackingResults(name, listener.isVisible(), isUpdatedRobotLocation, matrix);
  }

  /**
   * Returns the TrackingResults of the pose of the trackable with the given name.
   * The pose is the location of the trackable in the phone's coordinate system.
   *
   * @throws IllegalStateException if initialized has not been called yet.
   * @throws IllegalArgumentException if name is not the name of a trackable.
   */
  public TrackingResults trackPose(String name) {
    if (vuforiaTrackables == null) {
      throw new IllegalStateException("You forgot to call Vuforia.initialize!");
    }
    VuforiaTrackableDefaultListener listener = getListener(name);
    if (listener == null) {
      throw new IllegalArgumentException("name");
    }
    OpenGLMatrix matrix = listener.getPose();
    boolean isUpdatedRobotLocation = false;
    if (matrix != null) {
      poseMap.put(name, matrix);
    } else {
      matrix = poseMap.get(name);
    }
    return new TrackingResults(name, listener.isVisible(), isUpdatedRobotLocation, matrix);
  }

  public TrackingResults emptyTrackingResults(String name) {
    return new TrackingResults(name);
  }

  protected VuforiaTrackableDefaultListener getListener(String name) {
    return listenerMap.get(name.toUpperCase(Locale.ENGLISH));
  }

  /**
   * Deactivates the trackables and cleans up.
   */
  public void close() {
    if (vuforiaTrackables != null) {
      vuforiaTrackables.deactivate();
      vuforiaTrackables = null;
    }
    listenerMap.clear();
    locationMap.clear();
    poseMap.clear();
  }

  public String printTrackableNames() {
    StringBuilder sb = new StringBuilder();
    String delimiter = "";
    for (String trackableName : trackableNames) {
      sb.append(delimiter).append(trackableName);
      delimiter = ", ";
    }
    return sb.toString();
  }

}
