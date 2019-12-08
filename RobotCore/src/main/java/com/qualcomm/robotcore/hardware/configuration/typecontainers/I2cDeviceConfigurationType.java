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
package com.qualcomm.robotcore.hardware.configuration.typecontainers;

import androidx.annotation.Nullable;

import com.qualcomm.robotcore.hardware.HardwareDevice;
import com.qualcomm.robotcore.hardware.I2cController;
import com.qualcomm.robotcore.hardware.I2cDevice;
import com.qualcomm.robotcore.hardware.I2cDeviceImpl;
import com.qualcomm.robotcore.hardware.I2cDeviceSynch;
import com.qualcomm.robotcore.hardware.I2cDeviceSynchImpl;
import com.qualcomm.robotcore.hardware.I2cDeviceSynchSimple;
import com.qualcomm.robotcore.hardware.RobotCoreLynxModule;
import com.qualcomm.robotcore.hardware.configuration.ConfigurationTypeManager;
import com.qualcomm.robotcore.hardware.configuration.ConstructorPrototype;
import com.qualcomm.robotcore.hardware.configuration.I2cSensor;
import com.qualcomm.robotcore.hardware.configuration.LynxConstants;
import com.qualcomm.robotcore.util.ClassUtil;

import org.firstinspires.ftc.robotcore.external.Func;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * {@link I2cDeviceConfigurationType} contains the meta-data for a user-defined I2c sensor driver.
 */
public final class I2cDeviceConfigurationType extends InstantiableUserConfigurationType
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------
    private static final ConstructorPrototype ctorI2cDeviceSynchSimple  = new ConstructorPrototype(I2cDeviceSynchSimple.class);
    private static final ConstructorPrototype ctorI2cDeviceSynch        = new ConstructorPrototype(I2cDeviceSynch.class);
    private static final ConstructorPrototype ctorI2cDevice             = new ConstructorPrototype(I2cDevice.class);
    private static final ConstructorPrototype ctorI2cControllerPort     = new ConstructorPrototype(I2cController.class, int.class);

    private static final ConstructorPrototype[] allowableConstructorPrototypes =
        {
            ctorI2cDeviceSynchSimple,
            ctorI2cDeviceSynch,
            ctorI2cDevice,
            ctorI2cControllerPort
        };

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public I2cDeviceConfigurationType(Class<? extends HardwareDevice> clazz, String xmlTag)
        {
        super(clazz, DeviceFlavor.I2C, xmlTag, allowableConstructorPrototypes);
        }

    public static I2cDeviceConfigurationType getLynxEmbeddedIMUType()
        {
        return (I2cDeviceConfigurationType) ConfigurationTypeManager.getInstance().configurationTypeFromTag(LynxConstants.EMBEDDED_IMU_XML_TAG);
        }

    // Used by gson deserialization
    public I2cDeviceConfigurationType()
        {
        super(DeviceFlavor.I2C);
        }

    public void processAnnotation(@Nullable I2cSensor i2cSensor)
        {
        if (i2cSensor != null)
            {
            if (name.isEmpty())
                {
                name = ClassUtil.decodeStringRes(i2cSensor.name().trim());
                }
            this.description = ClassUtil.decodeStringRes(i2cSensor.description());
            }
        }

    //----------------------------------------------------------------------------------------------
    // Instance creation
    //----------------------------------------------------------------------------------------------

    public @Nullable HardwareDevice createInstance(RobotCoreLynxModule lynxModule,
            Func<I2cDeviceSynchSimple> simpleSynchFunc,
            Func<I2cDeviceSynch> synchFunc)
        {
        try {
            Constructor<HardwareDevice> ctor;

            ctor = findMatch(ctorI2cDeviceSynchSimple);
            if (null != ctor)
                {
                I2cDeviceSynchSimple i2cDeviceSynchSimple = simpleSynchFunc.value();
                return ctor.newInstance(i2cDeviceSynchSimple);
                }

            ctor = findMatch(ctorI2cDeviceSynch);
            if (null != ctor)
                {
                I2cDeviceSynch i2cDeviceSynch = synchFunc.value();
                return ctor.newInstance(i2cDeviceSynch);
                }
            }
        catch (IllegalAccessException|InstantiationException|InvocationTargetException e)
             {
             handleConstructorExceptions(e);
             return null;
             }
        throw new RuntimeException("internal error: unable to locate constructor for user sensor type " + getName());
        }

    public @Nullable HardwareDevice createInstance(I2cController controller, int port)
        {
        try {
            Constructor<HardwareDevice> ctor;

            ctor = findMatch(ctorI2cDeviceSynch);
            if (null == ctor) ctor = findMatch(ctorI2cDeviceSynchSimple);
            if (null != ctor)
                {
                I2cDevice      i2cDevice      = new I2cDeviceImpl(controller, port);
                I2cDeviceSynch i2cDeviceSynch = new I2cDeviceSynchImpl(i2cDevice, true);
                return ctor.newInstance(i2cDeviceSynch);
                }

            ctor = findMatch(ctorI2cDevice);
            if (null != ctor)
                {
                I2cDevice i2cDevice = new I2cDeviceImpl(controller, port);
                return ctor.newInstance(i2cDevice);
                }

            ctor = findMatch(ctorI2cControllerPort);
            if (null != ctor)
                {
                return ctor.newInstance(controller, port);
                }
            }
         catch (IllegalAccessException|InstantiationException|InvocationTargetException e)
             {
             handleConstructorExceptions(e);
             return null;
             }
        throw new RuntimeException("internal error: unable to locate constructor for user sensor type " + getName());
        }

    //----------------------------------------------------------------------------------------------
    // Serialization (used in local marshalling during configuration editing)
    //----------------------------------------------------------------------------------------------

    private Object writeReplace()
        {
        return new SerializationProxy(this);
        }
    }
