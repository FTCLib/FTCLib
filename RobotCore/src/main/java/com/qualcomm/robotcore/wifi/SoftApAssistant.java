/*
 * Copyright (c) 2016 Molly Nicholas
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
 * Neither the name of Molly Nicholas nor the names of her contributors may be used to
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.qualcomm.robotcore.util.ReadWriteFile;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.system.PreferencesHelper;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class SoftApAssistant extends NetworkConnection {

  public static final String TAG = "SoftApAssistant";

  private static SoftApAssistant softApAssistant = null;

  private final List<ScanResult> scanResults = new ArrayList<ScanResult>();

  private final WifiManager wifiManager;
  private Context context = null;

  private static IntentFilter intentFilter;
  private BroadcastReceiver receiver;

  private static String DEFAULT_PASSWORD = "password";
  private static String DEFAULT_SSID = "FTC-1234";

  String ssid = DEFAULT_SSID;
  String password = DEFAULT_PASSWORD;

  private final static String NETWORK_SSID_FILE = "FTC_RobotController_SSID.txt";
  private final static String NETWORK_PASSWORD_FILE = "FTC_RobotController_password.txt";

  public synchronized static SoftApAssistant getSoftApAssistant(Context context) {
    if (softApAssistant == null) softApAssistant = new SoftApAssistant(context);

    // Set up the intent filter for wifi direct
    intentFilter = new IntentFilter();
    intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
    intentFilter.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION);
    intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
    intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
    intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
    intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);

    return softApAssistant;
  }

  private SoftApAssistant(Context context) {
    super(context);
    wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
  }

  public List<ScanResult> getScanResults() {
    return scanResults;
  }

  @Override
  public NetworkType getNetworkType() {
    return NetworkType.SOFTAP;
  }

  @Override
  public void enable() {

    if (receiver == null) receiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        RobotLog.v("onReceive(), action: " + action + ", wifiInfo: " + wifiInfo);

        if (wifiInfo.getSSID().equals(ssid) && wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
          sendEvent(NetworkEvent.CONNECTION_INFO_AVAILABLE);
        }
        if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
          scanResults.clear();
          scanResults.addAll(wifiManager.getScanResults());

          RobotLog.v("Soft AP scanResults found: " + scanResults.size());
          for (ScanResult scanResult : scanResults) {
            // deviceAddress is the MAC address, deviceName is the human readable name
            String s = "    scanResult: " + scanResult.SSID;
            RobotLog.v(s);
          }
          sendEvent(NetworkEvent.PEERS_AVAILABLE);
        }
        if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(action)) {
          if (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
            sendEvent(NetworkEvent.CONNECTION_INFO_AVAILABLE);
          }
        }
      }
    };

    context.registerReceiver(receiver, intentFilter);
  }

  @Override
  public void disable() {
    try {
      context.unregisterReceiver(receiver);
    } catch (IllegalArgumentException e) {
      // disable() was called, but enable() was never called; ignore
    }
  }

  @Override
  public void discoverPotentialConnections() {
    wifiManager.startScan();
  }

  @Override
  public void cancelPotentialConnections() {
    // nothing to do, as the startScan() call operates asynchronously and, seemingly, has no means to be cancelled
  }

  private WifiConfiguration buildConfig(String ssid, String pass) {
    WifiConfiguration myConfig =  new WifiConfiguration();
    myConfig.SSID = ssid;
    myConfig.preSharedKey = pass;
    RobotLog.v("Setting up network, myConfig.SSID: " + myConfig.SSID + ", password: " + myConfig.preSharedKey);
    myConfig.status = WifiConfiguration.Status.ENABLED;
    myConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
    myConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
    myConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
    myConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
    myConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
    myConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
    myConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
    myConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);

    return myConfig;
  }

  @Override
  public void createConnection() {
    if (wifiManager.isWifiEnabled()) {
      wifiManager.setWifiEnabled(false);
    }

    File directory = AppUtil.FIRST_FOLDER;
    File fileSSID = new File(directory, NETWORK_SSID_FILE);
    if (!fileSSID.exists()) {
      ReadWriteFile.writeFile(directory, NETWORK_SSID_FILE, DEFAULT_SSID);
    }

    File filePassword = new File(directory, NETWORK_PASSWORD_FILE);
    if (!filePassword.exists()) {
      ReadWriteFile.writeFile(directory, NETWORK_PASSWORD_FILE, DEFAULT_PASSWORD);
    }

    String userSSID = ReadWriteFile.readFile(fileSSID);
    String userPass = ReadWriteFile.readFile(filePassword);

    if(userSSID.isEmpty() || userSSID.length() >= 15) {
      ReadWriteFile.writeFile(directory, NETWORK_SSID_FILE, DEFAULT_SSID);
    }

    if (userPass.isEmpty()) {
      ReadWriteFile.writeFile(directory, NETWORK_PASSWORD_FILE, DEFAULT_PASSWORD);
    }
    this.ssid = ReadWriteFile.readFile(fileSSID);
    this.password = ReadWriteFile.readFile(filePassword);

    WifiConfiguration wifiConfig = buildConfig(this.ssid, this.password);

    RobotLog.v("Advertising SSID: " + this.ssid + ", password: " + this.password);

    try {
      Boolean success = false;

      if (isSoftAccessPoint()) {
        // AP is already set up
        success = true;
      } else {
        // Set up AP
        Method setApConfig = wifiManager.getClass().getMethod("setWifiApConfiguration", WifiConfiguration.class);
        setApConfig.invoke(wifiManager, wifiConfig);

        Method enableAp = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
        enableAp.invoke(wifiManager, null, false);
        success = (Boolean) enableAp.invoke(wifiManager, wifiConfig, true);
      }

      if (success) {
        sendEvent(NetworkEvent.AP_CREATED);
      }
    } catch (NoSuchMethodException|InvocationTargetException|IllegalAccessException e) {
      RobotLog.e(e.getMessage());
      e.printStackTrace();
    }
  }

  @Override
  public void connect(String ssid, String password) {
    this.ssid = ssid;
    this.password = password;
    // setup a wifi configuration
    WifiConfiguration wifiConfig = buildConfig(String.format("\"%s\"", ssid), String.format("\"%s\"", password));

    WifiInfo wifiInfo = wifiManager.getConnectionInfo();

    RobotLog.v("Connecting to SoftAP, SSID: " + wifiConfig.SSID + ", supplicant state: " + wifiInfo.getSupplicantState());
    if (wifiInfo.getSSID().equals(wifiConfig.SSID) && wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
      sendEvent(NetworkEvent.CONNECTION_INFO_AVAILABLE);
    }
    if (!wifiInfo.getSSID().equals(wifiConfig.SSID) || wifiInfo.getSupplicantState() != SupplicantState.COMPLETED) {
      // connect to and enable the connection
      int netId = wifiManager.addNetwork(wifiConfig);
      wifiManager.saveConfiguration();
      if (netId != -1) {

        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration i : list) {
          if(i.SSID != null && i.SSID.equals("\"" + ssid + "\"")) {
            wifiManager.disconnect();
            wifiManager.enableNetwork(i.networkId, true);
            wifiManager.reconnect();
            break;
          }
        }
      }
    }
  }

  @Override
  public void connect(String ssid) {
    connect(ssid, DEFAULT_PASSWORD);
  }

  @Override
  public InetAddress getConnectionOwnerAddress() {
    InetAddress address = null;
    try {
      address = InetAddress.getByName("192.168.43.1");
    } catch (UnknownHostException e) {
      e.printStackTrace();
    }

    return  address;
  }

  @Override
  public String getConnectionOwnerName() {
    RobotLog.v("ssid in softap assistant: " + ssid);
    return this.ssid.replace("\"", "");
  }

  @Override
  public String getConnectionOwnerMacAddress() {
    return this.ssid.replace("\"", "");
  }

  @Override
  public boolean isConnected() {
    if (isSoftAccessPoint()) return true;

    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
    RobotLog.v("isConnected(), current supplicant state: " + wifiInfo.getSupplicantState().toString());
    return wifiInfo.getSupplicantState() == SupplicantState.COMPLETED;
  }

  @Override
  public String getDeviceName() {
    return ssid;
  }

  private boolean isSoftAccessPoint() {

    Method isWifiApEnabled = null;
    try {
      isWifiApEnabled = wifiManager.getClass().getMethod("isWifiApEnabled");
      return (Boolean) isWifiApEnabled.invoke(wifiManager);
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }
    return false;
  }

  @Override
  public String getInfo() {
    StringBuilder s = new StringBuilder();

    WifiInfo wifiInfo = wifiManager.getConnectionInfo();

    s.append("Name: ").append(getDeviceName());
    if (isSoftAccessPoint()) {
      s.append("\nAccess Point SSID: ").append(getConnectionOwnerName());
      s.append("\nPassphrase: ").append(getPassphrase());
      s.append("\nAdvertising");
    } else if (isConnected()) {
      s.append("\nIP Address: ").append(getIpAddressAsString(wifiInfo.getIpAddress()));
      s.append("\nAccess Point SSID: ").append(getConnectionOwnerName());
      s.append("\nPassphrase: ").append(getPassphrase());
    } else {
      s.append("\nNo connection information");
    }

    return s.toString();
  }

  private String getIpAddressAsString(int ipAddress) {
    String address =
        String.format("%d.%d.%d.%d",
            (ipAddress & 0xff),
            (ipAddress >> 8 & 0xff),
            (ipAddress >> 16 & 0xff),
            (ipAddress >> 24 & 0xff));
    return address;
  }

  @Override
  public String getFailureReason() {
    return null;
  }

  @Override
  public String getPassphrase() {
    return password;
  }

  @Override
  public ConnectStatus getConnectStatus() {
    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
    switch (wifiInfo.getSupplicantState()) {
      case ASSOCIATING:
        return ConnectStatus.CONNECTING;
      case COMPLETED:
        return ConnectStatus.CONNECTED;
      case SCANNING:
        return ConnectStatus.NOT_CONNECTED;
      default:
        return ConnectStatus.NOT_CONNECTED;
    }
  }

  /**
   * Degenerate implementations
   */
  @Override
  public void onWaitForConnection() { }

  @Override
  public void detectWifiReset() { }
}
