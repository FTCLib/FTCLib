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

import android.graphics.Color;
import androidx.annotation.ColorInt;

import com.qualcomm.robotcore.hardware.ColorSensor;
import com.qualcomm.robotcore.hardware.I2cAddr;
import com.qualcomm.robotcore.hardware.I2cAddrConfig;
import com.qualcomm.robotcore.hardware.I2cDeviceSynch;
import com.qualcomm.robotcore.hardware.I2cDeviceSynchDevice;
import com.qualcomm.robotcore.hardware.I2cWaitControl;
import com.qualcomm.robotcore.hardware.NormalizedColorSensor;
import com.qualcomm.robotcore.hardware.NormalizedRGBA;
import com.qualcomm.robotcore.hardware.SwitchableLight;
import com.qualcomm.robotcore.hardware.usb.RobotUsbDevice;
import com.qualcomm.robotcore.util.TypeConversion;

/**
 * {@link ModernRoboticsI2cColorSensor} provides support for the Modern Robotics Color Sensor.
 * @see <a href="http://www.modernroboticsinc.com/color-sensor">MR Color Sensor</a>
 */
public class ModernRoboticsI2cColorSensor extends I2cDeviceSynchDevice<I2cDeviceSynch>
        implements ColorSensor, NormalizedColorSensor, SwitchableLight, I2cAddrConfig
    {
    //----------------------------------------------------------------------------------------------
    // Constants
    //----------------------------------------------------------------------------------------------

    public final static I2cAddr ADDRESS_I2C_DEFAULT = I2cAddr.create8bit(0x3C);

    public enum Register
        {
            FIRMWARE_REV(0x00),
            MANUFACTURE_CODE(0x01),
            SENSOR_ID(0x02),
            COMMAND(0x03),
            COLOR_NUMBER(0x04),

            RED(0x05),
            GREEN(0x06),
            BLUE(0x07),
            ALPHA(0x08),

            COLOR_INDEX(0x09),
            RED_INDEX(0x0a),
            GREEN_INDEX(0x0b),
            BLUE_INDEX(0x0c),

            RED_READING(0x0e),
            GREEN_READING(0x10),
            BLUE_READING(0x12),
            ALPHA_READING(0x14),

            NORMALIZED_RED_READING(0x16),
            NORMALIZED_GREEN_READING(0x18),
            NORMALIZED_BLUE_READING(0x1a),
            NORMALIZED_ALPHA_READING(0x1c),

            READ_WINDOW_FIRST(RED.bVal),
            READ_WINDOW_LAST(NORMALIZED_ALPHA_READING.bVal+1);
        public byte bVal;
        Register(int value) { this.bVal = (byte)value; }
        }

    public enum Command
        {
            ACTIVE_LED(0x00),
            PASSIVE_LED(0x01),
            HZ50(0x35),
            HZ60(0x36),
            CALIBRATE_BLACK(0x042),
            CALIBRATE_WHITE(0x43);
        public byte bVal;
        Command(int value) { this.bVal = (byte)value; }
        }

    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    /** In {@link NormalizedColorSensor} we report the 16-bit sensor-normalized values, not the
     * 8 bit colors reported through {@link ColorSensor}. */
    protected final float colorNormalizationFactor = 1.0f / 65536.0f;

    protected boolean isLightOn = false;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public ModernRoboticsI2cColorSensor(I2cDeviceSynch deviceClient)
        {
        super(deviceClient, true);

        I2cDeviceSynch.ReadWindow readWindow = new I2cDeviceSynch.ReadWindow(
              Register.READ_WINDOW_FIRST.bVal,
              Register.READ_WINDOW_LAST.bVal - Register.READ_WINDOW_FIRST.bVal + 1,
              I2cDeviceSynch.ReadMode.REPEAT);
        this.deviceClient.setReadWindow(readWindow);
        this.deviceClient.setI2cAddress(ADDRESS_I2C_DEFAULT);

        this.registerArmingStateCallback(false);
        this.deviceClient.engage();
        }

    @Override
    protected synchronized boolean doInitialize()
        {
        enableLed(true);
        return true;
        }

    @Override
    public Manufacturer getManufacturer()
        {
        return Manufacturer.ModernRobotics;
        }

    @Override public String getDeviceName()
        {
        RobotUsbDevice.FirmwareVersion firmwareVersion = new RobotUsbDevice.FirmwareVersion(this.read8(Register.FIRMWARE_REV));
        return String.format("Modern Robotics I2C Color Sensor %s", firmwareVersion);
        }

    //------------------------------------------------------------------------------------------------
    // Utility
    //------------------------------------------------------------------------------------------------

    public byte read8(Register reg)
        {
        return this.deviceClient.read8(reg.bVal);
        }
    public void write8(Register reg, byte value)
        {
        this.deviceClient.write8(reg.bVal, value);
        }

    public int readUnsignedByte(Register register)
        {
        return TypeConversion.unsignedByteToInt(read8(register));
        }
    public int readUnsignedShort(Register register)
        {
        return TypeConversion.unsignedShortToInt(TypeConversion.byteArrayToShort(this.deviceClient.read(register.bVal, 2)));
        }

    public void writeCommand(Command command)
        {
        this.deviceClient.waitForWriteCompletions(I2cWaitControl.ATOMIC);    // avoid overwriting previous command
        this.write8(Register.COMMAND, command.bVal);
        }

    //----------------------------------------------------------------------------------------------
    // Operations
    //
    // red(), green() and blue() return 8 bit values for historical reasons. The normalized
    // versions use greater precision as they are not similarly constrained.
    //----------------------------------------------------------------------------------------------

    @Override
    public String toString()
        {
        return String.format("argb: 0x%08x", argb());
        }

    @Override
    public int red()
        {
        return readUnsignedByte(Register.RED);
        }

    @Override
    public int green()
        {
        return readUnsignedByte(Register.GREEN);
        }

    @Override
    public int blue()
        {
        return readUnsignedByte(Register.BLUE);
        }

    @Override
    public int alpha()
        {
        return readUnsignedByte(Register.ALPHA);
        }

    @Override
    public @ColorInt int argb()
        {
        return Color.argb(alpha(), red(), green(), blue());
        }

    @Override public NormalizedRGBA getNormalizedColors()
        {
        NormalizedRGBA result = new NormalizedRGBA();
        result.red   = readUnsignedShort(Register.NORMALIZED_RED_READING)   * colorNormalizationFactor;
        result.green = readUnsignedShort(Register.NORMALIZED_GREEN_READING) * colorNormalizationFactor;
        result.blue  = readUnsignedShort(Register.NORMALIZED_BLUE_READING)  * colorNormalizationFactor;
        result.alpha = readUnsignedShort(Register.NORMALIZED_ALPHA_READING) * colorNormalizationFactor;
        return result;
        }

    @Override
    public synchronized void enableLed(boolean enable)
        {
        writeCommand(enable ? Command.ACTIVE_LED : Command.PASSIVE_LED);
        this.isLightOn = enable;
        }

    @Override public void enableLight(boolean enable)
        {
        enableLed(enable);
        }

    @Override public synchronized boolean isLightOn()
        {
        return isLightOn;
        }

    @Override
    public void setI2cAddress(I2cAddr newAddress)
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
