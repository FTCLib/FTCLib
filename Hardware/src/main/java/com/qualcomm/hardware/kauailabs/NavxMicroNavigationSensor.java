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
SERVICES), LOSS OF USE, DATA, OR PROFITS), OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

// Portions of this implementation are licensed as follows:

/* ============================================
 NavX-MXP source code is placed under the MIT license
 Copyright (c) 2015 Kauai Labs

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 ===============================================
 */
package com.qualcomm.hardware.kauailabs;

import androidx.annotation.NonNull;

import com.qualcomm.robotcore.hardware.Gyroscope;
import com.qualcomm.robotcore.hardware.I2cAddr;
import com.qualcomm.robotcore.hardware.I2cAddrConfig;
import com.qualcomm.robotcore.hardware.I2cDeviceSynch;
import com.qualcomm.robotcore.hardware.I2cDeviceSynchDeviceWithParameters;
import com.qualcomm.robotcore.hardware.IntegratingGyroscope;
import com.qualcomm.robotcore.hardware.TimestampedData;
import com.qualcomm.robotcore.hardware.configuration.annotations.DeviceProperties;
import com.qualcomm.robotcore.hardware.configuration.annotations.I2cDeviceType;
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
import java.util.Set;

/**
 * {@link NavxMicroNavigationSensor} provides support for the Kauai Labs navX-Micro Robotics
 * Navigation Sensor. This sensor contains an Invensense MPU-9250 integrated circuit.
 *
 * @see <a href="http://pdocs.kauailabs.com/navx-micro/">navX-Micro</a>
 * @see <a href="https://www.invensense.com/products/motion-tracking/9-axis/mpu-9250/">Invensense MPU-9250</a>
 */
@I2cDeviceType
@DeviceProperties(name = "@string/navx_micro_name", description = "@string/navx_micro_description", xmlTag = "KauaiLabsNavxMicro", builtIn = true)
public class NavxMicroNavigationSensor extends I2cDeviceSynchDeviceWithParameters<I2cDeviceSynch, NavxMicroNavigationSensor.Parameters>
        implements Gyroscope, IntegratingGyroscope, I2cAddrConfig
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public final int NAVX_WRITE_COMMAND_BIT = 0x80;

    // the register set on this device is large enough that we need two read widows to cover common reads
    protected static final I2cDeviceSynch.ReadMode readMode = I2cDeviceSynch.ReadMode.REPEAT;
    protected static final I2cDeviceSynch.ReadWindow lowerWindow = newWindow(Register.SENSOR_STATUS_L, Register.LINEAR_ACC_Z_H);
    protected static final I2cDeviceSynch.ReadWindow upperWindow = newWindow(Register.GYRO_X_L, Register.MAG_Z_H);

    protected static I2cDeviceSynch.ReadWindow newWindow(Register regFirst, Register regMax)
        {
        return new I2cDeviceSynch.ReadWindow(regFirst.bVal, regMax.bVal-regFirst.bVal, readMode);
        }

    protected float gyroScaleFactor;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public NavxMicroNavigationSensor(I2cDeviceSynch deviceClient)
        {
        super(deviceClient, true, new Parameters());

        setReadWindow();
        this.deviceClient.setI2cAddress(ADDRESS_I2C_DEFAULT);
        this.registerArmingStateCallback(true);
        this.deviceClient.engage();
        }

    protected void setReadWindow()
        {
        this.deviceClient.setReadWindow(lowerWindow);
        }

    protected boolean internalInitialize(@NonNull Parameters parameters)
        {
        // Remember the parameters for later
        this.parameters = parameters.clone();

        // Utilize the requested parameters
        write8(Register.UPDATE_RATE_HZ, (byte)parameters.updateRate);

        // Reset defaults for user
        write8(Register.INTEGRATION_CTL, IntegrationControl.RESET_ALL.bVal);

        // Dig out state from the sensor that we'll need for computation
        float gyroFullScaleRangeDegreesPerSecond = readShort(Register.GYRO_FSR_DPS_L);
        this.gyroScaleFactor = gyroFullScaleRangeDegreesPerSecond / ((float)Short.MAX_VALUE+1);

        return true;
        }

    @Override
    public Manufacturer getManufacturer()
        {
        return Manufacturer.Other;
        }

    @Override public String getDeviceName()
        {
        return String.format("Kauai Labs navX-Micro Gyro %s", getFirmwareVersion());
        }

    public RobotUsbDevice.FirmwareVersion getFirmwareVersion()
        {
        return new RobotUsbDevice.FirmwareVersion(read8(Register.FW_VER_MAJOR), read8(Register.FW_VER_MINOR));
        }

    //----------------------------------------------------------------------------------------------
    // Utility
    //----------------------------------------------------------------------------------------------

    protected void ensureReadWindow(I2cDeviceSynch.ReadWindow needed)
    // We optimize small windows into larger ones if we can
        {
        I2cDeviceSynch.ReadWindow windowToSet = lowerWindow.containsWithSameMode(needed)
            ? lowerWindow
            : upperWindow.containsWithSameMode(needed)
                ? upperWindow
                : needed;           // just use what's needed if it's not within our two main windows
        this.deviceClient.ensureReadWindow(needed, windowToSet);
        }

    public synchronized TimestampedData readTimeStamped(Register reg, int creg)
        {
        I2cDeviceSynch.ReadWindow window = new I2cDeviceSynch.ReadWindow(reg.bVal, creg, readMode);
        ensureReadWindow(window);
        return this.deviceClient.readTimeStamped(reg.bVal, creg);
        }

    public byte read8(Register reg)
        {
        return readTimeStamped(reg, 1).data[0];
        }
    public short readShort(Register reg)
        {
        return TypeConversion.byteArrayToShort(readTimeStamped(reg, 2).data, ByteOrder.LITTLE_ENDIAN);
        }
    public float readSignedHundredthsFloat(Register reg)
        {
        return shortToSignedHundredths(readShort(reg));
        }
    protected float shortToSignedHundredths(short value)
        {
        /* -327.68 to +327.67 */
        return value * 0.01f;
        }

    public void write8(Register reg, byte value)
        {
        this.deviceClient.write8(NAVX_WRITE_COMMAND_BIT | reg.bVal, value);
        }
    public void writeShort(Register reg, short value)
        {
        this.deviceClient.write(NAVX_WRITE_COMMAND_BIT | reg.bVal, TypeConversion.shortToByteArray(value, ByteOrder.LITTLE_ENDIAN));
        }

    //----------------------------------------------------------------------------------------------
    // Device-specific functionality
    //----------------------------------------------------------------------------------------------

    /**
     * Returns true if the sensor is currently performing automatic gyro/accelerometer calibration.
     * Automatic calibration occurs when the sensor is initially powered on, during which time the
     * sensor should be held still, with the Z-axis pointing up (perpendicular to the earth).
     *
     * <p>NOTE: During this automatic calibration, the angular orientation data may not be accurate.</p>
     *
     * @return whether the sensor is currently performing automatic calibration
     */
    public boolean isCalibrating()
        {
        byte calibrationStatus = read8(Register.SENSOR_STATUS_H);
        boolean complete = (calibrationStatus & CalibrationStatus.IMU_CAL_MASK.bVal) == CalibrationStatus.IMU_CAL_COMPLETE.bVal;
        return !complete;
        }

    //----------------------------------------------------------------------------------------------
    // Gyroscope & IntegratingGyroscope interface
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
        result.add(Axis.X);
        result.add(Axis.Y);
        result.add(Axis.Z);
        return result;
        }

    @Override public Orientation getAngularOrientation(AxesReference reference, AxesOrder order, org.firstinspires.ftc.robotcore.external.navigation.AngleUnit angleUnit)
        {
        TimestampedData data = this.deviceClient.readTimeStamped(Register.YAW_L.bVal, 3 * 2/*sizeof short*/);
        //
        float zDegrees = -shortToSignedHundredths(TypeConversion.byteArrayToShort(data.data, 0, ByteOrder.LITTLE_ENDIAN));
        float yDegrees =  shortToSignedHundredths(TypeConversion.byteArrayToShort(data.data, 2, ByteOrder.LITTLE_ENDIAN));
        float xDegrees =  shortToSignedHundredths(TypeConversion.byteArrayToShort(data.data, 4, ByteOrder.LITTLE_ENDIAN));
        //
        return new Orientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES, zDegrees, yDegrees, xDegrees, data.nanoTime)
                .toAxesReference(reference)
                .toAxesOrder(order)
                .toAngleUnit(angleUnit);
        }

    @Override public AngularVelocity getAngularVelocity(AngleUnit unit)
        {
        TimestampedData data = this.deviceClient.readTimeStamped(Register.GYRO_X_L.bVal, 3 * 2/*sizeof short*/);

        float xDegPerSec = TypeConversion.byteArrayToShort(data.data, 0, ByteOrder.LITTLE_ENDIAN) * gyroScaleFactor;
        float yDegPerSec = TypeConversion.byteArrayToShort(data.data, 2, ByteOrder.LITTLE_ENDIAN) * gyroScaleFactor;
        float zDegPerSec = TypeConversion.byteArrayToShort(data.data, 4, ByteOrder.LITTLE_ENDIAN) * gyroScaleFactor;

        return new AngularVelocity(AngleUnit.DEGREES, xDegPerSec, yDegPerSec, zDegPerSec, data.nanoTime).toAngleUnit(unit);
        }

    //----------------------------------------------------------------------------------------------
    // I2cAddrConfig interface
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
    // Constants
    //----------------------------------------------------------------------------------------------

    public final static I2cAddr ADDRESS_I2C_DEFAULT = I2cAddr.create7bit(0x32);

    public static class Parameters implements Cloneable
        {
        /** The desired update rate for the sensor, in Hz */
        public int updateRate = 50;

        /**
         * Returns the update rate actually used with these parameters. The only update rates that
         * can actually be realized are those evenly divisible by the internal sample clock,
         * which is 200Hz. Thus, the actual rate may be higher than the requested rate.
         * For example, a request of 58Hz will result in 200 / (200 / 58) = 200/3 == 66Hz.
         * @return the actual update rate used, in Hz
         */
        public int realizedUpdateRate()
            {
            final int hzInternalSampleClock = 200;
            return hzInternalSampleClock / (hzInternalSampleClock / updateRate);
            }

        public Parameters clone()
            {
            try {
                return (Parameters)super.clone();
                }
            catch (CloneNotSupportedException e)
                {
                throw new RuntimeException("internal error: Parameters can't be cloned");
                }
            }
        }

    /** @see <a href="http://pdocs.kauailabs.com/navx-micro/advanced/register-protocol/">navX registers</a> */
    public enum Register
        {
        FIRST(0x00),

        /**********************************************/
        /* Device Identification Registers            */
        /**********************************************/

        WHOAMI(0x00), /* IMU_MODEL_XXX */
        HW_REV(0x01),
        FW_VER_MAJOR(0x02),
        FW_VER_MINOR(0x03),

        /**********************************************/
        /* Status and Control Registers               */
        /**********************************************/

        /* Read-write */
        UPDATE_RATE_HZ  (0x04), /* Range:  4 - 50 [unsigned byte] */
        /* Read-only */
        /* Accelerometer Full-Scale Range: in units of G [unsigned byte] */
        ACCEL_FSR_G     (0x05),
        /* Gyro Full-Scale Range (Degrees/Sec):  Range:  250, 500, 1000 or 2000 [unsigned short] */
        GYRO_FSR_DPS_L  (0x06), /* Lower 8-bits of Gyro Full-Scale Range */
        GYRO_FSR_DPS_H  (0x07), /* Upper 8-bits of Gyro Full-Scale Range */
        OP_STATUS       (0x08), /* NAVX_OP_STATUS_XXX */
        CAL_STATUS      (0x09), /* NAVX_CAL_STATUS_XXX */
        SELFTEST_STATUS (0x0A), /* NAVX_SELFTEST_STATUS_XXX */
        CAPABILITY_FLAGS_L(0x0B),
        CAPABILITY_FLAGS_H(0x0C),

        /**********************************************/
        /* Processed Data Registers                   */
        /**********************************************/

        SENSOR_STATUS_L	(0x10), /* NAVX_SENSOR_STATUS_XXX */
        SENSOR_STATUS_H	(0x11), // upper 8 bits of sensor status shadows calibration status
        /* Timestamp:  [unsigned long] */
        TIMESTAMP_L_L   (0x12),
        TIMESTAMP_L_H   (0x13),
        TIMESTAMP_H_L	(0x14),
        TIMESTAMP_H_H	(0x15),


        /* Yaw, Pitch, Roll:  Range: -180.00 to 180.00 [signed hundredths] */
        /* Compass Heading:   Range: 0.00 to 360.00 [unsigned hundredths] */
        /* Altitude in Meters:  In units of meters [16:16] */
        YAW_L 			(0x16), /* Lower 8 bits of Yaw     */
        YAW_H 			(0x17), /* Upper 8 bits of Yaw     */
        ROLL_L 			(0x18), /* Lower 8 bits of Roll    */
        ROLL_H 			(0x19), /* Upper 8 bits of Roll    */
        PITCH_L 		(0x1A), /* Lower 8 bits of Pitch   */
        PITCH_H 		(0x1B), /* Upper 8 bits of Pitch   */
        HEADING_L 		(0x1C), /* Lower 8 bits of Heading */
        HEADING_H 		(0x1D), /* Upper 8 bits of Heading */
        FUSED_HEADING_L	(0x1E), /* Upper 8 bits of Fused Heading */
        FUSED_HEADING_H	(0x1F), /* Upper 8 bits of Fused Heading */
        ALTITUDE_I_L	(0x20),
        ALTITUDE_I_H	(0x21),
        ALTITUDE_D_L	(0x22),
        ALTITUDE_D_H	(0x23),

        /* World-frame Linear Acceleration: In units of +/- G * 1000 [signed thousandths] */
        LINEAR_ACC_X_L	(0x24), /* Lower 8 bits of Linear Acceleration X */
        LINEAR_ACC_X_H	(0x25), /* Upper 8 bits of Linear Acceleration X */
        LINEAR_ACC_Y_L	(0x26), /* Lower 8 bits of Linear Acceleration Y */
        LINEAR_ACC_Y_H	(0x27), /* Upper 8 bits of Linear Acceleration Y */
        LINEAR_ACC_Z_L	(0x28), /* Lower 8 bits of Linear Acceleration Z */
        LINEAR_ACC_Z_H	(0x29), /* Upper 8 bits of Linear Acceleration Z */

        /* Quaternion:  Range -1 to 1 [signed short ratio] */
        QUAT_W_L 		(0x2A), /* Lower 8 bits of Quaternion W */
        QUAT_W_H 		(0x2B), /* Upper 8 bits of Quaternion W */
        QUAT_X_L 		(0x2C), /* Lower 8 bits of Quaternion X */
        QUAT_X_H 		(0x2D), /* Upper 8 bits of Quaternion X */
        QUAT_Y_L 		(0x2E), /* Lower 8 bits of Quaternion Y */
        QUAT_Y_H 		(0x2F), /* Upper 8 bits of Quaternion Y */
        QUAT_Z_L 		(0x30), /* Lower 8 bits of Quaternion Z */
        QUAT_Z_H 		(0x31), /* Upper 8 bits of Quaternion Z */

        /**********************************************/
        /* Raw Data Registers                         */
        /**********************************************/

        /* Sensor Die Temperature:  Range +/- 150, In units of Centigrade * 100 [signed hundredths float */
        MPU_TEMP_C_L	(0x32), /* Lower 8 bits of Temperature */
        MPU_TEMP_C_H	(0x33), /* Upper 8 bits of Temperature */

        /* Raw, Calibrated Angular Rotation, in device units.  Value in DPS(units / GYRO_FSR_DPS [signed short] */
        GYRO_X_L        (0x34),
        GYRO_X_H		(0x35),
        GYRO_Y_L		(0x36),
        GYRO_Y_H		(0x37),
        GYRO_Z_L		(0x38),
        GYRO_Z_H		(0x39),

        /* Raw, Calibrated, Acceleration Data, in device units.  Value in G(units / ACCEL_FSR_G [signed short] */
        ACC_X_L			(0x3A),
        ACC_X_H			(0x3B),
        ACC_Y_L			(0x3C),
        ACC_Y_H			(0x3D),
        ACC_Z_L			(0x3E),
        ACC_Z_H			(0x3F),

        /* Raw, Calibrated, Un-tilt corrected Magnetometer Data, in device units.  1 unit(0.15 uTesla [signed short] */
        MAG_X_L			(0x40),
        MAG_X_H			(0x41),
        MAG_Y_L			(0x42),
        MAG_Y_H			(0x43),
        MAG_Z_L			(0x44),
        MAG_Z_H			(0x45),

        /* Calibrated Pressure in millibars Valid Range:  10.00 Max:  1200.00 [16:16 float]  */
        PRESSURE_IL     (0x46),
        PRESSURE_IH     (0x47),
        PRESSURE_DL     (0x48),
        PRESSURE_DH		(0x49),

        /* Pressure Sensor Die Temperature:  Range +/- 150.00C [signed hundredths] */
        PRESSURE_TEMP_L	(0x4A),
        PRESSURE_TEMP_H	(0x4B),

        /**********************************************/
        /* Calibration Registers                      */
        /**********************************************/

        /* Yaw Offset: Range -180.00 to 180.00 [signed hundredths] */

        YAW_OFFSET_L		(0x4C), /* Lower 8 bits of Yaw Offset */
        YAW_OFFSET_H		(0x4D), /* Upper 8 bits of Yaw Offset */

        /* Quaternion Offset:  Range: -1 to 1 [signed short ratio]  */

        QUAT_OFFSET_W_L 	(0x4E), /* Lower 8 bits of Quaternion W */
        QUAT_OFFSET_W_H 	(0x4F), /* Upper 8 bits of Quaternion W */
        QUAT_OFFSET_X_L 	(0x50), /* Lower 8 bits of Quaternion X */
        QUAT_OFFSET_X_H 	(0x51), /* Upper 8 bits of Quaternion X */
        QUAT_OFFSET_Y_L 	(0x52), /* Lower 8 bits of Quaternion Y */
        QUAT_OFFSET_Y_H 	(0x53), /* Upper 8 bits of Quaternion Y */
        QUAT_OFFSET_Z_L 	(0x54), /* Lower 8 bits of Quaternion Z */
        QUAT_OFFSET_Z_H 	(0x55), /* Upper 8 bits of Quaternion Z */

        /**********************************************/
        /* Integrated Data Registers                  */
        /**********************************************/

        /* Integration Control (Write-Only)           */
        INTEGRATION_CTL	(0x56),
        PAD_UNUSED(0x57),

        /* Velocity:  Range -32768.9999 - 32767.9999 in units of Meters/Sec      */
        VEL_X_I_L(0x58),
        VEL_X_I_H(0x59),
        VEL_X_D_L(0x5A),
        VEL_X_D_H(0x5B),
        VEL_Y_I_L(0x5C),
        VEL_Y_I_H(0x5D),
        VEL_Y_D_L(0x5E),
        VEL_Y_D_H(0x5F),
        VEL_Z_I_L(0x60),
        VEL_Z_I_H(0x61),
        VEL_Z_D_L(0x62),
        VEL_Z_D_H(0x63),

        /* Displacement:  Range -32768.9999 - 32767.9999 in units of Meters      */
        DISP_X_I_L(0x64),
        DISP_X_I_H(0x65),
        DISP_X_D_L(0x66),
        DISP_X_D_H(0x67),
        DISP_Y_I_L(0x68),
        DISP_Y_I_H(0x69),
        DISP_Y_D_L(0x6A),
        DISP_Y_D_H(0x6B),
        DISP_Z_I_L(0x6C),
        DISP_Z_I_H(0x6D),
        DISP_Z_D_L(0x6E),
        DISP_Z_D_H(0x6F),

        LAST(DISP_Z_D_H.bVal),

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

    public enum OpStatus
        {
            INITIALIZING(0x00),
            SELFTEST_IN_PROGRESS(0x01),
            ERROR(0x02),
            IMU_AUTOCAL_IN_PROGRESS(0x03),
            NORMAL(0x04);

        public byte bVal;
        OpStatus(int value) { this.bVal = (byte) value; }
        }

    public enum SensorStatus
        {
            MOVING(0x01),
            YAW_STABLE(0x02),
            MAG_DISTURBANCE(0x04),
            ALTITUDE_VALID(0x08),
            SEALEVEL_PRESS_SET(0x10),
            FUSED_HEADING_VALID(0x20);

        public byte bVal;
        SensorStatus(int value) { this.bVal = (byte) value; }
        }

    public enum CalibrationStatus
        {
            IMU_CAL_INPROGRESS(0x00),
            IMU_CAL_ACCUMULATE(0x01),
            IMU_CAL_COMPLETE(0x02),
            IMU_CAL_MASK(0x03),
            MAG_CAL_COMPLETE(0x04),
            BARO_CAL_COMPLETE(0x08);

        public byte bVal;
        CalibrationStatus(int value) { this.bVal = (byte) value; }
        }

    public enum SelfTestStatus
        {
            COMPLETE(0x80),
            RESULT_GYRO_PASSED(0x01),
            RESULT_ACCEL_PASSED(0x02),
            RESULT_MAG_PASSED(0x04),
            RESULT_BARO_PASSED(0x08);

        public byte bVal;
        SelfTestStatus(int value) { this.bVal = (byte) value; }
        }

    public enum IntegrationControl
        {
            RESET_VEL_X(0x01),
            RESET_VEL_Y(0x02),
            RESET_VEL_Z(0x04),
            RESET_DISP_X(0x08),
            RESET_DISP_Y(0x10),
            RESET_DISP_Z(0x20),
            RESET_YAW(0x80),
            RESET_ALL(
                    RESET_VEL_X.bVal | RESET_VEL_Y.bVal | RESET_VEL_Z.bVal |
                    RESET_DISP_X.bVal | RESET_DISP_Y.bVal | RESET_DISP_Z.bVal |
                    RESET_YAW.bVal);

        public byte bitor(IntegrationControl integrationControl)
            {
            return (byte)(this.bVal | integrationControl.bVal);
            }
        public byte bitor(byte bVal)
            {
            return (byte)(this.bVal | bVal);
            }

        public byte bVal;
        IntegrationControl(int value) { this.bVal = (byte) value; }
        }

    }
