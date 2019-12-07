/* Copyright (c) 2014, 2015 Qualcomm Technologies Inc

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of Qualcomm Technologies Inc nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. */

package com.qualcomm.ftccommon.configuration;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.qualcomm.ftccommon.R;
import com.qualcomm.hardware.modernrobotics.ModernRoboticsUsbDeviceInterfaceModule;
import com.qualcomm.robotcore.hardware.ControlSystem;
import com.qualcomm.robotcore.hardware.configuration.DeviceConfiguration;
import com.qualcomm.robotcore.hardware.configuration.DeviceInterfaceModuleConfiguration;

import java.util.List;

public class EditDeviceInterfaceModuleActivity extends EditUSBDeviceActivity {

  @Override public String getTag() { return this.getClass().getSimpleName(); }
  public static final RequestCode requestCode = RequestCode.EDIT_DEVICE_INTERFACE_MODULE;

  private DeviceInterfaceModuleConfiguration  deviceInterfaceModuleConfiguration;
  private EditText                            device_interface_module_name;
  private DisplayNameAndRequestCode[]         listKeys;

  /**
   * In onCreate, we gather all of the linearLayout's that are associated with each port.
   * this is how the simple_device.xml file is reused, but we read and write to the correct
   * Spinners, EditTexts, TextViews, and Buttons. The TextView port# is set during onCreate
   * as a way to "name" that chunk of xml code. Each layout is then identified by the port number.
   */
  @Override
  protected void onCreate(Bundle savedInstanceState){
    super.onCreate(savedInstanceState);
    setContentView(R.layout.device_interface_module);

    String[] strings = getResources().getStringArray(R.array.device_interface_module_options_array);
    listKeys = DisplayNameAndRequestCode.fromArray(strings);

    ListView listView = (ListView) findViewById(R.id.listView_devices);
    listView.setAdapter(new ArrayAdapter<DisplayNameAndRequestCode>(this, android.R.layout.simple_list_item_1, listKeys));
    listView.setOnItemClickListener(editLaunchListener);

    device_interface_module_name = (EditText) findViewById(R.id.device_interface_module_name);

    EditParameters parameters = EditParameters.fromIntent(this, getIntent());
    deserialize(parameters);

    device_interface_module_name.addTextChangedListener(new SetNameTextWatcher(controllerConfiguration));
    device_interface_module_name.setText(controllerConfiguration.getName());

    showFixSwapButtons();
    deviceInterfaceModuleConfiguration = (DeviceInterfaceModuleConfiguration)controllerConfiguration;
  }

  @Override protected void refreshSerialNumber() {
    TextView serialNumberView = (TextView) findViewById(R.id.serialNumber);
    serialNumberView.setText(formatSerialNumber(this, controllerConfiguration));
  }

  @Override
  protected void onStart() {
    super.onStart();
  }

  private AdapterView.OnItemClickListener editLaunchListener = new AdapterView.OnItemClickListener() {
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
      DisplayNameAndRequestCode key = listKeys[position];

      if (key.requestCode==EditPWMDevicesActivity.requestCode) {
        handleLaunchEdit(key.requestCode, EditPWMDevicesActivity.class, deviceInterfaceModuleConfiguration.getPwmOutputs());
      } else if (key.requestCode==EditI2cDevicesActivity.requestCode) {
        EditParameters<DeviceConfiguration> parameters = new EditParameters<DeviceConfiguration>(EditDeviceInterfaceModuleActivity.this,
                DeviceConfiguration.class,
                deviceInterfaceModuleConfiguration.getI2cDevices(),
                ModernRoboticsUsbDeviceInterfaceModule.MAX_I2C_PORT_NUMBER + 1);
        parameters.setControlSystem(ControlSystem.MODERN_ROBOTICS);
        handleLaunchEdit(key.requestCode, EditI2cDevicesActivity.class, parameters);
      } else if (key.requestCode==EditAnalogInputDevicesActivity.requestCode) {
        editSimple(key, EditAnalogInputDevicesActivity.class, deviceInterfaceModuleConfiguration.getAnalogInputDevices());
      } else if (key.requestCode==EditDigitalDevicesActivity.requestCode) {
        editSimple(key, EditDigitalDevicesActivity.class, deviceInterfaceModuleConfiguration.getDigitalDevices());
      } else if (key.requestCode==EditAnalogOutputDevicesActivity.requestCode) {
        editSimple(key, EditAnalogOutputDevicesActivity.class, deviceInterfaceModuleConfiguration.getAnalogOutputDevices());
      }
    }
  };

  private EditParameters initParameters(List<DeviceConfiguration> devices) {
    EditParameters result = new EditParameters<DeviceConfiguration>(this, DeviceConfiguration.class, devices);
    result.setControlSystem(ControlSystem.MODERN_ROBOTICS);
    return result;
  }

  private void editSimple(DisplayNameAndRequestCode key, Class launchClass, List<DeviceConfiguration> devices) {
    EditParameters parameters = initParameters(devices);
    handleLaunchEdit(key.requestCode, launchClass, parameters);
  }

  @Override
  protected void onActivityResult(int requestCodeValue, int resultCode, Intent data) {
    logActivityResult(requestCodeValue, resultCode, data);

    RequestCode requestCode = RequestCode.fromValue(requestCodeValue);
    if (resultCode == RESULT_OK) {
      EditParameters<DeviceConfiguration> parameters = EditParameters.fromIntent(this, data);
      if (requestCode == EditSwapUsbDevices.requestCode) {
          completeSwapConfiguration(requestCodeValue, resultCode, data);
      } else if (requestCode == EditPWMDevicesActivity.requestCode) {
          deviceInterfaceModuleConfiguration.setPwmOutputs(parameters.getCurrentItems());
      } else if (requestCode == EditAnalogInputDevicesActivity.requestCode) {
          deviceInterfaceModuleConfiguration.setAnalogInputDevices(parameters.getCurrentItems());
      } else if (requestCode == EditDigitalDevicesActivity.requestCode) {
          deviceInterfaceModuleConfiguration.setDigitalDevices(parameters.getCurrentItems());
      } else if (requestCode == EditI2cDevicesActivity.requestCode) {
          deviceInterfaceModuleConfiguration.setI2cDevices(parameters.getCurrentItems());
      } else if (requestCode == EditAnalogOutputDevicesActivity.requestCode) {
          deviceInterfaceModuleConfiguration.setAnalogOutputDevices(parameters.getCurrentItems());
      }
      currentCfgFile.markDirty();
      robotConfigFileManager.setActiveConfigAndUpdateUI(currentCfgFile);
    }
  }

  public void onDoneButtonPressed(View v) {
    finishOk();
  }

  @Override
  protected void finishOk() {
    controllerConfiguration.setName(device_interface_module_name.getText().toString());
    finishOk(new EditParameters(this, controllerConfiguration, getRobotConfigMap()));
  }

  public void onCancelButtonPressed(View v) {
    finishCancel();
  }

  //----------------------------------------------------------------------------------------------
  // Fixing and swapping
  //----------------------------------------------------------------------------------------------

  public void onFixButtonPressed(View v) {
    fixConfiguration();
  }

  public void onSwapButtonPressed(View view) {
    swapConfiguration();
  }

}