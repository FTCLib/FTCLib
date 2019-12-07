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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.qualcomm.ftccommon.R;
import com.qualcomm.robotcore.hardware.ControlSystem;
import com.qualcomm.robotcore.hardware.configuration.BuiltInConfigurationType;
import com.qualcomm.robotcore.hardware.configuration.ConfigurationType;
import com.qualcomm.robotcore.hardware.configuration.ConfigurationUtility;
import com.qualcomm.robotcore.hardware.configuration.ControllerConfiguration;
import com.qualcomm.robotcore.hardware.configuration.DeviceConfiguration;
import com.qualcomm.robotcore.hardware.configuration.MatrixConstants;
import com.qualcomm.robotcore.hardware.configuration.MatrixControllerConfiguration;
import com.qualcomm.robotcore.hardware.configuration.ModernRoboticsConstants;
import com.qualcomm.robotcore.hardware.configuration.MotorControllerConfiguration;
import com.qualcomm.robotcore.hardware.configuration.ServoControllerConfiguration;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.SerialNumber;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EditLegacyModuleControllerActivity extends EditUSBDeviceActivity {

  @Override public String getTag() { return this.getClass().getSimpleName(); }	
  public static final RequestCode requestCode = RequestCode.EDIT_LEGACY_MODULE;

  private static boolean DEBUG = false;

  private EditText controller_name;
  private ArrayList<DeviceConfiguration> devices = new ArrayList<DeviceConfiguration>();

  private View info_port0;
  private View info_port1;
  private View info_port2;
  private View info_port3;
  private View info_port4;
  private View info_port5;

  /**
   * In onCreate, we gather all of the linearLayout's that are associated with each port.
   * this is how the simple_device.xml file is reused, but we read and write to the correct
   * Spinners, EditTexts, TextViews, and Buttons. The TextView port# is set during onCreate
   * as a way to "name" that chunk of xml code. Each layout is then identified by the port number.
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.legacy);

    info_port0 = createPortView(R.id.linearLayout0, 0);
    info_port1 = createPortView(R.id.linearLayout1, 1);
    info_port2 = createPortView(R.id.linearLayout2, 2);
    info_port3 = createPortView(R.id.linearLayout3, 3);
    info_port4 = createPortView(R.id.linearLayout4, 4);
    info_port5 = createPortView(R.id.linearLayout5, 5);

    controller_name = (EditText) findViewById(R.id.device_interface_module_name);

    EditParameters parameters = EditParameters.fromIntent(this, getIntent());
    deserialize(parameters);

    devices = (ArrayList<DeviceConfiguration>) controllerConfiguration.getDevices();

    controller_name.setText(controllerConfiguration.getName());
    controller_name.addTextChangedListener(new UsefulTextWatcher());

    showFixSwapButtons();

    for (int i = 0; i < devices.size(); i++) {
      DeviceConfiguration device = devices.get(i);
      if (DEBUG) RobotLog.e("[onStart] device name: " + device.getName() + ", port: " + device.getPort() + ", type: " + device.getConfigurationType());
      populatePort(findViewByPort(i), device);
    }
  }

  @Override protected void refreshSerialNumber() {
    TextView serialNumberView = (TextView) findViewById(R.id.serialNumber);
    serialNumberView.setText(formatSerialNumber(this, controllerConfiguration));
  }

  @Override
  protected void onStart() {
    super.onStart();
  }

  private View createPortView(int id, int portNumber) {
    LinearLayout layout = (LinearLayout) findViewById(id);
    View result = getLayoutInflater().inflate(R.layout.simple_device, layout, true);
    TextView port = (TextView) result.findViewById(R.id.portNumber);
    port.setText(String.format(Locale.getDefault(), "%d", portNumber));

    Spinner spinner = (Spinner)result.findViewById(R.id.choiceSpinner);
    localizeConfigTypeSpinner(ConfigurationType.DisplayNameFlavor.Legacy, spinner);

    return result;
  }


  @Override
  protected void onActivityResult(int requestCodeValue, int resultCode, Intent data) {
    logActivityResult(requestCodeValue, resultCode, data);

    if (resultCode == RESULT_OK) {
      EditParameters parameters = EditParameters.fromIntent(this, data);
      RequestCode requestCode = RequestCode.fromValue(requestCodeValue);

      if (requestCode == EditSwapUsbDevices.requestCode) {
        completeSwapConfiguration(requestCodeValue, resultCode, data);
      } else {
        ControllerConfiguration newC = (ControllerConfiguration) parameters.getConfiguration();
        setModule(newC);
        populatePort(findViewByPort(newC.getPort()), devices.get(newC.getPort()));

      }
      currentCfgFile.markDirty();
      robotConfigFileManager.setActiveConfigAndUpdateUI(currentCfgFile);
    }
  }

  /**
   * Launches the activity for the controller
   * @param controller - the module we're about to edit
   */
  private void editController_general(DeviceConfiguration controller) {
    //names already gone
    LinearLayout layout = (LinearLayout) findViewByPort(controller.getPort());
    EditText nameText = (EditText) layout.findViewById(R.id.editTextResult);
    controller.setName(nameText.getText().toString());

    if (controller.getConfigurationType() == BuiltInConfigurationType.MOTOR_CONTROLLER) {
      EditParameters<DeviceConfiguration> parameters = new EditParameters<>(this, controller, DeviceConfiguration.class, ((MotorControllerConfiguration)controller).getMotors());
      parameters.setInitialPortNumber(ModernRoboticsConstants.INITIAL_MOTOR_PORT);
      handleLaunchEdit(EditLegacyMotorControllerActivity.requestCode, EditLegacyMotorControllerActivity.class, parameters);
    }
    else if (controller.getConfigurationType() == BuiltInConfigurationType.SERVO_CONTROLLER) {
      EditParameters<DeviceConfiguration> parameters = new EditParameters<DeviceConfiguration>(this, controller, DeviceConfiguration.class, ((ServoControllerConfiguration)controller).getServos());
      parameters.setInitialPortNumber(ModernRoboticsConstants.INITIAL_SERVO_PORT);
      parameters.setControlSystem(ControlSystem.MODERN_ROBOTICS);
      handleLaunchEdit(EditLegacyServoControllerActivity.requestCode, EditLegacyServoControllerActivity.class, parameters);
    }
    else if (controller.getConfigurationType() == BuiltInConfigurationType.MATRIX_CONTROLLER) {
      handleLaunchEdit(EditMatrixControllerActivity.requestCode, EditMatrixControllerActivity.class, controller);
    }
  }

  public void onDoneButtonPressed(View v) {
    finishOk();
  }

  @Override
  protected void finishOk() {
    controllerConfiguration.setName(controller_name.getText().toString());
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

  //----------------------------------------------------------------------------------------------
  // Heavy lifting
  //----------------------------------------------------------------------------------------------

  /**
   * This method loads information from the module (Gyro, Accelerometer, Motor Controller, etc) into
   * the xml file that's associated with that port.
   *
   * @param v - the LinearLayout that loaded the simple_device.xml code
   * @param device - the global device whose information should populate that port
   */
  private void populatePort(View v, DeviceConfiguration device) {
    handleSpinner(v, R.id.choiceSpinner, device, true);

    String name = device.getName();
    EditText nameText = (EditText) v.findViewById(R.id.editTextResult);

    TextView portNumber = (TextView) v.findViewById(R.id.portNumber);
    int port = Integer.parseInt(portNumber.getText().toString());
    nameText.addTextChangedListener(new UsefulTextWatcher(findViewByPort(port)));
    nameText.setText(name);

    if (DEBUG) RobotLog.e("[populatePort] name: " + name + ", port: " + port + ", type: " + device.getConfigurationType());
  }

  /**
   * Sets device to a non-attached device (name "NO DEVICE ATTACHED" and name field grayed out).
   * Removes the button if necessary.
   * @param itemView - the view that holds all the necessary UI elements
   */
  @Override protected void clearDevice(View itemView) {
    TextView portNumber = (TextView) itemView.findViewById(R.id.portNumber);
    int port = Integer.parseInt(portNumber.getText().toString());
    EditText nameText = (EditText) itemView.findViewById(R.id.editTextResult);

    nameText.setEnabled(false);
    nameText.setText(disabledDeviceName());

    DeviceConfiguration newModule = new DeviceConfiguration(BuiltInConfigurationType.NOTHING);
    newModule.setPort(port);
    setModule(newModule);

    setButtonVisibility(port, View.GONE);
  }

  /**
   * Updates the module behind the scenes to the type that was selected on the spinner.
   * @param itemView - - the view that holds all the necessary UI elements
   * @param type   - the new type that was just selected
   */
  @Override protected void changeDevice(View itemView, ConfigurationType type) {
    TextView portNumber = (TextView) itemView.findViewById(R.id.portNumber);
    int port = Integer.parseInt(portNumber.getText().toString());
    EditText nameText = (EditText) itemView.findViewById(R.id.editTextResult);
    DeviceConfiguration currentModule = devices.get(port);

    nameText.setEnabled(true);
    clearNameIfNecessary(nameText, currentModule);

    if (type == BuiltInConfigurationType.MOTOR_CONTROLLER ||
        type == BuiltInConfigurationType.SERVO_CONTROLLER ||
        type == BuiltInConfigurationType.MATRIX_CONTROLLER) {
      createController(port, type);
      setButtonVisibility(port, View.VISIBLE);
    } else {
      currentModule.setConfigurationType(type);
      if (type == BuiltInConfigurationType.NOTHING) {
        currentModule.setEnabled(false);
      } else {
        currentModule.setEnabled(true);
      }
      setButtonVisibility(port, View.GONE);
    }

    if (DEBUG) {
      DeviceConfiguration module = devices.get(port);
      RobotLog.e("[changeDevice] modules.get(port) name: " + module.getName() + ", port: " + module.getPort() + ", type: " + module.getConfigurationType());
    }

  }


  /**
   * If the drop-down spinner-selected item is a Controller (Motor- or Servo-), we need to create
   * an empty Controller with the proper number of motors/servos.
   * @param port - the port where this controller got added
   * @param newType - the type of controller we're creating
   */
  private void createController(int port, ConfigurationType newType) {

    DeviceConfiguration currentModule = devices.get(port);

    String name = currentModule.getName();

    SerialNumber serialNumber = SerialNumber.createFake();

    ConfigurationType currentType = currentModule.getConfigurationType();
    if (!(currentType == newType)) { //only update the controller if it's a new choice.
      ControllerConfiguration newModule;
      if (newType == BuiltInConfigurationType.MOTOR_CONTROLLER) {
        List<DeviceConfiguration> motors = ConfigurationUtility.buildEmptyMotors(ModernRoboticsConstants.INITIAL_MOTOR_PORT, ModernRoboticsConstants.NUMBER_OF_MOTORS);
        newModule = new MotorControllerConfiguration(name, motors, serialNumber);
        newModule.setPort(port);
      }
      else if (newType == BuiltInConfigurationType.SERVO_CONTROLLER) {
        List<DeviceConfiguration> servos = ConfigurationUtility.buildEmptyServos(ModernRoboticsConstants.INITIAL_SERVO_PORT, ModernRoboticsConstants.NUMBER_OF_SERVOS);
        newModule = new ServoControllerConfiguration(name, servos, serialNumber);
        newModule.setPort(port);
      }
      else if (newType == BuiltInConfigurationType.MATRIX_CONTROLLER) {
        List<DeviceConfiguration> motors = ConfigurationUtility.buildEmptyMotors(MatrixConstants.INITIAL_MOTOR_PORT, MatrixConstants.NUMBER_OF_MOTORS);
        List<DeviceConfiguration> servos = ConfigurationUtility.buildEmptyServos(MatrixConstants.INITIAL_SERVO_PORT, MatrixConstants.NUMBER_OF_SERVOS);

        newModule = new MatrixControllerConfiguration(name, motors, servos, serialNumber);
        newModule.setPort(port);
      }
      else {
         newModule = null;
      }
      if (newModule != null) {
        newModule.setEnabled(true);
        setModule(newModule);
      }
    }
  }

  /**
   * When the drop-down item is a Controller, this button appears. Clicking it launches the
   * appropriate activity
   * @param v - the button that got pressed
   */
  public void editController_portALL(View v) {
    //view is Button
    //view.getParent is RelativeLayout
    //view.getparent.getparent is the LinearLayout around the whole module zone
    LinearLayout layout = (LinearLayout) v.getParent().getParent();
    TextView portNumber = (TextView) layout.findViewById(R.id.portNumber);
    int port = Integer.parseInt(portNumber.getText().toString());
    DeviceConfiguration currentModule = devices.get(port);

    if (!isController(currentModule)) {
      Spinner choiceSpinner = (Spinner) layout.findViewById(R.id.choiceSpinner);
      ConfigurationTypeAndDisplayName pair = (ConfigurationTypeAndDisplayName)choiceSpinner.getSelectedItem();
      createController(port, pair.configurationType);
    }
    editController_general(currentModule);
  }

  private void setModule(DeviceConfiguration dev) {
    int port = dev.getPort();
    devices.set(port, dev);
  }

  /****************** SIMPLE UTILITIES ********************/

  private View findViewByPort(int port) {
    switch (port){
      case 0: return info_port0;
      case 1: return info_port1;
      case 2: return info_port2;
      case 3: return info_port3;
      case 4: return info_port4;
      case 5: return info_port5;
      default: return null;
    }
  }

  /**
   * Makes the "edit controller" button visible or invisible.
   * @param port - the port that now holds a controller
   * @param visibility - whether or not the button should be visible
   */
  private void setButtonVisibility(int port, int visibility ) {
    View layout = findViewByPort(port);
    Button button = (Button) layout.findViewById(R.id.edit_controller_btn);
    button.setVisibility(visibility);
  }

  /**
   *
   * @param module - the module in question
   * @return - whether or not it's a controller, based on device type
   */
  private boolean isController(DeviceConfiguration module) {
    return (module.getConfigurationType() == BuiltInConfigurationType.MOTOR_CONTROLLER ||
            module.getConfigurationType() == BuiltInConfigurationType.SERVO_CONTROLLER);
  }

  /***************************** Private inner class ***************************/

  /**
   * For some reason, the default TextWatcher in Android does not give you any way to access
   * the view around the text field in question. This inner class is instantiated with the
   * correct view, which can be used to find the correct module, so we can update the module's
   * name as soon as the user types it in.
   */
  private class UsefulTextWatcher implements TextWatcher {

    private int port;
    private boolean isController = false;

    private UsefulTextWatcher() {
      isController = true;
    }
    private UsefulTextWatcher(View layout) {
      TextView portNumber = (TextView) layout.findViewById(R.id.portNumber);
      port = Integer.parseInt(portNumber.getText().toString());
    }

    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

    public void afterTextChanged(Editable editable) {
      String text = editable.toString();
      if (isController) {
        controllerConfiguration.setName(text);
      } else {
        DeviceConfiguration dev = devices.get(port);
        dev.setName(text);
      }
    }
  }
}