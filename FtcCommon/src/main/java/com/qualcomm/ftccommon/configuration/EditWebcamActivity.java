/*
Copyright (c) 2018 Robert Atkinson

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
import android.widget.TextView;

import com.qualcomm.ftccommon.R;
import com.qualcomm.robotcore.hardware.configuration.WebcamConfiguration;

public class EditWebcamActivity extends EditUSBDeviceActivity
    {
	@Override public String getTag() { return this.getClass().getSimpleName(); }
    public static final RequestCode requestCode = RequestCode.EDIT_USB_CAMERA;

    private WebcamConfiguration webcamConfiguration;
    private EditText textCameraName;

    @Override
    protected void onCreate(Bundle savedInstanceState)
        {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.webcam_device);

        textCameraName = (EditText) findViewById(R.id.cameraName);

        EditParameters parameters = EditParameters.fromIntent(this, getIntent());
        deserialize(parameters);
        webcamConfiguration = (WebcamConfiguration) this.controllerConfiguration;

        textCameraName.addTextChangedListener(new SetNameTextWatcher(controllerConfiguration));
        textCameraName.setText(controllerConfiguration.getName());

        showFixSwapButtons();
        }

    @Override protected void refreshSerialNumber()
        {
        TextView serialNumberView = (TextView) findViewById(R.id.serialNumber);
        serialNumberView.setText(formatSerialNumber(this, controllerConfiguration));
        }

    @Override
    protected void onStart()
        {
        super.onStart();
        }

    @Override
    protected void onActivityResult(int requestCodeValue, int resultCode, Intent data)
        {
        logActivityResult(requestCodeValue, resultCode, data);
        if (resultCode == RESULT_OK)
            {
            EditParameters parameters = EditParameters.fromIntent(this, data);
            RequestCode requestCode = RequestCode.fromValue(requestCodeValue);

            if (requestCode == EditSwapUsbDevices.requestCode)
                {
                completeSwapConfiguration(requestCodeValue, resultCode, data);
                }
            else if (requestCode == EditWebcamActivity.requestCode)
                {
                WebcamConfiguration newModule = (WebcamConfiguration) parameters.getConfiguration();
                if (newModule != null)
                    {
                    }
                }

            currentCfgFile.markDirty();
            robotConfigFileManager.setActiveConfig(currentCfgFile);
            }
        }

    public void onDoneButtonPressed(View v)
        {
        finishOk();
        }

    @Override protected void finishOk()
        {
        controllerConfiguration.setName(textCameraName.getText().toString());
        finishOk(new EditParameters(this, controllerConfiguration, getRobotConfigMap()));
        }

    public void onCancelButtonPressed(View v)
        {
        finishCancel();
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
    }
