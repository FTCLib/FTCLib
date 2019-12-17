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
package org.firstinspires.ftc.robotcore.internal.network;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;

import com.qualcomm.robotcore.R;
import com.qualcomm.robotcore.util.ClassUtil;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.external.Consumer;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.system.Assert;
import org.firstinspires.ftc.robotcore.internal.system.CallbackRegistrar;
import org.firstinspires.ftc.robotcore.internal.system.PreferencesHelper;

import java.lang.reflect.Method;
import java.util.Random;
import java.util.concurrent.TimeoutException;

/**
 * {@link WifiDirectDeviceNameManager} manages the name by which the current device (robot controller or
 * driver station) is known externally. By default, it tracks the WifiDirect name of the device.
 */
@SuppressWarnings("WeakerAccess")
public class WifiDirectDeviceNameManager extends WifiStartStoppable implements DeviceNameManager
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = NetworkDiscoveryManager.TAG + "_name";
    public String getTag() { return TAG; }

    protected Context                       context;
    protected SharedPreferences             sharedPreferences;
    protected PreferencesHelper             preferencesHelper;
    protected final Object                  callbackLock              = new Object();
    protected SharedPreferencesListener     sharedPreferencesListener = new SharedPreferencesListener();
    protected String                        defaultMadeUpDeviceName   = null;
    protected String                        wifiDirectName            = null;
    protected WifiAgentCallback             wifiAgentCallback         = new WifiAgentCallback();
    protected CallbackRegistrar<DeviceNameListener> callbacks         = new CallbackRegistrar<DeviceNameListener>();
    protected StartResult deviceNameManagerStartResult                = new StartResult();

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public WifiDirectDeviceNameManager()
        {
        super(WifiDirectAgent.getInstance());
        context             = AppUtil.getInstance().getApplication();
        sharedPreferences   = PreferenceManager.getDefaultSharedPreferences(context);
        preferencesHelper   = new PreferencesHelper(TAG, sharedPreferences);
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferencesListener);
        }

    @Override protected boolean doStart()
        {
        String uniquifier = generateNameUniquifier();
        defaultMadeUpDeviceName = AppUtil.getInstance().isRobotController()
                ? context.getString(R.string.device_name_format_rc, uniquifier)
                : context.getString(R.string.device_name_format_ds, uniquifier);
        return startWifiDirect();
        }

    protected String generateNameUniquifier()
        {
        Random random = new Random();
        String uniquifier = "";
        for (int i = 0; i < 4; i++)
            {
            int r = random.nextInt(26); // we choose uppercase-only for looks
            char ch = (char) (r < 26 ? ('A' + r) : ('a' + r - 26));
            uniquifier += ch;
            }
        return uniquifier;
        }

    @Override protected void doStop()
        {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferencesListener);
        stopWifiDirect();
        }

    //----------------------------------------------------------------------------------------------
    // Public API
    //----------------------------------------------------------------------------------------------

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

    /*
     * Note: it's unreasonable to call this unless WifiDirect is enabled, or rather, has been
     * enabled at least once on this device. From Android's WifiP2pServiceImpl:
     *
     *     class P2pEnabledState extends State {
     *
     *          public void enter() {
     *              if (DBG) logd(getName());
     *              sendP2pStateChangedBroadcast(true);
     *              mNetworkInfo.setIsAvailable(true);
     *              sendP2pConnectionChangedBroadcast();
     *              initializeP2pSettings();
     *          }
     *
     * It's the initializeP2pSettings() here that initializes the Wifi Direct device name. Note that
     * that happens *after* the state and connection changed broadcasts (!). Also note that we don't
     * *necessarily* get a WIFI_P2P_THIS_DEVICE_CHANGED_ACTION with the name in it: if this is a reboot
     * after name initialization, and something in the WIFI_P2P_THIS_DEVICE_CHANGED_ACTION state hasn't
     * changed to warrant a broadcast, that won't happen. So we might have to poll.
     *
     * Coping strategy: until we know it's valid, we'll return something fake. On the not-valid/valid
     * transition, we'll issue a 'name changed' announcement to the valid name.
     */
    /**
     * Returns the name by which this device is publicly known (in the UI, on the network, etc)
     * @return the name by which this device is publicly known (in the UI, on the network, etc)
     */
    @Override
    public synchronized @NonNull String getDeviceName()
        {
        initializeDeviceNameIfNecessary();
        return internalGetDeviceName();
        }

    /**
     * Sets the name by which this device is publicly known. Attempts in doing so to keep secondary
     * system names (e.g. the WifiDirect name, if applicable) in sync with this name.
     * @param deviceName the new name for the device
     */
    @Override
    public synchronized void setDeviceName(@NonNull String deviceName)
        {
        if (!validDeviceName(deviceName))
            {
            RobotLog.ee(TAG, "setDeviceName(%s): failed; invalid WiFi-Direct name", deviceName);
            return;
            }
        internalSetDeviceName(deviceName);
        }

    // The official rules of the FIRST Tech Challenge require that you
    // change the Wi-Fi name of your smartphones to include your team
    // number and '-RC' if the phone is a Robot Controller or '-DS' if
    // it is a Driver Station. A team can insert an additional dash and
    // a letter (starting with 'B' and continuing up the alphabet) if
    // the team has more than one set of Android phones.
    //
    // Note: The error checking is is done in the exposed public method,
    // since otherwise we could get into a weird state if the android
    // device already has the wrong name from setting it using the
    // Android Settings menu, and validating in the internal method may
    // not be allow us to to recover from it/change it.
    //
    // Also, we are allowing non-numeric prefix's for testing, but the
    // suffix must be DS and RC.
    //
    // Ex:
    //  first set of phones for a team;     9999-RC, 9999-DS
    //  next set of phones on team;         9999-B-RC, 9999-B-DS
    public static boolean validDeviceName(@NonNull String deviceName)
        {
        return deviceName.matches("[a-zA-Z0-9]+(-[a-zA-Z])?-(?i)(DS|RC)");
        }

    /**
     * Reverts the system to some randomly fabricated name.
     */
    @Override
    public void resetDeviceName()
        {
        initializeDeviceNameFromMadeUp();
        }

    //----------------------------------------------------------------------------------------------
    // Internal name management
    //----------------------------------------------------------------------------------------------

    /** Used to keep track of how our sense of the device name relates to what the system thinks.
     * Right now we only synchronized to the WifiDirect name, but in theory, we could synchronize
     * to something else, if we chose. */
    protected enum DeviceNameTracking { UNINITIALIZED, AWAITING_WIFIDIRECT, WIFIDIRECT }

    /** From android.provider.Settings: the WiFi peer-to-peer device name */
    protected static final String WIFI_P2P_DEVICE_NAME = "wifi_p2p_device_name";

    @Override
    public synchronized void initializeDeviceNameIfNecessary()
        {
        // Get from Wifi Direct if we're just starting out: ask nicely,
        // using the sanctioned public API.
        if (getDeviceNameTracking()==DeviceNameTracking.UNINITIALIZED)
            {
            initializeDeviceNameFromWifiDirect();
            }

        // If that failed, then go spelunking inside of Android
        if (getDeviceNameTracking()==DeviceNameTracking.UNINITIALIZED)
            {
            initializeDeviceNameFromAndroidInternal();
            }

        // If that still doesn't work, then post a temporary name, but let a
        // subsequent real WifiDirect name notification override.
        if (getDeviceNameTracking()==DeviceNameTracking.UNINITIALIZED)
            {
            initializeDeviceNameFromMadeUp();
            }

        Assert.assertTrue(getDeviceNameTracking()!=DeviceNameTracking.UNINITIALIZED);
        }

    @Override
    public boolean start(StartResult startResult)
        {
        return super.start(startResult);
        }

    @Override
    public void stop(StartResult startResult)
        {
        super.stop(startResult);
        }


    protected void initializeDeviceNameFromWifiDirect()
        {
        RobotLog.vv(TAG, "initializeDeviceNameFromWifiDirect()...");
        try {
            waitForWifiDirectName();
            RobotLog.vv(TAG, "initializeDeviceNameFromWifiDirect(): name=%s", wifiDirectName);
            setDeviceNameTracking(DeviceNameTracking.WIFIDIRECT);
            internalSetDeviceName(wifiDirectName);
            }
        catch (TimeoutException e)
            {
            // Couldn't get the wifiDirectName that way
            }
        finally
            {
            RobotLog.vv(TAG, "...initializeDeviceNameFromWifiDirect()");
            }
        }

    protected void initializeDeviceNameFromAndroidInternal()
        {
        RobotLog.vv(TAG, "initializeDeviceNameFromAndroidInternal()...");
        // Try using the Android-internal global setting (why not give it a whirl?)
        String deviceName = Settings.Global.getString(context.getContentResolver(), WIFI_P2P_DEVICE_NAME);
        if (deviceName != null)
            {
            RobotLog.vv(TAG, "initializeDeviceNameFromAndroidInternal(): name=%s", deviceName);
            setDeviceNameTracking(DeviceNameTracking.WIFIDIRECT);
            wifiDirectName = deviceName;
            internalSetDeviceName(deviceName);
            }
        RobotLog.vv(TAG, "...initializeDeviceNameFromAndroidInternal()");
        }

    protected void initializeDeviceNameFromMadeUp()
        {
        RobotLog.vv(TAG, "initializeDeviceNameFromMadeUp(): name=%s ...", defaultMadeUpDeviceName);
        setDeviceNameTracking(DeviceNameTracking.AWAITING_WIFIDIRECT);
        internalSetDeviceName(defaultMadeUpDeviceName);
        RobotLog.vv(TAG, "..initializeDeviceNameFromMadeUp()");
        }

    protected DeviceNameTracking getDeviceNameTracking()
        {
        return DeviceNameTracking.valueOf(preferencesHelper.readString(context.getString(R.string.pref_device_name_tracking), DeviceNameTracking.UNINITIALIZED.toString()));
        }

    protected void setDeviceNameTracking(DeviceNameTracking tracking)
        {
        preferencesHelper.writeStringPrefIfDifferent(context.getString(R.string.pref_device_name_tracking), tracking.toString());
        }

    protected String internalGetDeviceName()
        {
        return preferencesHelper.readString(context.getString(R.string.pref_device_name), defaultMadeUpDeviceName);
        }

    protected void internalSetDeviceName(@NonNull String deviceName)
        {
        preferencesHelper.writeStringPrefIfDifferent(context.getString(R.string.pref_device_name), deviceName);
        }

    //----------------------------------------------------------------------------------------------
    // Preferences
    //----------------------------------------------------------------------------------------------

    protected class SharedPreferencesListener implements SharedPreferences.OnSharedPreferenceChangeListener
        {
        @Override public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
            {
            if (key.equals(context.getString(R.string.pref_device_name)))
                {
                // Did the name *really* change? We use a second property, only ever written here,
                // to find out. This helps us avoid loops in the tracking dependencies
                //
                final String newDeviceName = internalGetDeviceName();
                if (preferencesHelper.writeStringPrefIfDifferent(context.getString(R.string.pref_device_name_old), newDeviceName))
                    {
                    RobotLog.vv(TAG, "deviceName pref changed: now=%s", newDeviceName);

                    // keep the wifi direct name in sync if they're supposed to be locked
                    if (getDeviceNameTracking()==DeviceNameTracking.WIFIDIRECT)
                        {
                        setWifiDirectDeviceName(newDeviceName);
                        }

                    // tell anyone else who's interested too
                    callbacks.callbacksDo(new Consumer<DeviceNameListener>()
                        {
                        @Override public void accept(DeviceNameListener callback)
                            {
                            callback.onDeviceNameChanged(newDeviceName);
                            }
                        });
                    }
                }
            }
        }

    //----------------------------------------------------------------------------------------------
    // Wifi Direct
    //----------------------------------------------------------------------------------------------

    protected class WifiAgentCallback implements WifiDirectAgent.Callback
        {
        @Override public void onReceive(Context context, Intent intent)
            {
            String action = intent.getAction();

            // WIFI_P2P_THIS_DEVICE_CHANGED_ACTION is a sticky broadcast, so we get usually
            // get this quickly once we register. But see above!
            if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action))
                {
                WifiP2pDevice wifiP2pDevice = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
                internalRememberWifiDirectName(wifiP2pDevice.deviceName);
                }
            }
        }

    protected void internalRememberWifiDirectName(@NonNull String wifiDirectName)
        {
        RobotLog.vv(TAG, "remembering wifiDirectName: %s...", wifiDirectName);
        synchronized (callbackLock)
            {
            Assert.assertNotNull(wifiDirectName);

            // Is this a different name than we previously knew?
            if (!wifiDirectName.equals(this.wifiDirectName))
                {
                // Update our in-memory copy of the WifiDirect name
                this.wifiDirectName = wifiDirectName;
                RobotLog.vv(TAG, "wifiDirectName=%s", wifiDirectName);

                // If our device name is supposed to track wifi direct, then update that too
                DeviceNameTracking tracking = getDeviceNameTracking();
                if (tracking==DeviceNameTracking.WIFIDIRECT || tracking==DeviceNameTracking.AWAITING_WIFIDIRECT)
                    {
                    // We have a real Wifi direct name in hand now, so enough of this AWAITING
                    if (tracking==DeviceNameTracking.AWAITING_WIFIDIRECT)
                        {
                        setDeviceNameTracking(DeviceNameTracking.WIFIDIRECT);
                        }

                    // Remember our device name as the wifi direct one
                    internalSetDeviceName(wifiDirectName);
                    }

                // Unstick waitForWifiDirectName
                callbackLock.notifyAll();
                }
            }
        RobotLog.vv(TAG, "...remembering wifiDirectName");
        }

    protected boolean startWifiDirect()
        {
        wifiDirectAgent.registerCallback(wifiAgentCallback);
        return wifiDirectAgent.start(wifiDirectAgentStarted);
        }

    protected void stopWifiDirect()
        {
        wifiDirectAgent.stop(wifiDirectAgentStarted);
        wifiDirectAgent.unregisterCallback(wifiAgentCallback);
        }

    /** Waits a good long while, but not forever, for the wifi direct name to be initialized */
    protected void waitForWifiDirectName() throws TimeoutException
        {
        RobotLog.vv(TAG, "waitForWifiDirectName() thread=%d...", Thread.currentThread().getId());
        try {
            synchronized (callbackLock)
                {
                int msTimeout = 1000;
                int msWaitQuantum = 100;
                ElapsedTime timer = new ElapsedTime();
                for (;;)
                    {
                    if (wifiDirectName != null)
                        break;
                    if (timer.milliseconds() >= msTimeout)
                        {
                        RobotLog.vv(TAG, "timeout in waitForWifiDirectName()");
                        throw new TimeoutException("timeout in waitForWifiDirectName()");
                        }
                    callbackLock.wait(msWaitQuantum);
                    }
                }
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            }
        finally
            {
            RobotLog.vv(TAG, "...waitForWifiDirectName()");
            }
        }

    /**
     * Attempts to set the Android Wifi Direct name of this device. No status as to whether the
     * attempt is successful or not is (currently) provided, there being no current motivation and
     * the asynchrony involved making the providing thereof not a trivial thing.
     * @param deviceName the new name for the device
     */
    protected void setWifiDirectDeviceName(@NonNull final String deviceName)
        {
        RobotLog.vv(TAG, "setWifiDirectDeviceName(%s)...", deviceName);
        synchronized (callbackLock)
            {
            if (wifiDirectName==null || !wifiDirectName.equals(deviceName))
                {
                RobotLog.vv(TAG, "setWifiDirectDeviceName(%s): changing", deviceName);

                Method method = ClassUtil.getDeclaredMethod(wifiDirectAgent.getWifiP2pManager().getClass(), "setDeviceName",
                        WifiP2pManager.Channel.class,
                        String.class,
                        WifiP2pManager.ActionListener.class);

                ClassUtil.invoke(wifiDirectAgent.getWifiP2pManager(), method, wifiDirectAgent.getWifiP2pChannel(), deviceName, new WifiP2pManager.ActionListener()
                    {
                    @Override public void onSuccess()
                        {
                        RobotLog.vv(TAG, "setWifiDirectDeviceName(%s): success", deviceName);
                        }
                    @Override public void onFailure(int reason)
                        {
                        RobotLog.ee(TAG, "setWifiDirectDeviceName(%s): failed; reason=%d", deviceName, reason);
                        }
                    });

                // If that actually made a change, we'll get a callback on our broadcast
                // receiver, and we'll update our internal wifiDirectName variable then.
                //
                // Or, at least mostly we do: are their cases when the system doesn't get that
                // to us correctly? Can we be robust against that? Let's be paranoid.
                //
                internalRememberWifiDirectName(deviceName);
                }
            }
        RobotLog.vv(TAG, "...setWifiDirectDeviceName(%s)", deviceName);
        }
    }
