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
import com.qualcomm.robotcore.hardware.I2cAddr;
import com.qualcomm.robotcore.hardware.I2cAddressableDevice;
import com.qualcomm.robotcore.hardware.I2cController;
import com.qualcomm.robotcore.hardware.I2cControllerPortDeviceImpl;
import com.qualcomm.robotcore.hardware.IrSeekerSensor;
import com.qualcomm.robotcore.util.TypeConversion;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.util.concurrent.locks.Lock;

/**
 * HiTechnic NXT IR Seeker Sensor
 */
public class HiTechnicNxtIrSeekerSensor extends I2cControllerPortDeviceImpl implements IrSeekerSensor, I2cAddressableDevice, I2cController.I2cPortReadyCallback {

  //------------------------------------------------------------------------------------------------
  // Constants
  //------------------------------------------------------------------------------------------------

  public static final I2cAddr I2C_ADDRESS = I2cAddr.create8bit(0x10);
  public static final int MEM_MODE_ADDRESS = 0x41;
  public static final int MEM_DC_START_ADDRESS = 0x42;
  public static final int MEM_AC_START_ADDRESS = 0x49;
  public static final int MEM_READ_LENGTH = 6;

  public static final byte MODE_AC = 0x00;
  public static final byte MODE_DC = 0x02;

  public static final byte DIRECTION = 0 + ModernRoboticsUsbLegacyModule.OFFSET_I2C_PORT_MEMORY_BUFFER;
  public static final byte SENSOR_FIRST = 1 + ModernRoboticsUsbLegacyModule.OFFSET_I2C_PORT_MEMORY_BUFFER;
  public static final byte SENSOR_COUNT = 5 + ModernRoboticsUsbLegacyModule.OFFSET_I2C_PORT_MEMORY_BUFFER;

  public static final double MAX_SENSOR_STRENGTH = 256.0;

  public static final byte INVALID_ANGLE = 0;
  public static final byte MIN_ANGLE = 1;
  public static final byte MAX_ANGLE = 9;

  public static final double[] DIRECTION_TO_ANGLE = {
    0, -120, -90, -60, -30, 0, 30, 60, 90, 120
  };

  public static final double DEFAULT_SIGNAL_DETECTED_THRESHOLD = 1.0 / MAX_SENSOR_STRENGTH;

  //------------------------------------------------------------------------------------------------
  // State
  //------------------------------------------------------------------------------------------------

  private byte[]    readBuffer;
  private Lock      readBufferLock;
  private byte[]    writeBuffer;
  private Lock      writeBufferLock;
  private Mode      mode;

  private double    signalDetectedThreshold = DEFAULT_SIGNAL_DETECTED_THRESHOLD;

  private volatile boolean switchingModes;

  //------------------------------------------------------------------------------------------------
  // Construction
  //------------------------------------------------------------------------------------------------

  public HiTechnicNxtIrSeekerSensor(I2cController module, int physicalPort) {
    super(module, physicalPort);

    // we need to default to Mode.MODE_1200HZ, since that's what the hardware defaults to
    this.mode = Mode.MODE_1200HZ;

    finishConstruction();
  }

  @Override
  protected void controllerNowArmedOrPretending() {
    this.readBuffer = controller.getI2cReadCache(physicalPort);
    this.readBufferLock = controller.getI2cReadCacheLock(physicalPort);
    this.writeBuffer = controller.getI2cWriteCache(physicalPort);
    this.writeBufferLock = controller.getI2cWriteCacheLock(physicalPort);

    controller.registerForI2cPortReadyCallback(this, physicalPort);

    switchingModes = true;
  }

  //------------------------------------------------------------------------------------------------
  // Operations
  //------------------------------------------------------------------------------------------------

  @Override
  public String toString() {
    if (signalDetected()) {
      return String.format("IR Seeker: %3.0f%% signal at %6.1f degrees", getStrength() * 100.0, getAngle());
    }
  else {
    return "IR Seeker:  --% signal at  ---.- degrees";
    }
  }

  @Override
  public void setSignalDetectedThreshold(double threshold) {
    signalDetectedThreshold = threshold;
  }

  @Override
  public double getSignalDetectedThreshold() {
    return signalDetectedThreshold;
  }

  @Override
  public synchronized void setMode(Mode mode) {
    // switching modes is expensive, don't do it if not needed
    if (this.mode == mode) return;

    this.mode = mode;
    writeModeSwitch();
  }

  @Override
  public Mode getMode() {
    return mode;
  }

  @Override
  public boolean signalDetected() {
    if (switchingModes) return false;

    boolean detected = false;

    try {
      readBufferLock.lock();
      // when no signal is detected, buffer[DIRECTION] will equal 0
      detected = readBuffer[DIRECTION] != INVALID_ANGLE;
    } finally {
      readBufferLock.unlock();
    }

    detected = (detected && getStrength() > signalDetectedThreshold);

    return detected;
  }

  @Override
  public double getAngle() {
    if (switchingModes) return INVALID_ANGLE;

    double angle = 0.0;

    try {
      readBufferLock.lock();
      if (readBuffer[DIRECTION] < MIN_ANGLE || readBuffer[DIRECTION] > MAX_ANGLE) {
        angle = INVALID_ANGLE;
      } else {
        angle = DIRECTION_TO_ANGLE[readBuffer[DIRECTION]];
      }
    } finally {
      readBufferLock.unlock();
    }

    return angle;
  }

  @Override
  public double getStrength() {
    if (switchingModes) return 0;

    double strength = 0;

    try {
      readBufferLock.lock();
      for (int i = 0; i < SENSOR_COUNT; i++) {
        strength = Math.max(strength, getSensorStrength(readBuffer, i));
      }
    } finally {
      readBufferLock.unlock();
    }

    return strength;
  }

  @Override
  public IrSeekerIndividualSensor[] getIndividualSensors() {
    IrSeekerIndividualSensor sensors[] = new IrSeekerIndividualSensor[SENSOR_COUNT];
    if (switchingModes) return sensors;

    try {
      readBufferLock.lock();
      for (int i = 0; i < SENSOR_COUNT; i++) {
        double angle = DIRECTION_TO_ANGLE[i * 2 + 1];
        double strength = getSensorStrength(readBuffer, i);
        sensors[i] = new IrSeekerIndividualSensor(angle, strength);
      }
    } finally {
      readBufferLock.unlock();
    }

    return sensors;
  }

  @Override
  public void setI2cAddress(I2cAddr newAddress) {
    // Necessary because of legacy considerations
    throw new UnsupportedOperationException("This method is not supported.");
  }

  @Override
  public I2cAddr getI2cAddress() {
    return I2C_ADDRESS;
  }

  private void writeModeSwitch() {
    switchingModes = true;
    byte modeAsByte = (mode == Mode.MODE_600HZ) ? MODE_DC : MODE_AC;

    controller.enableI2cWriteMode(physicalPort, I2C_ADDRESS, MEM_MODE_ADDRESS, 1);
    try {
      writeBufferLock.lock();
      writeBuffer[ModernRoboticsUsbLegacyModule.OFFSET_I2C_PORT_MEMORY_BUFFER] = modeAsByte;
    } finally {
      writeBufferLock.unlock();
    }
  }

  /*
   * Get the sensor strength, scaled from 0 to 1
   * @param buffer
   * @param sensor
   * @return sensor strength
   */
  private double getSensorStrength(byte[] buffer, int sensor) {
    return TypeConversion.unsignedByteToDouble(buffer[sensor + SENSOR_FIRST]) / MAX_SENSOR_STRENGTH;
  }

  /*
   * Callback method, will be called by the Legacy Module when the port is ready, assuming we
   * registered that call
   */
  public void portIsReady(int port) {

    controller.setI2cPortActionFlag(physicalPort);
    controller.readI2cCacheFromController(physicalPort);

    if (switchingModes) {
      if (mode == Mode.MODE_600HZ) {
        controller.enableI2cReadMode(physicalPort, I2C_ADDRESS, MEM_DC_START_ADDRESS, MEM_READ_LENGTH);
      } else {
        controller.enableI2cReadMode(physicalPort, I2C_ADDRESS, MEM_AC_START_ADDRESS, MEM_READ_LENGTH);
      }
      controller.writeI2cCacheToController(physicalPort);
      switchingModes = false;
    } else {
      controller.writeI2cPortFlagOnlyToController(physicalPort);
    }
  }

  @Override public Manufacturer getManufacturer() {
    return Manufacturer.HiTechnic;
  }

  @Override
  public String getDeviceName() {
    return AppUtil.getDefContext().getString(com.qualcomm.robotcore.R.string.configTypeHTIrSeeker);
  }

  @Override
  public String getConnectionInfo() {
    return controller.getConnectionInfo() + "; port " + physicalPort;
  }

  @Override
  public int getVersion() {
    return 2;
  }

  @Override
  public void resetDeviceConfigurationForOpMode() {
  }

  @Override
  public void close() {
    // take no action
  }
}
