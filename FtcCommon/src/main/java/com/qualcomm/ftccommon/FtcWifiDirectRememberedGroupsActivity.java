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

package com.qualcomm.ftccommon;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.robocol.Command;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.ThreadPool;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.network.CallbackResult;
import org.firstinspires.ftc.robotcore.internal.network.NetworkConnectionHandler;
import org.firstinspires.ftc.robotcore.internal.network.RecvLoopRunnable;
import org.firstinspires.ftc.robotcore.internal.network.WifiDirectAgent;
import org.firstinspires.ftc.robotcore.internal.network.WifiDirectGroupName;
import org.firstinspires.ftc.robotcore.internal.network.WifiDirectPersistentGroupManager;
import org.firstinspires.ftc.robotcore.internal.ui.ThemedActivity;
import org.firstinspires.ftc.robotcore.internal.ui.UILocation;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("WeakerAccess")
public class FtcWifiDirectRememberedGroupsActivity extends ThemedActivity
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "FtcWifiDirectRememberedGroupsActivity";
    @Override public String getTag() { return TAG; }
    @Override protected FrameLayout getBackBar() { return findViewById(org.firstinspires.inspection.R.id.backbar); }

    private final boolean                       remoteConfigure = AppUtil.getInstance().isDriverStation();
    private final NetworkConnectionHandler      networkConnectionHandler = NetworkConnectionHandler.getInstance();
    private final RecvLoopCallback              recvLoopCallback = new RecvLoopCallback();
    private final Object                        requestGroupsFutureLock = new Object();
    private Future                              requestGroupsFuture = null;
    private WifiDirectPersistentGroupManager    persistentGroupManager;

    //----------------------------------------------------------------------------------------------
    // Life Cycle
    //----------------------------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState)
        {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ftc_wifi_remembered_groups);

        if (!remoteConfigure)
            {
            persistentGroupManager = new WifiDirectPersistentGroupManager(WifiDirectAgent.getInstance());
            }
        else
            {
            networkConnectionHandler.pushReceiveLoopCallback(recvLoopCallback);
            }
        }

    @Override protected void onStart()
        {
        super.onStart();

        if (!remoteConfigure)
            {
            loadLocalGroups();
            }
        else
            {
            requestRememberedGroups();
            }
        }

    @Override protected void onDestroy()
        {
        super.onDestroy();
        if (remoteConfigure)
            {
            networkConnectionHandler.removeReceiveLoopCallback(recvLoopCallback);
            }
        }

    //----------------------------------------------------------------------------------------------
    // List Management
    //----------------------------------------------------------------------------------------------

    protected void loadLocalGroups()
        {
        loadGroupList(getLocalGroupList());
        }

    protected void requestRememberedGroups()
        {
        RobotLog.vv(TAG, "requestRememberedGroups()");
        networkConnectionHandler.sendCommand(new Command(CommandList.CMD_REQUEST_REMEMBERED_GROUPS));
        }

    // RC has informed DS of the list of list of remembered groups
    protected CallbackResult handleCommandRequestRememberedGroupsResp(String extra) throws RobotCoreException
        {
        RobotLog.vv(TAG, "handleCommandRequestRememberedGroupsResp()");
        List<WifiDirectGroupName> names = WifiDirectGroupName.deserializeNames(extra);
        loadGroupList(names);
        return CallbackResult.HANDLED;
        }

    protected CallbackResult handleRememberedGroupsChanged()
        {
        // We may get a flurry of these. So wait a touch before we request the now-current list
        synchronized (requestGroupsFutureLock)
            {
            if (requestGroupsFuture != null)
                {
                requestGroupsFuture.cancel(false);
                requestGroupsFuture = null;
                }
            requestGroupsFuture = ThreadPool.getDefaultScheduler().schedule(new Callable()
                {
                @Override public Object call() throws Exception
                    {
                    synchronized (requestGroupsFutureLock)
                        {
                        requestRememberedGroups();
                        requestGroupsFuture = null;
                        }
                    return null;
                    }
                }, 250, TimeUnit.MILLISECONDS);
            }

        return CallbackResult.HANDLED_CONTINUE; // others may want to know
        }

    protected List<WifiDirectGroupName> getLocalGroupList()
        {
        return WifiDirectGroupName.namesFromGroups(persistentGroupManager.getPersistentGroups());
        }

    protected void loadGroupList(final List<WifiDirectGroupName> names)
        {
        AppUtil.getInstance().runOnUiThread(new Runnable()
            {
            @Override public void run()
                {
                ListView groupList = (ListView) findViewById(R.id.groupList);
                Collections.sort(names);
                if (names.isEmpty())
                    {
                    names.add(new WifiDirectGroupName(getString(R.string.noRememberedGroupsFound)));
                    }
                ArrayAdapter<WifiDirectGroupName> adapter = new WifiP2pGroupItemAdapter(AppUtil.getInstance().getActivity(), android.R.layout.simple_spinner_dropdown_item, names);
                groupList.setAdapter(adapter);
                }
            });
        }

    protected class WifiP2pGroupItemAdapter extends ArrayAdapter<WifiDirectGroupName>
        {
        public WifiP2pGroupItemAdapter(Context context, @LayoutRes int resource, @NonNull List<WifiDirectGroupName> objects)
            {
            super(context, resource, objects);
            }
        }

    //----------------------------------------------------------------------------------------------
    // Actions
    //----------------------------------------------------------------------------------------------

    public void onClearRememberedGroupsClicked(View view)
        {
        RobotLog.vv(TAG, "onClearRememberedGroupsClicked()");
        if (!remoteConfigure)
            {
            persistentGroupManager.deleteAllPersistentGroups();
            AppUtil.getInstance().showToast(UILocation.BOTH, getString(R.string.toastWifiP2pRememberedGroupsCleared));
            loadLocalGroups();
            }
        else
            {
            networkConnectionHandler.sendCommand(new Command(CommandList.CMD_CLEAR_REMEMBERED_GROUPS));
            }
        }

    //------------------------------------------------------------------------------------------------
    // Remote handling
    //------------------------------------------------------------------------------------------------

    protected class RecvLoopCallback extends RecvLoopRunnable.DegenerateCallback
        {
        @Override public CallbackResult commandEvent(Command command)
            {
            CallbackResult result = CallbackResult.NOT_HANDLED;
            try
                {
                switch (command.getName())
                    {
                    case CommandList.CMD_REQUEST_REMEMBERED_GROUPS_RESP:
                        result = handleCommandRequestRememberedGroupsResp(command.getExtra());
                        break;
                    case CommandList.CMD_NOTIFY_WIFI_DIRECT_REMEMBERED_GROUPS_CHANGED:
                        handleRememberedGroupsChanged();
                        break;
                    }
                }
            catch (RobotCoreException e)
                {
                RobotLog.logStacktrace(e);
                }
            return result;
            }
        }
    }
