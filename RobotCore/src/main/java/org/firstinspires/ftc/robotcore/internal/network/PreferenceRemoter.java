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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.qualcomm.robotcore.robocol.Command;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.system.PreferencesHelper;

import java.util.Map;

/**
 * {@link PreferenceRemoter} has the responsibility of monitoring certain preference settings
 * from one device to the other
 */
@SuppressWarnings("WeakerAccess")
public abstract class PreferenceRemoter extends WifiStartStoppable
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected Context                   context;
    private   SharedPreferences         sharedPreferences;
    protected PreferencesHelper         preferencesHelper;
    protected SharedPreferences.OnSharedPreferenceChangeListener sharedPreferencesListener;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public PreferenceRemoter()
        {
        super(WifiDirectAgent.getInstance());

        context                   = AppUtil.getInstance().getApplication();
        sharedPreferences         = PreferenceManager.getDefaultSharedPreferences(context);
        preferencesHelper         = new PreferencesHelper(getTag(), sharedPreferences);
        sharedPreferencesListener = makeSharedPrefListener();

        dumpAllPrefs();
        }

    protected abstract SharedPreferences.OnSharedPreferenceChangeListener makeSharedPrefListener();

    protected void dumpAllPrefs()
        {
        RobotLog.vv(getTag(), "----- all preferences -----");
        for (Map.Entry<String, ?> pair : sharedPreferences.getAll().entrySet())
            {
            RobotLog.vv(getTag(), "name='%s' value=%s", pair.getKey(), pair.getValue());
            }
        }

    //----------------------------------------------------------------------------------------------
    // Starting and stopping
    //----------------------------------------------------------------------------------------------

    @Override protected boolean doStart() throws InterruptedException
        {
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferencesListener);
        return true;
        }

    @Override protected void doStop() throws InterruptedException
        {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferencesListener);
        }

    //----------------------------------------------------------------------------------------------
    // Remoting
    //----------------------------------------------------------------------------------------------

    protected void sendPreference(RobotControllerPreference pair)
        {
        RobotLog.vv(getTag(), "sending RC pref name=%s value=%s", pair.getPrefName(), pair.getValue());
        Command command = new Command(RobotCoreCommandList.CMD_ROBOT_CONTROLLER_PREFERENCE, pair.serialize());
        NetworkConnectionHandler.getInstance().sendCommand(command);
        }

    public abstract CallbackResult handleCommandRobotControllerPreference(String extra);

    }
