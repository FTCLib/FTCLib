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
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Looper;

import com.qualcomm.robotcore.R;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.internal.network.WifiDirectAgent;
import org.firstinspires.ftc.robotcore.internal.network.WifiUtil;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.system.PreferencesHelper;
import org.firstinspires.ftc.robotcore.internal.network.WifiDirectInviteDialogMonitor;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused, WeakerAccess")
public class WifiDirectAssistant extends NetworkConnection {

  public static final String TAG = "WifiDirect";

  private static WifiDirectAssistant wifiDirectAssistant = null;

  private final Object connectStatusLock = new Object();
  private final Object groupOwnerLock = new Object();

  private final List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();

  private final IntentFilter intentFilter;
  private final Channel wifiP2pChannel;
  private final WifiP2pManager wifiP2pManager;

  private final WifiDirectConnectionInfoListener connectionListener;
  private final WifiDirectPeerListListener peerListListener;
  private final WifiDirectGroupInfoListener groupInfoListener;

  private PreferencesHelper preferencesHelper;
  private WifiDirectInviteDialogMonitor inviteDialogMonitor = null;
  private boolean isWifiP2pEnabled = false;
  private WifiP2pBroadcastReceiver receiver;
  private int failureReason = WifiP2pManager.ERROR;
  private ConnectStatus connectStatus = ConnectStatus.NOT_CONNECTED;
  private NetworkEvent lastEvent = null;

  private String deviceMacAddress = "";
  private String deviceName = "";
  private InetAddress groupOwnerAddress = null;
  private String groupOwnerMacAddress = "";
  private String groupOwnerName = "";
  private String groupInterface = "";
  private String groupNetworkName = "";
  private String passphrase = "";
  private boolean groupFormed = false;

  // tracks the number of clients, must be thread safe
  private int clients = 0;

  /*
   * Maintains the list of wifi p2p peers available
   */
  private class WifiDirectPeerListListener implements PeerListListener {

    /*
     * callback method, called by Android when the peer list changes
     */
    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList) {


      peers.clear();
      peers.addAll(peerList.getDeviceList());

      RobotLog.vv(TAG, "peers found: " + peers.size());
      if (peers.size() == 0) {
        WifiUtil.doLocationServicesCheck();
      }

      for (WifiP2pDevice peer : peers) {
        // deviceAddress is the MAC address, deviceName is the human readable name
        String s = "    peer: " + peer.deviceAddress + " " + peer.deviceName;
        RobotLog.vv(TAG, s);
      }

      sendEvent(NetworkEvent.PEERS_AVAILABLE);
    }
  }

  /*
   * Updates when this device connects
   */
  private class WifiDirectConnectionInfoListener implements WifiP2pManager.ConnectionInfoListener {

    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {

      wifiP2pManager.requestGroupInfo(wifiP2pChannel, groupInfoListener);
      synchronized (groupOwnerLock) {
        groupOwnerAddress = info.groupOwnerAddress;
        RobotLog.dd(TAG, "group owners address: " + groupOwnerAddress.toString());
      }

      if (info.groupFormed && info.isGroupOwner) {
        RobotLog.dd(TAG, "group formed, this device is the group owner (GO)");
        synchronized (connectStatusLock) {
          connectStatus = ConnectStatus.GROUP_OWNER;
        }
        sendEvent(NetworkEvent.CONNECTED_AS_GROUP_OWNER);
      } else if (info.groupFormed) {
        RobotLog.dd(TAG, "group formed, this device is a client");
        synchronized (connectStatusLock) {
          connectStatus = ConnectStatus.CONNECTED;
        }
        sendEvent(NetworkEvent.CONNECTED_AS_PEER);
      } else {
        RobotLog.dd(TAG, "group NOT formed, ERROR: " + info.toString());
        failureReason = WifiP2pManager.ERROR; // there is no error code for this
        synchronized (connectStatusLock) {
          connectStatus = ConnectStatus.ERROR;
        }
        sendEvent(NetworkEvent.ERROR);
      }
    }
  }

  private class WifiDirectGroupInfoListener implements WifiP2pManager.GroupInfoListener {

    @Override
    public void onGroupInfoAvailable(WifiP2pGroup group) {
      if (group == null) return;

      if (group.isGroupOwner()) {
        groupOwnerMacAddress = deviceMacAddress;
        groupOwnerName = deviceName;
      } else {
        WifiP2pDevice go = group.getOwner();
        groupOwnerMacAddress = go.deviceAddress;
        groupOwnerName = go.deviceName;
      }

      groupInterface = group.getInterface();
      groupNetworkName = group.getNetworkName();

      passphrase = group.getPassphrase();

      // make sure passphase isn't null
      passphrase = (passphrase != null) ? passphrase : "";

      RobotLog.vv(TAG, "connection information available");
      RobotLog.vv(TAG, "connection information - groupOwnerName = " + groupOwnerName);
      RobotLog.vv(TAG, "connection information - groupOwnerMacAddress = " + groupOwnerMacAddress);
      RobotLog.vv(TAG, "connection information - groupInterface = " + groupInterface);
      RobotLog.vv(TAG, "connection information - groupNetworkName = " + groupNetworkName);

      sendEvent(NetworkEvent.CONNECTION_INFO_AVAILABLE);
    }
  }

  private class WifiP2pBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();

      if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
        int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
        isWifiP2pEnabled = (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED);
        RobotLog.dd(TAG, "broadcast: state - enabled: " + isWifiP2pEnabled);

      } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
        RobotLog.dd(TAG, "broadcast: peers changed");
        wifiP2pManager.requestPeers(wifiP2pChannel, peerListListener);

      } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
        // Note: this is sent out periodically (on the order of four times an hour) even when
        // *nothing* has actually changed. So be careful when processing things: we get data *levels*
        // here, not data *transitions*.
        //
        NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
        WifiP2pInfo wifip2pinfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);
        WifiP2pGroup wifiP2pGroup = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP);
        //
        RobotLog.dd(TAG, "broadcast: connection changed: connectStatus=%s networkInfo.state=%s", connectStatus, networkInfo.getState());
        //
        if (networkInfo.isConnected()) {
          if (!isConnected()) {
            preferencesHelper.writeStringPrefIfDifferent(context.getString(R.string.pref_wifip2p_groupowner_connectedto), wifiP2pGroup.getOwner().deviceName);
            wifiP2pManager.requestConnectionInfo(wifiP2pChannel, connectionListener);
            wifiP2pManager.stopPeerDiscovery(wifiP2pChannel, null);
          }
        } else {
          preferencesHelper.remove(context.getString(R.string.pref_wifip2p_groupowner_connectedto));
          synchronized (connectStatusLock) {
            connectStatus = ConnectStatus.NOT_CONNECTED;
          }
          if (!groupFormed) {
            discoverPeers();
          }
          // if we were previously connected, notify that we are now disconnected
          if (isConnected()) {
            RobotLog.vv(TAG, "disconnecting");
            sendEvent(NetworkEvent.DISCONNECTED);
          }
          groupFormed = wifip2pinfo.groupFormed;
        }

      } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
        RobotLog.dd(TAG, "broadcast: this device changed");
        onWifiP2pThisDeviceChanged((WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));

      } else {
        RobotLog.dd(TAG, "broadcast: %s", action);
      }
    }
  }

  public synchronized static WifiDirectAssistant getWifiDirectAssistant(Context context) {
    if (wifiDirectAssistant == null) wifiDirectAssistant = new WifiDirectAssistant(context);
    return wifiDirectAssistant;
  }

  private WifiDirectAssistant(Context context) {
    super(context);

    // Set up the intent filter for wifi direct
    intentFilter = new IntentFilter();
    intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
    intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
    intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
    intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

    wifiP2pManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
    wifiP2pChannel = wifiP2pManager.initialize(context, Looper.getMainLooper(), null);
    receiver = new WifiP2pBroadcastReceiver();
    connectionListener = new WifiDirectConnectionInfoListener();
    peerListListener = new WifiDirectPeerListListener();
    groupInfoListener = new WifiDirectGroupInfoListener();
    inviteDialogMonitor = new WifiDirectInviteDialogMonitor(this.context);
    preferencesHelper = new PreferencesHelper(TAG, this.context);
    preferencesHelper.remove(context.getString(R.string.pref_wifip2p_groupowner_connectedto));
  }

  @Override
  public NetworkType getNetworkType() {
    return NetworkType.WIFIDIRECT;
  }

  public synchronized void enable() {
    clients += 1;
    RobotLog.vv(TAG, "There are " + clients + " Wifi Direct Assistant Clients (+)");

    if (clients == 1) {
      RobotLog.vv(TAG, "Enabling Wifi Direct Assistant");
      if (receiver == null) receiver = new WifiP2pBroadcastReceiver();
      context.registerReceiver(receiver, intentFilter);
      inviteDialogMonitor.startMonitoring();
    }

    WifiDirectAgent.getInstance().doListen();
  }

  public synchronized void disable() {
    clients -= 1;
    RobotLog.vv(TAG, "There are " + clients + " Wifi Direct Assistant Clients (-)");

    if (clients == 0) {
      RobotLog.vv(TAG, "Disabling Wifi Direct Assistant");
      wifiP2pManager.stopPeerDiscovery(wifiP2pChannel, null);
      wifiP2pManager.cancelConnect(wifiP2pChannel, null);

      try {
        inviteDialogMonitor.stopMonitoring();
        context.unregisterReceiver(receiver);
      } catch (IllegalArgumentException e) {
        // disable() was called, but enable() was never called; ignore
      }
      lastEvent = null;
      synchronized (connectStatusLock) {
        connectStatus = ConnectStatus.NOT_CONNECTED;
      }
    }
  }

  @Override
  public void discoverPotentialConnections() {
    discoverPeers();
  }

  @Override
  public void createConnection() {
    createGroup();
  }

  @Override
  public void cancelPotentialConnections() {
    cancelDiscoverPeers();
  }


  @Override
  public String getInfo() {
    StringBuilder s = new StringBuilder();

    if (isEnabled()) {
      s.append("Name: ").append(getDeviceName());
      if (isGroupOwner()) {
        s.append("\nIP Address: ").append(getGroupOwnerAddress().getHostAddress());
        s.append("\nPassphrase: ").append(getPassphrase());
        s.append("\nGroup Owner");
      } else if (isConnected()) {
        s.append("\nGroup Owner: ").append(getGroupOwnerName());
        s.append("\nConnected");
      } else {
        s.append("\nNo connection information");
      }
    }

    return s.toString();
  }

  public synchronized boolean isEnabled() {
    return (clients > 0);
  }

  public ConnectStatus getConnectStatus() {
    synchronized (connectStatusLock) {
      return connectStatus;
    }
  }

  public List<WifiP2pDevice> getPeers() {
    return new ArrayList<WifiP2pDevice>(peers);
  }

  /**
   * Get the device mac address
   * @return mac address
   */
  public String getDeviceMacAddress() {
    return deviceMacAddress;
  }

  /**
   * Get the device name
   * @return device name
   */
  public String getDeviceName() {
    return deviceName;
  }

  public InetAddress getConnectionOwnerAddress() { return getGroupOwnerAddress(); }
  /**
   * Get the IP address of the group owner
   * @return ip address
   */
  public InetAddress getGroupOwnerAddress() {
    synchronized(groupOwnerLock) {
      return groupOwnerAddress;
    }
  }

  public String getConnectionOwnerMacAddress() {
    return getGroupOwnerMacAddress();
  }
  /**
   * Get the group owners mac address. Example: 'aa:a6:68:83:01:fa'
   * @return mac address
   */
  private String getGroupOwnerMacAddress() {
    return groupOwnerMacAddress;
  }

  public String getConnectionOwnerName() {
    return getGroupOwnerName();
  }

  /**
   * Get the group owners device name. Example: '417-Z-RC'
   * @return device name
   */
  public String getGroupOwnerName() {
    return groupOwnerName;
  }

  /**
   * Return the passphrase for this network; only valid if this device is the group owner
   * @return the passphrase to this device
   */
  public String getPassphrase() {
    return passphrase;
  }

  /**
   * Get the group owners interface used. Example. 'p2p0'
   * @return interface
   */
  public String getGroupInterface() {
    return groupInterface;
  }

  /**
   * Get the group network name. Example: 'DIRECT-dR-417-Z-RC'
   * @return group network name
   */
  public String getGroupNetworkName() {
    return groupNetworkName;
  }

  public boolean isWifiP2pEnabled() {
    return isWifiP2pEnabled;
  }

  /**
   * Returns true if connected, or group owner
   * @return true if connected, otherwise false
   */
  public boolean isConnected() {
    synchronized (connectStatusLock) {
      return (connectStatus == ConnectStatus.CONNECTED
              || connectStatus == ConnectStatus.GROUP_OWNER);
    }
  }

  /**
   * Returns true if this device is the group owner
   * @return true if group owner, otherwise false
   */
  public boolean isGroupOwner() {
    synchronized (connectStatusLock) {
      return (connectStatus == ConnectStatus.GROUP_OWNER);
    }
  }

  /**
   * Discover Wifi Direct peers
   */
  public void discoverPeers() {
    wifiP2pManager.discoverPeers(wifiP2pChannel, new WifiP2pManager.ActionListener() {

      @Override
      public void onSuccess() {
        sendEvent(NetworkEvent.DISCOVERING_PEERS);
        RobotLog.dd(TAG, "discovering peers");
      }

      @Override
      public void onFailure(int reason) {
        String reasonStr = failureReasonToString(reason);
        failureReason = reason;
        RobotLog.w("Wifi Direct failure while trying to discover peers - reason: " + reasonStr);
        sendEvent(NetworkEvent.ERROR);
      }
    });
  }

  /**
   * Cancel discover Wifi Direct peers request
   */
  public void cancelDiscoverPeers() {
    RobotLog.dd(TAG, "stop discovering peers");
    wifiP2pManager.stopPeerDiscovery(wifiP2pChannel, null);
  }

  /**
   * Create a Wifi Direct group
   * <p>
   * Will receive a NetworkEvent.GROUP_CREATED if the group is created. If there is an
   * error creating group NetworkEvent.ERROR will be sent. If group already exists, no
   * event will be sent. However, an NetworkEvent.CONNECTED_AS_GROUP_OWNER should be
   * received.
   */
  public void createGroup() {
    wifiP2pManager.createGroup(wifiP2pChannel, new WifiP2pManager.ActionListener() {

      @Override
      public void onSuccess() {
        sendEvent(NetworkEvent.GROUP_CREATED);
        RobotLog.dd(TAG, "created group");
      }

      @Override
      public void onFailure(int reason) {
        if (reason == WifiP2pManager.BUSY) {
          // most likely group is already created
          RobotLog.dd(TAG, "cannot create group, does group already exist?");
        } else {
          String reasonStr = failureReasonToString(reason);
          failureReason = reason;
          RobotLog.w("Wifi Direct failure while trying to create group - reason: " + reasonStr);
          synchronized (connectStatusLock) {
            connectStatus = ConnectStatus.ERROR;
          }
          sendEvent(NetworkEvent.ERROR);
        }
      }
    });
  }

  /**
   * Remove a Wifi Direct group
   */
  public void removeGroup() {
    wifiP2pManager.removeGroup(wifiP2pChannel, null);
  }

  @Override
  public void connect(String deviceAddress, String notSupported) {
    throw new UnsupportedOperationException("This method is not supported for this class");
  }

  @Override
  public void connect(String deviceAddress) {
    synchronized (connectStatusLock) {
      if (connectStatus == ConnectStatus.CONNECTING || connectStatus == ConnectStatus.CONNECTED) {
        RobotLog.dd(TAG, "connection request to " + deviceAddress + " ignored, already connected");
        return;
      }

      RobotLog.dd(TAG, "connecting to " + deviceAddress);
      connectStatus = ConnectStatus.CONNECTING;
    }
    WifiP2pConfig config = new WifiP2pConfig();
    config.deviceAddress = deviceAddress;
    config.wps.setup = WpsInfo.PBC;
    config.groupOwnerIntent = 1;

    wifiP2pManager.connect(wifiP2pChannel, config, new WifiP2pManager.ActionListener() {

      @Override
      public void onSuccess() {
        RobotLog.dd(TAG, "connect started");
        sendEvent(NetworkEvent.CONNECTING);
      }

      @Override
      public void onFailure(int reason) {
        String reasonStr = failureReasonToString(reason);
        failureReason = reason;
        RobotLog.dd(TAG, "connect cannot start - reason: " + reasonStr);
        sendEvent(NetworkEvent.ERROR);
      }
    });
  }

  private void onWifiP2pThisDeviceChanged(WifiP2pDevice wifiP2pDevice) {
    deviceName = wifiP2pDevice.deviceName;
    deviceMacAddress = wifiP2pDevice.deviceAddress;
    RobotLog.vv(TAG, "device information: " + deviceName + " " + deviceMacAddress);
  }

  public String getFailureReason() {
    return failureReasonToString(failureReason);
  }

  public static String failureReasonToString(int reason) {
    switch (reason) {
      case WifiP2pManager.P2P_UNSUPPORTED:
        return "P2P_UNSUPPORTED";
      case WifiP2pManager.ERROR:
        return "ERROR";
      case WifiP2pManager.BUSY:
        return "BUSY";
      default:
        return "UNKNOWN (reason " + reason + ")" ;
    }
  }

  protected void sendEvent(NetworkEvent event) {
    // don't send duplicate events
    if (lastEvent == event && lastEvent != NetworkEvent.PEERS_AVAILABLE) return;
    lastEvent = event;

    synchronized (callbackLock) {
      if (callback != null) callback.onNetworkConnectionEvent(event);
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
