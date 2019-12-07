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
import android.widget.CheckBox;
import android.widget.EditText;

import com.qualcomm.robotcore.hardware.configuration.DeviceConfiguration;

/**
 * EditPortListCheckboxActivity provides a template-driven editing of a list of checkboxed list items
 */
public abstract class EditPortListCheckboxActivity<ITEM_T extends DeviceConfiguration> extends EditPortListActivity<ITEM_T>
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected int idItemCheckbox;   // id within item which is the checkbox we are to handle

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    protected EditPortListCheckboxActivity()
        {
        }

    @Override
    protected void addViewListenersOnIndex(int index)
    // Plumb up all the event handlers for the indicated item
        {
        addCheckBoxListenerOnIndex(index);
        addNameTextChangeWatcherOnIndex(index);
        handleDisabledDeviceByIndex(index);
        }

    protected void handleDisabledDeviceByIndex(int index)
        {
        View itemView = findViewByIndex(index);
        CheckBox checkbox = (CheckBox) itemView.findViewById(idItemCheckbox);
        DeviceConfiguration device = this.itemList.get(index);
        if (device.isEnabled())
            {
            checkbox.setChecked(true);
            EditText name = (EditText) itemView.findViewById(idItemEditTextResult);
            name.setText(device.getName());
            }
        else
            {
            checkbox.setChecked(true); // kind of a hack. Sets the checkbox to true, so
            // when performing the click programmatically,
            // the checkbox becomes "unclicked" which does the right thing.
            checkbox.performClick();
            }
        }

    protected void addCheckBoxListenerOnIndex(final int index)
        {
        View itemView = findViewByIndex(index);
        final EditText name;
        name = (EditText) itemView.findViewById(this.idItemEditTextResult);

        final DeviceConfiguration device;
        device = this.itemList.get(index);

        CheckBox checkbox = (CheckBox) itemView.findViewById(this.idItemCheckbox);
        checkbox.setOnClickListener(new View.OnClickListener()
        {
        @Override
        public void onClick(View view)
            {
            if (((CheckBox) view).isChecked())
                {
                name.setEnabled(true);
                name.setText("");
                device.setEnabled(true);
                device.setName("");
                }
            else
                {
                name.setEnabled(false);
                name.setText(disabledDeviceName());
                device.setEnabled(false);
                device.setName(disabledDeviceName());
                }
            }
        });
        }
    }
