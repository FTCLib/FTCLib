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

package org.firstinspires.ftc.robotcore.external.android;

import static java.lang.Math.PI;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.Surface;
import android.view.WindowManager;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

/**
 * A class that provides access to the Android sensors for Orientation.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
public class AndroidOrientation implements SensorEventListener {
  private volatile boolean listening;
  private volatile long timestampAcceleration;
  private volatile long timestampMagneticField;
  private volatile double azimuth; // in radians
  private volatile double pitch;   // in radians
  private volatile double roll;    // in radians
  private volatile AngleUnit angleUnit = AngleUnit.RADIANS;

  private final float[] acceleration = new float[3];
  private final float[] magneticField = new float[3];
  private final float[] rotationMatrix = new float[9];
  private final float[] inclinationMatrix = new float[9];
  private final float[] orientation = new float[3];

  // SensorEventListener methods

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {
  }

  @Override
  public void onSensorChanged(SensorEvent sensorEvent) {
    int eventType = sensorEvent.sensor.getType();
    if (eventType == Sensor.TYPE_ACCELEROMETER) {
      timestampAcceleration = sensorEvent.timestamp;
      acceleration[0] = sensorEvent.values[0];
      acceleration[1] = sensorEvent.values[1];
      acceleration[2] = sensorEvent.values[2];
    } else if (eventType == Sensor.TYPE_MAGNETIC_FIELD) {
      timestampMagneticField = sensorEvent.timestamp;
      magneticField[0] = sensorEvent.values[0];
      magneticField[1] = sensorEvent.values[1];
      magneticField[2] = sensorEvent.values[2];
    }
    if (timestampAcceleration != 0 && timestampMagneticField != 0) {
      SensorManager.getRotationMatrix(
          rotationMatrix, inclinationMatrix, acceleration, magneticField);
      SensorManager.getOrientation(rotationMatrix, orientation);
      azimuth = normalizeAzimuth(orientation[0]);
      pitch = normalizePitch(orientation[1]);
      // Sign change for roll is for compatibility with App Inventor.
      roll = normalizeRoll(-orientation[2]);

      // Adjust pitch and roll for phone rotation (e.g., landscape vs portrait).
      Activity activity = AppUtil.getInstance().getRootActivity();
      int rotation = ((WindowManager) activity.getSystemService(Context.WINDOW_SERVICE))
          .getDefaultDisplay().getRotation();
      if (rotation == Surface.ROTATION_90) {
        // Phone is turned 90 degrees counter-clockwise.
        double temp = -pitch;
        pitch = -roll;
        roll = temp;
      } else if (rotation == Surface.ROTATION_180) {
        // Phone is rotated 180 degrees.
        roll = -roll;
      } else if (rotation == Surface.ROTATION_270) {
        // Phone is turned 90 degrees clockwise.
        double temp = pitch;
        pitch = roll;
        roll = temp;
      }
    }
  }

  /**
   * Computes the modulo relationship.  This is not the same as
   * Java's remainder (%) operation, which always returns a
   * value with the same sign as the dividend or 0.
   *
   * @param dividend number to divide
   * @param quotient number to divide by
   * @return the number r with the smallest absolute value such
   *         that sign(r) == sign(quotient) and there exists an
   *         integer k such that k * quotient + r = dividend
   */
  private static double mod(double dividend, double quotient) {
    double result = dividend % quotient;
    if (result == 0 || Math.signum(dividend) == Math.signum(quotient)) {
      return result;
    } else {
      return result + quotient;
    }
  }

  /**
   * Normalizes azimuth to be in the range [0, 2*PI).
   *
   * @param azimuth an angle in radians, likely to be in (-2*PI, +2*PI)
   * @return an equivalent angle in the range [0, 2*PI)
   */
  private static double normalizeAzimuth(double azimuth) {
    return mod(azimuth, 2*PI);
  }

  /**
   * Normalizes pitch to be in the range [-PI, +PI).
   *
   * @param pitch an angle in radians, likely to be in (-2*PI, +2*PI)
   * @return an equivalent angle in the range [-PI, +PI)
   */
  private static double normalizePitch(double pitch) {
    return mod(pitch + PI, 2*PI) - PI;
  }

  /**
   * Normalizes roll to be in the range [-PI/2, +PI/2] radians.
   * The App Inventor definition of Roll in the documentation is:
   * <blockquote>
   * 0 radians when the device is level, increasing to PI/2 radians as the
   * device is tilted up onto its left side, and decreasing to -PI/2
   * radians when the device is tilted up onto its right side.
   * </blockquote>
   * After rotating the phone more than PI/2 radians, Roll decreased.
   * For compatibility, we are guaranteeing the same behavior.
   *
   * @param roll an angle likely to be in the range [-PI, +PI]
   *
   * @return the equivalent angle in the range [-PI/2, +PI/2], where angles
   *         with an absolute value greater than PI/2 are reflected over
   *         the x-axis; the value is not defined for inputs outside of
   *         [-PI, +PI]
   */
  private static double normalizeRoll(double roll) {
    // Guarantee that roll is in [-PI, +PI].  It could legitimately
    // be slightly outside due to floating point rounding issues.
    roll = Math.min(roll, PI);
    roll = Math.max(roll, -PI);

    // If roll is in [-PI/2, +PI/2], we're done.
    if (roll >= -PI/2 && roll <= PI/2) {
      return roll;
    }

    // Otherwise, reflect over x-axis to put in 1st or 4th quadrant.
    roll = PI - roll;

    // Put in range [-PI/2, +PI/2].
    if (roll >= 3*PI/2) {
      roll -= 2*PI;
    }
    return roll;
  }

  // public methods

  /**
   * Sets the AngleUnit to be used.
   */
  public void setAngleUnit(AngleUnit angleUnit) {
    if (angleUnit != null) {
      this.angleUnit = angleUnit;
    }
  }

  /**
   * Returns the AngleUnit being used.
   */
  public AngleUnit getAngleUnit() {
    return angleUnit;
  }

  /**
   * Returns the azimuth.
   */
  public double getAzimuth() {
    if (timestampAcceleration != 0 && timestampMagneticField != 0) {
      return angleUnit.fromRadians(azimuth);
    }
    return 0;
  }

  /**
   * Returns the pitch.
   */
  public double getPitch() {
    if (timestampAcceleration != 0 && timestampMagneticField != 0) {
      return angleUnit.fromRadians(pitch);
    }
    return 0;
  }

  /**
   * Returns the roll.
   */
  public double getRoll() {
    if (timestampAcceleration != 0 && timestampMagneticField != 0) {
      return angleUnit.fromRadians(roll);
    }
    return 0;
  }

  /**
   * Returns the angle in which the orientation sensor is tilted, treating Roll as the x-coordinate
   * and Pitch as the y-coordinate.
   */
  public double getAngle() {
    if (timestampAcceleration != 0 && timestampMagneticField != 0) {
      double angle = Math.atan2(pitch, -roll); // Invert roll to correct sign.
      return angleUnit.fromRadians(angle);
    }
    return 0;
  }

  /**
   * Returns a number between 0 and 1, indicating how much the device is tilted.
   */
  public double getMagnitude() {
    if (timestampAcceleration != 0 && timestampMagneticField != 0) {
      // Limit pitch and roll to PI/2; otherwise, the phone is upside down.
      // The official documentation falsely claims that the range of pitch and
      // roll is [-PI/2, PI/2].  If the device is upside-down, it can range from
      // -PI to PI.  We restrict it to the range [-PI/2, PI/2].
      // With that restriction, if the pitch and roll angles are P and R, then
      // the force is given by 1 - cos(P)cos(R).
      final double MAX_VALUE = PI/2;
      double limitedPitch = Math.min(MAX_VALUE, Math.abs(pitch));
      double limitedRoll = Math.min(MAX_VALUE, Math.abs(roll));
      return 1.0 - Math.cos(limitedPitch) * Math.cos(limitedRoll);
    }
    return 0;
  }

  /**
   * Returns true if the Android device has the sensors required for orientation.
   */
  public boolean isAvailable() {
    Activity activity = AppUtil.getInstance().getRootActivity();
    SensorManager sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
    return !sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).isEmpty()
        && !sensorManager.getSensorList(Sensor.TYPE_MAGNETIC_FIELD).isEmpty();
  }

  /**
   * Start listening to events from the Android sensors.
   */
  public void startListening() {
    if (!listening) {
      Activity activity = AppUtil.getInstance().getRootActivity();
      SensorManager sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
      Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
      sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
      Sensor magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
      sensorManager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_NORMAL);
      listening = true;
    }
  }

  /**
   * Stop listening to events from the Android sensors.
   */
  public void stopListening() {
    if (listening) {
      Activity activity = AppUtil.getInstance().getRootActivity();
      SensorManager sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
      sensorManager.unregisterListener(this);
      listening = false;
      timestampAcceleration = 0;
      timestampMagneticField = 0;
    }
  }
}
