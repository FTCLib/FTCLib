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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.util.SparseArray;
import android.view.KeyEvent;

import com.qualcomm.robotcore.hardware.Blinker;
import com.qualcomm.robotcore.hardware.LightBlinker;
import com.qualcomm.robotcore.hardware.LightMultiplexor;
import com.qualcomm.robotcore.hardware.configuration.LynxConstants;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.ThreadPool;

import org.firstinspires.ftc.robotcore.internal.hardware.android.AndroidBoard;
import org.firstinspires.ftc.robotcore.internal.hardware.android.DragonboardIndicatorLED;
import org.firstinspires.ftc.robotcore.internal.ui.InputManager;
import org.firstinspires.ftc.robotcore.internal.system.SystemProperties;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * {@link WifiDirectInviteDialogMonitor} monitors and controls the invite dialogs
 * for WifiDirect connection invitations. The implementation relies on enhancements
 * we've made to Android in our FTCAndroid build.
 */
@SuppressWarnings("WeakerAccess")
public class WifiDirectInviteDialogMonitor extends BroadcastReceiver
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "WifiDirectInviteMonitor";

    // These notifications are defined only in FTCAndroid
    public static final String WIFI_P2P_INVITE_DIALOG_SHOWING_ACTION = "android.net.wifi.p2p.INVITE_DIALOG_SHOWING";
    public static final String WIFI_P2P_INVITE_DIALOG_DISMISSED_ACTION = "android.net.wifi.p2p.INVITE_DIALOG_DISMISSED";
    public static final String EXTRA_WIFI_P2P_INVITE_DIALOG = "dialogIdentity";

    protected static volatile Blinker     uiLynxModule = null;

    protected       Context               context;
    protected       LightBlinker          indicatorLEDBlinker;
    protected       Blinker               lynxModulePushed;
    protected       InputManager          inputManager;
    protected       int                   acceptKey;
    protected       int                   cancelKey;
    protected final Set<Integer>          activeDialogs;
    protected final SparseArray<Future>   futures;
    protected       int                   msAcceptInterval = 10000;
    protected       int                   msDeclineInterval = 30000;
    protected       int                   msPollButtonInterval = 250;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public WifiDirectInviteDialogMonitor(Context context)
        {
        this.context              = context;
        this.indicatorLEDBlinker  = null;
        this.inputManager         = InputManager.getInstance();
        this.acceptKey            = keycode(SystemProperties.get("persist.ftcandroid.p2p.accept", null), KeyEvent.KEYCODE_VOLUME_MUTE);
        this.cancelKey            = keycode(SystemProperties.get("persist.ftcandroid.p2p.cancel", null), KeyEvent.KEYCODE_ESCAPE);
        this.activeDialogs        = new HashSet<Integer>();
        this.futures              = new SparseArray<Future>();
        }

    protected int keycode(String integerValue, int defKeyCode)
        {
        try {
            return Integer.valueOf(integerValue);
            }
        catch (Exception e)
            {
            return defKeyCode;
            }
        }

    public void startMonitoring()
        {
        RobotLog.vv(TAG, "startMonitoring()");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WIFI_P2P_INVITE_DIALOG_SHOWING_ACTION);
        intentFilter.addAction(WIFI_P2P_INVITE_DIALOG_DISMISSED_ACTION);

        context.registerReceiver(this, intentFilter);
        }

    public void stopMonitoring()
        {
        RobotLog.vv(TAG, "stopMonitoring()");
        stopBlinking();
        try
            {
            context.unregisterReceiver(this);
            }
        catch (IllegalArgumentException e)
            {
            // stop called w/o start: ignore
            }
        removeAllFutures();
        }

    //----------------------------------------------------------------------------------------------
    // Accessing
    //----------------------------------------------------------------------------------------------

    public static void clearUILynxModule()
        {
        RobotLog.vv(TAG, "clearUILynxModule()");
        uiLynxModule = null;
        }

    /** Remember which lynx module should be used for UI feedback regarding WiFi connections etc */
    public static void setUILynxModule(Blinker lynxModule)
        {
        RobotLog.vv(TAG, "setUILynxModule()");
        uiLynxModule = lynxModule;
        }

    //----------------------------------------------------------------------------------------------
    // Acceptance / dismissal
    //----------------------------------------------------------------------------------------------

    protected static String propertyForDialog(int dialogIdentity)
        {
        return "ftcandroid.p2p.dialog." + dialogIdentity;
        }

    protected void acceptInvitation(int dialogId)
        {
        RobotLog.dd(TAG, "accepting invitation for %d", dialogId);
        SystemProperties.set(propertyForDialog(dialogId), "accept");
        }

    protected void declineInvitation(int dialogId)
        {
        RobotLog.dd(TAG, "declining invitation for %d", dialogId);
        SystemProperties.set(propertyForDialog(dialogId), "decline");
        }

    protected void waitForUserButtonPress(final int dialogId)
        {
        synchronized (futures)
            {
            removeFuture(dialogId);
            final ElapsedTime timer = new ElapsedTime();
            final Future future = ThreadPool.getDefaultScheduler().scheduleAtFixedRate(new Runnable()
                {
                @Override public void run()
                    {
                    if (AndroidBoard.getInstance().getUserButtonPin().getState())
                        {
                        synchronized (futures)
                            {
                            removeFuture(dialogId);
                            acceptInvitation(dialogId);
                            }
                        }
                    else if (timer.milliseconds() > msDeclineInterval)
                        {
                        synchronized (futures)
                            {
                            removeFuture(dialogId);
                            declineInvitation(dialogId);
                            }
                        }
                    }
                }, msPollButtonInterval, msPollButtonInterval, TimeUnit.MILLISECONDS);
            futures.put(dialogId, future);
            }
        }

    protected void scheduleAcceptance(final int dialogId)
        {
        synchronized (futures)
            {
            removeFuture(dialogId);
            Future future = ThreadPool.getDefaultScheduler().schedule(new Runnable()
                {
                @Override public void run()
                    {
                    synchronized (futures)
                        {
                        removeFuture(dialogId);
                        acceptInvitation(dialogId);
                        }
                    }
                }, msAcceptInterval, TimeUnit.MILLISECONDS);
            futures.put(dialogId, future);
            }
        }

    protected void removeFuture(int dialogId)
        {
        synchronized (futures)
            {
            Future future = futures.get(dialogId);
            if (future != null)
                {
                future.cancel(false);
                futures.remove(dialogId);
                }
            }
        }

    protected void removeAllFutures()
        {
        synchronized (futures)
            {
            while (futures.size() > 0)
                {
                int dialogId = futures.keyAt(0);
                removeFuture(dialogId);
                }
            }
        }

    //----------------------------------------------------------------------------------------------
    // Monitoring
    //----------------------------------------------------------------------------------------------

    @Override public void onReceive(Context context, Intent intent)
        {
        final String action = intent.getAction();
        int dialogId;
        switch (action)
            {
            case WIFI_P2P_INVITE_DIALOG_SHOWING_ACTION:
                dialogId = intent.getIntExtra(EXTRA_WIFI_P2P_INVITE_DIALOG, -1);
                RobotLog.dd(TAG, "broadcast: %s dialog=%d", action, dialogId);
                synchronized (activeDialogs)
                    {
                    // WIFI_P2P_INVITE_DIALOG_SHOWING_ACTION will broadcast repeatedly while
                    // the dialog is up (for robustness). We want to take action only the
                    // first time that we see it.
                    if (!this.activeDialogs.contains(dialogId))
                        {
                        this.activeDialogs.add(dialogId);
                        // If we just went from no dialog to some dialog, then start giving feedback
                        if (this.activeDialogs.size()==1)
                            {
                            startBlinking();
                            }
                        // Wait until the button is pressed or a timeout occurs
                        waitForUserButtonPress(dialogId);
                        }
                    }
                break;

            case WIFI_P2P_INVITE_DIALOG_DISMISSED_ACTION:
                dialogId = intent.getIntExtra(EXTRA_WIFI_P2P_INVITE_DIALOG, -1);
                RobotLog.dd(TAG, "broadcast: %s dialog=%d", action, dialogId);
                synchronized (activeDialogs)
                    {
                    removeFuture(dialogId);
                    this.activeDialogs.remove(dialogId);
                    if (this.activeDialogs.size() == 0)
                        {
                        stopBlinking();
                        }
                    }
                break;
            }
        }

    protected synchronized void startBlinking()
        {
        // Make up a reasonably noticable set of colors
        List<Blinker.Step> steps = new ArrayList<Blinker.Step>();
        int msIncrement = 100;
        for (int i = 0; i < 5; i++)
            {
            steps.add(new Blinker.Step(Color.RED,    msIncrement,    TimeUnit.MILLISECONDS));
            steps.add(new Blinker.Step(Color.BLACK,  msIncrement /2, TimeUnit.MILLISECONDS));
            steps.add(new Blinker.Step(Color.YELLOW, msIncrement,    TimeUnit.MILLISECONDS));
            steps.add(new Blinker.Step(Color.BLACK,  msIncrement /2, TimeUnit.MILLISECONDS));
            }

        // Show on the DragonBoard LED if we're supposed to
        if (LynxConstants.useIndicatorLEDS())
            {
            indicatorLEDBlinker = new LightBlinker(LightMultiplexor.forLight(DragonboardIndicatorLED.forIndex(LynxConstants.INDICATOR_LED_INVITE_DIALOG_ACTIVE)));
            indicatorLEDBlinker.pushPattern(steps);
            }

        // Show on the UI Lynx module if we know who that is
        lynxModulePushed = uiLynxModule;
        if (lynxModulePushed != null)
            {
            lynxModulePushed.pushPattern(steps);
            }
        }

    protected synchronized void stopBlinking()
        {
        if (lynxModulePushed != null)
            {
            lynxModulePushed.popPattern();
            lynxModulePushed = null;
            }
        if (indicatorLEDBlinker != null)
            {
            indicatorLEDBlinker.popPattern();
            indicatorLEDBlinker = null;
            }
        }
    }
