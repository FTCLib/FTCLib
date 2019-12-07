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

package com.qualcomm.robotcore.wifi;

import android.net.wifi.WifiManager;

import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.network.PreferenceRemoterDS;

/**
 * Static class to fix configuration issues with wifi direct
 */
public class FixWifiDirectSetup {

  public static final int WIFI_TOGGLE_DELAY = 2 * 1000; // in milliseconds

  public static void fixWifiDirectSetup(WifiManager wifiManager) throws InterruptedException {
    toggleWifi(false, wifiManager);
    toggleWifi(true, wifiManager);
  }

  public static void disableWifiDirect(WifiManager wifiManager) throws InterruptedException {
    toggleWifi(false, wifiManager);
  }

  private static void toggleWifi(boolean enabled, WifiManager wifiManager) throws InterruptedException {
    String toggle = enabled ? "on" : "off";
    RobotLog.i("Toggling Wifi " + toggle);
    wifiManager.setWifiEnabled(enabled);
    if (AppUtil.getInstance().isDriverStation()) {
      PreferenceRemoterDS.getInstance().onWifiToggled(enabled);
    }
    Thread.sleep(WIFI_TOGGLE_DELAY);
  }
}
