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
import android.net.wifi.p2p.WifiP2pManager;
import android.widget.Toast;

import com.qualcomm.robotcore.R;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.system.PreferencesHelper;
import org.firstinspires.ftc.robotcore.internal.ui.UILocation;

/**
 * {@link WifiDirectChannelChanger} provides a simple API for changing WifiP2p channels.
 * It issues toasts on success or failure.
 */
@SuppressWarnings("WeakerAccess")
public class WifiDirectChannelChanger
    {
    public static final String TAG = "WifiDirectChannelChanger";

    private Context             context = AppUtil.getDefContext();
    private PreferencesHelper   preferencesHelper = new PreferencesHelper(TAG, context);
    private WifiDirectAgent     wifiDirectAgent = WifiDirectAgent.getInstance();
    private int                 channel = 0;
    private int                 listenChannel = 0;
    private volatile boolean    isChangingChannels = false;

    private void issueSuccessToast()
        {
        AppUtil.getInstance().showToast(UILocation.BOTH, context.getString(R.string.setWifiChannelSuccess, WifiDirectChannelAndDescription.getDescription(channel)), Toast.LENGTH_LONG);
        }

    private void issueFailureToast()
        {
        AppUtil.getInstance().showToast(UILocation.BOTH, context.getString(R.string.setWifiChannelFailure, WifiDirectChannelAndDescription.getDescription(channel)), Toast.LENGTH_LONG);
        }

    private void startChannelChange(int channel)
        {
        RobotLog.vv(TAG, "startChannelChange() channel=%d", channel);
        isChangingChannels = true;
        this.channel = channel;
        }

    private void finishChannelChange(boolean success)
        {
        RobotLog.vv(TAG, "finishChannelChange() channel=%d success=%s", channel, success);
        if (success)
            issueSuccessToast();
        else
            issueFailureToast();
        isChangingChannels = false;
        }

    public boolean isBusy()
        {
        return isChangingChannels;
        }

    /** Asynchronously changes the channel; issues a toast when finished */
    public void changeToChannel(int newChannel)
        {
        RobotLog.vv(TAG, "changeToChannel() channel=%d", newChannel);
        startChannelChange(newChannel);
        AppUtil.getInstance().runOnUiThread(new Runnable()
            {
            @Override public void run()
                {
                // it appears that the listen channel must be a 2.4GHz or auto channel.
                // if the new channel is > 11 (i.e., it's a 5GHz channel) use 0 as the listen channel.
                if (channel > 11)
                    {
                    listenChannel = 0;
                    }
                else
                    {
                    listenChannel = channel;
                    }
                wifiDirectAgent.setWifiP2pChannels(listenChannel, channel, new WifiP2pManager.ActionListener()
                    {
                    @Override public void onSuccess()
                        {
                        RobotLog.vv(TAG, "callSetWifiP2pChannels() success");

                        // Save the preference so we have a hope in heck of knowing what we last set to (though
                        // unfortunately, that can't be authoritive).
                        preferencesHelper.writePrefIfDifferent(context.getString(R.string.pref_wifip2p_channel), channel);

                        RobotLog.vv(TAG, "Channel %d saved as preference \"pref_wifip2p_channel\".", channel);
                        finishChannelChange(true);
                        }

                    @Override public void onFailure(int reason)
                        {
                        // Log reason for failure.
                        switch(reason)
                            {
                            case WifiP2pManager.P2P_UNSUPPORTED:
                                RobotLog.vv(TAG, "callSetWifiP2pChannels() failure (P2P_UNSUPPORTED)");
                                break;

                            case WifiP2pManager.BUSY:
                                RobotLog.vv(TAG, "callSetWifiP2pChannels() failure (BUSY)");
                                break;

                            case WifiP2pManager.ERROR:
                                RobotLog.vv(TAG, "callSetWifiP2pChannels() failure (ERROR)");
                                break;

                            default:
                                RobotLog.vv(TAG, "callSetWifiP2pChannels() failure (unknown reason)");
                            }

                        finishChannelChange(false);
                        }
                    });
                }
            });
        }

    }
