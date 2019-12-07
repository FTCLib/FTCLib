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

package com.qualcomm.robotcore.hardware;

import com.qualcomm.robotcore.util.SerialNumber;

import org.firstinspires.ftc.robotcore.internal.hardware.TimeWindow;

import java.util.concurrent.locks.Lock;

/**
 * Interface for working with Digital Channel Controllers
 * <p>
 * Different digital channel controllers will implement this interface.
 */
public interface I2cController extends HardwareDevice {

  @SuppressWarnings("unused")
  byte I2C_BUFFER_START_ADDRESS = 0x4;

  /**
   * Callback interface for I2C port ready notifications
   */
  interface I2cPortReadyCallback {
    void portIsReady(int port);
  }

  /**
   * A callback interface through which a client can learn when portIsReady callbacks
   * begin and then later end.
   */
  interface I2cPortReadyBeginEndNotifications {
    void onPortIsReadyCallbacksBegin(int port) throws InterruptedException;
    void onPortIsReadyCallbacksEnd(int port) throws InterruptedException;
  }

  /**
   * Serial Number
   *
   * @return return the USB serial number of this device
   */
  SerialNumber getSerialNumber();


  /**
   * Enable read mode for a particular I2C device
   * @param physicalPort the port the device is attached to
   * @param i2cAddress the i2c address of the device
   * @param memAddress mem address at which to start reading
   * @param length number of bytes to read
   */
  void enableI2cReadMode(int physicalPort, I2cAddr i2cAddress, int memAddress, int length);

  /**
   * Enable write mode for a particular I2C device
   * @param physicalPort the port the device is attached to
   * @param i2cAddress the i2c address of the device
   * @param memAddress the memory address at which to start writing
   * @param length number of bytes to read
   */
  void enableI2cWriteMode(int physicalPort, I2cAddr i2cAddress, int memAddress, int length);

  /**
   * Get a copy of the most recent data read in from the device
   * @param physicalPort the port the device is attached to
   * @return a copy of the most recent data read in from the device
   */
  byte[] getCopyOfReadBuffer(int physicalPort);

  /**
   * Get a copy of the data that is set to be written out to the device
   * @param physicalPort the port the device is attached to
   * @return a copy of the data set to be written out to the device
   */
  byte[] getCopyOfWriteBuffer(int physicalPort);

  /**
   * Copy a byte array into the buffer that is set to be written out to the device
   * @param physicalPort the port the device is attached to
   * @param buffer buffer to copy
   */
  void copyBufferIntoWriteBuffer(int physicalPort, byte[] buffer);

  /**
   * Set the port action flag; this flag tells the controller to send the
   * current data in its buffer to the I2C device
   * @param port physical port number on the device
   * @see #clearI2cPortActionFlag(int)
   */
  void setI2cPortActionFlag(int port);

  /**
   * Clears the port action flag, undoing the effect of previous setI2cPortActionFlag()
   * @param port physical port number on the device
   * @see #setI2cPortActionFlag(int)
   */
  void clearI2cPortActionFlag(int port);

  /**
   * Get the port action flag; this flag is set if the particular port is busy.
   * @param port physical port number on the device
   */
  boolean isI2cPortActionFlagSet(int port);

  /**
   * Read the local cache in from the I2C Controller
   *
   * NOTE: unless this method is called the internal cache isn't updated
   * @param port physical port number on the device
   */
  void readI2cCacheFromController(int port);

  /**
   * Write the local cache to the I2C Controller
   *
   * NOTE: unless this method is called the internal cache isn't updated
   * @param port physical port number on the device
   */
  void writeI2cCacheToController(int port);

  /**
   * Write just the port action flag in the local cache to the I2C controller
   * @param port physical port number on the device
   */
  void writeI2cPortFlagOnlyToController(int port);

  /**
   * Is the port in read mode?
   * @param port physical port number on the device
   * @return true if in read mode; otherwise false
   */
  boolean isI2cPortInReadMode(int port);

  /**
   * Is the port in write mode?
   * @param port physical port number on the device
   * @return true if in write mode; otherwise false
   */
  boolean isI2cPortInWriteMode(int port);

  /**
   * Determine if a physical port is ready
   * @param port physical port number on the device
   * @return true if ready for command; false otherwise
   */
  boolean isI2cPortReady(int port);

  /**
   * Get access to the read cache lock.
   * <p>
   * This is needed if you are accessing the read cache directly. The read
   * cache lock needs to be acquired before attempting to interact with the read cache
   * @param port physical port number on the device
   * @return lock
   */
  Lock getI2cReadCacheLock(int port);

  /**
   * Get access to the write cache lock.
   * <p>
   * This is needed if you are accessing the write cache directly. The write
   * cache lock needs to be acquired before attempting to interact with the
   * write cache
   * @param port physical port number on the device
   * @return lock
   */
  Lock getI2cWriteCacheLock(int port);

  /**
   * Get direct access to the cache that I2C reads will be populated into
   * <p>
   * Please lock the cache before accessing it.
   * @param port physical port number on the device
   * @return byte array
   */
  byte[] getI2cReadCache(int port);

  /**
   * Returns the time window object into which timestamps are placed as the read cache is populated
   * @param port physical port number on the device
   * @return the time window object into which timestamps are placed as the read cache is populated
   */
  TimeWindow getI2cReadCacheTimeWindow(int port);

  /**
   * Get direct access to the cache that I2C writes will be populated into
   * <p>
   * Please lock the cache before accessing it.
   * @param port physical port number on the device
   * @return byte array
   */
  byte[] getI2cWriteCache(int port);

  /**
   * Returns the maximum interval, in milliseconds, from when the controller receives an I2c write
   * transmission over USB to when that write is actually issued to the I2c device.
   *
   * @return the maximum interval, in milliseconds, from when the controller receives an I2c write
   * transmission over USB to when that write is actually issued to the I2c device.
   */
  int getMaxI2cWriteLatency(int port);

  /**
   * Register to be notified when a given I2C port is ready.
   *
   * The callback method will be called after the latest data has been read from the I2C Controller.
   *
   * Only one callback can be registered for a given port. Last to register wins.
   *
   * @param callback register a callback
   * @param port port to be monitored
   */
  void registerForI2cPortReadyCallback(I2cPortReadyCallback callback, int port);

  /**
   * Returns the callback currently registered to receive portIsReady notifications for the
   * indicated port
   * @param port  the port of interest
   * @return      the currently registered callback, if any
   */
  I2cPortReadyCallback getI2cPortReadyCallback(int port);

  /**
   * De-register for port ready notifications.
   * @param port port no longer being monitored.
   */
  void deregisterForPortReadyCallback(int port);


  /**
   * Registers to be notification when portIsReady callbacks begin or cease
   * @param callback  the callback to register
   * @param port      the port number to register the callback on
   */
  void registerForPortReadyBeginEndCallback(I2cPortReadyBeginEndNotifications callback, int port);

  /**
   * Returns the current callback registered for a given port
   * @param port the port in question
   * @return  the current callback for that port, or null if none presently exists
   */
  I2cPortReadyBeginEndNotifications getPortReadyBeginEndCallback(int port);

  /**
   * Deregisters any existing notifications callback for the given port
   * @param port the port in question
   */
  void deregisterForPortReadyBeginEndCallback(int port);

  /**
   * Returns whether, as of this instant, this controller is alive and operational in
   * its normally expected mode; that is, whether it is currently in communication
   * with its underlying hardware or whether it is in some other state
   *
   * @return the arming status of this controller
   */
  boolean isArmed();

  /**
   * Deprecated, use readI2cCacheFromController(port)
   * @param port physical port number on the device
   */
  @Deprecated
  void readI2cCacheFromModule(int port);

  /**
   * Deprecated, use writeI2cCacheToController(port)
   * @param port physical port number on the device
   */
  @Deprecated
  void writeI2cCacheToModule(int port);

  /**
   * Deprecated, use writeI2cPortFlagOnlyToController(port)
   * @param port physical port number on the device
   */
  @Deprecated
  void writeI2cPortFlagOnlyToModule(int port);
}
