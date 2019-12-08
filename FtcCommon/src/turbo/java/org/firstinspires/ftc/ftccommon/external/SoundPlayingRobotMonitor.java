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
package org.firstinspires.ftc.ftccommon.external;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;

import com.qualcomm.ftccommon.R;
import com.qualcomm.ftccommon.SoundPlayer;
import com.qualcomm.robotcore.robot.RobotState;
import com.qualcomm.robotcore.robot.RobotStatus;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.external.function.Consumer;
import org.firstinspires.ftc.robotcore.internal.network.NetworkStatus;
import org.firstinspires.ftc.robotcore.internal.network.PeerStatus;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link SoundPlayingRobotMonitor} is an implementation of {@link RobotStateMonitor} that
 * plays sounds at certain event transitions within the Robot Controller application.
 */
@SuppressWarnings("WeakerAccess")
public class SoundPlayingRobotMonitor implements RobotStateMonitor
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static boolean DEBUG = false;
    protected Context       context;
    protected RobotState    robotState     = RobotState.UNKNOWN;
    protected RobotStatus   robotStatus    = RobotStatus.UNKNOWN;
    protected NetworkStatus networkStatus  = NetworkStatus.UNKNOWN;
    protected PeerStatus    peerStatus     = PeerStatus.UNKNOWN;
    protected String        errorMessage   = null;
    protected String        warningMessage = null;
    protected Sound         lastSoundPlayed = Sound.None;
    protected AtomicInteger runningsInFlight = new AtomicInteger(0);

    // Identity of the sounds played by this monitor. Users can change these
    // instance variables in order to cause different sounds to be played.
    public static @RawRes int soundConnect    = R.raw.ss_r2d2_up;
    public static @RawRes int soundDisconnect = R.raw.ss_bb8_down;
    public static @RawRes int soundRunning    = R.raw.ss_light_speed;
    public static @RawRes int soundWarning    = R.raw.ss_mine;
    public static @RawRes int soundError      = R.raw.ss_mf_fail;

    protected enum Sound { None, Connect, Disconnect, Running, Warning, Error }

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public SoundPlayingRobotMonitor()
        {
        this(AppUtil.getInstance().getApplication());
        }
    public SoundPlayingRobotMonitor(Context context)
        {
        this.context = context;
        }

    public static void prefillSoundCache()
        {
        SoundPlayer.getInstance().prefillSoundCache(soundConnect, soundDisconnect, soundRunning, soundWarning, soundError);
        }

    //----------------------------------------------------------------------------------------------
    // Notifications
    //----------------------------------------------------------------------------------------------

    protected void playConnect()
        {
        if (!SoundPlayer.getInstance().isLocalSoundOn())
            {
            // If the last sound played is 'running', but that sound was in fact transmitted
            // to the remote before this 'connect' happened, then (probably) the remote didn't
            // hear the 'running', so send it out again. This is a pretty reliable but not
            // perfect heuristic. Fortunately, the failure mode is only that a sound is repeated, 
            // and we can live with that.
            if (lastSoundPlayed==Sound.Running)
                {
                if (runningsInFlight.get() == 0)
                    {
                    RobotLog.vv(SoundPlayer.TAG, "playing running again");
                    playRunning();
                    }
                }
            }

        playSound(Sound.Connect, soundConnect);
        }

    protected void playDisconnect()
        {
        playSound(Sound.Disconnect,soundDisconnect);
        }

    protected void playRunning()
        {
        runningsInFlight.getAndIncrement();
        // This might be better decrementing on 'finish' instead of 'start', but all the testing has
        // been done on 'start' so we'll leave it that way for now.
        playSound(Sound.Running, soundRunning, new Consumer<Integer>()
            {
            @Override public void accept(Integer nonZeroOnSuccess)
                {
                runningsInFlight.decrementAndGet();
                }
            }, null);
        }

    protected void playWarning()
        {
        playSound(Sound.Warning, soundWarning);
        }

    protected void playError()
        {
        playSound(Sound.Error, soundError);
        }

    @Override public synchronized void updateRobotState(@NonNull RobotState robotState)
        {
        if (robotState != this.robotState)
            {
            if (DEBUG) RobotLog.vv(SoundPlayer.TAG, "updateRobotState(%s)", robotState.toString());
            switch (robotState)
                {
                case NOT_STARTED:         break;
                case INIT:                break;
                case STOPPED:             break;
                case EMERGENCY_STOP:      break;
                default:                  break;
                case RUNNING:
                    playRunning();
                    break;
                }
            }
        this.robotState = robotState;
        }

    @Override public synchronized void updateRobotStatus(@NonNull RobotStatus robotStatus)
        {
        if (robotStatus != this.robotStatus)
            {
            if (DEBUG) RobotLog.vv(SoundPlayer.TAG, "updateRobotStatus(%s)", robotStatus.toString());
            switch (robotStatus)
                {
                case NONE:                  break;
                default:                    break;
                }
            }
        this.robotStatus = robotStatus;
        }

    @Override public void updatePeerStatus(@NonNull PeerStatus peerStatus)
        {
        if (peerStatus != this.peerStatus)
            {
            if (DEBUG) RobotLog.vv(SoundPlayer.TAG, "updatePeerStatus(%s)", peerStatus.toString());
            switch (peerStatus)
                {
                case UNKNOWN:               break;
                case CONNECTED:             if (this.peerStatus != PeerStatus.CONNECTED) playConnect(); break;
                case DISCONNECTED:          if (this.peerStatus != PeerStatus.DISCONNECTED) playDisconnect(); break;
                default:                    break;
                }
            }
        this.peerStatus = peerStatus;
        }

    @Override public synchronized void updateNetworkStatus(@NonNull NetworkStatus networkStatus, @Nullable String extra)
        {
        if (networkStatus != this.networkStatus)
            {
            if (DEBUG) RobotLog.vv(SoundPlayer.TAG, "updateNetworkStatus(%s)", networkStatus.toString());
            switch (networkStatus)
                {
                case UNKNOWN:               break;
                case ACTIVE:                break;
                case INACTIVE:              break;
                case ENABLED:               break;
                case ERROR:                 break;
                case CREATED_AP_CONNECTION: break;
                default:                    break;
                }
            }
        this.networkStatus = networkStatus;
        }

    @Override public synchronized void updateErrorMessage(@Nullable String errorMessage)
        {
        if (errorMessage != null && !errorMessage.equals(this.errorMessage))
            {
            if (DEBUG) RobotLog.vv(SoundPlayer.TAG, "updateErrorMessage()");
            playError();
            }
        this.errorMessage = errorMessage;
        }

    @Override public synchronized void updateWarningMessage(@Nullable String warningMessage)
        {
        if (warningMessage != null && !warningMessage.equals(this.warningMessage))
            {
            if (DEBUG) RobotLog.vv(SoundPlayer.TAG, "updateWarningMessage()");
            playWarning();
            }
        this.warningMessage = warningMessage;
        }

    protected void playSound(Sound sound,@RawRes final int resourceId)
        {
        playSound(sound, resourceId, null, null);
        }

    protected void playSound(Sound sound, @RawRes final int resourceId, @Nullable Consumer<Integer> runWhenStarted, @Nullable Runnable runWhenFinished)
        {
        lastSoundPlayed = sound;
        SoundPlayer.getInstance().startPlaying(context, resourceId, new SoundPlayer.PlaySoundParams(true), runWhenStarted, runWhenFinished);
        }
    }
