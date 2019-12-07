/*
Copyright (c) 2016-17 Robert Atkinson

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
package com.qualcomm.hardware.modernrobotics;

import com.qualcomm.robotcore.hardware.DistanceSensor;
import com.qualcomm.robotcore.hardware.I2cAddr;
import com.qualcomm.robotcore.hardware.I2cAddrConfig;
import com.qualcomm.robotcore.hardware.I2cDeviceSynch;
import com.qualcomm.robotcore.hardware.I2cDeviceSynchDevice;
import com.qualcomm.robotcore.hardware.I2cWaitControl;
import com.qualcomm.robotcore.hardware.OpticalDistanceSensor;
import com.qualcomm.robotcore.hardware.configuration.annotations.DeviceProperties;
import com.qualcomm.robotcore.hardware.configuration.annotations.I2cDeviceType;
import com.qualcomm.robotcore.hardware.usb.RobotUsbDevice;
import com.qualcomm.robotcore.util.Range;
import com.qualcomm.robotcore.util.TypeConversion;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

import java.util.Locale;

/**
 * {@link ModernRoboticsI2cRangeSensor} implements support for the MR ultrasonic/optical combo
 * range sensor.
 *
 * @see <a href="http://www.modernroboticsinc.com/range-sensor">MR Range Sensor</a>
 */
@SuppressWarnings("WeakerAccess")
@I2cDeviceType
@DeviceProperties(name = "@string/mr_range_name", description = "@string/mr_range_description", xmlTag = "ModernRoboticsI2cRangeSensor", builtIn = true)
public class ModernRoboticsI2cRangeSensor extends I2cDeviceSynchDevice<I2cDeviceSynch> implements DistanceSensor, OpticalDistanceSensor, I2cAddrConfig
    {
    //----------------------------------------------------------------------------------------------
    // Constants
    //----------------------------------------------------------------------------------------------

    public final static I2cAddr ADDRESS_I2C_DEFAULT = I2cAddr.create8bit(0x28);

    public enum Register
        {
            FIRST(0),
            FIRMWARE_REV(0x00),
            MANUFACTURE_CODE(0x01),
            SENSOR_ID(0x02),
            ULTRASONIC(0x04),
            OPTICAL(0x05),
            LAST(OPTICAL.bVal),
            UNKNOWN(-1);

        public byte bVal;
        Register(int bVal) { this.bVal = (byte)bVal; }
        }

    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected static final double apiLevelMin = 0.0;
    protected static final double apiLevelMax = 1.0;

    /**
     * Experimentally determined constants for converting optical measurements to distance.
     */
    public double aParam = 5.11595056535567;
    public double bParam = 457.048400147437;
    public double cParam = -0.8061002068394054;
    public double dParam = 0.004048820370701007;
    public int    rawOpticalMinValid = 3;

    protected static final int cmUltrasonicMax = 255;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public ModernRoboticsI2cRangeSensor(I2cDeviceSynch deviceClient)
        {
        super(deviceClient, true);

        this.setOptimalReadWindow();
        this.deviceClient.setI2cAddress(ADDRESS_I2C_DEFAULT);

        super.registerArmingStateCallback(false);
        this.deviceClient.engage();
        }

    protected void setOptimalReadWindow()
        {
        I2cDeviceSynch.ReadWindow readWindow = new I2cDeviceSynch.ReadWindow(
                Register.FIRST.bVal,
                Register.LAST.bVal - Register.FIRST.bVal + 1,
                I2cDeviceSynch.ReadMode.REPEAT);
        this.deviceClient.setReadWindow(readWindow);
        }

    @Override
    protected synchronized boolean doInitialize()
        {
        return true;    // nothing to do
        }

    //----------------------------------------------------------------------------------------------
    // DistanceSensor
    //----------------------------------------------------------------------------------------------

    /**
     * Returns a calibrated, linear sense of distance as read by the infrared proximity
     * part of the sensor. Distance is measured to the plastic housing at the front of the
     * sensor.
     *
     * The manufacturer states that the optical readings decay exponentially. This was a surprise,
     * as optical sensors usually decay with an inverse square law. However, both forms were
     * fitted, and the the exponential form produced better results. Accordingly, exponential parameters
     * have been fitted to turn the reported reading into a linear measure of distance. The function
     * fitted was of the form:
     *
     *      rawOptical == a + b exp(c cm - d)
     *
     * This fitted linearity is fairly accurate over a range of target surfaces, but is ultimately
     * affected by the reflectivity of the surface. However, even on surfaces where there is
     * significantly different reflectivity, the linearity calculated here tends to be preserved,
     * so distance accuracy can often be refined with a simple further multiplicative scaling.
     *
     * Note that readings are most accurate when perpendicular to the surface. For non-perpendicularity,
     * a cosine correction factor is usually appropriate.
     *
     * @param unit  the unit of distance in which the result should be returned
     * @return      the currently measured distance in the indicated units
     */
    @Override public double getDistance(DistanceUnit unit)
        {
        int rawOptical = rawOptical(); // the very low readings are quite noisy

        double cm;
        if (rawOptical >= rawOpticalMinValid)
            {
            cm = cmFromOptical(rawOptical);
            }
        else
            {
            cm = cmUltrasonic();
            if (cm == cmUltrasonicMax)
                {
                return DistanceSensor.distanceOutOfRange;
                }
            }

        return unit.fromUnit(DistanceUnit.CM, cm);
        }

    /**
     * Converts a raw optical inverse-square reading into a fitted, calibrated linear reading in cm.
     */
    protected double cmFromOptical(int rawOptical)
        {
        return (dParam + Math.log((-aParam + rawOptical)/bParam))/cParam;
        }

    public double cmUltrasonic()
        {
        return rawUltrasonic();
        }

    public double cmOptical()
        {
        int rawOptical = rawOptical(); // the very low readings are quite noisy
         if (rawOptical >= rawOpticalMinValid)
            {
            return cmFromOptical(rawOptical);
            }
        else
            return DistanceSensor.distanceOutOfRange;
        }

    //----------------------------------------------------------------------------------------------
    // OpticalDistanceSensor
    //----------------------------------------------------------------------------------------------

    @Override public double getLightDetected()
        {
        return Range.clip(
                Range.scale(getRawLightDetected(), 0, getRawLightDetectedMax(), apiLevelMin, apiLevelMax),
                apiLevelMin, apiLevelMax);
        }

    @Override public double getRawLightDetected()
        {
        return rawOptical();
        }

    @Override public double getRawLightDetectedMax()
        {
        return 255;
        }

    @Override public void enableLed(boolean enable)
        {
        // enabling or disabling the LED does nothing on this piece of hardware
        }

    @Override public String status()
        {
        return String.format(Locale.getDefault(), "%s on %s", getDeviceName(), getConnectionInfo());
        }

    //----------------------------------------------------------------------------------------------
    // Raw sensor data
    //----------------------------------------------------------------------------------------------

    /**
     * Returns the raw reading on the ultrasonic sensor
     * @return the raw reading on the ultrasonic sensor
     */
    public int rawUltrasonic()
        {
        return readUnsignedByte(Register.ULTRASONIC);
        }

    /**
     * Returns the raw reading on the optical sensor
     * @return the raw reading on the optical sensor
     */
    public int rawOptical()
        {
        return readUnsignedByte(Register.OPTICAL);
        }

    //----------------------------------------------------------------------------------------------
    // I2cAddressConfig
    //----------------------------------------------------------------------------------------------

    @Override public void setI2cAddress(I2cAddr newAddress)
        {
        this.deviceClient.setI2cAddress(newAddress);
        }

    @Override public I2cAddr getI2cAddress()
        {
        return this.deviceClient.getI2cAddress();
        }

    //----------------------------------------------------------------------------------------------
    // HardwareDevice
    //----------------------------------------------------------------------------------------------

    @Override
    public Manufacturer getManufacturer()
        {
        return Manufacturer.ModernRobotics;
        }

    @Override public String getDeviceName()
        {
        return String.format(Locale.getDefault(), "Modern Robotics Range Sensor %s",
                new RobotUsbDevice.FirmwareVersion(this.read8(Register.FIRMWARE_REV)));
        }

    //----------------------------------------------------------------------------------------------
    // Utility
    //----------------------------------------------------------------------------------------------

    public byte read8(Register reg)
        {
        return this.deviceClient.read8(reg.bVal);
        }

    public void write8(Register reg, byte value)
        {
        this.write8(reg, value, I2cWaitControl.NONE);
        }
    public void write8(Register reg, byte value, I2cWaitControl waitControl)
        {
        this.deviceClient.write8(reg.bVal, value, waitControl);
        }

    protected int readUnsignedByte(Register reg)
        {
        return TypeConversion.unsignedByteToInt(this.read8(reg));
        }

    }
