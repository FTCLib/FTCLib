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

import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.qualcomm.robotcore.hardware.ControlSystem;
import com.qualcomm.robotcore.hardware.configuration.ConfigurationType;
import com.qualcomm.robotcore.hardware.configuration.ConfigurationTypeManager;
import com.qualcomm.robotcore.hardware.configuration.DeviceConfiguration;

import java.util.List;

/**
 * EditPortListSpinnerActivity provides a template-driven editing of a list of spinner list items
 */
public abstract class EditPortListSpinnerActivity<ITEM_T extends DeviceConfiguration> extends EditPortListActivity<ITEM_T>
    {
    protected abstract ConfigurationType.DeviceFlavor getDeviceFlavorBeingConfigured();

    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected int idItemSpinner;
	protected ControlSystem controlSystem;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    protected EditPortListSpinnerActivity()
        {
        }

    @Override
    protected void deserialize(EditParameters parameters)
        {
        super.deserialize(parameters);
        this.controlSystem = parameters.getControlSystem();
        }

    @Override
    protected View createItemViewForPort(int portNumber)
        {
        View itemView = super.createItemViewForPort(portNumber);
        localizeSpinner(itemView);
        return itemView;
        }

    /**
     * Override if you need the 3-parameter variant of getApplicableConfigTypes()
     */
    protected void localizeSpinner(View itemView)
        {
        Spinner spinner = (Spinner) itemView.findViewById(idItemSpinner);
        List<ConfigurationType> deviceTypes =
                ConfigurationTypeManager.getInstance().getApplicableConfigTypes(getDeviceFlavorBeingConfigured(), controlSystem);

        localizeConfigTypeSpinnerTypes(ConfigurationType.DisplayNameFlavor.Normal, spinner, deviceTypes);
        }

    @Override
    protected void addViewListenersOnIndex(int index)
    // Plumb up all the event handlers for the indicated item
        {
        View itemView = findViewByIndex(index);
        DeviceConfiguration config = findConfigByIndex(index);

        addNameTextChangeWatcherOnIndex(index);
        handleDisabledDevice(itemView, config);
        handleSpinner(itemView, this.idItemSpinner, config);
        }

    private void handleDisabledDevice(View itemView, DeviceConfiguration deviceConfiguration)
        {
        EditText name = (EditText) itemView.findViewById(this.idItemEditTextResult);
        if (deviceConfiguration.isEnabled())
            {
            name.setText(deviceConfiguration.getName());
            name.setEnabled(true);
            }
        else
            {
            name.setText(disabledDeviceName());
            name.setEnabled(false);
            }
        }

    @Override
    protected void clearDevice(View itemView)
        {
        TextView textViewPortNumber = (TextView) itemView.findViewById(this.idItemPortNumber);
        int portNumber = Integer.parseInt(textViewPortNumber.getText().toString());
        EditText nameText = (EditText) itemView.findViewById(this.idItemEditTextResult);

        nameText.setEnabled(false);
        nameText.setText(disabledDeviceName());

        DeviceConfiguration config = findConfigByPort(portNumber);
        config.setEnabled(false);
        }

    @Override
    protected void changeDevice(View itemView, ConfigurationType type)
        {
        TextView textViewPortNumber = (TextView) itemView.findViewById(this.idItemPortNumber);
        int portNumber = Integer.parseInt(textViewPortNumber.getText().toString());
        EditText nameText = (EditText) itemView.findViewById(this.idItemEditTextResult);

        nameText.setEnabled(true);

        DeviceConfiguration config = findConfigByPort(portNumber);
        clearNameIfNecessary(nameText, config);
        config.setConfigurationType(type);
        config.setEnabled(true);
        }
    }
