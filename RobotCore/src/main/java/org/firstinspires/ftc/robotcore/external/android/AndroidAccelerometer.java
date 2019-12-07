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

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import org.firstinspires.ftc.robotcore.external.navigation.Acceleration;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

/**
 * A class that provides access to the Android Accelerometer.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
public class AndroidAccelerometer implements SensorEventListener {
  private volatile boolean listening;
  private volatile long timestamp;
  private volatile float x; // Acceleration minus Gx on the x-axis, in SI units (m/s^2).
  private volatile float y; // Acceleration minus Gx on the y-axis, in SI units (m/s^2).
  private volatile float z; // Acceleration minus Gx on the z-axis, in SI units (m/s^2).
  private volatile DistanceUnit distanceUnit = DistanceUnit.METER;

  // SensorEventListener methods

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {
  }

  @Override
  public void onSensorChanged(SensorEvent sensorEvent) {
    timestamp = sensorEvent.timestamp;
    x = sensorEvent.values[0];
    y = sensorEvent.values[1];
    z = sensorEvent.values[2];
  }

  // public methods

  /**
   * Sets the DistanceUnit to be used.
   */
  public void setDistanceUnit(DistanceUnit distanceUnit) {
    if (distanceUnit != null) {
      this.distanceUnit = distanceUnit;
    }
  }

  /**
   * Returns the DistanceUnit being used.
   */
  public DistanceUnit getDistanceUnit() {
    return distanceUnit;
  }

  /**
   * Returns the acceleration in the x-axis.
   */
  public double getX() {
    if (timestamp != 0) {
      return distanceUnit.fromMeters(x);
    }
    return 0;
  }

  /**
   * Returns the acceleration in the y-axis.
   */
  public double getY() {
    if (timestamp != 0) {
      return distanceUnit.fromMeters(y);
    }
    return 0;
  }

  /**
   * Returns the acceleration in the z-axis.
   */
  public double getZ() {
    if (timestamp != 0) {
      return distanceUnit.fromMeters(z);
    }
    return 0;
  }

  /**
   * Returns an Acceleration object representing acceleration in X, Y and Z axes.
   */
  public Acceleration getAcceleration() {
    if (timestamp != 0) {
      return new Acceleration(DistanceUnit.METER, x, y, z, timestamp)
          .toUnit(distanceUnit);
    }
    return null;
  }

  /**
   * Returns true if the Android device has a accelerometer.
   */
  public boolean isAvailable() {
    Activity activity = AppUtil.getInstance().getRootActivity();
    SensorManager sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
    return !sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).isEmpty();
  }

  /**
   * Start listening to events from the Android accelerometer.
   */
  public void startListening() {
    if (!listening) {
      Activity activity = AppUtil.getInstance().getRootActivity();
      SensorManager sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
      Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
      sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
      listening = true;
    }
  }

  /**
   * Stop listening to events from the Android accelerometer.
   */
  public void stopListening() {
    if (listening) {
      Activity activity = AppUtil.getInstance().getRootActivity();
      SensorManager sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
      sensorManager.unregisterListener(this);
      listening = false;
      timestamp = 0;
    }
  }
}
