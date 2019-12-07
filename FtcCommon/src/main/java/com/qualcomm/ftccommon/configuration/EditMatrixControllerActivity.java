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

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.qualcomm.ftccommon.R;
import com.qualcomm.robotcore.hardware.configuration.DeviceConfiguration;
import com.qualcomm.robotcore.hardware.configuration.MatrixControllerConfiguration;

import java.util.List;

public class EditMatrixControllerActivity extends EditActivity {

  @Override public String getTag() { return this.getClass().getSimpleName(); }	
  public static final RequestCode requestCode = RequestCode.EDIT_MATRIX_CONTROLLER;
  private MatrixControllerConfiguration matrixControllerConfigurationConfig;
  private List<DeviceConfiguration> motors;
  private List<DeviceConfiguration> servos;
  private EditText controller_name;

  private View info_port1;
  private View info_port2;
  private View info_port3;
  private View info_port4;
  private View info_port5;
  private View info_port6;
  private View info_port7;
  private View info_port8;

  @Override
  protected void onCreate(Bundle savedInstanceState){
    super.onCreate(savedInstanceState);
    setContentView(R.layout.matrices);

    controller_name = (EditText) findViewById(R.id.matrixcontroller_name);

    // Servos
    LinearLayout layout_port1 = (LinearLayout) findViewById(R.id.linearLayout_matrix1);
    info_port1 = getLayoutInflater().inflate(R.layout.matrix_devices, layout_port1, true);
    TextView port1 = (TextView) info_port1.findViewById(R.id.port_number);
    port1.setText("1");

    LinearLayout layout_port2 = (LinearLayout) findViewById(R.id.linearLayout_matrix2);
    info_port2 = getLayoutInflater().inflate(R.layout.matrix_devices, layout_port2, true);
    TextView port2 = (TextView) info_port2.findViewById(R.id.port_number);
    port2.setText("2");

    LinearLayout layout_port3 = (LinearLayout) findViewById(R.id.linearLayout_matrix3);
    info_port3 = getLayoutInflater().inflate(R.layout.matrix_devices, layout_port3, true);
    TextView port3 = (TextView) info_port3.findViewById(R.id.port_number);
    port3.setText("3");

    LinearLayout layout_port4 = (LinearLayout) findViewById(R.id.linearLayout_matrix4);
    info_port4 = getLayoutInflater().inflate(R.layout.matrix_devices, layout_port4, true);
    TextView port4 = (TextView) info_port4.findViewById(R.id.port_number);
    port4.setText("4");

    // Motors
    LinearLayout layout_port5 = (LinearLayout) findViewById(R.id.linearLayout_matrix5);
    info_port5 = getLayoutInflater().inflate(R.layout.matrix_devices, layout_port5, true);
    TextView port5 = (TextView) info_port5.findViewById(R.id.port_number);
    port5.setText("1");

    LinearLayout layout_port6 = (LinearLayout) findViewById(R.id.linearLayout_matrix6);
    info_port6 = getLayoutInflater().inflate(R.layout.matrix_devices, layout_port6, true);
    TextView port6 = (TextView) info_port6.findViewById(R.id.port_number);
    port6.setText("2");

    LinearLayout layout_port7 = (LinearLayout) findViewById(R.id.linearLayout_matrix7);
    info_port7 = getLayoutInflater().inflate(R.layout.matrix_devices, layout_port7, true);
    TextView port7 = (TextView) info_port7.findViewById(R.id.port_number);
    port7.setText("3");

    LinearLayout layout_port8 = (LinearLayout) findViewById(R.id.linearLayout_matrix8);
    info_port8 = getLayoutInflater().inflate(R.layout.matrix_devices, layout_port8, true);
    TextView port8 = (TextView) info_port8.findViewById(R.id.port_number);
    port8.setText("4");

    EditParameters parameters = EditParameters.fromIntent(this, getIntent());
    if (parameters != null) {
      matrixControllerConfigurationConfig = (MatrixControllerConfiguration)parameters.getConfiguration();
      motors = matrixControllerConfigurationConfig.getMotors();
      servos = matrixControllerConfigurationConfig.getServos();
    }

    controller_name.setText(matrixControllerConfigurationConfig.getName());

    for (int i = 0; i < motors.size(); i++) {
      View info_port = findMotorViewByPort(i+1);
      addCheckBoxListenerOnPort(i+1, info_port, motors);
      addNameTextChangeWatcherOnPort(info_port, motors.get(i));
      handleDisabledDevice(i+1, info_port, motors);
    }

    for (int i = 0; i < servos.size(); i++) {
      View info_port = findServoViewByPort(i+1);
      addCheckBoxListenerOnPort(i+1, info_port, servos);
      addNameTextChangeWatcherOnPort(info_port, servos.get(i));
      handleDisabledDevice(i+1, info_port, servos);
    }
  }

  @Override
  protected void onStart() {
    super.onStart();
  }

  private void addNameTextChangeWatcherOnPort(View info_port, DeviceConfiguration module) {
    EditText name = (EditText) info_port.findViewById(R.id.editTextResult);

    name.addTextChangedListener(new UsefulTextWatcher(module));
  }

  private void handleDisabledDevice(int port, View info_port, List<? extends DeviceConfiguration> list) {
    CheckBox checkbox = (CheckBox) info_port.findViewById(R.id.checkbox_port);
    DeviceConfiguration device = list.get(port-1);
    if (device.isEnabled()) {
      checkbox.setChecked(true);
      EditText name = (EditText) info_port.findViewById(R.id.editTextResult);
      name.setText(device.getName());
    } else {
      checkbox.setChecked(true); // kind of a hack. Sets the checkbox to true, so
      // when performing the click programmatically,
      // the checkbox becomes "unclicked" which does the right thing.
      checkbox.performClick();
    }
  }

  private void addCheckBoxListenerOnPort(final int port, View info_port, List<? extends DeviceConfiguration> list) {

    final EditText name;
    name = (EditText) info_port.findViewById(R.id.editTextResult);

    final DeviceConfiguration device;
    device = list.get(port-1);

    CheckBox checkbox = (CheckBox) info_port.findViewById(R.id.checkbox_port);
    checkbox.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if ( ((CheckBox) view).isChecked()) {
          name.setEnabled(true);
          name.setText("");
          device.setEnabled(true);
          device.setName("");
        } else {
          name.setEnabled(false);
          device.setEnabled(false);
          device.setName(disabledDeviceName());
          name.setText(disabledDeviceName());
        }
      }
    });
  }

  private View findServoViewByPort(int port) {
    switch (port) {
      case 1:
        return info_port1;
      case 2:
        return info_port2;
      case 3:
        return info_port3;
      case 4:
        return info_port4;
      default:
        return null;
    }
  }

  private View findMotorViewByPort(int port) {
    switch (port) {
      case 1:
        return info_port5;
      case 2:
        return info_port6;
      case 3:
        return info_port7;
      case 4:
        return info_port8;
      default:
        return null;
    }
  }

  public void onDoneButtonPressed(View v) {
    finishOk();
  }

  @Override
  protected void finishOk() {
    matrixControllerConfigurationConfig.setServos(servos);
    matrixControllerConfigurationConfig.setMotors(motors);
    matrixControllerConfigurationConfig.setName(controller_name.getText().toString());
    //
    finishOk(new EditParameters(this, matrixControllerConfigurationConfig));
  }

  public void onCancelButtonPressed(View v) {
    finishCancel();
  }

  /***************************** Private inner class ***************************/

  /**
   * For some reason, the default TextWatcher in Android does not give you any way to access
   * the view around the text field in question. This inner class is instantiated with the
   * correct view, which can be used to find the correct module, so we can update the module's
   * name as soon as the user types it in.
   */
  private class UsefulTextWatcher implements TextWatcher {

    private DeviceConfiguration module;
    private UsefulTextWatcher(DeviceConfiguration module) {
      this.module = module;
    }

    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

    public void afterTextChanged(Editable editable) {
      String text = editable.toString();
      module.setName(text);
    }
  }

}