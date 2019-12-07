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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.qualcomm.robotcore.hardware.configuration.ControllerConfiguration;
import com.qualcomm.robotcore.util.SerialNumber;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class DeviceInfoAdapter extends BaseAdapter implements ListAdapter {

  private List<ControllerConfiguration> deviceControllers = new LinkedList<ControllerConfiguration>();
  private EditActivity editActivity;
  private int list_id;

  public DeviceInfoAdapter(EditActivity editActivity, int list_id, List<ControllerConfiguration> deviceControllers) {
    super();
    this.editActivity = editActivity;
    this.deviceControllers = deviceControllers;
    this.list_id = list_id;
  }

  @Override
  public int getCount() {
    return deviceControllers.size();
  }

  @Override
  public Object getItem(int arg0) {
    return deviceControllers.get(arg0);
  }

  @Override
  public long getItemId(int arg0) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public View getView(int pos, View convertView, ViewGroup parent) {
    View row = convertView;
    if (row == null){
      LayoutInflater inflater = editActivity.getLayoutInflater();
      row = inflater.inflate(list_id, parent, false);
    }

    ControllerConfiguration controllerConfiguration = deviceControllers.get(pos);
    String serialNum = editActivity.formatSerialNumber(editActivity, controllerConfiguration);
    TextView displayNum = (TextView)row.findViewById(android.R.id.text2);
    displayNum.setText(serialNum);

    String name = deviceControllers.get(pos).getName();
    TextView text = (TextView)row.findViewById(android.R.id.text1);
    text.setText(name);
    return row;

  }
}