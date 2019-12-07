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

import com.qualcomm.robotcore.hardware.ServoController;
import com.qualcomm.robotcore.hardware.configuration.MatrixConstants;
import com.qualcomm.robotcore.util.Range;
import com.qualcomm.robotcore.util.TypeConversion;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.util.Arrays;

/*
 * Modern Robotics Matrix Servo Controller
 */
public class MatrixServoController implements ServoController {

    public  final static int  SERVO_POSITION_MAX = 0xf0; // some servos go to max power at values near 0xff

    private final static byte MAX_SERVOS         = MatrixConstants.NUMBER_OF_SERVOS;
    private final static byte SERVO_ENABLE_ALL   = 0x0F;
    private final static byte SERVO_DISABLE_ALL  = 0x00;
    private final static byte I2C_DATA_OFFSET    = 0x04;

    private MatrixMasterController master;

    protected PwmStatus pwmStatus;
    protected double[] servoCache = new double[MAX_SERVOS];

    public MatrixServoController(MatrixMasterController master)
    {
        this.master = master;
        this.pwmStatus = PwmStatus.DISABLED;
        Arrays.fill(servoCache, 0.0);

        master.registerServoController(this);
    }

    @Override
    public void pwmEnable()
    {
        MatrixI2cTransaction transaction = new MatrixI2cTransaction((byte)0, MatrixI2cTransaction.I2cTransactionProperty.PROPERTY_SERVO_ENABLE, SERVO_ENABLE_ALL);
        master.queueTransaction(transaction);
        pwmStatus = PwmStatus.ENABLED;
    }

    @Override
    public void pwmDisable()
    {
        MatrixI2cTransaction transaction = new MatrixI2cTransaction((byte)0, MatrixI2cTransaction.I2cTransactionProperty.PROPERTY_SERVO_ENABLE, SERVO_DISABLE_ALL);
        master.queueTransaction(transaction);
        pwmStatus = PwmStatus.DISABLED;
    }

    @Override
    public PwmStatus getPwmStatus()
    {
        return pwmStatus;
    }

    /*
     * Sets a servo position at the maximum servo change rate.
     */
    @Override
    public void setServoPosition(int channel, double position)
    {
        throwIfChannelIsInvalid(channel);
        Range.throwIfRangeIsInvalid(position, 0.0, 1.0);

        byte newPosition = (byte)(position * SERVO_POSITION_MAX);

        MatrixI2cTransaction transaction = new MatrixI2cTransaction((byte)channel, newPosition, (byte)0);
        master.queueTransaction(transaction);
    }

    /*
     * Set a position and a speed for the servo to move.  This is equivalent to
     * the old servoChangeRate variable in the dear old departed RobotC.
     *
     * A speed of 0 implies maximum, otherwise changes occur at a rate
     * of 10*speed milliseconds per step.
     */
    public void setServoPosition(int channel, double position, byte speed)
    {
        throwIfChannelIsInvalid(channel);
        Range.throwIfRangeIsInvalid(position, 0.0, 1.0);

        byte newPosition = (byte)(position * SERVO_POSITION_MAX);

        MatrixI2cTransaction transaction = new MatrixI2cTransaction((byte)channel, newPosition, speed);
        master.queueTransaction(transaction);
    }

    @Override
    public double getServoPosition(int channel)
    {
        MatrixI2cTransaction transaction = new MatrixI2cTransaction((byte)channel, MatrixI2cTransaction.I2cTransactionProperty.PROPERTY_SERVO);

        if (master.queueTransaction(transaction)) {
            master.waitOnRead();
        }

        return ((double)servoCache[channel] / SERVO_POSITION_MAX);
    }

    @Override public Manufacturer getManufacturer()
    {
        return Manufacturer.Matrix;
    }

    @Override
    public String getDeviceName()
    {
        return AppUtil.getDefContext().getString(com.qualcomm.robotcore.R.string.displayNameMatrixServoController);
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

    @Override
    public void resetDeviceConfigurationForOpMode()
    {
        pwmDisable();
    }

    @Override
    public void close()
    {
        pwmDisable();
    }

    private void throwIfChannelIsInvalid(int channel) {
        if (channel < 1 || channel > MAX_SERVOS) {
            throw new IllegalArgumentException(
                    String.format("Channel %d is invalid; valid channels are %d..%d", channel, MatrixConstants.INITIAL_SERVO_PORT, MAX_SERVOS));
        }
    }

    /*
     * I really want these to be protected friend functions of the master, but Java
     * alas does not have that feature.
     *
     * These should not be documented, and teams should not be calling them.
     */
    public void handleReadServo(MatrixI2cTransaction transaction, byte[] buffer)
    {
        servoCache[transaction.servo] = TypeConversion.unsignedByteToInt(buffer[I2C_DATA_OFFSET]);
    }

}
