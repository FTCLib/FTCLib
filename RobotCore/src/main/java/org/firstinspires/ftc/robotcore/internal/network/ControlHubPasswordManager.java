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
import com.qualcomm.robotcore.util.Intents;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.system.PreferencesHelper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Stuff to manage passwords.  Note that at present they are not stored by the robot controller
 * application, which implies that the web server does not have access to the password, and hence can't
 * reasonably prepopulate anything in the password field.  But they are stored in cleartext elsewhere.
 * See comments below.
 */
public class ControlHubPasswordManager implements PasswordManager {

    private final static String TAG = "ControlHubPasswordManager";
    private final static String FACTORY_DEFAULT_PASSWORD = "password";

    private String password;
    private Context context;
    private SharedPreferences sharedPreferences;
    private PreferencesHelper preferencesHelper;

    public ControlHubPasswordManager()
    {
        context = AppUtil.getInstance().getApplication();
        sharedPreferences   = PreferenceManager.getDefaultSharedPreferences(context);
        preferencesHelper   = new PreferencesHelper(TAG, sharedPreferences);
    }

    private StringBuffer stringify(byte[] hash)
    {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < hash.length; i++) {
            String byteStr = Integer.toHexString(hash[i] & 0xFF);
            if (byteStr.length() == 1) {
                byteStr = '0' + byteStr;
            }
            buf.append(byteStr);
        }
        return buf;
    }

    private String toSha256(@NonNull String password)
    {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuffer stringBuffer = stringify(hash);
            return stringBuffer.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean validatePassword(String password)
    {
        // TODO: Implement for reals.  But don't be a jerk about password requirements.
        if ((password.length() < 8) || (password.length() > 128)) {
            RobotLog.ee(TAG, "Invalid password length of " + password.length() + " chars.");
            return false;
        } else {
            return true;
        }
    }

    /**
     * setPassword
     *
     * For those asking, "Why the cleartext passwords?"  The access point service that establishes
     * the access point that the device will broadcast needs the cleartext password to pass to
     * setWifiApConfiguration upon boot of the device.  We can't give it a hashed password or users
     * won't be able to connect to the AP.  This presents a bit of a security hole as we need to store
     * this cleartext password somewhere on the device and I'm unaware of any support for secure storage
     * on the dragon boards.   We'll leave the basic unsalted hashing above in place as it may come
     * in useful during phone to phone AP pairing as opposed to phone to control hub pairing.
     *
     * @param password
     */
    @Override
    public boolean setPassword(@NonNull String password)
    {
        if (validatePassword(password) == false) {
            return false;
        }

        internalSetDevicePassword(password);

        RobotLog.vv(TAG, "Sending password change intent");
        Intent intent = new Intent(Intents.ACTION_FTC_AP_PASSWORD_CHANGE);
        intent.putExtra(Intents.EXTRA_AP_PREF, password);
        context.sendBroadcast(intent);
        return true;
    }

    protected void internalSetDevicePassword(@NonNull String password)
    {
        RobotLog.ii(TAG, "Robot controller password: " + password);

        preferencesHelper.writeStringPrefIfDifferent(
                context.getString(R.string.pref_connection_owner_password), password);
    }

    protected void initializePasswordIfNecessary()
    {
        password = preferencesHelper.readString(
                context.getString(R.string.pref_connection_owner_password), "");
        if(password.isEmpty()) {
            preferencesHelper.writeStringPrefIfDifferent(
            context.getString(R.string.pref_connection_owner_password), FACTORY_DEFAULT_PASSWORD);
        }

        password = preferencesHelper.readString(
                context.getString(R.string.pref_connection_owner_password), "");

        if(password.isEmpty()) {
            throw new IllegalStateException("Password not set");
        }
    }

    /**
     * resetPassword
     *
     * Resets the password to the factory default.
     */
    @Override
    public boolean resetPassword()
    {
        return setPassword(FACTORY_DEFAULT_PASSWORD);
    }

    @Override
    public String getPassword()
    {
        initializePasswordIfNecessary();

        String toRtn = preferencesHelper.readString(
                context.getString(R.string.pref_connection_owner_password),
                FACTORY_DEFAULT_PASSWORD);

        RobotLog.i(toRtn);
        return toRtn;
    }
}
