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

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Xml;

import com.qualcomm.robotcore.exception.DuplicateNameException;
import com.qualcomm.robotcore.exception.RobotCoreException;

import org.firstinspires.ftc.robotcore.external.function.ThrowingRunnable;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;


public class WriteXMLFileHandler {

  private XmlSerializer serializer;
  private HashSet<String> names = new HashSet<String>();
  private List<String> duplicates = new ArrayList<String>();
  private String[] indentation = {"    ", "        ", "            "};
  private int indent = 0;

  public WriteXMLFileHandler(Context context) {
    serializer = Xml.newSerializer();
  }

  public String toXml(Collection<ControllerConfiguration> deviceControllerConfigurations) {
    return toXml(deviceControllerConfigurations, null, null);
  }

  public String toXml(Collection<ControllerConfiguration> deviceControllerConfigurations, @Nullable String attribute, @Nullable String attributeValue) {
    duplicates = new ArrayList<String>();
    names = new HashSet<String>();

    StringWriter writer = new StringWriter();
    try {
      serializer.setOutput(writer);
      serializer.startDocument("UTF-8", true);
      serializer.ignorableWhitespace("\n");
      serializer.startTag("", "Robot");
      if (attribute!= null) serializer.attribute("", attribute, attributeValue);
      serializer.ignorableWhitespace("\n");
      for (ControllerConfiguration controllerConfiguration : deviceControllerConfigurations) {

        ConfigurationType type = controllerConfiguration.getConfigurationType();
        if (type == BuiltInConfigurationType.MOTOR_CONTROLLER) {
          writeController((MotorControllerConfiguration)controllerConfiguration, true);
        } else if (type == BuiltInConfigurationType.SERVO_CONTROLLER) {
          writeController((ServoControllerConfiguration)controllerConfiguration, true);
        } else if (type == BuiltInConfigurationType.LEGACY_MODULE_CONTROLLER) {
          writeLegacyModuleController((LegacyModuleControllerConfiguration)controllerConfiguration);
        } else if (type == BuiltInConfigurationType.DEVICE_INTERFACE_MODULE) {
          writeDeviceInterfaceModule((DeviceInterfaceModuleConfiguration)controllerConfiguration);
        } else if (type == BuiltInConfigurationType.LYNX_USB_DEVICE) {
          writeLynxUSBDevice((LynxUsbDeviceConfiguration)controllerConfiguration);
        } else if (type == BuiltInConfigurationType.WEBCAM) {
          writeWebcam((WebcamConfiguration) controllerConfiguration);
        }
      }
      serializer.endTag("", "Robot");
      serializer.ignorableWhitespace("\n");
      serializer.endDocument();
      return writer.toString();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void checkForDuplicates(DeviceConfiguration config) {
    if (config.isEnabled()) {
      String name = config.getName();
      if (names.contains(name)){
        duplicates.add(name);
      } else {
        names.add(name);
      }
    }
  }

  private void writeDeviceInterfaceModule(final DeviceInterfaceModuleConfiguration controller) throws IOException {
    writeUsbController(controller, null, new ThrowingRunnable<IOException>() {
      @Override public void run() throws IOException {
          DeviceInterfaceModuleConfiguration deviceInterfaceModuleConfiguration = (DeviceInterfaceModuleConfiguration) controller;

          for (DeviceConfiguration device : deviceInterfaceModuleConfiguration.getPwmOutputs()) {
            writeDeviceNameAndPort(device);
          }

          for (DeviceConfiguration device : deviceInterfaceModuleConfiguration.getI2cDevices()) {
            writeDeviceNameAndPort(device);
          }

          for (DeviceConfiguration device : deviceInterfaceModuleConfiguration.getAnalogInputDevices()) {
            writeDeviceNameAndPort(device);
          }

          for (DeviceConfiguration device : deviceInterfaceModuleConfiguration.getDigitalDevices()) {
            writeDeviceNameAndPort(device);
          }

          for (DeviceConfiguration device : deviceInterfaceModuleConfiguration.getAnalogOutputDevices()) {
            writeDeviceNameAndPort(device);
          }
        }
      });
  }

  private void writeLegacyModuleController(final LegacyModuleControllerConfiguration controller) throws IOException {
    writeUsbController(controller, null, new ThrowingRunnable<IOException>() {
      @Override public void run() throws IOException {
          // step through the list of attached devices,
          for (DeviceConfiguration device : controller.getDevices()) {
            ConfigurationType type = device.getConfigurationType();
            if (type == BuiltInConfigurationType.MOTOR_CONTROLLER)        { writeController((MotorControllerConfiguration)device, false);
            } else if (type == BuiltInConfigurationType.SERVO_CONTROLLER) { writeController((ServoControllerConfiguration)device, false);
            } else if (type == BuiltInConfigurationType.MATRIX_CONTROLLER){ writeController((MatrixControllerConfiguration)device, false);
            } else {
              writeDeviceNameAndPort(device);
            }
          }
        }
      });
  }

  private void writeWebcam(final WebcamConfiguration controller) throws IOException {
    writeUsbController(controller, new ThrowingRunnable<IOException>() {
      @Override public void run() throws IOException {
        if (controller.getAutoOpen()) {
          // 'false' is the default; don't need to write
          serializer.attribute("", WebcamConfiguration.XMLATTR_AUTO_OPEN_CAMERA, String.valueOf(controller.getAutoOpen()));
        }
      }
    }, null);
  }

  private void writeLynxUSBDevice(final LynxUsbDeviceConfiguration controller) throws IOException {
    writeUsbController(controller,
      new ThrowingRunnable<IOException>() {
        @Override public void run() throws IOException {
          serializer.attribute("", LynxUsbDeviceConfiguration.XMLATTR_PARENT_MODULE_ADDRESS, Integer.toString(controller.getParentModuleAddress()));
          }
        },
      new ThrowingRunnable<IOException>() {
          @Override public void run() throws IOException {
            for (DeviceConfiguration device : controller.getDevices()) {
              ConfigurationType type = device.getConfigurationType();
              if (type == BuiltInConfigurationType.LYNX_MODULE) {
                writeController((LynxModuleConfiguration)device, false);
              } else {
                writeDeviceNameAndPort(device);
              }
            }
          }
        }
    );
  }

  private <CONTROLLER_T extends ControllerConfiguration<? extends DeviceConfiguration>>  void writeController(final CONTROLLER_T controller, final boolean isUsbDevice) throws IOException {
    writeNamedController(controller,
      new ThrowingRunnable<IOException>() {
        @Override
        public void run() throws IOException {
          if (isUsbDevice) {
            serializer.attribute("", ControllerConfiguration.XMLATTR_SERIAL_NUMBER, controller.getSerialNumber().getString());
          } else {
            serializer.attribute("", DeviceConfiguration.XMLATTR_PORT, String.valueOf(controller.getPort())); // for lynx modules, 'port' is 'moduleAddress'
          }
        }
      },
      new ThrowingRunnable<IOException>() {
        @Override
        public void run() throws IOException {
          if (controller.getConfigurationType() == BuiltInConfigurationType.LYNX_MODULE) {
            LynxModuleConfiguration moduleConfiguration = (LynxModuleConfiguration) controller;

            for (DeviceConfiguration device : moduleConfiguration.getMotors()) {
              writeDeviceNameAndPort(device);
            }
            for (DeviceConfiguration device : moduleConfiguration.getServos()) {
              writeDeviceNameAndPort(device);
            }
            for (DeviceConfiguration device : moduleConfiguration.getAnalogInputs()) {
              writeDeviceNameAndPort(device);
            }
            for (DeviceConfiguration device : moduleConfiguration.getPwmOutputs()) {
              writeDeviceNameAndPort(device);
            }
            for (DeviceConfiguration device : moduleConfiguration.getDigitalDevices()) {
              writeDeviceNameAndPort(device);
            }
            for (DeviceConfiguration device : moduleConfiguration.getI2cDevices()) {
              writeDeviceNameAndPort(device);
            }
          }

          else if(controller.getConfigurationType() == BuiltInConfigurationType.MATRIX_CONTROLLER) {
            for (DeviceConfiguration device : ((MatrixControllerConfiguration) controller).getMotors()) {
              writeDeviceNameAndPort(device);
            }
            for (DeviceConfiguration device : ((MatrixControllerConfiguration) controller).getServos()) {
              writeDeviceNameAndPort(device);
            }
          }

          else {
            for (DeviceConfiguration device : controller.getDevices()) {
              writeDeviceNameAndPort(device);
            }
          }
        }
      }
    );
  }

  // Emits an XML element for the device that has name and port attributes
  private void writeDeviceNameAndPort(final DeviceConfiguration device) throws IOException {
    if (!device.isEnabled()) {
      return;
    }
    writeDevice(device, new ThrowingRunnable<IOException>() {
      @Override public void run() throws IOException {
        device.serializeXmlAttributes(serializer);
      }
    }, null);
  }

  private void writeUsbController(final ControllerConfiguration controller, @Nullable final ThrowingRunnable<IOException> handleAttributes, @Nullable ThrowingRunnable<IOException> handleChildren) throws IOException {
    writeNamedController(controller, new ThrowingRunnable<IOException>() {
      @Override public void run() throws IOException {
        serializer.attribute("", ControllerConfiguration.XMLATTR_SERIAL_NUMBER, controller.getSerialNumber().getString());
        if (handleAttributes != null) { handleAttributes.run(); }
      }
    }, handleChildren);
  }

  private void writeNamedController(final ControllerConfiguration controller, final @Nullable ThrowingRunnable<IOException> handleAttributes, final @Nullable ThrowingRunnable<IOException> handleChildren) throws IOException {
    writeDevice(controller, new ThrowingRunnable<IOException>() {
      @Override public void run() throws IOException {
        serializer.attribute("", DeviceConfiguration.XMLATTR_NAME, controller.getName());
        if (handleAttributes != null) { handleAttributes.run(); }
      }
    }, handleChildren);
  }

  private void writeDevice(DeviceConfiguration deviceConfiguration, @Nullable ThrowingRunnable<IOException> handleAttributes, @Nullable ThrowingRunnable<IOException> handleChildren) throws IOException {
    /** TODO: should we check for isEnabled() here instead of only in {@link #writeDeviceNameAndPort(DeviceConfiguration)} */
    serializer.ignorableWhitespace(indentation[indent]);
    serializer.startTag("", conform(deviceConfiguration.getConfigurationType()));
    checkForDuplicates(deviceConfiguration);
    if (handleAttributes != null) { handleAttributes.run(); }
    if (handleChildren != null) {
      serializer.ignorableWhitespace("\n");
      indent++;
      handleChildren.run();
      indent--;
      serializer.ignorableWhitespace(indentation[indent]);
    }
    serializer.endTag("", conform(deviceConfiguration.getConfigurationType()));
    serializer.ignorableWhitespace("\n");
  }

  public void writeToFile(String data, File folder, String filenameWithExt) throws RobotCoreException, IOException {
    if (duplicates.size() > 0) {
      throw new DuplicateNameException("Duplicate names: " + duplicates);
    }

    boolean success = true;

    if (!folder.exists()) {
      success = folder.mkdir();
    }
    if (success) {
      File file = new File(folder, filenameWithExt);
      FileOutputStream stream = null;
      try {
        stream = new FileOutputStream(file);
        stream.write(data.getBytes());
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        try {
          stream.close();
        } catch (IOException e) {
          // Auto-generated catch block
          e.printStackTrace();
        }
      }
    } else {
      throw new RobotCoreException("Unable to create directory");
    }
  }

  private String conform(ConfigurationType type) {
    return type.getXmlTag();
  }
}