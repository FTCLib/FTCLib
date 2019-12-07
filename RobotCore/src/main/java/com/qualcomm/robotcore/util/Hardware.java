/*
 * Copyright (c) 2014, 2015 Qualcomm Technologies Inc
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * (subject to the limitations in the disclaimer below) provided that the following conditions are
 * met:
 * 
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the
 * distribution.
 * 
 * Neither the name of Qualcomm Technologies Inc nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 * 
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS LICENSE. THIS
 * SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.qualcomm.robotcore.util;

import android.view.InputDevice;

import java.util.HashSet;
import java.util.Set;

/**
 * Utility class for performing hardware operations
 */
public class Hardware {

  private static boolean mIsIFC = CheckIfIFC();
    
  /**
   * Get a list of all game controller ID's connected to this device
   * 
   * @return a set of all game controller ID's
   */
  public static Set<Integer> getGameControllerIds() {
    final Set<Integer> gameControllerDeviceIds = new HashSet<Integer>();
    final int[] deviceIds = InputDevice.getDeviceIds();
    for (final int deviceId : deviceIds) {
      final int sources = InputDevice.getDevice(deviceId).getSources();

      if (((sources & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD)
          || ((sources & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK)) {
        gameControllerDeviceIds.add(deviceId);
      }
    }
    return gameControllerDeviceIds;
  }

  /**
   * Function that returns a pre-computed flag indicating whether or not the platform is an IFC 
   * board.
   * 
   * @return true if IFC board, false if not
   * 
   */
  public static boolean IsIFC()
  {
     return mIsIFC;
  }
    
  /**
   * Function that actually checks whether or not the platform is an IFC board.
   * 
   * @return true if IFC board, false if not (could be a different board or smartphone.
   * 
   */
  public static boolean CheckIfIFC() {
    boolean ifcBoard = false;

    final String board = android.os.Build.BOARD;
    final String brand = android.os.Build.BRAND;
    final String device = android.os.Build.DEVICE;
    final String hardware = android.os.Build.HARDWARE;
    final String manufacturer = android.os.Build.MANUFACTURER;
    final String model = android.os.Build.MODEL;
    final String product = android.os.Build.PRODUCT;

    // Check what platform this is
    // See http://developer.android.com/reference/android/os/Build.html#BOARD

    RobotLog.d("Platform information: board = " + board + " brand = " + brand + " device = "
        + device + " hardware = " + hardware + " manufacturer = " + manufacturer + " model = "
        + model + " product = " + product);

    if ((board.equals("MSM8960") == true) && (brand.equals("qcom") == true)
        && (device.equals("msm8960") == true) && (hardware.equals("qcom") == true)
        && (manufacturer.equals("unknown") == true) && (model.equals("msm8960") == true)
        && (product.equals("msm8960") == true)) {
      RobotLog.d("Detected IFC6410 Device!");
      ifcBoard = true;
    } else {
      RobotLog.d("Detected regular SmartPhone Device!");
    }

    return ifcBoard;
  }
}
