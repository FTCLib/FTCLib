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
package org.firstinspires.ftc.robotcore.internal.system;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.qualcomm.robotcore.util.RobotLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * {@link ServiceController} provides control of and auto-starting for services
 */
@SuppressWarnings("WeakerAccess")
public class ServiceController
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "ServiceStarter";

    protected static final String metaDataAutoStartPrefix = "autoStartService.";

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    public static void onApplicationStart()
        {
        autoStartServices();
        }

    protected static class AutoStartableService
        {
        public String   className;
        public int      launchOrder;
        public AutoStartableService(String className, int launchOrder)
            {
            this.className = className;
            this.launchOrder = launchOrder;
            }
        }

    protected static List<AutoStartableService> getAutoStartableServices()
        {
        List<AutoStartableService> result = new ArrayList<AutoStartableService>();

        // Enumerate the metdata looking for autostart instructions
        try {
            PackageManager packageManager = AppUtil.getDefContext().getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(AppUtil.getInstance().getApplication().getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = applicationInfo.metaData;
            for (String key : bundle.keySet())
                {
                if (key.startsWith(metaDataAutoStartPrefix))
                    {
                    String value = bundle.getString(key);
                    String[] splits = value.split("\\|");
                    if (splits.length == 2)
                        {
                        if ("RC".equalsIgnoreCase(splits[0]) && AppUtil.getInstance().isRobotController()
                         || "DS".equalsIgnoreCase(splits[0]) && AppUtil.getInstance().isDriverStation()
                         || "BOTH".equalsIgnoreCase(splits[0]))
                            {
                            String serviceClassName = key.substring(metaDataAutoStartPrefix.length());
                            result.add(new AutoStartableService(serviceClassName, Integer.parseInt(splits[1])));
                            }

                        }
                    else
                        throw AppUtil.getInstance().failFast(TAG, "incorrect manifest construction");
                    }
                }
            }
        catch (PackageManager.NameNotFoundException e)   // we shouldn't configure classes we can't find
            {
            throw AppUtil.getInstance().unreachable(TAG, e);
            }

        // Sort the list according to priority. Smaller launch order starts sooner, so
        // sort in increasing order by that.
        Collections.sort(result, new Comparator<AutoStartableService>()
            {
            @Override
            public int compare(AutoStartableService lhs, AutoStartableService rhs)
                {
                int result = lhs.launchOrder - rhs.launchOrder;
                if (result==0)
                    {
                    result = lhs.className.compareTo(rhs.className);
                    }
                return result;
                }
            });
        return result;
        }

    protected static void autoStartServices()
        {
        List<AutoStartableService> autoStartableServices = getAutoStartableServices();
        for (AutoStartableService service : autoStartableServices)
            {
            try {
                startService(Class.forName(service.className));
                }
            catch (ClassNotFoundException e)
                {
                throw AppUtil.getInstance().failFast(TAG, e, "configured service not found");
                }
            }
        }

    public static boolean startService(Class serviceClass)
        {
        RobotLog.vv(TAG, "attempting to start service %s", serviceClass.getSimpleName());

        Context context = AppUtil.getDefContext();
        Intent intent = new Intent(context, serviceClass);
        try
            {
            ComponentName componentName = context.startService(intent);
            if (componentName == null)
                {
                RobotLog.ee(TAG, "unable to start service %s", serviceClass.getSimpleName());
                }
            else
                {
                RobotLog.vv(TAG, "started service %s", serviceClass.getSimpleName());
                return true;
                }
            }
        catch (SecurityException e)
            {
            RobotLog.ee(TAG, e, "unable to start service %s", serviceClass.getSimpleName());
            }
        return false;
        }

    public static boolean stopService(Class serviceClass)
        {
        RobotLog.vv(TAG, "attempting to stop service %s", serviceClass.getSimpleName());

        Context context = AppUtil.getDefContext();
        Intent intent = new Intent(context, serviceClass);
        try
            {
            context.stopService(intent);
            return true;
            }
        catch (SecurityException e)
            {
            RobotLog.ee(TAG, e, "unable to stop service %s", serviceClass.getSimpleName());
            }
        return false;
        }
    }
