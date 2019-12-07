/*
 * Copyright (c) 2015 Craig MacFarlane
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
 * Neither the name of Craig MacFarlane nor the names of its contributors may be used to
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

import com.qualcomm.robotcore.hardware.GyroSensor;
import com.qualcomm.robotcore.hardware.Gyroscope;
import com.qualcomm.robotcore.hardware.I2cAddr;
import com.qualcomm.robotcore.hardware.I2cAddrConfig;
import com.qualcomm.robotcore.hardware.I2cDeviceSynch;
import com.qualcomm.robotcore.hardware.I2cDeviceSynchDevice;
import com.qualcomm.robotcore.hardware.I2cWaitControl;
import com.qualcomm.robotcore.hardware.IntegratingGyroscope;
import com.qualcomm.robotcore.hardware.TimestampedData;
import com.qualcomm.robotcore.hardware.usb.RobotUsbDevice;
import com.qualcomm.robotcore.util.TypeConversion;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AngularVelocity;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.Axis;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;

import java.nio.ByteOrder;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * {@link ModernRoboticsI2cGyro} supports the Modern Robotics integrating gyro.
 *
 * <p>This sensor contains an L3GD20 MEMS three-axis digital output gyroscope. Internally,
 * the chip is labelled (e.g.) "AGD2 2437 JR4IJ".</p>
 *
 * @see <a href="http://www.modernroboticsinc.com/integrating-3-axis-gyro">MR Integrating Gyro</a>
 * @see <a href="http://www.modernroboticsinc.com/sensors">MR sensor documentation</a>
 * @see <a href="http://www.st.com/content/st_com/en/products/mems-and-sensors/gyroscopes/l3gd20.html">L3GD20</a>
 */
// @I2cSensor(name = "MR Gyroscope", description = "a MR gyroscope", xmlTag = "ModernRoboticsI2cGyro")  // ModernRoboticsI2cGyro is built-in
public class ModernRoboticsI2cGyro extends I2cDeviceSynchDevice<I2cDeviceSynch>
        implements GyroSensor, Gyroscope, IntegratingGyroscope, I2cAddrConfig
    {
    //----------------------------------------------------------------------------------------------
    // Constants
    //----------------------------------------------------------------------------------------------

    public final static I2cAddr ADDRESS_I2C_DEFAULT = I2cAddr.create8bit(0x20);

    public enum Register
        {
        READ_WINDOW_FIRST(0x00),
        FIRMWARE_REV(0x00),
        MANUFACTURE_CODE(0x01),
        SENSOR_ID(0x02),
        COMMAND(0x03),
        HEADING_DATA(0x04),
        INTEGRATED_Z_VALUE(0x06),
        RAW_X_VAL(0x08),
        RAW_Y_VAL(0x0A),
        RAW_Z_VAL(0x0C),
        Z_AXIS_OFFSET(0x0E),
        Z_AXIS_SCALE_COEF(0x10),
        READ_WINDOW_LAST(Z_AXIS_SCALE_COEF.bVal + 1),
        UNKNOWN(-1);

        public byte bVal;
        Register(int value) { this.bVal = (byte)value; }
        public static Register fromByte(byte bVal) {
            for (Register register : values()) {
                if (register.bVal == bVal) return register;
                }
            return UNKNOWN;
            }
        }
    
    public enum Command
        {
        NORMAL(0x00),
        CALIBRATE(0x4E),
        RESET_Z_AXIS(0x52),
        WRITE_EEPROM(0x57),
        UNKNOWN(-1);

        public byte bVal;
        Command(int value) { this.bVal = (byte)value; }
        public static Command fromByte(byte bVal) {
            for (Command command : values()) {
                if (command.bVal == bVal) return command;
                }
            return UNKNOWN;
            }
        }

    /**
     * {@link HeadingMode} can be used to configure the software to return either cartesian or
     * cardinal headings.
     *
     * @see #getHeading()
     * @see #getHeadingMode()
     * @see #setHeadingMode(HeadingMode)
     */
    public enum HeadingMode
        {
        HEADING_CARTESIAN,
        HEADING_CARDINAL,
        }

    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    // The sensor uses a measurement range of +- 250 dps. Per the sensor datasheet,
    // in this mode that has the following sensitivity.
    protected float degreesPerSecondPerDigit = .00875f;

    protected HeadingMode headingMode = HeadingMode.HEADING_CARTESIAN;

    protected float degreesPerZAxisTick; // set in setZAxisScalingCoefficient()

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public ModernRoboticsI2cGyro(I2cDeviceSynch deviceClient)
        {
        super(deviceClient, true);

        setOptimalReadWindow();
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

    @Override
    protected synchronized boolean doInitialize()
        {
        this.writeCommand(Command.NORMAL);
        this.resetZAxisIntegrator();
        this.setZAxisScalingCoefficient(1<<8);
        this.headingMode = HeadingMode.HEADING_CARTESIAN;
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
        return String.format(Locale.getDefault(), "Modern Robotics Gyroscope %s", firmwareVersion);
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

    public short readShort(Register reg)
        {
        return TypeConversion.byteArrayToShort(this.deviceClient.read(reg.bVal, 2), ByteOrder.LITTLE_ENDIAN);
        }
    public void writeShort(Register reg, short value)
        {
        this.deviceClient.write(reg.bVal, TypeConversion.shortToByteArray(value, ByteOrder.LITTLE_ENDIAN));
        }

    public void writeCommand(Command command)
        {
        // Wait for any previous command write to finish so we don't clobber it
        // before the USB controller gets a chance to see it and pass it on to the sensor
        this.deviceClient.waitForWriteCompletions(I2cWaitControl.ATOMIC);

        this.write8(Register.COMMAND, command.bVal);
        }

    public Command readCommand()
        {
        return Command.fromByte(this.read8(Register.COMMAND));
        }

    //----------------------------------------------------------------------------------------------
    // Gyroscope interface
    //----------------------------------------------------------------------------------------------

    @Override public Set<Axis> getAngularVelocityAxes()
        {
        Set<Axis> result = new HashSet<Axis>();
        result.add(Axis.X);
        result.add(Axis.Y);
        result.add(Axis.Z);
        return result;
        }

    @Override public Set<Axis> getAngularOrientationAxes()
        {
        Set<Axis> result = new HashSet<Axis>();
        result.add(Axis.Z);
        return result;
        }

    @Override public AngularVelocity getAngularVelocity(AngleUnit unit)
        {
        TimestampedData data = this.deviceClient.readTimeStamped(Register.RAW_X_VAL.bVal, 3 * 2/*sizeof short*/);

        int rawX = TypeConversion.byteArrayToShort(data.data, 0, ByteOrder.LITTLE_ENDIAN);
        int rawY = TypeConversion.byteArrayToShort(data.data, 2, ByteOrder.LITTLE_ENDIAN);
        int rawZ = TypeConversion.byteArrayToShort(data.data, 4, ByteOrder.LITTLE_ENDIAN);

        float degPerSecondX = rawX * degreesPerSecondPerDigit;
        float degPerSecondY = rawY * degreesPerSecondPerDigit;
        float degPerSecondZ = rawZ * degreesPerSecondPerDigit;

        return new AngularVelocity(AngleUnit.DEGREES,
                    degPerSecondX, degPerSecondY, degPerSecondZ, data.nanoTime)
                .toAngleUnit(unit);
        }

    @Override public Orientation getAngularOrientation(AxesReference reference, AxesOrder order, org.firstinspires.ftc.robotcore.external.navigation.AngleUnit angleUnit)
        {
        TimestampedData data = this.deviceClient.readTimeStamped(Register.INTEGRATED_Z_VALUE.bVal, 2);
        int integratedZ = TypeConversion.byteArrayToShort(data.data, ByteOrder.LITTLE_ENDIAN);
        float cartesian = AngleUnit.normalizeDegrees(degreesZFromIntegratedZ(integratedZ));
        return new Orientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES, cartesian, 0, 0, data.nanoTime)
                .toAxesReference(reference)
                .toAxesOrder(order)
                .toAngleUnit(angleUnit);
        }

    //----------------------------------------------------------------------------------------------
    // MR Gyro / Gyro interface
    //----------------------------------------------------------------------------------------------

    public synchronized void setHeadingMode(HeadingMode headingMode)
        {
        this.headingMode = headingMode;
        }

    public HeadingMode getHeadingMode()
        {
        return this.headingMode;
        }

    /**
     * "Gyro Raw Values: The three fields X, Y and Z are the unprocessed values being
     * obtained from the sensor element. These values are updated at approximately 760Hz."
     * @see <a href="http://www.modernroboticsinc.com/sensors">Modern Robotics sensor documentation</a>
     */
    @Override public int rawX()
        {
        return readShort(Register.RAW_X_VAL);
        }

    /** @see <a href="http://www.modernroboticsinc.com/sensors">Modern Robotics sensor documentation</a>
     * @see #rawX() */
    @Override public int rawY()
        {
        return readShort(Register.RAW_Y_VAL);
        }

    /** @see <a href="http://www.modernroboticsinc.com/sensors">Modern Robotics sensor documentation</a>
     * @see #rawX() */
    @Override public int rawZ()
        {
        return readShort(Register.RAW_Z_VAL);
        }

    /** @see <a href="http://www.modernroboticsinc.com/sensors">Modern Robotics sensor documentation</a>*/
    public int getZAxisOffset()
        {
        return readShort(Register.Z_AXIS_OFFSET);
        }

    /** @see <a href="http://www.modernroboticsinc.com/sensors">Modern Robotics sensor documentation</a>*/
    public void setZAxisOffset(short offset)
        {
        writeShort(Register.Z_AXIS_OFFSET, offset);
        }

    /** @see <a href="http://www.modernroboticsinc.com/sensors">Modern Robotics sensor documentation</a>*/
    public int getZAxisScalingCoefficient()
        {
        return TypeConversion.unsignedShortToInt(readShort(Register.Z_AXIS_SCALE_COEF));
        }

    /** @see <a href="http://www.modernroboticsinc.com/sensors">Modern Robotics sensor documentation</a>*/
    public void setZAxisScalingCoefficient(int zAxisScalingCoefficient)
        {
        writeShort(Register.Z_AXIS_SCALE_COEF, (short)zAxisScalingCoefficient);
        this.degreesPerZAxisTick = 256.f / zAxisScalingCoefficient;
        }

    /** @see <a href="http://www.modernroboticsinc.com/sensors">Modern Robotics sensor documentation</a>*/
    public int getIntegratedZValue()
        {
        /**
         *  "The integrated gyro Z value returns the current value obtained by integrating the Z axis
         *  rate value, adjusted by the Z axis offset continuously. This integrated value can be reset
         *  to 0 by issuing command 0x52.
         *
         *  This value can also be used as a signed heading value where CW is in the positive
         *  direction and CCW is in the negative direction.
         *
         *  The integrated Z value is subject to scaling based on the Z axis scaling coefficient. This
         *  value defaults to 0x0100 which has a binary 'decimal point' between bits 7 and 8. Thus
         *  the 0x0100 represents a value of 1.0. This value may be adjusted to ensure that a
         *  reading of 360° corresponds to one exact revolution of the sensor. The Z axis scaling
         *  coefficient must be calculated using the below formula. Once the value is entered into
         *  the Z axis scaling coefficient register, a command of 0x57 must be made to the
         *  command register to save the value to the EEPROM.
         *
         *      angleRotated / headingValue * 256 = scaleValue"
         */
        return readShort(Register.INTEGRATED_Z_VALUE);
        }

    @Override public synchronized int getHeading()
        {
        float cartesian = normalize0359(degreesZFromIntegratedZ(getIntegratedZValue()));
        if (headingMode == HeadingMode.HEADING_CARDINAL)
            {
            return truncate(cartesian==0 ? cartesian : Math.abs(cartesian - 360));
            }
        else
            {
            return truncate(cartesian);
            }
        }

    protected int truncate(float angle)
        {
        return (int)angle;
        }

    protected float normalize0359(float degrees)
        {
        degrees = AngleUnit.normalizeDegrees(degrees);
        return degrees < 0 ? degrees + 360 : degrees;
        }

    protected float degreesZFromIntegratedZ(int integratedZ)
        {
        return integratedZ * degreesPerZAxisTick;
        }

    @Override public void resetZAxisIntegrator()
        {
        // The gyro will automatically revert to COMMAND_NORMAL once the reset is complete
        this.writeCommand(Command.RESET_Z_AXIS);
        }

    @Override public String status()
        {
        return String.format(Locale.getDefault(), "%s on %s", getDeviceName(), this.getConnectionInfo());
        }

    @Override public void calibrate()
        {
        // The gyro will automatically revert to COMMAND_NORMAL once the calibration is complete
        this.writeCommand(Command.CALIBRATE);
        }

    @Override public boolean isCalibrating()
        {
        return this.readCommand()==Command.CALIBRATE;
        }

    //----------------------------------------------------------------------------------------------
    // I2cAddrConfig interface
    //----------------------------------------------------------------------------------------------

    @Override public void setI2cAddress(I2cAddr newAddress)
        {
        // In light of the existence of I2C multiplexers, we don't *require* a valid Modern Robotics I2cAddr
        this.deviceClient.setI2cAddress(newAddress);
        }

    @Override public I2cAddr getI2cAddress()
        {
        return this.deviceClient.getI2cAddress();
        }

    //----------------------------------------------------------------------------------------------
    // Deprecated
    //----------------------------------------------------------------------------------------------

    /**
     * @deprecated This method has no utility for this gyro.
     */
    @Deprecated
    @Override public double getRotationFraction()
        {
        notSupported();
        return 0;
        }

    /**
     * @deprecated This provides little useful utility beyond {@link #isCalibrating()}
     */
    @Deprecated
    public enum MeasurementMode
        {
        GYRO_CALIBRATION_PENDING,
        GYRO_CALIBRATING,
        GYRO_NORMAL,
        }

    /**
     * This provides little useful utility beyond {@link #isCalibrating()}
     */
    @Deprecated
    public MeasurementMode getMeasurementMode()
        {
        return this.isCalibrating()
                ? MeasurementMode.GYRO_CALIBRATING
                : MeasurementMode.GYRO_NORMAL;
        }

    protected void notSupported()
        {
        throw new UnsupportedOperationException("This method is not supported for " + getDeviceName());
        }
    }
