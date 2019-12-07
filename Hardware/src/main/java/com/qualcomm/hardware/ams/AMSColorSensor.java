/*
Copyright (c) 2016 Robert Atkinson & Steve Geffner

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the names of Robert Atkinson nor Steve Geffner nor the names of their contributors
may be used to endorse or promote products derived from this software without specific
prior written permission.

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
package com.qualcomm.hardware.ams;

import com.qualcomm.robotcore.hardware.ColorSensor;
import com.qualcomm.robotcore.hardware.I2cAddr;
import com.qualcomm.robotcore.hardware.I2cDeviceSynch;
import com.qualcomm.robotcore.hardware.NormalizedColorSensor;

/**
 * {@link AMSColorSensor} is an extension of ColorSensor that provides additional functionality
 * supported by a family of color sensor chips from AMS.
 *
 * @see <a href="http://ams.com/eng/Support/Demoboards/Light-Sensors/(show)/145298">AMS Color Sensors</a>
 * @see <a href="http://adafru.it/1334">Adafruit color sensor</a>
 */
@SuppressWarnings("WeakerAccess")
public interface AMSColorSensor extends ColorSensor, NormalizedColorSensor
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
        public Gain gain = Gain.GAIN_4;

        /** the integration time to use for color sensing */
        public int atime = atimeFromMs(24);

        /** whether we should turn on the proximity functionality if it
         * is available on the chip in question */
        public boolean useProximityIfAvailable = true;

        /** when using the proximity functionality, controls the number of times
         * the proximity LED is pulsed each cycle. */
        public int proximityPulseCount = 8;

        /** when using proximity, controls the nominal proximity LED drive current */
        public LEDDrive ledDrive = LEDDrive.Percent12_5;

        /** the maximum possible raw proximity value read. is sensitive to ledDrive
         * and proximityPulseCount. */
        public int proximitySaturation = 1023;

        /** debugging aid: enable logging for this device? */
        public boolean loggingEnabled = false;

        /** debugging aid: the logging tag to use when logging */
        public String loggingTag = "AMSColorSensor";

        /** set of registers to read in background, if supported by underlying I2cDeviceSynch */
        public I2cDeviceSynch.ReadWindow readWindow = new I2cDeviceSynch.ReadWindow(
                Register.READ_WINDOW_FIRST.bVal,
                Register.READ_WINDOW_LAST.bVal-Register.READ_WINDOW_FIRST.bVal+1,
                I2cDeviceSynch.ReadMode.REPEAT);

        //------------------------------------------------------------------------------------------
        // Accessing
        //------------------------------------------------------------------------------------------

        /** Returns the minimum integration register value ('atime') for which the accumulation
         * interval is at least the indicated duration in length. */
        public static int atimeFromMs(float msAccumulationInterval)
            {
            int intervals = (int)Math.ceil(msAccumulationInterval / 2.4f);
            return Math.max(0, 256 - intervals);
            }

        /** Returns the number of 2.4ms integration cycles currently configured for each
         * accumulation time interval */
        public int integrationCycles()
            {
            return 256 - this.atime;
            }

        /** Returns the maximum intensity count that can be reached in the currently configured
         * accumulation time interval */
        public int getMaximumReading()
            {
            return Math.min(65535, 1024 * integrationCycles());
            }

        /** Returns the duration of the accumulation interval */
        public float msAccumulationInterval()
            {
            return integrationCycles() * 2.4f;
            }

        //------------------------------------------------------------------------------------------
        // Construction
        //------------------------------------------------------------------------------------------

        public Parameters(I2cAddr i2cAddr, int deviceId)
            {
            this.i2cAddr = i2cAddr;
            this.deviceId = deviceId;
            }
        public static Parameters createForTCS34725()
            {
            return new Parameters(AMS_TCS34725_ADDRESS, AMS_TCS34725_ID);
            }
        public static Parameters createForTMD37821()
            {
            return new Parameters(AMS_TMD37821_ADDRESS, AMS_TMD37821_ID);
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
     * Returns the flavor of the AMS color sensor as reported by the chip itself
     * @return the flavor of the AMS color sensor as reported by the chip itself
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
            ENABLE(0x00),
            ATIME(0x01),
            REGISTER2(0x02), // unknown register (omitted from documentation) Is this actually PTIME?
            WTIME(0x03),     // Wait time = if AMS_COLOR_ENABLE_WEN is asserted
            AILT(0x04),
            AIHT(0x06),
            PERS(0x0C),
            CONFIGURATION(0x0D),
            PPLUSE(0x0E),    // proximity sensor pulse count config
            CONTROL(0x0F),
            DEVICE_ID(0x12),
            STATUS(0x13),
            ALPHA(0x14),     // clear color value: 0x14 = low byte, 0x15 = high byte
            RED(0x16),       // red color value: 0x16 = low byte, 0x17 = high byte
            GREEN(0x18),     // etc.
            BLUE(0x1A),
            PDATA(0x1C),     // proximity value: short: low=0x1C high=0x1D

            READ_WINDOW_FIRST(WTIME.bVal), // may as well...
            READ_WINDOW_LAST(BLUE.bVal +1);

        public final byte bVal;
        Register(int i) { this.bVal = (byte) i; }
        }

    enum Enable
        {
            RES7(0x80), /* reserved, write as zero */
            RES6(0x40), /* reserved, write as zero */
            PIEN(0x20), /* Proximity interrupt enable ('reserved' on the TCS3472) */
            AIEN(0x10), /* RGBC Interrupt Enable */
            WEN(0x08),  /* Wait enable - Writing 1 activates the wait timer */
            PEN(0x04),  /* Proximity enable ('reserved' on the TCS3472) */
            AEN(0x02),  /* RGBC Enable - Writing 1 actives the ADC, 0 disables it */
            PON(0x01),  /* Power on - Writing 1 activates the internal oscillator, 0 disables it */
            OFF(0x00),  /* Nothing on */
            UNKNOWN(-1);

        public byte bitOr(Enable him) { return (byte)(this.bVal | him.bVal); }
        public byte bitOr(byte him)   { return (byte)(this.bVal | him); }

        public final byte bVal;
        Enable(int i) { this.bVal = (byte) i; }
        }

    /** Wait time is set 2.4 ms increments unless the WLONG bit is
      * asserted, in which case the wait times are 12× longer. WTIME is
      * programmed as a 2’s complement number. */
    enum Wait
        {
            MS_2_4(0xFF),
            MS_204(0xAB),
            MS_614(0x00),
            UNKNOWN(-2);    // -1 is taken
        public final byte bVal;
        Wait(int i) { this.bVal = (byte) i; }
        }

    enum Pers
        {
            CYCLE_NONE(0b0000),  /* Every RGBC cycle generates an interrupt                                */
            CYCLE_1(0b0001),     /* 1 clean channel value outside threshold range generates an interrupt   */
            CYCLE_2(0b0010),     /* 2 clean channel values outside threshold range generates an interrupt  */
            CYCLE_3(0b0011),     /* 3 clean channel values outside threshold range generates an interrupt  */
            CYCLE_5(0b0100),     /* 5 clean channel values outside threshold range generates an interrupt  */
            CYCLE_10(0b0101),    /* 10 clean channel values outside threshold range generates an interrupt */
            CYCLE_15(0b0110),    /* 15 clean channel values outside threshold range generates an interrupt */
            CYCLE_20(0b0111),    /* 20 clean channel values outside threshold range generates an interrupt */
            CYCLE_25(0b1000),    /* 25 clean channel values outside threshold range generates an interrupt */
            CYCLE_30(0b1001),    /* 30 clean channel values outside threshold range generates an interrupt */
            CYCLE_35(0b1010),    /* 35 clean channel values outside threshold range generates an interrupt */
            CYCLE_40(0b1011),    /* 40 clean channel values outside threshold range generates an interrupt */
            CYCLE_45(0b1100),    /* 45 clean channel values outside threshold range generates an interrupt */
            CYCLE_50(0b1101),    /* 50 clean channel values outside threshold range generates an interrupt */
            CYCLE_55(0b1110),    /* 55 clean channel values outside threshold range generates an interrupt */
            CYCLE_60(0b1111),    /* 60 clean channel values outside threshold range generates an interrupt */
            UNKNOWN(-1);
        public final byte bVal;
        Pers(int i) { this.bVal = (byte) i; }
        }

    enum Config
        {
            NORMAL(0x00),           /* normal wait times */
            LONG_WAIT(0x02);        /* Extended wait time = 12x normal wait times via AMS_COLOR_WTIME */
        public final byte bVal;
        Config(int i) { this.bVal = (byte) i; }
        }

    enum Gain
        {
            UNKNOWN(-1),
            GAIN_1(0x00),
            GAIN_4(0x01),
            GAIN_16(0x02),
            GAIN_64(0x03),
            MASK(0x03);

        public final byte bVal;
        Gain(int i) { this.bVal = (byte) i; }
        public static Gain fromByte(byte byteVal) {
            for (Gain value : values()) { if (value.bVal == byteVal) return value; }
            return UNKNOWN;
            }
        }

    enum LEDDrive
        {
            Percent100(0x00 << 6),
            Percent50(0x01 << 6),
            Percent25(0x10 << 6),
            Percent12_5(0x11 << 6),
            MASK(0xC0);
        public final byte bVal;
        LEDDrive(int i) { this.bVal = (byte) i; }
        }

    enum Status
        {
            PINT(0x20),        /* Proximity interrupt */
            AINT(0x10),        /* RGBC Clean channel interrupt */
            PVALID(0x02),      /* Indicates that a proximity cycle has completed since PEN was asserted */
            AVALID(0x01);      /* Indicates that the RGBC cycle has completed since AEN was asserted */
        public final byte bVal;
        Status(int i) { this.bVal = (byte) i; }
        }

    /*
        ADDRESS NAME        R/W     FUNCTION                            RESET VALUE
        −−      COMMAND     W       Specifies register address              0x00
        0x00    ENABLE      R/W     Enables states and interrupts           0x00
        0x01    ATIME       R/W     RGBC time                               0xFF
        0x03    WTIME       R/W     Wait time                               0xFF
        0x04    AILTL       R/W     Clear interrupt low threshold low byte  0x00
        0x05    AILTH       R/W     Clear interrupt low threshold high byte 0x00
        0x06    AIHTL       R/W     Clear interrupt high threshold low byte 0x00
        0x07    AIHTH       R/W     Clear interrupt high threshold high byte 0x00
        0x0C    PERS        R/W     Interrupt persistence filter            0x00
        0x0D    CONFIG      R/W     Configuration                           0x00
        0x0F    CONTROL     R/W     Control                                 0x00
        0x12    ID          R       Device ID                                ID
        0x13    STATUS      R       Device status                           0x00
        0x14    CDATAL      R       Clear data low byte                     0x00
        0x15    CDATAH      R       Clear data high byte                    0x00
        0x16    RDATAL      R       Red data low byte                       0x00
        0x17    RDATAH      R       Red data high byte                      0x00
        0x18    GDATAL      R       Green data low byte                     0x00
        0x19    GDATAH      R       Green data high byte                    0x00
        0x1A    BDATAL      R       Blue data low byte                      0x00
        0x1B    BDATAH      R       Blue data high byte                     0x00
        */
    //----------------------------------------------------------------------------------------------

    // The 7-bit I2C address of this device
    I2cAddr AMS_TCS34725_ADDRESS = I2cAddr.create7bit(0x29);
    I2cAddr AMS_TMD37821_ADDRESS = I2cAddr.create7bit(0x39);

    // ID values for selected IC variants
    byte AMS_TCS34725_ID = 0x44;
    byte AMS_TMD37821_ID = 0x60;
    byte AMS_TMD37823_ID = 0x69;

    int AMS_COLOR_COMMAND_BIT                 = 0x80;
    int AMS_COLOR_COMMAND_TYPE_REPEATED_BYTE  = (0x00 << 5);
    int AMS_COLOR_COMMAND_TYPE_AUTO_INCREMENT = (0x01 << 5);
    int AMS_COLOR_COMMAND_TYPE_RESERVED       = (0x10 << 5);
    int AMS_COLOR_COMMAND_TYPE_SPECIAL        = (0x11 << 5);
    }

