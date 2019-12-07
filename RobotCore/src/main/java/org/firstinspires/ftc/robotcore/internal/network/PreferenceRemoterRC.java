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
import android.content.SharedPreferences;

import com.qualcomm.robotcore.R;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.util.HashSet;
import java.util.Set;

/**
 * {@link PreferenceRemoterRC} has the responsibility of monitoring certain preference settings
 * on the robot controller and transmitting them to the driver station
 */
@SuppressWarnings("WeakerAccess")
public class PreferenceRemoterRC extends PreferenceRemoter
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = NetworkDiscoveryManager.TAG + "_prefremrc";
    public String getTag() { return TAG; }

    @SuppressLint("StaticFieldLeak") protected static PreferenceRemoterRC theInstance = null;
    public synchronized static PreferenceRemoterRC getInstance()
        {
        if (null == theInstance) theInstance = new PreferenceRemoterRC();
        return theInstance;
        }

    protected Set<String> rcPrefsOfInterestToDS;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public PreferenceRemoterRC()
        {
        rcPrefsOfInterestToDS = new HashSet<String>();
        rcPrefsOfInterestToDS.add(context.getString(R.string.pref_device_name));
        rcPrefsOfInterestToDS.add(context.getString(R.string.pref_app_theme));
        rcPrefsOfInterestToDS.add(context.getString(R.string.pref_sound_on_off));
        rcPrefsOfInterestToDS.add(context.getString(R.string.pref_wifip2p_remote_channel_change_works));
        rcPrefsOfInterestToDS.add(context.getString(R.string.pref_wifip2p_channel));
        rcPrefsOfInterestToDS.add(context.getString(R.string.pref_has_independent_phone_battery));
        rcPrefsOfInterestToDS.add(context.getString(R.string.pref_has_speaker));
        }

    @Override
    protected SharedPreferences.OnSharedPreferenceChangeListener makeSharedPrefListener()
        {
        return new SharedPreferencesListenerRC();
        }

    //----------------------------------------------------------------------------------------------
    // Remoting
    //----------------------------------------------------------------------------------------------

    @Override public CallbackResult handleCommandRobotControllerPreference(String extra)
        {
        RobotControllerPreference pair = RobotControllerPreference.deserialize(extra);

        if (pair.getPrefName().equals(AppUtil.getDefContext().getString(R.string.pref_wifip2p_channel)))
            {
            if (pair.getValue() != null && pair.getValue() instanceof Integer)
                {
                // Just change the WifiP2p channel
                (new WifiDirectChannelChanger()).changeToChannel((Integer)pair.getValue());
                }
            else
                {
                RobotLog.ee(TAG, "incorrect preference value type: " + pair.getValue());
                }
            }
        else
            {
            // Otherwise, if we're asked to write a preference setting, we just do it
            preferencesHelper.writePrefIfDifferent(pair.getPrefName(), pair.getValue());
            }

        return CallbackResult.HANDLED;
        }

    //----------------------------------------------------------------------------------------------
    // Preferences
    //----------------------------------------------------------------------------------------------

    protected class SharedPreferencesListenerRC implements SharedPreferences.OnSharedPreferenceChangeListener
        {
        @Override public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String rcPrefName)
            {
            RobotLog.vv(TAG, "onSharedPreferenceChanged(name=%s, value=%s)", rcPrefName, preferencesHelper.readPref(rcPrefName));

            // If the DS wants to know about this one, then tell him
            if (rcPrefsOfInterestToDS.contains(rcPrefName))
                {
                sendPreference(rcPrefName);
                }
            }
        }

    protected void sendPreference(String rcPrefName)
        {
        Object value = preferencesHelper.readPref(rcPrefName);
        if (value != null)
            {
            sendPreference(new RobotControllerPreference(rcPrefName, value));
            }
        }

    public void sendAllPreferences()
        {
        RobotLog.vv(TAG, "sendAllPreferences()");
        for (String rcPrefName : rcPrefsOfInterestToDS)
            {
            sendPreference(rcPrefName);
            }
        }
    }
