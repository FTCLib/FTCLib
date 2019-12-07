/* Copyright (c) 2015 Qualcomm Technologies Inc

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

package com.qualcomm.robotcore.hardware.usb.ftdi;

import android.content.Context;

import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.usb.RobotUsbDevice;
import com.qualcomm.robotcore.hardware.usb.RobotUsbManager;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.SerialNumber;

import org.firstinspires.ftc.robotcore.internal.ftdi.FtDeviceIOException;
import org.firstinspires.ftc.robotcore.internal.ftdi.FtDeviceManager;
import org.firstinspires.ftc.robotcore.internal.ftdi.FtDevice;
import org.firstinspires.ftc.robotcore.internal.ftdi.FtDeviceInfo;
import org.firstinspires.ftc.robotcore.internal.usb.exception.RobotUsbException;

import java.util.ArrayList;
import java.util.List;

public class RobotUsbManagerFtdi implements RobotUsbManager {

  public static final String TAG = "RobotUsbManagerFtdi";

  private Context     context;
  private FtDeviceManager ftDeviceManager;
  private int         numberOfDevices;

  /**
   * Constructor
   * @param context application context
   */
  public RobotUsbManagerFtdi(Context context) throws RobotCoreException {
    this.context = context;

    try {
      ftDeviceManager = FtDeviceManager.getInstance(context);
    } catch (FtDeviceIOException e) {
      RobotLog.ee(TAG, e, "Unable to create FtDeviceManager; cannot open USB devices");
      throw RobotCoreException.createChained(e, "unable to create FtDeviceManager");
    }
  }

  /**
   * Scan for USB devices
   * @return number of USB devices found
   * @throws RobotCoreException
   */
  @Override
  public synchronized List<SerialNumber> scanForDevices() throws RobotCoreException {
    numberOfDevices = ftDeviceManager.createDeviceInfoList(context);
    List<SerialNumber> result = new ArrayList<>(numberOfDevices);
    for (int i = 0; i < numberOfDevices; i++) {
        result.add(getDeviceSerialNumberByIndex(i));
    }
    return result;
  }

 /**
   * get device serial number. Is thread safe.
   * @param index index of device
   * @return serial number
   * @throws RobotCoreException
   */
  protected SerialNumber getDeviceSerialNumberByIndex(int index) throws RobotCoreException {
    return SerialNumber.fromString(ftDeviceManager.getDeviceInfoListDetail(index).serialNumber);
  }

  public static SerialNumber getSerialNumber(FtDevice device) {
    FtDeviceInfo devInfo = device.getDeviceInfo();
    return SerialNumber.fromString(devInfo.serialNumber);
  }

  /**
   * Open device by serial number. Is threadsafe since we only ever pass the one context.
   * @param serialNumber USB serial number
   * @return usb device
   * @throws RobotCoreException
   */
  @Override
  public RobotUsbDevice openBySerialNumber(SerialNumber serialNumber) throws RobotCoreException {
    // openBySerialNumber() will return null if the device can't be opened. In particular, it
    // will return null if the device is *already* opened.
    FtDevice device = ftDeviceManager.openBySerialNumber(context, serialNumber.getString());
    if (device == null) {
      throw new RobotCoreException("FTDI driver failed to open USB device with serial number " + serialNumber + " (returned null device)");
    }
    // Some good housekeeping: reset the FTDI chip in the device (why not?)
    try {
      device.resetDevice();
    } catch (RobotUsbException e) {
      RobotLog.ee(TAG, e, "unable to reset FtDevice(%s): ignoring");
    }

    return new RobotUsbDeviceFtdi(device, serialNumber);
  }
}
