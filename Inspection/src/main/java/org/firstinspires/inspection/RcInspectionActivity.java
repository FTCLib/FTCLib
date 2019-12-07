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
package org.firstinspires.inspection;

import android.os.Bundle;

import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.robocol.Command;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.network.CallbackResult;
import org.firstinspires.ftc.robotcore.internal.network.NetworkConnectionHandler;
import org.firstinspires.ftc.robotcore.internal.network.RecvLoopRunnable;
import org.firstinspires.ftc.robotcore.internal.network.RobotCoreCommandList;
import org.firstinspires.ftc.robotcore.internal.ui.UILocation;

public class RcInspectionActivity extends InspectionActivity
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    final boolean remoteConfigure = AppUtil.getInstance().isDriverStation();
    private AppUtil.DialogContext dialogContext; //added for OpenRC

    final RecvLoopRunnable.RecvLoopCallback recvLoopCallback = new RecvLoopRunnable.DegenerateCallback()
        {
        @Override public CallbackResult commandEvent(Command command) throws RobotCoreException
            {
            if (remoteConfigure)
                {
                switch (command.getName())
                    {
                    case RobotCoreCommandList.CMD_REQUEST_INSPECTION_REPORT_RESP: {
                        final InspectionState rcState = InspectionState.deserialize(command.getExtra());
                        AppUtil.getInstance().runOnUiThread(new Runnable()
                            {
                            @Override public void run()
                                {
                                refresh(rcState);
                                }
                            });
                        return CallbackResult.HANDLED;
                        }
                    }
                }
            return CallbackResult.NOT_HANDLED;
            }
        };

    //----------------------------------------------------------------------------------------------
    // Life Cycle
    //----------------------------------------------------------------------------------------------

    @Override protected void onCreate(Bundle savedInstanceState)
        {
        super.onCreate(savedInstanceState);
        NetworkConnectionHandler.getInstance().pushReceiveLoopCallback(recvLoopCallback);
        }

    @Override protected void onDestroy()
        {
        super.onDestroy();
        NetworkConnectionHandler.getInstance().removeReceiveLoopCallback(recvLoopCallback);
        }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    @Override protected void refresh()
        {
        //below if added for OpenRC
        if(dialogContext == null || dialogContext.dismissed.getCount() == 0)
            {
            dialogContext = AppUtil.getInstance().showAlertDialog(UILocation.BOTH, "Competition Legality", "In its default configuration, OpenRC is illegal for competition use.\n\nMake sure to switch to the stock build variant before going to competition!");
            }

        if (remoteConfigure)
            {
            NetworkConnectionHandler.getInstance().sendCommand(new Command(RobotCoreCommandList.CMD_REQUEST_INSPECTION_REPORT));
            }
        else
            {
            super.refresh();
            }
        }

    @Override protected boolean inspectingRobotController()
        {
        return true;
        }

    @Override protected boolean useMenu()
        {
        // When we're remote configuring, the only thing on the menu is something
        // that will simply make the RC inaccessible. So we don't bother.
        return !remoteConfigure;
        }

    @Override protected boolean validateAppsInstalled(InspectionState state)
        {
        if (state.channelChangerRequired && !state.isChannelChangerInstalled())
            {
            return false;
            }

        // Driver Station cannot be installed
        if (state.isDriverStationInstalled())
            {
            return false;
            }

        // RobotController or AppInventor Required
        else
            {
            return state.isRobotControllerInstalled() || state.isAppInventorInstalled();
            }
        }
    }
