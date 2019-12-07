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
import android.widget.Toast;

import com.qualcomm.robotcore.hardware.configuration.LynxConstants;
import com.qualcomm.robotcore.robocol.Command;
import com.qualcomm.robotcore.util.Intents;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.internal.hardware.android.AndroidBoard;
import org.firstinspires.ftc.robotcore.internal.network.ApChannelManager;
import org.firstinspires.ftc.robotcore.internal.network.ApChannelManagerFactory;
import org.firstinspires.ftc.robotcore.internal.network.DeviceNameManager;
import org.firstinspires.ftc.robotcore.internal.network.DeviceNameManagerFactory;
import org.firstinspires.ftc.robotcore.internal.network.NetworkConnectionHandler;
import org.firstinspires.ftc.robotcore.internal.network.PasswordManager;
import org.firstinspires.ftc.robotcore.internal.network.PasswordManagerFactory;
import org.firstinspires.ftc.robotcore.internal.network.RobotCoreCommandList;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.ui.UILocation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class RobotControllerAccessPointAssistant extends AccessPointAssistant {

    private static final String TAG = "RobotControllerAccessPointAssistant";

    private static RobotControllerAccessPointAssistant robotControllerAccessPointAssistant = null;

    private IntentFilter intentFilter;
    private BroadcastReceiver receiver;
    private NetworkConnection.ConnectStatus connectStatus;
    private DeviceNameManager nameManager = DeviceNameManagerFactory.getInstance();
    private PasswordManager passwordManager = PasswordManagerFactory.getInstance();
    private ApChannelManager apChannelManager = ApChannelManagerFactory.getInstance();

    private final Object enableDisableLock = new Object();

    public static final int WIFI_AP_STATE_DISABLED = 11;
    public static final int WIFI_AP_STATE_ENABLED = 13;

    private RobotControllerAccessPointAssistant(Context context)
    {
        super(context);

        intentFilter = new IntentFilter();
        intentFilter.addAction(Intents.ANDROID_ACTION_WIFI_STATE_CHANGED);
        intentFilter.addAction(Intents.ACTION_FTC_WIFI_FACTORY_RESET);
    }

    /**
     * getRobotControllerAccessPointAssistant
     *
     * Standard singleton.
     */
    public synchronized static RobotControllerAccessPointAssistant getRobotControllerAccessPointAssistant(Context context)
    {
        if (robotControllerAccessPointAssistant == null) {
            robotControllerAccessPointAssistant = new RobotControllerAccessPointAssistant(context);
        }

        return robotControllerAccessPointAssistant;
    }

    /**
     * handleWifiStateChange
     *
     * Simply notifies listeners of state change.  All real work is done elsewhere.
     */
    private void handleWifiStateChange(Intent intent)
    {
        int state = intent.getIntExtra("wifi_state", 0);
        RobotLog.ii(TAG, "Wifi state change:, wifiApState: " + state);
        if (state == WIFI_AP_STATE_DISABLED) {
            connectStatus = ConnectStatus.NOT_CONNECTED;
            sendEvent(NetworkEvent.DISCONNECTED);
        } else if (state == WIFI_AP_STATE_ENABLED) {
            connectStatus = ConnectStatus.CONNECTED;
            sendEvent(NetworkEvent.CONNECTION_INFO_AVAILABLE);
        }
    }

    /**
     * handleFactoryReset
     *
     * Resets the device name and password to the factory default.
     */
    private void handleFactoryReset()
    {
        RobotLog.ww(TAG, "Received request to do access point factory reset");
        AppUtil.getInstance().showToast(UILocation.BOTH, "Resetting access point to default name and password", Toast.LENGTH_LONG);
        NetworkConnectionHandler.getInstance().injectReceivedCommand(new Command(RobotCoreCommandList.CMD_VISUALLY_CONFIRM_WIFI_RESET));
        try {
            Thread.sleep(400); // Make sure the Driver Station gets the toast
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        nameManager.resetDeviceName();
        passwordManager.resetPassword();
        apChannelManager.resetChannel();
    }

    /**
     * enable
     *
     * On the control hub, connected just means wifi is enabled and it's broadcasting a ssid.
     * As a tethered device, it's automatically connected to the access point.  Don't confuse
     * with "connected" to a driver station.
     */
    @Override
    public void enable()
    {
        synchronized (enableDisableLock) {
            if (receiver == null) {
                RobotLog.ii(TAG, "Enabling network services");
                receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (intent.getAction().equals(Intents.ANDROID_ACTION_WIFI_STATE_CHANGED)) {
                            handleWifiStateChange(intent);
                        } else if (intent.getAction().equals(Intents.ACTION_FTC_WIFI_FACTORY_RESET)) {
                            handleFactoryReset();
                        }
                    }
                };
                context.registerReceiver(receiver, intentFilter);
            }
        }
    }

    /**
     * disable
     *
     * Stop listening for wifi state changes.
     */
    @Override
    public void disable()
    {
        synchronized (enableDisableLock) {
            if (receiver != null) {
                context.unregisterReceiver(receiver);
                receiver = null;
            }
        }
    }

    /**
     * isConnected
     *
     * We really don't care if we are "connected" on the robot controller.  We just want to open
     * a socket to listen on.  As long as the AP is broadcasting on a Control Hub, connectivity between the DS and RC
     * exists.
     */
    @Override
    public boolean isConnected()
    {
        return (getConnectStatus() == ConnectStatus.CONNECTED);
    }

    /**
     * isWifiApEnabled
     */
    protected boolean isWifiApEnabled()
    {
        boolean enabled;

        try {
            Method setApConfig = wifiManager.getClass().getMethod("isWifiApEnabled");
            enabled = (Boolean)setApConfig.invoke(wifiManager);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return false;
        }

        return enabled;
    }

    /**
     * getConnectStatus
     *
     * Returns "connected" if wifi is enabled on the control hub.
     */
    @Override
    public ConnectStatus getConnectStatus()
    {
        if (isWifiApEnabled() == true) {
            RobotLog.ii(TAG, "Wifi AP is enabled");
            return ConnectStatus.CONNECTED;
        } else {
            RobotLog.ii(TAG, "Wifi AP is not enabled");
            return ConnectStatus.NOT_CONNECTED;
        }
    }

    @Override protected String getIpAddress() {
        return getConnectionOwnerAddress().getHostAddress();
    }

    /**
     * createConnection
     *
     * Sends the correct username and password info to the AP service
     */
    @Override
    public void createConnection()
    {
        RobotLog.ii(TAG, "Sending SSID and password to AP service");
        nameManager.setDeviceName(nameManager.getDeviceName());
        passwordManager.setPassword(passwordManager.getPassword());
    }

    /**
     * detectWifiReset
     *
     * Sends a WiFi factory reset intent if the Control Hub's button is being held down
     */
    @Override
    public void detectWifiReset() {
        RobotLog.dd(TAG, "detectWifiReset button=%b", AndroidBoard.getInstance().getUserButtonPin().getState());
        if (LynxConstants.isRevControlHub() && AndroidBoard.getInstance().getUserButtonPin().getState()) {
            RobotLog.ii(TAG, "Wifi settings reset requested through the Control Hub button");
            Intent wifiResetIntent = new Intent(Intents.ACTION_FTC_WIFI_FACTORY_RESET);
            context.sendBroadcast(wifiResetIntent);
        }
    }

    /**
     * getConnectionOwnerName
     *
     * Returns the ssid of the access point we are currently broadcasting.
     */
    @Override public String getConnectionOwnerName() {
        return DeviceNameManagerFactory.getInstance().getDeviceName();
    }

    @Override
    public String getPassphrase()
    {
       return PasswordManagerFactory.getInstance().getPassword();
    }

    @Override
    public void onWaitForConnection()
    {
        // While the AP is not yet started, we need to keep telling the AP service what the correct credentials are
        createConnection();
    }
}
