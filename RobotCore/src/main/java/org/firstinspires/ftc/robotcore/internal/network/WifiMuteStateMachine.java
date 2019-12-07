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

package org.firstinspires.ftc.robotcore.internal.network;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.CountDownTimer;
import android.widget.TextView;
import android.widget.Toast;

import com.qualcomm.robotcore.R;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.external.Consumer;
import org.firstinspires.ftc.robotcore.external.Event;
import org.firstinspires.ftc.robotcore.external.State;
import org.firstinspires.ftc.robotcore.external.StateMachine;
import org.firstinspires.ftc.robotcore.external.StateTransition;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.system.CallbackRegistrar;
import org.firstinspires.ftc.robotcore.internal.system.Watchdog;
import org.firstinspires.ftc.robotcore.internal.ui.UILocation;

import java.util.concurrent.TimeUnit;

/**
 * State machine that manages the Wifi mute feature.
 */
public class WifiMuteStateMachine extends StateMachine {

    private final static String TAG = "WifiMuteStateMachine";
    private final static int WIFI_MUTE_TIMEOUT = 600;
    private final static int WIFI_MUTE_WARN = 10;
    private final static int WIFI_MUTE_PERIOD = 1;

    private WifiManager wifiManager;
    private Activity activity;

    private final WifiState blackhole = new WifiState();
    private final WifiOn wifiOn = new WifiOn();
    private final WifiPendingOff wifiPendingOff = new WifiPendingOff();
    private final WifiOff wifiOff = new WifiOff();
    private final TimeoutSuspended timeoutSuspended = new TimeoutSuspended();
    private final WifiMuteFragment wifiMuteFragment = new WifiMuteFragment();

    protected Watchdog wifiMuzzleWatchdog = new Watchdog(new WifiMuteRunnable(), new WifiGrowlRunnable(),
                            WIFI_MUTE_WARN, WIFI_MUTE_PERIOD, WIFI_MUTE_TIMEOUT, TimeUnit.SECONDS);


    /*****************************************************************************************
     * States
     *****************************************************************************************/

    /*
     * Simple base class to provide support for tracing state transitions.
     */
    private class WifiState implements State {

        @Override
        public void onEnter(Event event)
        {
            RobotLog.ii(TAG, "Enter State: " + getClass().getSimpleName());
        }

        @Override
        public void onExit(Event event)
        {
            RobotLog.ii(TAG, "Exit State: " + getClass().getSimpleName());
        }

        public String toString()
        {
            return getClass().getSimpleName();
        }
    }

    /**
     * State representing wifi on and the timeout watchdog is active.
     */
    private class WifiOn extends WifiState {

        boolean isEnabled = true;

        @Override
        public void onEnter(Event event)
        {
            super.onEnter(event);

            if (wifiMuzzleWatchdog.isRunning()) {
                wifiMuzzleWatchdog.stroke();
            } else {
                wifiMuzzleWatchdog.start();
            }

            if (!isWifiEnabled()) {
                AppUtil.getInstance().showToast(UILocation.ONLY_LOCAL, AppUtil.getDefContext().getString(R.string.toastEnableWifi));
                enableWifi(true);
                notifyWifiOn();
            }

            activity.getFragmentManager()
                    .beginTransaction()
                    .hide(wifiMuteFragment)
                    .commit();
        }
    }

    /**
     * State representing wifi off.
     */
    private class WifiOff extends WifiState {
        @Override
        public void onEnter(Event event)
        {
            super.onEnter(event);

            wifiMuteFragment.displayDisabledMessage();

            if (wifiMuzzleWatchdog.isRunning()) {
                wifiMuzzleWatchdog.euthanize();
            }

            if (isWifiEnabled()) {
                AppUtil.getInstance().showToast(UILocation.ONLY_LOCAL, AppUtil.getDefContext().getString(R.string.toastDisableWifi));
                enableWifi(false);
                notifyWifiOff();
            }
        }

        @Override
        public void onExit(Event event)
        {
        }
    }

    /**
     * State when we are warning the user that wifi is about to be
     * turned off.
     */
    private class WifiPendingOff extends WifiState {

        private final String msg = AppUtil.getDefContext().getString(R.string.toastDisableWifiWarn);

        /*
         * Simple little countdown mechanism.  For warning user.
         */
        CountDownTimer wifiOffNotificationTimer = new CountDownTimer(TimeUnit.SECONDS.toMillis(WIFI_MUTE_WARN), TimeUnit.SECONDS.toMillis(1)) {
            @Override
            public void onTick(long l)
            {
                wifiMuteFragment.setCountdownNumber(l/TimeUnit.SECONDS.toMillis(1));
            }

            @Override
            public void onFinish()
            {
                // noop
            }
        };

        @Override
        public void onEnter(Event event)
        {
            super.onEnter(event);

            wifiMuteFragment.reset();

            activity.getFragmentManager()
                    .beginTransaction()
                    .show(wifiMuteFragment)
                    .commit();

            notifyPendingOn();
            wifiOffNotificationTimer.start();
        }

        @Override
        public void onExit(Event event)
        {
            super.onExit(event);

            notifyPendingCancel();
            wifiOffNotificationTimer.cancel();
        }
    }

    /**
     * State representing wifi on and the timeout watchdog is suspended.
     */
    private class TimeoutSuspended extends WifiState {
        @Override
        public void onEnter(Event event)
        {
            super.onEnter(event);

            if (wifiMuzzleWatchdog.isRunning()) {
                wifiMuzzleWatchdog.euthanize();
            }
        }

        @Override
        public void onExit(Event event)
        {
            super.onExit(event);

            if (!wifiMuzzleWatchdog.isRunning()) {
                wifiMuzzleWatchdog.start();
            }
        }
    }

    protected final CallbackRegistrar<Callback> callbacks = new CallbackRegistrar<Callback>();
    public interface Callback {
        void onWifiOn();
        void onWifiOff();
        void onPendingOn();
        void onPendingCancel();
    }

    public void registerCallback(Callback callback)
    {
        callbacks.registerCallback(callback);
    }
    public void unregisterCallback(Callback callback)
    {
        callbacks.unregisterCallback(callback);
    }

    protected void notifyWifiOn()
    {
        callbacks.callbacksDo(new Consumer<Callback>()
        {
            @Override public void accept(Callback callback)
            {
                callback.onWifiOn();
            }
        });
    }

    protected void notifyWifiOff()
    {
        callbacks.callbacksDo(new Consumer<Callback>()
        {
            @Override public void accept(Callback callback)
            {
                callback.onWifiOff();
            }
        });
    }

    protected void notifyPendingOn()
    {
        callbacks.callbacksDo(new Consumer<Callback>()
        {
            @Override public void accept(Callback callback)
            {
                callback.onPendingOn();
            }
        });
    }

    protected void notifyPendingCancel()
    {
        callbacks.callbacksDo(new Consumer<Callback>()
        {
            @Override public void accept(Callback callback)
            {
                callback.onPendingCancel();
            }
        });
    }

    /**
     * WifiMuteStateMachine
     */
    public WifiMuteStateMachine()
    {
        wifiMuteFragment.setStateMachine(this);

        this.wifiManager = (WifiManager) AppUtil.getDefContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        this.activity = AppUtil.getInstance().getActivity();
        this.activity.getFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, wifiMuteFragment)
                .hide(wifiMuteFragment)
                .commit();
    }

    /**
     * start
     *
     * Start the state machine in it's initial state.
     */
    public void start()
    {
        super.start(blackhole);

        wifiMuzzleWatchdog.start();
    }

    /**
     * stop
     *
     * Stop the state machine and do no more monitoring.
     */
    public void stop()
    {
        if (wifiMuzzleWatchdog.isRunning()) {
            wifiMuzzleWatchdog.euthanize();
        }
    }

    /**
     * initialize
     *
     * Defines the state machine as a directed graph where a transition is defined as the exit
     * of one vertex to the entry of another.  By defining all exits to their respective adjacent
     * vertices, we define the entire graph.
     */
    public void initialize()
    {
        StateTransition transition;

        /**************************************************
         * WifiOn exit transitions
         **************************************************/

        transition = new StateTransition(
                wifiOn,
                WifiMuteEvent.USER_ACTIVITY,
                wifiOn);
        addTransition(transition);

        transition = new StateTransition(
                wifiOn,
                WifiMuteEvent.WATCHDOG_WARNING,
                wifiPendingOff);
        addTransition(transition);

        transition = new StateTransition(
                wifiOn,
                WifiMuteEvent.RUNNING_OPMODE,
                timeoutSuspended);
        addTransition(transition);

        transition = new StateTransition(
                wifiOn,
                WifiMuteEvent.ACTIVITY_STOP,
                wifiOff);
        addTransition(transition);

        transition = new StateTransition(
                wifiOn,
                WifiMuteEvent.ACTIVITY_OTHER,
                timeoutSuspended);
        addTransition(transition);

        /**************************************************
         * PendingOff exit transitions
         **************************************************/

        transition = new StateTransition(
                wifiPendingOff,
                WifiMuteEvent.USER_ACTIVITY,
                wifiOn);
        addTransition(transition);

        transition = new StateTransition(
                wifiPendingOff,
                WifiMuteEvent.WATCHDOG_TIMEOUT,
                wifiOff);
        addTransition(transition);

        /**************************************************
         * TimeoutSuspened exit transitions
         **************************************************/

        transition = new StateTransition(
                timeoutSuspended,
                WifiMuteEvent.STOPPED_OPMODE,
                wifiOn);
        addTransition(transition);

        transition = new StateTransition(
                timeoutSuspended,
                WifiMuteEvent.ACTIVITY_START,
                wifiOn);
        addTransition(transition);

        /**************************************************
         * WifiOff exit transitions
         **************************************************/

        transition = new StateTransition(
                wifiOff,
                WifiMuteEvent.USER_ACTIVITY,
                wifiOn);
        addTransition(transition);

        transition = new StateTransition(
                wifiOff,
                WifiMuteEvent.ACTIVITY_START,
                wifiOn);
        addTransition(transition);

        RobotLog.ii(TAG, "State Machine " + this.toString());
    }

    /**************************************************************************************************************
     * Utilities
     **************************************************************************************************************/

    protected void enableWifi(boolean enable)
    {
        RobotLog.ii(TAG, "Set wifi enable " + enable);
        if (enable) {
            AppUtil.getInstance().showToast(UILocation.ONLY_LOCAL, AppUtil.getDefContext().getString(R.string.toastEnableWifi));
        } else {
            AppUtil.getInstance().showToast(UILocation.ONLY_LOCAL, AppUtil.getDefContext().getString(R.string.toastDisableWifi));
        }
        wifiManager.setWifiEnabled(enable);
    }

    protected boolean isWifiEnabled()
    {
        return wifiManager.isWifiEnabled();
    }

    protected Toast makeToast(Activity activity, String msg)
    {
        Toast toast = Toast.makeText(activity.getApplicationContext(), msg, Toast.LENGTH_SHORT);
        TextView message = (TextView) toast.getView().findViewById(android.R.id.message);
        message.setTextColor(AppUtil.getColor(R.color.text_toast));
        message.setTextSize(18);
        toast.show();
        return toast;
    }

    /**
     * The watchdog barked.
     */
    private class WifiMuteRunnable implements Runnable {
        @Override
        public void run()
        {
            RobotLog.ii(TAG, "Watchdog barked");
            consumeEvent(WifiMuteEvent.WATCHDOG_TIMEOUT);
        }
    }

    /**
     * The watchdog warned.
     */
    private class WifiGrowlRunnable implements Runnable {
        @Override
        public void run()
        {
            RobotLog.ii(TAG, "Watchdog growled");
            consumeEvent(WifiMuteEvent.WATCHDOG_WARNING);
        }
    }

}
