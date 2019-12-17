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
package com.qualcomm.hardware.bosch;

import android.support.annotation.NonNull;
import android.util.Log;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerNotifier;
import com.qualcomm.robotcore.hardware.Gyroscope;
import com.qualcomm.robotcore.hardware.I2cAddr;
import com.qualcomm.robotcore.hardware.I2cAddrConfig;
import com.qualcomm.robotcore.hardware.I2cController;
import com.qualcomm.robotcore.hardware.I2cDeviceSynch;
import com.qualcomm.robotcore.hardware.I2cDeviceSynchDeviceWithParameters;
import com.qualcomm.robotcore.hardware.I2cWaitControl;
import com.qualcomm.robotcore.hardware.IntegratingGyroscope;
import com.qualcomm.robotcore.hardware.TimestampedData;
import com.qualcomm.robotcore.hardware.TimestampedI2cData;
import com.qualcomm.robotcore.hardware.configuration.typecontainers.UserConfigurationType;
import com.qualcomm.robotcore.hardware.configuration.annotations.I2cDeviceType;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.ReadWriteFile;
import com.qualcomm.robotcore.util.ThreadPool;
import com.qualcomm.robotcore.util.TypeConversion;

import org.firstinspires.ftc.robotcore.external.Func;
import org.firstinspires.ftc.robotcore.external.navigation.Acceleration;
import org.firstinspires.ftc.robotcore.external.navigation.AngularVelocity;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.Axis;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.MagneticFlux;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;
import org.firstinspires.ftc.robotcore.external.navigation.Position;
import org.firstinspires.ftc.robotcore.external.navigation.Quaternion;
import org.firstinspires.ftc.robotcore.external.navigation.Temperature;
import org.firstinspires.ftc.robotcore.external.navigation.Velocity;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * {@link BNO055IMUImpl} provides support for communicating with a BNO055 inertial motion
 * unit. Sensors using this integrated circuit are available from several manufacturers.
 */
public abstract class BNO055IMUImpl extends I2cDeviceSynchDeviceWithParameters<I2cDeviceSynch, BNO055IMU.Parameters>
        implements BNO055IMU, Gyroscope, IntegratingGyroscope, I2cAddrConfig, OpModeManagerNotifier.Notifications
    {
    //------------------------------------------------------------------------------------------
    // State
    //------------------------------------------------------------------------------------------

    protected SensorMode             currentMode;

    protected final Object           dataLock = new Object();
    protected AccelerationIntegrator accelerationAlgorithm;

    protected final Object           startStopLock = new Object();
    protected ExecutorService        accelerationMananger;
    protected float                  delayScale             = 1;
    protected static final int       msAwaitChipId          = 2000;
    protected static final int       msAwaitSelfTest        = 2000;
    // The msAwaitSelfTest value is lore. We choose here to use the same value for awaiting chip id,
    // on the (not completely unreasonable) theory that similar things are happening in the chip in both
    // cases. A survey of other libraries is as follows:
    //  1000ms:     https://github.com/OpenROV/openrov-software-arduino/blob/master/OpenROV/BNO055.cpp
    //              https://github.com/alexstyl/Adafruit-BNO055-SparkCore-port/blob/master/Adafruit_BNO055.cpp

    // We always read as much as we can when we have nothing else to do
    protected static final I2cDeviceSynch.ReadMode readMode = I2cDeviceSynch.ReadMode.REPEAT;

    /**
     * One of two primary register windows we use for reading from the BNO055.
     * 
     * Given the maximum allowable size of a register window, the set of registers on 
     * a BNO055 can be usefully divided into two windows, which we here call lowerWindow
     * and upperWindow. 
     * 
     * When we find the need to change register windows depending on what data is being requested
     * from the sensor, we try to use these two windows so as to reduce the number of register
     * window switching that might be required as other data is read in the future.
     */
    protected static final I2cDeviceSynch.ReadWindow lowerWindow = newWindow(Register.CHIP_ID, Register.EUL_H_LSB);
    /**
     * A second of two primary register windows we use for reading from the BNO055.
     * We'd like to include the temperature register, too, but that would make a 27-byte window, and
     * those don't (currently) work in the CDIM.
     *
     * @see #lowerWindow
     */
    protected static final I2cDeviceSynch.ReadWindow upperWindow = newWindow(Register.EUL_H_LSB, Register.TEMP);
    
    protected static I2cDeviceSynch.ReadWindow newWindow(Register regFirst, Register regMax)
        {
        return new I2cDeviceSynch.ReadWindow(regFirst.bVal, regMax.bVal-regFirst.bVal, readMode);
        }

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    /**
     * This constructor is used by {@link UserConfigurationType#createInstance(I2cController, int)}
     * @see UserConfigurationType#createInstance(I2cController, int)
     * @see I2cDeviceType
     */
    public BNO055IMUImpl(I2cDeviceSynch deviceClient)
        {
        super(deviceClient, true, disabledParameters());

        this.deviceClient.setReadWindow(lowerWindow);
        this.deviceClient.engage();

        this.currentMode           = null;
        this.accelerationAlgorithm = new NaiveAccelerationIntegrator();
        this.accelerationMananger  = null;

        this.registerArmingStateCallback(false);
        }

    // A concrete instance to use that allows us to avoid ever having NULL parameters
    protected static Parameters disabledParameters()
        {
        Parameters result = new Parameters();
        result.mode = SensorMode.DISABLED;
        return result;
        }

    //------------------------------------------------------------------------------------------
    // Notifications
    // This particular sensor wants to take action not only when the a user opmode is started (for
    // which it gets a resetDeviceConfigurationForOpMode() as all HardwareDevices do) but also when
    // the opmode ends. To that end, it implements OpModeManagerNotifier.Notifications.
    //------------------------------------------------------------------------------------------

    @Override public void resetDeviceConfigurationForOpMode()
        {
        stopAccelerationIntegration();
        this.parameters = disabledParameters();
        super.resetDeviceConfigurationForOpMode();
        }

    @Override public void onOpModePreInit(OpMode opMode)
        {
        }

    @Override public void onOpModePreStart(OpMode opMode)
        {
        }

    @Override public void onOpModePostStop(OpMode opMode)
        {
        stopAccelerationIntegration();
        }

    //------------------------------------------------------------------------------------------
    // Initialization
    //------------------------------------------------------------------------------------------

    @Override public I2cAddr getI2cAddress()
        {
        return this.parameters.i2cAddr;
        }

    @Override public void setI2cAddress(I2cAddr newAddress)
        {
        this.parameters.i2cAddr = newAddress;
        this.deviceClient.setI2cAddress(newAddress);
        }

    /**
     * Initialize the device to be running in the indicated operation mode
     */
    @Override public boolean internalInitialize(@NonNull Parameters parameters)
        {
        if (parameters.mode==SensorMode.DISABLED)
            return false;

        // Remember parameters so they're accessible starting during initialization.
        // Disconnect from user parameters so he won't interfere with us later.
        Parameters prevParameters = this.parameters;
        this.parameters = parameters.clone();

        // Configure logging as desired (we no longer log at the I2C level)
        // this.deviceClient.setLogging(this.parameters.loggingEnabled);
        // this.deviceClient.setLoggingTag(this.parameters.loggingTag);

        // Make sure we're talking to the correct I2c address
        this.deviceClient.setI2cAddress(parameters.i2cAddr);

        // We retry the initialization a few times: it's been reported to fail, intermittently,
        // but, so far as we can tell, entirely non-deterministically. Ideally, we'd like that to
        // never happen, but in light of our (current) inability to figure out how to prevent that,
        // we simply retry the initialization if it seems to fail.

        SystemStatus expectedStatus = parameters.mode.isFusionMode() ? SystemStatus.RUNNING_FUSION : SystemStatus.RUNNING_NO_FUSION;

        for (int attempt=0; !Thread.currentThread().isInterrupted() && attempt < 5; attempt++)
            {
            if (internalInitializeOnce(expectedStatus))
                {
                this.isInitialized = true;
                return true;
                }

            // Hack: try again with more delay next time
            delayScale = Math.min(3, delayScale * 1.2f);
            log_w("retrying IMU initialization");
            }

        log_e("IMU initialization failed");
        this.parameters = prevParameters;
        return false;
        }

    /**
     * Do one attempt at initializing the device to be running in the indicated operation mode
     */
    protected boolean internalInitializeOnce(SystemStatus expectedStatus)
        {
        // Validate parameters
        if (SensorMode.CONFIG == parameters.mode)
            throw new IllegalArgumentException("SensorMode.CONFIG illegal for use as initialization mode");

        ElapsedTime elapsed = new ElapsedTime();
        if (parameters.accelerationIntegrationAlgorithm != null)
            {
            this.accelerationAlgorithm = parameters.accelerationIntegrationAlgorithm;
            }

        // Lore: "send a throw-away command [...] just to make sure the BNO is in a good state
        // and ready to accept commands (this seems to be necessary after a hard power down)."
        write8(Register.PAGE_ID, 0);

        // Make sure we have the right device
        byte chipId = read8(Register.CHIP_ID);
        if (chipId != bCHIP_ID_VALUE)
            {
            delayExtra(650);     // delay value is from from Table 0-2 in the BNO055 specification
            chipId = read8(Register.CHIP_ID);
            if (chipId != bCHIP_ID_VALUE)
                {
                log_e("unexpected chip: expected=%d found=%d", bCHIP_ID_VALUE, chipId);
                return false;
                }
            }
        
        // Get us into config mode, for sure
        setSensorMode(SensorMode.CONFIG);
        
        // Reset the system, and wait for the chip id register to switch back from its reset state 
        // to the it's chip id state. This can take a very long time, some 650ms (Table 0-2, p13) 
        // perhaps. While in the reset state the chip id (and other registers) reads as 0xFF.
        TimestampedI2cData.suppressNewHealthWarnings(true);
        try {
            elapsed.reset();
            write8(Register.SYS_TRIGGER, 0x20);
            for (;;)
                {
                chipId = read8(Register.CHIP_ID);
                if (chipId == bCHIP_ID_VALUE)
                    break;
                delayExtra(10);
                if (elapsed.milliseconds() > msAwaitChipId)
                    {
                    log_e("failed to retrieve chip id");
                    return false;
                    }
                }
            delayLoreExtra(50);
            }
        finally
            {
            TimestampedI2cData.suppressNewHealthWarnings(false);
            }

        // Set to normal power mode
        write8(Register.PWR_MODE, POWER_MODE.NORMAL.getValue());
        delayLoreExtra(10);

        // Make sure we're looking at register page zero, as the other registers
        // we need to set here are on that page.
        write8(Register.PAGE_ID, 0);

        // Set the output units. Section 3.6, p31
        int unitsel = (parameters.pitchMode.bVal << 7) |       // pitch angle convention
                      (parameters.temperatureUnit.bVal << 4) | // temperature
                      (parameters.angleUnit.bVal << 2) |       // euler angle units
                      (parameters.angleUnit.bVal << 1) |       // gyro units, per second
                      (parameters.accelUnit.bVal /*<< 0*/);    // accelerometer units
        write8(Register.UNIT_SEL, unitsel);

        // Use or don't use the external crystal
        // See Section 5.5 (p100) of the BNO055 specification.
        write8(Register.SYS_TRIGGER, parameters.useExternalCrystal ? 0x80 : 0x00);
        delayLoreExtra(50);

        // Switch to page 1 so we can write some more registers
        write8(Register.PAGE_ID, 1);

        // Configure selected page 1 registers
        write8(Register.ACC_CONFIG, parameters.accelPowerMode.bVal | parameters.accelBandwidth.bVal | parameters.accelRange.bVal);
        write8(Register.MAG_CONFIG, parameters.magPowerMode.bVal | parameters.magOpMode.bVal | parameters.magRate.bVal);
        write8(Register.GYR_CONFIG_0, parameters.gyroBandwidth.bVal | parameters.gyroRange.bVal);
        write8(Register.GYR_CONFIG_1, parameters.gyroPowerMode.bVal);

        // Switch back
        write8(Register.PAGE_ID, 0);

        // Run a self test. This appears to be a necessary step in order for the
        // sensor to be able to actually be used. That is, we've observed that absent this,
        // the sensors do not return correct data. We wish that were documented somewhere.
        write8(Register.SYS_TRIGGER, read8(Register.SYS_TRIGGER) | 0x01);           // SYS_TRIGGER=0x3F

        // Start a timer: we only give the self-test a certain length of time to run
        elapsed.reset();

        // It's a little unclear how to conclude when the self test is complete. getSystemStatus()
        // can report SystemStatus.SELF_TEST, and one might be lead to think that that will remain
        // true while the self test is running, but that appears not actually to be the case, as
        // sometimes we see SystemStatus.SELF_TEST being reported even after two full seconds. So,
        // we fall back on to what we've always done, and just check the results of the tested
        // sensors we actually care about.

        // Per Section 3.9.2 Built In Self Test, when we manually kick off a self test,
        // the accelerometer, gyro, and magnetometer are tested, but the microcontroller is not.
        // So: we only look for successful results from those three.
        final int successfulResult = 0x07;
        final int successfulResultMask = 0x07;
        boolean selfTestSuccessful = false;
        while (!selfTestSuccessful && elapsed.milliseconds() < msAwaitSelfTest)
            {
            selfTestSuccessful = (read8(Register.SELFTEST_RESULT)&successfulResultMask) == successfulResult;    // SELFTEST_RESULT=0x36
            }
        if (!selfTestSuccessful)
            {
            int result = read8(Register.SELFTEST_RESULT);
            log_e("self test failed: 0x%02x", result);
            return false;
            }

        if (this.parameters.calibrationData != null)
            {
            writeCalibrationData(this.parameters.calibrationData);
            }
        else if (this.parameters.calibrationDataFile != null)
            {
            try {
                File file = AppUtil.getInstance().getSettingsFile(this.parameters.calibrationDataFile);
                String serialized = ReadWriteFile.readFileOrThrow(file);
                CalibrationData data = CalibrationData.deserialize(serialized);
                writeCalibrationData(data);
                }
            catch (IOException e)
                {
                // Ignore the absence of the indicated file, etc
                }
            }

        // Finally, enter the requested operating mode (see section 3.3).
        setSensorMode(parameters.mode);

        // At this point, the chip should in fact report correctly that it's in the mode requested.
        // See Section '4.3.58 SYS_STATUS' of the BNO055 specification. That said, we've seen issues
        // where the first mode request somehow doesn't take, so we re-issue. We don't understand the
        // circumstances that cause this condition (or we'd avoid them!).
        SystemStatus status = getSystemStatus();
        if (status != expectedStatus)
            {
            log_w("re-issuing IMU mode: system status=%s expected=%s", status, expectedStatus);
            delayLore(100);
            setSensorMode(parameters.mode);
            status = getSystemStatus();
            }

        if (status==expectedStatus)
            return true;
        else
            {
            log_w("IMU initialization failed: system status=%s expected=%s", status, expectedStatus);
            return false;
            }
        }
    
    protected void setSensorMode(SensorMode mode)
    /* The default operation mode after power-on is CONFIGMODE. When the user changes to another 
    operation mode, the sensors which are required in that particular sensor mode are powered, 
    while the sensors whose signals are not required are set to suspend mode. */
        {
        // Remember the mode, 'cause that's easy
        this.currentMode = mode;
        
        // Actually change the operation/sensor mode
        this.write8(Register.OPR_MODE, mode.bVal & 0x0F);                           // OPR_MODE=0x3D

        // Delay per Table 3-6 of BNO055 Data sheet (p21)
        if (mode == SensorMode.CONFIG)
            delayExtra(19);
        else
            delayExtra(7);
        }

    public synchronized SystemStatus getSystemStatus()
        {
        byte bVal = read8(Register.SYS_STAT);
        SystemStatus status = SystemStatus.from(bVal);
        if (status==SystemStatus.UNKNOWN)
            {
            log_w("unknown system status observed: 0x%08x", bVal);
            }
        return status;
        }

    public synchronized SystemError getSystemError()
        {
        byte bVal = read8(Register.SYS_ERR);
        SystemError error = SystemError.from(bVal);
        if (error==SystemError.UNKNOWN)
            {
            log_w("unknown system error observed: 0x%08x", bVal);
            }
        return error;
        }

    public synchronized CalibrationStatus getCalibrationStatus()
        {
        byte bVal = read8(Register.CALIB_STAT);
        return new CalibrationStatus(bVal);
        }

    //----------------------------------------------------------------------------------------------
    // HardwareDevice
    //----------------------------------------------------------------------------------------------

    @Override
    public void close()
        {
        stopAccelerationIntegration();
        super.close();
        }

    @Override public abstract String getDeviceName();

    @Override public abstract Manufacturer getManufacturer();

    //----------------------------------------------------------------------------------------------
    // Gyroscope
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

    @Override
    public synchronized AngularVelocity getAngularVelocity(org.firstinspires.ftc.robotcore.external.navigation.AngleUnit unit)
        {
        VectorData vector = getVector(VECTOR.GYROSCOPE, getAngularScale());
        float zRotationRate = -vector.next();
        float yRotationRate =  vector.next();
        float xRotationRate =  vector.next();
        return new AngularVelocity(parameters.angleUnit.toAngleUnit(),
                    xRotationRate, yRotationRate, zRotationRate,
                    vector.data.nanoTime)
                .toAngleUnit(unit);
        }

    @Override
    public Orientation getAngularOrientation(AxesReference reference, AxesOrder order, org.firstinspires.ftc.robotcore.external.navigation.AngleUnit angleUnit)
        {
        return getAngularOrientation().toAxesReference(reference).toAxesOrder(order).toAngleUnit(angleUnit);
        }

    //------------------------------------------------------------------------------------------
    // Calibration
    //------------------------------------------------------------------------------------------

    public synchronized boolean isSystemCalibrated()
        {
        byte b = this.read8(Register.CALIB_STAT);
        return ((b>>6) & 0x03) == 0x03;
        }

    public synchronized boolean isGyroCalibrated()
        {
        byte b = this.read8(Register.CALIB_STAT);
        return ((b>>4) & 0x03) == 0x03;
        }

    public synchronized boolean isAccelerometerCalibrated()
        {
        byte b = this.read8(Register.CALIB_STAT);
        return ((b>>2) & 0x03) == 0x03;
        }

    public synchronized boolean isMagnetometerCalibrated()
        {
        byte b = this.read8(Register.CALIB_STAT);
        return ((b/*>>0*/) & 0x03) == 0x03;
        }

    public CalibrationData readCalibrationData()
        {
        // From Section 3.11.4 of the datasheet:
        //
        // "The calibration profile includes sensor offsets and sensor radius. Host system can
        // read the offsets and radius only after a full calibration is achieved and the operation
        // mode is switched to CONFIG_MODE. Refer to sensor offsets and sensor radius registers."
        //
        // Other useful links:
        //      https://forums.adafruit.com/viewtopic.php?f=22&t=83965
        //      https://learn.adafruit.com/bno055-absolute-orientation-sensor-with-raspberry-pi-and-beaglebone-black/webgl-example#sensor-calibration
        //      http://iotdk.intel.com/docs/master/upm/classupm_1_1_b_n_o055.html

        SensorMode prevMode = this.currentMode;
        if (prevMode != SensorMode.CONFIG) setSensorMode(SensorMode.CONFIG);

        CalibrationData result = new CalibrationData();
        result.dxAccel = readShort(Register.ACC_OFFSET_X_LSB);
        result.dyAccel = readShort(Register.ACC_OFFSET_Y_LSB);
        result.dzAccel = readShort(Register.ACC_OFFSET_Z_LSB);
        result.dxMag   = readShort(Register.MAG_OFFSET_X_LSB);
        result.dyMag   = readShort(Register.MAG_OFFSET_Y_LSB);
        result.dzMag   = readShort(Register.MAG_OFFSET_Z_LSB);
        result.dxGyro  = readShort(Register.GYR_OFFSET_X_LSB);
        result.dyGyro  = readShort(Register.GYR_OFFSET_Y_LSB);
        result.dzGyro  = readShort(Register.GYR_OFFSET_Z_LSB);
        result.radiusAccel = readShort(Register.ACC_RADIUS_LSB);
        result.radiusMag   = readShort(Register.MAG_RADIUS_LSB);

        // Restore the previous mode and return
        if (prevMode != SensorMode.CONFIG) setSensorMode(prevMode);
        return result;
        }

    public void writeCalibrationData(CalibrationData data)
        {
        // Section 3.11.4:
        //
        // It is important that the correct offsets and corresponding sensor radius are used.
        // Incorrect offsets may result in unreliable orientation data even at calibration
        // accuracy level 3. To set the calibration profile the following steps need to be taken
        //
        //    1. Select the operation mode to CONFIG_MODE
        //    2. Write the corresponding sensor offsets and radius data
        //    3. Change operation mode to fusion mode

        SensorMode prevMode = this.currentMode;
        if (prevMode != SensorMode.CONFIG) setSensorMode(SensorMode.CONFIG);

        writeShort(Register.ACC_OFFSET_X_LSB, data.dxAccel);
        writeShort(Register.ACC_OFFSET_Y_LSB, data.dyAccel);
        writeShort(Register.ACC_OFFSET_Z_LSB, data.dzAccel);
        writeShort(Register.MAG_OFFSET_X_LSB, data.dxMag);
        writeShort(Register.MAG_OFFSET_Y_LSB, data.dyMag);
        writeShort(Register.MAG_OFFSET_Z_LSB, data.dzMag);
        writeShort(Register.GYR_OFFSET_X_LSB, data.dxGyro);
        writeShort(Register.GYR_OFFSET_Y_LSB, data.dyGyro);
        writeShort(Register.GYR_OFFSET_Z_LSB, data.dzGyro);
        writeShort(Register.ACC_RADIUS_LSB,   data.radiusAccel);
        writeShort(Register.MAG_RADIUS_LSB,   data.radiusMag);

        // Restore the previous mode and return
        if (prevMode != SensorMode.CONFIG) setSensorMode(prevMode);
        }

    //------------------------------------------------------------------------------------------
    // IBNO055IMU data retrieval
    //------------------------------------------------------------------------------------------

    public synchronized Temperature getTemperature()
        {
        byte b = this.read8(Register.TEMP);
        return new Temperature(this.parameters.temperatureUnit.toTempUnit(), (double)b, System.nanoTime());
        }

    public synchronized MagneticFlux getMagneticFieldStrength()
        {
        VectorData vector = getVector(VECTOR.MAGNETOMETER, getFluxScale());
        return new MagneticFlux(vector.next(), vector.next(), vector.next(), vector.data.nanoTime);
        }
    public synchronized Acceleration getOverallAcceleration()
        {
        VectorData vector = getVector(VECTOR.ACCELEROMETER, getMetersAccelerationScale());
        return new Acceleration(DistanceUnit.METER, vector.next(), vector.next(), vector.next(), vector.data.nanoTime);
        }
    public synchronized Acceleration getLinearAcceleration()
        {
        VectorData vector = getVector(VECTOR.LINEARACCEL, getMetersAccelerationScale());
        return new Acceleration(DistanceUnit.METER, vector.next(), vector.next(), vector.next(), vector.data.nanoTime);
        }
    public synchronized Acceleration getGravity()
        {
        VectorData vector = getVector(VECTOR.GRAVITY, getMetersAccelerationScale());
        return new Acceleration(DistanceUnit.METER, vector.next(), vector.next(), vector.next(), vector.data.nanoTime);
        }
    public synchronized AngularVelocity getAngularVelocity()
        {
        return getAngularVelocity(parameters.angleUnit.toAngleUnit());
        }
    public synchronized Orientation getAngularOrientation()
        {
        // Data returned from VECTOR.EULER is heading, roll, pitch, in that order.
        //
        // Note that the IMU returns heading in what one might call 'compass' direction, with values
        // increasing CW. We need a geometric direction, with values increasing CCW. So we simply negate.
        //
        // The data returned from the IMU is in the units that we initialized the IMU to return.
        // However, the IMU has a different sense of angle normalization than we do, so we explicitly
        // normalize such that users aren't surprised by (e.g.) Z angles which always appear as negative 
        // (in the range (-360, 0]).
        //
        VectorData vector = getVector(VECTOR.EULER, getAngularScale());
        org.firstinspires.ftc.robotcore.external.navigation.AngleUnit angleUnit = parameters.angleUnit.toAngleUnit();
        return new Orientation(AxesReference.INTRINSIC, AxesOrder.ZYX, angleUnit,
                angleUnit.normalize(-vector.next()),
                angleUnit.normalize(vector.next()),
                angleUnit.normalize(vector.next()),
                vector.data.nanoTime);
        }

    public synchronized Quaternion getQuaternionOrientation()
        {
        // Ensure we can see the registers we need
        deviceClient.ensureReadWindow(
                new I2cDeviceSynch.ReadWindow(Register.QUA_DATA_W_LSB.bVal, 8, readMode),
                upperWindow);

        // Section 3.6.5.5 of BNO055 specification
        TimestampedData ts = deviceClient.readTimeStamped(Register.QUA_DATA_W_LSB.bVal, 8);
        VectorData vector = new VectorData(ts, (1 << 14));
        return new Quaternion(vector.next(), vector.next(), vector.next(), vector.next(), vector.data.nanoTime);
        }

    /**
     * Return the number by which we need to divide a raw angle as read from the device in order
     * to convert it to our current angular units. See Table 3-22 of the BNO055 spec
     */
    protected float getAngularScale()
        {
        return this.parameters.angleUnit == AngleUnit.DEGREES ? 16.0f : 900.0f;
        }

    /**
     * Return the number by which we need to divide a raw acceleration as read from the device in order
     * to convert it to our current acceleration units. See Table 3-17 of the BNO055 spec.
     */
    protected float getAccelerationScale()
        {
        return this.parameters.accelUnit == AccelUnit.METERS_PERSEC_PERSEC ? 100.0f : 1.0f;
        }

    protected float getMetersAccelerationScale()
        {
        // Logically, the difference in scale between m/s^2 and mg should be 1000 / gravity
        // == 1000 / 9.80665 == 101.97162. And that's not 100. But if we actually use the
        // logically correct scale factor, the magnitude of the reported gravity vector doesn't
        // come out correct when running in MILLI_EARTH_GRAVITY mode, and, presumably, the other
        // accelerations are equally incorrect. A value of 100 seems to make that work, so we use it.
        // Which is a bit of a mystery, as it's almost like the MILLI_EARTH_GRAVITY and
        // METERS_PERSEC_PERSEC modes are actually one and the same. For now, we go with what
        // works.

        float scaleConversionFactor = 100;

        return this.parameters.accelUnit == AccelUnit.METERS_PERSEC_PERSEC
                ? getAccelerationScale()
                : getAccelerationScale() /*in mg*/ * scaleConversionFactor;
        }

    /**
     * Return the number by which we need to divide a raw acceleration as read from the device in order
     * to convert it to our current angular units. See Table 3-19 of the BNO055 spec. Note that the
     * BNO055 natively uses micro Teslas; we instead use Teslas.
     */
    protected float getFluxScale()
        {
        return 16.0f * 1000000.0f;
        }

    protected VectorData getVector(final VECTOR vector, float scale)
        {
        // Ensure that the 6 bytes for this vector are visible in the register window.
        ensureReadWindow(new I2cDeviceSynch.ReadWindow(vector.getValue(), 6, readMode));

        // Read the data
        return new VectorData(deviceClient.readTimeStamped(vector.getValue(), 6), scale);
        }

    protected static class VectorData
        {
        public    TimestampedData   data;
        public    float             scale;
        protected ByteBuffer        buffer;

        public VectorData(TimestampedData data, float scale)
            {
            this.data = data;
            this.scale = scale;
            buffer = ByteBuffer.wrap(data.data).order(ByteOrder.LITTLE_ENDIAN);
            }

        public float next()
            {
            return buffer.getShort() / scale;
            }
        }

    //------------------------------------------------------------------------------------------
    // Position and velocity management
    //------------------------------------------------------------------------------------------
    
    public Acceleration getAcceleration()
        {
        synchronized (dataLock)
            {
            Acceleration result = this.accelerationAlgorithm.getAcceleration();
            if (result == null) result = new Acceleration();
            return result;
            }
        }
    public Velocity getVelocity()
        {
        synchronized (dataLock)
            {
            Velocity result = this.accelerationAlgorithm.getVelocity();
            if (result == null) result = new Velocity();
            return result;
            }
        }
    public Position getPosition()
        {
        synchronized (dataLock)
            {
            Position result = this.accelerationAlgorithm.getPosition();
            if (result == null) result = new Position();
            return result;
            }
        }

    public void startAccelerationIntegration(Position initalPosition, Velocity initialVelocity, int msPollInterval)
    // Start integrating acceleration to determine position and velocity by polling for acceleration every while
        {
        synchronized (this.startStopLock)
            {
            // Stop doing this if we're already in flight
            this.stopAccelerationIntegration();

            // Set the current position and velocity
            this.accelerationAlgorithm.initialize(this.parameters, initalPosition, initialVelocity);

            // Make a new thread on which to do the integration
            this.accelerationMananger = ThreadPool.newSingleThreadExecutor("imu acceleration");

            // Start the whole schebang a rockin...
            this.accelerationMananger.execute(new AccelerationManager(msPollInterval));
            }
        }
    
    public void stopAccelerationIntegration() // needs a different lock than 'synchronized(this)'
        {
        synchronized (this.startStopLock)
            {
            // Stop the integration thread
            if (this.accelerationMananger != null)
                {
                this.accelerationMananger.shutdownNow();
                ThreadPool.awaitTerminationOrExitApplication(this.accelerationMananger, 10, TimeUnit.SECONDS, "IMU acceleration", "unresponsive user acceleration code");
                this.accelerationMananger = null;
                }
            }
        }

    /** Maintains current velocity and position by integrating acceleration */
    class AccelerationManager implements Runnable
        {
        protected final int msPollInterval;
        protected final static long nsPerMs = ElapsedTime.MILLIS_IN_NANO;
        
        AccelerationManager(int msPollInterval)
            {
            this.msPollInterval = msPollInterval;
            }
        
        @Override public void run()
            {
            // Don't let inappropriate exceptions sneak out
            try
                {
                // Loop until we're asked to stop
                while (!isStopRequested())
                    {
                    // Read the latest available acceleration
                    final Acceleration linearAcceleration = BNO055IMUImpl.this.getLinearAcceleration();

                    // Have the algorithm do its thing
                    synchronized (dataLock)
                        {
                        accelerationAlgorithm.update(linearAcceleration);
                        }
                    
                    // Wait an appropriate interval before beginning again
                    if (msPollInterval > 0)
                        {
                        long msSoFar = (System.nanoTime() - linearAcceleration.acquisitionTime) / nsPerMs;
                        long msReadFudge = 5;   // very roughly accounts for delta from read request to acquisitionTime setting
                        Thread.sleep(Math.max(0,msPollInterval - msSoFar - msReadFudge));
                        }
                    else
                        Thread.yield(); // never do a hard spin
                    }
                }
            catch (InterruptedException|CancellationException e)
                {
                return;
                }
            }
        }

    boolean isStopRequested()
        {
        return Thread.currentThread().isInterrupted();
        }

    @Override public synchronized byte read8(final Register reg)
        {
        return deviceClient.read8(reg.bVal);
        }

    @Override public synchronized byte[] read(final Register reg, final int cb)
        {
        return deviceClient.read(reg.bVal, cb);
        }

    protected short readShort(final Register reg)
        {
        byte[] data = read(reg, 2);
        return TypeConversion.byteArrayToShort(data, ByteOrder.LITTLE_ENDIAN);
        }


    @Override public void write8(Register reg, int data)
        {
        this.deviceClient.write8(reg.bVal, data);
        }
    @Override public void write(Register reg, byte[] data)
        {
        this.deviceClient.write(reg.bVal, data);
        }

    protected void writeShort(final Register reg, short value)
        {
        byte[] data = TypeConversion.shortToByteArray(value, ByteOrder.LITTLE_ENDIAN);
        write(reg, data);
        }
    protected void waitForWriteCompletions()
        {
        // We use ATOMIC for legacy reasons, but now that we have WRITTEN, that might
        // be a better choice.
        this.deviceClient.waitForWriteCompletions(I2cWaitControl.ATOMIC);
        }

    //------------------------------------------------------------------------------------------
    // Internal utility
    //------------------------------------------------------------------------------------------
    
    protected String getLoggingTag()
        {
        return parameters.loggingTag;
        }

    protected void log_v(String format, Object... args)
        {
        if (this.parameters.loggingEnabled)
            {
            String message = String.format(format, args);
            Log.v(getLoggingTag(), message);
            }
        }

    protected void log_d(String format, Object... args)
        {
        if (this.parameters.loggingEnabled)
            {
            String message = String.format(format, args);
            Log.d(getLoggingTag(), message);
            }
        }

    protected void log_w(String format, Object... args)
        {
        if (this.parameters.loggingEnabled)
            {
            String message = String.format(format, args);
            Log.w(getLoggingTag(), message);
            }
        }

    protected void log_e(String format, Object... args)
        {
        if (this.parameters.loggingEnabled)
            {
            String message = String.format(format, args);
            Log.e(getLoggingTag(), message);
            }
        }

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

    // Our write logic doesn't actually know when the I2C writes are issued. All it knows is
    // when the write has made it to the USB Core Device Interface Module. It's a pretty
    // deterministic interval after that that the I2C write occurs, we guess, but we don't
    // really know what that is. To account for this, we slop in some extra time to the
    // delays so that we're not cutting things too close to the edge. And given that this is
    // initialization logic and so not time critical, we err on being generous: the current
    // setting of this extra can undoubtedly be reduced.

    protected final static int msExtra = 50;

    protected void delayExtra(int ms)
        {
        delay(ms + msExtra);
        }
    protected void delayLoreExtra(int ms)
        {
        delayLore(ms + msExtra);
        }

    /**
     * delayLore() implements a delay that only known by lore and mythology to be necessary.
     * 
     * @see #delay(int) 
     */
    protected void delayLore(int ms)
        {
        delay(ms);
        }

    /**
     * delay() implements delays which are known to be necessary according to the BNO055 specification
     * 
     * @see #delayLore(int) 
     */
    protected void delay(int ms)
        {
        try
            {
            // delays are usually relative to preceding writes, so make sure they're all out to the controller
            this.waitForWriteCompletions();
            Thread.sleep((int)(ms * delayScale));
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            }
        }

    protected void enterConfigModeFor(Runnable action)
        {
        SensorMode modePrev = this.currentMode;
        setSensorMode(SensorMode.CONFIG);
        delayLoreExtra(25);
        try
            {
            action.run();
            }
        finally
            {
            setSensorMode(modePrev);
            delayLoreExtra(20);
            }
        }

    protected <T> T enterConfigModeFor(Func<T> lambda)
        {
        T result;
        
        SensorMode modePrev = this.currentMode;
        setSensorMode(SensorMode.CONFIG);
        delayLoreExtra(25);
        try
            {
            result = lambda.value();
            }
        finally
            {
            setSensorMode(modePrev);
            delayLoreExtra(20);
            }
        //
        return result;
        }
    
    //------------------------------------------------------------------------------------------
    // Constants
    //------------------------------------------------------------------------------------------

    final static byte bCHIP_ID_VALUE = (byte)0xa0;

    enum VECTOR
        {
            ACCELEROMETER   (Register.ACC_DATA_X_LSB),
            MAGNETOMETER    (Register.MAG_DATA_X_LSB),
            GYROSCOPE       (Register.GYR_DATA_X_LSB),
            EULER           (Register.EUL_H_LSB),
            LINEARACCEL     (Register.LIA_DATA_X_LSB),
            GRAVITY         (Register.GRV_DATA_X_LSB);
        //------------------------------------------------------------------------------------------
        protected byte value;
        VECTOR(int value) { this.value = (byte)value; }
        VECTOR(Register register) { this(register.bVal); }
        public byte getValue() { return this.value; }
        }

    enum POWER_MODE
        {
            NORMAL(0X00),
            LOWPOWER(0X01),
            SUSPEND(0X02);
        //------------------------------------------------------------------------------------------
        protected byte value;
        POWER_MODE(int value) { this.value = (byte)value; }
        public byte getValue() { return this.value; }
        }

    }

// This code is in part modelled after https://github.com/adafruit/Adafruit_BNO055

/***************************************************************************
 This is a library for the BNO055 orientation sensor

 Designed specifically to work with the Adafruit BNO055 Breakout.

 Pick one up today in the adafruit shop!
 ------> http://www.adafruit.com/products

 These sensors use I2C to communicate, 2 pins are required to interface.

 Adafruit invests time and resources providing this open source code,
 please support Adafruit andopen-source hardware by purchasing products
 from Adafruit!

 Written by KTOWN for Adafruit Industries.

 MIT license, all text above must be included in any redistribution
 ***************************************************************************/
