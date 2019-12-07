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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

@SuppressWarnings("WeakerAccess")
public class BatteryChecker {

  //------------------------------------------------------------------------------------------------
  // Callback interface
  //------------------------------------------------------------------------------------------------

  public interface BatteryWatcher {
    // called whenever the battery watcher should receive the latest
    // battery level, reported as a percent.
    void updateBatteryStatus(BatteryStatus status);
  }

  public static class BatteryStatus {
    public double  percent;
    public boolean isCharging;

    public BatteryStatus(double percent, boolean isCharging) {
      this.percent = percent;
      this.isCharging = isCharging;
    }

    protected BatteryStatus() { }

    public String serialize() {
      StringBuilder result = new StringBuilder();
      result.append(percent);
      result.append('|');
      result.append(isCharging);
      return result.toString();
    }

    public static BatteryStatus deserialize(String serialized) {
      String[] data = serialized.split("\\|");
      BatteryStatus result = new BatteryStatus();
      result.percent = Double.parseDouble(data[0]);
      result.isCharging = Boolean.parseBoolean(data[1]);
      return result;
    }
  }

  //------------------------------------------------------------------------------------------------
  // State
  //------------------------------------------------------------------------------------------------

  public static final String TAG = "BatteryChecker";

  // Don't spam the log if there's nothing wrong with the battery.
  private static final int LOG_THRESHOLD = 70;

  private Context context;
  private long repeatDelay;
  private long initialDelay = 5000; // ms. 'not exactly clear why we wait to send
  private BatteryWatcher watcher;
  protected final Handler batteryHandler;
  protected boolean closed;
  protected final Monitor monitor = new Monitor();

  protected final static boolean debugBattery = false;
  protected final static int BATTERY_WARN_THRESHOLD = 30;

  //------------------------------------------------------------------------------------------------
  // Construction & control
  //------------------------------------------------------------------------------------------------

  public BatteryChecker(BatteryWatcher watcher, long delay) {
    this.context = AppUtil.getDefContext();
    this.watcher = watcher;
    this.repeatDelay = delay;
    batteryHandler = new Handler();
    closed = true;
  }

  public void startBatteryMonitoring() {
    // sends one battery update after a short delay.
    synchronized (batteryHandler) {
      closed = false;
      batteryHandler.postDelayed(batteryLevelChecker, initialDelay);
    }
    registerReceiver(monitor);
  }

  public void close() {

    if (!closed) {

      // If the following throws an exception, it is not a big deal, log it and continue
      try {
        context.unregisterReceiver(monitor);
      } catch (Exception ex) {
        RobotLog.ee(TAG, ex, "Failed to unregister battery monitor receiver; ignored");
      }

      try {
        synchronized (batteryHandler) {
          closed = true; // force any in-flight callback to simply drain
          batteryHandler.removeCallbacks(batteryLevelChecker);
        }
      } catch (Exception ex) {
        RobotLog.ee(TAG, ex, "Failed to remove battery monitor callbacks; ignored");
      }
    }
  }

  //------------------------------------------------------------------------------------------------
  // Monitoring
  //
  // There are no guarantees about how frequently the hardware will broadcast the battery level
  // so it's more reliable to do our own polling. I register the receiver with a null receiver
  // since I don't care about actually receiving the broadcast. registerReceiver() gives me
  // the intent with all the info I want. Then I do some processing, and I'm done.
  //
  // That said, we *also* do register so that we *can* get prompt notifications on transitions
  // so that we can, especially, update the charging status more quickly than our polling permits.
  //
  //------------------------------------------------------------------------------------------------

  protected class Monitor extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
      switch (intent.getAction()) {
        case Intent.ACTION_BATTERY_CHANGED:
          processBatteryChanged(intent);
          break;
      }
    }
  }

  Runnable batteryLevelChecker = new Runnable() {
    @Override
    public void run() {
      pollBatteryLevel(watcher);

      // Posts the next iteration of this runnable, to be run after "delay" milliseconds.
      synchronized (batteryHandler) {
        if (!closed) {
          batteryHandler.postDelayed(batteryLevelChecker, repeatDelay);
        }
      }
    }
  };

  public void pollBatteryLevel(BatteryWatcher watcher) {
    Intent intent = registerReceiver(null);
    processBatteryChanged(intent);
  }

  protected Intent registerReceiver(BroadcastReceiver receiver) {
    IntentFilter batteryLevelFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    return context.registerReceiver(receiver, batteryLevelFilter);
  }

  protected void processBatteryChanged(Intent intent) {

    if (intent == null) {
      return;
    }

    int currentLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
    int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

    if (currentLevel >= 0 && scale > 0) {
      int batteryPlugged = BatteryManager.BATTERY_PLUGGED_AC | BatteryManager.BATTERY_PLUGGED_USB | BatteryManager.BATTERY_PLUGGED_WIRELESS;
      boolean isCharging = (intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 1) & batteryPlugged) != 0;
      int percent = (currentLevel * 100) / scale;
      logBatteryInfo(percent, isCharging);
      watcher.updateBatteryStatus(new BatteryStatus(percent, isCharging));
    }
  }

  protected void logBatteryInfo(int percent, boolean isCharging) {
    if ((debugBattery) || (percent < BATTERY_WARN_THRESHOLD)) {
      RobotLog.ii(TAG, "percent remaining: " + percent + " is charging: " + isCharging);
    }
  }
}
