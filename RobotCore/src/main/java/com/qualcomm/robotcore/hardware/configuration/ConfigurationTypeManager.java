/*
Copyright (c) 2016 Robert Atkinson, Noah Andrews

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

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import com.qualcomm.robotcore.hardware.ControlSystem;
import com.qualcomm.robotcore.hardware.HardwareDevice;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.configuration.annotations.AnalogSensorType;
import com.qualcomm.robotcore.hardware.configuration.annotations.DeviceProperties;
import com.qualcomm.robotcore.hardware.configuration.annotations.DigitalIoDeviceType;
import com.qualcomm.robotcore.hardware.configuration.annotations.ExpansionHubPIDFPositionParams;
import com.qualcomm.robotcore.hardware.configuration.annotations.ExpansionHubPIDFVelocityParams;
import com.qualcomm.robotcore.hardware.configuration.annotations.I2cDeviceType;
import com.qualcomm.robotcore.hardware.configuration.annotations.ServoType;
import com.qualcomm.robotcore.hardware.configuration.typecontainers.AnalogSensorConfigurationType;
import com.qualcomm.robotcore.hardware.configuration.typecontainers.DigitalIoDeviceConfigurationType;
import com.qualcomm.robotcore.hardware.configuration.typecontainers.I2cDeviceConfigurationType;
import com.qualcomm.robotcore.hardware.configuration.typecontainers.InstantiableUserConfigurationType;
import com.qualcomm.robotcore.hardware.configuration.typecontainers.MotorConfigurationType;
import com.qualcomm.robotcore.hardware.configuration.typecontainers.ServoConfigurationType;
import com.qualcomm.robotcore.hardware.configuration.typecontainers.UserConfigurationType;
import com.qualcomm.robotcore.robocol.Command;
import com.qualcomm.robotcore.util.ClassUtil;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.Util;

import org.firstinspires.ftc.robotcore.external.Predicate;
import org.firstinspires.ftc.robotcore.internal.network.NetworkConnectionHandler;
import org.firstinspires.ftc.robotcore.internal.network.RobotCoreCommandList;
import org.firstinspires.ftc.robotcore.internal.opmode.ClassFilter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@link ConfigurationTypeManager} is responsible for managing configuration types.
 *
 * @see I2cDeviceType
 * @see AnalogSensorType
 * @see DigitalIoDeviceType
 * @see ServoType
 * @see com.qualcomm.robotcore.hardware.configuration.annotations.MotorType
 * @see I2cDeviceType
 */
@SuppressLint("StaticFieldLeak")
public final class ConfigurationTypeManager implements ClassFilter
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "UserDeviceTypeManager";
    public static boolean DEBUG = true;

    public static ConfigurationTypeManager getInstance()
        {
        return theInstance;
        }

    private static ConfigurationTypeManager theInstance = new ConfigurationTypeManager();

    private Gson gson = newGson();
    private Map<String, UserConfigurationType> mapTagToUserType = new HashMap<>();
    private Set<String>  existingXmlTags = new HashSet<>();
    private Map<ConfigurationType.DeviceFlavor, Set<String>> existingTypeDisplayNamesMap = new HashMap<>();

    private static String unspecifiedMotorTypeXmlTag = getXmlTag(UnspecifiedMotor.class);
    private static String standardServoTypeXmlTag = getXmlTag(Servo.class);

    private static final Class[] typeAnnotationsArray = { ServoType.class, AnalogSensorType.class, DigitalIoDeviceType.class, I2cDeviceType.class, com.qualcomm.robotcore.hardware.configuration.annotations.MotorType.class};
    private static final List<Class> typeAnnotationsList = Arrays.asList(typeAnnotationsArray);

    private Comparator<? super ConfigurationType> simpleConfigTypeComparator = new Comparator<ConfigurationType>()
        {
        @Override
        public int compare(ConfigurationType lhs, ConfigurationType rhs)
            {
            return lhs.getDisplayName(ConfigurationType.DisplayNameFlavor.Normal).compareTo(rhs.getDisplayName(ConfigurationType.DisplayNameFlavor.Normal));
            }
        };

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public ConfigurationTypeManager()
        {
        for (ConfigurationType.DeviceFlavor flavor : ConfigurationType.DeviceFlavor.values())
            {
            existingTypeDisplayNamesMap.put(flavor, new HashSet<String>());
            }
        addBuiltinConfigurationTypes();
        }

    //----------------------------------------------------------------------------------------------
    // Retrieval
    //----------------------------------------------------------------------------------------------

    public MotorConfigurationType getUnspecifiedMotorType()
        {
        return (MotorConfigurationType)configurationTypeFromTag(unspecifiedMotorTypeXmlTag);
        }

    public ServoConfigurationType getStandardServoType()
        {
        return (ServoConfigurationType)configurationTypeFromTag(standardServoTypeXmlTag);
        }

    public ConfigurationType configurationTypeFromTag(String xmlTag)
        {
        ConfigurationType result = BuiltInConfigurationType.fromXmlTag(xmlTag);
        if (result == BuiltInConfigurationType.UNKNOWN)
            {
            result = mapTagToUserType.get(xmlTag);
            if (result == null)
                {
                result = BuiltInConfigurationType.UNKNOWN;
                }
            }
        return result;
        }

    // TODO(Noah): Remove flavor parameter after I2cSensor and the original MotorType have been removed
    public @Nullable UserConfigurationType userTypeFromClass(ConfigurationType.DeviceFlavor flavor, Class<?> clazz)
        {
        String xmlTag = null;

        DeviceProperties deviceProperties = clazz.getAnnotation(DeviceProperties.class);
        if (deviceProperties != null)
            {
            xmlTag = getXmlTag(deviceProperties);
            }

        if (xmlTag == null)
            {
            switch (flavor)
                {
                case I2C:
                    I2cSensor i2cSensor = clazz.getAnnotation(I2cSensor.class);
                    if (i2cSensor != null)
                        {
                        xmlTag = getXmlTag(i2cSensor);
                        }
                    break;
                case MOTOR:
                    MotorType motorType = clazz.getAnnotation(MotorType.class);
                    if (motorType != null)
                        {
                        xmlTag = getXmlTag(motorType);
                        }
                    break;
                }
            }
        return xmlTag==null ? null : (UserConfigurationType) configurationTypeFromTag(xmlTag);
        }

    /**
     * Get the applicable configuration types to populate dropdowns with
     *
     * @param deviceFlavor What type of device is being configured
     * @param controlSystem What type of control system the device is connected to. If null, we err on the side of including types.
     * @param i2cBus Which I2C bus on the REV hub the device is connected to. Ignored if you pass anything other than DeviceFlavor.I2C and ControlSystem.REV_HUB.
     * @return The list of types that can be selected from
     */
    public @NonNull List<ConfigurationType> getApplicableConfigTypes(ConfigurationType.DeviceFlavor deviceFlavor, @Nullable ControlSystem controlSystem, int i2cBus)
        {
        LinkedList<ConfigurationType> result = new LinkedList<>();
        for (UserConfigurationType type : mapTagToUserType.values())
            {
            if (result.contains(type)) continue; // Prevent duplicate entries which would otherwise result from XML tag aliases
            if (type.getDeviceFlavor() == deviceFlavor && (controlSystem == null || type.isCompatibleWith(controlSystem)))
                {
                if (type == I2cDeviceConfigurationType.getLynxEmbeddedIMUType() && controlSystem != null &&
                        (controlSystem != ControlSystem.REV_HUB || i2cBus != LynxConstants.EMBEDDED_IMU_BUS))
                    {
                    continue;
                    }
                result.add(type);
                }
            }

        result.addAll(getApplicableBuiltInTypes(deviceFlavor, controlSystem));
        Collections.sort(result, simpleConfigTypeComparator);
        result.addAll(getDeprecatedConfigTypes(deviceFlavor, controlSystem));
        result.addFirst(BuiltInConfigurationType.NOTHING);
        return result;
        }

    /**
     * Get the applicable configuration types to populate dropdowns with (don't use this variant for REV I2C)
     *
     * @param deviceFlavor What type of device is being configured
     * @param controlSystem What type of control system the device is connected to. If null, we err on the side of including types.
     * @return The list of types that can be selected from
     */
    public @NonNull List<ConfigurationType> getApplicableConfigTypes(ConfigurationType.DeviceFlavor deviceFlavor, @Nullable ControlSystem controlSystem)
        {
        return getApplicableConfigTypes(deviceFlavor, controlSystem, 0);
        }

    private List<BuiltInConfigurationType> getApplicableBuiltInTypes(ConfigurationType.DeviceFlavor flavor, @Nullable ControlSystem controlSystem)
        {
        List<BuiltInConfigurationType> result = new LinkedList<>();
        switch (flavor)
            {
            case ANALOG_OUTPUT:
                result.add(BuiltInConfigurationType.ANALOG_OUTPUT);
                break;
            case DIGITAL_IO:
                if (controlSystem == null || controlSystem == ControlSystem.MODERN_ROBOTICS)
                    {
                    result.add(BuiltInConfigurationType.TOUCH_SENSOR);
                    }
                break;
            case I2C:
                result.add(BuiltInConfigurationType.IR_SEEKER_V3);
                result.add(BuiltInConfigurationType.ADAFRUIT_COLOR_SENSOR);
                result.add(BuiltInConfigurationType.COLOR_SENSOR);
                result.add(BuiltInConfigurationType.GYRO);
                if (controlSystem == ControlSystem.REV_HUB)
                    {
                    result.add(BuiltInConfigurationType.LYNX_COLOR_SENSOR);
                    }
            }
        return result;
        }

    private List<BuiltInConfigurationType> getDeprecatedConfigTypes(ConfigurationType.DeviceFlavor flavor, @Nullable ControlSystem controlSystem)
        {
        List<BuiltInConfigurationType> result = new LinkedList<>();
        switch (flavor)
            {
            case I2C:
                if (controlSystem == null || controlSystem == ControlSystem.MODERN_ROBOTICS)
                    {
                    result.add(BuiltInConfigurationType.I2C_DEVICE);
                    }
                result.add(BuiltInConfigurationType.I2C_DEVICE_SYNCH);
            }
        return result;
        }

    //----------------------------------------------------------------------------------------------
    // Serialization
    //----------------------------------------------------------------------------------------------

    public Gson getGson()
        {
        return gson;
        }

    public void sendUserDeviceTypes()
        {
        String userDeviceTypes = this.serializeUserDeviceTypes();
        NetworkConnectionHandler.getInstance().sendCommand(new Command(RobotCoreCommandList.CMD_NOTIFY_USER_DEVICE_LIST, userDeviceTypes));
        }


    // Replace the current user device types with the ones contained in the serialization
    public void deserializeUserDeviceTypes(String serialization) // Used on the DS
        {
        clearUserTypes();
        //
        ConfigurationType[] newTypes = gson.fromJson(serialization, ConfigurationType[].class);
        for (ConfigurationType deviceType : newTypes)
            {
            if (deviceType.isDeviceFlavor(ConfigurationType.DeviceFlavor.BUILT_IN)) continue; // paranoia
            add((UserConfigurationType) deviceType);
            }

        if (DEBUG)
            {
            for (Map.Entry<String, UserConfigurationType> pair : mapTagToUserType.entrySet())
                {
                RobotLog.vv(TAG, "deserialized: xmltag=%s name=%s class=%s", pair.getValue().getXmlTag(), pair.getValue().getName(), pair.getValue().getClass().getSimpleName());
                }
            }
        }

    private Gson newGson()
        {
        RuntimeTypeAdapterFactory<ConfigurationType> userDeviceTypeAdapterFactory
                = RuntimeTypeAdapterFactory.of(ConfigurationType.class, "flavor")
                .registerSubtype(BuiltInConfigurationType.class, ConfigurationType.DeviceFlavor.BUILT_IN.toString())
                .registerSubtype(I2cDeviceConfigurationType.class, ConfigurationType.DeviceFlavor.I2C.toString())
                .registerSubtype(MotorConfigurationType.class, ConfigurationType.DeviceFlavor.MOTOR.toString())
                .registerSubtype(ServoConfigurationType.class, ConfigurationType.DeviceFlavor.SERVO.toString())
                .registerSubtype(AnalogSensorConfigurationType.class, ConfigurationType.DeviceFlavor.ANALOG_SENSOR.toString())
                .registerSubtype(DigitalIoDeviceConfigurationType.class, ConfigurationType.DeviceFlavor.DIGITAL_IO.toString());

        return new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .registerTypeAdapter(BuiltInConfigurationType.class, new BuiltInConfigurationTypeJsonAdapter())
                .registerTypeAdapterFactory(userDeviceTypeAdapterFactory)
                .create();
        }


    private String serializeUserDeviceTypes()
        {
        return gson.toJson(mapTagToUserType.values());
        }


    //----------------------------------------------------------------------------------------------
    // Type list management
    //----------------------------------------------------------------------------------------------

    private void addBuiltinConfigurationTypes()
        {
        for (BuiltInConfigurationType type : BuiltInConfigurationType.values())
            {
            existingXmlTags.add(type.getXmlTag());
            existingTypeDisplayNamesMap.get(type.getDeviceFlavor()).add(type.getDisplayName(ConfigurationType.DisplayNameFlavor.Normal));
            }
        }

    private void add(UserConfigurationType deviceType)
        {
        mapTagToUserType.put(deviceType.getXmlTag(), deviceType);
        existingTypeDisplayNamesMap.get(deviceType.getDeviceFlavor()).add(deviceType.getName());
        existingXmlTags.add(deviceType.getXmlTag());

        for (String xmlTagAlias : deviceType.getXmlTagAliases())
            {
            mapTagToUserType.put(xmlTagAlias, deviceType);
            existingXmlTags.add(xmlTagAlias);
            }
        }

    private void clearUserTypes()
        {
        List<UserConfigurationType> extant = new ArrayList<>(mapTagToUserType.values()); // capture to avoid deleting while iterating

        for (UserConfigurationType userType : extant)
            {
            existingTypeDisplayNamesMap.get(userType.getDeviceFlavor()).remove(userType.getName());
            existingXmlTags.remove(userType.getXmlTag());
            mapTagToUserType.remove(userType.getXmlTag());
            }
        }

    private void clearOnBotJavaTypes()
        {
        List<UserConfigurationType> extantUserTypes = new ArrayList<>(mapTagToUserType.values());  // capture to avoid deleting while iterating

        for (UserConfigurationType userType : extantUserTypes)
            {
            if (userType.isOnBotJava())
                {
                existingTypeDisplayNamesMap.get(userType.getDeviceFlavor()).remove(userType.getName());
                existingXmlTags.remove(userType.getXmlTag());
                mapTagToUserType.remove(userType.getXmlTag());
                }
            }
        }

    //----------------------------------------------------------------------------------------------
    // Annotation parsing
    //----------------------------------------------------------------------------------------------

    @Override public void filterAllClassesStart()
        {
        clearUserTypes();
        }

    @Override public void filterOnBotJavaClassesStart()
        {
        clearOnBotJavaTypes();
        }

    @SuppressWarnings("unchecked")
    @Override public void filterClass(Class clazz)
        {
        if (addMotorTypeFromDeprecatedAnnotation(clazz)) return;

        if (addI2cTypeFromDeprecatedAnnotation(clazz)) return;

        Annotation specificTypeAnnotation = getTypeAnnotation(clazz);
        if (specificTypeAnnotation == null) return;

        UserConfigurationType configurationType;

        DeviceProperties devicePropertiesAnnotation = (DeviceProperties) clazz.getAnnotation(DeviceProperties.class);
        if (devicePropertiesAnnotation == null)
            {
            reportConfigurationError("Class " + clazz.getSimpleName() + " annotated with " + specificTypeAnnotation + " is missing @DeviceProperties annotation.");
            return;
            }

        configurationType = createAppropriateConfigurationType(specificTypeAnnotation, devicePropertiesAnnotation, clazz);
        configurationType.processAnnotation(devicePropertiesAnnotation);
        configurationType.finishedAnnotations(clazz);
        if (configurationType instanceof InstantiableUserConfigurationType && ((InstantiableUserConfigurationType) configurationType).classMustBeInstantiable())
            {
            if (checkInstantiableTypeConstraints((InstantiableUserConfigurationType)configurationType))
                {
                add(configurationType);
                }
            }
        else
            {
            if (checkAnnotationParameterConstraints(configurationType))
                {
                add(configurationType);
                }
            }
        }

    @Override public void filterOnBotJavaClass(Class clazz)
        {
        filterClass(clazz);
        }

    @Override public void filterAllClassesComplete()
        {
        // Nothing to do
        }

    @Override public void filterOnBotJavaClassesComplete()
        {
        filterAllClassesComplete();
        }

    @SuppressWarnings("unchecked")
    private UserConfigurationType createAppropriateConfigurationType(Annotation specificTypeAnnotation, DeviceProperties devicePropertiesAnnotation, Class clazz)
        {
        UserConfigurationType configurationType = null;
        if (specificTypeAnnotation instanceof ServoType)
            {
            configurationType = new ServoConfigurationType(clazz, getXmlTag(devicePropertiesAnnotation));
            ((ServoConfigurationType) configurationType).processAnnotation((ServoType) specificTypeAnnotation);
            }
        else if (specificTypeAnnotation instanceof com.qualcomm.robotcore.hardware.configuration.annotations.MotorType)
            {
            configurationType = new MotorConfigurationType(clazz, getXmlTag(devicePropertiesAnnotation));
            processMotorSupportAnnotations(clazz, (MotorConfigurationType) configurationType);
            ((MotorConfigurationType) configurationType).processAnnotation((com.qualcomm.robotcore.hardware.configuration.annotations.MotorType) specificTypeAnnotation);
            }
        else if (specificTypeAnnotation instanceof AnalogSensorType)
            {
            configurationType = new AnalogSensorConfigurationType(clazz, getXmlTag(devicePropertiesAnnotation));
            }
        else if (specificTypeAnnotation instanceof DigitalIoDeviceType)
            {
            configurationType = new DigitalIoDeviceConfigurationType(clazz, getXmlTag(devicePropertiesAnnotation));
            }
        else if (specificTypeAnnotation instanceof I2cDeviceType)
            {
            configurationType = new I2cDeviceConfigurationType(clazz, getXmlTag(devicePropertiesAnnotation));
            }
        return configurationType;
        }

    /**
     * @return true if a new MotorConfigurationType was added
     */
    @SuppressWarnings("deprecation")
    private boolean addMotorTypeFromDeprecatedAnnotation(Class clazz)
        {
        if (clazz.isAnnotationPresent(MotorType.class))
            {
            MotorType motorTypeAnnotation = (MotorType) clazz.getAnnotation(MotorType.class);
            MotorConfigurationType motorType = new MotorConfigurationType(clazz, getXmlTag(motorTypeAnnotation));
            motorType.processAnnotation(motorTypeAnnotation);
            processMotorSupportAnnotations(clazz, motorType);
            motorType.finishedAnnotations(clazz);
            // There's some things we need to check about the actual class
            if (!checkAnnotationParameterConstraints(motorType))
                return false;

            add(motorType);
            return true;
            }
        return false;
        }

    /**
     * @return true if a new MotorConfigurationType was added
     */
    @SuppressWarnings({"deprecation", "unchecked"})
    private boolean addI2cTypeFromDeprecatedAnnotation(Class clazz)
        {
        if (isHardwareDevice(clazz))
            {
            if (clazz.isAnnotationPresent(I2cSensor.class))
                {
                I2cSensor i2cSensorAnnotation = (I2cSensor) clazz.getAnnotation(I2cSensor.class);
                I2cDeviceConfigurationType sensorType = new I2cDeviceConfigurationType(clazz, getXmlTag(i2cSensorAnnotation));
                sensorType.processAnnotation(i2cSensorAnnotation);
                sensorType.finishedAnnotations(clazz);

                if (!checkInstantiableTypeConstraints(sensorType))
                    return false;

                add(sensorType);
                return true;
                }
            }
        return false;
        }

    private void processMotorSupportAnnotations(Class<?> clazz, MotorConfigurationType motorType)
        {
        motorType.processAnnotation(findAnnotation(clazz, ModernRoboticsMotorControllerParams.class));
        motorType.processAnnotation(findAnnotation(clazz, DistributorInfo.class));

        // Can't have both old and new local declarations (pick your horse!), but local definitions
        // override inherited ones
        processNewOldAnnotations(motorType, clazz, ExpansionHubPIDFVelocityParams.class, ExpansionHubMotorControllerVelocityParams.class);
        processNewOldAnnotations(motorType, clazz, ExpansionHubPIDFPositionParams.class, ExpansionHubMotorControllerPositionParams.class);
        }

    protected <NewType extends Annotation, OldType extends Annotation> void processNewOldAnnotations(
            final MotorConfigurationType motorConfigurationType,
            final Class<?> clazz,
            final Class<NewType> newType,
            final Class<OldType> oldType)
        {
        // newType is logical superset of oldType. Thus, there's no reason to ever have an oldType
        // annotation if you've already got a newType one on the same class. Thus, we prohibit same.
        // However, you might want to override an inherited value with either.
        if (!ClassUtil.searchInheritance(clazz, new Predicate<Class<?>>()
                {
                @Override public boolean test(Class<?> aClass)
                    {
                    return processAnnotationIfPresent(motorConfigurationType, clazz, newType);
                    }
                }))
            {
            ClassUtil.searchInheritance(clazz, new Predicate<Class<?>>()
                {
                @Override public boolean test(Class<?> aClass)
                    {
                    return processAnnotationIfPresent(motorConfigurationType, clazz, oldType);
                    }
                });
            }
        }

     protected <A extends Annotation> boolean processAnnotationIfPresent(MotorConfigurationType motorConfigurationType, Class<?> clazz, Class<A> annotationType)
        {
        A annotation = clazz.getAnnotation(annotationType);
        if (annotation != null)
            {
            motorConfigurationType.processAnnotation(annotation);
            return true;
            }
        return false;
        }

    private Annotation getTypeAnnotation(Class clazz)
        {
        Annotation[] annotations = clazz.getAnnotations();
        for (Annotation annotation : annotations)
            {
            if (typeAnnotationsList.contains(annotation.annotationType())) return annotation;
            }

        return null;
        }

    /** Allow annotations to be inherited if we want them to. */
    private <A extends Annotation> A findAnnotation(Class<?> clazz, final Class<A> annotationType)
        {
        final ArrayList<A> result = new ArrayList<>(1);
        result.add(null);

        ClassUtil.searchInheritance(clazz, new Predicate<Class<?>>()
            {
            @Override public boolean test(Class<?> aClass)
                {
                A annotation = aClass.getAnnotation(annotationType);
                if (annotation != null)
                    {
                    result.set(0, annotation);
                    return true;
                    }
                else
                    return false;
                }
            });

        return result.get(0);
        }

    private boolean checkAnnotationParameterConstraints(UserConfigurationType deviceType)
        {
        // Check the user-visible form of the sensor name
        if (!isLegalDeviceTypeName(deviceType.getName()))
            {
            reportConfigurationError("\"%s\" is not a legal device type name", deviceType.getName());
            return false;
            }
        if (existingTypeDisplayNamesMap.get(deviceType.getDeviceFlavor()).contains(deviceType.getName()))
            {
            reportConfigurationError("the device type \"%s\" is already defined", deviceType.getName());
            return false;
            }

        // Check the XML tag
        if (!isLegalXmlTag(deviceType.getXmlTag()))
            {
            reportConfigurationError("\"%s\" is not a legal XML tag for the device type \"%s\"", deviceType.getXmlTag(), deviceType.getName());
            return false;
            }
        if (existingXmlTags.contains(deviceType.getXmlTag()))
            {
            reportConfigurationError("the XML tag \"%s\" is already defined", deviceType.getXmlTag());
            return false;
            }

        return true;
        }

    private boolean checkInstantiableTypeConstraints(InstantiableUserConfigurationType deviceType)
        {
        if (!checkAnnotationParameterConstraints(deviceType))
            {
            return false;
            }
        // If the class doesn't extend HardwareDevice, that's an error, we'll ignore it
        if (!isHardwareDevice(deviceType.getClazz()))
            {
            reportConfigurationError("'%s' class doesn't inherit from the class 'HardwareDevice'", deviceType.getClazz().getSimpleName());
            return false;
            }

        // If it's not 'public', it can't be loaded by the system and won't work. We report
        // the error and ignore
        if (!Modifier.isPublic(deviceType.getClazz().getModifiers()))
            {
            reportConfigurationError("'%s' class is not declared 'public'", deviceType.getClazz().getSimpleName());
            return false;
            }

        // Can we instantiate?
        if (!deviceType.hasConstructors())
            {
            reportConfigurationError("'%s' class lacks necessary constructor", deviceType.getClazz().getSimpleName());
            return false;
            }
        return true;
        }

    private boolean isLegalDeviceTypeName(String name)
        {
        return Util.isGoodString(name);
        }

    //----------------------------------------------------------------------------------------------
    // Utility
    //----------------------------------------------------------------------------------------------
    public static String getXmlTag(Class clazz)
        {
        DeviceProperties devicePropertiesAnnotation = (DeviceProperties) clazz.getAnnotation(DeviceProperties.class);
        return getXmlTag(devicePropertiesAnnotation);
        }
    private void reportConfigurationError(String format, Object... args)
        {
        String message = String.format(format, args);
        RobotLog.ee(TAG, String.format("configuration error: %s", message));
        RobotLog.setGlobalErrorMsg(message);
        }

    private boolean isHardwareDevice(Class clazz)
        {
        return ClassUtil.inheritsFrom(clazz, HardwareDevice.class);
        }

    private boolean isLegalXmlTag(String xmlTag)
        {
        if (!Util.isGoodString(xmlTag))
            return false;

        // For simplicity, we only allow a restricted subset of what XML allows
        //  https://www.w3.org/TR/REC-xml/#NT-NameStartChar
        String nameStartChar = "\\p{Alpha}_:";
        String nameChar      = nameStartChar + "0-9\\-\\.";

        return xmlTag.matches("^[" + nameStartChar + "][" + nameChar + "]*$");
        }

    private static String getXmlTag(I2cSensor i2cSensor)
        {
        return ClassUtil.decodeStringRes(i2cSensor.xmlTag().trim());
        }
    private static String getXmlTag(MotorType motorType)
        {
        return ClassUtil.decodeStringRes(motorType.xmlTag().trim());
        }
    private static String getXmlTag(DeviceProperties deviceProperties)
        {
        return ClassUtil.decodeStringRes(deviceProperties.xmlTag().trim());
        }
    }
