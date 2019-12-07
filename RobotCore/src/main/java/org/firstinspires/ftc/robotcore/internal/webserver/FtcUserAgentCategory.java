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
package org.firstinspires.ftc.robotcore.internal.webserver;

import android.content.Context;
import android.content.pm.PackageManager;

import com.qualcomm.robotcore.util.Version;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.util.regex.Pattern;

/**
 * For the js code, {@link FtcUserAgentCategory} distinguishes the RC and DS embedded clients from others
 */
@SuppressWarnings("WeakerAccess")
public enum FtcUserAgentCategory
    {
        DRIVER_STATION, ROBOT_CONTROLLER, OTHER;

    public static final String TAG = FtcUserAgentCategory.class.getSimpleName();
    private static final String uaRobotController = "FtcRobotController";
    private static final String uaDriverStation = "FtcDriverStation";
    private static final Pattern patternRobotController = Pattern.compile("[\\s]*" + uaRobotController + "/", 0);
    private static final Pattern patternDriverStation = Pattern.compile("[\\s]*" + uaDriverStation + "/", 0);

    public static FtcUserAgentCategory fromUserAgent(String userAgent)
        {
        FtcUserAgentCategory result =
                (patternRobotController.matcher(userAgent).find())
                    ? ROBOT_CONTROLLER
                    : ((patternDriverStation.matcher(userAgent).find())
                        ? DRIVER_STATION
                        : OTHER);
        return result;
        }

    // https://en.wikipedia.org/wiki/User_agent
    public static String addToUserAgent(String existingUserAgent)
        {
        String suffix = "";
        Context context = AppUtil.getDefContext();
        String appVersion;
        try
            {
            appVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
            }
        catch (PackageManager.NameNotFoundException e)
            {
            appVersion = "3.1";
            }

        if (AppUtil.getInstance().isRobotController())
            {
            suffix = String.format(" %s/%s (library:%s)", uaRobotController, appVersion, Version.getLibraryVersion());
            }
        else if (AppUtil.getInstance().isDriverStation())
            {
            suffix = String.format(" %s/%s (library:%s)", uaDriverStation, appVersion, Version.getLibraryVersion());
            }
        return existingUserAgent + suffix;
        }



    }
