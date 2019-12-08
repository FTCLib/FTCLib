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

package com.qualcomm.robotcore.hardware.configuration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.qualcomm.robotcore.hardware.DeviceManager;
import com.qualcomm.robotcore.util.SerialNumber;

import org.xmlpull.v1.XmlPullParser;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * ControllerConfiguration represents container of DeviceConfigurations.
 * It may or may not be USB attached; in the latter case, the serial number will be an instance of FakeSerialNumber.
 */
public abstract class ControllerConfiguration<ITEM_T extends DeviceConfiguration> extends DeviceConfiguration implements Serializable {

  //------------------------------------------------------------------------------------------------
  // State
  //------------------------------------------------------------------------------------------------

  public static final String XMLATTR_SERIAL_NUMBER = "serialNumber";

  /** If the controller contains only one "kind" of configuration, this can be used to store same
   *  Heterogenous contains will tend to ignore this.
   */
  private List<ITEM_T> devices;

  /** the serial number of the controller, if any */
  private @NonNull SerialNumber serialNumber;

  /** If we're a USB device, then do we know if we are attached, right now, or not? */
  private boolean knownToBeAttached = false;  // not persisted in XML

  /** True if this device is made up by the system, user is not aware of same */
  private boolean isSystemSynthetic = false; // not persisted in XML

  //------------------------------------------------------------------------------------------------
  // Construction
  //------------------------------------------------------------------------------------------------

  public ControllerConfiguration(String name, @NonNull SerialNumber serialNumber, ConfigurationType type) {
    this(name, new ArrayList<ITEM_T>(), serialNumber, type);
  }

  public ControllerConfiguration(String name, List<ITEM_T> devices, @NonNull SerialNumber serialNumber, ConfigurationType type) {
    super(type);
    super.setName(name);
    this.devices = devices;
    this.serialNumber = serialNumber;
  }

  public static @Nullable ControllerConfiguration forType(String name, @NonNull SerialNumber serialNumber, ConfigurationType type) {

    if (type==BuiltInConfigurationType.DEVICE_INTERFACE_MODULE)  return new DeviceInterfaceModuleConfiguration(name, serialNumber);
    if (type==BuiltInConfigurationType.LEGACY_MODULE_CONTROLLER) return new LegacyModuleControllerConfiguration(name, new LinkedList<DeviceConfiguration>(), serialNumber);
    if (type==BuiltInConfigurationType.MATRIX_CONTROLLER)        return new MatrixControllerConfiguration(name, new LinkedList<DeviceConfiguration>(), new LinkedList<DeviceConfiguration>(), serialNumber);
    if (type==BuiltInConfigurationType.MOTOR_CONTROLLER)         return new MotorControllerConfiguration(name, new LinkedList<DeviceConfiguration>(), serialNumber);
    if (type==BuiltInConfigurationType.SERVO_CONTROLLER)         return new ServoControllerConfiguration(name, new LinkedList<DeviceConfiguration>(), serialNumber);
    if (type==BuiltInConfigurationType.LYNX_USB_DEVICE)          return new LynxUsbDeviceConfiguration(name, new LinkedList<LynxModuleConfiguration>(), serialNumber);
    if (type==BuiltInConfigurationType.LYNX_MODULE)              return new LynxModuleConfiguration(name);  // unclear if necessary
    if (type==BuiltInConfigurationType.WEBCAM)                   return new WebcamConfiguration(name, serialNumber);

    return null;
  }

  //------------------------------------------------------------------------------------------------
  // Accessing
  //------------------------------------------------------------------------------------------------

  public List<ITEM_T> getDevices() {
    return devices;
  }

  public ConfigurationType getConfigurationType(){
    return super.getConfigurationType();
  }

  public void setSerialNumber(@NonNull SerialNumber serialNumber) {
    this.serialNumber = serialNumber;
  }

  public @NonNull SerialNumber getSerialNumber(){
    return this.serialNumber;
  }

  public boolean isKnownToBeAttached() {
    return this.knownToBeAttached;
  }

  public void setKnownToBeAttached(boolean knownToBeAttached) {
    this.knownToBeAttached = knownToBeAttached;
  }

  public boolean isSystemSynthetic() {
    return this.isSystemSynthetic;
  }

  public void setSystemSynthetic(boolean systemSynthetic) {
    this.isSystemSynthetic = systemSynthetic;
  }

  public void setDevices(List<ITEM_T> devices){
    this.devices = devices;
  }

  public DeviceManager.UsbDeviceType toUSBDeviceType() {
    return this.getConfigurationType().toUSBDeviceType();
  }

  @Override protected void deserializeAttributes(XmlPullParser parser) {
    super.deserializeAttributes(parser);
    String serialNumber = parser.getAttributeValue(null, XMLATTR_SERIAL_NUMBER);
    if (serialNumber != null) { // The device is USB attached
      setSerialNumber(SerialNumber.fromString(serialNumber));
      setPort(-1);
    } else { // The device is not USB attached
      // The port was already set in the call to super.deserializeAttributes(), so we just need to set the serial number
      setSerialNumber(SerialNumber.createFake());
    }
  }
}

