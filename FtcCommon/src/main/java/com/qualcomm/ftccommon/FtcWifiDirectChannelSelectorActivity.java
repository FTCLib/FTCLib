/*
 * Copyright (c) 2014, 2015 Qualcomm Technologies Inc
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
 * Neither the name of Qualcomm Technologies Inc nor the names of its contributors may be used to
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
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.AnyRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.internal.network.WifiDirectChannelAndDescription;
import org.firstinspires.ftc.robotcore.internal.network.WifiDirectChannelChanger;
import org.firstinspires.ftc.robotcore.internal.network.WifiUtil;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.system.PreferencesHelper;
import org.firstinspires.ftc.robotcore.internal.ui.ThemedActivity;
import org.firstinspires.ftc.robotcore.internal.ui.UILocation;

import java.util.Arrays;

@SuppressWarnings("WeakerAccess")
public class FtcWifiDirectChannelSelectorActivity extends ThemedActivity implements AdapterView.OnItemClickListener
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "FtcWifiDirectChannelSelectorActivity";
    @Override public String getTag() { return TAG; }
    @Override protected FrameLayout getBackBar() { return findViewById(org.firstinspires.inspection.R.id.backbar); }

    private boolean                  remoteConfigure = AppUtil.getInstance().isDriverStation();
    private PreferencesHelper        preferencesHelper = new PreferencesHelper(TAG);
    private WifiDirectChannelChanger configurer = null;

    //----------------------------------------------------------------------------------------------
    // Life Cycle
    //----------------------------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState)
        {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ftc_wifi_channel_selector);

        ListView channelPickList = (ListView) findViewById(R.id.channelPickList);
        loadAdapter(channelPickList);
        channelPickList.setOnItemClickListener(this);
        channelPickList.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);

        if (!remoteConfigure)
            {
            configurer = new WifiDirectChannelChanger();
            }
        }

    @Override
    protected void onStart()
        {
        super.onStart();

        // Since is the WiFi Direct Channel Selector activity, we assume this is a WiFi Direct connection.
        // Check to see if a preferred channel has already been defined.
        int prefChannel = preferencesHelper.readInt(getString(com.qualcomm.ftccommon.R.string.pref_wifip2p_channel), -1);
        if (prefChannel == -1)
            {
            prefChannel = 0;
            RobotLog.vv(TAG, "pref_wifip2p_channel: No preferred channel defined. Will use a default value of %d", prefChannel);
            }
        else
            {
            RobotLog.vv(TAG, "pref_wifip2p_channel: Found existing preferred channel (%d).", prefChannel);
            }

        ListView channelPickList = (ListView) findViewById(R.id.channelPickList);
        ArrayAdapter<WifiDirectChannelAndDescription> adapter = getAdapter(channelPickList);

        int index = -1;
        for (int i = 0; i < adapter.getCount(); i++)
            {
            // does the select channel match one of the list items?
            WifiDirectChannelAndDescription item = adapter.getItem(i);
            if (prefChannel == item.getChannel())
                {
                // set index then exit loop.
                index = i;
                channelPickList.setItemChecked(index, true);
                RobotLog.vv(TAG, "preferred channel matches ListView index %d (%d).", index, item.getChannel());
                i = adapter.getCount();
                }
            }
        }

    @Override protected void onDestroy()
        {
        super.onDestroy();
        }

    //----------------------------------------------------------------------------------------------
    // Spinner
    //----------------------------------------------------------------------------------------------

    protected ArrayAdapter<WifiDirectChannelAndDescription> getAdapter(AdapterView<?> av)
        {
        return (ArrayAdapter<WifiDirectChannelAndDescription>) av.getAdapter();
        }

    protected void loadAdapter(ListView itemsListView)
        {
        WifiDirectChannelAndDescription[] items = WifiDirectChannelAndDescription.load().toArray(new WifiDirectChannelAndDescription[0]);
        Arrays.sort(items);

        // if 5GHz is not available, then truncate list of available channels.
        if (WifiUtil.is5GHzAvailable() == false)
            {
            items = Arrays.copyOf(items, INDEX_AUTO_AND_2_4_ITEMS);
            RobotLog.vv(TAG, "5GHz radio not available.");
            }
        else
            {
            RobotLog.vv(TAG, "5GHz radio is available.");
            }

        ArrayAdapter<WifiDirectChannelAndDescription> adapter = new WifiChannelItemAdapter(this, android.R.layout.simple_spinner_dropdown_item, items);  // simple_spinner_item, simple_spinner_dropdown_item
        itemsListView.setAdapter(adapter);
        }

    protected class WifiChannelItemAdapter extends ArrayAdapter<WifiDirectChannelAndDescription>
        {
        @AnyRes int checkmark;

        public WifiChannelItemAdapter(Context context, @LayoutRes int resource, @NonNull WifiDirectChannelAndDescription[] objects)
            {
            super(context, resource, objects);

            // Find the checkmark appropriate to the current theme
            TypedValue typedValue = new TypedValue();
            FtcWifiDirectChannelSelectorActivity.this.getTheme().resolveAttribute(android.R.attr.listChoiceIndicatorSingle, typedValue, true);
            checkmark = typedValue.resourceId;
            }

        @NonNull @Override public View getView(int position, View convertView, ViewGroup parent)
            {
            // Create the view
            View view = super.getView(position, convertView, parent);

            // Set its checkmark image
            CheckedTextView checkedTextView = (CheckedTextView)view;
            checkedTextView.setCheckMarkDrawable(checkmark);

            // Return the new view
            return view;
            }
        }

    //----------------------------------------------------------------------------------------------
    // Actions
    //----------------------------------------------------------------------------------------------

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
        {
        if (configurer == null || !configurer.isBusy())
            {
            ArrayAdapter<WifiDirectChannelAndDescription> adapter = getAdapter(parent);
            WifiDirectChannelAndDescription item = adapter.getItem(position);

            // Give UI feedback
            CheckedTextView checkedTextView = (CheckedTextView)view;
            checkedTextView.setChecked(true);

            // Change to the indicated item
            if (remoteConfigure)
                {
                if (preferencesHelper.writePrefIfDifferent(getString(R.string.pref_wifip2p_channel), item.getChannel()))
                    {
                    AppUtil.getInstance().showToast(UILocation.ONLY_LOCAL, getString(R.string.toastWifiP2pChannelChangeRequestedDS, item.getDescription()));
                    }
                }
            else
                {
                configurer.changeToChannel(item.getChannel());
                }
            }
        }

    public void onWifiSettingsClicked(View view)
        {
        RobotLog.vv(TAG, "launch wifi settings");
        startActivity(new Intent(WifiManager.ACTION_PICK_WIFI_NETWORK));
        }


    //----------------------------------------------------------------------------------------------
    // Additional variables and methods
    //----------------------------------------------------------------------------------------------
    private final int INDEX_AUTO_AND_2_4_ITEMS = 12;

    }
