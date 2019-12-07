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

package com.qualcomm.hardware.modernrobotics;

import android.content.Context;

import com.qualcomm.hardware.HardwareDeviceManager;
import com.qualcomm.hardware.R;
import com.qualcomm.hardware.modernrobotics.comm.ReadWriteRunnable;
import com.qualcomm.hardware.modernrobotics.comm.ReadWriteRunnableSegment;
import com.qualcomm.hardware.modernrobotics.comm.ReadWriteRunnableStandard;
import com.qualcomm.robotcore.eventloop.SyncdDevice;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.DeviceInterfaceModule;
import com.qualcomm.robotcore.hardware.DigitalChannel;
import com.qualcomm.robotcore.hardware.I2cAddr;
import com.qualcomm.robotcore.hardware.usb.RobotUsbDevice;
import com.qualcomm.robotcore.util.Range;
import com.qualcomm.robotcore.util.SerialNumber;
import com.qualcomm.robotcore.util.TypeConversion;

import org.firstinspires.ftc.robotcore.internal.hardware.TimeWindow;

import java.nio.ByteOrder;
import java.util.concurrent.locks.Lock;

/**
 * Modern Robotics USB Core Device Interface Module
 *
 * Use {@link HardwareDeviceManager} to create an instance of this class
 */
@SuppressWarnings("unused")
public class ModernRoboticsUsbDeviceInterfaceModule extends ModernRoboticsUsbI2cController implements DeviceInterfaceModule {

  //------------------------------------------------------------------------------------------------
  // State
  //------------------------------------------------------------------------------------------------

  public final static String TAG = "MRDeviceInterfaceModule";
  @Override protected String getTag() { return TAG; }

  /**
   * Enable DEBUG_LOGGING logging
   */
  public static final boolean DEBUG_LOGGING = false;

  public static final int MIN_I2C_PORT_NUMBER = 0;
  public static final int MAX_I2C_PORT_NUMBER = 5;
  public static final int MAX_ANALOG_PORT_NUMBER = 7;
  public static final int MIN_ANALOG_PORT_NUMBER = 0;
  public static final int NUMBER_OF_PORTS = 6;
  public static final int START_ADDRESS = 0x03;
  public static final int MONITOR_LENGTH  = 0x15;
  public static final int SIZE_I2C_BUFFER = 27;
  public static final int SIZE_ANALOG_BUFFER = 2;
  public static final int WORD_SIZE = 2;
  public static final double MAX_ANALOG_INPUT_VOLTAGE = 5.0;

  public static final int ADDRESS_BUFFER_STATUS = 0x03;
  public static final int ADDRESS_ANALOG_PORT_A0 = 0x04;
  public static final int ADDRESS_ANALOG_PORT_A1 = 0x06;
  public static final int ADDRESS_ANALOG_PORT_A2 = 0x08;
  public static final int ADDRESS_ANALOG_PORT_A3 = 0x0A;
  public static final int ADDRESS_ANALOG_PORT_A4 = 0x0C;
  public static final int ADDRESS_ANALOG_PORT_A5 = 0x0E;
  public static final int ADDRESS_ANALOG_PORT_A6 = 0x10;
  public static final int ADDRESS_ANALOG_PORT_A7 = 0x12;

  public static final int ADDRESS_DIGITAL_INPUT_STATE = 0x14;
  public static final int ADDRESS_DIGITAL_IO_CONTROL = 0x15;
  public static final int ADDRESS_DIGITAL_OUTPUT_STATE = 0x16;

  public static final int ADDRESS_LED_SET = 0x17;

  public static final int ADDRESS_VOLTAGE_OUTPUT_PORT_0 = 0x18;
  public static final int ADDRESS_VOLTAGE_OUTPUT_PORT_1 = 0x1E;

  public static final int ADDRESS_PULSE_OUTPUT_PORT_0 = 0X24;
  public static final int ADDRESS_PULSE_OUTPUT_PORT_1 = 0X28;

  public static final int ADDRESS_I2C0 = 0x30;
  public static final int ADDRESS_I2C1 = 0x50;
  public static final int ADDRESS_I2C2 = 0x70;
  public static final int ADDRESS_I2C3 = 0x90;
  public static final int ADDRESS_I2C4 = 0xB0;
  public static final int ADDRESS_I2C5 = 0xD0;

  public static final byte BUFFER_FLAG_I2C0     = (byte) 0x01;
  public static final byte BUFFER_FLAG_I2C1     = (byte) 0x02;
  public static final byte BUFFER_FLAG_I2C2     = (byte) 0x04;
  public static final byte BUFFER_FLAG_I2C3     = (byte) 0x08;
  public static final byte BUFFER_FLAG_I2C4     = (byte) 0x10;
  public static final byte BUFFER_FLAG_I2C5     = (byte) 0x20;

  /*
   * memory offsets used by this controller
   */
  public static final int OFFSET_ANALOG_VOLTAGE_OUTPUT_VOLTAGE = 0;
  public static final int OFFSET_ANALOG_VOLTAGE_OUTPUT_FREQ = 0x2;
  public static final int OFFSET_ANALOG_VOLTAGE_OUTPUT_MODE = 0x4;
  public static final int ANALOG_VOLTAGE_OUTPUT_BUFFER_SIZE = 0x5;

  public static final int OFFSET_PULSE_OUTPUT_TIME = 0x0;
  public static final int OFFSET_PULSE_OUTPUT_PERIOD = 0x2;
  public static final int PULSE_OUTPUT_BUFFER_SIZE = 0x4;

  public static final int OFFSET_I2C_PORT_MODE = 0x0;
  public static final int OFFSET_I2C_PORT_I2C_ADDRESS = 0x1;
  public static final int OFFSET_I2C_PORT_MEMORY_ADDRESS = 0x2;
  public static final int OFFSET_I2C_PORT_MEMORY_LENGTH = 0x3;
  public static final int OFFSET_I2C_PORT_MEMORY_BUFFER = 0x4;
  public static final int OFFSET_I2C_PORT_FLAG = 0x1f;
  public static final int I2C_PORT_BUFFER_SIZE = 0x20;

  public static final byte I2C_MODE_READ        = (byte) 0x80;
  public static final byte I2C_MODE_WRITE       = (byte) 0x00;
  public static final byte I2C_ACTION_FLAG      = (byte) 0xff;
  public static final byte I2C_NO_ACTION_FLAG   = (byte) 0x00;

  public static final int LED_0_BIT_MASK = 0x01;
  public static final int LED_1_BIT_MASK = 0x02;

  /*
   * map of LED bit masks
   */
  public static final int[] LED_BIT_MASK_MAP = {
    LED_0_BIT_MASK,
    LED_1_BIT_MASK
  };

  /*
   * digital masks used to pull out one specific bit.
   */
  public static final int D0_MASK = 0x1;
  public static final int D1_MASK = 0x2;
  public static final int D2_MASK = 0x4;
  public static final int D3_MASK = 0x8;
  public static final int D4_MASK = 0x10;
  public static final int D5_MASK = 0x20;
  public static final int D6_MASK = 0x40;
  public static final int D7_MASK = 0x80;

  /*
   * map of physical ports to bit mask
   */
  public static final int[] ADDRESS_DIGITAL_BIT_MASK = {
      D0_MASK,
      D1_MASK,
      D2_MASK,
      D3_MASK,
      D4_MASK,
      D5_MASK,
      D6_MASK,
      D7_MASK
  };

  /*
   * map of analog ports to memory addresses
   */
  public static final int[] ADDRESS_ANALOG_PORT_MAP = {
    ADDRESS_ANALOG_PORT_A0,
    ADDRESS_ANALOG_PORT_A1,
    ADDRESS_ANALOG_PORT_A2,
    ADDRESS_ANALOG_PORT_A3,
    ADDRESS_ANALOG_PORT_A4,
    ADDRESS_ANALOG_PORT_A5,
    ADDRESS_ANALOG_PORT_A6,
    ADDRESS_ANALOG_PORT_A7
  };

  /*
   * map of voltage output ports to memory addresses
   */
  public static final int[] ADDRESS_VOLTAGE_OUTPUT_PORT_MAP = {
      ADDRESS_VOLTAGE_OUTPUT_PORT_0,
      ADDRESS_VOLTAGE_OUTPUT_PORT_1
  };

  /*
   * map of pulse output ports to memory addresses
   */
  public static final int[] ADDRESS_PULSE_OUTPUT_PORT_MAP = {
      ADDRESS_PULSE_OUTPUT_PORT_0,
      ADDRESS_PULSE_OUTPUT_PORT_1
  };

  /*
   * map of I2C ports to memory addresses
   */
  public static final int[] ADDRESS_I2C_PORT_MAP = {
    ADDRESS_I2C0,
    ADDRESS_I2C1,
    ADDRESS_I2C2,
    ADDRESS_I2C3,
    ADDRESS_I2C4,
    ADDRESS_I2C5
  };

  /*
   * map of buffer flags
   */
  public static final int[] BUFFER_FLAG_MAP = {
    BUFFER_FLAG_I2C0,
    BUFFER_FLAG_I2C1,
    BUFFER_FLAG_I2C2,
    BUFFER_FLAG_I2C3,
    BUFFER_FLAG_I2C4,
    BUFFER_FLAG_I2C5
  };

  private static final int SEGMENT_KEY_ANALOG_VOLTAGE_OUTPUT_PORT_0 =  0;
  private static final int SEGMENT_KEY_ANALOG_VOLTAGE_OUTPUT_PORT_1 =  1;

  private static final int SEGMENT_KEY_PULSE_OUTPUT_PORT_0 =  2;
  private static final int SEGMENT_KEY_PULSE_OUTPUT_PORT_1 =  3;

  private static final int SEGMENT_KEY_I2C_PORT_0 =  4;
  private static final int SEGMENT_KEY_I2C_PORT_1 =  5;
  private static final int SEGMENT_KEY_I2C_PORT_2 =  6;
  private static final int SEGMENT_KEY_I2C_PORT_3 =  7;
  private static final int SEGMENT_KEY_I2C_PORT_4 =  8;
  private static final int SEGMENT_KEY_I2C_PORT_5 =  9;

  private static final int SEGMENT_KEY_I2C_PORT_0_FLAG_ONLY = 10;
  private static final int SEGMENT_KEY_I2C_PORT_1_FLAG_ONLY = 11;
  private static final int SEGMENT_KEY_I2C_PORT_2_FLAG_ONLY = 12;
  private static final int SEGMENT_KEY_I2C_PORT_3_FLAG_ONLY = 13;
  private static final int SEGMENT_KEY_I2C_PORT_4_FLAG_ONLY = 14;
  private static final int SEGMENT_KEY_I2C_PORT_5_FLAG_ONLY = 15;

  private static final int[] SEGMENT_KEY_ANALOG_VOLTAGE_PORT_MAP = {
      SEGMENT_KEY_ANALOG_VOLTAGE_OUTPUT_PORT_0, SEGMENT_KEY_ANALOG_VOLTAGE_OUTPUT_PORT_1
  };

  private static final int[] SEGMENT_KEY_PULSE_OUTPUT_PORT_MAP = {
      SEGMENT_KEY_PULSE_OUTPUT_PORT_0, SEGMENT_KEY_PULSE_OUTPUT_PORT_1
  };

  private static final int[] SEGMENT_KEY_I2C_PORT_MAP = {
      SEGMENT_KEY_I2C_PORT_0, SEGMENT_KEY_I2C_PORT_1, SEGMENT_KEY_I2C_PORT_2,
      SEGMENT_KEY_I2C_PORT_3, SEGMENT_KEY_I2C_PORT_4, SEGMENT_KEY_I2C_PORT_5
  };

  private static final int[] SEGMENT_KEY_I2C_PORT_FLAG_ONLY_MAP = {
      SEGMENT_KEY_I2C_PORT_0_FLAG_ONLY, SEGMENT_KEY_I2C_PORT_1_FLAG_ONLY, SEGMENT_KEY_I2C_PORT_2_FLAG_ONLY,
      SEGMENT_KEY_I2C_PORT_3_FLAG_ONLY, SEGMENT_KEY_I2C_PORT_4_FLAG_ONLY, SEGMENT_KEY_I2C_PORT_5_FLAG_ONLY
  };

  // tracks which ports have a registered callback
  private final I2cPortReadyCallback[] i2cPortReadyCallback = new I2cPortReadyCallback[NUMBER_OF_PORTS];

  private ReadWriteRunnableSegment[] analogVoltagePortSegments = new ReadWriteRunnableSegment[SEGMENT_KEY_ANALOG_VOLTAGE_PORT_MAP.length];
  private ReadWriteRunnableSegment[] pulseOutputPortSegments = new ReadWriteRunnableSegment[SEGMENT_KEY_PULSE_OUTPUT_PORT_MAP.length];
  private ReadWriteRunnableSegment[] i2cPortSegments = new ReadWriteRunnableSegment[SEGMENT_KEY_I2C_PORT_MAP.length];
  private ReadWriteRunnableSegment[] i2cPortFlagOnlySegments = new ReadWriteRunnableSegment[SEGMENT_KEY_I2C_PORT_FLAG_ONLY_MAP.length];

  protected final byte[] lastI2cPortModes = new byte[NUMBER_OF_PORTS];

  //------------------------------------------------------------------------------------------------
  // Construction
  //------------------------------------------------------------------------------------------------

  /**
   * Use ModernRoboticsUsbDeviceManager to create an instance of this class
   */
  public ModernRoboticsUsbDeviceInterfaceModule(
      final Context context, final SerialNumber serialNumber, OpenRobotUsbDevice openRobotUsbDevice, SyncdDevice.Manager manager)
          throws RobotCoreException, InterruptedException {

    super(NUMBER_OF_PORTS, context, serialNumber, manager, openRobotUsbDevice, new CreateReadWriteRunnable() {
    @Override public ReadWriteRunnable create(RobotUsbDevice device) {
        return new ReadWriteRunnableStandard(context, serialNumber, device, MONITOR_LENGTH, START_ADDRESS, DEBUG_LOGGING);
      }});
  }

  @Override
  protected void doArm() throws RobotCoreException, InterruptedException {
    super.doArm();
    createSegments();
  }

  @Override
  protected void doPretend() throws RobotCoreException, InterruptedException {
    super.doPretend();
    createSegments();
  }

  protected void createSegments() {

    // Segments can't be created until the read-write-runnable is
    for (int i = 0; i < SEGMENT_KEY_ANALOG_VOLTAGE_PORT_MAP.length; i++) {
      analogVoltagePortSegments[i] = readWriteRunnable.createSegment(SEGMENT_KEY_ANALOG_VOLTAGE_PORT_MAP[i], ADDRESS_VOLTAGE_OUTPUT_PORT_MAP[i], ANALOG_VOLTAGE_OUTPUT_BUFFER_SIZE);
    }

    for (int i = 0; i < SEGMENT_KEY_PULSE_OUTPUT_PORT_MAP.length; i++) {
      pulseOutputPortSegments[i] = readWriteRunnable.createSegment(SEGMENT_KEY_PULSE_OUTPUT_PORT_MAP[i], ADDRESS_PULSE_OUTPUT_PORT_MAP[i], PULSE_OUTPUT_BUFFER_SIZE);
    }

    for (int i = 0; i < SEGMENT_KEY_I2C_PORT_MAP.length; i++) {
      i2cPortSegments[i] = readWriteRunnable.createSegment(SEGMENT_KEY_I2C_PORT_MAP[i], ADDRESS_I2C_PORT_MAP[i], I2C_PORT_BUFFER_SIZE);
      i2cPortFlagOnlySegments[i] = readWriteRunnable.createSegment(SEGMENT_KEY_I2C_PORT_FLAG_ONLY_MAP[i], ADDRESS_I2C_PORT_MAP[i] + OFFSET_I2C_PORT_FLAG, 1);
      lastI2cPortModes[i] = 0;  // be deterministic
    }
  }

  @Override
  public void initializeHardware() {
    // nothing to do
  }

  //------------------------------------------------------------------------------------------------
  // Operations
  //------------------------------------------------------------------------------------------------

  @Override public Manufacturer getManufacturer() {
    return Manufacturer.ModernRobotics;
  }

  /**
   * Device Name
   *
   * @return device name
   */
  @Override
  public String getDeviceName() {
    return String.format("%s %s", context.getString(R.string.moduleDisplayNameCDIM), this.robotUsbDevice.getFirmwareVersion());
  }

  @Override
  public String getConnectionInfo() {
    return "USB " + getSerialNumber();
  }

  @Override
  public void resetDeviceConfigurationForOpMode() {
  }


  //***************************************** Analog Channel **************************************//
  @Override
  public double getAnalogInputVoltage(int channel) {
    throwIfAnalogPortIsInvalid(channel);

    // http://modernroboticsinc.com/core-device-interface-module-2
    // "Analog Inputs can read analog sensors like the Optical Distance Sensor which returns a voltage
    // between 0 and 5V to be converted to a 10 bit value between 0 and 1023."
    byte[] buffer = read(ADDRESS_ANALOG_PORT_MAP[channel], SIZE_ANALOG_BUFFER);
    int tenBits = (int) TypeConversion.byteArrayToShort(buffer, ByteOrder.LITTLE_ENDIAN) & 0x3FF;
    return Range.scale(tenBits, 0, 1023, 0.0, getMaxAnalogInputVoltage());
  }

  @Override
  public double getMaxAnalogInputVoltage() {
    return MAX_ANALOG_INPUT_VOLTAGE;
  }

//***************************************** Digital Channel **************************************//
  @Override
  public DigitalChannel.Mode getDigitalChannelMode(int channel) {
    return byteToRunMode(channel, getDigitalIOControlByte());
  }

  @Override
  public void setDigitalChannelMode(int channel, DigitalChannel.Mode mode) {
    int mask = runModeToBitMask(channel, mode);
    int oldMode = readFromWriteCache(ADDRESS_DIGITAL_IO_CONTROL);

    int newMode;
    // relevant bit is 1, the rest are 0s, so OR turns that bit to output mode (1)
    // and is identity otherwise.
    if (mode == DigitalChannel.Mode.OUTPUT) {
      newMode = oldMode | mask;
    } else {
      // relevant bit in the mask is 0, so AND turns that bit to input mode (0)
      // other bits are 1s, so AND is identity.
      newMode = oldMode & mask;
    }
    write8(ADDRESS_DIGITAL_IO_CONTROL, newMode);
  }

  @Override @Deprecated public void setDigitalChannelMode(int channel, Mode mode) {
    setDigitalChannelMode(channel, mode.migrate());
  }

@Override
  public boolean getDigitalChannelState(int channel) {
    int val;
    if (DigitalChannel.Mode.OUTPUT == getDigitalChannelMode(channel)) {
      val = getDigitalOutputStateByte();
    } else {
      val = getDigitalInputStateByte();
    }
    val &= ADDRESS_DIGITAL_BIT_MASK[channel];
    return val > 0;
  }

  @Override
  public void setDigitalChannelState(int channel, boolean state) {
    if (DigitalChannel.Mode.OUTPUT == getDigitalChannelMode(channel)) {
      int outputState = readFromWriteCache(ADDRESS_DIGITAL_OUTPUT_STATE);
      if (state) {
        // OR with that bit flipped on to turn it on.
        outputState |=  ADDRESS_DIGITAL_BIT_MASK[channel];
      } else {
        // AND with that bit set to 0 to turn it off.
        outputState &= ~ADDRESS_DIGITAL_BIT_MASK[channel];
      }
      setDigitalOutputByte((byte) outputState);
    }
  }

  @Override
  public int getDigitalInputStateByte() {
    byte buff = read8(ADDRESS_DIGITAL_INPUT_STATE);
    return TypeConversion.unsignedByteToInt(buff);
  }

  @Override
  public byte getDigitalIOControlByte() {
    return read8(ADDRESS_DIGITAL_IO_CONTROL);
  }

  @Override
  public void setDigitalIOControlByte(byte input) {
    write8(ADDRESS_DIGITAL_IO_CONTROL, input);
  }

  @Override
  public byte getDigitalOutputStateByte() {
    return read8(ADDRESS_DIGITAL_OUTPUT_STATE);
  }

  @Override
  public void setDigitalOutputByte(byte input) {
    write8(ADDRESS_DIGITAL_OUTPUT_STATE, input);
  }

  private int runModeToBitMask(int channel, DigitalChannel.Mode mode) {
    if (mode == DigitalChannel.Mode.OUTPUT) {
      // relevant bit is 1, the rest are 0
      return ADDRESS_DIGITAL_BIT_MASK[channel];
    }
    else {
      // relevant bit is 0, the rest are 1s
      return ~ADDRESS_DIGITAL_BIT_MASK[channel];
    }
  }

  private DigitalChannel.Mode byteToRunMode(int channel, int input) {
    int masked = ADDRESS_DIGITAL_BIT_MASK[channel] & input;
    if (masked > 0) {
      return DigitalChannel.Mode.OUTPUT;
    } else {
      return DigitalChannel.Mode.INPUT;
    }
  }

  //*********************************************** LED ********************************************//
  @Override
  public boolean getLEDState(int channel) {
    throwIfLEDChannelIsInvalid(channel);

    byte buff = read8(ADDRESS_LED_SET);
    int val = (buff & LED_BIT_MASK_MAP[channel]);
    return val > 0;
  }

  @Override
  public void setLED(int channel, boolean set) {
    throwIfLEDChannelIsInvalid(channel);

    int val;
    byte buff = readFromWriteCache(ADDRESS_LED_SET);
    if (set) {
      val = (buff | LED_BIT_MASK_MAP[channel]);

    } else {
      val = (buff & ~LED_BIT_MASK_MAP[channel]);
    }
    write8(ADDRESS_LED_SET, val);
  }

  //****************************************** Analog Output ***************************************//
  @Override
  public void setAnalogOutputVoltage(int port, int voltage) {
    throwIfAnalogOutputPortIsInvalid(port);

    Lock lock = analogVoltagePortSegments[port].getWriteLock();
    byte[] buffer = analogVoltagePortSegments[port].getWriteBuffer();
    byte[] newValue = TypeConversion.shortToByteArray((short) voltage, ByteOrder.LITTLE_ENDIAN);

    try {
      lock.lock();
      System.arraycopy(newValue, 0, buffer, OFFSET_ANALOG_VOLTAGE_OUTPUT_VOLTAGE, newValue.length);
    } finally {
      lock.unlock();
    }

    readWriteRunnable.queueSegmentWrite(SEGMENT_KEY_ANALOG_VOLTAGE_PORT_MAP[port]);
  }

  @Override
  public void setAnalogOutputFrequency(int port, int freq) {
    throwIfAnalogOutputPortIsInvalid(port);

    Lock lock = analogVoltagePortSegments[port].getWriteLock();
    byte[] buffer = analogVoltagePortSegments[port].getWriteBuffer();
    byte[] newValue = TypeConversion.shortToByteArray((short) freq, ByteOrder.LITTLE_ENDIAN);

    try {
      lock.lock();
      System.arraycopy(newValue, 0, buffer, OFFSET_ANALOG_VOLTAGE_OUTPUT_FREQ, newValue.length);
    } finally {
      lock.unlock();
    }

    readWriteRunnable.queueSegmentWrite(SEGMENT_KEY_ANALOG_VOLTAGE_PORT_MAP[port]);
  }

  @Override
  public void setAnalogOutputMode(int port, byte mode) {
    throwIfAnalogOutputPortIsInvalid(port);

    Lock lock = analogVoltagePortSegments[port].getWriteLock();
    byte[] buffer = analogVoltagePortSegments[port].getWriteBuffer();

    try {
      lock.lock();
      buffer[OFFSET_ANALOG_VOLTAGE_OUTPUT_MODE] = mode;
    } finally {
      lock.unlock();
    }

    readWriteRunnable.queueSegmentWrite(SEGMENT_KEY_ANALOG_VOLTAGE_PORT_MAP[port]);
  }

  //*********************************************** PWM ********************************************//
  @Override
  public void setPulseWidthOutputTime(int port, int usDuration) {
    throwIfPulseWidthPortIsInvalid(port);

    Lock lock = pulseOutputPortSegments[port].getWriteLock();
    byte[] buffer = pulseOutputPortSegments[port].getWriteBuffer();
    byte[] newValue = TypeConversion.shortToByteArray((short) usDuration, ByteOrder.LITTLE_ENDIAN);

    try {
      lock.lock();
      System.arraycopy(newValue, 0, buffer, OFFSET_PULSE_OUTPUT_TIME, newValue.length);
    } finally {
      lock.unlock();
    }
    
    readWriteRunnable.queueSegmentWrite(SEGMENT_KEY_PULSE_OUTPUT_PORT_MAP[port]);
  }

  @Override
  public void setPulseWidthPeriod(int port, int usPeriod) {
    throwIfI2cPortIsInvalid(port);

    Lock lock = pulseOutputPortSegments[port].getWriteLock();
    byte[] buffer = pulseOutputPortSegments[port].getWriteBuffer();
    byte[] newValue = TypeConversion.shortToByteArray((short) usPeriod, ByteOrder.LITTLE_ENDIAN);

    try {
      lock.lock();
      System.arraycopy(newValue, 0, buffer, OFFSET_PULSE_OUTPUT_PERIOD, newValue.length);
    } finally {
      lock.unlock();
    }

    readWriteRunnable.queueSegmentWrite(SEGMENT_KEY_PULSE_OUTPUT_PORT_MAP[port]);
  }

  @Override
  public int getPulseWidthOutputTime(int port) {
    throw new UnsupportedOperationException("getPulseWidthOutputTime is not implemented.");
  }

  @Override
  public int getPulseWidthPeriod(int port) {
    throw new UnsupportedOperationException("getPulseWidthOutputTime is not implemented.");
  }

  //*********************************************** I2C ********************************************//


  @Override
  public void enableI2cReadMode(int physicalPort, I2cAddr i2cAddress, int memAddress, int length) {
    throwIfI2cPortIsInvalid(physicalPort);
    throwIfBufferIsTooLarge(length);

    try {
      i2cPortSegments[physicalPort].getWriteLock().lock();
      byte[] buffer = i2cPortSegments[physicalPort].getWriteBuffer();
      lastI2cPortModes[physicalPort] =
      buffer[OFFSET_I2C_PORT_MODE] = I2C_MODE_READ;
      buffer[OFFSET_I2C_PORT_I2C_ADDRESS] = (byte) i2cAddress.get8Bit();
      buffer[OFFSET_I2C_PORT_MEMORY_ADDRESS] = (byte) memAddress;
      buffer[OFFSET_I2C_PORT_MEMORY_LENGTH] = (byte) length;
    } finally {
      i2cPortSegments[physicalPort].getWriteLock().unlock();
    }
  }

  @Override
  public void enableI2cWriteMode(int physicalPort, I2cAddr i2cAddress, int memAddress, int length) {
    throwIfI2cPortIsInvalid(physicalPort);
    throwIfBufferIsTooLarge(length);

    try {
      i2cPortSegments[physicalPort].getWriteLock().lock();
      byte[] buffer = i2cPortSegments[physicalPort].getWriteBuffer();
      lastI2cPortModes[physicalPort] =
      buffer[OFFSET_I2C_PORT_MODE] = I2C_MODE_WRITE;
      buffer[OFFSET_I2C_PORT_I2C_ADDRESS] = (byte) i2cAddress.get8Bit();
      buffer[OFFSET_I2C_PORT_MEMORY_ADDRESS] = (byte) memAddress;
      buffer[OFFSET_I2C_PORT_MEMORY_LENGTH] = (byte) length;
    } finally {
      i2cPortSegments[physicalPort].getWriteLock().unlock();
    }
  }

  @Override
  public byte[] getCopyOfReadBuffer(int physicalPort) {
    throwIfI2cPortIsInvalid(physicalPort);

    byte[] bufferCopy = null;
    try {
      i2cPortSegments[physicalPort].getReadLock().lock();
      byte[] buffer = i2cPortSegments[physicalPort].getReadBuffer();
      int length = buffer[OFFSET_I2C_PORT_MEMORY_LENGTH];
      bufferCopy = new byte[length];
      System.arraycopy(buffer, OFFSET_I2C_PORT_MEMORY_BUFFER, bufferCopy, 0, bufferCopy.length);
    } finally {
      i2cPortSegments[physicalPort].getReadLock().unlock();
    }
    return bufferCopy;
  }

  @Override
  public byte[] getCopyOfWriteBuffer(int physicalPort) {
    throwIfI2cPortIsInvalid(physicalPort);

    byte[] bufferCopy = null;
    try {
      i2cPortSegments[physicalPort].getWriteLock().lock();
      byte[] buffer = i2cPortSegments[physicalPort].getWriteBuffer();
      int length = buffer[OFFSET_I2C_PORT_MEMORY_LENGTH];
      bufferCopy = new byte[length];
      System.arraycopy(buffer, OFFSET_I2C_PORT_MEMORY_BUFFER, bufferCopy, 0, bufferCopy.length);
    } finally {
      i2cPortSegments[physicalPort].getWriteLock().unlock();
    }
    return bufferCopy;
  }

  @Override
  public void copyBufferIntoWriteBuffer(int physicalPort, byte[] buffer) {
    throwIfI2cPortIsInvalid(physicalPort);
    throwIfBufferIsTooLarge(buffer.length);

    try {
      i2cPortSegments[physicalPort].getWriteLock().lock();
      byte[] writeBuffer = i2cPortSegments[physicalPort].getWriteBuffer();
      System.arraycopy(buffer, 0, writeBuffer,OFFSET_I2C_PORT_MEMORY_BUFFER, buffer.length);
    } finally {
      i2cPortSegments[physicalPort].getWriteLock().unlock();
    }
  }

  @Override
  public void setI2cPortActionFlag(int port) {
    throwIfI2cPortIsInvalid(port);

    try {
      i2cPortSegments[port].getWriteLock().lock();
      byte[] buffer = i2cPortSegments[port].getWriteBuffer();
      buffer[OFFSET_I2C_PORT_FLAG] = I2C_ACTION_FLAG;
    } finally {
      i2cPortSegments[port].getWriteLock().unlock();
    }
  }

  @Override
  public void clearI2cPortActionFlag(int port) {
    throwIfI2cPortIsInvalid(port);

    try {
      i2cPortSegments[port].getWriteLock().lock();
      byte[] buffer = i2cPortSegments[port].getWriteBuffer();
      buffer[OFFSET_I2C_PORT_FLAG] = I2C_NO_ACTION_FLAG;
    } finally {
      i2cPortSegments[port].getWriteLock().unlock();
    }
  }

  @Override
  public boolean isI2cPortActionFlagSet(int port) {
    throwIfI2cPortIsInvalid(port);
    boolean isSet = false;
    try {
      i2cPortSegments[port].getReadLock().lock();
      byte[] buffer = i2cPortSegments[port].getReadBuffer();
      isSet = buffer[OFFSET_I2C_PORT_FLAG] == I2C_ACTION_FLAG;
    } finally {
      i2cPortSegments[port].getReadLock().unlock();
    }
    return isSet;
  }

  @Override
  public void readI2cCacheFromController(int port) {
    throwIfI2cPortIsInvalid(port);

    readWriteRunnable.queueSegmentRead(SEGMENT_KEY_I2C_PORT_MAP[port]);
  }

  @Override
  public void writeI2cCacheToController(int port) {
    throwIfI2cPortIsInvalid(port);

    readWriteRunnable.queueSegmentWrite(SEGMENT_KEY_I2C_PORT_MAP[port]);
  }

  @Override
  public void writeI2cPortFlagOnlyToController(int port) {
    throwIfI2cPortIsInvalid(port);

    ReadWriteRunnableSegment full = i2cPortSegments[port];
    ReadWriteRunnableSegment flagOnly = i2cPortFlagOnlySegments[port];

    try {
      full.getWriteLock().lock();
      flagOnly.getWriteLock().lock();
      flagOnly.getWriteBuffer()[0] = full.getWriteBuffer()[OFFSET_I2C_PORT_FLAG];
    } finally {
      full.getWriteLock().unlock();
      flagOnly.getWriteLock().unlock();
    }

    readWriteRunnable.queueSegmentWrite(SEGMENT_KEY_I2C_PORT_FLAG_ONLY_MAP[port]);
  }

  @Override
  public boolean isI2cPortInReadMode(int port) {
  // If we're not actually armed, then we report that we are in whatever mode it was
  // was last requested that we be in
    throwIfI2cPortIsInvalid(port);

    boolean inReadMode = false;

    try {
      i2cPortSegments[port].getReadLock().lock();
      byte[] buffer = i2cPortSegments[port].getReadBuffer();
      inReadMode = ((isArmed()
              ? buffer[OFFSET_I2C_PORT_MODE]
              : lastI2cPortModes[port])
            == I2C_MODE_READ);
    } finally {
      i2cPortSegments[port].getReadLock().unlock();
    }

    return inReadMode;
  }

  @Override
  public boolean isI2cPortInWriteMode(int port) {
  // If we're not actually armed, then we report that we are in whatever mode it was
  // was last requested that we be in
    throwIfI2cPortIsInvalid(port);

    boolean inWriteMode = false;

    try {
      i2cPortSegments[port].getReadLock().lock();
      byte[] buffer = i2cPortSegments[port].getReadBuffer();
      inWriteMode = ((isArmed()
              ? buffer[OFFSET_I2C_PORT_MODE]
              : lastI2cPortModes[port])
            == (I2C_MODE_WRITE));
    } finally {
      i2cPortSegments[port].getReadLock().unlock();
    }

    return inWriteMode;
  }

  @Override
  public boolean isI2cPortReady(int port) {
    return isI2cPortReady(port, read8(ADDRESS_BUFFER_STATUS));
  }

  @Override
  public Lock getI2cReadCacheLock(int port) {
    return i2cPortSegments[port].getReadLock();
  }

  @Override
  public Lock getI2cWriteCacheLock(int port) {
    return i2cPortSegments[port].getWriteLock();
  }

  @Override
  public byte[] getI2cReadCache(int port) {
    return i2cPortSegments[port].getReadBuffer();
  }

  @Override public TimeWindow getI2cReadCacheTimeWindow(int port) {
    return i2cPortSegments[port].getTimeWindow();
  }

  @Override
  public byte[] getI2cWriteCache(int port) {
    return i2cPortSegments[port].getWriteBuffer();
  }

  @Override public int getMaxI2cWriteLatency(int port) {
    // This is a pessimistic estimate. We can probably do better if we get more information
    // from Modern Robotics.
    return 20;  // a semi-reasoned guess
  }

  @Override
  public void registerForI2cPortReadyCallback(I2cPortReadyCallback callback, int port) {
    throwIfI2cPortIsInvalid(port);
    i2cPortReadyCallback[port] = callback;
  }

  @Override
  public I2cPortReadyCallback getI2cPortReadyCallback(int port) {
    throwIfI2cPortIsInvalid(port);
    return i2cPortReadyCallback[port];
  }

  @Override
  public void deregisterForPortReadyCallback(int port) {
    throwIfI2cPortIsInvalid(port);
    i2cPortReadyCallback[port] = null;
  }

  @Deprecated
  @Override
  public void readI2cCacheFromModule(int port) {
    readI2cCacheFromController(port);
  }

  @Deprecated
  @Override
  public void writeI2cCacheToModule(int port) {
    writeI2cCacheToController(port);
  }

  @Deprecated
  @Override
  public void writeI2cPortFlagOnlyToModule(int port) {
    writeI2cPortFlagOnlyToController(port);
  }

  //********************************************* OTHER ********************************************//

  private void throwIfLEDChannelIsInvalid(int port) {
    if (port != 0 && port != 1) {
      throw new IllegalArgumentException(String.format("port %d is invalid; valid ports are 0 and 1.", port));
    }
  }

  private void throwIfAnalogOutputPortIsInvalid(int port) {
    if (port != 0 && port != 1) {
      throw new IllegalArgumentException(String.format("port %d is invalid; valid ports are 0 and 1.", port));
    }
  }

  private void throwIfPulseWidthPortIsInvalid(int port) {
    if (port != 0 && port != 1) {
      throw new IllegalArgumentException(String.format("port %d is invalid; valid ports are 0 and 1.", port));
    }
  }

  private void throwIfAnalogPortIsInvalid(int port) {
    if (port < MIN_ANALOG_PORT_NUMBER || port > MAX_ANALOG_PORT_NUMBER) {
      throw new IllegalArgumentException(
          String.format("port %d is invalid; valid ports are %d..%d", port, MIN_ANALOG_PORT_NUMBER, MAX_ANALOG_PORT_NUMBER));
    }
  }

  private void throwIfDigitalLineIsInvalid(int line) {
    if (line != 0 && line != 1) {
      throw new IllegalArgumentException("line is invalid, valid lines are 0 and 1");
    }
  }

  private void throwIfBufferIsTooLarge(int length) {
    if (length > SIZE_I2C_BUFFER) {
      throw new IllegalArgumentException(
          String.format("buffer is too large (%d byte), max size is %d bytes", length, SIZE_I2C_BUFFER));
    }
  }

  private boolean isI2cPortReady(int port, byte flags) {
  // if we're not armed, then we always report as ready so that callbacks will proceed
    if (isArmed()) {
      return (flags & BUFFER_FLAG_MAP[port]) == 0;
    } else {
      return true;
    }
  }

  @Override
  public void readComplete() throws InterruptedException {
    // since this method is a callback, it might be called before
    // the class is fully initialized
    if (i2cPortReadyCallback == null) return;

    byte bufferStatusByte = read8(ADDRESS_BUFFER_STATUS);
    for (int i = 0; i < NUMBER_OF_PORTS; i++) {
      // callback devices waiting for a port to be ready
      if (i2cPortReadyCallback[i] != null && isI2cPortReady(i, bufferStatusByte)) {
        i2cPortReadyCallback[i].portIsReady(i);
      }
    }
  }

  //------------------------------------------------------------------------------------------------
  // Utility
  //------------------------------------------------------------------------------------------------

  public static final int MAX_NEW_I2C_ADDRESS = 0x7e;
  public static final int MIN_NEW_I2C_ADDRESS = 0x10;

  public static void throwIfModernRoboticsI2cAddressIsInvalid(I2cAddr newAddress) {
    // New I2C address must be in the proper range, and must be even.
    if (newAddress.get8Bit() < MIN_NEW_I2C_ADDRESS || newAddress.get8Bit() > MAX_NEW_I2C_ADDRESS) {
      throw new IllegalArgumentException(
              String.format("New I2C address %d is invalid; valid range is: %d..%d", newAddress.get8Bit(),
                      MIN_NEW_I2C_ADDRESS,
                      MAX_NEW_I2C_ADDRESS));
    }
  }
}
