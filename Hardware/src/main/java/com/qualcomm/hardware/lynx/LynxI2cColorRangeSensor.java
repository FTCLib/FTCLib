/*
Copyright (c) 2016-2017 Robert Atkinson

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
package com.qualcomm.hardware.lynx;

import com.qualcomm.hardware.ams.AMSColorSensor;
import com.qualcomm.hardware.ams.AMSColorSensorImpl;
import com.qualcomm.robotcore.hardware.DistanceSensor;
import com.qualcomm.robotcore.hardware.HardwareDevice;
import com.qualcomm.robotcore.hardware.I2cDeviceSynchSimple;
import com.qualcomm.robotcore.hardware.OpticalDistanceSensor;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.nio.ByteOrder;
import java.util.Locale;

/**
 * {@link LynxI2cColorRangeSensor} is both a color sensor and a distance sensor, supporting both
 * {@link DistanceSensor} for calibrated readings and {@link OpticalDistanceSensor} (which is an
 * historical name that could perhaps have been chosen better) for raw, uncalibrated readings.
 */
@SuppressWarnings("WeakerAccess")
public class LynxI2cColorRangeSensor extends AMSColorSensorImpl implements DistanceSensor, OpticalDistanceSensor
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected static final double apiLevelMin = 0.0;
    protected static final double apiLevelMax = 1.0;

    /**
     * Experimentally determined constants for converting optical measurements to distance.
     */
    public double aParam = 186.347;
    public double bParam = 30403.5;
    public double cParam = 0.576649;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public LynxI2cColorRangeSensor(I2cDeviceSynchSimple deviceClient)
        {
        super(AMSColorSensor.Parameters.createForTMD37821(), deviceClient, true);
        }

    //----------------------------------------------------------------------------------------------
    // DistanceSensor
    //----------------------------------------------------------------------------------------------

    /**
     * Returns a calibrated, linear sense of distance as read by the infrared proximity
     * part of the sensor. Distance is measured to the plastic housing at the front of the
     * sensor.
     *
     * Natively, the raw optical signal follows an inverse square law. Here, parameters have
     * been fitted to turn that into a <em>linear</em> measure of distance. The function fitted
     * was of the form:
     *
     *      rawOptical = a + b * (cm + c)^(-2)
     *
     * This fitted linearity is fairly accurate over a wide range of target surfaces, but is ultimately
     * affected by the infrared reflectivity of the surface. However, even on surfaces where there is 
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
        int rawOptical = rawOptical();
        double cmOptical = cmFromOptical(rawOptical);
        return unit.fromUnit(DistanceUnit.CM, cmOptical);
        }

    /**
     * Converts a raw optical inverse-square reading into a fitted, calibrated linear reading in cm.
     */
    protected double cmFromOptical(int rawOptical)
        {
        return (-aParam * cParam + cParam * rawOptical - Math.sqrt(-aParam * bParam + bParam * rawOptical))/(aParam - rawOptical);
        }

    //----------------------------------------------------------------------------------------------
    // HardwareDevice
    //----------------------------------------------------------------------------------------------

    @Override
    public String getDeviceName()
        {
        return AppUtil.getDefContext().getString(com.qualcomm.robotcore.R.string.configTypeLynxColorSensor);
        }

    @Override
    public HardwareDevice.Manufacturer getManufacturer()
        {
        return Manufacturer.Lynx;
        }

    //----------------------------------------------------------------------------------------------
    // OpticalDistanceSensor / LightSensor
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
        return parameters.proximitySaturation;
        }

    @Override public String status()
        {
        return String.format(Locale.getDefault(), "%s on %s", getDeviceName(), getConnectionInfo());
        }

    //----------------------------------------------------------------------------------------------
    // Raw sensor data
    //----------------------------------------------------------------------------------------------

    public int rawOptical()
        {
        return readUnsignedShort(Register.PDATA, ByteOrder.LITTLE_ENDIAN);
        }
    }
