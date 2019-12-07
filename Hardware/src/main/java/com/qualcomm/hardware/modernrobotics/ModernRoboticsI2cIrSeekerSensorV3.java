/*
 * Copyright (c) 2014, 2015 Qualcomm Technologies Inc
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * (subject to the limitations in the disclaimer below) provided that the following conditions are
 * met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the
 * distribution.
 *
 * Neither the name of Qualcomm Technologies Inc nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS LICENSE. THIS
 * SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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

package com.qualcomm.hardware.modernrobotics;

import com.qualcomm.robotcore.hardware.I2cAddr;
import com.qualcomm.robotcore.hardware.I2cAddrConfig;
import com.qualcomm.robotcore.hardware.I2cDeviceSynch;
import com.qualcomm.robotcore.hardware.I2cDeviceSynchDevice;
import com.qualcomm.robotcore.hardware.IrSeekerSensor;
import com.qualcomm.robotcore.hardware.usb.RobotUsbDevice;
import com.qualcomm.robotcore.util.TypeConversion;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.nio.ByteOrder;

/**
 * {@link ModernRoboticsI2cIrSeekerSensorV3} supports the Modern Robotics IR Seeker V3.
 * @see <a href="http://www.modernroboticsinc.com/ir-seeker-v3-2">MR IR Seeker V3</a>
 */
public class ModernRoboticsI2cIrSeekerSensorV3 extends I2cDeviceSynchDevice<I2cDeviceSynch> implements IrSeekerSensor, I2cAddrConfig
    {
    //------------------------------------------------------------------------------------------------
    // Constants
    //------------------------------------------------------------------------------------------------

    public static final I2cAddr ADDRESS_I2C_DEFAULT = I2cAddr.create8bit(0x38);

    public enum Register
        {
        READ_WINDOW_FIRST(0x00),
        FIRMWARE_REV(0x00),
        MANUFACTURE_CODE(0x01),
        SENSOR_ID(0x02),
        UNUSED(0x03),
        DIR_DATA_1200(0x04),
        SIGNAL_STRENTH_1200(0x05),
        DIR_DATA_600(0x06),
        SIGNAL_STRENTH_600(0x07),
        LEFT_SIDE_DATA_1200(0x08),
        RIGHT_SIDE_DATA_1200(0x0A),
        LEFT_SIDE_DATA_600(0x0C),
        RIGHT_SIDE_DATA_600(0x0E),
        READ_WINDOW_LAST(RIGHT_SIDE_DATA_600.bVal + 1),
        UNKNOWN(-1);
        public byte bVal;
        Register(int value) { this.bVal = (byte) value; }
        }

    public static final double MAX_SENSOR_STRENGTH = 255.0;

    //------------------------------------------------------------------------------------------------
    // State
    //------------------------------------------------------------------------------------------------

    protected Mode      mode = Mode.MODE_1200HZ;
    protected double    signalDetectedThreshold;    // set in doInitialize

    //------------------------------------------------------------------------------------------------
    // Construction
    //------------------------------------------------------------------------------------------------

    public ModernRoboticsI2cIrSeekerSensorV3(I2cDeviceSynch deviceClient)
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
                Register.READ_WINDOW_FIRST.bVal,
                Register.READ_WINDOW_LAST.bVal - Register.READ_WINDOW_FIRST.bVal + 1,
                I2cDeviceSynch.ReadMode.REPEAT);
        this.deviceClient.setReadWindow(readWindow);
        }

    @Override protected boolean doInitialize()
        {
        setMode(Mode.MODE_1200HZ);
        this.signalDetectedThreshold = 1 / MAX_SENSOR_STRENGTH;
        return true;
        }

    @Override public Manufacturer getManufacturer()
        {
        return Manufacturer.ModernRobotics;
        }

    @Override public String getDeviceName()
        {
        return String.format("%s %s",
                AppUtil.getDefContext().getString(com.qualcomm.robotcore.R.string.configTypeIrSeekerV3),
                new RobotUsbDevice.FirmwareVersion(this.read8(Register.FIRMWARE_REV)));
        }

    @Override public int getVersion()
        {
        return 3;
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
        this.deviceClient.write8(reg.bVal, value);
        }

    protected short readShort(Register reg)
        {
        return TypeConversion.byteArrayToShort(this.deviceClient.read(reg.bVal, 2), ByteOrder.LITTLE_ENDIAN);
        }

    //------------------------------------------------------------------------------------------------
    // Operations
    //------------------------------------------------------------------------------------------------

    @Override
    public String toString()
        {
        if (signalDetected())
            {
            return String.format("IR Seeker: %3.0f%% signal at %6.1f degrees", getStrength() * 100.0, getAngle());
            }
        else
            {
            return "IR Seeker:  --% signal at  ---.- degrees";
            }
        }

    @Override
    public synchronized void setSignalDetectedThreshold(double threshold)
        {
        signalDetectedThreshold = threshold;
        }

    @Override
    public double getSignalDetectedThreshold()
        {
        return signalDetectedThreshold;
        }

    @Override
    public synchronized void setMode(Mode mode)
        {
        this.mode = mode;
        }

    @Override
    public Mode getMode()
        {
        return mode;
        }

    @Override
    public boolean signalDetected()
        {
        return (getStrength() > signalDetectedThreshold);
        }

    @Override
    public synchronized double getAngle()
        {
        /**
         * "The heading value gives an indication of the source direction. If the value is negative,
         * then the source is to the left of center. If the value is positive, then the source is
         * to the right of center. The magnitude of the values gives an indication of how far off
         * the axis the source is. If the value is zero, then the source is in the center of the
         * field of view."
         */
        Register reg = getMode()==Mode.MODE_1200HZ ? Register.DIR_DATA_1200 : Register.DIR_DATA_600;
        return this.read8(reg);
        }

    @Override
    public synchronized double getStrength()
        {
        /**
         * "The strength value represents the magnitude of the receive signal. If this value is set
         * to 0, it means that not enough IR signal is available to estimate the heading value. The
         * value of the strength will increase as an IR source approaches the sensor.
         */
        Register reg = getMode()==Mode.MODE_1200HZ ? Register.SIGNAL_STRENTH_1200 : Register.SIGNAL_STRENTH_600;
        return TypeConversion.unsignedByteToDouble(this.read8(reg)) / MAX_SENSOR_STRENGTH;
        }

    // Returns right and left raw values, scaled from -1 to 1.
    @Override
    public synchronized IrSeekerIndividualSensor[] getIndividualSensors()
        {
        // we don't know the angle of these sensors so we will give a bad estimate; -1 for left, +1 for right
        IrSeekerIndividualSensor sensors[] = new IrSeekerIndividualSensor[2];

        /* TODO: the scaling here seems to be wrong: we've got 16 bit values, and we're scaling by an 8 bit max.
           TODO: We don't know whether they're signed or unsigned 16 bit values, but either way, the result won't be in [-1,1].
           TODO: We leave like this (for now, at least) for compatibility reasons until we understand better.
        */
        Register reg = getMode()==Mode.MODE_1200HZ ? Register.LEFT_SIDE_DATA_1200 : Register.LEFT_SIDE_DATA_600;
        double strength = this.readShort(reg) / MAX_SENSOR_STRENGTH;
        sensors[0] = new IrSeekerIndividualSensor(-1, strength);

        reg = getMode()==Mode.MODE_1200HZ ? Register.RIGHT_SIDE_DATA_1200 : Register.RIGHT_SIDE_DATA_600;
        strength = this.readShort(reg) / MAX_SENSOR_STRENGTH;
        sensors[1] = new IrSeekerIndividualSensor(1, strength);

        return sensors;
        }

    @Override
    public synchronized void setI2cAddress(I2cAddr newAddress)
        {
        // In light of the existence of I2C multiplexers, we don't *require* a valid Modern Robotics I2cAddr
        this.deviceClient.setI2cAddress(newAddress);
        }

    @Override
    public I2cAddr getI2cAddress()
        {
        return this.deviceClient.getI2cAddress();
        }
    }
