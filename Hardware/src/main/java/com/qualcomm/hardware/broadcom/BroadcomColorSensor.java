/*
Copyright (c) 2019 REV Robotics LLC

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of REV Robotics LLC nor the names of his contributors may be used to
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
package com.qualcomm.hardware.broadcom;

import com.qualcomm.robotcore.hardware.ColorSensor;
import com.qualcomm.robotcore.hardware.I2cAddr;
import com.qualcomm.robotcore.hardware.I2cDeviceSynch;
import com.qualcomm.robotcore.hardware.NormalizedColorSensor;
import com.qualcomm.robotcore.util.Range;

/**
 * {@link BroadcomColorSensor} is an extension of ColorSensor that provides additional functionality
 * supported by a family of color sensor chips from Broadcom.
 */
@SuppressWarnings("WeakerAccess")
public interface BroadcomColorSensor extends ColorSensor, NormalizedColorSensor
{
    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    /**
     * Initialize the sensor using the indicated set of parameters.
     * @param parameters the parameters with which to initialize the device
     * @return whether initialization was successful or not
     */
    boolean initialize(Parameters parameters);

    /**
     * Returns the parameters which which initialization was last attempted, if any
     * @return the parameters which which initialization was last attempted, if any
     */
    Parameters getParameters();

    /**
     * Instances of Parameters contain data indicating how the
     * sensor is to be initialized.
     *
     * @see #initialize(Parameters)
     */
    class Parameters implements Cloneable
    {
        //------------------------------------------------------------------------------------------
        // State
        //------------------------------------------------------------------------------------------

        /** the device id expected to be reported by the color sensor chip */
        public int deviceId;

        /** the address at which the sensor resides on the I2C bus. */
        public I2cAddr i2cAddr;

        /** the gain level to use for color sensing */
        public Gain gain = Gain.GAIN_3;

        /** controls the number of times the proximity LED is pulsed each cycle. */
        public int proximityPulseCount = 32;

        /** number of bits used for proximity sensing */
        public static PSResolution proximityResolution = PSResolution.R11BIT;

        /** periodic measurement rate for the proximity sensor */
        public PSMeasurementRate proximityMeasRate = PSMeasurementRate.R100ms;

        /** when using proximity, controls the nominal proximity LED drive current */
        public LEDCurrent ledCurrent = LEDCurrent.CURRENT_125mA;

        /** the maximum possible raw proximity value read. is sensitive to ledDrive
         * and proximityPulseCount. */
        public int proximitySaturation = 2047;

        /** the maximum possible color value. */
        public int colorSaturation = 65535;

        /** LED pulse modulation frequency. */
        public LEDPulseModulation pulseModulation = LEDPulseModulation.LED_PULSE_60kHz;

        /** debugging aid: enable logging for this device? */
        public boolean loggingEnabled = false;

        /** debugging aid: the logging tag to use when logging */
        public String loggingTag = "BroadcomColorSensor";

        /** set of registers to read in background, if supported by underlying I2cDeviceSynch */
        public I2cDeviceSynch.ReadWindow readWindow = new I2cDeviceSynch.ReadWindow(
                Register.READ_WINDOW_FIRST.bVal,
                Register.READ_WINDOW_LAST.bVal-Register.READ_WINDOW_FIRST.bVal+1,
                I2cDeviceSynch.ReadMode.REPEAT);

        //------------------------------------------------------------------------------------------
        // Construction
        //------------------------------------------------------------------------------------------

        public Parameters(I2cAddr i2cAddr, int deviceId)
        {
            this.i2cAddr = i2cAddr;
            this.deviceId = deviceId;
        }
        public static Parameters createForAPDS9151()
        {
            return new Parameters(BROADCOM_APDS9151_ADDRESS, BROADCOM_APDS9151_ID);
        }
        public Parameters clone()
        {
            try {
                return (Parameters)super.clone();
            }
            catch (CloneNotSupportedException e)
            {
                throw new RuntimeException("internal error: Parameters not cloneable");
            }
        }
    }

    //----------------------------------------------------------------------------------------------
    // Status inquiry
    //----------------------------------------------------------------------------------------------

    /**
     * Returns the flavor of the Broadcom color sensor as reported by the chip itself
     * @return the flavor of the Broadcom color sensor as reported by the chip itself
     */
    byte getDeviceID();

    //----------------------------------------------------------------------------------------------
    // Low level reading and writing
    //----------------------------------------------------------------------------------------------

    /**
     * Low level: read the byte starting at the indicated register
     *
     * @param register the location from which to read the data
     * @return the data that was read
     */
    byte read8(Register register);

    /**
     * Low level: read data starting at the indicated register
     *
     * @param register the location from which to read the data
     * @param cb       the number of bytes to read
     * @return the data that was read
     */
    byte[] read(Register register, int cb);

    /**
     * Low level: write a byte to the indicated register
     *
     * @param register the location at which to write the data
     * @param bVal     the data to write
     */
    void write8(Register register, int bVal);

    /** Low level: write data starting at the indicated register */
    void write(Register register, byte[] data);

    //------------------------------------------------------------------------------------------
    // Constants
    //------------------------------------------------------------------------------------------

    /**
     * {@link Register} provides symbolic names for interesting device registers
     */
    enum Register
    {
        MAIN_CTRL(0x00),
        PS_LED(0x01),
        PS_PULSES(0x02),
        PS_MEAS_RATE(0x03),
        LS_MEAS_RATE(0x04),
        LS_GAIN(0x05),
        PART_ID(0x06),
        MAIN_STATUS(0x07),
        PS_DATA(0x08),
        LS_DATA_IR(0x0A),
        LS_DATA_GREEN(0x0D),
        LS_DATA_BLUE(0x10),
        LS_DATA_RED(0x13),
        INT_CFG(0x19),
        INT_PST(0x1A),
        PS_THRES_UP(0x1B),
        PS_THRES_LOW(0x1E),
        PS_CAN(0x1F),
        LS_THRES_UP(0x21),
        LS_THRES_LOW(0x24),
        LS_THRES_VAR(0x27),

        READ_WINDOW_FIRST(PS_DATA.bVal), // may as well...
        READ_WINDOW_LAST(LS_DATA_RED.bVal +1);

        public final byte bVal;
        Register(int i) { this.bVal = (byte) i; }
    }

    enum MainControl
    {
        RES7(0x80), /* reserved, write as zero */
        SAI_PS(0x40), /* Sleep after Interrupt for PS */
        SAI_LS(0x20), /* Sleep after Interrupt for LS */
        SW_RESET(0x10), /* If bit is set to 1, a software reset will be triggered immediately */
        RES3(0x08),  /* reserved, write as zero */
        RGB_MODE(0x04),  /* If bit is set to 1, color channels are activated */
        LS_EN(0x02),  /* Enable light sensor */
        PS_EN(0x01),  /* Proximity sensor active */
        OFF(0x00);  /* Nothing on */

        public byte bitOr(MainControl him) { return (byte)(this.bVal | him.bVal); }
        public byte bitOr(byte him)   { return (byte)(this.bVal | him); }

        public final byte bVal;
        MainControl(int i) { this.bVal = (byte) i; }
    }

    enum MainStatus
    {
        POWER_ON_STATUS(0x20),
        LS_INT_STAT(0x10),
        LS_DATA_STATUS(0x08),
        PS_LOGIC_SIG_STAT(0x04),
        PS_INT_STAT(0x02),
        PS_DATA_STAT(0x01);

        public byte bitOr(MainStatus him) { return (byte)(this.bVal | him.bVal); }
        public byte bitOr(byte him)   { return (byte)(this.bVal | him); }

        public final byte bVal;
        MainStatus(int i) { this.bVal = (byte) i; }
    }

    enum Gain
    {
        UNKNOWN(-1),
        GAIN_1(0x00),
        GAIN_3(0x01), /* default value */
        GAIN_6(0x02),
        GAIN_9(0x03),
        GAIN_18(0x04);

        public final byte bVal;
        Gain(int i) { this.bVal = (byte) i; }
        public static Gain fromByte(byte byteVal) {
            for (Gain value : values()) { if (value.bVal == byteVal) return value; }
            return UNKNOWN;
        }
    }

    enum LEDCurrent
    {
        CURRENT_2_5mA(0x00),
        CURRENT_5mA(0x01),
        CURRENT_10mA(0x02),
        CURRENT_25mA(0x03),
        CURRENT_50mA(0x04),
        CURRENT_75mA(0x05),
        CURRENT_100mA(0x06), /* default value */
        CURRENT_125mA(0x07);

        public byte bitOr(LEDCurrent him) { return (byte)(this.bVal | him.bVal); }
        public byte bitOr(byte him)   { return (byte)(this.bVal | him); }

        public final byte bVal;
        LEDCurrent(int i) { this.bVal = (byte) i; }
    }

    enum LEDPulseModulation
    {
        RES0(0x00),
        RES1(0x01),
        RES2(0x02),
        LED_PULSE_60kHz(0x03), /* default value */
        LED_PULSE_70kHz(0x04),
        LED_PULSE_80kHz(0x05),
        LED_PULSE_90kHz(0x06),
        LED_PULSE_100kHz(0x07);

        public byte bitOr(LEDPulseModulation him) { return (byte)(this.bVal | him.bVal); }
        public byte bitOr(byte him)   { return (byte)(this.bVal | him); }

        public final byte bVal;
        LEDPulseModulation(int i) { this.bVal = (byte) i; }
    }

    enum PSResolution
    {
        R8BIT(0x00), /* default value */
        R9BIT(0x01),
        R10BIT(0x02),
        R11BIT(0x03);

        public byte bitOr(PSResolution him) { return (byte)(this.bVal | him.bVal); }
        public byte bitOr(byte him)   { return (byte)(this.bVal | him); }

        public final byte bVal;
        PSResolution(int i) { this.bVal = (byte) i; }
    }

    enum PSMeasurementRate
    {
        RES(0x00),
        R6_25ms(0x01),
        R12_5ms(0x02),
        R25ms(0x03),
        R50ms(0x04),
        R100ms(0x05), /* default value */
        R200ms(0x06),
        R400ms(0x07);

        public byte bitOr(PSMeasurementRate him) { return (byte)(this.bVal | him.bVal); }
        public byte bitOr(byte him)   { return (byte)(this.bVal | him); }

        public final byte bVal;
        PSMeasurementRate(int i) { this.bVal = (byte) i; }
    }

    // The 7-bit I2C address of device
    I2cAddr BROADCOM_APDS9151_ADDRESS = I2cAddr.create7bit(0x52);

    // ID value for device
    byte BROADCOM_APDS9151_ID = (byte) 0xC2;
}

