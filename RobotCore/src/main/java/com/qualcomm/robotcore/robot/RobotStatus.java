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
package com.qualcomm.robotcore.robot;

import android.content.Context;

import com.qualcomm.robotcore.R;

/**
 * {@link RobotStatus} provides additional status information about a robot
 * beyond what is indicated in {@link RobotState} when the state is not 'running'.
 */
public enum RobotStatus
    {
    UNKNOWN, NONE, SCANNING_USB, ABORT_DUE_TO_INTERRUPT, WAITING_ON_WIFI, WAITING_ON_WIFI_DIRECT,
        WAITING_ON_NETWORK_CONNECTION, NETWORK_TIMED_OUT, STARTING_ROBOT, UNABLE_TO_START_ROBOT;

    public String toString(Context context)
        {
        switch (this)
            {
            case UNKNOWN:                   return context.getString(R.string.robotStatusUnknown);
            case NONE:                      return "";
            case SCANNING_USB:              return context.getString(R.string.robotStatusScanningUSB);
            case WAITING_ON_WIFI:           return context.getString(R.string.robotStatusWaitingOnWifi);
            case WAITING_ON_WIFI_DIRECT:    return context.getString(R.string.robotStatusWaitingOnWifiDirect);
            case WAITING_ON_NETWORK_CONNECTION: return context.getString(R.string.robotStatusWaitingOnNetworkConnection);
            case NETWORK_TIMED_OUT:         return context.getString(R.string.robotStatusNetworkTimedOut);
            case STARTING_ROBOT:            return context.getString(R.string.robotStatusStartingRobot);
            case UNABLE_TO_START_ROBOT:     return context.getString(R.string.robotStatusUnableToStartRobot);
            default:                        return context.getString(R.string.robotStatusInternalError);
            }
        }
    }
