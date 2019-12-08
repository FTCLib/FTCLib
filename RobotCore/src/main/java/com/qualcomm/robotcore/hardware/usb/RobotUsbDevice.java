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

package com.qualcomm.robotcore.hardware.usb;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.qualcomm.robotcore.hardware.DeviceManager;
import com.qualcomm.robotcore.hardware.usb.serial.RobotUsbDeviceTty;
import com.qualcomm.robotcore.util.SerialNumber;

import org.firstinspires.ftc.robotcore.internal.system.Assert;
import org.firstinspires.ftc.robotcore.internal.hardware.TimeWindow;
import org.firstinspires.ftc.robotcore.internal.system.Misc;
import org.firstinspires.ftc.robotcore.internal.usb.UsbConstants;
import org.firstinspires.ftc.robotcore.internal.usb.exception.RobotUsbException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * {@link RobotUsbDevice} is an interface to USB devices that are commuicated with using
 * a serial communication stream. This is in contrast to more other USB devices that use
 * the full capabilities of USB. See http://www.usb.org/developers/defined_class.
 *
 * Note that this is not to be confused with {@link RobotUsbDeviceTty}.
 */
@SuppressWarnings("WeakerAccess")
public interface RobotUsbDevice {

  enum Channel { RX, TX, NONE, BOTH };

  class FirmwareVersion {
    public int  majorVersion;
    public int  minorVersion;
    public FirmwareVersion(int majorVersion, int minorVersion) {
      this.majorVersion = majorVersion;
      this.minorVersion = minorVersion;
    }
    public FirmwareVersion(int bVersion) {
      majorVersion = (bVersion >> 4) & 0x0F;
      minorVersion = (bVersion >> 0) & 0x0F;
    }
    public FirmwareVersion() {
      this(0, 0);
    }

    @Override public String toString() {
      return Misc.formatInvariant("v%d.%d", this.majorVersion, this.minorVersion);
    }
  }

  /** @see #getUsbIdentifiers() */
  class USBIdentifiers {
    public int    vendorId;
    public int    productId;
    public int    bcdDevice;

    // See also device_filter.xml
    private static final int vendorIdFTDI = UsbConstants.VENDOR_ID_FTDI;
    private static final Set<Integer> productIdsModernRobotics = new HashSet<Integer>(Arrays.asList(new Integer[] { 0x6001 }));
    private static final Set<Integer> bcdDevicesModernRobotics = new HashSet<Integer>(Arrays.asList(new Integer[] { 0x0600 }));
    private static final Set<Integer> productIdsLynx           = new HashSet<Integer>(Arrays.asList(new Integer[] { 0x6015 }));
    private static final Set<Integer> bcdDevicesLynx           = new HashSet<Integer>(Arrays.asList(new Integer[] { 0x1000 }));

    public boolean isModernRoboticsDevice() {
      return this.vendorId == vendorIdFTDI
              && productIdsModernRobotics.contains(this.productId)
              && bcdDevicesModernRobotics.contains(this.bcdDevice & 0xFF00);
    }

    public boolean isLynxDevice() {
      return this.vendorId == vendorIdFTDI
              && productIdsLynx.contains(this.productId)
              && bcdDevicesLynx.contains(this.bcdDevice & 0xFF00);
    }

    @SuppressWarnings("ConstantConditions")
    public static USBIdentifiers createLynxIdentifiers() {
      USBIdentifiers result = new USBIdentifiers();
      result.vendorId = vendorIdFTDI;
      result.productId = first(productIdsLynx);
      result.bcdDevice = first(bcdDevicesLynx);
      Assert.assertTrue(result.isLynxDevice());
      return result;
    }

    protected static <T> T first(Set<T> set) {
      //noinspection LoopStatementThatDoesntLoop
      for (T t : set) { return t; }
      return null;
    }
  }

  void setDebugRetainBuffers(boolean retain);

  boolean getDebugRetainBuffers();

  void logRetainedBuffers(long nsOrigin, long nsTimerExpire, String tag, String format, Object...args);

  /**
   * Sets the rate of data transmission used to communicate with the device.
   * @param rate baud rate
   */
  void setBaudRate(int rate) throws RobotUsbException;

  /**
   * Set the Data Characteristics
   * @param dataBits data bits
   * @param stopBits stop bits
   * @param parity   parity
   */
  void setDataCharacteristics(byte dataBits, byte stopBits, byte parity) throws RobotUsbException;

  /**
   * Set the latency timer
   * @param latencyTimer latency timer
   */
  void setLatencyTimer(int latencyTimer) throws RobotUsbException;

  /**
   * Sets or unsets a break state on the communication line
   * @param enable whether or not to set a break state
   */
  void setBreak(boolean enable) throws RobotUsbException;

  /**
   * Resets the remote USB device comm-layer as best we can. Additionally, flushes
   * any incoming or outgoing data buffers on this end of the line.
   */
  void resetAndFlushBuffers() throws RobotUsbException;

  /**
   * Write to device
   * @param data byte array
   */
  void write(byte[] data) throws InterruptedException, RobotUsbException;

  /**
   * Skips to the beginning of a USB packet, if not already there. It is safe to err
   * on the side of saying we're at the start when in fact we may not be. The point of this
   * is that the packet start can have significance for resynchronization.
   */
  void skipToLikelyUsbPacketStart();

  /**
   * Answers as to whether we're it's probably the case we're at the start of a Usb
   * packet. No is definitive; yes is uncertain.
   */
  boolean mightBeAtUsbPacketStart();

  /**
   * Reads a requested number of bytes from the device.
   * @param data       byte array into which to read the data
   * @param ibFirst    first index in data at which to read the data
   * @param cbToRead   number of bytes to read
   * @param msTimeout  amount of time to wait, in ms, for data to become available to read
   * @param timeWindow optional place to return information as to when the data was read
   * @return number of bytes read into byte array, or 0 if the timeout was hit
   */
  int read(byte[] data, int ibFirst, int cbToRead, long msTimeout, @Nullable TimeWindow timeWindow) throws RobotUsbException, InterruptedException;

  /**
   * Interrupts any reads that are currently pending inside the device, possibly waiting
   * on a perhaps lengthy timeout.
   */
  void requestReadInterrupt(boolean interruptRequested);

  /**
   * Closes the device
   */
  void close();

  /**
   * Returns whether the device is open or not
   */
  boolean isOpen();

  /**
   * Returns whether or not this USB device is known to be physically attached
   */
  boolean isAttached();

  /** Returns the firmware version of this USB device, or null if no such version is known.
   * @see #setFirmwareVersion(FirmwareVersion) */
  FirmwareVersion getFirmwareVersion();

  /**
   * Sets the firmware version of this USB device. Note that this does not actually change
   * the persistent firmware version inside the device, only our local copy of it here.
   * @see #getFirmwareVersion()
   */
  void setFirmwareVersion(FirmwareVersion version);

  /**
   * Returns the USB-level vendor and product id of this device.
   *
   * All the devices we are interested in use FTDI chips (2018.06.01: no longer true: webcams!), which 
   * report as vendor 0x0403. Modern Robotics modules (currently?) use a product id of 0x6001 and 
   * bcdDevice of 0x0600. Lynx modules use a product id of 0x6015 and bcdDevice of 0x1000. Note that for FTDI,
   * only the upper byte of the two-byte bcdDevice seems to be of significance.
   *
   * "Every Universal Serial Bus (USB) device must be able to provide a single device descriptor that
   * contains relevant information about the device. The USB_DEVICE_DESCRIPTOR structure describes a
   * device descriptor. Windows uses that information to derive various sets of information. For
   * example, the idVendor and idProduct fields specify vendor and product identifiers, respectively.
   * Windows uses those field values to construct a hardware ID for the device. To view the hardware
   * ID of a particular device, open Device Manager and view device properties. In the Details tab,
   * the Hardware Ids property value indicates the hardware ID ("USB\XXX") that is generated by Windows.
   * The bcdUSB field indicates the version of the USB specification to which the device conforms.
   * For example, 0x0200 indicates that the device is designed as per the USB 2.0 specification. The
   * bcdDevice value indicates the device-defined revision number. The USB driver stack uses bcdDevice,
   * along with idVendor and idProduct, to generate hardware and compatible IDs for the device. You
   * can view the those identifiers in Device Manager. The device descriptor also indicates the total
   * number of configurations that the device supports"
   *
   * @see <a href="https://msdn.microsoft.com/en-us/library/windows/hardware/ff539283(v=vs.85).aspx">USB Device Descriptors</a>
   */
  USBIdentifiers getUsbIdentifiers();

  @NonNull SerialNumber getSerialNumber();

  @NonNull String getProductName();

  void setDeviceType(@NonNull DeviceManager.UsbDeviceType deviceType);
  @NonNull DeviceManager.UsbDeviceType getDeviceType();
}
