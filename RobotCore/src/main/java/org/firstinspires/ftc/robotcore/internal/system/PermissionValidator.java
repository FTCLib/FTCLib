/*
 * Copyright (c) 2018 Craig MacFarlane
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * (subject to the limitations in the disclaimer below) provided that the following conditions are
 * met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the
 * distribution.
 *
 * Neither the name of Craig MacFarlane nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS LICENSE. THIS
 * SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.firstinspires.ftc.robotcore.internal.system;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.qualcomm.robotcore.util.RobotLog;

import java.util.ArrayList;
import java.util.List;

public final class PermissionValidator {

    private final String TAG = "PermissionValidator";

    private static Activity activity;
    private PermissionListener listener;
    private PreferencesHelper preferencesHelper;
    private List<String> asked;

    private enum PermissionState {
        GRANTED,
        DENIED,
        PERMANENTLY_DENIED
    }

    private static final int SOME_RANDOM_NUMBER = 1;

    public PermissionValidator(Activity activity, PermissionListener listener) {
        this.activity = activity;
        this.listener = listener;
        this.preferencesHelper = new PreferencesHelper(TAG);
        this.asked = new ArrayList<String>();
    }

    protected void requestPermission(String permission) {
        ActivityCompat.requestPermissions(activity, new String[]{permission}, SOME_RANDOM_NUMBER);
        asked.add(permission);
    }

    /*
     * Preferences tracking if "Don't ask again" has been selected.  Massively annoying that google
     * doesn't expose an api for this.
     */
    protected PermissionState getPermissionState(String permission) {
        if (ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED) {
            preferencesHelper.writeBooleanPrefIfDifferent(permission, false);
            return PermissionState.GRANTED;
        } else {
            if (preferencesHelper.readBoolean(permission, false)) {
                /*
                 * Permission permanently denied.
                 */
                return PermissionState.PERMANENTLY_DENIED;
            } else {
                /*
                 * Permission denied.
                 */
                return PermissionState.DENIED;
            }
        }
    }

    public void checkPermission(String permission) {
        RobotLog.ii(TAG, "Checking permission for " + permission);
        switch (getPermissionState(permission)) {
            case GRANTED:
                RobotLog.ii(TAG, "    Granted: " + permission);
                listener.onPermissionGranted(permission);
                break;
            case DENIED:
                RobotLog.ii(TAG, "    Denied: " + permission);
                requestPermission(permission);
                break;
            case PERMANENTLY_DENIED:
                RobotLog.ii(TAG, "    Permanently denied: " + permission);
                listener.onPermissionPermanentlyDenied(permission);
                break;
        }
    }

    public void explain(final String permission) {

        AlertDialog.Builder alert = new AlertDialog.Builder(activity);
        alert.setMessage(((PermissionValidatorActivity)activity).mapPermissionToExplanation(permission));
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                checkPermission(permission);
            }
        });
        alert.create();
        alert.show();
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                RobotLog.ee(TAG, "You must grant permission to %s.", permissions[i]);
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permissions[i]) == false) {
                    RobotLog.ee(TAG, "PR permanently denied: " + permissions[i]);
                    preferencesHelper.writeBooleanPrefIfDifferent(permissions[i], true);
                }
                listener.onPermissionDenied(permissions[i]);
            } else {
                listener.onPermissionGranted(permissions[i]);
            }
        }
    }
}
