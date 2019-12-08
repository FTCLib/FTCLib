/*
Copyright (c) 2016 Robert Atkinson

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

import androidx.annotation.NonNull;

import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.util.SerialNumber;

import org.firstinspires.ftc.robotcore.internal.usb.LynxModuleSerialNumber;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by bob on 2016-03-11.
 */
@SuppressWarnings("WeakerAccess")
public class LynxModuleConfiguration extends ControllerConfiguration<DeviceConfiguration>
    {
    public static final String TAG = "LynxModuleConfiguration";

    private boolean                   isParent       = false;
    private List<DeviceConfiguration>  motors         = new LinkedList<>();
    private List<DeviceConfiguration>  servos         = new LinkedList<>();
    private List<DeviceConfiguration> pwmOutputs     = new LinkedList<>();
    private List<DeviceConfiguration> digitalDevices = new LinkedList<>();
    private List<DeviceConfiguration> analogInputs   = new LinkedList<>();
    private List<LynxI2cDeviceConfiguration> i2cDevices  = new LinkedList<>();

    /** not persisted in XML */
    private @NonNull SerialNumber     usbDeviceSerialNumber = SerialNumber.createFake();

    // A note: unlike everything else, we don't have a fixed number of attachable i2c devices.
    // Rather, we have four i2c busses, to each of which may be attached any number of i2c devices
    // so long as no two i2c devices with the same address reside on the same bus.
    //
    // In each i2c DeviceConfiguration, the 'port' is the 0-based bus-number.
    //
    // This will have consequences for the configuration editor!

    public LynxModuleConfiguration()
        {
        this("");
        }

    public LynxModuleConfiguration(String name)
        {
        super(name, new ArrayList<DeviceConfiguration>(), SerialNumber.createFake(), BuiltInConfigurationType.LYNX_MODULE);

        servos          = ConfigurationUtility.buildEmptyServos(LynxConstants.INITIAL_SERVO_PORT, LynxConstants.NUMBER_OF_SERVO_CHANNELS);
        motors          = ConfigurationUtility.buildEmptyMotors(LynxConstants.INITIAL_MOTOR_PORT, LynxConstants.NUMBER_OF_MOTORS);
        pwmOutputs      = ConfigurationUtility.buildEmptyDevices(0,                               LynxConstants.NUMBER_OF_PWM_CHANNELS,   BuiltInConfigurationType.NOTHING);
        analogInputs    = ConfigurationUtility.buildEmptyDevices(0,                               LynxConstants.NUMBER_OF_ANALOG_INPUTS,  BuiltInConfigurationType.NOTHING);
        digitalDevices  = ConfigurationUtility.buildEmptyDevices(0,                               LynxConstants.NUMBER_OF_DIGITAL_IOS,    BuiltInConfigurationType.NOTHING);
        i2cDevices      = new LinkedList<LynxI2cDeviceConfiguration>();

        }

    @Override
    public void setPort(int port)
        {
        super.setPort(port);
        setSerialNumber(new LynxModuleSerialNumber(usbDeviceSerialNumber, port));
        }

    public void setModuleAddress(int moduleAddress)
        {
        this.setPort(moduleAddress);
        }
    public int getModuleAddress()
        {
        return this.getPort();
        }

    /**
     * A USB-connected Controller Module is considered a "Parent" and an EIA485-connected Controller Module is a "Child".
     */
    public void setIsParent(boolean isParent)
        {
        this.isParent = isParent;
        }
    public boolean isParent()
        {
        return this.isParent;
        }

    public void setUsbDeviceSerialNumber(@NonNull SerialNumber usbDeviceSerialNumber)
        {
        this.usbDeviceSerialNumber = usbDeviceSerialNumber;
        setSerialNumber(new LynxModuleSerialNumber(usbDeviceSerialNumber, getModuleAddress()));
        }

    @Override public void setSerialNumber(@NonNull SerialNumber serialNumber)
        {
        super.setSerialNumber(serialNumber);
        // RobotLog.vv(TAG, "setSerialNumber(%s)", serialNumber);
        }

    public @NonNull SerialNumber getUsbDeviceSerialNumber()
        {
        return usbDeviceSerialNumber;
        }

    /** separate method just to reinforce whose serial number we're retreiving */
    public @NonNull SerialNumber getModuleSerialNumber()
        {
        return getSerialNumber();
        }

    public List<DeviceConfiguration> getServos()
        {
        return servos;
        }
    public void setServos(List<DeviceConfiguration> servos)
        {
        this.servos = servos;
        }

    public List<DeviceConfiguration> getMotors()
        {
        return motors;
        }
    public void setMotors(List<DeviceConfiguration> motors)
        {
        this.motors = motors;
        }

    public List<DeviceConfiguration> getAnalogInputs()
        {
        return this.analogInputs;
        }
    public void setAnalogInputs(List<DeviceConfiguration> inputs)
        {
        this.analogInputs = inputs;
        }

    public List<DeviceConfiguration> getPwmOutputs()
        {
        return this.pwmOutputs;
        }
    public void setPwmOutputs(List<DeviceConfiguration> pwmOutputs)
        {
        this.pwmOutputs = pwmOutputs;
        }

    public List<LynxI2cDeviceConfiguration> getI2cDevices()
        {
        return i2cDevices;
        }
    public void setI2cDevices(List<LynxI2cDeviceConfiguration> i2cDevices)
        {
        this.i2cDevices = new LinkedList<LynxI2cDeviceConfiguration>();
        for (LynxI2cDeviceConfiguration i2cDevice : i2cDevices)
            {
            if (i2cDevice.isEnabled() && i2cDevice.getPort() >= 0 && i2cDevice.getPort() < LynxConstants.NUMBER_OF_I2C_BUSSES)
                {
                this.i2cDevices.add(i2cDevice);
                }
            }
        }

    public List<LynxI2cDeviceConfiguration> getI2cDevices(int busZ)
        {
        List<LynxI2cDeviceConfiguration> result = new LinkedList<LynxI2cDeviceConfiguration>();
        for (LynxI2cDeviceConfiguration configuration : this.i2cDevices)
            {
            if (configuration.getBus() == busZ)
                {
                result.add(configuration);
                }
            }
        return result;
        }

    public void setI2cDevices(int busZ, List<LynxI2cDeviceConfiguration> devices)
        {
        List<LynxI2cDeviceConfiguration> result = new LinkedList<LynxI2cDeviceConfiguration>();
        for (LynxI2cDeviceConfiguration configuration : this.i2cDevices)
            {
            if (configuration.getBus() != busZ)
                {
                result.add(configuration);
                }
            }
        for (LynxI2cDeviceConfiguration configuration : devices)
            {
            if (configuration.isEnabled())
                {
                configuration.setBus(busZ);
                result.add(configuration);
                }
            }
        this.i2cDevices = result;
        }

    public List<DeviceConfiguration> getDigitalDevices()
        {
        return this.digitalDevices;
        }
    public void setDigitalDevices(List<DeviceConfiguration> digitalDevices)
        {
        this.digitalDevices = digitalDevices;
        }

    @Override
    protected void deserializeChildElement(ConfigurationType configurationType, XmlPullParser parser, ReadXMLFileHandler xmlReader) throws IOException, XmlPullParserException, RobotCoreException
        {
        super.deserializeChildElement(configurationType, parser, xmlReader);

        if (configurationType.isDeviceFlavor(ConfigurationType.DeviceFlavor.SERVO))
            {
            DeviceConfiguration servo = new DeviceConfiguration();
            servo.deserialize(parser, xmlReader);
            getServos().set(servo.getPort()-LynxConstants.INITIAL_SERVO_PORT, servo);
            }
        else if (configurationType.isDeviceFlavor(ConfigurationType.DeviceFlavor.MOTOR))
            {
            DeviceConfiguration motor = new DeviceConfiguration();
            motor.deserialize(parser, xmlReader);
            getMotors().set(motor.getPort()-LynxConstants.INITIAL_MOTOR_PORT, motor);
            }
        else if (configurationType == BuiltInConfigurationType.PULSE_WIDTH_DEVICE)
            {
            DeviceConfiguration pwmOutput = new DeviceConfiguration();
            pwmOutput.deserialize(parser, xmlReader);
            getPwmOutputs().set(pwmOutput.getPort(), pwmOutput);
            }
        else if (configurationType.isDeviceFlavor(ConfigurationType.DeviceFlavor.ANALOG_SENSOR))
            {
            DeviceConfiguration analogSensor = new DeviceConfiguration();
            analogSensor.deserialize(parser, xmlReader);
            getAnalogInputs().set(analogSensor.getPort(), analogSensor);
            }
        else if (configurationType.isDeviceFlavor(ConfigurationType.DeviceFlavor.DIGITAL_IO))
            {
            DeviceConfiguration digitalIoDevice = new DeviceConfiguration();
            digitalIoDevice.deserialize(parser, xmlReader);
            getDigitalDevices().set(digitalIoDevice.getPort(), digitalIoDevice);
            }
        else if (configurationType.isDeviceFlavor(ConfigurationType.DeviceFlavor.I2C))
            {
            LynxI2cDeviceConfiguration i2cDevice = new LynxI2cDeviceConfiguration();
            i2cDevice.deserialize(parser, xmlReader);
            getI2cDevices().add(i2cDevice);
            }
        }

    @Override
    protected void deserializeAttributes(XmlPullParser parser)
        {
        super.deserializeAttributes(parser);
        setModuleAddress(getPort());
        }
    }