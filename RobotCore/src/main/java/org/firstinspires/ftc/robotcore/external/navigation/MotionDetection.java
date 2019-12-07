/*
 * Copyright (c) 2018 Craig MacFarlane
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * (subject to the limitations in the disclaimer below) provided that the following conditions are
 * met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the
 * distribution.
 *
 * Neither the name of Craig MacFarlane nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS LICENSE. THIS
 * SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.firstinspires.ftc.robotcore.external.navigation;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.system.Deadline;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * A class that will notify listeners when a phone is in motion.
 */
public class MotionDetection implements SensorEventListener {

    public interface MotionDetectionListener {
        void onMotionDetected(double vector);
    }

    private final static boolean DEBUG = false;
    private final static String TAG = "MotionDetection";

    private final double DEFAULT_DETECTION_THRESHOLD = 2.0;
    private final int DEFAULT_RATE_LIMIT_SECONDS = 1;
    private double detectionThreshold;
    private int rateLimitSeconds;
    private boolean listening;
    private CopyOnWriteArrayList<MotionDetectionListener> listeners = new CopyOnWriteArrayList<>();
    private Deadline rateLimiter;

    public class Vector {
        double x;
        double y;
        double z;

        public double magnitude()
        {
            return Math.sqrt(x*x + y*y + z*z);
        }
    }
    protected Vector gravity = new Vector();

    /**
     * filter
     *
     * Taken straight from the google documentation for implementing a low pass
     * filter for filtering out gravity.
     *
     * See https://developer.android.com/reference/android/hardware/SensorEvent#values
     *
     * @param sensorEvent The sensor data spit out by android for the accelerometer.
     * @return a Vector filtered of gravity's influence.
     */
    protected Vector filter(SensorEvent sensorEvent)
    {
        Vector acceleration = new Vector();

        // alpha is calculated as t / (t + dT)
        // with t, the low-pass filter's time-constant
        // and dT, the event delivery rate

        final double alpha = 0.8;

        gravity.x = alpha * gravity.x + (1 - alpha) * sensorEvent.values[0];
        gravity.y = alpha * gravity.y + (1 - alpha) * sensorEvent.values[1];
        gravity.z = alpha * gravity.z + (1 - alpha) * sensorEvent.values[2];

        acceleration.x = sensorEvent.values[0] - gravity.x;
        acceleration.y = sensorEvent.values[1] - gravity.y;
        acceleration.z = sensorEvent.values[2] - gravity.z;

        return acceleration;
    }

    public MotionDetection()
    {
        this.detectionThreshold = DEFAULT_DETECTION_THRESHOLD;
        this.rateLimiter = new Deadline(DEFAULT_RATE_LIMIT_SECONDS, TimeUnit.SECONDS);
        this.listening = false;
    }

    public MotionDetection(double detectionThreshold, int rateLimitSeconds)
    {
        this.detectionThreshold = detectionThreshold;
        this.rateLimiter = new Deadline(rateLimitSeconds, TimeUnit.SECONDS);
        this.listening = false;
    }

    /**
     * registerListener
     */
    public void registerListener(MotionDetectionListener listener)
    {
        listeners.add(listener);
    }

    /**
     * purgeListeners
     */
    public void purgeListeners()
    {
        listeners.clear();
    }

    /**
     * Is the required sensor available on this device?
     */
    public boolean isAvailable()
    {
        Activity activity = AppUtil.getInstance().getRootActivity();
        SensorManager sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
        return !sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).isEmpty();
    }

    /**
     * Start processing sensor data.
     */
    public void startListening()
    {
        if (!listening) {
            Activity activity = AppUtil.getInstance().getRootActivity();
            SensorManager sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
            Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
            listening = true;
        }
    }

    /**
     * Stop processing sensor data.
     */
    public void stopListening()
    {
        if (listening) {
            Activity activity = AppUtil.getInstance().getRootActivity();
            SensorManager sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
            sensorManager.unregisterListener(this);
            listening = false;
        }
    }

    protected void notifyListeners(double vector)
    {
        for (MotionDetectionListener l : listeners) {
            l.onMotionDetected(vector);
        }
    }

    /*************************************************************************************
     * Internal implementations of the SensorEventListener interface.
     *************************************************************************************/

    @Override
    public void onSensorChanged(SensorEvent sensorEvent)
    {
        double x, y, z;
        double vector;

        Vector accel = filter(sensorEvent);
        double magnitude = accel.magnitude();
        if (DEBUG) RobotLog.dd(TAG, "Motion magnitude: " + magnitude);

        if ((magnitude >= detectionThreshold) && (rateLimiter.hasExpired())) {
            rateLimiter.reset();
            notifyListeners(magnitude);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) { /* noop */ }

}
