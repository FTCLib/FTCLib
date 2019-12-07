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

import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.DeviceManager;
import com.qualcomm.robotcore.util.GlobalWarningSource;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.internal.system.Assert;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class ReadXMLFileHandler extends ConfigurationUtility {
  public static final String TAG = "ReadXMLFileHandler";

  private XmlPullParser parser;
  private DeviceManager deviceManager;

  private static WarningManager warningManager;

  static {
    warningManager = new WarningManager();
    RobotLog.registerGlobalWarningSource(warningManager);
  }

  public ReadXMLFileHandler() {
    deviceManager = null;
  }

  public ReadXMLFileHandler(DeviceManager deviceManager) {
    this();
    this.deviceManager = deviceManager;
  }

  public static XmlPullParser xmlPullParserFromReader(Reader reader) {
    XmlPullParserFactory factory;
    XmlPullParser parser = null;
    try {
      factory = XmlPullParserFactory.newInstance();
      factory.setNamespaceAware(true);
      parser = factory.newPullParser();
      parser.setInput(reader);
    } catch (XmlPullParserException e) {
      e.printStackTrace();
    }
    return parser;
  }

  public List<ControllerConfiguration> parse(Reader reader) throws RobotCoreException {
    parser = xmlPullParserFromReader(reader);
    return parseDocument();
  }

  public List<ControllerConfiguration> parse(XmlPullParser parser) throws RobotCoreException {
    this.parser = parser;
    return parseDocument();
  }

  private List<ControllerConfiguration> parseDocument() throws RobotCoreException {
    warningManager.actuallyClearWarning();

    List<ControllerConfiguration> deviceControllers = null;
    try {
      int eventType = parser.getEventType();
      while (eventType != XmlPullParser.END_DOCUMENT) {
        if (eventType == XmlPullParser.START_TAG){
          String name = parser.getName();
          ConfigurationType configurationType = deform(name);
          if (configurationType == BuiltInConfigurationType.ROBOT) {
            deviceControllers = parseRobot();
          } else {
            parseIgnoreElementChildren();  // It's a start tag we don't know about; ignore that element entirely
          }
        }
        eventType = parser.next();
      } // hit end of document

    } catch (XmlPullParserException e) {
      RobotLog.w("XmlPullParserException");
      e.printStackTrace();
    } catch (IOException e) {
      RobotLog.w("IOException");
      e.printStackTrace();
    }

    if (deviceControllers == null) {
      deviceControllers = new ArrayList<ControllerConfiguration>();
    }

    addEmbeddedLynxModuleIfNecessary(deviceControllers);
    return deviceControllers;
  }

  private List<ControllerConfiguration> parseRobot() throws XmlPullParserException, IOException, RobotCoreException {
    Assert.assertTrue(parser.getEventType()==XmlPullParser.START_TAG && deform(parser.getName())==BuiltInConfigurationType.ROBOT);

    List<ControllerConfiguration> deviceControllers = new ArrayList<>();

    int eventType = parser.next();
    while (eventType != XmlPullParser.END_TAG) {
      if (eventType == XmlPullParser.START_TAG){
        String name = parser.getName();
        ConfigurationType configurationType = deform(name);

        ControllerConfiguration controllerConfiguration = null;

        if (configurationType == BuiltInConfigurationType.MOTOR_CONTROLLER) {
          controllerConfiguration = new MotorControllerConfiguration();

        }
        else if (configurationType == BuiltInConfigurationType.SERVO_CONTROLLER) {
          controllerConfiguration = new ServoControllerConfiguration();
        }
        else if (configurationType == BuiltInConfigurationType.LEGACY_MODULE_CONTROLLER) {
          controllerConfiguration = new LegacyModuleControllerConfiguration();
        }
        else if (configurationType == BuiltInConfigurationType.DEVICE_INTERFACE_MODULE) {
          controllerConfiguration = new DeviceInterfaceModuleConfiguration();
        }
        else if (configurationType == BuiltInConfigurationType.LYNX_USB_DEVICE) {
          controllerConfiguration = new LynxUsbDeviceConfiguration();
        }
        else if (configurationType == BuiltInConfigurationType.WEBCAM) {
          controllerConfiguration = new WebcamConfiguration();
        }
        else {
          parseIgnoreElementChildren();  // It's a start tag we don't know about; ignore that element entirely
        }
        if (controllerConfiguration != null) {
          controllerConfiguration.deserialize(parser, this);
          deviceControllers.add(controllerConfiguration);
        }
      }
      eventType = parser.next();
    }
    return deviceControllers;
  }

  /** If we are a device with an integrated / embedded lynx device (ie: a Rev Control Hub), make *sure*
   * that the serially connected "USB" device and the primary module are in the map */
  private void addEmbeddedLynxModuleIfNecessary(List<ControllerConfiguration> deviceControllers) {
    if (LynxConstants.isRevControlHub()) {
      for (ControllerConfiguration controllerConfiguration : deviceControllers) {
        if (LynxConstants.isEmbeddedSerialNumber(controllerConfiguration.getSerialNumber())) {
          // Ok, the 'usb' device is there, so we'll *assume* that the primary lynx module
          // is there too, as there's no reasonable way to remove same
          RobotLog.vv(TAG, "embedded lynx USB device is already present");
          return;
        }
      }
      // An embedded (serially connected) lynx module is absent. Make a new one.
      // The need for this typically occurs when we have no actual hardware configuration
      // that is active: all that we're parsing is the result of calling RobotConfigFile.getXmlNone(),
      // which is (currently at least) an entirely empty <Robot/>
      RobotLog.vv(TAG, "auto-configuring embedded lynx USB device");
      ControllerConfiguration controllerConfiguration = buildNewEmbeddedLynxUsbDevice(deviceManager);
      deviceControllers.add(controllerConfiguration);
    }
  }

  public List<ControllerConfiguration> parse(InputStream is) throws RobotCoreException {
    XmlPullParserFactory factory;
    parser = null;
    try {
      factory = XmlPullParserFactory.newInstance();
      factory.setNamespaceAware(true);
      parser = factory.newPullParser();
      parser.setInput(is, null);
    } catch (XmlPullParserException e) {
      e.printStackTrace();
    }
    return parseDocument();
  }

  public void onDeviceParsed(DeviceConfiguration device) {
    noteExistingName(device.getConfigurationType(), device.getName());
    handleDeprecation(device);
  }

  private void handleDeprecation(DeviceConfiguration device) {
    if (device.getConfigurationType().isDeprecated()) {
      // TODO(i18n): Convert to XML string
      warningManager.addWarning(String.format("%s is a deprecated configuration type and may be removed in a future release", device.getConfigurationType().getDisplayName(ConfigurationType.DisplayNameFlavor.Normal)));
    }

    else if (device.getConfigurationType() == BuiltInConfigurationType.LEGACY_MODULE_CONTROLLER) {
      // TODO(i18n): Convert to XML string
      warningManager.addWarning("The Legacy Module is illegal for competition and support may be removed from this app in a future release.");
    }
  }

  private void parseIgnoreElementChildren() throws IOException, XmlPullParserException {
    Assert.assertTrue(parser.getEventType() == XmlPullParser.START_TAG);
    int eventType = parser.next();
    while (eventType != XmlPullParser.END_TAG) {
      if (eventType == XmlPullParser.END_DOCUMENT) {
        return; // unexpected, shouldn't happen, but safely get out
      } else if (eventType == XmlPullParser.START_TAG) {
        parseIgnoreElementChildren(); // recurse!
      }
      eventType = parser.next();
    }
  }

  public static ConfigurationType deform(String xmlTag) {
    ConfigurationType result = null;
    if (xmlTag != null) {
      result = ConfigurationTypeManager.getInstance().configurationTypeFromTag(xmlTag);
    }
    return result;
  }

  private static class WarningManager implements GlobalWarningSource {
    private int warningMessageSuppressionCount = 0;
    private String warningMessage = "";

    private synchronized void actuallyClearWarning() {
      clearGlobalWarning();
      warningMessage = "";
    }

    private synchronized void addWarning(String newWarning) {
      if (warningMessage.isEmpty()) {
        warningMessage = newWarning;
      } else {
        warningMessage += String.format("; %s", newWarning);
      }
    }

    @Override
    public synchronized String getGlobalWarning() {
      return warningMessageSuppressionCount > 0 ? "" : warningMessage;
    }

    @Override
    synchronized public void suppressGlobalWarning(boolean suppress) {
      if (suppress)
        warningMessageSuppressionCount++;
      else
        warningMessageSuppressionCount--;
    }

    @Override
    public synchronized void setGlobalWarning(String warning) { /* Ignore */ }

    @Override
    public synchronized void clearGlobalWarning() {
      // We want the warning to survive robot restarts, so don't clear.
      warningMessageSuppressionCount = 0;
    }
  }
}