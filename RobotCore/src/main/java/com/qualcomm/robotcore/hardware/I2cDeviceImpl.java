/*
 * Copyright (c) 2015 Qualcomm Technologies Inc
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

import com.qualcomm.robotcore.R;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.hardware.TimeWindow;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;

/**
 * Control a single I2C Device
 */
public class I2cDeviceImpl extends I2cControllerPortDeviceImpl implements I2cDevice, HardwareDevice, I2cController.I2cPortReadyCallback {

  //------------------------------------------------------------------------------------------------
  // State
  //------------------------------------------------------------------------------------------------

  protected I2cController.I2cPortReadyCallback callback;
  protected AtomicInteger                      callbackCount;

  //------------------------------------------------------------------------------------------------
  // Construction
  //------------------------------------------------------------------------------------------------

  /**
   * Constructor
   *
   * @param controller I2C controller this channel is attached to
   * @param port port on the I2C controller
   */
  public I2cDeviceImpl(I2cController controller, int port) {
    super(controller, port);
    this.callback      = null;
    this.callbackCount = new AtomicInteger(0);
  }

  @Override
  protected void controllerNowArmedOrPretending() {
    // Nothing needed
    }

  //------------------------------------------------------------------------------------------------
  // I2cDevice interface
  //------------------------------------------------------------------------------------------------

 /**
   * returns the I2cController on which this device is found
   * @return the I2cController on which this device is found
   */
  @Override public I2cController getController() {
    return this.controller;
  }

  /**
   * returns the port number on the controller on which this device is found
   * @return the port number on the controller on which this device is found
   */
  @Override public int getPort() {
    return this.physicalPort;
  }

  /**
   * Enable read mode for this I2C device
   * @param memAddress mem address at which to start reading
   * @param length number of bytes to read
   */
  @Deprecated @Override
  public void enableI2cReadMode(I2cAddr i2cAddr, int memAddress, int length) {
    controller.enableI2cReadMode(physicalPort, i2cAddr, memAddress, length);
  }

  /**
   * Enable write mode for this I2C device
   * @param memAddress mem address at which to start writing
   * @param length number of bytes to write
   */
  @Deprecated @Override
  public void enableI2cWriteMode(I2cAddr i2cAddr, int memAddress, int length) {
    controller.enableI2cWriteMode(physicalPort, i2cAddr, memAddress, length);
  }

  /**
   * Get a copy of the most recent data read in from the device
   * @return a copy of the most recent data read in from the device
   */
  @Override public byte[] getCopyOfReadBuffer() {
    return controller.getCopyOfReadBuffer(physicalPort);
  }

  /**
   * Get a copy of the data that is set to be written out to the device
   * @return a copy of the data set to be written out to the device
   */
  @Override public byte[] getCopyOfWriteBuffer() {
    return controller.getCopyOfWriteBuffer(physicalPort);
  }

  /**
   * Copy a byte array into the buffer that is set to be written out to the device
   * @param buffer buffer to copy
   */
  @Override public void copyBufferIntoWriteBuffer(byte[] buffer) {
    controller.copyBufferIntoWriteBuffer(physicalPort, buffer);
  }
  /**
   * Set the port action flag; this flag tells the controller to send the
   * current data in its buffer to the I2C device
   */
  @Override public void setI2cPortActionFlag() {
    controller.setI2cPortActionFlag(physicalPort);
  }

  @Override public void clearI2cPortActionFlag() {
    controller.clearI2cPortActionFlag(physicalPort);
  }

/**
   * Check whether or not the action flag is set for this I2C port
   * @return a boolean indicating whether or not the flag is set
   */
  @Override public boolean isI2cPortActionFlagSet() {
    return controller.isI2cPortActionFlagSet(physicalPort);
  }

  /**
   * Trigger a read of the I2C cache
   */
  @Override public void readI2cCacheFromController() {
    controller.readI2cCacheFromController(physicalPort);
  }

  /**
   * Trigger a write of the I2C cache
   */
  @Override public void writeI2cCacheToController() {
    controller.writeI2cCacheToController(physicalPort);
  }

  /**
   * Write only the action flag
   */
  @Override public void writeI2cPortFlagOnlyToController() {
    controller.writeI2cPortFlagOnlyToController(physicalPort);
  }

  /**
   * Query whether or not the port is in Read mode
   * @return whether or not this port is in read mode
   */
  @Override public boolean isI2cPortInReadMode() {
    return controller.isI2cPortInReadMode(physicalPort);
  }

  /**
   * Query whether or not this port is in write mode
   * @return whether or not this port is in write mode
   */
  @Override public boolean isI2cPortInWriteMode() {
    return controller.isI2cPortInWriteMode(physicalPort);
  }

  /**
   * Query whether or not this I2c port is ready
   * @return boolean indicating I2c port readiness
   */
  @Override public boolean isI2cPortReady() {
    return controller.isI2cPortReady(physicalPort);
  }

  /**
   * Get access to the read cache lock.
   * <p>
   * This is needed if you are accessing the read cache directly. The read
   * cache lock needs to be acquired before attempting to interact with the read cache
   * @return the read cache lock
   */
  @Override public Lock getI2cReadCacheLock() {
    return controller.getI2cReadCacheLock(physicalPort);
  }

  /**
   * Get access to the write cache lock.
   * <p>
   * This is needed if you ace accessing the write cache directly. The write
   * cache lock needs to be acquired before attempting to interact with the
   * write cache
   * @return write cache lock
   */
  @Override public Lock getI2cWriteCacheLock() {
    return controller.getI2cWriteCacheLock(physicalPort);
  }

  /**
   * Get direct access to the read cache used by this I2C device
   * <p>
   * Please lock the cache before accessing it.
   * @return the read cache
   */
  @Override public byte[] getI2cReadCache() {
    return controller.getI2cReadCache(physicalPort);
  }

  @Override public TimeWindow getI2cReadCacheTimeWindow() {
    return controller.getI2cReadCacheTimeWindow(physicalPort);
  }

  /**
   * Get direct access to the write cache used by this I2C device
   * <p>
   * Please lock the cache before accessing it.
   * @return the write cache
   */
  @Override public byte[] getI2cWriteCache() {
    return controller.getI2cWriteCache(physicalPort);
  }

  //------------------------------------------------------------------------------------------------
  // portIsReady callback management
  //------------------------------------------------------------------------------------------------

  @Override
  public void portIsReady(int port) {
    this.callbackCount.incrementAndGet();
    I2cController.I2cPortReadyCallback callback = this.callback;
    if (callback != null) {
      callback.portIsReady(port);
    }
  }

  @Override public int getMaxI2cWriteLatency() {
    return controller.getMaxI2cWriteLatency(physicalPort);
  }

  /**
   * The method used to register for a port-ready callback
   * @param callback pass in the I2C callback that will be called when the device is ready
   */
  @Override public synchronized void registerForI2cPortReadyCallback(I2cController.I2cPortReadyCallback callback) {
    this.callback = callback;
    controller.registerForI2cPortReadyCallback(this, physicalPort);
  }

  /**
   * returns the currently registered port-ready callback for this device
   * @return the currently registered port-ready callback for this device
   */
  @Override public I2cController.I2cPortReadyCallback getI2cPortReadyCallback() {
    return this.callback;
  }

  /**
   * Unregister for a port-ready callback
   */
  @Override public synchronized void deregisterForPortReadyCallback() {
    controller.deregisterForPortReadyCallback(physicalPort);
    this.callback = null;
  }

  @Override
  public int getCallbackCount() {
    return this.callbackCount.get();
  }

//------------------------------------------------------------------------------------------------
  // Callback begin / end notifications
  //------------------------------------------------------------------------------------------------

  /**
   * registers for notifications as to when port-ready callbacks begin or cease
   * @param callback the callback which will receive such notifications
   */
  @Override public void registerForPortReadyBeginEndCallback(I2cController.I2cPortReadyBeginEndNotifications callback) {
    controller.registerForPortReadyBeginEndCallback(callback, physicalPort);
  }

  /**
   * returns the currently registered callback that will receive begin and cessation notifications
   * @return the currently registered callback that will receive begin and cessation notifications
   */
  @Override public I2cController.I2cPortReadyBeginEndNotifications getPortReadyBeginEndCallback() {
    return controller.getPortReadyBeginEndCallback(physicalPort);
  }

  /**
   * deregister for port-ready begin and cessation notifications
   */
  @Override public void deregisterForPortReadyBeginEndCallback() {
    controller.deregisterForPortReadyBeginEndCallback(physicalPort);
  }

  //------------------------------------------------------------------------------------------------
  // Arming and disarming
  //------------------------------------------------------------------------------------------------

  @Override
  public boolean isArmed() {
    return controller.isArmed();
    }

  //------------------------------------------------------------------------------------------------
  // HardwareDevice interface
  //------------------------------------------------------------------------------------------------

  @Override public Manufacturer getManufacturer() {
    return controller.getManufacturer();
  }

  @Override
  public String getDeviceName() {
    return AppUtil.getDefContext().getString(R.string.configTypeI2cDevice);
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

  /**
   * Deprecated, use readI2cCacheFromController()
   */
  @Deprecated @Override
  public void readI2cCacheFromModule() {
    readI2cCacheFromController();
  }

  /**
   * Deprecated, use writeI2cCacheToController()
   */
  @Deprecated @Override
  public void writeI2cCacheToModule() {
    writeI2cCacheToController();
  }

  /**
   * Deprecated, use writeI2cPortFlagOnlyToController()
   */
  @Deprecated @Override
  public void writeI2cPortFlagOnlyToModule() {
    writeI2cPortFlagOnlyToController();
  }
}
