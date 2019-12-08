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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.qualcomm.robotcore.hardware.I2cAddr;

import org.firstinspires.ftc.robotcore.external.navigation.Acceleration;
import org.firstinspires.ftc.robotcore.external.navigation.AngularVelocity;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.MagneticFlux;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;
import org.firstinspires.ftc.robotcore.external.navigation.Position;
import org.firstinspires.ftc.robotcore.external.navigation.Quaternion;
import org.firstinspires.ftc.robotcore.external.navigation.Temperature;
import org.firstinspires.ftc.robotcore.external.navigation.Velocity;
import org.firstinspires.ftc.robotcore.internal.collections.SimpleGson;

import java.util.Locale;

/**
 * {@link BNO055IMU} interface abstracts the functionality of the Bosch/Sensortec
 * BNO055 Intelligent 9-axis absolute orientation sensor. The BNO055 can output the
 * following sensor data (as described in <a href="http://www.adafruit.com/products/2472">AdaFruit
 * Absolute Orientation Sensor</a>).
 *
 *  <ol>
 *      <li>Absolute Orientation (Euler Vector, 100Hz) Three axis orientation data based on a 360° sphere</li>
 *      <li>Absolute Orientation (Quaterion, 100Hz) Four point quaternion output for more accurate data manipulation</li>
 *      <li>Angular Velocity Vector (100Hz) Three axis of 'rotation speed' in rad/s</li>
 *      <li>Acceleration Vector (100Hz) Three axis of acceleration (gravity + linear motion) in m/s^2</li>
 *      <li>Magnetic Field Strength Vector (20Hz) Three axis of magnetic field sensing in micro Tesla (uT)</li>
 *      <li>Linear Acceleration Vector (100Hz) Three axis of linear acceleration data (acceleration minus gravity) in m/s^2</li>
 *      <li>Gravity Vector (100Hz) Three axis of gravitational acceleration (minus any movement) in m/s^2</li>
 *      <li>Temperature (1Hz) Ambient temperature in degrees celsius</li>
 *  </ol>
 *
 * <p>Of those, the first (the gravity-corrected absolute orientation vector) is arguably the most
 * useful in FTC robot design. It's really handy.</p>
 *
 * @see BNO055IMUImpl
 * @see <a href="https://www.bosch-sensortec.com/bst/products/all_products/bno055">BNO055 product page</a>
 * @see <a href="https://ae-bst.resource.bosch.com/media/_tech/media/datasheets/BST_BNO055_DS000_14.pdf">BNO055 specification</a>
 */
@SuppressWarnings("WeakerAccess")
public interface BNO055IMU
    {
    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    /**
     * Initialize the sensor using the indicated set of parameters. Note that the execution of
     * this method can take a fairly long while, possibly several tens of milliseconds.
     *
     * @param parameters the parameters with which to initialize the device
     * @return whether initialization was successful or not
     */
    boolean initialize(@NonNull Parameters parameters);

    /**
     * Returns the parameters which which initialization was last attempted, if any
     * @return the parameters which which initialization was last attempted, if any
     */
    @NonNull Parameters getParameters();

    /**
     * Instances of Parameters contain data indicating how a BNO055 absolute orientation
     * sensor is to be initialized.
     *
     * @see #initialize(Parameters)
     */
    class Parameters implements Cloneable
        {
        /** the address at which the sensor resides on the I2C bus.  */
        public I2cAddr          i2cAddr             = I2CADDR_DEFAULT;

        /** the mode we wish to use the sensor in */
        public SensorMode       mode                = SensorMode.IMU;

        /** whether to use the external or internal 32.768khz crystal. External crystal
         * use is recommended by the BNO055 specification. */
        public boolean          useExternalCrystal  = true;

        /** units in which temperature are measured. See Section 3.6.1 (p31) of the BNO055 specification */
        public TempUnit         temperatureUnit     = TempUnit.CELSIUS;
        /** units in which angles and angular rates are measured. See Section 3.6.1 (p31) of the BNO055 specification */
        public AngleUnit        angleUnit           = AngleUnit.RADIANS;
        /** units in which accelerations are measured. See Section 3.6.1 (p31) of the BNO055 specification */
        public AccelUnit        accelUnit           = AccelUnit.METERS_PERSEC_PERSEC;
        /** directional convention for measureing pitch angles. See Section 3.6.1 (p31) of the BNO055 specification */
        public PitchMode        pitchMode           = PitchMode.ANDROID;    // Section 3.6.2

        /** accelerometer range. See Section 3.5.2 (p27) and Table 3-4 (p21) of the BNO055 specification */
        public AccelRange       accelRange          = AccelRange.G4;
        /** accelerometer bandwidth. See Section 3.5.2 (p27) and Table 3-4 (p21) of the BNO055 specification */
        public AccelBandwidth   accelBandwidth      = AccelBandwidth.HZ62_5;
        /** accelerometer power mode. See Section 3.5.2 (p27) and Section 4.2.2 (p77) of the BNO055 specification */
        public AccelPowerMode   accelPowerMode      = AccelPowerMode.NORMAL;

        /** gyroscope range. See Section 3.5.2 (p27) and Table 3-4 (p21) of the BNO055 specification */
        public GyroRange        gyroRange           = GyroRange.DPS2000;
        /** gyroscope bandwidth. See Section 3.5.2 (p27) and Table 3-4 (p21) of the BNO055 specification */
        public GyroBandwidth    gyroBandwidth       = GyroBandwidth.HZ32;
        /** gyroscope power mode. See Section 3.5.2 (p27) and Section 4.4.4 (p78) of the BNO055 specification */
        public GyroPowerMode    gyroPowerMode       = GyroPowerMode.NORMAL;

        /** magnetometer data rate. See Section 3.5.3 (p27) and Section 4.4.3 (p77) of the BNO055 specification */
        public MagRate          magRate             = MagRate.HZ10;
        /** magnetometer op mode. See Section 3.5.3 (p27) and Section 4.4.3 (p77) of the BNO055 specification */
        public MagOpMode        magOpMode           = MagOpMode.REGULAR;
        /** magnetometer power mode. See Section 3.5.3 (p27) and Section 4.4.3 (p77) of the BNO055 specification */
        public MagPowerMode     magPowerMode        = MagPowerMode.NORMAL;

        /** Calibration data with which the BNO055 should be initialized. If calibrationData is non-null,
         * it is used. Otherwise, if calibrationDataFile is non-null, it is used. Otherwise, only the default
         * automatic calibration of the IMU is used*/
        public CalibrationData  calibrationData     = null;
        public String           calibrationDataFile = null;

        /** the algorithm to use for integrating acceleration to produce velocity and position.
         * If not specified, a simple but not especially effective internal algorithm will be used.
         * @see #startAccelerationIntegration(Position, Velocity, int) */
        public AccelerationIntegrator accelerationIntegrationAlgorithm = null;

        /** debugging aid: enable logging for this device? */
        public boolean          loggingEnabled      = false;
        /** debugging aid: the logging tag to use when logging */
        public String           loggingTag          = "AdaFruitIMU";

        public Parameters clone()
            {
            try {
                Parameters result = (Parameters)super.clone();
                result.calibrationData = result.calibrationData==null ? null : result.calibrationData.clone();
                return result;
                }
            catch (CloneNotSupportedException e)
                {
                throw new RuntimeException("internal error: Parameters can't be cloned");
                }
            }
        }

    /**
     * Shut down the sensor. This doesn't do anything in the hardware device itself, but rather
     * shuts down any resources (threads, etc) that we use to communicate with it. It is rare
     * that user code has a need to call this method.
     */
    void close();

    //----------------------------------------------------------------------------------------------
    // Reading sensor output
    //----------------------------------------------------------------------------------------------

    /** Returns the absolute orientation of the sensor as a set three angles
     * @see #getQuaternionOrientation()
     * @return the absolute orientation of the sensor
     * @see Orientation
     * @see #getAngularOrientation(AxesReference, AxesOrder, org.firstinspires.ftc.robotcore.external.navigation.AngleUnit)
     */
    Orientation getAngularOrientation();

    /**
     * Returns the absolute orientation of the sensor as a set three angles with indicated parameters.
     * @param reference the axes reference in which the result will be expressed
     * @param order     the axes order in which the result will be expressed
     * @param angleUnit the angle units in which the result will be expressed
     * @return the absolute orientation of the sensor
     * @see Orientation
     * @see #getAngularOrientation()
     */
    Orientation getAngularOrientation(AxesReference reference, AxesOrder order, org.firstinspires.ftc.robotcore.external.navigation.AngleUnit angleUnit);

    /**
     * Returns the overall acceleration experienced by the sensor. This is composed of
     * a component due to the movement of the sensor and a component due to the force of gravity.
     * @return  the overall acceleration vector experienced by the sensor
     * @see #getLinearAcceleration()
     * @see #getGravity()
     */
    Acceleration getOverallAcceleration();

    /**
     * Returns the rate of change of the absolute orientation of the sensor.
     * @return the rate at which the orientation of the sensor is changing.
     * @see #getAngularOrientation()
     */
    AngularVelocity getAngularVelocity();

    /**
     * Returns the acceleration experienced by the sensor due to the movement of the sensor.
     * @return  the acceleration vector of the sensor due to its movement
     * @see #getOverallAcceleration()
     * @see #getGravity()
     */
    Acceleration getLinearAcceleration();

    /**
     * Returns the direction of the force of gravity relative to the sensor.
     * @return  the acceleration vector of gravity relative to the sensor
     * @see #getOverallAcceleration()
     * @see #getLinearAcceleration()
     */
    Acceleration getGravity();

    /**
     * Returns the current temperature.
     * @return  the current temperature
     */
    Temperature getTemperature();

    /**
     * Returns the magnetic field strength experienced by the sensor. See Section 3.6.5.2 of
     * the BNO055 specification.
     * @return  the magnetic field strength experienced by the sensor
     */
    MagneticFlux getMagneticFieldStrength();

    /** Returns the absolute orientation of the sensor as a quaternion.
     * @see #getAngularOrientation()
     *
     * @return  the absolute orientation of the sensor
     */
    Quaternion getQuaternionOrientation();

    //----------------------------------------------------------------------------------------------
    // Position and velocity management
    //----------------------------------------------------------------------------------------------

    /**
     * Returns the current position of the sensor as calculated by doubly integrating the observed
     * sensor accelerations.
     * @return  the current position of the sensor.
     * @see Parameters#accelerationIntegrationAlgorithm
     * @see #startAccelerationIntegration(Position, Velocity, int)
     */
    Position getPosition();

    /**
     * Returns the current velocity of the sensor as calculated by integrating the observed
     * sensor accelerations.
     * @return  the current velocity of the sensor
     * @see Parameters#accelerationIntegrationAlgorithm
     * @see #startAccelerationIntegration(Position, Velocity, int)
     */
    Velocity getVelocity();

    /**
     * Returns the last observed acceleration of the sensor. Note that this does not communicate
     * with the sensor, but rather returns the most recent value reported to the acceleration
     * integration algorithm.
     * @return  the last observed acceleration of the sensor
     * @see #getLinearAcceleration()
     * @see Parameters#accelerationIntegrationAlgorithm
     * @see #startAccelerationIntegration(Position, Velocity, int)
     */
    Acceleration getAcceleration();

    /**
     * Start (or re-start) a thread that continuously at intervals polls the current linear acceleration
     * of the sensor and integrates it to provide velocity and position information.
     * @param initialPosition  If non-null, the current sensor position is set to this value. If
     *                         null, the current sensor position is unchanged.
     * @param initialVelocity  If non-null, the current sensor velocity is set to this value. If
     *                         null, the current sensor velocity is unchanged.
     * @param msPollInterval   the interval to use, in milliseconds, between successive calls to {@link #getLinearAcceleration()}
     * @see #stopAccelerationIntegration()
     * @see AccelerationIntegrator
     */
    void startAccelerationIntegration(Position initialPosition, Velocity initialVelocity, int msPollInterval);

    /**
     * Stop the integration thread if it is currently running.
     * @see #startAccelerationIntegration(Position, Velocity, int)
     */
    void stopAccelerationIntegration();

    /**
     * {@link AccelerationIntegrator} encapsulates an algorithm for integrating
     * acceleration information over time to produce velocity and position.
     *
     * @see BNO055IMU
     */
    interface AccelerationIntegrator
        {
        /**
         * (Re)initializes the algorithm with a starting position and velocity. Any timestamps that
         * are present in these data are not to be considered as significant. The initial acceleration
         * should be taken as undefined; you should set it to null when this method is called.
         * @param parameters        configuration parameters for the IMU
         * @param initialPosition   If non-null, the current sensor position is set to this value. If
         *                          null, the current sensor position is unchanged.
         * @param initialVelocity   If non-null, the current sensor velocity is set to this value. If
         *                          null, the current sensor velocity is unchanged.
         *
         * @see #update(Acceleration)
         */
        void initialize(@NonNull Parameters parameters, @Nullable Position initialPosition, @Nullable Velocity initialVelocity);

        /**
         * Returns the current position as calculated by the algorithm
         * @return  the current position
         */
        Position getPosition();

        /**
         * Returns the current velocity as calculated by the algorithm
         * @return  the current velocity
         */
        Velocity getVelocity();

        /**
         * Returns the current acceleration as understood by the algorithm. This is typically
         * just the value provided in the most recent call to {@link #update(Acceleration)}, if any.
         * @return  the current acceleration, or null if the current position is undefined
         */
        Acceleration getAcceleration();

        /**
         * Step the algorithm as a result of the stimulus of new acceleration data.
         * @param linearAcceleration  the acceleration as just reported by the IMU
         */
        void update(Acceleration linearAcceleration);
        }

    //----------------------------------------------------------------------------------------------
    // Status inquiry
    //----------------------------------------------------------------------------------------------

    /**
     * Returns the current status of the system.
     * @return the current status of the system
     *
     * See section 4.3.58 of the BNO055 specification.
     * @see #getSystemError()
     *
    <table summary="System Status Codes">
     <tr><td>Result</td><td>Meaning</td></tr>
     <tr><td>0</td><td>idle</td></tr>
     <tr><td>1</td><td>system error</td></tr>
     <tr><td>2</td><td>initializing peripherals</td></tr>
     <tr><td>3</td><td>system initialization</td></tr>
     <tr><td>4</td><td>executing self-test</td></tr>
     <tr><td>5</td><td>sensor fusion algorithm running</td></tr>
     <tr><td>6</td><td>system running without fusion algorithms</td></tr>
     </table> */
    SystemStatus getSystemStatus();

    /** If {@link #getSystemStatus()} is 'system error' (1), returns particulars
    * regarding that error.
    *
    * See section 4.3.58 of the BNO055 specification.
    * @return the current error status
    * @see #getSystemStatus()
    *
    <table summary="System Error Codes">
    <tr><td>Result</td><td>Meaning</td></tr>
    <tr><td>0</td><td>no error</td></tr>
    <tr><td>1</td><td>peripheral initialization error</td></tr>
    <tr><td>2</td><td>system initialization error</td></tr>
    <tr><td>3</td><td>self test result failed</td></tr>
    <tr><td>4</td><td>register map value out of range</td></tr>
    <tr><td>5</td><td>register map address out of range</td></tr>
    <tr><td>6</td><td>register map write error</td></tr>
    <tr><td>7</td><td>BNO low power mode not available for selected operation mode</td></tr>
    <tr><td>8</td><td>accelerometer power mode not available</td></tr>
    <tr><td>9</td><td>fusion algorithm configuration error</td></tr>
    <tr><td>A</td><td>sensor configuration error</td></tr>
    </table> */
    SystemError getSystemError();

    /**
     * Returns the calibration status of the IMU
     * @return the calibration status of the IMU
     */
    CalibrationStatus getCalibrationStatus();

    /**
     * Answers as to whether the system is fully calibrated. The system is fully
     * calibrated if the gyro, accelerometer, and magnetometer are fully calibrated.
     * @return whether the system is fully calibrated.
     */
    boolean isSystemCalibrated();
    /**
     * Answers as to whether the gyro is fully calibrated.
     * @return whether the gyro is fully calibrated.
     */
    boolean isGyroCalibrated();
    /**
     * Answers as to whether the accelerometer is fully calibrated.
     * @return whether the accelerometer is fully calibrated.
     */
    boolean isAccelerometerCalibrated();
    /**
     * Answers as to whether the magnetometer is fully calibrated.
     * @return whether the magnetometer is fully calibrated.
     */
    boolean isMagnetometerCalibrated();

    /**
     * See Section 3.6.4 of the BNO055 Specification.
     */
    class CalibrationData implements Cloneable
        {
        public short dxAccel, dyAccel, dzAccel; // units are milli-g's
        public short dxMag,   dyMag,   dzMag;   // units are micro telsa
        public short dxGyro,  dyGyro,  dzGyro;  // units are degrees / second
        public short radiusAccel, radiusMag;    // units are unknown

        public String serialize() {
            return SimpleGson.getInstance().toJson(this);
            }
        public static CalibrationData deserialize(String data) {
            return SimpleGson.getInstance().fromJson(data, CalibrationData.class);
            }

        public CalibrationData clone()
            {
            try {
                CalibrationData result = (CalibrationData)super.clone();
                return result;
                }
            catch (CloneNotSupportedException e)
                {
                throw new RuntimeException("internal error: CalibrationData can't be cloned");
                }
            }
        }

    /**
     * Read calibration data from the IMU which later can be restored with writeCalibrationData().
     * This might be persistently stored, and reapplied at a later power-on.
     *
     * For greatest utility, full calibration should be achieved before reading
     * the calibration data.
     *
     * @return the calibration data
     * @see #writeCalibrationData(CalibrationData)
     */
    CalibrationData readCalibrationData();

    /**
     * Write calibration data previously retrieved.
     *
     * @param data  the calibration data to write
     * @see #readCalibrationData()
     */
    void writeCalibrationData(CalibrationData data);

    //----------------------------------------------------------------------------------------------
    // Low level reading and writing
    //----------------------------------------------------------------------------------------------

    /**
     * Low level: read the byte starting at the indicated register
     * @param register  the location from which to read the data
     * @return          the data that was read
     */
    byte  read8(Register register);
    /**
     * Low level: read data starting at the indicated register
     * @param register  the location from which to read the data
     * @param cb        the number of bytes to read
     * @return          the data that was read
     */
    byte[] read(Register register, int cb);

    /**
     * Low level: write a byte to the indicated register
     * @param register  the location at which to write the data
     * @param bVal      the data to write
     */
    void write8(Register register, int bVal);
    /**
     * Low level: write data starting at the indicated register
     * @param register  the location at which to write the data
     * @param data      the data to write
     */
    void write (Register register, byte[] data);

    //----------------------------------------------------------------------------------------------
    // Enumerations to make all of the above work
    //----------------------------------------------------------------------------------------------

    public static final I2cAddr I2CADDR_UNSPECIFIED = I2cAddr.zero();
    public static final I2cAddr I2CADDR_DEFAULT     = I2cAddr.create7bit(0x28);
    public static final I2cAddr I2CADDR_ALTERNATE   = I2cAddr.create7bit(0x29);

    enum TempUnit { CELSIUS(0), FARENHEIT(1); public final byte bVal; TempUnit(int i)  { bVal =(byte)i; }
        public org.firstinspires.ftc.robotcore.external.navigation.TempUnit toTempUnit()
            {
            if (this==CELSIUS)
                return org.firstinspires.ftc.robotcore.external.navigation.TempUnit.CELSIUS;
            else
                return org.firstinspires.ftc.robotcore.external.navigation.TempUnit.FARENHEIT;
            }
        public static TempUnit fromTempUnit(org.firstinspires.ftc.robotcore.external.navigation.TempUnit tempUnit)
            {
            if (tempUnit==org.firstinspires.ftc.robotcore.external.navigation.TempUnit.CELSIUS)
              return CELSIUS;
            else if (tempUnit==org.firstinspires.ftc.robotcore.external.navigation.TempUnit.FARENHEIT)
              return FARENHEIT;
            else
              throw new UnsupportedOperationException("TempUnit." + tempUnit + " is not supported by BNO055IMU");
            }
    }
    enum AngleUnit { DEGREES(0), RADIANS(1); public final byte bVal; AngleUnit(int i) { bVal =(byte)i; }
        public org.firstinspires.ftc.robotcore.external.navigation.AngleUnit toAngleUnit()
            {
            if (this==DEGREES)
                return org.firstinspires.ftc.robotcore.external.navigation.AngleUnit.DEGREES;
            else
                return org.firstinspires.ftc.robotcore.external.navigation.AngleUnit.RADIANS;
            }
        public static AngleUnit fromAngleUnit(org.firstinspires.ftc.robotcore.external.navigation.AngleUnit angleUnit)
            {
            if (angleUnit==org.firstinspires.ftc.robotcore.external.navigation.AngleUnit.DEGREES)
                return DEGREES;
            else
                return RADIANS;
            }
    }
    enum AccelUnit { METERS_PERSEC_PERSEC(0), MILLI_EARTH_GRAVITY(1); public final byte bVal; AccelUnit(int i) { bVal =(byte)i; }}
    enum PitchMode { WINDOWS(0), ANDROID(1);                          public final byte bVal; PitchMode(int i) { bVal =(byte)i; }}

    enum GyroRange      { DPS2000(0), DPS1000(1), DPS500(2), DPS250(3), DPS125(4);                               public final byte bVal; GyroRange(int i)      { bVal =(byte)(i<<0);}}
    enum GyroBandwidth  { HZ523(0), HZ230(1), HZ116(2), HZ47(3), HZ23(4), HZ12(5), HZ64(6), HZ32(7);             public final byte bVal; GyroBandwidth(int i)  { bVal =(byte)(i<<3);}}
    enum GyroPowerMode  { NORMAL(0), FAST(1), DEEP(2), SUSPEND(3), ADVANCED(4) ;                                 public final byte bVal; GyroPowerMode(int i)  { bVal =(byte)(i<<0);}}
    enum AccelRange     { G2(0), G4(1), G8(2), G16(3);                                                           public final byte bVal; AccelRange(int i)     { bVal =(byte)(i<<0);}}
    enum AccelBandwidth { HZ7_81(0), HZ15_63(1), HZ31_25(2), HZ62_5(3), HZ125(4), HZ250(5), HZ500(6), HZ1000(7); public final byte bVal; AccelBandwidth(int i) { bVal =(byte)(i<<2);}}
    enum AccelPowerMode { NORMAL(0), SUSPEND(1), LOW1(2), STANDBY(3), LOW2(4), DEEP(5);                          public final byte bVal; AccelPowerMode(int i) { bVal =(byte)(i<<5);}}

    enum MagRate        { HZ2(0), HZ6(1), HZ8(2), HZ10(3), HZ15(4), HZ20(5), HZ25(6), HZ30(7);                   public final byte bVal; MagRate(int i)        { bVal =(byte)(i<<0);}}
    enum MagOpMode      { LOW(0), REGULAR(1), ENHANCED(2), HIGH(3);                                              public final byte bVal; MagOpMode(int i)      { bVal =(byte)(i<<3);}}
    enum MagPowerMode   { NORMAL(0), SLEEP(1), SUSPEND(2), FORCE(3);                                             public final byte bVal; MagPowerMode(int i)   { bVal =(byte)(i<<5);}}

    /** @see #getSystemStatus() */
    enum SystemStatus   { UNKNOWN(-1), IDLE(0), SYSTEM_ERROR(1), INITIALIZING_PERIPHERALS(2), SYSTEM_INITIALIZATION(3),
                          SELF_TEST(4), RUNNING_FUSION(5), RUNNING_NO_FUSION(6);                                 public final byte bVal; SystemStatus(int value) { this.bVal = (byte)value; }

        public static SystemStatus from(int value)
            {
            for (SystemStatus systemStatus : values())
                {
                if (systemStatus.bVal == value) return systemStatus;
                }
            return UNKNOWN;
            }

        public String toShortString()
            {
            switch (this)
                {
                case IDLE:                      return "idle";
                case SYSTEM_ERROR:              return "syserr";
                case INITIALIZING_PERIPHERALS:  return "periph";
                case SYSTEM_INITIALIZATION:     return "sysinit";
                case SELF_TEST:                 return "selftest";
                case RUNNING_FUSION:            return "fusion";
                case RUNNING_NO_FUSION:         return "running";
                }
            return "unk";
            }
        }

    /** @see #getSystemError()  */
    enum SystemError   { UNKNOWN(-1), NO_ERROR(0), PERIPHERAL_INITIALIZATION_ERROR(1), SYSTEM_INITIALIZATION_ERROR(2),
                         SELF_TEST_FAILED(3), REGISTER_MAP_OUT_OF_RANGE(4), REGISTER_MAP_ADDRESS_OUT_OF_RANGE(5),
                         REGISTER_MAP_WRITE_ERROR(6), LOW_POWER_MODE_NOT_AVAILABLE(7), ACCELEROMETER_POWER_MODE_NOT_AVAILABLE(8),
                         FUSION_CONFIGURATION_ERROR(9), SENSOR_CONFIGURATION_ERROR(10);                          public final byte bVal; SystemError(int value) { this.bVal = (byte)value; }

        public static SystemError from(int value)
            {
            for (SystemError systemError : values())
                {
                if (systemError.bVal == value) return systemError;
                }
            return UNKNOWN;
            }
        }

    /** @see #getCalibrationStatus() */
    class CalibrationStatus
        {
        public final byte calibrationStatus;
        public CalibrationStatus(int calibrationStatus)
            {
            this.calibrationStatus = (byte)calibrationStatus;
            }

        public @Override String toString()
            {
            StringBuilder result = new StringBuilder();
            result.append(String.format(Locale.getDefault(), "s%d", (calibrationStatus >> 6) & 0x03));  // SYS calibration status
            result.append(" ");
            result.append(String.format(Locale.getDefault(), "g%d", (calibrationStatus >> 4) & 0x03));  // GYR calibration status
            result.append(" ");
            result.append(String.format(Locale.getDefault(), "a%d", (calibrationStatus >> 2) & 0x03));  // ACC calibration status
            result.append(" ");
            result.append(String.format(Locale.getDefault(), "m%d", (calibrationStatus >> 0) & 0x03));  // MAG calibration status
            return result.toString();
            }
        }

    /**
     * Sensor modes are described in Table 3-5 (p21) of the BNO055 specification,
     * where they are termed "operation modes".
     */
    enum SensorMode
        {
            CONFIG(0X00),       ACCONLY(0X01),          MAGONLY(0X02),
            GYRONLY(0X03),      ACCMAG(0X04),           ACCGYRO(0X05),
            MAGGYRO(0X06),      AMG(0X07),              IMU(0X08),
            COMPASS(0X09),      M4G(0X0A),              NDOF_FMC_OFF(0X0B),
            NDOF(0X0C),
            DISABLED(-1);   // DISABLED isn't an actual IMU mode
        //------------------------------------------------------------------------------------------
        public final byte bVal;
        SensorMode(int i) { this.bVal = (byte) i; }

        /** Is this SensorMode one of the fusion modes in which the BNO055 operates? */
        public boolean isFusionMode()
            {
            // See Table 3-5, p21, of the BNO055 specification
            switch (this)
                {
                case IMU:
                case COMPASS:
                case M4G:
                case NDOF_FMC_OFF:
                case NDOF:
                    return true;
                default:
                    return false;
                }
            }
        }

    /**
     * {@link Register} provides symbolic names for each of the BNO055 device registers.
     */
    enum Register
        {
            /** Controls which of the two register pages are visible */
            PAGE_ID(0X07),

            CHIP_ID(0x00),
            ACC_ID(0x01),
            MAG_ID(0x02),
            GYR_ID(0x03),
            SW_REV_ID_LSB(0x04),
            SW_REV_ID_MSB(0x05),
            BL_REV_ID(0X06),

            /** Acceleration data register */
            ACC_DATA_X_LSB(0X08),
            ACC_DATA_X_MSB(0X09),
            ACC_DATA_Y_LSB(0X0A),
            ACC_DATA_Y_MSB(0X0B),
            ACC_DATA_Z_LSB(0X0C),
            ACC_DATA_Z_MSB(0X0D),

            /** Magnetometer data register */
            MAG_DATA_X_LSB(0X0E),
            MAG_DATA_X_MSB(0X0F),
            MAG_DATA_Y_LSB(0X10),
            MAG_DATA_Y_MSB(0X11),
            MAG_DATA_Z_LSB(0X12),
            MAG_DATA_Z_MSB(0X13),

            /** Gyro data registers */
            GYR_DATA_X_LSB(0X14),
            GYR_DATA_X_MSB(0X15),
            GYR_DATA_Y_LSB(0X16),
            GYR_DATA_Y_MSB(0X17),
            GYR_DATA_Z_LSB(0X18),
            GYR_DATA_Z_MSB(0X19),

            /** Euler data registers */
            EUL_H_LSB(0X1A),
            EUL_H_MSB(0X1B),
            EUL_R_LSB(0X1C),
            EUL_R_MSB(0X1D),
            EUL_P_LSB(0X1E),
            EUL_P_MSB(0X1F),

            /** Quaternion data registers */
            QUA_DATA_W_LSB(0X20),
            QUA_DATA_W_MSB(0X21),
            QUA_DATA_X_LSB(0X22),
            QUA_DATA_X_MSB(0X23),
            QUA_DATA_Y_LSB(0X24),
            QUA_DATA_Y_MSB(0X25),
            QUA_DATA_Z_LSB(0X26),
            QUA_DATA_Z_MSB(0X27),

            /** Linear acceleration data registers */
            LIA_DATA_X_LSB(0X28),
            LIA_DATA_X_MSB(0X29),
            LIA_DATA_Y_LSB(0X2A),
            LIA_DATA_Y_MSB(0X2B),
            LIA_DATA_Z_LSB(0X2C),
            LIA_DATA_Z_MSB(0X2D),

            /** Gravity data registers */
            GRV_DATA_X_LSB(0X2E),
            GRV_DATA_X_MSB(0X2F),
            GRV_DATA_Y_LSB(0X30),
            GRV_DATA_Y_MSB(0X31),
            GRV_DATA_Z_LSB(0X32),
            GRV_DATA_Z_MSB(0X33),

            /** Temperature data register */
            TEMP(0X34),

            /** Status registers */
            CALIB_STAT(0X35),
            SELFTEST_RESULT(0X36),
            INTR_STAT(0X37),

            SYS_CLK_STAT(0X38),
            SYS_STAT(0X39),
            SYS_ERR(0X3A),

            /** Unit selection register */
            UNIT_SEL(0X3B),
            DATA_SELECT(0X3C),

            /** Mode registers */
            OPR_MODE(0X3D),
            PWR_MODE(0X3E),

            SYS_TRIGGER(0X3F),
            TEMP_SOURCE(0X40),

            /** Axis remap registers */
            AXIS_MAP_CONFIG(0X41),
            AXIS_MAP_SIGN(0X42),

            /** SIC registers */
            SIC_MATRIX_0_LSB(0X43),
            SIC_MATRIX_0_MSB(0X44),
            SIC_MATRIX_1_LSB(0X45),
            SIC_MATRIX_1_MSB(0X46),
            SIC_MATRIX_2_LSB(0X47),
            SIC_MATRIX_2_MSB(0X48),
            SIC_MATRIX_3_LSB(0X49),
            SIC_MATRIX_3_MSB(0X4A),
            SIC_MATRIX_4_LSB(0X4B),
            SIC_MATRIX_4_MSB(0X4C),
            SIC_MATRIX_5_LSB(0X4D),
            SIC_MATRIX_5_MSB(0X4E),
            SIC_MATRIX_6_LSB(0X4F),
            SIC_MATRIX_6_MSB(0X50),
            SIC_MATRIX_7_LSB(0X51),
            SIC_MATRIX_7_MSB(0X52),
            SIC_MATRIX_8_LSB(0X53),
            SIC_MATRIX_8_MSB(0X54),

            /**Accelerometer Offset registers */
            ACC_OFFSET_X_LSB(0X55),
            ACC_OFFSET_X_MSB(0X56),
            ACC_OFFSET_Y_LSB(0X57),
            ACC_OFFSET_Y_MSB(0X58),
            ACC_OFFSET_Z_LSB(0X59),
            ACC_OFFSET_Z_MSB(0X5A),

            /** Magnetometer Offset registers */
            MAG_OFFSET_X_LSB(0X5B),
            MAG_OFFSET_X_MSB(0X5C),
            MAG_OFFSET_Y_LSB(0X5D),
            MAG_OFFSET_Y_MSB(0X5E),
            MAG_OFFSET_Z_LSB(0X5F),
            MAG_OFFSET_Z_MSB(0X60),

            /** Gyroscope Offset register s*/
            GYR_OFFSET_X_LSB(0X61),
            GYR_OFFSET_X_MSB(0X62),
            GYR_OFFSET_Y_LSB(0X63),
            GYR_OFFSET_Y_MSB(0X64),
            GYR_OFFSET_Z_LSB(0X65),
            GYR_OFFSET_Z_MSB(0X66),

            /** Radius registers */
            ACC_RADIUS_LSB(0X67),
            ACC_RADIUS_MSB(0X68),
            MAG_RADIUS_LSB(0X69),
            MAG_RADIUS_MSB(0X6A),

            /** Selected Page 1 registers */
            ACC_CONFIG(0x08),
            MAG_CONFIG(0x09),
            GYR_CONFIG_0(0x0A),
            GYR_CONFIG_1(0x0B),
            ACC_SLEEP_CONFIG(0x0C),
            GYR_SLEEP_CONFIG(0x0D),
            INT_MSK(0x0F),
            INT_EN(0x10),
            ACC_AM_THRES(0x11),
            ACC_INT_SETTINGS(0x12),
            ACC_HG_DURATION(0x13),
            ACC_HG_THRES(0x14),
            ACC_NM_THRES(0x15),
            ACC_NM_SET(0x16),
            GRYO_INT_SETTING(0x17),
            GRYO_HR_X_SET(0x18),
            GRYO_DUR_X(0x19),
            GRYO_HR_Y_SET(0x1A),
            GRYO_DUR_Y(0x1B),
            GRYO_HR_Z_SET(0x1C),
            GRYO_DUR_Z(0x1D),
            GRYO_AM_THRES(0x1E),
            GRYO_AM_SET(0x1F),

            UNIQUE_ID_FIRST(0x50),
            UNIQUE_ID_LAST(0x5F)
            ;

        //------------------------------------------------------------------------------------------
        public final byte bVal;
        Register(int i)
            {
            this.bVal = (byte)i;
            }
        }
    }
