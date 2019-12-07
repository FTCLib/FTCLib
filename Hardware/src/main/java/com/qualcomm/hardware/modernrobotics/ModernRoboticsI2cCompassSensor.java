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

import com.qualcomm.robotcore.hardware.CompassSensor;
import com.qualcomm.robotcore.hardware.I2cAddr;
import com.qualcomm.robotcore.hardware.I2cAddrConfig;
import com.qualcomm.robotcore.hardware.I2cDeviceSynch;
import com.qualcomm.robotcore.hardware.I2cDeviceSynchDevice;
import com.qualcomm.robotcore.hardware.I2cWaitControl;
import com.qualcomm.robotcore.hardware.TimestampedData;
import com.qualcomm.robotcore.hardware.configuration.annotations.DeviceProperties;
import com.qualcomm.robotcore.hardware.configuration.annotations.I2cDeviceType;
import com.qualcomm.robotcore.hardware.usb.RobotUsbDevice;
import com.qualcomm.robotcore.util.TypeConversion;

import org.firstinspires.ftc.robotcore.external.navigation.Acceleration;
import org.firstinspires.ftc.robotcore.external.navigation.MagneticFlux;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;

/**
 * {@link ModernRoboticsI2cCompassSensor} implements support for the Modern Robotics compass sensor.
 *
 * "During normal operation the LED will blink briefly at 1Hz. During Hard Iron Calibration the LED
 * will blink at 1/2Hz. During tilt up and tilt down calibration the LED will be on during a period
 * of calibration measurement."
 *
 * @see <a href="http://www.modernroboticsinc.com/compass">MR Compass Sensor</a>
 * @see <a href="https://cdn-shop.adafruit.com/datasheets/LSM303DLHC.PDF">LSM 303 datasheet</a>
 */
@I2cDeviceType
@DeviceProperties(name = "@string/mr_compass_name", description = "@string/mr_compass_description", xmlTag = "ModernRoboticsI2cCompassSensor", builtIn = true)
public class ModernRoboticsI2cCompassSensor extends I2cDeviceSynchDevice<I2cDeviceSynch> implements CompassSensor, I2cAddrConfig
    {
    //----------------------------------------------------------------------------------------------
    // Constants
    //----------------------------------------------------------------------------------------------

    public final static I2cAddr ADDRESS_I2C_DEFAULT = I2cAddr.create8bit(0x24);

    public enum Register
        {
            READ_WINDOW_FIRST(0x00),
            FIRMWARE_REV(0x00),
            MANUFACTURE_CODE(0x01),
            SENSOR_ID(0x02),
            COMMAND(0x03),
            HEADING(0x04),
            ACCELX(0x06),
            ACCELY(0x08),
            ACCELZ(0x0a),
            MAGX(0x0c),
            MAGY(0x0e),
            MAGZ(0x10),
            READ_WINDOW_LAST(MAGZ.bVal+1),
            ACCELX_OFFSET(0x12),
            ACCELY_OFFSET(0x14),
            ACCELZ_OFFSET(0x16),
            MAGX_OFFSET(0x18),
            MAGY_OFFSET(0x1a),
            MAGZ_OFFSET(0x1c),
            MAG_TILT_COEFF(0x1e),
            ACCEL_SCALE_COEFF(0x20),
            MAG_SCALE_COEFF_X(0x22),
            MAG_SCALE_COEFF_Y(0x24),
            UNKNOWN(-1);

        public byte bVal;
        Register(int bVal) { this.bVal = (byte)bVal; }
        }

    public enum Command
        {
            NORMAL(0x00),
            CALIBRATE_IRON(0x43),
            ACCEL_NULL_X(0x58),
            ACCEL_NULL_Y(0x59),
            ACCEL_NULL_Z(0x5A),
            ACCEL_GAIN_ADJUST(0x47),
            MEASURE_TILT_UP(0x55),
            MEASURE_TILT_DOWN(0x44),
            WRITE_EEPROM(0x57),
            CALIBRATION_FAILED(0x46),
            UNKNOWN(-1);

        public byte bVal;
        Command(int bVal) { this.bVal = (byte)bVal; }
        public static Command fromByte(byte b) {
            for (Command command : values()) {
                if (command.bVal == b) return command;
                }
            return UNKNOWN;
            }
        }

    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    // none

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public ModernRoboticsI2cCompassSensor(I2cDeviceSynch deviceClient)
        {
        super(deviceClient, true);

        this.setOptimalReadWindow();
        this.deviceClient.setI2cAddress(ADDRESS_I2C_DEFAULT);

        this.registerArmingStateCallback(false);
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

    @Override
    protected synchronized boolean doInitialize()
        {
        setMode(CompassMode.MEASUREMENT_MODE);
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
        return String.format(Locale.getDefault(), "Modern Robotics Compass Sensor %s", firmwareVersion);
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

    public int readShort(Register reg)
        {
        return TypeConversion.byteArrayToShort(this.deviceClient.read(reg.bVal, 2), ByteOrder.LITTLE_ENDIAN);
        }

    public void writeShort(Register reg, short value)
        {
        this.deviceClient.write(reg.bVal, TypeConversion.shortToByteArray(value, ByteOrder.LITTLE_ENDIAN));
        }

    public void writeCommand(Command command)
        {
        this.deviceClient.waitForWriteCompletions(I2cWaitControl.ATOMIC);    // avoid overwriting previous command
        this.write8(Register.COMMAND, command.bVal);
        }

    public Command readCommand()
        {
        return Command.fromByte(this.read8(Register.COMMAND));
        }

    //----------------------------------------------------------------------------------------------
    // CompassSensor
    //----------------------------------------------------------------------------------------------

    public Acceleration getAcceleration()
        {
        // Capture all the data at once so as to get them all from one read
        TimestampedData ts = this.deviceClient.readTimeStamped(Register.ACCELX.bVal, 3 * 2/*sizeof short*/);
        ByteBuffer buffer = ByteBuffer.wrap(ts.data).order(ByteOrder.LITTLE_ENDIAN);
        // units are milli-earth's-gravity
        int mgX = (buffer.getShort());
        int mgY = (buffer.getShort());
        int mgZ = (buffer.getShort());
        double scale = 0.001;
        return Acceleration.fromGravity(mgX * scale, mgY * scale, mgZ * scale, ts.nanoTime);
        }

    public MagneticFlux getMagneticFlux()
        {
        // Capture all the data at once so as to get them all from one read
        TimestampedData ts = this.deviceClient.readTimeStamped(Register.MAGX.bVal, 3 * 2/*sizeof short*/);
        ByteBuffer buffer = ByteBuffer.wrap(ts.data).order(ByteOrder.LITTLE_ENDIAN);
        // units are in Gauss. One Tesla is 10,000 Gauss.
        int magX = (buffer.getShort());
        int magY = (buffer.getShort());
        int magZ = (buffer.getShort());
        double scale = 0.0001;
        return new MagneticFlux(magX * scale, magY * scale,  magZ * scale, ts.nanoTime);
        }

    @Override public double getDirection()
        {
        return this.readShort(Register.HEADING);
        }

    @Override public String status()
        {
        return String.format(Locale.getDefault(), "%s on %s", getDeviceName(), this.getConnectionInfo());
        }

    /*
        The calibration process is as follows:

        "Set the command to 0x43 to set calibration mode. The compass does not have to be facing
        north, any heading will do. Once it is in cal mode rotate the compass clockwise at least
        360 degrees making sure it does not tilt. This rotation should take at least 5 seconds so
        don't turn too fast. Once the process is compete your program must write a 0x00 to the
        command indicating that you have completed the calibration procedure.

        Then read back the command and if the cal was successful the command will contain the 0x00.
        If there was an error and the cal did not work then the status of 0x46 (F) will be read
        from the command byte indicating a Failed state.
     */

    public boolean isCalibrating()
        {
        return this.readCommand()==Command.CALIBRATE_IRON;
        }

    @Override public boolean calibrationFailed()
        {
        return this.readCommand()==Command.CALIBRATION_FAILED;
        }

    @Override public void setMode(CompassMode mode)
        {
        this.writeCommand(mode==CompassMode.CALIBRATION_MODE ? Command.CALIBRATE_IRON : Command.NORMAL);
        }

    //----------------------------------------------------------------------------------------------
    // I2cAddrConfig
    //----------------------------------------------------------------------------------------------

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
