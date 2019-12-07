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

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.qualcomm.ftccommon.R;
import com.qualcomm.robotcore.hardware.configuration.ControllerConfiguration;
import com.qualcomm.robotcore.util.RobotLog;

/**
 * {@link EditSwapUsbDevices} provides the means to pick an item from a list of USB controllers
 * for the purposes of swapping serial-numbers.
 */
public class EditSwapUsbDevices extends EditActivity
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "EditSwapUsbDevices";
    @Override public String getTag() { return TAG; }
    public static final RequestCode requestCode = RequestCode.EDIT_SWAP_USB_DEVICES;


    /** the controller for whom we seek a swap */
    protected ControllerConfiguration targetConfiguration;

    //------------------------------------------------------------------------------------------------
    // Life cycle
    //------------------------------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState)
        {
        super.onCreate(savedInstanceState);
        RobotLog.vv(TAG, "onCreate()");
        setContentView(R.layout.activity_swap_usb_devices);

        EditParameters parameters = EditParameters.fromIntent(this, getIntent());
        deserialize(parameters);
        this.targetConfiguration = (ControllerConfiguration) parameters.getConfiguration();

        String message = String.format(getString(R.string.swapPrompt), targetConfiguration.getName());
        TextView caption = (TextView) findViewById(R.id.swapCaption);
        caption.setText(message);

        // there's nothing to edit here, so the 'Done' / 'Save' button isn't appropriate
        Button doneButton = (Button) findViewById(R.id.doneButton);
        doneButton.setVisibility(View.GONE);

        populateList();
        }

    //------------------------------------------------------------------------------------------------
    // Life cycle support
    //------------------------------------------------------------------------------------------------

    protected void populateList()
        {
        ListView controllerListView = (ListView) findViewById(R.id.controllersList);

        DeviceInfoAdapter adapter = new DeviceInfoAdapter(this, android.R.layout.simple_list_item_2, getRobotConfigMap().getEligibleSwapTargets(targetConfiguration, scannedDevices, this));
        controllerListView.setAdapter(adapter);

        controllerListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
            {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View v, int pos, long arg3)
                {
                ControllerConfiguration controllerConfiguration = (ControllerConfiguration) adapterView.getItemAtPosition(pos);
                finishOk(new EditParameters(EditSwapUsbDevices.this, controllerConfiguration));
                }
            });
        }

    @Override
    public void onBackPressed()
        {
        RobotLog.vv(TAG, "onBackPressed()");
        doBackOrCancel();
        }

    public void onCancelButtonPressed(View view)
        {
        RobotLog.vv(TAG, "onCancelButtonPressed()");
        doBackOrCancel();
        }

    protected void doBackOrCancel()
        {
        finishCancel();
        }
    }
