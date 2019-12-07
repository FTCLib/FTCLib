/*
 * Copyright (c) 2015 Qualcomm Technologies Inc
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
 * Neither the name of Qualcomm Technologies Inc nor the names of its contributors may be used to
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

package com.qualcomm.robotcore.util;

import android.app.Activity;
import android.os.Handler;
import android.view.WindowManager;

/**
 * A class that will dim the screen after a set amount of time.
 */
public class Dimmer {

  public static final int DEFAULT_DIM_TIME = 30*1000; // milliseconds
  public static final int LONG_BRIGHT_TIME = 60*1000; // milliseconds
  public static final float MAXIMUM_BRIGHTNESS = 1.0f;
  public static final float MINIMUM_BRIGHTNESS = 0.05f;

  Handler handler = new Handler();

  Activity activity;
  final WindowManager.LayoutParams layoutParams;
  long waitTime; // milliseconds
  float userBrightness = MAXIMUM_BRIGHTNESS;

  public Dimmer(Activity activity) {
    this(DEFAULT_DIM_TIME, activity);
  }

  public Dimmer(long waitTime, Activity activity) {
    this.waitTime = waitTime;
    this.activity = activity;
    this.layoutParams = activity.getWindow().getAttributes();
    this.userBrightness = layoutParams.screenBrightness;
  }

  private float percentageDim() {
    float newBrightness = MINIMUM_BRIGHTNESS * userBrightness;
    if (newBrightness < MINIMUM_BRIGHTNESS) {
      return MINIMUM_BRIGHTNESS;
    }
    return newBrightness;
  }

  public void handleDimTimer() {
    sendToUIThread(userBrightness);
    handler.removeCallbacks(null);
    handler.postDelayed(new Runnable() {
      @Override
      public void run() {
        sendToUIThread(percentageDim());
      }
    }, waitTime);
  }

  private void sendToUIThread(float brightness) {
    layoutParams.screenBrightness = brightness;
    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        activity.getWindow().setAttributes(layoutParams);
      }
    });
  }

  /**
   * Cancels all existing handler calls that are not already running, and sets up a new handler
   * that will post in x milliseconds.
   *
   * I.e., leaves the screen bright for one full minute.
   */
  public void longBright(){
    sendToUIThread(userBrightness);
    Runnable runnable = new Runnable() {
      @Override
      public void run() {
        sendToUIThread(percentageDim());
      }
    };
    handler.removeCallbacksAndMessages(null);
    handler.postDelayed(runnable, LONG_BRIGHT_TIME); // milliseconds
  }
}
