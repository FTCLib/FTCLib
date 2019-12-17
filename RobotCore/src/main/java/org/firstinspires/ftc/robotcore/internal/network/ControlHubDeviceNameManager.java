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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import com.qualcomm.robotcore.R;
import com.qualcomm.robotcore.util.AndroidSerialNumberNotFoundException;
import com.qualcomm.robotcore.util.Device;
import com.qualcomm.robotcore.util.Intents;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.ShortHash;

import org.firstinspires.ftc.robotcore.external.Consumer;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.system.Assert;
import org.firstinspires.ftc.robotcore.internal.system.CallbackRegistrar;
import org.firstinspires.ftc.robotcore.internal.system.PreferencesHelper;

import java.util.zip.CRC32;

@SuppressWarnings("WeakerAccess")
public class ControlHubDeviceNameManager implements DeviceNameManager {

    protected enum DeviceNameTracking { UNINITIALIZED, WIFIAP };

    private static final ControlHubDeviceNameManager theInstance = new ControlHubDeviceNameManager();
    private static final String TAG = NetworkDiscoveryManager.TAG + "_ControlHubNameManager";
    private static final String MISSING_SERIAL_SSID = "FTC-MISSING-SERIAL";
    private static final int MAX_SSID_CHARS = 4;

    private String deviceName;
    private String defaultMadeUpDeviceName;
    private Context context;
    private SharedPreferences sharedPreferences;
    private PreferencesHelper preferencesHelper;
    private CallbackRegistrar<DeviceNameListener> callbacks = new CallbackRegistrar<DeviceNameListener>();
    private SharedPreferencesListener sharedPreferencesListener = new SharedPreferencesListener();

    public static ControlHubDeviceNameManager getControlHubDeviceNameManager()
    {
        RobotLog.i(TAG, "Getting name manager");
        return theInstance;
    }

    public ControlHubDeviceNameManager()
    {
        context             = AppUtil.getDefContext();
        sharedPreferences   = PreferenceManager.getDefaultSharedPreferences(context);
        preferencesHelper   = new PreferencesHelper(TAG, sharedPreferences);
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferencesListener);
    }


    protected void initializeDeviceNameFromSharedPrefs()
    {
        deviceName = internalGetDeviceName();
        if (deviceName != null) {
            setDeviceNameTracking(DeviceNameTracking.WIFIAP);
            internalSetDeviceName(deviceName);
        }
    }

    /**
     * handleFactoryReset
     *
     * Generate a short hash of the board serial number.  A number of different common hash algorithms were
     * evaluated, but none of them could generate reliably short enough hashes where short <= 4.
     *
     * The approach here to get to <= 4 is very simple.  Convert the board serial number into a CRC32 value so that
     * we ensure we have a 32 bit reasonably unique number given _any_ possible alphanumeric serial number of _any_
     * length. Take the 32 bit value, modulus it to a maximum of a 4 digit base <ShortHash.alphabetLength> number, and run that
     * through ShortHash, which converts to base alphabetLength while guaranteeing the output to be family friendly, which
     * in this usage is base 44.  See: http://hashids.org/java
     *
     * Collisions depend upon the probability that the 44^4-1 possible values in the low order bits of
     * the crc will be the same.
     *
     * For the test board the author has been given the serial number 3cdc2e50 this produces case sensitive 53wE.
     */
    protected String handleFactoryReset()
    {
        RobotLog.dd(TAG, "handleFactoryReset");
        String serialNumber = null;
        try {
            serialNumber = Device.getSerialNumber();
        } catch (AndroidSerialNumberNotFoundException e) {
            RobotLog.ee(TAG, "Failed to find Android serial number. Setting SSID to " + MISSING_SERIAL_SSID);
            return MISSING_SERIAL_SSID;
        }

        RobotLog.dd(TAG, "Serial: %s", serialNumber);
        CRC32 serialCrc32 = new CRC32();
        ShortHash hashid = new ShortHash("FiRsTiNsPiReS");
        int base = hashid.getAlphabetLength();
        int maxNum = (int)Math.pow(base, MAX_SSID_CHARS) - 1;
        int modulusMaxNum;
        String hash;

        serialCrc32.update(serialNumber.getBytes());
        modulusMaxNum = (int)(serialCrc32.getValue() % maxNum);
        hash = hashid.encode(modulusMaxNum);

        return ("FTC-" + hash);
    }

    protected void initializeDeviceNameFromMadeUp()
    {
        RobotLog.vv(TAG, "initializeDeviceNameFromMadeUp(): name=%s ...", defaultMadeUpDeviceName);
        defaultMadeUpDeviceName = handleFactoryReset();
        setDeviceNameTracking(DeviceNameTracking.WIFIAP);
        internalSetDeviceName(defaultMadeUpDeviceName);
        RobotLog.vv(TAG, "..initializeDeviceNameFromMadeUp()");
    }

    @NonNull
    @Override
    public String getDeviceName()
    {
        initializeDeviceNameIfNecessary();
        return internalGetDeviceName();
    }

    @Override
    public void setDeviceName(@NonNull String deviceName)
    {
        internalSetDeviceName(deviceName);
    }

    /**
     * resetDeviceName
     *
     * Performs a factory reset of this device name.
     */
    @Override
    public void resetDeviceName()
    {
        initializeDeviceNameFromMadeUp();
    }

    /**
     * initializeDeviceNameIfNecessary
     *
     * The control hub is the center of the naming universe.  It dictates to the access point service what
     * name the access point should broadcast.  If no name is stored on the control hub, then the control hub
     * fabricates one to dictate to the access point what it should broadcast.  The access point service will
     * cache the last known name, but it in no way controls that name.
     */
    public synchronized void initializeDeviceNameIfNecessary()
    {
        // Look in shared preferences for a name.
        if (getDeviceNameTracking()==DeviceNameTracking.UNINITIALIZED)
        {
            initializeDeviceNameFromSharedPrefs();
        }

        // Nothing in shared preferences, go fabricate a name.
        if (getDeviceNameTracking()==DeviceNameTracking.UNINITIALIZED)
        {
            initializeDeviceNameFromMadeUp();
        }

        Assert.assertTrue(getDeviceNameTracking()!=DeviceNameTracking.UNINITIALIZED);

        // Make sure we are sync'd with the access point service.
        internalSetDeviceName(internalGetDeviceName());
    }

    /**
     * internalSetDeviceName
     *
     * Sync the device name over to the access point service and write to prefs.
     */
    protected void internalSetDeviceName(@NonNull final String deviceName)
    {
        RobotLog.ii(TAG, "Robot controller name: " + deviceName);

        // Even if the name isn't changing on our end, we should take this opportunity to make sure
        // the AP service has the correct information, as is done by ControlHubPasswordManager.

        Intent intent = new Intent(Intents.ACTION_FTC_AP_NAME_CHANGE);
        intent.putExtra(Intents.EXTRA_AP_PREF, deviceName);
        context.sendBroadcast(intent);

        // pref_device_name_internal is only ever set here. So our name really did change if and only
        // if that property changed.
        if (preferencesHelper.writeStringPrefIfDifferent(context.getString(R.string.pref_device_name_internal), deviceName)) {
            // Make sure that the non-internal notion of the name tracks that
            preferencesHelper.writeStringPrefIfDifferent(context.getString(R.string.pref_device_name), deviceName);

            // Do internal bookkeeping
            this.deviceName = deviceName;

            // Tell our listeners
            callbacks.callbacksDo(new Consumer<DeviceNameListener>() {
                @Override public void accept(DeviceNameListener callback) {
                    callback.onDeviceNameChanged(deviceName);
                }
            });
        }
    }

    /**
     * internalSetAccessPointPassword
     *
     * Send the password over to the access point service, but do not store it locally.
     */
    protected void internalSetAccessPointPassword(@NonNull String password)
    {
        Intent intent = new Intent(Intents.ACTION_FTC_AP_PASSWORD_CHANGE);
        intent.putExtra(Intents.EXTRA_AP_PREF, password);
        context.sendBroadcast(intent);
    }

    /**
     * getDeviceNameTracking
     *
     * Returns the state of the device name.  Note that we can not tolerate device name data corruption,
     * hence if for any reason a read of the preferred device name property or the internal copy, returns the
     * empty string then UNITITALIZED to force a reset to a default name.
     * @return
     */
    protected DeviceNameTracking getDeviceNameTracking()
    {
        String deviceName;
        String deviceNameInternal;

        deviceName = preferencesHelper.readString(context.getString(R.string.pref_device_name), "");
        deviceNameInternal = preferencesHelper.readString(context.getString(R.string.pref_device_name_internal), "");
        if (deviceName.isEmpty() || deviceNameInternal.isEmpty()) {
            return DeviceNameTracking.UNINITIALIZED;
        } else {
            try {
                return DeviceNameTracking.valueOf(preferencesHelper.readString(context.getString(R.string.pref_device_name_tracking), DeviceNameTracking.UNINITIALIZED.toString()));
            } catch (Exception e) {
                return DeviceNameTracking.UNINITIALIZED;
            }
        }
    }


    /**
     * setDeviceNameTracking
     *
     * Sets the device name tracking property in shared preferences.
     */
    protected void setDeviceNameTracking(DeviceNameTracking tracking)
    {
        preferencesHelper.writeStringPrefIfDifferent(context.getString(R.string.pref_device_name_tracking), tracking.toString());
    }

    /**
     * internalGetDeviceName
     */
    protected String internalGetDeviceName()
    {
        return preferencesHelper.readString(context.getString(R.string.pref_device_name_internal), defaultMadeUpDeviceName);
    }

    @Override
    public boolean start(StartResult startResult) {
        return true; // degenerate
    }

    @Override
    public void stop(StartResult startResult) {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferencesListener);
    }

    /**
     * Instances of {@link DeviceNameListener} can be used to inform interested parties when the
     * name of the device is changed.
     */
    @Override
    public void registerCallback(DeviceNameListener callback)
    {
        callbacks.registerCallback(callback);
        // Always make sure they see an initial value
        callback.onDeviceNameChanged(getDeviceName());
    }
    @Override
    public void unregisterCallback(DeviceNameListener callback)
    {
        callbacks.unregisterCallback(callback);
    }

    //----------------------------------------------------------------------------------------------
    // Preferences
    //----------------------------------------------------------------------------------------------

    protected class SharedPreferencesListener implements SharedPreferences.OnSharedPreferenceChangeListener
    {
        @Override public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
        {
            if (key.equals(context.getString(R.string.pref_device_name))) {
                // Either we have just ourselves changed the name (in which case internalSetDeviceName
                // will catch that and stop the recursion) or someone *else* has asked us to change the
                // name, and we need to honor that.
                String newName = sharedPreferences.getString(key, defaultMadeUpDeviceName);
                internalSetDeviceName(newName);
            }
        }
    }
}
