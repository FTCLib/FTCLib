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

import com.qualcomm.hardware.modernrobotics.comm.ModernRoboticsUsbUtil;
import com.qualcomm.hardware.modernrobotics.comm.ReadWriteRunnable;
import com.qualcomm.hardware.modernrobotics.comm.ReadWriteRunnableStandard;
import com.qualcomm.hardware.modernrobotics.comm.RobotUsbDevicePretendModernRobotics;
import com.qualcomm.robotcore.eventloop.SyncdDevice;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.usb.RobotUsbDevice;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.SerialNumber;
import com.qualcomm.robotcore.util.ThreadPool;

import org.firstinspires.ftc.robotcore.internal.hardware.usb.ArmableUsbDevice;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Base class for Modern Robotics USB Devices
 */

@SuppressWarnings("unused,WeakerAccess")
public abstract class ModernRoboticsUsbDevice extends ArmableUsbDevice implements ReadWriteRunnableStandard.Callback
  {
  //------------------------------------------------------------------------------------------------
  // State
  //------------------------------------------------------------------------------------------------

  protected           ExecutorService         readWriteService;
  protected volatile  ReadWriteRunnable       readWriteRunnable;
  protected final     CreateReadWriteRunnable createReadWriteRunnable;

  //------------------------------------------------------------------------------------------------
  // Construction
  //------------------------------------------------------------------------------------------------

  public interface CreateReadWriteRunnable {
    ReadWriteRunnable create(RobotUsbDevice device) throws RobotCoreException, InterruptedException;
  }

  public ModernRoboticsUsbDevice(Context context, SerialNumber serialNumber, SyncdDevice.Manager manager, OpenRobotUsbDevice openRobotUsbDevice, CreateReadWriteRunnable createReadWriteRunnable)
      throws RobotCoreException, InterruptedException {

    super(context, serialNumber, manager, openRobotUsbDevice);

    this.createReadWriteRunnable = createReadWriteRunnable;
    this.readWriteService     = null;

    this.finishConstruction();
  }

  public void initializeHardware() {
    // subclasses do all the work that needs doing

    // Note: ModernRoboticsUsbDevices do not instantiate as armed; thus initializeHardware()
    // cannot be included as part of their constructor logic. Rather, they have to wait for their
    // instantiator to arm or pretend them, then manually initialize their hardware.

    // This is done, typically, in HardwareDeviceManager
  }

  //------------------------------------------------------------------------------------------------
  // Internal arming and disarming
  //------------------------------------------------------------------------------------------------

  /** intended as subclass hook */
  @Override protected RobotUsbDevice getPretendDevice(SerialNumber serialNumber) {
    return new RobotUsbDevicePretendModernRobotics(serialNumber);
  }

  @Override protected void armDevice(RobotUsbDevice device) throws RobotCoreException, InterruptedException {

    synchronized (armingLock) {

      // Remember the device we're using so we can guarantee it eventually gets close()d
      robotUsbDevice = device;

      // Create the appropriate ReadWriteRunnable on the device
      readWriteRunnable = this.createReadWriteRunnable.create(device);

      // Did that give us something?
      if (readWriteRunnable != null) {
        RobotLog.v("Starting up %sdevice %s", (this.armingState == ARMINGSTATE.TO_PRETENDING ? "pretend " : ""), this.serialNumber);

        readWriteRunnable.setOwner(this);

        readWriteRunnable.setCallback(this);
        readWriteService = ThreadPool.newSingleThreadExecutor("readWriteService");
        readWriteRunnable.executeUsing(readWriteService);

        syncdDeviceManager.registerSyncdDevice(readWriteRunnable);

        readWriteRunnable.setAcceptingWrites(true);
      }
    }
  }

  /**
   * Closes down the device in an orderly and reliable way. The device can be activated again
   * by subsequently calling armDevice(). Note that this implementation *must* be able to
   * disarm the device from any state, even a partially armed one.
   *
   * @throws InterruptedException
   */
  @Override protected void disarmDevice() throws InterruptedException {

    synchronized (armingLock) {

      // Stop accepting new work in our executor
      if (readWriteService != null)
        readWriteService.shutdown();

      // Close the readWriteRunnable
      if (readWriteRunnable != null) {
        // stop accepting new writes
        readWriteRunnable.setAcceptingWrites(false);

        // wait for any existing writes to finish
        readWriteRunnable.drainPendingWrites();

        // tear down connections and close
        syncdDeviceManager.unregisterSyncdDevice(readWriteRunnable);
        readWriteRunnable.close();
        readWriteRunnable = null;
        }

      // Wait until the executor service terminates
      if (readWriteService != null) {
        String serviceName = "ReadWriteRunnable for Modern Robotics USB Device";
        ThreadPool.awaitTerminationOrExitApplication(this.readWriteService, 30, TimeUnit.SECONDS, serviceName, "internal error");
        this.readWriteService = null;
      }

      // Close the device, for sure. It probably? was closed the rwrunnable was closed,
      // but it's possible in theory that we opened the device but never made the rwrunnable.
      // Note that it's ok to close() a RobotUsbDevice more than once.
      if (robotUsbDevice != null) {
        robotUsbDevice.close();
        robotUsbDevice = null;
      }
    }
  }

  //------------------------------------------------------------------------------------------------
  // Primitive accessors
  //------------------------------------------------------------------------------------------------

  public ReadWriteRunnable getReadWriteRunnable() {
    return this.readWriteRunnable;
  }

  public OpenRobotUsbDevice getOpenRobotUsbDevice() {
    return this.openRobotUsbDevice;
  }

  public CreateReadWriteRunnable getCreateReadWriteRunnable() {
    return this.createReadWriteRunnable;
  }

  //------------------------------------------------------------------------------------------------
  // Accessors and Operations
  //------------------------------------------------------------------------------------------------

  /**
   * Device Name
   *
   * @return device name
   */
  @SuppressWarnings("unused")
  public abstract String getDeviceName();

  /**
   * Version
   *
   * @return get the version of this device
   */
  public int getVersion() {
    return read8(ModernRoboticsUsbUtil.ADDRESS_VERSION_NUMBER);
  }

  /**
   * Write a single byte to the device
   *
   * @param address address to write
   */
  public void write8(int address, byte data) {
    write(address, new byte[]{data});
  }

  /**
   * Write a single byte to the device
   *
   * @param address address to write
   * @param data data to write (will be cast to a byte)
   */
  public void write8(int address, int data) {
    write(address, new byte[] {(byte) data});
  }

  /**
   * Write a single byte to the device
   *
   * @param address address to write
   * @param data data to write (will be cast to a byte)
   */
  public void write8(int address, double data) {
    write(address, new byte[] {(byte) data});
  }

  /**
   * Write to device
   *
   * @param address address to write
   * @param data data to write
   * @throws IllegalArgumentException if address is out of range
   */
  public void write(int address, byte[] data) {
      ReadWriteRunnable r = readWriteRunnable;
      if (r != null) r.write(address, data);
  }

  /**
   * Read from the device write cache
   *
   * @param address address of byte to read
   * @return byte
   */
  public byte readFromWriteCache(int address) {
    return readFromWriteCache(address, 1)[0];
  }

  /**
   * Read from the device write cache
   *
   * @param address address of byte to read
   * @param size number of bytes to read
   * @return byte array
   */
  public byte[] readFromWriteCache(int address, int size) {
    ReadWriteRunnable r = readWriteRunnable;
    if (r != null)
      return r.readFromWriteCache(address, size);
    else
      return new byte[size];
  }

  /**
   * Read a single byte from device
   *
   * @param address address to read
   * @throws IllegalArgumentException if address is out of range, or if read failed
   */
  public byte read8(int address) {
    return read(address, 1)[0];
  }

  /**
   * Read from device
   *
   * @param address address to read
   * @param size number of bytes to read
   * @throws IllegalArgumentException if address is out of range, or if read failed
   */
  public byte[] read(int address, int size) {
    ReadWriteRunnable r = readWriteRunnable;
    if (r != null)
      return r.read(address, size);
    else
      return new byte[size];
  }

  public void startupComplete() throws InterruptedException  {
    // no implementation by default
    }

  public void readComplete() throws InterruptedException {
    // no implementation by default
  }

  public void writeComplete() throws InterruptedException {
    // no implementation by default
  }

  public void shutdownComplete() throws InterruptedException  {
    // no implementation by default
    }

  private static void logAndThrow(String errMsg) throws RobotCoreException {
    System.err.println(errMsg);
    throw new RobotCoreException(errMsg);
  }
}
