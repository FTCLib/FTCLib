/*
Copyright (c) 2017 Robert Atkinson

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
package org.firstinspires.ftc.robotcore.internal.ui;

import android.app.Activity;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.qualcomm.robotcore.R;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.system.PreferencesHelper;

/**
 * {@link ThemedActivity} supports the dynamic theming of activities, even
 * on API 19
 */
@SuppressWarnings("WeakerAccess")
public abstract class ThemedActivity extends BaseActivity
    {
    //----------------------------------------------------------------------------------------------
    // Life Cycle
    //----------------------------------------------------------------------------------------------

    @Override protected void onCreate(@Nullable Bundle savedInstanceState)
        {
        appAppThemeToActivity(getTag(), this);
        super.onCreate(savedInstanceState);
        }

    public static void appAppThemeToActivity(String tag, Activity activity)
        {
        // Find / initialize the app theme
        PreferencesHelper preferencesHelper = new PreferencesHelper(tag, activity);
        String pref_app_theme = activity.getString(R.string.pref_app_theme);
        String tokenCur = preferencesHelper.readString(pref_app_theme, activity.getString(R.string.tokenThemeRed));
        preferencesHelper.writePrefIfDifferent(pref_app_theme, tokenCur);

        String[] tokens = activity.getResources().getStringArray(R.array.app_theme_tokens);
        TypedArray ar = activity.getResources().obtainTypedArray(R.array.app_theme_ids);

        boolean found = false;
        for (int i = 0; i < tokens.length; i++)
            {
            if (tokens[i].equals(tokenCur))
                {
                int themeId = ar.getResourceId(i, 0);
                activity.setTheme(themeId);
                found = true;
                break;
                }
            }
        if (!found)
            {
            activity.setTheme(ar.getResourceId(0, 0));
            }
        ar.recycle();
        }

    public void restartForAppThemeChange(@StringRes final int idToast)
        {
        restartForAppThemeChange(getTag(), getString(idToast));
        }

    public static void restartForAppThemeChange(final String tag, final String toast)
        {
        final AppUtil appUtil = AppUtil.getInstance();

        RobotLog.vv(tag, "app theme changed: restarting app: %s", toast);
        appUtil.runOnUiThread(new Runnable()
            {
            @Override public void run()
                {
                appUtil.showToast(UILocation.BOTH, toast);
                (new Handler()).postDelayed(new Runnable()
                    {
                    @Override public void run()
                        {
                        appUtil.restartApp(0);
                        }
                    }, 1250);
                }
            });
        }
    }
