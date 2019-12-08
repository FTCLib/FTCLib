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

import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.IBinder;
import androidx.annotation.Nullable;

import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.external.Supplier;

import java.io.Closeable;
import java.io.IOException;

/**
 * {@link StartableService} provides the framework of a startable service
 * on top of the instantiation of a closeable object
 */
@SuppressWarnings("WeakerAccess")
public abstract class StartableService extends Service
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public abstract String getTag();

    protected final Supplier<Closeable>   instantiator;
    protected       Closeable             instance;

    //----------------------------------------------------------------------------------------------
    // Life Cycle
    //----------------------------------------------------------------------------------------------

    protected StartableService(Supplier<Closeable> instantiator)
        {
        this.instantiator = instantiator;
        }

    @Override public void onCreate()
        {
        RobotLog.vv(getTag(), "onCreate()");
        }

    @Override public int onStartCommand(Intent intent, int flags, int startId)
        {
        RobotLog.vv(getTag(), "onStartCommand() intent=%s flags=0x%x startId=%d", intent, flags, startId);

        instance = instantiator.get();

        return START_NOT_STICKY;
        }

    @Override public void onDestroy()
        {
        RobotLog.vv(getTag(), "onDestroy()");

        if (instance != null)
            {
            try {
                instance.close();
                }
            catch (IOException e)
                {
                RobotLog.ee(getTag(), e, "exception during close; ignored");
                }
            instance = null;
            }
        }

    @Override public void onConfigurationChanged(Configuration newConfig)
        {
        RobotLog.vv(getTag(), "onConfigurationChanged()");
        }

    @Override public void onLowMemory()
        {
        RobotLog.vv(getTag(), "onLowMemory()");
        }

    @Override public void onTrimMemory(int level)
        {
        RobotLog.vv(getTag(), "onTrimMemory()");
        }

    //----------------------------------------------------------------------------------------------
    // Binding
    //----------------------------------------------------------------------------------------------

    @Nullable @Override public IBinder onBind(Intent intent)
        {
        RobotLog.vv(getTag(), "onBind()");
        return null; // we're not this kind of service: we're a 'startable' not a 'bindable' one
        }

    @Override public boolean onUnbind(Intent intent)
        {
        RobotLog.vv(getTag(), "onUnbind()");
        return super.onUnbind(intent);
        }

    @Override public void onRebind(Intent intent)
        {
        RobotLog.vv(getTag(), "onRebind()");
        }

    @Override public void onTaskRemoved(Intent rootIntent)
        {
        RobotLog.vv(getTag(), "onTaskRemoved()");;
        }

    }
