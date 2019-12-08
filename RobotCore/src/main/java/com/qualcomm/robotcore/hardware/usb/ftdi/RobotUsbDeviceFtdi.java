/* Copyright (c) 2015 Qualcomm Technologies Inc

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of Qualcomm Technologies Inc nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. */

package com.qualcomm.robotcore.hardware.usb.ftdi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.qualcomm.robotcore.hardware.usb.RobotUsbDevice;
import com.qualcomm.robotcore.hardware.usb.RobotUsbDeviceImplBase;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.SerialNumber;

import org.firstinspires.ftc.robotcore.internal.hardware.TimeWindow;
import org.firstinspires.ftc.robotcore.internal.ftdi.FtDevice;
import org.firstinspires.ftc.robotcore.internal.ftdi.FtDeviceManager;
import org.firstinspires.ftc.robotcore.internal.usb.exception.RobotUsbDeviceClosedException;
import org.firstinspires.ftc.robotcore.internal.usb.exception.RobotUsbException;
import org.firstinspires.ftc.robotcore.internal.usb.exception.RobotUsbFTDIException;
import org.firstinspires.ftc.robotcore.internal.usb.exception.RobotUsbUnspecifiedException;

import java.io.File;

@SuppressWarnings("WeakerAccess")
public class RobotUsbDeviceFtdi extends RobotUsbDeviceImplBase implements RobotUsbDevice {
  
  public static final String TAG = "RobotUsbDeviceFtdi";
  public static boolean DEBUG = false;
  @Override public String getTag() { return TAG; }

  private final FtDevice             device;
  private int                        cbus_mask    = 0;
  private int                        cbus_outputs = 0;

  public RobotUsbDeviceFtdi(FtDevice device, SerialNumber serialNumber) {
    super(serialNumber);
    this.device = device;
    this.firmwareVersion = new FirmwareVersion();
  }

  protected interface RunnableWithRobotUsbCommException {
    void run() throws RobotUsbException;
  }

  @Override public
  void setDebugRetainBuffers(boolean retain) {
    device.setDebugRetainBuffers(retain);
  }

  @Override public
  boolean getDebugRetainBuffers() {
    return device.getDebugRetainBuffers();
  }

  @Override public void logRetainedBuffers(long nsTimerStart, long nsTimerExpire, String tag, String format, Object...args) {
    device.logRetainedBuffers(nsTimerStart, nsTimerExpire, tag, format, args);
  }

  @Override
  public void setBaudRate(int rate) throws RobotUsbException {
    device.setBaudRate(rate);
  }

  @Override
  public void setDataCharacteristics(byte dataBits, byte stopBits, byte parity) throws RobotUsbException {
    device.setDataCharacteristics(dataBits, stopBits, parity);
  }

  @Override
  public void setLatencyTimer(int latencyTimer) throws RobotUsbException {
    device.setLatencyTimer((byte) latencyTimer);
  }

  @Override public void setBreak(boolean enable) throws RobotUsbException {
    if (enable)
      device.setBreakOn();
    else
      device.setBreakOff();
  }

  @Override public void resetAndFlushBuffers() throws RobotUsbException {
    device.resetDevice();
    device.flushBuffers();
  }

  @Override
  public void write(final byte[] data) throws InterruptedException, RobotUsbException {
    device.write(data);
  }

  @Override
  public int read(byte[] data, int ibFirst, int cbToRead, long msTimeout, @Nullable TimeWindow timeWindow) throws RobotUsbException, InterruptedException {
    try {
      if (cbToRead > 0) {
        // FT_Device.read returns one of the following
        //
        //  neg       an error occurred
        //   0        timeout reached
        //  cbToRead  read successfully completed
        //
        int cbRead = device.read(data, ibFirst, cbToRead, msTimeout, timeWindow);
        //
        if (cbRead == cbToRead) {
          // got the data we were looking for
          if (DEBUG) dumpBytesReceived(data, ibFirst, cbRead);
          return cbRead;

        } else if (cbRead < 0) {
          // read returned an error
          switch (cbRead) {
            case FtDevice.RC_DEVICE_CLOSED:
              RobotUsbException deviceClosedReason = device.getDeviceClosedReason();
              throw deviceClosedReason != null ? deviceClosedReason : new RobotUsbDeviceClosedException("error: closed: FT_Device.read()==RC_DEVICE_CLOSED");
            case FtDevice.RC_ILLEGAL_ARGUMENT:
              throw new IllegalArgumentException("illegal argument passed to RobotUsbDevice.read()");
            case FtDevice.RC_ILLEGAL_STATE:
              throw new RobotUsbUnspecifiedException("error: illegal state");
            default:
              throw new RobotUsbUnspecifiedException("error: FT_Device.read()=%d", cbRead);
          }

        } else if (cbRead == 0) {
          // Timeout happened

        } else {
          // Something bizarre happened
          throw new RobotUsbUnspecifiedException("unexpected result %d from FT_Device_.read()", cbRead);
        }
      }

      return 0;

    } catch (RuntimeException e) {
      throw RobotUsbFTDIException.createChained(e, "runtime exception during read() of %d bytes on %s", cbToRead, serialNumber);
    }
  }

  @Override public boolean mightBeAtUsbPacketStart() {
    return device.mightBeAtUsbPacketStart();
  }

  @Override public void skipToLikelyUsbPacketStart() {
    device.skipToLikelyUsbPacketStart();
  }

  @Override
  public void requestReadInterrupt(boolean interruptRequested) {
    device.requestReadInterrupt(interruptRequested);
  }

  @Override
  public synchronized void close() {
    RobotLog.vv(TAG, "closing %s", serialNumber);
    device.close();
    removeFromExtantDevices();
  }

  @Override
  public boolean isOpen() {
    return FtDevice.isOpen(device);
  }

  @Override public boolean isAttached() {
    return new File(device.getUsbDevice().getDeviceName()).exists();
  }

  @Override
  public USBIdentifiers getUsbIdentifiers() {
    USBIdentifiers result = new USBIdentifiers();
    // FT_Device ctor has this construct: // this.mDeviceInfoNode.id = this.mUsbDevice.getVendorId() << 16 | this.mUsbDevice.getProductId();
    int id = device.getDeviceInfo().id;
    result.vendorId  = (id >> 16) & 0xFFFF;
    result.productId = id & 0xFFFF;
    result.bcdDevice = device.getDeviceInfo().bcdDevice;
    return result;
  }

  @Override
  public @NonNull String getProductName() {
    return device.getDeviceInfo().productName;
  }

//------------------------------------------------------------------------------------------------
  // FTDI cbus control logic
  //  APIs modeled after https://github.com/lsgunth/pyft232/blob/master/ft232/d2xx.py
  //------------------------------------------------------------------------------------------------

  public boolean supportsCbusBitbang() {
    // This should undoubtedly check the FTDI device type and reason thereon.
    // For the moment, we only use this with the FTDI chip in the Lynx Module,
    // which we know will work (it uses a FT230XQ-R)
    return true;
  }

  public void cbus_setup(int mask, int init) throws InterruptedException, RobotUsbException {
    cbus_mask = mask & 0x0f;
    cbus_outputs = init & 0x0f;
    mask = (cbus_mask << 4) | (cbus_outputs & cbus_mask);
    cbus_setBitMode(mask, FtDeviceManager.BITMODE_CBUS_BITBANG);
  }

  public void cbus_write(int outputs) throws InterruptedException, RobotUsbException {
    cbus_outputs = outputs & 0x0f;
    int mask = (cbus_mask << 4) | (cbus_outputs & cbus_mask);
    cbus_setBitMode(mask, FtDeviceManager.BITMODE_CBUS_BITBANG);
  }

  private void cbus_setBitMode(final int mask, final int mode) throws InterruptedException, RobotUsbException {
    /**
     * mask - Bit-mask that specifies which pins are input (0) and which are output (1). Required
     * for bit-bang modes. In the case of CBUS bit-bang, the upper nibble of this value controls
     * which pins are inputs and outputs, while the lower nibble controls which of the outputs are
     * high and low.
     *
     * mode - The desired device mode. This can be one of the following: FT_BITMODE_RESET, FT_BITMODE_ASYNC_BITBANG,
     * FT_BITMODE_MPSSE, FT_BITMODE_SYNC_BITBANG, FT_BITMODE_MCU_HOST, FT_BITMODE_FAST_SERIAL,
     * FT_BITMODE_CBUS_BITBANG or FT_BITMODE_SYNC_FIFO.
     */
    if (!device.setBitMode((byte)mask, (byte)mode)) {
      throw new RobotUsbUnspecifiedException("setBitMode(0x%02x 0x02x) failed", mask, mode);
    }
  }

}
