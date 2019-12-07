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

import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Build;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.LynxModuleMeta;
import com.qualcomm.robotcore.hardware.RobotCoreLynxModule;
import com.qualcomm.robotcore.hardware.RobotCoreLynxUsbDevice;
import com.qualcomm.robotcore.util.Device;
import com.qualcomm.robotcore.util.Hardware;
import com.qualcomm.robotcore.wifi.NetworkType;

import org.firstinspires.ftc.robotcore.internal.network.DeviceNameManager;
import org.firstinspires.ftc.robotcore.internal.network.DeviceNameManagerFactory;
import org.firstinspires.ftc.robotcore.internal.network.NetworkConnectionHandler;
import org.firstinspires.ftc.robotcore.internal.network.WifiDirectDeviceNameManager;
import org.firstinspires.ftc.robotcore.internal.network.WifiUtil;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.collections.SimpleGson;
import org.firstinspires.ftc.robotcore.internal.network.StartResult;
import org.firstinspires.ftc.robotcore.internal.network.WifiDirectAgent;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link InspectionState} contains the inspection state of either a RC or a DS
 */
@SuppressWarnings("WeakerAccess")
public class InspectionState
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String zteChannelChangePackage = "com.zte.wifichanneleditor"; // see also: LaunchActivityConstantsList.ZTE_WIFI_CHANNEL_EDITOR_PACKAGE;
    public static final String robotControllerPackage = "com.qualcomm.ftcrobotcontroller";
    public static final String driverStationPackage = "com.qualcomm.ftcdriverstation";

    public static final String noPackageVersion = "";
    private static String firmwareInspectionVersion = "N/A";


    public String manufacturer;
    public String model;
    public String osVersion;
    public String firmwareVersion;
    public int sdkInt;
    public boolean airplaneModeOn;
    public boolean bluetoothOn;
    public boolean wifiEnabled;
    public boolean wifiConnected;
    public boolean wifiDirectEnabled;
    public boolean wifiDirectConnected;
    public String deviceName;
    public double batteryFraction;
    public String zteChannelChangeVersion;
    public int    ztcChannelChangeVersionCode;
    public String robotControllerVersion;
    public int    robotControllerVersionCode;
    public String driverStationVersion;
    public int    driverStationVersionCode;
    public boolean isAppInventorInstalled;
    public boolean channelChangerRequired;
    public long    rxDataCount;
    public long    txDataCount;
    public long    bytesPerSecond;

    //----------------------------------------------------------------------------------------------
    // Construction and initialization
    //----------------------------------------------------------------------------------------------

    public InspectionState()
        {
        }

    public void initializeLocal()
        {
        DeviceNameManager nameManager = DeviceNameManagerFactory.getInstance();
        StartResult startResult = new StartResult();
        nameManager.start(startResult);
        initializeLocal(nameManager);
        nameManager.stop(startResult);
        }

    public void initializeLocal(DeviceNameManager nameManager)
        {
        this.manufacturer = Build.MANUFACTURER;
        this.model = Build.MODEL;
        this.osVersion = Build.VERSION.RELEASE;
        this.firmwareVersion = firmwareInspectionVersion;
        this.sdkInt = Build.VERSION.SDK_INT;
        this.airplaneModeOn = WifiUtil.isAirplaneModeOn();
        this.bluetoothOn = WifiUtil.isBluetoothOn();
        this.wifiEnabled = WifiUtil.isWifiEnabled();
        this.batteryFraction = getLocalBatteryFraction();

        this.zteChannelChangeVersion        = getPackageVersion(zteChannelChangePackage);
        this.ztcChannelChangeVersionCode    = getPackageVersionCode(zteChannelChangePackage);
        this.robotControllerVersion         = getPackageVersion(robotControllerPackage);
        this.robotControllerVersionCode     = getPackageVersionCode(robotControllerPackage);
        this.driverStationVersion           = getPackageVersion(driverStationPackage);
        this.driverStationVersionCode       = getPackageVersionCode(driverStationPackage);
        this.isAppInventorInstalled         = isAppInventorLocallyInstalled();
        this.deviceName                     = nameManager.getDeviceName();

        this.channelChangerRequired = Device.isZteSpeed()
                && Device.useZteProvidedWifiChannelEditorOnZteSpeeds()
                && AppUtil.getInstance().isRobotController();

        NetworkConnectionHandler networkConnectionHandler = NetworkConnectionHandler.getInstance();
        if (networkConnectionHandler.getNetworkType() == NetworkType.WIRELESSAP)
            {
            if (Device.isRevControlHub())
                {
                this.wifiEnabled = WifiUtil.isWifiApEnabled();
                if (this.wifiEnabled) this.wifiConnected = true;
                }
                else
                {
                this.deviceName = WifiUtil.getConnectedSsid();
                this.wifiDirectEnabled = WifiUtil.isWifiEnabled();
                this.wifiConnected = WifiUtil.isWifiConnected();
                }
            this.wifiDirectConnected = false;  // Not shown on inspection activity.  Why does it exist?
            }
            else
            {
            this.wifiConnected = WifiDirectAgent.getInstance().isWifiConnected();
            this.wifiDirectEnabled = WifiDirectAgent.getInstance().isWifiDirectEnabled();
            this.wifiDirectConnected = WifiDirectAgent.getInstance().isWifiDirectConnected();
            }
            this.rxDataCount = networkConnectionHandler.getRxDataCount();
            this.txDataCount = networkConnectionHandler.getTxDataCount();
            this.bytesPerSecond = networkConnectionHandler.getBytesPerSecond();
        }

    public static boolean isPackageInstalled(String packageVersion) { return !packageVersion.equals(noPackageVersion); }

    public boolean isRobotControllerInstalled()
        {
        return isPackageInstalled(robotControllerVersion);
        }
    public boolean isDriverStationInstalled()
        {
        return isPackageInstalled(driverStationVersion);
        }
    public boolean isChannelChangerInstalled()
        {
        return isPackageInstalled(zteChannelChangeVersion);
        }
    protected boolean isAppInventorInstalled()
        {
        return isAppInventorInstalled;
        }

    protected double getLocalBatteryFraction()
        {
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = AppUtil.getInstance().getApplication().registerReceiver(null, intentFilter);
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        return level / (double) scale;
        }

    protected int getPackageVersionCode(String packageName)
        {
        PackageManager pm = AppUtil.getDefContext().getPackageManager();
        try
            {
            return pm.getPackageInfo(packageName, PackageManager.GET_META_DATA).versionCode;
            }
        catch (PackageManager.NameNotFoundException e)
            {
            return 0;
            }
        }

    protected String getPackageVersion(String packageName)
        {
        PackageManager pm = AppUtil.getDefContext().getPackageManager();
        try
            {
            return pm.getPackageInfo(packageName, PackageManager.GET_META_DATA).versionName;
            }
        catch (PackageManager.NameNotFoundException e)
            {
            return noPackageVersion;
            }
        }

    protected boolean isAppInventorLocallyInstalled()
        {
        final PackageManager pm = AppUtil.getDefContext().getPackageManager();
        final List<ApplicationInfo> installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        if (installedApps != null)
            {
            for (ApplicationInfo app : installedApps)
                {
                if (app.packageName.startsWith("appinventor.ai_"))
                    {
                    return true;
                    }
                }
            }

        return false;
        }

    //----------------------------------------------------------------------------------------------
    // Serialization
    //----------------------------------------------------------------------------------------------

    public String serialize()
        {
        return SimpleGson.getInstance().toJson(this);
        }

    //----------------------------------------------------------------------------------------------
    // Firmware determination
    //----------------------------------------------------------------------------------------------

    public static InspectionState deserialize(String serialized)
        {
        return SimpleGson.getInstance().fromJson(serialized, InspectionState.class);
        }

    /*
     * getFirmwareVersions
     *
     * Returns a list of firmware versions for all connected expansion/control hubs
     */
    protected static List<String> getFirmwareVersions(HardwareMap hardwareMap)
        {
        List<String> versions = new ArrayList<String>();

        if (hardwareMap == null)
            {
            return versions;
            }

        List<RobotCoreLynxModule> lynxModules = hardwareMap.getAll(RobotCoreLynxModule.class);

        for (RobotCoreLynxModule lynxModule : lynxModules)
            {
            versions.add(lynxModule.getFirmwareVersionString());
            }
        return versions;
        }

    /*
     * getFirmwareDisplayVersion
     *
     * Returns displayable text that we can use for firmware version on the Inspection activity.
     * For simplification purposes, this will be either the firmware string, or in the case of multiple
     * hubs and mismatched firmare, the text "Mismatched".  This eliminates the need to have an arbitrary
     * number of firmware entries but prompts the user to go look at the firmware on the user's hubs.
     */
    public static String getFirmwareInspectionVersion(HardwareMap hardwareMap)
        {
        List<String> versions = getFirmwareVersions(hardwareMap);
        String first;

        if (versions.isEmpty())
            {
            return "N/A";
            }

        if (versions.size() == 1)
            {
            return formatFirmwareVersion(versions.get(0));
            }
        else
            {
            first = versions.get(0);
            for (String version : versions)
                {
                if (!version.equals(first))
                    {
                    return "Mismatched";
                    }
                }
            return formatFirmwareVersion(first);
            }
        }

    public static void cacheFirmwareInspectionVersion(HardwareMap hardwareMap)
        {
        firmwareInspectionVersion = getFirmwareInspectionVersion(hardwareMap);
        }

    /*
     * formatFirmwareVersion
     *
     * Strip the leading hardware revision, and all alphabetic chars.
     */
    protected static String formatFirmwareVersion(String version)
        {
        String tmp = version.substring(version.indexOf(',')+1).replaceAll("[a-zA-Z: ]*", "").replaceAll(",", ".");
        return tmp;
        }

    }
