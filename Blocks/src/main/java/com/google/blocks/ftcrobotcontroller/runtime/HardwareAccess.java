/*
Copyright 2016 Google LLC.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.google.blocks.ftcrobotcontroller.runtime;

import com.google.blocks.ftcrobotcontroller.hardware.HardwareItem;
import com.google.blocks.ftcrobotcontroller.hardware.HardwareType;
import com.qualcomm.robotcore.hardware.HardwareDevice;
import com.qualcomm.robotcore.hardware.HardwareMap;

/**
 * An abstract class for classes that provides JavaScript access to a {@link HardwareDevice}.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
abstract class HardwareAccess<DEVICE_TYPE extends HardwareDevice> extends Access {
  protected final HardwareItem hardwareItem;
  protected final DEVICE_TYPE hardwareDevice;

  /**
   * Constructs a {@link HardwareAccess} for the given {@link HardwareItem}.
   */
  protected HardwareAccess(BlocksOpMode blocksOpMode, HardwareItem hardwareItem,
      HardwareMap hardwareMap, Class<DEVICE_TYPE> deviceType) {
    super(blocksOpMode, hardwareItem.identifier, hardwareItem.visibleName);
    this.hardwareItem = hardwareItem;

    DEVICE_TYPE hardwareDevice = null;
    try {
      hardwareDevice = hardwareMap.get(deviceType, hardwareItem.deviceName);
    } catch (Exception e) {
      // Determine if the deviceName is not present or if it is the wrong type.
      String message;
      try {
        hardwareMap.get(hardwareItem.deviceName);
        message = "The name \"" + hardwareItem.deviceName + "\" is present in the active " +
            "configuration, but it does not correspond to a " + deviceType.getSimpleName() + ".";
      } catch (Exception ee) {
        message = "The name \"" + hardwareItem.deviceName + "\" is not present in the active " +
            "configuration.";
      }
      reportHardwareError(message);
    }
    this.hardwareDevice = hardwareDevice;
  }

  /**
   * Creates a new {@link HardwareAccess} for the given {@link HardwareItem}.
   */
  static HardwareAccess newHardwareAccess(BlocksOpMode blocksOpMode,
      HardwareType hardwareType, HardwareMap hardwareMap, HardwareItem hardwareItem) {

    switch (hardwareType) {
      case ACCELERATION_SENSOR:
        return new AccelerationSensorAccess(blocksOpMode, hardwareItem, hardwareMap);
      case ANALOG_INPUT:
        return new AnalogInputAccess(blocksOpMode, hardwareItem, hardwareMap);
      case ANALOG_OUTPUT:
        return new AnalogOutputAccess(blocksOpMode, hardwareItem, hardwareMap);
      case BNO055IMU:
        return new BNO055IMUAccess(blocksOpMode, hardwareItem, hardwareMap);
      case COLOR_SENSOR:
        return new ColorSensorAccess(blocksOpMode, hardwareItem, hardwareMap);
      case COMPASS_SENSOR:
        return new CompassSensorAccess(blocksOpMode, hardwareItem, hardwareMap);
      case CR_SERVO:
        return new CRServoAccess(blocksOpMode, hardwareItem, hardwareMap);
      case DC_MOTOR:
        return new DcMotorAccess(blocksOpMode, hardwareItem, hardwareMap);
      case DIGITAL_CHANNEL:
        return new DigitalChannelAccess(blocksOpMode, hardwareItem, hardwareMap);
      case DISTANCE_SENSOR:
        return new DistanceSensorAccess(blocksOpMode, hardwareItem, hardwareMap);
      case GYRO_SENSOR:
        return new GyroSensorAccess(blocksOpMode, hardwareItem, hardwareMap);
      case IR_SEEKER_SENSOR:
        return new IrSeekerSensorAccess(blocksOpMode, hardwareItem, hardwareMap);
      case LED:
        return new LedAccess(blocksOpMode, hardwareItem, hardwareMap);
      case LIGHT_SENSOR:
        return new LightSensorAccess(blocksOpMode, hardwareItem, hardwareMap);
      case LYNX_I2C_COLOR_RANGE_SENSOR:
        return new LynxI2cColorRangeSensorAccess(blocksOpMode, hardwareItem, hardwareMap);
      case LYNX_MODULE:
        return null;
      case MR_I2C_COMPASS_SENSOR:
        return new MrI2cCompassSensorAccess(blocksOpMode, hardwareItem, hardwareMap);
      case MR_I2C_RANGE_SENSOR:
        return new MrI2cRangeSensorAccess(blocksOpMode, hardwareItem, hardwareMap);
      case OPTICAL_DISTANCE_SENSOR:
        return new OpticalDistanceSensorAccess(blocksOpMode, hardwareItem, hardwareMap);
      case REV_BLINKIN_LED_DRIVER:
        return new RevBlinkinLedDriverAccess(blocksOpMode, hardwareItem, hardwareMap);
      case SERVO:
        return new ServoAccess(blocksOpMode, hardwareItem, hardwareMap);
      case SERVO_CONTROLLER:
        return new ServoControllerAccess(blocksOpMode, hardwareItem, hardwareMap);
      case TOUCH_SENSOR:
        return new TouchSensorAccess(blocksOpMode, hardwareItem, hardwareMap);
      case ULTRASONIC_SENSOR:
        return new UltrasonicSensorAccess(blocksOpMode, hardwareItem, hardwareMap);
      case VOLTAGE_SENSOR:
        return new VoltageSensorAccess(blocksOpMode, hardwareItem, hardwareMap);
      case WEBCAM_NAME:
        return null;
    }
    throw new IllegalArgumentException("Unknown hardware type " + hardwareType);
  }
}
