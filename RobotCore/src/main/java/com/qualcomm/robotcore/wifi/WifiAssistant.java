/* Copyright (c) 2014, 2015 Qualcomm Technologies Inc

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of Qualcomm Technologies Inc nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. */

package com.qualcomm.robotcore.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

import com.qualcomm.robotcore.util.RobotLog;

/**
 * Class for monitoring if wifi is connected or not.
 */
public class WifiAssistant {

  /**
   * Wifi NetworkEvent
   */
  public enum WifiState { CONNECTED, NOT_CONNECTED; }

  /**
   * Interface for callback methods
   */
  public interface WifiAssistantCallback {

    /**
     * Callback - called when wifi connects / disconnects
     * @param event WifiEvent
     */
    public void wifiEventCallback(WifiState event);
  }

  private static class WifiStateBroadcastReceiver extends BroadcastReceiver {
    private WifiState state = null;
    private final WifiAssistantCallback callback;

    public WifiStateBroadcastReceiver(WifiAssistantCallback callback) {
      this.callback = callback;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      if(action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
        NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
        if (info.isConnected()) {
          notify(WifiState.CONNECTED);
        } else {
          notify(WifiState.NOT_CONNECTED);
        }
      }
    }

    private void notify(WifiState newState) {
      if (state == newState) return; // nothing to do

        state = newState;
      if (callback != null) callback.wifiEventCallback(state);
    }
  }

  private final IntentFilter intentFilter;
  private final Context context;
  private final WifiStateBroadcastReceiver receiver;

  /**
   * Constructor
   * @param context needed to register BroadcastRecivers
   * @param callback will be used for callbacks
   */
  public WifiAssistant(Context context, WifiAssistantCallback callback) {
    this.context = context;

    if (callback == null) RobotLog.v("WifiAssistantCallback is null");
    receiver = new WifiStateBroadcastReceiver(callback);

    intentFilter = new IntentFilter();
    intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
  }

  /**
   * Enable callbacks
   * <p>
   * It is recommended to place this in the onReceive method
   */
  public void enable() {
    context.registerReceiver(receiver, intentFilter);
  }

  /**
   * Disable callbacks
   * <p>
   * It is recommended to place this in the onPause method
   */
  public void disable() {
    context.unregisterReceiver(receiver);
  }


}
