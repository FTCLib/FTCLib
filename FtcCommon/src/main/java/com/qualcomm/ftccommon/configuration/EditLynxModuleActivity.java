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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import com.qualcomm.ftccommon.CommandList;
import com.qualcomm.ftccommon.R;
import com.qualcomm.robotcore.hardware.ControlSystem;
import com.qualcomm.robotcore.hardware.configuration.DeviceConfiguration;
import com.qualcomm.robotcore.hardware.configuration.LynxConstants;
import com.qualcomm.robotcore.hardware.configuration.LynxI2cDeviceConfiguration;
import com.qualcomm.robotcore.hardware.configuration.LynxModuleConfiguration;
import com.qualcomm.robotcore.robocol.Command;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.internal.system.Assert;

import java.util.List;

/**
 * {@link EditLynxModuleActivity} handles the configuration of the devices in a Lynx module.
 */
public class EditLynxModuleActivity extends EditActivity
    {
	@Override public String getTag() { return this.getClass().getSimpleName(); }
    public static final RequestCode requestCode = RequestCode.EDIT_LYNX_MODULE;

    private LynxModuleConfiguration         lynxModuleConfiguration;
    private EditText                        lynx_module_name;
    private DisplayNameAndRequestCode[]     listKeys;

    @Override
    protected void onCreate(Bundle savedInstanceState)
        {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lynx_module);

        String[] strings = getResources().getStringArray(R.array.lynx_module_options_array);
        listKeys = DisplayNameAndRequestCode.fromArray(strings);

        ListView listView = (ListView) findViewById(R.id.lynx_module_devices);
        listView.setAdapter(new ArrayAdapter<DisplayNameAndRequestCode>(this, android.R.layout.simple_list_item_1, listKeys));
        listView.setOnItemClickListener(editLaunchListener);

        lynx_module_name = (EditText) findViewById(R.id.lynx_module_name);

        EditParameters parameters = EditParameters.fromIntent(this, getIntent());
        deserialize(parameters);

        lynxModuleConfiguration = (LynxModuleConfiguration) controllerConfiguration;
        lynx_module_name.addTextChangedListener(new SetNameTextWatcher(lynxModuleConfiguration));
        lynx_module_name.setText(lynxModuleConfiguration.getName());

        RobotLog.vv(TAG, "lynxModuleConfiguration.getSerialNumber()=%s", lynxModuleConfiguration.getSerialNumber());
        visuallyIdentify();
        }

    @Override protected void onDestroy()
        {
        super.onDestroy();
        visuallyUnidentify();
        }

    protected void visuallyIdentify()
        {
        sendIdentify(true);
        }
    protected void visuallyUnidentify()
        {
        sendIdentify(false);
        }
    protected void sendIdentify(boolean shouldIdentify)
        {
        CommandList.CmdVisuallyIdentify cmdVisuallyIdentify = new CommandList.CmdVisuallyIdentify(lynxModuleConfiguration.getModuleSerialNumber(), shouldIdentify);
        sendOrInject(new Command(cmdVisuallyIdentify.Command, cmdVisuallyIdentify.serialize()));
        }

    private AdapterView.OnItemClickListener editLaunchListener = new AdapterView.OnItemClickListener()
        {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
            DisplayNameAndRequestCode key = listKeys[position];
            switch (key.requestCode)
                {
                case EDIT_MOTOR_LIST:
                    editMotors(key);
                    break;
                case EDIT_SERVO_LIST:
                    editServos(key, LynxConstants.INITIAL_SERVO_PORT, EditServoListActivity.class, lynxModuleConfiguration.getServos());
                    break;
                case EDIT_I2C_BUS0:
                    editI2cBus(key, 0);
                    break;
                case EDIT_I2C_BUS1:
                    editI2cBus(key, 1);
                    break;
                case EDIT_I2C_BUS2:
                    editI2cBus(key, 2);
                    break;
                case EDIT_I2C_BUS3:
                    editI2cBus(key, 3);
                    break;
                case EDIT_DIGITAL:
                    editSimple(key, 0, EditDigitalDevicesActivityLynx.class, lynxModuleConfiguration.getDigitalDevices());
                    break;
                case EDIT_ANALOG_INPUT:
                    editSimple(key, 0, EditAnalogInputDevicesActivity.class, lynxModuleConfiguration.getAnalogInputs());
                    break;
                }
            }
        };

    <ITEM_T extends DeviceConfiguration> EditParameters initParameters(int initialPortNumber, Class<ITEM_T> clazz, List<ITEM_T> currentItems)
        {
        EditParameters result = new EditParameters<ITEM_T>(this, clazz, currentItems);
        result.setInitialPortNumber(initialPortNumber);
        result.setControlSystem(ControlSystem.REV_HUB);
        return result;
        }

    private void editSimple(DisplayNameAndRequestCode key, int initialPort, Class launchClass, List<DeviceConfiguration> devices)
        {
        EditParameters parameters = initParameters(initialPort, DeviceConfiguration.class, devices);
        handleLaunchEdit(key.requestCode, launchClass, parameters);
        }

    private void editServos(DisplayNameAndRequestCode key, int initialPort, Class launchClass, List<DeviceConfiguration> devices)
        {
        EditParameters parameters = initParameters(initialPort, DeviceConfiguration.class, devices);
        handleLaunchEdit(key.requestCode, launchClass, parameters);
        }

    private void editMotors(DisplayNameAndRequestCode key)
        {
        Assert.assertTrue(lynxModuleConfiguration.getMotors().size() == LynxConstants.NUMBER_OF_MOTORS);
        Assert.assertTrue(lynxModuleConfiguration.getMotors().get(0).getPort() == LynxConstants.INITIAL_MOTOR_PORT);
        //
        EditParameters parameters = initParameters(LynxConstants.INITIAL_MOTOR_PORT, DeviceConfiguration.class, lynxModuleConfiguration.getMotors());
        handleLaunchEdit(key.requestCode, EditMotorListActivity.class, parameters);
        }

    /** @see com.qualcomm.hardware.HardwareFactory#buildLynxI2cDevices */
    private void editI2cBus(DisplayNameAndRequestCode key, int busZ)
        {
        EditParameters parameters = initParameters(0, LynxI2cDeviceConfiguration.class, lynxModuleConfiguration.getI2cDevices(busZ));
        parameters.setI2cBus(busZ);
        parameters.setGrowable(true);
        handleLaunchEdit(key.requestCode, EditI2cDevicesActivityLynx.class, parameters);
        }

    @Override
    protected void onActivityResult(int requestCodeValue, int resultCode, Intent data)
        {
        logActivityResult(requestCodeValue, resultCode, data);
        RequestCode requestCode = RequestCode.fromValue(requestCodeValue);
        if (resultCode == RESULT_OK)
            {
            if (requestCode == RequestCode.EDIT_MOTOR_LIST)
                {
                EditParameters<DeviceConfiguration> parameters = EditParameters.fromIntent(this, data);
                lynxModuleConfiguration.setMotors(parameters.getCurrentItems());
                Assert.assertTrue(lynxModuleConfiguration.getMotors().size() == LynxConstants.NUMBER_OF_MOTORS);
                Assert.assertTrue(lynxModuleConfiguration.getMotors().get(0).getPort()== LynxConstants.INITIAL_MOTOR_PORT);
                }
            else if (requestCode == RequestCode.EDIT_SERVO_LIST)
                {
                EditParameters<DeviceConfiguration> parameters = EditParameters.fromIntent(this, data);
                lynxModuleConfiguration.setServos(parameters.getCurrentItems());
                }
            else if (requestCode == RequestCode.EDIT_ANALOG_INPUT)
                {
                EditParameters<DeviceConfiguration> parameters = EditParameters.fromIntent(this, data);
                lynxModuleConfiguration.setAnalogInputs(parameters.getCurrentItems());
                }
            else if (requestCode == RequestCode.EDIT_DIGITAL)
                {
                EditParameters<DeviceConfiguration> parameters = EditParameters.fromIntent(this, data);
                lynxModuleConfiguration.setDigitalDevices(parameters.getCurrentItems());
                }
            else
                {
                EditParameters<LynxI2cDeviceConfiguration> i2cParams = EditParameters.fromIntent(this, data);
                if (requestCode == RequestCode.EDIT_I2C_BUS0)
                    {
                    lynxModuleConfiguration.setI2cDevices(0, i2cParams.getCurrentItems());
                    }
                else if (requestCode == RequestCode.EDIT_I2C_BUS1)
                    {
                    lynxModuleConfiguration.setI2cDevices(1, i2cParams.getCurrentItems());
                    }
                else if (requestCode == RequestCode.EDIT_I2C_BUS2)
                    {
                    lynxModuleConfiguration.setI2cDevices(2, i2cParams.getCurrentItems());
                    }
                else if (requestCode == RequestCode.EDIT_I2C_BUS3)
                    {
                    lynxModuleConfiguration.setI2cDevices(3, i2cParams.getCurrentItems());
                    }
                }
            /*
            * Save as dirty.
            */
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
        controllerConfiguration.setName(lynx_module_name.getText().toString());
        finishOk(new EditParameters(this, controllerConfiguration));
        }

    public void onCancelButtonPressed(View v)
        {
        finishCancel();
        }
    }
