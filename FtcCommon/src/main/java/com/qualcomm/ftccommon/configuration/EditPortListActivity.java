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
package com.qualcomm.ftccommon.configuration;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.qualcomm.ftccommon.R;
import com.qualcomm.robotcore.hardware.configuration.BuiltInConfigurationType;
import com.qualcomm.robotcore.hardware.configuration.DeviceConfiguration;
import com.qualcomm.robotcore.util.RobotLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * EditPortListActivity is a helper class that assists in managing configuration of
 * lists of items associated with ports on a controller.
 */
public abstract class EditPortListActivity<ITEM_T extends DeviceConfiguration> extends EditUSBDeviceActivity // not all EditPortListActivity subclasses are USB devices, but some can be
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "EditPortListActivity";

    protected int layoutMain;
    protected int layoutControllerNameBanner = 0;   // optional layout id of a banner to allow editing of the parent configuration name
    protected int idListParentLayout;               // id within layout main of the (LinearLayout) parent of the list items
    protected int layoutItem;                       // layout id of the item to inflate for a new list item
    protected int idItemRowPort;                    // id of the root view within layoutItem
    protected int idItemPortNumber;                 // id of a TextView for the port number withing idItemRowPort
    protected int idItemEditTextResult;             // id within layoutItem that is an EditText for editing the name of same

    protected ArrayList<View>           itemViews = new ArrayList<View>();
    protected List<ITEM_T>              itemList = new ArrayList<ITEM_T>();
    protected Class<ITEM_T>             itemClass;
    protected int                       initialPortNumber;                          // only used if list is empty and we're growable

    // The following are relevant only if layoutControllerNameBanner is non-zero
    protected int                       idBannerParent = R.id.bannerParent;             // id of item under which to add banner
    protected int                       idControllerName = R.id.controller_name;        // id of the EditText for editing the controller name
    protected int                       idControllerSerialNumber = R.id.serialNumber;   // id of TextView for displaying serial number of controller configuration
    protected EditText                  editTextBannerControllerName;               // the view retrieved via idControllerName
    protected TextView                  textViewSerialNumber;                       // the view retrieved via idBannerSerialNumber

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    protected EditPortListActivity()
        {
        }

    @Override
    protected void onCreate(Bundle savedInstanceState)
        {
        super.onCreate(savedInstanceState);
        setContentView(layoutMain);

        Intent intent = getIntent();
        EditParameters<ITEM_T> parameters = EditParameters.fromIntent(this, intent);

        // Extract miscellaneous parameters in case they're needed later
        deserialize(parameters);
        this.initialPortNumber = parameters.getInitialPortNumber();

        // Hide or show the Add button
        showButton(idAddButton, parameters.isGrowable());

        // If there is a controller banner indicated, then insert that
        if (layoutControllerNameBanner != 0)
            {
            LinearLayout parent = (LinearLayout) findViewById(idBannerParent);
            View banner = getLayoutInflater().inflate(layoutControllerNameBanner, parent, false);
            parent.addView(banner);

            editTextBannerControllerName = (EditText) banner.findViewById(idControllerName);
            textViewSerialNumber = (TextView) banner.findViewById(idControllerSerialNumber);
            editTextBannerControllerName.setText(controllerConfiguration.getName());
            showFixSwapButtons();
            }

        createListViews(parameters);
        addViewListeners();
        }

    @Override
    protected void refreshSerialNumber()
        {
        String serialNum = formatSerialNumber(this, controllerConfiguration);
        textViewSerialNumber.setText(serialNum);
        }

    @Override
    protected void onStart()
        {
        super.onStart();
        }

    protected void createListViews(EditParameters<ITEM_T> parameters)
        {
        if (parameters != null)
            {
            this.itemList = parameters.getCurrentItems();
            this.itemClass = parameters.getItemClass();
            Collections.sort(this.itemList);

            for (int index = 0; index < this.itemList.size(); index++)
                {
                View itemView = createItemViewForPort(findConfigByIndex(index).getPort());
                itemViews.add(itemView);
                }
            }
        }

    protected void addViewListeners()
        {
        for (int index = 0; index < this.itemList.size(); index++)
            {
            addViewListenersOnIndex(index);
            }
        }

    protected abstract void addViewListenersOnIndex(int index);

    protected View createItemViewForPort(int portNumber)
        {
        LinearLayout parent = (LinearLayout) findViewById(idListParentLayout);
        View child = getLayoutInflater().inflate(layoutItem, parent, false);
        parent.addView(child);

        View result = child.findViewById(idItemRowPort);

        TextView port = (TextView) result.findViewById(idItemPortNumber);
        if (port != null)
            {
            port.setText(String.format(Locale.getDefault(), "%d", portNumber));
            }

        return result;
        }

    protected void addNameTextChangeWatcherOnIndex(final int index)
        {
        View itemView = findViewByIndex(index);
        EditText name = (EditText) itemView.findViewById(idItemEditTextResult);

        name.addTextChangedListener(new SetNameTextWatcher(findConfigByIndex(index)));
        }

    //----------------------------------------------------------------------------------------------
    // Dynamically sized list management
    //----------------------------------------------------------------------------------------------

    public void onAddButtonPressed(View v)
        {
        addNewItem();
        }

    protected void addNewItem()
        {
        try {
            // Figure out the port number of the new item. That's either one more than the
            // current last port number, or the initial port number we've been told to start
            // with, if there are currently no items. The index of the new item may be different
            // than its port number; be careful not to confuse the two.
            int portNumber = itemList.isEmpty() ? this.initialPortNumber : itemList.get(itemList.size()-1).getPort() + 1;
            int index = itemList.size();

            // Make a new configuration with that port number
            ITEM_T deviceConfiguration = this.itemClass.newInstance();
            deviceConfiguration.setPort(portNumber);
            deviceConfiguration.setConfigurationType(BuiltInConfigurationType.NOTHING);

            // Add it, and plumb it up
            itemList.add(deviceConfiguration);
            itemViews.add(createItemViewForPort(portNumber));
            addViewListenersOnIndex(index);
            }
        catch (IllegalAccessException|InstantiationException e)
            {
            RobotLog.ee(TAG, e, "exception thrown during addNewItem(); ignoring add");
            }
        }

    //----------------------------------------------------------------------------------------------
    // Fixing and swapping
    //----------------------------------------------------------------------------------------------

    public void onFixButtonPressed(View v)
        {
        fixConfiguration();
        }

    public void onSwapButtonPressed(View view)
        {
        swapConfiguration();
        }

    @Override
    protected void onActivityResult(int requestCodeValue, int resultCode, Intent data)
        {
        if (resultCode == RESULT_OK)
            {
            completeSwapConfiguration(requestCodeValue, resultCode, data);
            currentCfgFile.markDirty();
            robotConfigFileManager.updateActiveConfigHeader(currentCfgFile);
            }
        }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    protected View findViewByIndex(int index)
        {
        return itemViews.get(index);
        }

    protected DeviceConfiguration findConfigByIndex(int index)
        {
        return this.itemList.get(index);
        }

    protected DeviceConfiguration findConfigByPort(int port)
        {
        for (DeviceConfiguration config : this.itemList)
            {
            if (config.getPort() == port) return config;
            }
        return null;
        }

    //----------------------------------------------------------------------------------------------
    // Termination
    //----------------------------------------------------------------------------------------------

    public void onDoneButtonPressed(View v)
        {
        finishOk();
        }

    public void onCancelButtonPressed(View v)
        {
        finishCancel();
        }

    @Override
    protected void finishOk()
        {
        // If there's a controller banner, then the completion sets the whole configuration
        // instead of just a list. Note that subclasses should override to set the list within
        // this configuration, then call super.
        if (controllerConfiguration != null)
            {
            controllerConfiguration.setName(editTextBannerControllerName.getText().toString());
            finishOk(new EditParameters<ITEM_T>(this, controllerConfiguration, getRobotConfigMap()));
            }
        else
            {
            finishOk(new EditParameters<ITEM_T>(this, itemClass, itemList));
            }
        }
    }
