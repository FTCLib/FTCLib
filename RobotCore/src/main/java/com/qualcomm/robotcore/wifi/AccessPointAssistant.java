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

import android.content.Context;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.ThreadPool;

import org.firstinspires.ftc.robotcore.internal.network.WifiDirectAgent;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public abstract class AccessPointAssistant extends NetworkConnection {

    private static final String TAG = "AccessPointAssistant";
    private static final String DEFAULT_TETHERING_IP_ADDR = "192.168.43.1";

    protected final WifiManager wifiManager;
    protected boolean doContinuousScans;

    public AccessPointAssistant(Context context)
    {
        super(context);
        wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        this.doContinuousScans = false;

        /*
         * Undo the damage caused by auto-starting the WifiDirectAgent and not being able to stop it because
         * there are ref-counted references _everywhere_.
         */
        WifiDirectAgent.getInstance().doNotListen();
    }

    /**
     * getNetworkType
     */
    @Override
    public NetworkType getNetworkType()
    {
        return NetworkType.WIRELESSAP;
    }

    /**
     * getConnectionOwnerAddress
     *
     * Returns the default IP address for tethering.  Note that this is different than
     * the default IP address for WiFi Direct
     */
    @Override
    public InetAddress getConnectionOwnerAddress()
    {
        InetAddress address = null;
        try {
            address = InetAddress.getByName(DEFAULT_TETHERING_IP_ADDR);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        return  address;
    }


    /**
     * getConnectionOwnerMacAddress
     *
     * Returns the ssid of the access point we are currently connected to.  Yes, yes, it's not
     * a mac address, but it's currently unused.  TODO: Fix this.
     */
    @Override
    public String getConnectionOwnerMacAddress()
    {
        return getConnectionOwnerName();
    }

    /**
     * isConnected
     *
     * Are we connected to an AP?  This differs from that on a robot controller in that on a driver
     * station, one is not connected to an AP simply by virtue of acting as a tethering point.
     */
    @Override
    public boolean isConnected()
    {
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo != null) {
            return (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED);
        } else {
            return false;
        }
    }

    /**
     * getDeviceName
     *
     * The name of a device when in access point mode is the ssid.
     */
    @Override
    public String getDeviceName()
    {
        return getConnectionOwnerName();
    }

    /**
     * getInfo
     */
    @Override
    public String getInfo()
    {
        StringBuilder s = new StringBuilder();

        s.append("Name: ").append(getDeviceName());
        s.append("\nIP Address: ").append(getIpAddress());
        s.append("\nAccess Point SSID: ").append(getConnectionOwnerName());
        String passphrase = getPassphrase();

        if (passphrase != null) {
            s.append("\nPassphrase: ").append(getPassphrase());
        }

        return s.toString();
    }

    /**
     * getConnectStatus
     *
     * Reflects the state of the wpa supplicant as to whether or not it's
     * connected to an access point
     */
    @Override
    public ConnectStatus getConnectStatus()
    {
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
     *  discoverPotentialConnections
     *
     *  On disconnect from an already connected access point, start continuous scanning for access points.
     *  The scan results will be used to attempt to find the last known access point and automatically reconnect.
     *  We spend the cycles for continuous scanning as presumably we want to reconnect as quickly as possible.
     *  A typical scenario is pulling the power supply on a control hub that's broadcasting the ssid.
     *
     *  Proactive reconnects mitigate the captive portal problem wherein the underlying OS won't reconnect to
     *  an access point that it's determined has no upstream internet connectivity.
     *
     *  Note that this has a detrimental effect on battery life if left in this state for extended (hours and hours)
     *  periods of time.
     */
    @Override
    public void discoverPotentialConnections()
    {
        /*
         * If we come around here too quickly after detecting a disconnect, then a scan can
         * potentially still return the now non-existant ssid.  Wait a short period to let whatever
         * latent state there is floating around clear.
         */
        ScheduledFuture<?> scanFuture = ThreadPool.getDefaultScheduler().schedule(new Runnable() {
            @Override public void run() {
                RobotLog.ii(TAG, "Future scan now...");
                wifiManager.startScan();
                doContinuousScans = true;
            }
        }, 1000, TimeUnit.MILLISECONDS);
    }

    /**
     * cancelPotentialConnections
     *
     * Stop the continuous scanning.
     */
    @Override
    public void cancelPotentialConnections()
    {
        doContinuousScans = false;
    }

    protected abstract String getIpAddress();

    /**
     * Degenerate implementations.
     */
    @Override
    public void createConnection() { }

    @Override
    public void connect(String deviceAddress) { }

    @Override
    public void connect(String connectionName, String connectionPassword) { }

    @Override
    public void detectWifiReset() { }

    @Override
    public String getFailureReason()
    {
        return null;
    }

    @Override
    public String getPassphrase()
    {
        return null;
    }

    @Override
    public void onWaitForConnection() { }
}
