// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.hardware;

import com.qualcomm.ftccommon.configuration.RobotConfigFile;
import com.qualcomm.ftccommon.configuration.RobotConfigFileManager;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.HardwareDevice;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.configuration.ControllerConfiguration;
import com.qualcomm.robotcore.hardware.configuration.DeviceConfiguration;
import com.qualcomm.robotcore.hardware.configuration.DeviceInterfaceModuleConfiguration;
import com.qualcomm.robotcore.hardware.configuration.LynxModuleConfiguration;
import com.qualcomm.robotcore.hardware.configuration.MatrixControllerConfiguration;
import com.qualcomm.robotcore.hardware.configuration.MotorControllerConfiguration;
import com.qualcomm.robotcore.hardware.configuration.ReadXMLFileHandler;
import com.qualcomm.robotcore.hardware.configuration.ServoControllerConfiguration;
import com.qualcomm.robotcore.util.RobotLog;

import org.xmlpull.v1.XmlPullParser;

import java.io.Reader;
import java.util.*;

/**
 * A class that maps the supported hardware types to lists of specific hardware items.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
public class HardwareItemMap {
  private final Map<HardwareType, List<HardwareItem>> map =
      new TreeMap<HardwareType, List<HardwareItem>>();

  private final Set<DeviceConfiguration> devices = new HashSet<DeviceConfiguration>();

  /**
   * Creates a new {@link HardwareItemMap} with the supported hardware items in the active configuration.
   */
  public static HardwareItemMap newHardwareItemMap() {
    try {
      RobotConfigFileManager robotConfigFileManager = new RobotConfigFileManager();
      RobotConfigFile activeConfig = robotConfigFileManager.getActiveConfig();
      XmlPullParser pullParser = activeConfig.getXml();
      try {
        return new HardwareItemMap(pullParser);
      } finally {
        // TODO: it would be good to close the file behind the pull parser, rather than waiting for the finalizer
      }
    } catch (Exception e) {
      RobotLog.logStackTrace(e);
      return new HardwareItemMap();
    }
  }

  /**
   * Creates a new {@link HardwareItemMap} with the supported hardware items in the given
   * {@link HardwareMap}.
   */
  public static HardwareItemMap newHardwareItemMap(HardwareMap hardwareMap) {
    return new HardwareItemMap(hardwareMap);
  }

  /**
   * Constructs a {@link HardwareItemMap}.
   */
  // visible for testing
  HardwareItemMap() {
  }

  /**
   * Constructs a {@link HardwareItemMap} with the supported hardware items in the given
   * configuration {@link XmlPullParser}.
   */
  private HardwareItemMap(XmlPullParser pullParser) {
    try {
      ReadXMLFileHandler readXMLFileHandler = new ReadXMLFileHandler();
      HardwareItem parent = null;
      for (ControllerConfiguration controllerConfiguration : readXMLFileHandler.parse(pullParser)) {
        addDevice(parent, controllerConfiguration);
      }
    } catch (RobotCoreException e) {
      RobotLog.logStackTrace(e);
    }
  }

  /**
   * Constructs a {@link HardwareItemMap} with the supported hardware items in the given
   * configuration {@link Reader}.
   */
  // visible for testing
  HardwareItemMap(Reader reader) {
    try {
      ReadXMLFileHandler readXMLFileHandler = new ReadXMLFileHandler();
      HardwareItem parent = null;
      for (ControllerConfiguration controllerConfiguration : readXMLFileHandler.parse(reader)) {
        addDevice(parent, controllerConfiguration);
      }
    } catch (RobotCoreException e) {
      RobotLog.logStackTrace(e);
    }
  }

  /**
   * Constructs a {@link HardwareItemMap} with the supported hardware items in the given
   * {@link HardwareMap}.
   */
  private HardwareItemMap(HardwareMap hardwareMap) {
    HardwareItem parent = null;
    for (HardwareType hardwareType : HardwareType.values()) {
      List<HardwareDevice> devices = hardwareMap.getAll(hardwareType.deviceType);
      for (HardwareDevice device : devices) {
        // Having multiple names for a single device is confusing in our UI here, so we pick
        // one arbitrarily. Note that this virtually never actually happens in practice; the
        // one current (Sept '16) occurrence involves Matrix motor and servo controllers.
        List<String> deviceNames = new ArrayList<String>(hardwareMap.getNamesOf(device));
        if (!deviceNames.isEmpty()) {
          Collections.sort(deviceNames, new Comparator<String>() {
            @Override public int compare(String lhs, String rhs) {
              // sort first by length (shortest first) and second by content
              int result = lhs.length() - rhs.length();
              if (result == 0) {
                result = lhs.compareToIgnoreCase(rhs);
              }
              return result;
            }
          });
          addHardwareItem(parent, hardwareType, deviceNames.get(0));
        }
      }
    }
  }

  /**
   * Adds the given {@link ControllerConfiguration}, as well as any devices belonging to the
   * controller, to the given HardwareItemMap.
   */
  private void addController(HardwareItem parent, ControllerConfiguration<? extends DeviceConfiguration> controllerConfiguration) {
    for (DeviceConfiguration deviceConfiguration : controllerConfiguration.getDevices()) {
      addDevice(parent, deviceConfiguration);
    }
    if (controllerConfiguration instanceof DeviceInterfaceModuleConfiguration) {
      DeviceInterfaceModuleConfiguration deviceInterfaceModuleConfiguration =
          (DeviceInterfaceModuleConfiguration) controllerConfiguration;
      for (DeviceConfiguration deviceConfiguration :
          deviceInterfaceModuleConfiguration.getPwmOutputs()) {
        addDevice(parent, deviceConfiguration);
      }
      for (DeviceConfiguration deviceConfiguration :
          deviceInterfaceModuleConfiguration.getI2cDevices()) {
        addDevice(parent, deviceConfiguration);
      }
      for (DeviceConfiguration deviceConfiguration :
          deviceInterfaceModuleConfiguration.getAnalogInputDevices()) {
        addDevice(parent, deviceConfiguration);
      }
      for (DeviceConfiguration deviceConfiguration :
          deviceInterfaceModuleConfiguration.getDigitalDevices()) {
        addDevice(parent, deviceConfiguration);
      }
      for (DeviceConfiguration deviceConfiguration :
          deviceInterfaceModuleConfiguration.getAnalogOutputDevices()) {
        addDevice(parent, deviceConfiguration);
      }
    }
    if (controllerConfiguration instanceof MatrixControllerConfiguration) {
      MatrixControllerConfiguration matrixControllerConfiguration =
          (MatrixControllerConfiguration) controllerConfiguration;
      for (DeviceConfiguration deviceConfiguration : matrixControllerConfiguration.getServos()) {
        addDevice(parent, deviceConfiguration);
      }
      for (DeviceConfiguration deviceConfiguration : matrixControllerConfiguration.getMotors()) {
        addDevice(parent, deviceConfiguration);
      }
    }
    if (controllerConfiguration instanceof MotorControllerConfiguration) {
      MotorControllerConfiguration motorControllerConfiguration =
          (MotorControllerConfiguration) controllerConfiguration;
      for (DeviceConfiguration motorConfiguration : motorControllerConfiguration.getMotors()) {
        addDevice(parent, motorConfiguration);
      }
    }
    if (controllerConfiguration instanceof ServoControllerConfiguration) {
      ServoControllerConfiguration servoControllerConfiguration =
          (ServoControllerConfiguration) controllerConfiguration;
      for (DeviceConfiguration deviceConfiguration : servoControllerConfiguration.getServos()) {
        addDevice(parent, deviceConfiguration);
      }
    }
    if (controllerConfiguration instanceof LynxModuleConfiguration) {
      LynxModuleConfiguration lynxModuleConfiguration =
          (LynxModuleConfiguration) controllerConfiguration;
      for (DeviceConfiguration deviceConfiguration : lynxModuleConfiguration.getServos()) {
        addDevice(parent, deviceConfiguration);
      }
      for (DeviceConfiguration deviceConfiguration : lynxModuleConfiguration.getMotors()) {
        addDevice(parent, deviceConfiguration);
      }
      for (DeviceConfiguration deviceConfiguration : lynxModuleConfiguration.getAnalogInputs()) {
        addDevice(parent, deviceConfiguration);
      }
      for (DeviceConfiguration deviceConfiguration : lynxModuleConfiguration.getPwmOutputs()) {
        addDevice(parent, deviceConfiguration);
      }
      for (DeviceConfiguration deviceConfiguration : lynxModuleConfiguration.getI2cDevices()) {
        addDevice(parent, deviceConfiguration);
      }
      for (DeviceConfiguration deviceConfiguration : lynxModuleConfiguration.getDigitalDevices()) {
        addDevice(parent, deviceConfiguration);
      }
    }
  }

  /**
   * Adds the given {@link DeviceConfiguration} to the given HardwareItemMap.
   */
  private void addDevice(HardwareItem parent, DeviceConfiguration deviceConfiguration) {
    // Use a set to prevent duplicates. Duplicates can occur if a controller returns the same
    // devices in getDevices() as it does in getMotors, getServos, etc.
    if (devices.add(deviceConfiguration)) {
      if (deviceConfiguration.isEnabled()) {
        // If deviceConfiguration is a controller, but there's no corresponding HardwareType that
        // is a container, use the given parent as the container of the controller's children.
        // This is the case with DcMotorController. Blocks (so far) doesn't have a HardwareType for
        // DcMotorController.
        HardwareItem container = parent;
        for (HardwareType hardwareType : HardwareUtil.getHardwareTypes(deviceConfiguration)) {
          HardwareItem harwareItem = addHardwareItem(parent, hardwareType, deviceConfiguration.getName());
          if (hardwareType.isContainer()) {
            // Use this hardwareItem as the container for the controller's children.
            container = harwareItem;
          }
        }
        if (deviceConfiguration instanceof ControllerConfiguration) {
          addController(container, (ControllerConfiguration<? extends DeviceConfiguration>) deviceConfiguration);
        }
      }
    }
  }

  /**
   * Adds a {@link HardwareItem} to the given HardwareItemMap.
   */
  // visible for testing
  HardwareItem addHardwareItem(HardwareItem parent, HardwareType hardwareType, String deviceName) {
    if (deviceName.isEmpty()) {
      RobotLog.w("Blocks cannot support a hardware device (" +
          hardwareType.deviceType.getSimpleName() + ") whose name is empty.");
      return null;
    }
    List<HardwareItem> hardwareItemList = map.get(hardwareType);
    if (hardwareItemList == null) {
      hardwareItemList = new ArrayList<HardwareItem>();
      map.put(hardwareType, hardwareItemList);
    }
    // paranoia: avoid theoretically possible exact duplicates
    for (HardwareItem item : hardwareItemList) {
      if (item.deviceName.equals(deviceName)) {
        return null; // we already have this item
      }
    }
    HardwareItem hardwareItem = new HardwareItem(parent, hardwareType, deviceName);
    hardwareItemList.add(hardwareItem);
    return hardwareItem;
  }

  /**
   * Returns the number of {@link HardwareType}s stored in this HardwareItemMap.
   */
  public int getHardwareTypeCount() {
    return map.size();
  }

  /**
   * Returns true of this HardwareItemMap contains the given {@link HardwareType}.
   */
  public boolean contains(HardwareType hardwareType) {
    return map.containsKey(hardwareType);
  }

  /**
   * Returns a list of {@link HardwareItem}s for the given {@link HardwareType}.
   */
  public List<HardwareItem> getHardwareItems(HardwareType hardwareType) {
    return map.containsKey(hardwareType)
        ? Collections.<HardwareItem>unmodifiableList(map.get(hardwareType))
        : Collections.<HardwareItem>emptyList();
  }

  /**
   * Returns a list of all {@link HardwareItem}s in this HardwareItemMap, sorted by identifiers.
   */
  public Iterable<HardwareItem> getAllHardwareItems() {
    List<HardwareItem> list = new ArrayList<HardwareItem>();
    for (List<HardwareItem> hardwareItems : map.values()) {
      list.addAll(hardwareItems);
    }
    Collections.sort(list, new Comparator<HardwareItem>() {
      @Override public int compare(HardwareItem a, HardwareItem b) {
        return a.identifier.compareTo(b.identifier);
      }
    });
    return Collections.unmodifiableList(list);
  }

  /**
   * Returns a set of {@link HardwareType}s in this HardwareItemMap.
   */
  public Set<HardwareType> getHardwareTypes() {
    return Collections.unmodifiableSet(map.keySet());
  }

  // java.lang.Object methods

  @Override
  public boolean equals(Object o) {
    if (o instanceof HardwareItemMap) {
      HardwareItemMap that = (HardwareItemMap) o;
      return this.map.equals(that.map);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return map.hashCode();
  }
}
