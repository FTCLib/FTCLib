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
import com.qualcomm.robotcore.hardware.I2cAddr;
import com.qualcomm.robotcore.hardware.LegacyModule;
import com.qualcomm.robotcore.hardware.configuration.ModernRoboticsConstants;
import com.qualcomm.robotcore.hardware.usb.RobotUsbDevice;
import com.qualcomm.robotcore.util.Range;
import com.qualcomm.robotcore.util.SerialNumber;
import com.qualcomm.robotcore.util.TypeConversion;

import org.firstinspires.ftc.robotcore.internal.hardware.TimeWindow;

import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.concurrent.locks.Lock;

/**
 * Modern Robotics USB Legacy Module
 *
 * Use {@link HardwareDeviceManager} to create an instance of this class
 */
public class ModernRoboticsUsbLegacyModule extends ModernRoboticsUsbI2cController implements LegacyModule {

  //------------------------------------------------------------------------------------------------
  // State
  //------------------------------------------------------------------------------------------------

  public final static String TAG = "MRLegacyModule";
  @Override protected String getTag() { return TAG; }

  /** The maximum voltage that can be read by our ADC */
  public static final double MAX_ANALOG_INPUT_VOLTAGE = 5.0;

  /**
   * Enable DEBUG_LOGGING logging
   */
  public static final boolean DEBUG_LOGGING = false;

  /*
   * const values used by this class
   */
  public static final int MONITOR_LENGTH  = 0x0d;
  public static final byte START_ADDRESS = 0x03;

  public static final byte NUMBER_OF_PORTS = ModernRoboticsConstants.NUMBER_OF_LEGACY_MODULE_PORTS;
  public static final byte I2C_ACTION_FLAG = (byte) 0xff;
  public static final byte I2C_NO_ACTION_FLAG = (byte) 0x00;
  public static final byte SIZE_ANALOG_BUFFER = 2;
  public static final byte SIZE_I2C_BUFFER = 27;
  public static final byte SIZE_OF_PORT_BUFFER = 32;

  /*
   * const values used by legacy module
   */
  public static final byte NXT_MODE_ANALOG     = (byte) 0x00;
  public static final byte NXT_MODE_I2C        = (byte) 0x01;
  public static final byte NXT_MODE_9V_ENABLED = (byte) 0x02;
  public static final byte NXT_MODE_DIGITAL_0  = (byte) 0x04;
  public static final byte NXT_MODE_DIGITAL_1  = (byte) 0x08;
  public static final byte NXT_MODE_READ       = (byte) 0x80;
  public static final byte NXT_MODE_WRITE      = (byte) 0x00;

  public static final byte BUFFER_FLAG_S0     = (byte) 0x01;
  public static final byte BUFFER_FLAG_S1     = (byte) 0x02;
  public static final byte BUFFER_FLAG_S2     = (byte) 0x04;
  public static final byte BUFFER_FLAG_S3     = (byte) 0x08;
  public static final byte BUFFER_FLAG_S4     = (byte) 0x10;
  public static final byte BUFFER_FLAG_S5     = (byte) 0x20;

  /*
   * memory addresses used by controller
   */
  public static final int ADDRESS_BUFFER_STATUS  = 0x03;
  public static final int ADDRESS_ANALOG_PORT_S0 = 0x04;
  public static final int ADDRESS_ANALOG_PORT_S1 = 0x06;
  public static final int ADDRESS_ANALOG_PORT_S2 = 0x08;
  public static final int ADDRESS_ANALOG_PORT_S3 = 0x0a;
  public static final int ADDRESS_ANALOG_PORT_S4 = 0x0c;
  public static final int ADDRESS_ANALOG_PORT_S5 = 0x0e;
  public static final int ADDRESS_I2C_PORT_SO    = 0x10;
  public static final int ADDRESS_I2C_PORT_S1    = 0x30;
  public static final int ADDRESS_I2C_PORT_S2    = 0x50;
  public static final int ADDRESS_I2C_PORT_S3    = 0x70;
  public static final int ADDRESS_I2C_PORT_S4    = 0x90;
  public static final int ADDRESS_I2C_PORT_S5    = 0xb0;

  /*
   * memory offsets used by this controller
   */
  public static final byte OFFSET_I2C_PORT_MODE = 0x0;
  public static final byte OFFSET_I2C_PORT_I2C_ADDRESS = 0x1;
  public static final byte OFFSET_I2C_PORT_MEMORY_ADDRESS = 0x2;
  public static final byte OFFSET_I2C_PORT_MEMORY_LENGTH = 0x3;
  public static final byte OFFSET_I2C_PORT_MEMORY_BUFFER = 0x4;
  public static final byte OFFSET_I2C_PORT_FLAG = 0x1f;

  /*
   * map of analog ports to memory addresses
   */
  public static final int[] ADDRESS_ANALOG_PORT_MAP = {
    ADDRESS_ANALOG_PORT_S0,
    ADDRESS_ANALOG_PORT_S1,
    ADDRESS_ANALOG_PORT_S2,
    ADDRESS_ANALOG_PORT_S3,
    ADDRESS_ANALOG_PORT_S4,
    ADDRESS_ANALOG_PORT_S5
  };

  /*
 * map of NXT ports to memory addresses
 */
  public static final int[] ADDRESS_I2C_PORT_MAP = {
      ADDRESS_I2C_PORT_SO,
      ADDRESS_I2C_PORT_S1,
      ADDRESS_I2C_PORT_S2,
      ADDRESS_I2C_PORT_S3,
      ADDRESS_I2C_PORT_S4,
      ADDRESS_I2C_PORT_S5
  };

  /*
   * map of buffer flags
   */
  public static final int[] BUFFER_FLAG_MAP = {
    BUFFER_FLAG_S0,
    BUFFER_FLAG_S1,
    BUFFER_FLAG_S2,
    BUFFER_FLAG_S3,
    BUFFER_FLAG_S4,
    BUFFER_FLAG_S5
  };

  /*
   * map of digital lines
   */
  public static final int[] DIGITAL_LINE = {
    NXT_MODE_DIGITAL_0,
    NXT_MODE_DIGITAL_1
  };

  /*
   * list of ports that can enable 9v, sorted least to greatest
   */
  public static final int[] PORT_9V_CAPABLE = {
    4,
    5
  };

  // segment where only the action flag is written
  private static final int SEGMENT_OFFSET_PORT_FLAG_ONLY = NUMBER_OF_PORTS;

  // do not perform I2C writes slower than this
  private static final double MIN_I2C_WRITE_RATE = 2.000; // in seconds

  // The legacy module runs on a 25ms loop. We need to make sure that on
  // average we do not send commands to it at a faster rate. Since it takes
  // about 10ms to send a command, adding 15ms delay should do it.
  private static final int LEGACY_MODULE_FORCE_IO_DELAY = 15; // in milliseconds

  private final ReadWriteRunnableSegment[] segments = new ReadWriteRunnableSegment[NUMBER_OF_PORTS + SEGMENT_OFFSET_PORT_FLAG_ONLY];

  // tracks which ports have a registered callback
  private final I2cPortReadyCallback[] portReadyCallback = new I2cPortReadyCallback[NUMBER_OF_PORTS];
  protected final byte[]               lastI2cPortModes  = new byte[NUMBER_OF_PORTS];

  //------------------------------------------------------------------------------------------------
  // Construction
  //------------------------------------------------------------------------------------------------

  /**
   * Use ModernRoboticsUsbDeviceManager to create an instance of this class
   */
  public ModernRoboticsUsbLegacyModule(final Context context, final SerialNumber serialNumber, OpenRobotUsbDevice openRobotUsbDevice, SyncdDevice.Manager manager)
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

    // Segments can't be created until the ReadWriteRunnable is
    for (int i = 0; i < NUMBER_OF_PORTS; i++) {
      // create regular segment
      segments[i] = readWriteRunnable.createSegment(i, ADDRESS_I2C_PORT_MAP[i], SIZE_OF_PORT_BUFFER);

      // create segment that is port flag only
      segments[i + SEGMENT_OFFSET_PORT_FLAG_ONLY] = readWriteRunnable.createSegment(
          i + SEGMENT_OFFSET_PORT_FLAG_ONLY, ADDRESS_I2C_PORT_MAP[i]  + OFFSET_I2C_PORT_FLAG, 1);
    }
  }

  @Override
  public void initializeHardware() {
    for (int i = 0; i < NUMBER_OF_PORTS; i++) {
      // set segment to analog read mode (this will be over written as needed)
      enableAnalogReadMode(i);
      readWriteRunnable.queueSegmentWrite(i);
    }
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
   return String.format("%s %s", context.getString(R.string.moduleDisplayNameLegacyModule), this.robotUsbDevice.getFirmwareVersion());
  }

  @Override
  public String getConnectionInfo() {
    return "USB " + getSerialNumber();
  }

  @Override
  public void resetDeviceConfigurationForOpMode() {
  }

  @Override public int getMaxI2cWriteLatency(int port) {
    // This is a pessimistic estimate. We can probably do better if we get more information
    // from Modern Robotics.
    return 60;  // a total wild ass guess
  }

  /**
   * Register to be notified when a given port is ready.
   *
   * The callback method will be called after the latest data has been read from the Legacy Module.
   *
   * Only one callback can be registered for a given port. Last to register wins.
   *
   * @param callback register a callback
   * @param port port to be monitored
   */
  @Override
  public void registerForI2cPortReadyCallback(I2cPortReadyCallback callback, int port) {
    throwIfI2cPortIsInvalid(port);
    portReadyCallback[port] = callback;
  }

  @Override
  public I2cPortReadyCallback getI2cPortReadyCallback(int port) {
    throwIfI2cPortIsInvalid(port);
    return portReadyCallback[port];
  }

  @Override
  public void deregisterForPortReadyCallback(int port) {
    throwIfI2cPortIsInvalid(port);
    portReadyCallback[port] = null;
  }

  @Override
  public void enableI2cReadMode(int physicalPort, I2cAddr i2cAddress, int memAddress, int length) {
    throwIfI2cPortIsInvalid(physicalPort);
    throwIfBufferLengthIsInvalid(length);

    try {
      segments[physicalPort].getWriteLock().lock();
      byte[] buffer = segments[physicalPort].getWriteBuffer();
      lastI2cPortModes[physicalPort] =
      buffer[OFFSET_I2C_PORT_MODE] = NXT_MODE_READ | NXT_MODE_I2C;
      buffer[OFFSET_I2C_PORT_I2C_ADDRESS] = (byte) i2cAddress.get8Bit();
      buffer[OFFSET_I2C_PORT_MEMORY_ADDRESS] = (byte) memAddress;
      buffer[OFFSET_I2C_PORT_MEMORY_LENGTH] = (byte) length;
    } finally {
      segments[physicalPort].getWriteLock().unlock();
    }
  }

  @Override
  public void enableI2cWriteMode(int physicalPort, I2cAddr i2cAddress, int memAddress, int length) {
    throwIfI2cPortIsInvalid(physicalPort);
    throwIfBufferLengthIsInvalid(length);

    try {
      segments[physicalPort].getWriteLock().lock();
      byte[] buffer = segments[physicalPort].getWriteBuffer();
      lastI2cPortModes[physicalPort] =
      buffer[OFFSET_I2C_PORT_MODE] = NXT_MODE_WRITE | NXT_MODE_I2C;
      buffer[OFFSET_I2C_PORT_I2C_ADDRESS] = (byte) i2cAddress.get8Bit();
      buffer[OFFSET_I2C_PORT_MEMORY_ADDRESS] = (byte) memAddress;
      buffer[OFFSET_I2C_PORT_MEMORY_LENGTH] = (byte) length;
    } finally {
      segments[physicalPort].getWriteLock().unlock();
    }
  }

  @Override
  public void enableAnalogReadMode(int physicalPort) {
    throwIfI2cPortIsInvalid(physicalPort);

    try {
      segments[physicalPort].getWriteLock().lock();
      byte[] buffer = segments[physicalPort].getWriteBuffer();
      lastI2cPortModes[physicalPort] =
      buffer[OFFSET_I2C_PORT_MODE] = NXT_MODE_ANALOG;
    } finally {
      segments[physicalPort].getWriteLock().unlock();
    }
    writeI2cCacheToController(physicalPort);
  }

  @Override
  public void enable9v(int physicalPort, boolean enable) {
    if (Arrays.binarySearch(PORT_9V_CAPABLE, physicalPort) < 0) {
      throw new IllegalArgumentException("9v is only available on the following ports: " + Arrays.toString(PORT_9V_CAPABLE));
    }

    try {
      segments[physicalPort].getWriteLock().lock();
      byte mode = segments[physicalPort].getWriteBuffer()[OFFSET_I2C_PORT_MODE];
      if (enable) {
        mode = (byte) (mode | NXT_MODE_9V_ENABLED);
      } else {
        mode = (byte) (mode & ~NXT_MODE_9V_ENABLED);
      }
      segments[physicalPort].getWriteBuffer()[OFFSET_I2C_PORT_MODE] = mode;
    } finally {
      segments[physicalPort].getWriteLock().unlock();
    }
    writeI2cCacheToController(physicalPort);
  }

  public void setReadMode(int physicalPort, int i2cAddr, int memAddr, int memLen) {
    throwIfI2cPortIsInvalid(physicalPort);

    try {
      segments[physicalPort].getWriteLock().lock();
      byte[] buffer = segments[physicalPort].getWriteBuffer();
      lastI2cPortModes[physicalPort] =
      buffer[OFFSET_I2C_PORT_MODE] = NXT_MODE_READ | NXT_MODE_I2C;
      buffer[OFFSET_I2C_PORT_I2C_ADDRESS] = (byte) i2cAddr;
      buffer[OFFSET_I2C_PORT_MEMORY_ADDRESS] = (byte)memAddr;
      buffer[OFFSET_I2C_PORT_MEMORY_LENGTH] = (byte)memLen;
    } finally {
      segments[physicalPort].getWriteLock().unlock();
    }
  }

  public void setWriteMode(int physicalPort, int i2cAddress, int memAddress) {
    throwIfI2cPortIsInvalid(physicalPort);

    try {
      segments[physicalPort].getWriteLock().lock();
      byte[] buffer = segments[physicalPort].getWriteBuffer();
      lastI2cPortModes[physicalPort] =
      buffer[OFFSET_I2C_PORT_MODE] = NXT_MODE_WRITE | NXT_MODE_I2C;
      buffer[OFFSET_I2C_PORT_I2C_ADDRESS] = (byte) i2cAddress;
      buffer[OFFSET_I2C_PORT_MEMORY_ADDRESS] = (byte)memAddress;
    } finally {
      segments[physicalPort].getWriteLock().unlock();
    }
  }

  public void setData(int physicalPort, byte[] data, int length) {
    throwIfI2cPortIsInvalid(physicalPort);
    throwIfBufferLengthIsInvalid(length);

    try {
      segments[physicalPort].getWriteLock().lock();
      byte[] buffer = segments[physicalPort].getWriteBuffer();
      System.arraycopy(data, 0, buffer, OFFSET_I2C_PORT_MEMORY_BUFFER, length);
      buffer[OFFSET_I2C_PORT_MEMORY_LENGTH] = (byte)length;
    } finally {
      segments[physicalPort].getWriteLock().unlock();
    }
  }

  @Override
  public void setDigitalLine(int physicalPort, int line, boolean set) {
    throwIfI2cPortIsInvalid(physicalPort);
    throwIfDigitalLineIsInvalid(line);

    try {
      segments[physicalPort].getWriteLock().lock();
      byte mode = segments[physicalPort].getWriteBuffer()[OFFSET_I2C_PORT_MODE];

      if (set) {
        mode = (byte) (mode | DIGITAL_LINE[line]);
      } else {
        mode = (byte) (mode & ~DIGITAL_LINE[line]);
      }

      segments[physicalPort].getWriteBuffer()[OFFSET_I2C_PORT_MODE] = mode;
    } finally {
      segments[physicalPort].getWriteLock().unlock();
    }
    writeI2cCacheToController(physicalPort);
  }

  @Override
  public byte[] readAnalogRaw(int physicalPort) {
    throwIfI2cPortIsInvalid(physicalPort);
    return read(ADDRESS_ANALOG_PORT_MAP[physicalPort], SIZE_ANALOG_BUFFER);
  }

  @Override
  public double readAnalogVoltage(int physicalPort) {
    byte[] rawBytes = readAnalogRaw(physicalPort);
    int tenBits = TypeConversion.byteArrayToShort(rawBytes, ByteOrder.LITTLE_ENDIAN) & 0x3FF;
    return Range.scale(tenBits, 0, 1023, 0.0, getMaxAnalogInputVoltage());
  }

  @Override
  public double getMaxAnalogInputVoltage() {
    return MAX_ANALOG_INPUT_VOLTAGE;
  }

  @Override
  public byte[] getCopyOfReadBuffer(int physicalPort) {
    throwIfI2cPortIsInvalid(physicalPort);

    byte[] bufferCopy = null;
    try {
      segments[physicalPort].getReadLock().lock();
      byte[] buffer = segments[physicalPort].getReadBuffer();
      int length = buffer[OFFSET_I2C_PORT_MEMORY_LENGTH];
      bufferCopy = new byte[length];
      System.arraycopy(buffer, OFFSET_I2C_PORT_MEMORY_BUFFER, bufferCopy, 0, bufferCopy.length);
    } finally {
      segments[physicalPort].getReadLock().unlock();
    }
    return bufferCopy;
  }

  @Override
  public byte[] getCopyOfWriteBuffer(int physicalPort) {
    throwIfI2cPortIsInvalid(physicalPort);

    byte[] bufferCopy = null;
    try {
      segments[physicalPort].getWriteLock().lock();
      byte[] buffer = segments[physicalPort].getWriteBuffer();
      int length = buffer[OFFSET_I2C_PORT_MEMORY_LENGTH];
      bufferCopy = new byte[length];
      System.arraycopy(buffer, OFFSET_I2C_PORT_MEMORY_BUFFER, bufferCopy, 0, bufferCopy.length);
    } finally {
      segments[physicalPort].getWriteLock().unlock();
    }
    return bufferCopy;
  }

  @Override
  public void copyBufferIntoWriteBuffer(int physicalPort, byte[] buffer) {
    throwIfI2cPortIsInvalid(physicalPort);
    throwIfBufferLengthIsInvalid(buffer.length);

    try {
      segments[physicalPort].getWriteLock().lock();
      byte[] writeBuffer = segments[physicalPort].getWriteBuffer();
      System.arraycopy(buffer, 0, writeBuffer,OFFSET_I2C_PORT_MEMORY_BUFFER, buffer.length);
    } finally {
      segments[physicalPort].getWriteLock().unlock();
    }
  }

  @Override
  public void setI2cPortActionFlag(int physicalPort) {
    throwIfI2cPortIsInvalid(physicalPort);

    try {
      segments[physicalPort].getWriteLock().lock();
      byte[] buffer = segments[physicalPort].getWriteBuffer();
      buffer[OFFSET_I2C_PORT_FLAG] = I2C_ACTION_FLAG;
    } finally {
      segments[physicalPort].getWriteLock().unlock();
    }
  }

  @Override
  public void clearI2cPortActionFlag(int physicalPort) {
    throwIfI2cPortIsInvalid(physicalPort);

    try {
      segments[physicalPort].getWriteLock().lock();
      byte[] buffer = segments[physicalPort].getWriteBuffer();
      buffer[OFFSET_I2C_PORT_FLAG] = I2C_NO_ACTION_FLAG;
    } finally {
      segments[physicalPort].getWriteLock().unlock();
    }
  }

  @Override
  public boolean isI2cPortActionFlagSet(int physicalPort) {
    throwIfI2cPortIsInvalid(physicalPort);
    boolean isSet = false;
    try {
      segments[physicalPort].getReadLock().lock();
      byte[] buffer = segments[physicalPort].getReadBuffer();
      isSet = buffer[OFFSET_I2C_PORT_FLAG] == I2C_ACTION_FLAG;
    } finally {
      segments[physicalPort].getReadLock().unlock();
    }
    return isSet;
  }

  @Override
  public void readI2cCacheFromController(int physicalPort) {
    throwIfI2cPortIsInvalid(physicalPort);

    readWriteRunnable.queueSegmentRead(physicalPort);
  }

  @Override
  public void writeI2cCacheToController(int physicalPort) {
    throwIfI2cPortIsInvalid(physicalPort);

    readWriteRunnable.queueSegmentWrite(physicalPort);
  }

  @Override
  public void writeI2cPortFlagOnlyToController(int physicalPort) {
    throwIfI2cPortIsInvalid(physicalPort);

    ReadWriteRunnableSegment full = segments[physicalPort];
    ReadWriteRunnableSegment flagOnly = segments[physicalPort + SEGMENT_OFFSET_PORT_FLAG_ONLY];

    try {
      full.getWriteLock().lock();
      flagOnly.getWriteLock().lock();
      flagOnly.getWriteBuffer()[0] = full.getWriteBuffer()[OFFSET_I2C_PORT_FLAG];
    } finally {
      full.getWriteLock().unlock();
      flagOnly.getWriteLock().unlock();
    }

    readWriteRunnable.queueSegmentWrite(physicalPort + SEGMENT_OFFSET_PORT_FLAG_ONLY);
  }

  public boolean isI2cPortInReadMode(int physicalPort) {
  // If we're not actually armed, then we report that we are in whatever mode it was
  // was last requested that we be in
    throwIfI2cPortIsInvalid(physicalPort);

    boolean inReadMode = false;

    try {
      segments[physicalPort].getReadLock().lock();
      byte[] buffer = segments[physicalPort].getReadBuffer();
      inReadMode = (isArmed()
                ? buffer[OFFSET_I2C_PORT_MODE]
                : lastI2cPortModes[physicalPort])
              == (NXT_MODE_READ | NXT_MODE_I2C);
    } finally {
      segments[physicalPort].getReadLock().unlock();
    }

    return inReadMode;
  }

  public boolean isI2cPortInWriteMode(int physicalPort) {
  // If we're not actually armed, then we report that we are in whatever mode it was
  // was last requested that we be in
    throwIfI2cPortIsInvalid(physicalPort);

    boolean inWriteMode = false;

    try {
      segments[physicalPort].getReadLock().lock();
      byte[] buffer = segments[physicalPort].getReadBuffer();
      inWriteMode = (isArmed()
                ? buffer[OFFSET_I2C_PORT_MODE]
                : lastI2cPortModes[physicalPort])
              == (NXT_MODE_WRITE | NXT_MODE_I2C);
    } finally {
      segments[physicalPort].getReadLock().unlock();
    }

    return inWriteMode;
  }

  @Override
  public boolean isI2cPortReady(int physicalPort) {
    byte bufferStatusByte = read8(ADDRESS_BUFFER_STATUS);
    return isPortReady(physicalPort, bufferStatusByte);
  }

  private void throwIfBufferLengthIsInvalid(int length) {
    if (length < 0 || length > SIZE_I2C_BUFFER) {
      throw new IllegalArgumentException(
        String.format("buffer length of %d is invalid; max value is %d", length, SIZE_I2C_BUFFER)
      );
    }
  }

  private void throwIfDigitalLineIsInvalid(int line) {
    if (line != 0 && line != 1) {
      throw new IllegalArgumentException("line is invalid, valid lines are 0 and 1");
    }
  }


  @Override
  public void readComplete() throws InterruptedException {
    // since this method is a callback, it might be called before
    // the class is fully initialized
    if (portReadyCallback == null) return;

    byte bufferStatusByte = read8(ADDRESS_BUFFER_STATUS);
    for (int i = 0; i < NUMBER_OF_PORTS; i++) {
      // callback devices waiting for a port to be ready
      if (portReadyCallback[i] != null && isPortReady(i, bufferStatusByte)) {
        portReadyCallback[i].portIsReady(i);
      }
    }
  }

  private boolean isPortReady(int physicalPort, byte bufferStatusByte) {
  // If we're not actually armed, then we're *always* ready so that callbacks will proceed
    if (this.isArmed()) {
    return (bufferStatusByte & BUFFER_FLAG_MAP[physicalPort]) == 0;
    } else {
      return true;  // make up the most reasonable answer
    }
  }

  @Override
  public Lock getI2cReadCacheLock(int physicalPort) {
    throwIfI2cPortIsInvalid(physicalPort);

    return segments[physicalPort].getReadLock();
  }

  @Override
  public Lock getI2cWriteCacheLock(int physicalPort) {
    throwIfI2cPortIsInvalid(physicalPort);

    return segments[physicalPort].getWriteLock();
  }

  @Override
  public byte[] getI2cReadCache(int physicalPort) {
    throwIfI2cPortIsInvalid(physicalPort);

    return segments[physicalPort].getReadBuffer();
  }

  @Override public TimeWindow getI2cReadCacheTimeWindow(int physicalPort) {
    throwIfI2cPortIsInvalid(physicalPort);

    return segments[physicalPort].getTimeWindow();
  }

  @Override
  public byte[] getI2cWriteCache(int physicalPort) {
    throwIfI2cPortIsInvalid(physicalPort);

    return segments[physicalPort].getWriteBuffer();
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
}
