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
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AngularVelocity;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

/**
 * A class that provides access to the Android Gyroscope.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
public class AndroidGyroscope implements SensorEventListener {
  private volatile boolean listening;
  private volatile long timestamp;
  private volatile float x; // angular speed around the x-axis, in radians/second
  private volatile float y; // angular speed around the y-axis, in radians/second
  private volatile float z; // angular speed around the z-axis, in radians/second
  private volatile AngleUnit angleUnit = AngleUnit.RADIANS;

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
   * Returns the angular speed around the x-axis.
   */
  public float getX() {
    if (timestamp != 0) {
      return angleUnit.fromRadians(x);
    }
    return 0;
  }

  /**
   * Returns the angular speed around the y-axis.
   */
  public float getY() {
    if (timestamp != 0) {
      return angleUnit.fromRadians(y);
    }
    return 0;
  }

  /**
   * Returns the angular speed around the z-axis.
   */
  public float getZ() {
    if (timestamp != 0) {
      return angleUnit.fromRadians(z);
    }
    return 0;
  }

  /**
   * Returns an AngularVelocity object representing the rate of rotation around the device's local
   * X, Y and Z axis.
   */
  public AngularVelocity getAngularVelocity() {
    if (timestamp != 0) {
      return new AngularVelocity(AngleUnit.RADIANS, x, y, z, timestamp).toAngleUnit(angleUnit);
    }
    return null;
  }

  /**
   * Returns true if the Android device has a gyroscope.
   */
  public boolean isAvailable() {
    Activity activity = AppUtil.getInstance().getRootActivity();
    SensorManager sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
    return !sensorManager.getSensorList(Sensor.TYPE_GYROSCOPE).isEmpty();
  }

  /**
   * Start listening to events from the Android gyroscope.
   */
  public void startListening() {
    if (!listening) {
      Activity activity = AppUtil.getInstance().getRootActivity();
      SensorManager sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
      Sensor gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
      sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST);
      listening = true;
    }
  }

  /**
   * Stop listening to events from the Android gyroscope.
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
