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
import com.qualcomm.robotcore.hardware.DistanceSensor;
import com.qualcomm.robotcore.hardware.I2cAddr;
import com.qualcomm.robotcore.hardware.I2cController;
import com.qualcomm.robotcore.hardware.LegacyModulePortDeviceImpl;
import com.qualcomm.robotcore.hardware.UltrasonicSensor;
import com.qualcomm.robotcore.util.TypeConversion;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.util.concurrent.locks.Lock;

/**
 * Ultrasonic Sensor
 */
public class HiTechnicNxtUltrasonicSensor extends LegacyModulePortDeviceImpl implements UltrasonicSensor, DistanceSensor, I2cController.I2cPortReadyCallback {

  //------------------------------------------------------------------------------------------------
  // Constants
  //------------------------------------------------------------------------------------------------

  public static final I2cAddr I2C_ADDRESS = I2cAddr.create8bit(2);
  public static final int ADDRESS_DISTANCE = 0x42;
  public static final int MAX_PORT = 5;
  public static final int MIN_PORT = 4;

  protected static final int cmUltrasonicMax = 255;

  //------------------------------------------------------------------------------------------------
  // State
  //------------------------------------------------------------------------------------------------

  Lock   readLock;
  byte[] readBuffer;

  //------------------------------------------------------------------------------------------------
  // Construction
  //------------------------------------------------------------------------------------------------

  public HiTechnicNxtUltrasonicSensor(/*not LegacyModule since we verify port*/ ModernRoboticsUsbLegacyModule legacyModule, int physicalPort) {
    super(legacyModule, physicalPort);
    throwIfPortIsInvalid(physicalPort);
    finishConstruction();
  }

  @Override
  protected void moduleNowArmedOrPretending() {

    readLock = module.getI2cReadCacheLock(physicalPort);
    readBuffer = module.getI2cReadCache(physicalPort);

    module.enableI2cReadMode(physicalPort, I2C_ADDRESS, ADDRESS_DISTANCE, 1);
    module.enable9v(physicalPort, true);
    module.setI2cPortActionFlag(physicalPort);
    module.readI2cCacheFromController(physicalPort);

    module.registerForI2cPortReadyCallback(this, physicalPort);
  }

  //------------------------------------------------------------------------------------------------
  // Operations
  //------------------------------------------------------------------------------------------------

  @Override
  public String toString() {
    return String.format("Ultrasonic: %6.1f", getUltrasonicLevel());
  }

  protected byte rawUltrasonic() {
    byte distance;
    try {
      readLock.lock();
      distance = readBuffer[4];
    } finally {
      readLock.unlock();
    }
    return distance;
  }

  @Override
  public double getUltrasonicLevel() {
    byte distance = rawUltrasonic();
    return TypeConversion.unsignedByteToDouble(distance);
  }

  @Override public double getDistance(DistanceUnit unit) {
    int cm = TypeConversion.unsignedByteToInt(rawUltrasonic());
    return cm==cmUltrasonicMax
            ? DistanceSensor.distanceOutOfRange
            : unit.fromUnit(DistanceUnit.CM, cm);
  }

 /*
  * Callback method, will be called by the Legacy Module when the port is ready, assuming we
  * registered that call
  */
  @Override
  public void portIsReady(int port) {
    module.setI2cPortActionFlag(physicalPort);
    module.writeI2cCacheToController(physicalPort);
    module.readI2cCacheFromController(physicalPort);
  }

  @Override
  public String status() {
    return String.format("%s, connected via device %s, port %d",
            getDeviceName(),
            module.getSerialNumber(), physicalPort);  }

  @Override public Manufacturer getManufacturer() {
    return Manufacturer.Lego;
  }

  @Override
  public String getDeviceName() {
    return AppUtil.getDefContext().getString(com.qualcomm.robotcore.R.string.configTypeNXTUltrasonicSensor);
  }

  @Override
  public String getConnectionInfo() {
    return module.getConnectionInfo() + "; port " + physicalPort;
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

  private void throwIfPortIsInvalid(int port) {
    if (port < MIN_PORT || port > MAX_PORT) {
      throw new IllegalArgumentException(
          String.format( "Port %d is invalid for " + getDeviceName()+ "; valid ports are %d or %d", port, MIN_PORT, MAX_PORT));
    }
  }
}
