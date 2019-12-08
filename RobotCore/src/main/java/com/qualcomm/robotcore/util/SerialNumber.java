/* Copyright (c) 2014, 2015 Qualcomm Technologies Inc

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

package com.qualcomm.robotcore.util;

import android.hardware.usb.UsbDevice;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.qualcomm.robotcore.R;
import com.qualcomm.robotcore.hardware.ScannedDevices;
import com.qualcomm.robotcore.hardware.configuration.ControllerConfiguration;

import org.firstinspires.ftc.robotcore.internal.system.Misc;
import org.firstinspires.ftc.robotcore.internal.usb.EmbeddedSerialNumber;
import org.firstinspires.ftc.robotcore.internal.usb.FakeSerialNumber;
import org.firstinspires.ftc.robotcore.internal.usb.LynxModuleSerialNumber;
import org.firstinspires.ftc.robotcore.internal.usb.UsbSerialNumber;
import org.firstinspires.ftc.robotcore.internal.usb.VendorProductSerialNumber;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.UUID;

/**
 * Instances of {@link SerialNumber} represent serial number indentifiers of hardware devices. For
 * USB-attached devices, these are usually the low-level USB serial number (see {@link UsbDevice#getSerialNumber()},
 * but that is not required. Rather, the notion of {@link SerialNumber} is a general purpose one
 * representing a user-visible digital identity for a particular device instance.
 *
 * 'Fake' serial numbers are serial numbers that will *never* appear for a real device; they
 * are useful, for example, as the serial number of a {@link ControllerConfiguration} that
 * has not yet been associated with a actual controller device. Fake serial numbers are never
 * shown to users.
 *
 * Note that *all* serial numbers loaded in memory at any given instant are guaranteed unique and
 * different, even the fake ones; this allows code that processes USB-device-bound {@link
 * ControllerConfiguration}s to operate easily on unbound ones as well, a significant coding
 * simplification. The technology used in fake serial numbers, {@link UUID}s, in fact guarantees
 * uniqueness across space and time, so fake serial numbers can be recorded persistently and
 * still maintain uniqueness. Historically, non-unique 'fake' serial numbers were also used: these
 * appeared int the form of "-1" or "N/A". When loaded from persistent storage, such legacy
 * fake serial numbers are converted to unique ones to maintain the uniqueness guarantee.
 */
@SuppressWarnings("WeakerAccess")
@JsonAdapter(SerialNumber.GsonTypeAdapter.class)
public abstract class SerialNumber implements Serializable {

  //------------------------------------------------------------------------------------------------
  // State
  //------------------------------------------------------------------------------------------------

  protected static final String fakePrefix = "FakeUSB:";
  protected static final String vendorProductPrefix = "VendorProduct:";
  protected static final String lynxModulePrefix = "ExpHub:";
  protected static final String embedded = "(embedded)";

  protected final String serialNumberString;

  //------------------------------------------------------------------------------------------------
  // Construction
  //------------------------------------------------------------------------------------------------

  /**
   * Constructs a serial number using the supplied initialization string. If the initialization
   * string is a legacy form of fake serial number, a unique fake serial number is created.
   *
   * @param serialNumberString the initialization string for the serial number.
   */
  protected SerialNumber(String serialNumberString) {
    this.serialNumberString = serialNumberString;
  }

  public static @NonNull SerialNumber createFake() {
    return new FakeSerialNumber();
  }

  public static @NonNull SerialNumber createEmbedded() {
    return new EmbeddedSerialNumber();
  }

  public static @NonNull SerialNumber fromString(@Nullable String serialNumberString) {
    if (FakeSerialNumber.isLegacyFake(serialNumberString)) {
      return createFake();
    } else if (serialNumberString.startsWith(fakePrefix)) {
      return new FakeSerialNumber(serialNumberString);
    } else if (serialNumberString.startsWith(vendorProductPrefix)) {
      return new VendorProductSerialNumber(serialNumberString);
    } else if (serialNumberString.startsWith(lynxModulePrefix)) {
      return new LynxModuleSerialNumber(serialNumberString);
    } else if (serialNumberString.equals(embedded)) {
      return createEmbedded();
    } else {
      return new UsbSerialNumber(serialNumberString);
    }
  }

  public static @Nullable SerialNumber fromStringOrNull(@Nullable String serialNumberString) {
    if (!TextUtils.isEmpty(serialNumberString)) {
      return fromString(serialNumberString);
    }
    return null;
  }

  public static @Nullable SerialNumber fromUsbOrNull(@Nullable String serialNumberString) {
    if (UsbSerialNumber.isValidUsbSerialNumber(serialNumberString)) {
      return fromString(serialNumberString);
    }
    return null;
  }

  /** Makes up a serial-number-like-thing for USB devices that internally lack a serial number. */
  public static SerialNumber fromVidPid(int vid, int pid, String connectionPath) {
    return new VendorProductSerialNumber(vid, pid, connectionPath);
  }

  //------------------------------------------------------------------------------------------------
  // Gson
  //------------------------------------------------------------------------------------------------

  static class GsonTypeAdapter extends TypeAdapter<SerialNumber> {
    @Override public void write(JsonWriter writer, SerialNumber serialNumber) throws IOException {
      if (serialNumber==null) {
        writer.nullValue();
      } else {
        writer.value(serialNumber.getString());
      }
    }

    @Override public SerialNumber read(JsonReader reader) throws IOException {
      return SerialNumber.fromStringOrNull(reader.nextString());
    }
  }

  //------------------------------------------------------------------------------------------------
  // Accessing
  //------------------------------------------------------------------------------------------------

  public boolean isVendorProduct() {
    return false;
  }

  /**
   * Returns whether the indicated serial number is one of the legacy
   * fake serial number forms or not.
   * @return whether the the serial number is a legacy fake form of serial number
   */
  public boolean isFake() {
    return false;
  }

  /**
   * Returns whether the serial number is one of an actual USB device.
   */
  public boolean isUsb() {
    return false;
  }

  /**
   * Returns whether the serial number is the one used for the embedded 
   * Expansion Hub inside a Rev Control Hub.
   */
  public boolean isEmbedded() {
    return false;
  }

  /**
   * Returns the string contents of the serial number. Result is not intended to be
   * displayed to humans.
   * @see #toString() 
   */
  public String getString() {
    return serialNumberString;
  }


  /**
   * Returns the {@link SerialNumber} of the device associated with this one that would appear
   * in a {@link ScannedDevices}.
   */
  public SerialNumber getScannableDeviceSerialNumber() {
    return this;
  }

  //------------------------------------------------------------------------------------------------
  // Comparison
  //------------------------------------------------------------------------------------------------

  public boolean matches(Object pattern) {
    return this.equals(pattern);
  }

  @Override
  public boolean equals(Object object) {
    if (object == null) return false;
    if (object == this) return true;

    if (object instanceof SerialNumber) {
      return serialNumberString.equals(((SerialNumber) object).serialNumberString);
    }

    if (object instanceof String) {
      return this.equals((String)object);
    }

    return false;
  }

  // separate method to avoid annoying Android Studio inspection warnings when comparing SerialNumber against String
  public boolean equals(String string) {
    return serialNumberString.equals(string);
  }

  @Override
  public int hashCode() {
    return serialNumberString.hashCode() ^ 0xabcd9873;
  }

  //------------------------------------------------------------------------------------------------
  // Serial number display name management
  //------------------------------------------------------------------------------------------------

  protected static final HashMap<String,String> deviceDisplayNames = new HashMap<String, String>();

  public static void noteSerialNumberType(SerialNumber serialNumber, String typeName) {
    synchronized (deviceDisplayNames) {
      deviceDisplayNames.put(serialNumber.getString(), Misc.formatForUser("%s [%s]", typeName, serialNumber));
    }
  }

  public static String getDeviceDisplayName(SerialNumber serialNumber) {
    synchronized (deviceDisplayNames) {
      String result = deviceDisplayNames.get(serialNumber.getString());
      if (result == null) {
        result = Misc.formatForUser(R.string.deviceDisplayNameUnknownUSBDevice, serialNumber);
      }
      return result;
    }
  }

}
