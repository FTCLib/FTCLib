/*
Copyright (c) 2018 Craig MacFarlane

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of Craig MacFarlane nor the names of his contributors may be used to
endorse or promote products derived from this software without specific prior
written permission.

NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.qualcomm.robotcore.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.qualcomm.robotcore.R;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.internal.network.NetworkConnectionHandler;
import org.firstinspires.ftc.robotcore.internal.network.WifiUtil;
import org.firstinspires.ftc.robotcore.internal.system.PreferencesHelper;

import java.util.ArrayList;
import java.util.List;

public class DriverStationAccessPointAssistant extends AccessPointAssistant {

    private static final String TAG = "AccessPointAssistant";
    private static final String NO_AP = "No Access Point";
    private static final boolean DEBUG = false;

    private static DriverStationAccessPointAssistant wirelessAPAssistant = null;

    private final List<ScanResult> scanResults = new ArrayList<ScanResult>();
    private IntentFilter intentFilter;
    private BroadcastReceiver receiver;
    private ConnectStatus connectStatus;

    private DriverStationAccessPointAssistant(Context context) {

        super(context);

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);

        if (wifiManager.getConnectionInfo().getSupplicantState() == SupplicantState.COMPLETED) {
            connectStatus = ConnectStatus.CONNECTED;
            saveConnectionInfo(wifiManager.getConnectionInfo());
        } else {
            connectStatus = ConnectStatus.NOT_CONNECTED;
        }
    }

    /**
     * getDriverStationAccessPointAssistant
     */
    public synchronized static DriverStationAccessPointAssistant getDriverStationAccessPointAssistant(Context context)
    {
        if (wirelessAPAssistant == null) {
            wirelessAPAssistant = new DriverStationAccessPointAssistant(context);
        }

        return wirelessAPAssistant;
    }

    /**
     * enable
     *
     * Listen for wifi state changes.
     */
    @Override
    public void enable()
    {
        if (receiver == null) receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
                    handleScanResultsAvailable(intent);
                } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                    handleNetworkStateChanged(intent);
                }
            }
        };

        context.registerReceiver(receiver, intentFilter);
    }

    /**
     * disable
     *
     * Stop listening for wifi state changes.
     */
    @Override
    public void disable()
    {
        if (receiver != null) {
            context.unregisterReceiver(receiver);
        }
    }

    /**
     * getConnectionOwnerName
     *
     * Returns the ssid of the access point we are currently connected to.
     */
    @Override public String getConnectionOwnerName() {
        return WifiUtil.getConnectedSsid();
    }

    /**
     * lookForKnownAccessPoint
     *
     * Looks through a list of ssid's looking for the last known ssid that we
     * had connected to.  If it finds it, reconnect.  This is part of the captive
     * portal mitigation strategy.
     */
    protected boolean lookForKnownAccessPoint(String ssid, String macAddr, List<ScanResult> scanResults)
    {
        if ((ssid == null) || (macAddr == null)) {
            return false;
        }

        if (DEBUG) RobotLog.vv(TAG, "Access point scanResults found: " + scanResults.size());
        if (DEBUG) RobotLog.vv(TAG, "Looking for match to " + ssid + ", " + macAddr);
        for (ScanResult scanResult : scanResults) {
            if (scanResult.SSID.equals(ssid) && scanResult.BSSID.equals(macAddr)) {
                if (DEBUG) RobotLog.ii(TAG, "Found known access point " + scanResult.SSID + ", " + scanResult.BSSID);
                if (connectToAccessPoint(scanResult.SSID) == true) {
                    return true;
                }
                break;
            }
        }

        return false;
    }

    /**
     * handleScanResultsAvailable
     */
    protected void handleScanResultsAvailable(Intent intent)
    {
        PreferencesHelper prefs = new PreferencesHelper(TAG, context);
        String ssid = (String)prefs.readPref(context.getString(R.string.pref_last_known_ssid));
        String macAddr = (String)prefs.readPref(context.getString(R.string.pref_last_known_macaddr));

        scanResults.clear();
        scanResults.addAll(wifiManager.getScanResults());

        if (doContinuousScans == true) {
            if (lookForKnownAccessPoint(ssid, macAddr, scanResults) == false) {
                wifiManager.startScan();
            } else {
                doContinuousScans = false;
            }
        }
    }

    /**
     * handleNetworkStateChanged
     */
    protected void handleNetworkStateChanged(Intent intent)
    {
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        NetworkInfo state = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
        RobotLog.v("Wifi state change:, state: " + state + ", wifiInfo: " + wifiInfo);
        if ((connectStatus == ConnectStatus.CONNECTED) && (state.isConnected() == false)) {
            handleWifiDisconnect();
        } else if ((connectStatus == ConnectStatus.NOT_CONNECTED) && (state.isConnected() == true)) {
            connectStatus = ConnectStatus.CONNECTED;
            saveConnectionInfo(wifiInfo);
            sendEvent(NetworkEvent.CONNECTION_INFO_AVAILABLE);
        }
    }

    /**
     * connectToAccessPoint()
     *
     * Attempt to mitigate the damage done by captive portal detection wherein
     * a device will not automatically reconnect to an access point that it determines
     * has no broader internet access (can't ping a google server).
     */
    protected boolean connectToAccessPoint(String ssid)
    {
        boolean status;

        RobotLog.vv(TAG, "Attempting to auto-connect to " + ssid);

        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        if (list == null) {
            RobotLog.ee(TAG, "Wifi is likely off");
            return false;
        }

        for (WifiConfiguration i : list) {
            if(i.SSID != null && i.SSID.equals("\"" + ssid + "\"")) {
                // wifiManager.disconnect();
                status = wifiManager.enableNetwork(i.networkId, true);
                if (status == false) {
                    RobotLog.ww(TAG, "Could not enable " + ssid);
                    return false;
                }
                status = wifiManager.reconnect();
                if (status == false) {
                    RobotLog.ww(TAG, "Could not reconnect to " + ssid);
                    return false;
                }
                break;
            }
        }

        return true;
    }

    @Override
    protected String getIpAddress() {
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        return getIpAddressAsString(wifiInfo.getIpAddress());
    }

    /**
     * handleWifiDisconnect()
     *
     * The driver station disconnected from the AP.  Everything needs to revert to factory reset
     * as pre-existing sockets will not work properly upon a reconnect, even to the same AP.
     */
    private void handleWifiDisconnect()
    {
        RobotLog.vv(TAG, "Handling wifi disconnect");

        connectStatus = ConnectStatus.NOT_CONNECTED;
        sendEvent(NetworkEvent.DISCONNECTED);

        NetworkConnectionHandler networkConnection = NetworkConnectionHandler.getInstance();
        networkConnection.shutdown();
    }

    /**
     * saveConnectionInfo
     *
     * Caching the last known access point info.
     */
    private void saveConnectionInfo(WifiInfo wifiInfo)
    {
        String ssid = wifiInfo.getSSID().replace("\"", "");
        String macAddr = wifiInfo.getBSSID();

        PreferencesHelper prefs = new PreferencesHelper(TAG, context);
        prefs.writePrefIfDifferent(context.getString(R.string.pref_last_known_ssid), ssid);
        prefs.writePrefIfDifferent(context.getString(R.string.pref_last_known_macaddr), macAddr);
    }


    /**
     * getIpAddressAsString
     */
    private static String getIpAddressAsString(int ipAddress)
    {
        String address =
                String.format("%d.%d.%d.%d",
                        (ipAddress & 0xff),
                        (ipAddress >> 8 & 0xff),
                        (ipAddress >> 16 & 0xff),
                        (ipAddress >> 24 & 0xff));
        return address;
    }
}
