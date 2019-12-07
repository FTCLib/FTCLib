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

import com.qualcomm.robotcore.util.RobotLog;

public class MatrixI2cTransaction {

    enum I2cTransactionState {
        QUEUED,
        PENDING_I2C_READ,
        PENDING_I2C_WRITE,
        PENDING_READ_DONE,
        DONE
    }

    /*
     * The property to read/write.
     */
    enum I2cTransactionProperty {
        PROPERTY_MODE,
        PROPERTY_TARGET,
        PROPERTY_SPEED,
        PROPERTY_BATTERY,
        PROPERTY_POSITION,
        PROPERTY_MOTOR_BATCH,
        PROPERTY_SERVO,
        PROPERTY_SERVO_ENABLE,
        PROPERTY_START,
        PROPERTY_TIMEOUT
    }

    public byte motor;
    public byte servo;
    public I2cTransactionProperty property;
    public int value;
    public boolean write;

    /*
     * For the batched property
     */
    public byte speed;
    public int target;
    public byte mode;

    public I2cTransactionState state;

    /*
     * Generic read
     */
    MatrixI2cTransaction(byte motor, I2cTransactionProperty property)
    {
        this.motor = motor;
        this.property = property;
        this.state = I2cTransactionState.QUEUED;
        this.write = false;
    }

    /*
     * Generic write.
     */
    MatrixI2cTransaction(byte motor, I2cTransactionProperty property, int value)
    {
        this.motor = motor;
        this.value = value;
        this.property = property;
        this.state = I2cTransactionState.QUEUED;
        this.write = true;
    }

    /*
     * Batched motor write
     */
    MatrixI2cTransaction(byte motor, byte speed, int target, byte mode)
    {
        this.motor = motor;
        this.speed = speed;
        this.target = target;
        this.mode = mode;
        this.property = I2cTransactionProperty.PROPERTY_MOTOR_BATCH;
        this.state = I2cTransactionState.QUEUED;
        this.write = true;
    }

    /*
     * Servo write
     */
    MatrixI2cTransaction(byte servo, byte target, byte speed)
    {
        this.servo = servo;
        this.speed = speed;
        this.target = target;
        this.property = I2cTransactionProperty.PROPERTY_SERVO;
        this.state = I2cTransactionState.QUEUED;
        this.write = true;
    }

    public boolean isEqual(MatrixI2cTransaction transaction)
    {
        if (this.property != transaction.property) {
            return false;
        } else {
            switch (this.property) {
            case PROPERTY_MODE:
            case PROPERTY_START:
            case PROPERTY_TIMEOUT:
            case PROPERTY_TARGET:
            case PROPERTY_SPEED:
            case PROPERTY_BATTERY:
            case PROPERTY_POSITION:
                return ((this.write == transaction.write) && (this.motor == transaction.motor) && (this.value == transaction.value));
            case PROPERTY_MOTOR_BATCH:
                return ((this.write == transaction.write) && (this.motor == transaction.motor) && (this.speed == transaction.speed) && (this.target == transaction.target) && (this.mode == transaction.mode));
            case PROPERTY_SERVO:
                return ((this.write == transaction.write) && (this.servo == transaction.servo) && (this.speed == transaction.speed) && (this.target == transaction.target));
            case PROPERTY_SERVO_ENABLE:
                return ((this.write == transaction.write) && (this.value == transaction.value));
            default:
                RobotLog.e("Can not compare against unknown transaction property " + transaction.toString());
                return false;
            }
        }
    }

    @Override
    public String toString() {
        if (property == I2cTransactionProperty.PROPERTY_MOTOR_BATCH) {
            return "Matrix motor transaction: " + property + " motor " + motor + " write " + write + " speed " + speed + " target " + target + " mode " + mode;
        } else if (property == I2cTransactionProperty.PROPERTY_SERVO) {
            return "Matrix servo transaction: " + property + " servo " + servo + " write " + write + " change rate " + speed + " target " + (int)target;
        } else if (property == I2cTransactionProperty.PROPERTY_SERVO_ENABLE) {
            return "Matrix servo transaction: " + property + " servo " + servo + " write " + write + " value " + value;
        } else {
            return "Matrix motor transaction: " + property + " motor " + motor + " write " + write + " value " + value;
        }
    }
}
