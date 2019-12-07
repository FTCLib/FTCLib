/*
Copyright (c) 2016 Robert Atkinson

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of Robert Atkinson nor the names of his contributors may be used to
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

package org.firstinspires.inspection;

import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.qualcomm.robotcore.robocol.Command;
import com.qualcomm.robotcore.util.Device;
import com.qualcomm.robotcore.util.ThreadPool;
import com.qualcomm.robotcore.wifi.NetworkType;

import org.firstinspires.ftc.robotcore.internal.network.DeviceNameManagerFactory;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.network.NetworkConnectionHandler;
import org.firstinspires.ftc.robotcore.internal.network.RobotCoreCommandList;
import org.firstinspires.ftc.robotcore.internal.network.StartResult;
import org.firstinspires.ftc.robotcore.internal.network.WifiDirectAgent;
import org.firstinspires.ftc.robotcore.internal.ui.ThemedActivity;
import org.firstinspires.ftc.robotcore.internal.ui.UILocation;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public abstract class InspectionActivity extends ThemedActivity
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "InspectionActivity";
    @Override public String getTag() { return TAG; }
    @Override protected FrameLayout getBackBar() { return findViewById(R.id.backbar); }

    /*
     * To turn on traffic stats on the inspection activities, set this
     * and RecvLoopRunnable.DO_TRAFFIC_DATA to true.
     */
    private static final boolean SHOW_TRAFFIC_STATS = false;

    private static final String goodMark = "\u2713";    // a check mark
    private static final String badMark = "X";
    private static final String notApplicable = "N/A";

    private final boolean remoteConfigure = AppUtil.getInstance().isDriverStation();

    TextView widiName, widiConnected, wifiEnabled, batteryLevel, osVersion, firmwareVersion, airplaneMode, bluetooth, wifiConnected, appsStatus;
    TextView trafficCount, bytesPerSecond;
    TextView trafficCountLabel, bytesPerSecondLabel;
    TextView txtManufacturer, txtModel, txtAppVersion;
    TextView txtIsRCInstalled, txtIsDSInstalled, txtIsCCInstalled;
    Pattern teamNoRegex;
    Future refreshFuture = null;
    int textOk = AppUtil.getInstance().getColor(R.color.text_okay);
    int textWarning = AppUtil.getInstance().getColor(R.color.text_warning);
    int textError = AppUtil.getInstance().getColor(R.color.text_error);
    StartResult nameManagerStartResult = new StartResult();
    private boolean properWifiConnectedState;
    private boolean properBluetoothState;

    protected static final int RC_MIN_VERSIONCODE = 21;
    protected static final int DS_MIN_VERSIONCODE = 21;

    //----------------------------------------------------------------------------------------------
    // Life cycle
    //----------------------------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState)
        {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inspection);

        // Find our various bits on the screen
        txtIsRCInstalled = (TextView) findViewById(R.id.txtIsRCInstalled);
        txtIsDSInstalled = (TextView) findViewById(R.id.txtIsDSInstalled);
        txtIsCCInstalled = (TextView) findViewById(R.id.txtIsCCInstalled);

        widiName = (TextView) findViewById(R.id.widiName);
        trafficCount = (TextView) findViewById(R.id.trafficCount);
        bytesPerSecond = (TextView) findViewById(R.id.bytesPerSecond);
        trafficCountLabel = (TextView) findViewById(R.id.trafficCountLabel);
        bytesPerSecondLabel = (TextView) findViewById(R.id.bytesPerSecondLabel);
        widiConnected = (TextView) findViewById(R.id.widiConnected);
        wifiEnabled = (TextView) findViewById(R.id.wifiEnabled);
        batteryLevel = (TextView) findViewById(R.id.batteryLevel);
        osVersion = (TextView) findViewById(R.id.osVersion);
        firmwareVersion = (TextView) findViewById(R.id.hubFirmware);
        airplaneMode = (TextView) findViewById(R.id.airplaneMode);
        bluetooth = (TextView) findViewById(R.id.bluetoothEnabled);
        wifiConnected = (TextView) findViewById(R.id.wifiConnected);
        appsStatus = (TextView) findViewById(R.id.appsStatus);
        txtAppVersion = (TextView) findViewById(R.id.textDeviceName);

        txtAppVersion.setText(inspectingRobotController()
            ? getString(R.string.titleInspectionReportRC)
            : getString(R.string.titleInspectionReportDS));

        txtManufacturer = (TextView) findViewById(R.id.txtManufacturer);
        txtModel = (TextView) findViewById(R.id.txtModel);

        teamNoRegex = Pattern.compile("^\\d{1,5}(-\\w)?-(RC|DS)\\z", Pattern.CASE_INSENSITIVE);

        ImageButton buttonMenu = (ImageButton) findViewById(R.id.menu_buttons);
        if (useMenu())
            {
            buttonMenu.setOnClickListener(new View.OnClickListener()
                {
                @Override
                public void onClick(View v)
                    {
                    PopupMenu popupMenu = new PopupMenu(InspectionActivity.this, v);
                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            return onOptionsItemSelected(item); // Delegate to the handler for the hardware menu button
                        }
                    });
                    popupMenu.inflate(R.menu.main_menu);
                    popupMenu.show();
                    }
                });
            }
        else
            {
            buttonMenu.setEnabled(false);
            buttonMenu.setVisibility(View.INVISIBLE);
            }

        DeviceNameManagerFactory.getInstance().start(nameManagerStartResult);

        properWifiConnectedState = false;
        properBluetoothState = false;

        NetworkType networkType = NetworkConnectionHandler.getDefaultNetworkType(this);
        if (networkType == NetworkType.WIRELESSAP)
            {
            makeWirelessAPModeSane();
            }

        enableTrafficDataReporting(SHOW_TRAFFIC_STATS);

        // Off to the races
        refresh();
        }

    protected void enableTrafficDataReporting(boolean enable)
        {
        if (enable)
            {
            trafficCount.setVisibility(View.VISIBLE);
            bytesPerSecond.setVisibility(View.VISIBLE);
            trafficCountLabel.setVisibility(View.VISIBLE);
            bytesPerSecondLabel.setVisibility(View.VISIBLE);
            }
            else
            {
            trafficCount.setVisibility(View.GONE);
            bytesPerSecond.setVisibility(View.GONE);
            trafficCountLabel.setVisibility(View.GONE);
            bytesPerSecondLabel.setVisibility(View.GONE);
            }
        }

    protected void makeWirelessAPModeSane()
        {
        TextView labelWidiName = (TextView) findViewById(R.id.labelWidiName);
        labelWidiName.setText(getString(R.string.wifiAccessPointLabel));

        properWifiConnectedState = true;
        }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
        {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
        }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
        {
        // Historical note: we used to have other items on the menu as well, but
        // the ability to clear remembered groups is now available on the Settings screen,
        // and the ability to clear all wifi networks is (apparently) not available from M
        // onwards, and so is now of marginal utility. Thus, both of these items have
        // been removed.

        int id = item.getItemId();
        if (id == R.id.disconnect_from_wifidirect)
            {
            if (!remoteConfigure)
                {
                if (WifiDirectAgent.getInstance().disconnectFromWifiDirect())
                    {
                    showToast(getString(R.string.toastDisconnectedFromWifiDirect));
                    }
                else
                    {
                    showToast(getString(R.string.toastErrorDisconnectingFromWifiDirect));
                    }
                }
            else
                {
                NetworkConnectionHandler.getInstance().sendCommand(new Command(RobotCoreCommandList.CMD_DISCONNECT_FROM_WIFI_DIRECT));
                }
            return true;
            }

        return super.onOptionsItemSelected(item);
        }

    @Override
    protected void onResume()
        {
        super.onResume();
        startRefreshing();
        }

    @Override
    protected void onPause()
        {
        super.onPause();
        stopRefreshing();
        }

    @Override protected void onDestroy()
        {
        super.onDestroy();
        DeviceNameManagerFactory.getInstance().stop(nameManagerStartResult);
        }

    //----------------------------------------------------------------------------------------------
    // Refreshing
    //----------------------------------------------------------------------------------------------

    private void startRefreshing() {
        stopRefreshing();
        int msInterval = 5000;
        refreshFuture = ThreadPool.getDefaultScheduler().scheduleAtFixedRate(new Runnable() {
            @Override public void run() {
                AppUtil.getInstance().runOnUiThread(new Runnable() {
                    @Override public void run() {
                        refresh();
                    }
                });
            }
        }, msInterval, msInterval, TimeUnit.MILLISECONDS);
    }

    private void stopRefreshing() {
        if (refreshFuture != null) {
            refreshFuture.cancel(false);
            refreshFuture = null;
        }
    }

    private void refresh(TextView view, boolean value)
        {
        refresh(view, value, true);
        }
    private void refresh(TextView view, boolean value, boolean validValue)
        {
        view.setText(value ? goodMark : badMark);
        view.setTextColor(value==validValue ? textOk : textError);
        }
    private void refresh(TextView view, String value, boolean valid)
        {
        view.setText(value);
        view.setTextColor(valid ? textOk : textError);
        }
    private boolean refreshOptional(TextView view, String appVersion, boolean required)
        {
        boolean exists = InspectionState.isPackageInstalled(appVersion);
        if (required)
            {
            refresh(view, exists);
            return exists;
            }
        else
            {
            view.setText(notApplicable);
            view.setTextColor(textOk);
            return true;
            }
        }
    private boolean refreshPackage(TextView view, String version, int versionCode, int minVersion)
        {
        if (InspectionState.isPackageInstalled(version))
            {
            view.setText(version);
            if (versionCode < minVersion)
                {
                view.setTextColor(textWarning);
                return false;
                }
            else
                {
                view.setTextColor(textOk);
                }
            }
        else
            {
            view.setText(badMark);
            view.setTextColor(textOk);
            }
        return true;
        }
    private void refreshTrafficCount(TextView view, long rxData, long txData)
        {
        view.setText(String.format("%d/%d", rxData, txData));
        }
    private void refreshTrafficStats(InspectionState state)
        {
        if (SHOW_TRAFFIC_STATS)
            {
            refreshTrafficCount(trafficCount, state.rxDataCount, state.txDataCount);
            refresh(bytesPerSecond, state.bytesPerSecond);
            }
        }
    private void refresh(TextView view, long data)
        {
        view.setText(String.format("%d", data));
        }

    protected void refresh()
        {
        InspectionState state = new InspectionState();
        state.initializeLocal(DeviceNameManagerFactory.getInstance());
        refresh(state);
        }

    protected void refresh(InspectionState state)
        {
        // Set values
        refresh(widiConnected, state.wifiDirectConnected);
        refresh(wifiEnabled, state.wifiEnabled);
        refreshTrafficStats(state);
        refresh(airplaneMode, state.airplaneModeOn);
        refresh(bluetooth, state.bluetoothOn, properBluetoothState);
        refresh(wifiConnected, state.wifiConnected, properWifiConnectedState);
        txtManufacturer.setText(state.manufacturer);
        txtModel.setText(state.model);
        refresh(osVersion, state.osVersion, isValidOsVersion(state));
        refresh(firmwareVersion, state.firmwareVersion, isValidFirmwareVersion(state));
        refresh(widiName, state.deviceName, isValidDeviceName(state));
        batteryLevel.setText(Math.round(state.batteryFraction * 100f) + "%");
        batteryLevel.setTextColor(state.batteryFraction > 0.6 ? textOk : textWarning);

        // check the installed apps.
        boolean appsOkay = true;
        appsOkay = refreshOptional(txtIsCCInstalled, state.zteChannelChangeVersion, state.channelChangerRequired) && appsOkay;
        appsOkay = refreshPackage(txtIsRCInstalled, state.robotControllerVersion, state.robotControllerVersionCode, RC_MIN_VERSIONCODE) && appsOkay;
        appsOkay = refreshPackage(txtIsDSInstalled, state.driverStationVersion, state.driverStationVersionCode, DS_MIN_VERSIONCODE) && appsOkay;

        if (!state.isRobotControllerInstalled() && !state.isDriverStationInstalled()
            || state.isRobotControllerInstalled() && state.isDriverStationInstalled())
            {
            // you should have at least one or the other installed, but not both
            appsOkay = false;
            txtIsDSInstalled.setTextColor(textError);
            txtIsRCInstalled.setTextColor(textError);
            }

        appsOkay = validateAppsInstalled(state) && appsOkay;
        appsStatus.setTextColor(appsOkay ? textOk : textError);
        appsStatus.setText(appsOkay ? goodMark : badMark);
        }

    public boolean isValidOsVersion(InspectionState state)
        {
        if (Device.MANUFACTURER_ZTE.equalsIgnoreCase(state.manufacturer) && Device.MODEL_ZTE_SPEED.equalsIgnoreCase(state.model))
            {
            // ZTE Speed should be Kit Kat. ZTE did not upgrade Speed beyond Kit Kat (that we know of, at least)
            return (state.sdkInt >= Build.VERSION_CODES.KITKAT);
            }
        else
            {
            // for 2016-2017 season we recommend Marshmallow or higher.
            return (state.sdkInt >= Build.VERSION_CODES.M);
            }
        }

    public boolean isValidFirmwareVersion(InspectionState state)
        {
            return true;
        }

    public boolean isValidDeviceName(InspectionState state)
        {
        if (state.deviceName.contains("\n") || state.deviceName.contains("\r")) return false;
        return (teamNoRegex.matcher(state.deviceName)).find();
        }

    //----------------------------------------------------------------------------------------------
    // Subclass queries
    //----------------------------------------------------------------------------------------------

    protected abstract boolean validateAppsInstalled(InspectionState state);
    protected abstract boolean inspectingRobotController();
    protected abstract boolean useMenu();

    //----------------------------------------------------------------------------------------------
    // Utility
    //----------------------------------------------------------------------------------------------
    
    private void showToast(String message)
        {
        AppUtil.getInstance().showToast(UILocation.BOTH, message);
        }
    }
