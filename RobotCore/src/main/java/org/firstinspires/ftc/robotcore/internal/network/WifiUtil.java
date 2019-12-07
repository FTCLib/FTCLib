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
package org.firstinspires.ftc.robotcore.internal.network;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;

import com.qualcomm.robotcore.R;
import com.qualcomm.robotcore.util.Device;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.internal.hardware.android.AndroidBoard;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.system.Misc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/*
 * A collection of useful wifi related utilities.
 */
public class WifiUtil {

    private static final String NO_AP = "None";
    private static boolean showingLocationServicesDlg = false;

    public static boolean isAirplaneModeOn()
    {
        return Settings.Global.getInt(AppUtil.getDefContext().getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    public static boolean isBluetoothOn()
    {
        return BluetoothAdapter.getDefaultAdapter() != null && BluetoothAdapter.getDefaultAdapter().isEnabled();
    }

    public static boolean isWifiEnabled()
    {
        WifiManager mgr = getWifiManager();
        int state = mgr.getWifiState();
        RobotLog.i("state = " + state);
        return getWifiManager().isWifiEnabled();
    }

    public static boolean isWifiApEnabled()
    {
        WifiManager wifiMgr = getWifiManager();
        try {
            Method isEnabled = wifiMgr.getClass().getMethod("isWifiApEnabled");
            return (Boolean)isEnabled.invoke(wifiMgr);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            RobotLog.e("Could not invoke isWifiApEnabled " + e.toString());
        }

        return false;
    }

    public static boolean isWifiConnected()
    {
        /*
         * The supplicant state may be in a state of obtaining an ip address even when wifi is not enabled!
         * Ergo, one can not rely upon the WifiManager alone to determine connection state.
         */
        if (!isWifiEnabled()) {
            return false;
        }

        WifiManager m = getWifiManager();
        SupplicantState s = m.getConnectionInfo().getSupplicantState();
        NetworkInfo.DetailedState state = WifiInfo.getDetailedStateOf(s);

        return (state == NetworkInfo.DetailedState.CONNECTED ||
                state == NetworkInfo.DetailedState.OBTAINING_IPADDR);
    }

    public static void doLocationServicesCheck()
    {
        if ((Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) || (AppUtil.getInstance().isRobotController()) || (showingLocationServicesDlg)) {
            return;
        }

        int locationMode = 0;
        try {
            locationMode = Settings.Secure.getInt(AppUtil.getDefContext().getContentResolver(), Settings.Secure.LOCATION_MODE);
        } catch (Settings.SettingNotFoundException e) {
        }

        if (locationMode == Settings.Secure.LOCATION_MODE_OFF) {
            AlertDialog.Builder alert = new AlertDialog.Builder(AppUtil.getInstance().getActivity());
            alert.setMessage(Misc.formatForUser(R.string.locationServices));
            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i)
                {
                    AppUtil.getInstance().getActivity().startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    showingLocationServicesDlg = false;
                }
            });
            alert.create();
            alert.show();
            showingLocationServicesDlg = true;
        }
    }

    /*
     * getConnectedSsid
     *
     * This will return the name of an access point a device is connected.  Note however that
     * it will not return results if the device is acting as an access point.  e.g. Don't invoke
     * this on a control hub and expect it to return what you expect it to.
     */
    public static String getConnectedSsid()
    {
        if (!isWifiConnected()) {
            return NO_AP;
        } else {
            WifiInfo wifiInfo = getWifiManager().getConnectionInfo();
            return wifiInfo.getSSID().replace("\"", "");
        }
    }

    /*
     * is5GHzAvailable
     *
     * Answers whether or not this device has a 5GHz radio.
     */
    public static boolean is5GHzAvailable()
    {
        if (Device.isRevControlHub()) {
            return AndroidBoard.getInstance().supports5GhzAp();
        } else if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            // it's a kit kat device or lower.
            // assume 5GHz is not available;
            return false;
        } else {
            WifiManager wifiManager = getWifiManager();
            return wifiManager.is5GHzBandSupported();
        }
    }

    protected static WifiManager getWifiManager()
    {
        return (WifiManager)AppUtil.getDefContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }
}
