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

package com.qualcomm.hardware.matrix;

import com.qualcomm.hardware.motors.MatrixLegacyMotor;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorController;
import com.qualcomm.robotcore.hardware.configuration.MatrixConstants;
import com.qualcomm.robotcore.hardware.configuration.typecontainers.MotorConfigurationType;
import com.qualcomm.robotcore.util.Range;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.TypeConversion;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.util.Arrays;
import java.util.Set;

@SuppressWarnings("WeakerAccess")
public class MatrixDcMotorController implements DcMotorController {

    /*
     * Controller properties caches.
     */
    private class MotorProperties {

        public MotorProperties(int motor)
        {
            target = 0;
            position = 0;
            mode = 0;
            power = 0.0;
            floating = true;
            runMode = DcMotor.RunMode.STOP_AND_RESET_ENCODER;
            zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE;
            // Note: we default to the legacy, 9.6v motor
            motorType = MotorConfigurationType.getMotorType(MatrixLegacyMotor.class);
        }

        public int target;
        public int position;
        public byte mode;
        public boolean floating;
        public double power;
        public DcMotor.RunMode runMode;
        public DcMotor.ZeroPowerBehavior zeroPowerBehavior;
        public MotorConfigurationType motorType;
    }

    /*
     * Waste the zero element for ease of indexing.
     */
    private MotorProperties[] motorCache = {
        new MotorProperties(1), new MotorProperties(1), new MotorProperties(2), new MotorProperties(3), new MotorProperties(4)
    };

    /** Used to help implement setMotorPower for a set of motors */
    private boolean pendMotorPowerChanges = false;

    public static final byte POWER_MAX = 0x64;
    public static final byte POWER_MIN = -0x64;

    protected static final double apiPowerMin = -1.0;
    protected static final double apiPowerMax = 1.0;

    /*
     * Motors float.  No PID, no encoders.
     */
    private final static byte CHANNEL_MODE_FLAG_SELECT_FLOAT         = 0x00;

    /*
     * Run without PID control, but with motor braking
     */
    private final static byte CHANNEL_MODE_FLAG_SELECT_POWER_CONTROL = 0x01;
    /*
     * PID Speed control
     */
    private final static byte CHANNEL_MODE_FLAG_SELECT_SPEED_CONTROL = 0x02;
    /*
     * Run To Position (target)
     */
    private final static byte CHANNEL_MODE_FLAG_SELECT_RTP_CONTROL   = 0x03;
    /*
     * Reset position encoder to zero
     */
    private final static byte CHANNEL_MODE_FLAG_SELECT_RESET         = 0x04;

    private static final byte I2C_DATA_OFFSET    = 0x04;
    private static final byte MODE_PENDING_BIT   = 0x08;
    private static final byte SPEED_STOPPED      = 0;
    private static final int  MAX_NUM_MOTORS     = MatrixConstants.NUMBER_OF_MOTORS;
    private static final int  NO_TARGET          = 0;
    private static final int  BATTERY_UNITS      = 40;
    private static final int  POSITION_DATA_SIZE = 4;
    private static final int  TARGET_DATA_SIZE   = 4;

    protected MatrixMasterController master;
    private int batteryVal;

    public MatrixDcMotorController(MatrixMasterController master)
    {
        this.master = master;
        this.batteryVal = 0;

        master.registerMotorController(this);

        /*
         * Put the motors into a known state.  This also forces an i2c transaction
         * on the controller which will take the legacy module out of analog state on
         * that port.  (Otherwise we don't get the port ready callback because the
         * device never clears the ready flag.)
         */
        for (byte i = 0; i < MAX_NUM_MOTORS; i++) {
            MatrixI2cTransaction transaction = new MatrixI2cTransaction(i, SPEED_STOPPED, NO_TARGET, CHANNEL_MODE_FLAG_SELECT_FLOAT);
            master.queueTransaction(transaction);
            motorCache[i].runMode = DcMotor.RunMode.RUN_WITHOUT_ENCODER;
            motorCache[i].floating = true;
            motorCache[i].power = 0;    // because we're floating
        }

        pendMotorPowerChanges = false;
    }

    protected byte runModeToFlagMatrix(DcMotor.RunMode mode)
    {
        switch (mode.migrate()) {
        case RUN_USING_ENCODER: // PID Control (Speed control) 0x02
            return CHANNEL_MODE_FLAG_SELECT_SPEED_CONTROL;
        case RUN_WITHOUT_ENCODER: // Power Control 0x01
            return CHANNEL_MODE_FLAG_SELECT_POWER_CONTROL;
        case RUN_TO_POSITION: // 0x03
            return CHANNEL_MODE_FLAG_SELECT_RTP_CONTROL;
        case STOP_AND_RESET_ENCODER:
            /*
             * Set the reset bit in the Mode byte.  This will also reset the power/position/target bytes.
             */
            return CHANNEL_MODE_FLAG_SELECT_RESET;
        }
        return CHANNEL_MODE_FLAG_SELECT_RESET;
    }

    protected DcMotor.RunMode flagMatrixToRunMode(byte flag)
    {
        switch (flag) {
        case CHANNEL_MODE_FLAG_SELECT_SPEED_CONTROL: // PID Control (Speed control) 0x02
            return DcMotor.RunMode.RUN_USING_ENCODER;
        case CHANNEL_MODE_FLAG_SELECT_POWER_CONTROL: // Power Control 0x01
            return DcMotor.RunMode.RUN_WITHOUT_ENCODER;
        case CHANNEL_MODE_FLAG_SELECT_RTP_CONTROL: // 0x03
            return DcMotor.RunMode.RUN_TO_POSITION;
        case CHANNEL_MODE_FLAG_SELECT_RESET:
            /*
             * Set the reset bit in the Mode byte.  This will also reset the power/position/target bytes.
             */
            return DcMotor.RunMode.STOP_AND_RESET_ENCODER;
        }

        RobotLog.e("Invalid run mode flag " + flag);
        return DcMotor.RunMode.RUN_WITHOUT_ENCODER;
    }

    public boolean isBusy(int motor)
    {
        MatrixI2cTransaction transaction = new MatrixI2cTransaction((byte)motor, MatrixI2cTransaction.I2cTransactionProperty.PROPERTY_MODE);
        master.queueTransaction(transaction);

        master.waitOnRead();

        if ((motorCache[transaction.motor].mode & 0x80) != 0) {
            return true;
        } else {
            return false;
        }
    }

    @Override public void resetDeviceConfigurationForOpMode(int motor)
    {
        throwIfMotorIsInvalid(motor);
        // nothing to do
    }

    @Override public synchronized void setMotorType(int motor, MotorConfigurationType motorType)
    {
        throwIfMotorIsInvalid(motor);
        motorCache[motor].motorType = motorType;
    }

    @Override public synchronized MotorConfigurationType getMotorType(int motor)
    {
        this.throwIfMotorIsInvalid(motor);
        return motorCache[motor].motorType;
    }


    /*
     * Careful with the reset at it resets the entire motor.
     *
     * Teams have to select a mode again after doing the reset.
     */
    @Override
    public void setMotorMode(int motor, DcMotor.RunMode mode)
    {
        mode=mode.migrate();
        throwIfMotorIsInvalid(motor);

        /*
         * If we are floating then always set the channel mode, otherwise
         * don't queue a transaction if we are already in this mode.
         */
        if ((!motorCache[motor].floating) && (motorCache[motor].runMode == mode)) {
            return;
        }

        byte flag = runModeToFlagMatrix(mode);

        DcMotor.RunMode prevMode = motorCache[motor].runMode;
        double prevPower = getMotorPower(motor);

        MatrixI2cTransaction transaction =
                new MatrixI2cTransaction((byte)motor, MatrixI2cTransaction.I2cTransactionProperty.PROPERTY_MODE, flag);
        master.queueTransaction(transaction);

        motorCache[motor].runMode = mode;

        if (mode.isPIDMode() && !prevMode.isPIDMode()) {
            setMotorPower(motor, prevPower);
        }

        setFloatingFromMode(motor);
    }

    void setFloatingFromMode(int motor)
    {
        if (motorCache[motor].runMode == DcMotor.RunMode.STOP_AND_RESET_ENCODER) {
            motorCache[motor].floating = true;
        } else {
            motorCache[motor].floating = false;
        }
    }

    @Override
    public DcMotor.RunMode getMotorMode(int motor)
    {
        throwIfMotorIsInvalid(motor);

        return motorCache[motor].runMode;
    }

    @Override
    public synchronized void setMotorZeroPowerBehavior(int motor, DcMotor.ZeroPowerBehavior zeroPowerBehavior)
    {
        throwIfMotorIsInvalid(motor);
        if (zeroPowerBehavior == DcMotor.ZeroPowerBehavior.UNKNOWN) throw new IllegalArgumentException("zeroPowerBehavior may not be UNKNOWN");

        motorCache[motor].zeroPowerBehavior = zeroPowerBehavior;

        // If we're currently stopped, then reissue power to cause new zero behavior to take effect
        if (motorCache[motor].power == 0) {
            setMotorPower(motor, motorCache[motor].power);
        }
    }

    @Override
    public synchronized DcMotor.ZeroPowerBehavior getMotorZeroPowerBehavior(int motor)
    {
        throwIfMotorIsInvalid(motor);
        return motorCache[motor].zeroPowerBehavior;
    }

    /*
     * Off/float on the Matrix controller is set differently than on the Hitechnic controller.
     * The mode byte controls motor float, unlike the Hitechnic controller where the motor
     * power setting of -127 (-0x80) puts the motor into float mode and stopped.
     *
     * Unfortunately the RunMode enumeration does not map directly onto the Matrix
     * controller's mode byte as there is no "FLOAT" property in the RunMode enumeration.
     */
    protected void setMotorPowerFloat(int motor)
    {
        throwIfMotorIsInvalid(motor);

        /*
         * Motor RESET causes the position, target, speed and mode fields to ALL be set to ZERO.
         *
         * This causes the motor to go into float mode, stopped.
         */
        if (!motorCache[motor].floating) {
            MatrixI2cTransaction transaction = new MatrixI2cTransaction((byte)motor, MatrixI2cTransaction.I2cTransactionProperty.PROPERTY_MODE, CHANNEL_MODE_FLAG_SELECT_RESET);
            master.queueTransaction(transaction);
        }

        /*
         * TODO: Find out from Steve if we need to wait for the reset bit clear after asserting it.
         */
        motorCache[motor].floating = true;
        motorCache[motor].power = 0;    // in the SDK API, being in float mode is also logically being in zero power
    }

    @Override
    public boolean getMotorPowerFloat(int motor)
    {
        throwIfMotorIsInvalid(motor);

        return motorCache[motor].floating;
    }

    /**
     * Sets the power for a group of motors.
     *
     * @param motors This provides an optimization specific to the Matrix controller
     *               by using the controller's pending bit to tell all of the motors
     *               to start at the same time.
     * @param power The motor power to apply to all motors in the set.
     */
    public synchronized void setMotorPower(Set<DcMotor> motors, double power)
    {
        pendMotorPowerChanges = true;
        try {
            for (DcMotor motor : motors) {
                motor.setPower(power);
            }

            /*
             * Write the start flag to start the motors.
             */
            MatrixI2cTransaction transaction = new MatrixI2cTransaction((byte)0, MatrixI2cTransaction.I2cTransactionProperty.PROPERTY_START, 0x01);
            master.queueTransaction(transaction);
        } finally {
            pendMotorPowerChanges = false;
        }
    }

    @Override
    public synchronized void setMotorPower(int motor, double power)
    {
        throwIfMotorIsInvalid(motor);
        power = Range.clip(power, apiPowerMin, apiPowerMax);

        if (motorCache[motor].zeroPowerBehavior==DcMotor.ZeroPowerBehavior.FLOAT && power==0.0) {

            setMotorPowerFloat(motor);

        } else {

            byte p = (byte)(power * POWER_MAX);
            byte bit = pendMotorPowerChanges ? MODE_PENDING_BIT : 0;

            MatrixI2cTransaction transaction = new MatrixI2cTransaction((byte)motor, p, motorCache[motor].target, (byte)(runModeToFlagMatrix(motorCache[motor].runMode) | bit));
            master.queueTransaction(transaction);

            setFloatingFromMode(motor);
            motorCache[motor].power = power;
        }
    }

    @Override
    public double getMotorPower(int motor)
    {
        throwIfMotorIsInvalid(motor);
        double power = motorCache[motor].power;
        return power;
    }

    @Override
    public void setMotorTargetPosition(int motor, int position)
    {
        throwIfMotorIsInvalid(motor);

        MatrixI2cTransaction transaction =
                new MatrixI2cTransaction((byte)motor, MatrixI2cTransaction.I2cTransactionProperty.PROPERTY_TARGET, position);
        master.queueTransaction(transaction);
        motorCache[motor].target = position;
    }

    @Override
    public int getMotorTargetPosition(int motor)
    {
        throwIfMotorIsInvalid(motor);

        MatrixI2cTransaction transaction =
                new MatrixI2cTransaction((byte)motor, MatrixI2cTransaction.I2cTransactionProperty.PROPERTY_TARGET);

        if (master.queueTransaction(transaction)) {
            master.waitOnRead();
        }

        return motorCache[motor].target;
    }

    public int getMotorCurrentPosition(int motor)
    {
        throwIfMotorIsInvalid(motor);

        MatrixI2cTransaction transaction =
                new MatrixI2cTransaction((byte)motor, MatrixI2cTransaction.I2cTransactionProperty.PROPERTY_POSITION);

        if (master.queueTransaction(transaction)) {
            master.waitOnRead();
        }

        return motorCache[motor].position;
    }

    public int getBattery()
    {
        MatrixI2cTransaction transaction =
                new MatrixI2cTransaction((byte)0, MatrixI2cTransaction.I2cTransactionProperty.PROPERTY_BATTERY);

        if (master.queueTransaction(transaction)) {
            master.waitOnRead();
        }

        return batteryVal;
    }

    @Override public Manufacturer getManufacturer()
    {
        return Manufacturer.Matrix;
    }

    @Override
    public String getDeviceName()
    {
        return AppUtil.getDefContext().getString(com.qualcomm.robotcore.R.string.displayNameMatrixMotorController);
    }

    @Override
    public String getConnectionInfo()
    {
        return master.getConnectionInfo();
    }

    @Override
    public int getVersion()
    {
        return 1;
    }

    void brakeAllAtZero()
    {
        for (int motor = 0; motor < MAX_NUM_MOTORS; motor++)
            {
            motorCache[motor].zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE;
            }
    }

    @Override
    public void resetDeviceConfigurationForOpMode()
    {
        // TODO: fully mirror other motor controller's reset behavior
        brakeAllAtZero();
    }

    @Override
    public void close()
    {
        setMotorPowerFloat(1);
        setMotorPowerFloat(2);
        setMotorPowerFloat(3);
        setMotorPowerFloat(4);
    }

    /*
     * I really want these to be protected friend functions of the master, but Java
     * alas does not have that feature.
     *
     * These should not be documented, and teams should not be calling them.
     */
    public void handleReadBattery(byte[] buffer)
    {
        batteryVal = BATTERY_UNITS * TypeConversion.unsignedByteToInt(buffer[I2C_DATA_OFFSET]);
        RobotLog.v("Battery voltage: " + batteryVal + "mV");
    }

    public void handleReadPosition(MatrixI2cTransaction transaction, byte[] buffer)
    {
        motorCache[transaction.motor].position = TypeConversion.byteArrayToInt(Arrays.copyOfRange(buffer, I2C_DATA_OFFSET, I2C_DATA_OFFSET + POSITION_DATA_SIZE));
        RobotLog.v("Position motor: " + transaction.motor + " " + motorCache[transaction.motor].position);
    }

    public void handleReadTargetPosition(MatrixI2cTransaction transaction, byte[] buffer)
    {
        motorCache[transaction.motor].target = TypeConversion.byteArrayToInt(Arrays.copyOfRange(buffer, I2C_DATA_OFFSET, I2C_DATA_OFFSET + TARGET_DATA_SIZE));
        RobotLog.v("Target motor: " + transaction.motor + " " + motorCache[transaction.motor].target);
    }

    public void handleReadMode(MatrixI2cTransaction transaction, byte[] buffer)
    {
        motorCache[transaction.motor].mode = buffer[I2C_DATA_OFFSET];
        RobotLog.v("Mode: " + motorCache[transaction.motor].mode);
    }

    private void throwIfMotorIsInvalid(int motor) {
        if (motor < 1 || motor > MAX_NUM_MOTORS) {
            throw new IllegalArgumentException (
                    String.format( "Motor %d is invalid; valid motors are %d..%d", motor, MatrixConstants.INITIAL_MOTOR_PORT, MAX_NUM_MOTORS));
        }
    }
}
