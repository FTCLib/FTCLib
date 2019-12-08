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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.qualcomm.ftccommon.UpdateUI;
import com.qualcomm.robotcore.robot.RobotState;
import com.qualcomm.robotcore.robot.RobotStatus;

import org.firstinspires.ftc.robotcore.internal.network.NetworkStatus;
import org.firstinspires.ftc.robotcore.internal.network.PeerStatus;

/**
 * Instances of {@link RobotStateMonitor} can be registered with the {@link com.qualcomm.ftccommon.UpdateUI.Callback Callback}
 * of {@link UpdateUI} in order to receive notifications when certain event transitions
 * happen within the robot.
 */
public interface RobotStateMonitor
    {
    /**
     * Informs the monitor of the current state of the robot. This may or may not be
     * different from the state previously reported.
     *
     * @param robotState the current state of the robot.
     */
    void updateRobotState(@NonNull RobotState robotState);

    /**
     * Informs the monitor of the current status of the robot. This may or may not be
     * different from the status previously reported. The robot status provides additional
     * information when the robot is not in the running state.
     *
     * @param robotStatus the current status of the robot
     * @see RobotStatus
     */
    void updateRobotStatus(@NonNull RobotStatus robotStatus);


    /**
     * Informs the monitor of the current status of relationship to the peer application. This may
     * or may not be different from the status previously reported.
     *
     * @param peerStatus the relationship of this app to its peer
     * @see PeerStatus
     */
    void updatePeerStatus(@NonNull PeerStatus peerStatus);

    /**
     * Informs the monitor of the current status of the network connection. This may
     * or may not be different from the status previously reported.
     *
     * @param networkStatus the current status of the network connection
     * @param extra additional information useful in some statuses
     */
    void updateNetworkStatus(@NonNull NetworkStatus networkStatus, @Nullable String extra);

    /**
     * Informs the monitor that a critical error occurred in the robot, or clears
     * any error previously reported. This may or may not be different from the
     * error message previously reported.
     *
     * @param errorMessage the error message being reported, or null if the error is being cleared.
     */
    void updateErrorMessage(@Nullable String errorMessage);

    /**
     * Informs the monitor that a warning has occurred in the robot, or clears
     * any warning previously reported. This may or may not be different from the
     * warning message previously reported.
     *
     * @param warningMessage the warning message being reported, or null if the warning is being cleared.
     */
    void updateWarningMessage(@Nullable String warningMessage);
    }
