/*
 * Copyright (c) 2014, 2015 Qualcomm Technologies Inc
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

package com.qualcomm.ftccommon;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.wifi.FixWifiDirectSetup;

import org.firstinspires.ftc.robotcore.internal.ui.ThemedActivity;

/**
 * This activity is used to correct any problems detected with the current
 * Wifi Direct settings.
 */
public class ConfigWifiDirectActivity extends ThemedActivity {

  public static final String TAG = "ConfigWifiDirectActivity";
  @Override public String getTag() { return TAG; }

  public enum Flag {
    NONE,
    WIFI_DIRECT_FIX_CONFIG,
    WIFI_DIRECT_DEVICE_NAME_INVALID
  }

  private static Flag flag = Flag.NONE;

  private WifiManager wifiManager;
  private ProgressDialog progressDialog;
  private Context context;

  private TextView textPleaseWait;
  private TextView textBadDeviceName;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_config_wifi_direct);

    textPleaseWait = (TextView) findViewById(R.id.textPleaseWait);
    textBadDeviceName = (TextView) findViewById(R.id.textBadDeviceName);

    context = this;
  }

  @Override
  protected void onResume() {
    super.onResume();

    textPleaseWait.setVisibility(View.VISIBLE);

    wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

    RobotLog.ii(TAG, "Processing flag " + flag.toString());

    switch (flag) {
      case WIFI_DIRECT_DEVICE_NAME_INVALID:
        new Thread(new DisableWifiAndWarnBadDeviceName()).start();
        break;
      case  WIFI_DIRECT_FIX_CONFIG:
        new Thread(new ToggleWifiRunnable()).start();
        break;
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    flag = Flag.NONE;
    textBadDeviceName.setVisibility(View.INVISIBLE);
  }

  /*
   * This runnable toggles wifi off and on, which is the only reliable way to fix a misconfigured
   * wifi direct connection.
   */
  private class ToggleWifiRunnable implements Runnable {
    @Override
    public void run() {
      RobotLog.ii(TAG, "attempting to reconfigure Wifi Direct");
      showProgressDialog();
      try {
        try {
          FixWifiDirectSetup.fixWifiDirectSetup(wifiManager);
        } catch (InterruptedException e) {
          RobotLog.ee(TAG, "Cannot fix wifi setup - interrupted");
        }
      } finally {
        dismissProgressDialog();
      }
      RobotLog.ii(TAG, "reconfigure Wifi Direct complete");
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          finish();
        }
      });
    }
  }

  private class DisableWifiAndWarnBadDeviceName implements Runnable {
    @Override
    public void run() {
      RobotLog.ii(TAG, "attempting to disable Wifi due to bad wifi direct device name");
      showProgressDialog();
      try {
        try {
          FixWifiDirectSetup.disableWifiDirect(wifiManager);
        } catch (InterruptedException e) {
          RobotLog.ee(TAG, "Cannot fix wifi setup - interrupted");
        }
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            textPleaseWait.setVisibility(View.INVISIBLE);
            textBadDeviceName.setVisibility(View.VISIBLE);
          }
        });

      } finally {
        dismissProgressDialog();
      }
    }
  }

  private void showProgressDialog() {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        progressDialog = new ProgressDialog(context, R.style.ConfigWifiDirectDialog);
        progressDialog.setMessage(getString(R.string.progressPleaseWait));
        progressDialog.setTitle("Configuring Wifi Direct");
        progressDialog.setIndeterminate(true);
        progressDialog.show();
      }
    });
  }

  private void dismissProgressDialog() {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        progressDialog.dismiss();
      }
    });
  }

  public static void launch(Context context) {
    launch(context, Flag.WIFI_DIRECT_FIX_CONFIG);
  }

  public static void launch(Context context, Flag flag) {
    Intent configWifiDirectIntent = new Intent(context, ConfigWifiDirectActivity.class);
    configWifiDirectIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
    context.startActivity(configWifiDirectIntent);

    ConfigWifiDirectActivity.flag = flag;
  }
}
