/*
Copyright (c) 2017 Robert Atkinson

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of Robert Atkinson nor the names of his contributors may be used to
endorse or promote products derived from this software without specific prior
written permission.

NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.qualcomm.robotcore.hardware.configuration;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import com.qualcomm.robotcore.R;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.DeviceManager;
import com.qualcomm.robotcore.hardware.LynxModuleMeta;
import com.qualcomm.robotcore.hardware.LynxModuleMetaList;
import com.qualcomm.robotcore.hardware.RobotCoreLynxUsbDevice;
import com.qualcomm.robotcore.hardware.configuration.typecontainers.I2cDeviceConfigurationType;
import com.qualcomm.robotcore.hardware.configuration.typecontainers.MotorConfigurationType;
import com.qualcomm.robotcore.hardware.configuration.typecontainers.ServoConfigurationType;
import com.qualcomm.robotcore.hardware.configuration.typecontainers.UserConfigurationType;
import com.qualcomm.robotcore.util.Device;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.SerialNumber;

import org.firstinspires.ftc.robotcore.external.function.Supplier;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.system.Assert;
import org.firstinspires.ftc.robotcore.internal.system.Misc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * {@link ConfigurationUtility} is a utility class containing methods for construction and
 * initializing various types of {@link DeviceConfiguration}. This is used in two contexts:
 * constructing entirely new XML configurations in the configuration UI, and during reading
 * of an existing XML configuration, where it's used to reconstitute fully flushed out
 * configurations, since we only *store* the parts of a configuration which are actually
 * populated.
 */
@SuppressWarnings("WeakerAccess")
public class ConfigurationUtility
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "ConfigurationUtility";
    public static final int firstNamedDeviceNumber = 1;

    protected Set<String> existingNames;

    public ConfigurationUtility()
        {
        resetNameUniquifiers();
        }

    //----------------------------------------------------------------------------------------------
    // Name management
    //----------------------------------------------------------------------------------------------

    public void resetNameUniquifiers()
        {
        existingNames = new HashSet<String>();
        }

    /** Historical policy is that all names are in a single flat name space. We could perhaps
     * change that, and it might be an improvement, but that hasn't yet been done. */
    public Set<String> getExistingNames(ConfigurationType configurationType)
        {
        return existingNames;
        }

    protected void noteExistingName(ConfigurationType configurationType, String name)
        {
        getExistingNames(configurationType).add(name);
        }

    // TODO: the resId should be retreived from configurationType
    protected String createUniqueName(ConfigurationType configurationType, @StringRes int resId)
        {
        return createUniqueName(configurationType, AppUtil.getDefContext().getString(resId), firstNamedDeviceNumber);
        }

    protected String createUniqueName(ConfigurationType configurationType, @StringRes int resId, int preferredParam)
        {
        return createUniqueName(configurationType, AppUtil.getDefContext().getString(resId), preferredParam);
        }

    protected String createUniqueName(ConfigurationType configurationType, String format, int preferredParam)
        {
        return createUniqueName(configurationType, null, format, preferredParam);
        }

    protected String createUniqueName(ConfigurationType configurationType, @Nullable String firstChoice, @NonNull String format, int preferredParam)
        {
        Set<String> existing = getExistingNames(configurationType);
        if (firstChoice != null)
            {
            if (!existing.contains(firstChoice))
                {
                noteExistingName(configurationType, firstChoice);
                return firstChoice;
                }
            }
        String candidate = Misc.formatForUser(format, preferredParam);
        if (!existing.contains(candidate))
            {
            noteExistingName(configurationType, candidate);
            return candidate;
            }
        for (int i = firstNamedDeviceNumber; ; i++)
            {
            candidate = Misc.formatForUser(format, i);
            if (!existing.contains(candidate))
                {
                noteExistingName(configurationType, candidate);
                return candidate;
                }
            }
        }

    //----------------------------------------------------------------------------------------------
    // Building *new* configurations
    //----------------------------------------------------------------------------------------------

    public ControllerConfiguration buildNewControllerConfiguration(SerialNumber serialNumber, DeviceManager.UsbDeviceType deviceType, Supplier<LynxModuleMetaList> lynxModuleSupplier)
        {
        ControllerConfiguration result = null;
        switch (deviceType)
            {
            case MODERN_ROBOTICS_USB_DC_MOTOR_CONTROLLER:       result = buildNewModernMotorController(serialNumber); break;
            case MODERN_ROBOTICS_USB_SERVO_CONTROLLER:          result = buildNewModernServoController(serialNumber); break;
            case MODERN_ROBOTICS_USB_LEGACY_MODULE:             result = buildNewLegacyModule(serialNumber); break;
            case MODERN_ROBOTICS_USB_DEVICE_INTERFACE_MODULE:   result = buildNewDeviceInterfaceModule(serialNumber); break;
            case WEBCAM:                                        result = buildNewWebcam(serialNumber); break;
            case LYNX_USB_DEVICE:                               result = buildNewLynxUsbDevice(serialNumber, lynxModuleSupplier); break;
            }
        return result;
        }

    // TODO(Noah): Look into making it so that lists are never passed into DeviceConfiguration constructors
    // This would let us get rid of most of the buildEmptyX calls
    public MotorControllerConfiguration buildNewModernMotorController(SerialNumber serialNumber)
        {
        List<DeviceConfiguration> motors = buildEmptyMotors(ModernRoboticsConstants.INITIAL_MOTOR_PORT, ModernRoboticsConstants.NUMBER_OF_MOTORS);
        String name = createUniqueName(BuiltInConfigurationType.MOTOR_CONTROLLER, R.string.counted_motor_controller_name);
        MotorControllerConfiguration motorController = new MotorControllerConfiguration(name, motors, serialNumber);
        return motorController;
        }

    public ServoControllerConfiguration buildNewModernServoController(SerialNumber serialNumber)
        {
        List<DeviceConfiguration> servos = buildEmptyServos(ModernRoboticsConstants.INITIAL_SERVO_PORT, ModernRoboticsConstants.NUMBER_OF_SERVOS);
        String name = createUniqueName(BuiltInConfigurationType.SERVO_CONTROLLER, R.string.counted_servo_controller_name);
        ServoControllerConfiguration servoController = new ServoControllerConfiguration(name, servos, serialNumber);
        return servoController;
        }

    public DeviceInterfaceModuleConfiguration buildNewDeviceInterfaceModule(SerialNumber serialNumber)
        {
        String name = createUniqueName(BuiltInConfigurationType.DEVICE_INTERFACE_MODULE, R.string.counted_device_interface_module_name);
        DeviceInterfaceModuleConfiguration deviceInterfaceModule = new DeviceInterfaceModuleConfiguration(name, serialNumber);
        deviceInterfaceModule.setPwmOutputs         (buildEmptyDevices(0, ModernRoboticsConstants.NUMBER_OF_PWM_CHANNELS,   BuiltInConfigurationType.PULSE_WIDTH_DEVICE));
        deviceInterfaceModule.setI2cDevices         (buildEmptyDevices(0, ModernRoboticsConstants.NUMBER_OF_I2C_CHANNELS,   BuiltInConfigurationType.I2C_DEVICE));
        deviceInterfaceModule.setAnalogInputDevices (buildEmptyDevices(0, ModernRoboticsConstants.NUMBER_OF_ANALOG_INPUTS,  BuiltInConfigurationType.NOTHING));
        deviceInterfaceModule.setDigitalDevices     (buildEmptyDevices(0, ModernRoboticsConstants.NUMBER_OF_DIGITAL_IOS,    BuiltInConfigurationType.NOTHING));
        deviceInterfaceModule.setAnalogOutputDevices(buildEmptyDevices(0, ModernRoboticsConstants.NUMBER_OF_ANALOG_OUTPUTS, BuiltInConfigurationType.NOTHING));
        return deviceInterfaceModule;
        }

    public LegacyModuleControllerConfiguration buildNewLegacyModule(SerialNumber serialNumber)
        {
        List<DeviceConfiguration> legacies = new ArrayList<DeviceConfiguration>();
        for (int i = 0; i < ModernRoboticsConstants.NUMBER_OF_LEGACY_MODULE_PORTS; i++)
            {
            DeviceConfiguration module = new DeviceConfiguration(i + 0, BuiltInConfigurationType.NOTHING);
            legacies.add(module);
            }
        String name = createUniqueName(BuiltInConfigurationType.LEGACY_MODULE_CONTROLLER, R.string.counted_legacy_module_name);
        LegacyModuleControllerConfiguration legacyModule = new LegacyModuleControllerConfiguration(name, legacies, serialNumber);
        return legacyModule;
        }

    public WebcamConfiguration buildNewWebcam(SerialNumber serialNumber)
        {
        String name = createUniqueName(BuiltInConfigurationType.WEBCAM, R.string.counted_camera_name);
        return new WebcamConfiguration(name, serialNumber);
        }

    public LynxUsbDeviceConfiguration buildNewLynxUsbDevice(SerialNumber serialNumber, Supplier<LynxModuleMetaList> lynxModuleMetaListSupplier)
        {
        return buildNewLynxUsbDevice(serialNumber, lynxModuleMetaListSupplier.get());
        }
    public LynxUsbDeviceConfiguration buildNewLynxUsbDevice(SerialNumber serialNumber, LynxModuleMetaList metas)
        {
        RobotLog.vv(TAG, "buildNewLynxUsbDevice(%s)...", serialNumber);
        try {
            if (metas == null) metas = new LynxModuleMetaList(serialNumber);
            RobotLog.vv(TAG, "buildLynxUsbDevice(): discovered lynx modules: %s", metas);
            //
            List<LynxModuleConfiguration> modules = new LinkedList<LynxModuleConfiguration>();
            for (LynxModuleMeta meta : metas)
                {
                modules.add(buildNewLynxModule(meta.getModuleAddress(), meta.isParent(), true));
                }
            DeviceConfiguration.sortByName(modules);
            RobotLog.vv(TAG, "buildNewLynxUsbDevice(%s): %d modules", serialNumber, modules.size());

            String name = createUniqueName(BuiltInConfigurationType.LYNX_USB_DEVICE, R.string.counted_lynx_usb_device_name);
            LynxUsbDeviceConfiguration result = new LynxUsbDeviceConfiguration(name, modules, serialNumber);
            return result;
            }
        finally
            {
            RobotLog.vv(TAG, "...buildNewLynxUsbDevice(%s): ", serialNumber);
            }
        }

    public LynxModuleConfiguration buildNewLynxModule(int moduleAddress, boolean isParent, boolean isEnabled)
        {
        // Lynx Modules have a built-in uniquifier: their module address, so we use that as the uniquifier of choice
        String name = createUniqueName(BuiltInConfigurationType.LYNX_MODULE, R.string.counted_lynx_module_name, moduleAddress);
        LynxModuleConfiguration lynxModuleConfiguration = buildEmptyLynxModule(name, moduleAddress, isParent, isEnabled);

        // Add add the embedded IMU device to the newly created configuration
        Context context = AppUtil.getDefContext();
        UserConfigurationType embeddedIMUConfigurationType = I2cDeviceConfigurationType.getLynxEmbeddedIMUType();
        Assert.assertTrue(embeddedIMUConfigurationType!=null && embeddedIMUConfigurationType.isDeviceFlavor(ConfigurationType.DeviceFlavor.I2C));

        String imuName = createUniqueName(embeddedIMUConfigurationType, context.getString(R.string.preferred_imu_name), context.getString(R.string.counted_imu_name), 1);
        LynxI2cDeviceConfiguration imuConfiguration = new LynxI2cDeviceConfiguration();
        imuConfiguration.setConfigurationType(embeddedIMUConfigurationType);
        imuConfiguration.setName(imuName);
        imuConfiguration.setEnabled(true);
        imuConfiguration.setBus(LynxConstants.EMBEDDED_IMU_BUS);
        lynxModuleConfiguration.getI2cDevices().add(imuConfiguration);

        return lynxModuleConfiguration;
        }

    /** The distinguishing feature of the 'embedded' Lynx USB device is that it is labelled as
     * 'system synthetic', in that we make it up, making sure it's there independent of whether
     * it's been configured in the hardware map or not. We do this so that we can ALWAYS access
     * the functionality of that embedded module, such as its LEDs. */
    protected LynxUsbDeviceConfiguration buildNewEmbeddedLynxUsbDevice(final @NonNull DeviceManager deviceManager)
        {
        LynxUsbDeviceConfiguration controllerConfiguration = buildNewLynxUsbDevice(LynxConstants.SERIAL_NUMBER_EMBEDDED, new Supplier<LynxModuleMetaList>()
            {
            @Override public LynxModuleMetaList get()
                {
                RobotCoreLynxUsbDevice device = null;
                try {
                    device = deviceManager.createLynxUsbDevice(LynxConstants.SERIAL_NUMBER_EMBEDDED, null);
                    return device.discoverModules();
                    }
                catch (InterruptedException e)
                    {
                    Thread.currentThread().interrupt();
                    }
                catch (RobotCoreException e)
                    {
                    RobotLog.ee(TAG, e, "exception in buildNewEmbeddedLynxUsbDevice()");
                    }
                finally
                    {
                    if (device != null) device.close();
                    }
                return null;
                }
            });

        controllerConfiguration.setEnabled(true);
        controllerConfiguration.setSystemSynthetic(true);
        for (LynxModuleConfiguration moduleConfiguration : controllerConfiguration.getModules())
            {
            moduleConfiguration.setSystemSynthetic(true);
            }

        return controllerConfiguration;
        }

    //----------------------------------------------------------------------------------------------
    // Supporting methods
    //----------------------------------------------------------------------------------------------

    protected static List<DeviceConfiguration> buildEmptyDevices(int initialPort, int size, ConfigurationType type)
        {
        ArrayList<DeviceConfiguration> list = new ArrayList<DeviceConfiguration>();
        for (int i = 0; i < size; i++)
            {
            list.add(new DeviceConfiguration(i + initialPort, type, DeviceConfiguration.DISABLED_DEVICE_NAME, false));
            }
        return list;
        }

    public static List<DeviceConfiguration> buildEmptyMotors(int initialPort, int size)
        {
        return buildEmptyDevices(initialPort, size, MotorConfigurationType.getUnspecifiedMotorType());
        }

    public static List<DeviceConfiguration> buildEmptyServos(int initialPort, int size)
        {
        return buildEmptyDevices(initialPort, size, ServoConfigurationType.getStandardServoType());
        }

    protected LynxModuleConfiguration buildEmptyLynxModule(String name, int moduleAddress, boolean isParent, boolean isEnabled)
        {
        RobotLog.vv(TAG, "buildEmptyLynxModule() mod#=%d...", moduleAddress);
        //
        noteExistingName(BuiltInConfigurationType.LYNX_MODULE, name);
        //
        LynxModuleConfiguration moduleConfiguration = new LynxModuleConfiguration(name);
        moduleConfiguration.setModuleAddress(moduleAddress);
        moduleConfiguration.setIsParent(isParent);
        moduleConfiguration.setEnabled(isEnabled);
        //
        RobotLog.vv(TAG, "...buildEmptyLynxModule() mod#=%d", moduleAddress);
        return moduleConfiguration;
        }

    }
