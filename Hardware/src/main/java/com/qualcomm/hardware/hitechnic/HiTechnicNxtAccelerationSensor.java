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

import com.qualcomm.hardware.R;
import com.qualcomm.hardware.modernrobotics.ModernRoboticsUsbLegacyModule;
import com.qualcomm.robotcore.hardware.AccelerationSensor;
import com.qualcomm.robotcore.hardware.I2cAddr;
import com.qualcomm.robotcore.hardware.I2cController;
import com.qualcomm.robotcore.hardware.I2cControllerPortDeviceImpl;

import org.firstinspires.ftc.robotcore.external.navigation.Acceleration;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.util.concurrent.locks.Lock;

public class HiTechnicNxtAccelerationSensor extends I2cControllerPortDeviceImpl implements AccelerationSensor, I2cController.I2cPortReadyCallback {

  //------------------------------------------------------------------------------------------------
  // State
  //------------------------------------------------------------------------------------------------

  public static final I2cAddr I2C_ADDRESS = I2cAddr.create8bit(2);

  public static final int ADDRESS_ACCEL_START = 0x42;
  public static final int ACCEL_LENGTH = 0x6;

  // the value the sensor will return when at 1g
  private static final double ONE_G = 200.0;

  // shift the high byte by 2
  private static final double HIGH_BYTE_SCALING_VALUE = 4.0;

  private static final int X_HIGH_BYTE = 0 + ModernRoboticsUsbLegacyModule.OFFSET_I2C_PORT_MEMORY_BUFFER;
  private static final int Y_HIGH_BYTE = 1 + ModernRoboticsUsbLegacyModule.OFFSET_I2C_PORT_MEMORY_BUFFER;
  private static final int Z_HIGH_BYTE = 2 + ModernRoboticsUsbLegacyModule.OFFSET_I2C_PORT_MEMORY_BUFFER;
  private static final int X_LOW_BYTE = 3 + ModernRoboticsUsbLegacyModule.OFFSET_I2C_PORT_MEMORY_BUFFER;
  private static final int Y_LOW_BYTE = 4 + ModernRoboticsUsbLegacyModule.OFFSET_I2C_PORT_MEMORY_BUFFER;
  private static final int Z_LOW_BYTE = 5 + ModernRoboticsUsbLegacyModule.OFFSET_I2C_PORT_MEMORY_BUFFER;

  private byte[] readBuffer;
  private Lock readBufferLock;

  //------------------------------------------------------------------------------------------------
  // Construction
  //------------------------------------------------------------------------------------------------

  public HiTechnicNxtAccelerationSensor(I2cController module, int physicalPort) {
    super(module, physicalPort);
    finishConstruction();
  }

  protected void controllerNowArmedOrPretending() {

    controller.enableI2cReadMode(physicalPort, I2C_ADDRESS, ADDRESS_ACCEL_START, ACCEL_LENGTH);

    this.readBuffer = controller.getI2cReadCache(physicalPort);
    this.readBufferLock = controller.getI2cReadCacheLock(physicalPort);

    controller.registerForI2cPortReadyCallback(this, physicalPort);
  }

  //------------------------------------------------------------------------------------------------
  // AccelerationSensor
  //------------------------------------------------------------------------------------------------

  @Override
  public String toString() {
    return getAcceleration().toString();
  }

  @Override
  public Acceleration getAcceleration() {

    try {
      readBufferLock.lock();
      double gx = rawToG(readBuffer[X_HIGH_BYTE], readBuffer[X_LOW_BYTE]);
      double gy = rawToG(readBuffer[Y_HIGH_BYTE], readBuffer[Y_LOW_BYTE]);
      double gz = rawToG(readBuffer[Z_HIGH_BYTE], readBuffer[Z_LOW_BYTE]);
      return Acceleration.fromGravity(gx, gy, gz, System.nanoTime());
    } finally {
      readBufferLock.unlock();
    }
  }

  @Override
  public String status() {
    return String.format("NXT Acceleration Sensor, connected via device %s, port %d",
        controller.getSerialNumber(), physicalPort);
  }

  private double rawToG(double high, double low) {
    return (high * HIGH_BYTE_SCALING_VALUE + low) / ONE_G;
  }

  @Override
  public void portIsReady(int port) {
    controller.setI2cPortActionFlag(physicalPort);
    controller.writeI2cPortFlagOnlyToController(physicalPort);
    controller.readI2cCacheFromController(physicalPort);
  }

  @Override public Manufacturer getManufacturer() {
    return Manufacturer.HiTechnic;
  }

  @Override
  public String getDeviceName() {
    return AppUtil.getDefContext().getString(R.string.configTypeHTAccelerometer);
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
  }

  @Override
  public void close() {
    // take no action
  }
}
