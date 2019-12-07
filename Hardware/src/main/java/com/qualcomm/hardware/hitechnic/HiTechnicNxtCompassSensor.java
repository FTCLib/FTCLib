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

package com.qualcomm.hardware.hitechnic;

import com.qualcomm.hardware.modernrobotics.ModernRoboticsUsbLegacyModule;
import com.qualcomm.robotcore.hardware.CompassSensor;
import com.qualcomm.robotcore.hardware.I2cAddr;
import com.qualcomm.robotcore.hardware.I2cController;
import com.qualcomm.robotcore.hardware.I2cControllerPortDeviceImpl;
import com.qualcomm.robotcore.util.TypeConversion;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.concurrent.locks.Lock;

public class HiTechnicNxtCompassSensor extends I2cControllerPortDeviceImpl implements CompassSensor, I2cController.I2cPortReadyCallback {

  //------------------------------------------------------------------------------------------------
  // Constants
  //------------------------------------------------------------------------------------------------

  public static final I2cAddr I2C_ADDRESS = I2cAddr.create8bit(2);
  public static final byte MODE_CONTROL_ADDRESS = 0x41;
  public static final byte CALIBRATION = 0x43;
  public static final byte MEASUREMENT = 0x00;
  public static final byte HEADING_IN_TWO_DEGREE_INCREMENTS = 0x42; // current heading divided by 2, i.e., 0-179
  public static final byte ONE_DEGREE_HEADING_ADDER = 0x43; // current heading (mod 2)
  public static final byte CALIBRATION_FAILURE = 0x46;

  public static final byte DIRECTION_START = 3 + ModernRoboticsUsbLegacyModule.OFFSET_I2C_PORT_MEMORY_BUFFER;
  public static final byte DIRECTION_END = 5 + ModernRoboticsUsbLegacyModule.OFFSET_I2C_PORT_MEMORY_BUFFER;

  public static final double INVALID_DIRECTION = -1.0;

  public static final int HEADING_WORD_LENGTH = 0x2; // # of bytes
  public static final int COMPASS_BUFFER = 0x41; // read the entire 5 byte buffer
  public static final int COMPASS_BUFFER_SIZE = 5;

  //------------------------------------------------------------------------------------------------
  // State
  //------------------------------------------------------------------------------------------------

  private byte[]      readBuffer;
  private Lock        readBufferLock;
  private byte[]      writeBuffer;
  private Lock        writeBufferLock;
  private CompassMode mode = CompassMode.MEASUREMENT_MODE; // defaults to measurement
  private boolean     switchingModes = false;

  private double      direction;
  private boolean     calibrationFailed = false;

  //------------------------------------------------------------------------------------------------
  // Construction
  //------------------------------------------------------------------------------------------------

  public HiTechnicNxtCompassSensor(I2cController module, int physicalPort) {
    super(module, physicalPort);
    finishConstruction();
  }

  @Override
  protected void controllerNowArmedOrPretending() {
    controller.enableI2cReadMode(physicalPort, I2C_ADDRESS, COMPASS_BUFFER, COMPASS_BUFFER_SIZE);

    this.readBuffer = controller.getI2cReadCache(physicalPort);
    this.readBufferLock = controller.getI2cReadCacheLock(physicalPort);
    this.writeBuffer = controller.getI2cWriteCache(physicalPort);
    this.writeBufferLock = controller.getI2cWriteCacheLock(physicalPort);

    controller.registerForI2cPortReadyCallback(this, physicalPort);
  }

  //------------------------------------------------------------------------------------------------
  // Operations
  //------------------------------------------------------------------------------------------------

  @Override
  public String toString() {
    return String.format("Compass: %3.1f", getDirection());
  }

  @Override
  public synchronized double getDirection() {
    if (switchingModes) return INVALID_DIRECTION;
    if (mode == CompassMode.CALIBRATION_MODE) return INVALID_DIRECTION;

    byte[] heading = null;

    try {
      readBufferLock.lock();
      heading = Arrays.copyOfRange(readBuffer, DIRECTION_START, DIRECTION_END);
    } finally {
      readBufferLock.unlock();
    }

    return (double)TypeConversion.byteArrayToShort(heading, ByteOrder.LITTLE_ENDIAN);
  }

  @Override
  public String status() {
    return String.format("NXT Compass Sensor, connected via device %s, port %d",
        controller.getSerialNumber(), physicalPort);
  }

  public synchronized void setMode(CompassMode mode){
    // switching modes is expensive, don't do it if not needed
    if (this.mode == mode) return;

    this.mode = mode;
    writeModeSwitch();
  }

  private void writeModeSwitch() {
    switchingModes = true;
    byte modeAsByte = (mode == CompassMode.CALIBRATION_MODE) ? CALIBRATION : MEASUREMENT;

    // Only return to read mode after switching to "measurement mode",
    // because of how the docs describe the calibration sequence.
    controller.enableI2cWriteMode(physicalPort, I2C_ADDRESS, MODE_CONTROL_ADDRESS, 1);
    try {
      writeBufferLock.lock();
      writeBuffer[ModernRoboticsUsbLegacyModule.OFFSET_I2C_PORT_MEMORY_LENGTH] = modeAsByte;
    } finally {
      writeBufferLock.unlock();
    }
  }

  private void readModeSwitch() {
    if (mode == CompassMode.MEASUREMENT_MODE) {
      controller.enableI2cReadMode(physicalPort, I2C_ADDRESS, COMPASS_BUFFER, COMPASS_BUFFER_SIZE);
    }

    switchingModes = false;
  }

  /*
   * Checks the "mode" field for the failure byte.
   * After attempting a calibration, the hardware will (eventually) write 0x46 back to
   * this field if it was unsuccessful. Otherwise, it will be 0x00 to indicate success, even if
   * the hardware is not sure the calibration was successful.
   * A user should monitor this field for several seconds to determine success.
   */
  @Override
  public synchronized boolean calibrationFailed() {
    // Hardware default is "success", so extended monitoring is necessary.
    if (mode == CompassMode.CALIBRATION_MODE || switchingModes) return false;

    boolean failed = false;
    try {
      readBufferLock.lock();
      failed = readBuffer[ModernRoboticsUsbLegacyModule.OFFSET_I2C_PORT_MEMORY_LENGTH] == CALIBRATION_FAILURE;
    } finally {
      readBufferLock.unlock();
    }
    return failed;
  }

  /*
   * Callback method, will be called by the Legacy Module when the port is ready, assuming we
   * registered that call
   */
  public synchronized void portIsReady(int port) {
    controller.setI2cPortActionFlag(physicalPort);
    controller.readI2cCacheFromController(physicalPort);

      if (switchingModes) {
        readModeSwitch();
        controller.writeI2cCacheToController(physicalPort);
      } else {
        controller.writeI2cPortFlagOnlyToController(physicalPort);
      }
  }

  @Override public Manufacturer getManufacturer() {
    return Manufacturer.HiTechnic;
  }

  @Override
  public String getDeviceName() {
    return AppUtil.getDefContext().getString(com.qualcomm.robotcore.R.string.configTypeHTCompass);
  }

  @Override
  public String getConnectionInfo() {
    return controller.getConnectionInfo() + "; port " + physicalPort;
  }

  @Override
  public int getVersion() {
    return 1;
  }

  @Override
  public void resetDeviceConfigurationForOpMode() {
    // TODO: reset to measurement mode
  }

  @Override
  public void close() {
    // take no action
  }
}
