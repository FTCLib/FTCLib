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

import com.qualcomm.hardware.modernrobotics.ModernRoboticsUsbLegacyModule;
import com.qualcomm.robotcore.hardware.I2cController;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.TypeConversion;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

/*
 * The master is the only class that is allowed to touch the legacyModule port
 * that a Matrix controller sits upon.
 *
 * Yes master, the rules shall be followed.
 */
public class MatrixMasterController implements I2cController.I2cPortReadyCallback {

    private final static byte WASTED_BYTE                = 0x00;
    private final static byte MATRIX_CONTROLLER_I2C_ADDR = 0x10;
    private final static byte TIMEOUT_OFFSET             = 0x42;
    private final static byte BATTERY_OFFSET             = 0x43;
    private final static byte START_FLAG_OFFSET          = 0x44;
    private final static byte SERVO_ENABLE_OFFSET        = 0x45;

    private final static byte[] servoSpeedOffset    = { WASTED_BYTE, 0x46, 0x48, 0x4A, 0x4C };
    private final static byte[] motorPositionOffset = { WASTED_BYTE, 0x4E, 0x58, 0x62, 0x6C };
    private final static byte[] motorTargetOffset   = { WASTED_BYTE, 0x52, 0x5C, 0x66, 0x70 };
    private final static byte[] motorSpeedOffset    = { WASTED_BYTE, 0x56, 0x60, 0x6A, 0x74 };
    private final static byte[] motorModeOffset     = { WASTED_BYTE, 0x57, 0x61, 0x6B, 0x75 };

    protected ConcurrentLinkedQueue<MatrixI2cTransaction> transactionQueue;
    protected ModernRoboticsUsbLegacyModule legacyModule;
    protected MatrixDcMotorController motorController;
    protected MatrixServoController servoController;
    protected int physicalPort;
    private volatile boolean waitingForGodot = false;
    private final static boolean debug = false;

    private final ElapsedTime lastTransaction = new ElapsedTime(0);
    private static final double MIN_TRANSACTION_RATE = 2.0; // in seconds;
    private static final int DEFAULT_TIMEOUT         = 3;   // in seconds;

    public MatrixMasterController(ModernRoboticsUsbLegacyModule legacyModule, int physicalPort)
    {
        this.legacyModule = legacyModule;
        this.physicalPort = physicalPort;

        transactionQueue = new ConcurrentLinkedQueue<MatrixI2cTransaction>();

        legacyModule.registerForI2cPortReadyCallback(this, physicalPort);
    }

    public void registerMotorController(MatrixDcMotorController mc)
    {
        this.motorController = mc;
    }

    public void registerServoController(MatrixServoController sc)
    {
        this.servoController = sc;
    }

    public int getPort()
    {
        return physicalPort;
    }

    public String getConnectionInfo()
    {
        return legacyModule.getConnectionInfo() + "; port " + physicalPort;
    }

    public boolean queueTransaction(MatrixI2cTransaction transaction, boolean force)
    {
        /*
         * Yes, inefficient, but if the queue is more than a few transactions
         * deep we have other problems.  The force parameter allows a controller
         * to queue a transaction regardless of whether or not a matching
         * transaction is already queued.
         */
        if (!force) {
            Iterator<MatrixI2cTransaction> it = transactionQueue.iterator();
            while (it.hasNext()) {
                MatrixI2cTransaction t = (MatrixI2cTransaction)it.next();
                if (t.isEqual(transaction)) {
                    buginf("NO Queue transaction " + transaction.toString());
                    return false;
                }
            }
            /*
             * One might ask if we have a property match, but a value mismatch, why
             * not replace the new value with the old?  That would result in transaction
             * reordering which might not be desirable.  Something to think on.
             */
        }

        /*
         * Doesn't exist, plop it in.
         */
        buginf("YES Queue transaction " + transaction.toString());
        transactionQueue.add(transaction);
        return true;
    }

    public boolean queueTransaction(MatrixI2cTransaction transaction)
    {
        return queueTransaction(transaction, false);
    }

    public void waitOnRead()
    {
        synchronized(this) {
            waitingForGodot = true;
            try {
                while (waitingForGodot) {
                    this.wait(0);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    protected void handleReadDone(MatrixI2cTransaction transaction)
    {
        byte[] readBuffer;
        readBuffer = legacyModule.getI2cReadCache(physicalPort);

        switch (transaction.property) {
        case PROPERTY_BATTERY:
            motorController.handleReadBattery(readBuffer);
            break;
        case PROPERTY_POSITION:
            motorController.handleReadPosition(transaction, readBuffer);
            break;
        case PROPERTY_TARGET:
            motorController.handleReadPosition(transaction, readBuffer);
            break;
        case PROPERTY_MODE:
            motorController.handleReadMode(transaction, readBuffer);
            break;
        case PROPERTY_SERVO:
            servoController.handleReadServo(transaction, readBuffer);
            break;
        default:
            RobotLog.e("Transaction not a read " + transaction.property);
            break;
        }
        synchronized (this) {
            if (waitingForGodot) {
                waitingForGodot = false;
                this.notify();
            }
        }
    }

    protected void sendHeartbeat()
    {
        /*
         * Any transaction suffices for a heartbeat, so
         * we'll just continually send the timeout value
         */
        MatrixI2cTransaction transaction
                = new MatrixI2cTransaction((byte)0,  MatrixI2cTransaction.I2cTransactionProperty.PROPERTY_TIMEOUT, DEFAULT_TIMEOUT);
        queueTransaction(transaction);
    }

    public void portIsReady(int port)
    {
        byte[] buffer;
        byte offset;
        byte len;

        if (transactionQueue.isEmpty()) {
            if (lastTransaction.time() > MIN_TRANSACTION_RATE) {
                sendHeartbeat();
                lastTransaction.reset();
            }
            return;
        }

        MatrixI2cTransaction transaction = transactionQueue.peek();

        /*
         * If the transaction is in the PENDING_I2C state then if this is a read
         * go fetch the result (and wait for another round trip to this function.
         *
         * If it's a write, we are done, pull it off the transaction queue.
         *
         * Process the next transaction if the queue is not empty.
         */
        if (transaction.state == MatrixI2cTransaction.I2cTransactionState.PENDING_I2C_READ) {
            /*
             * Go do a usb read, and then come back here.
             */
            legacyModule.readI2cCacheFromModule(physicalPort);
            transaction.state = MatrixI2cTransaction.I2cTransactionState.PENDING_READ_DONE;
            return;
        } else if (transaction.state == MatrixI2cTransaction.I2cTransactionState.PENDING_I2C_WRITE) {
            /*
             * It was a write, dequeue it and see if we have anything else.
             */
            transaction = transactionQueue.poll();
            /*
             * Now are we empty?  If so we are done.
             */
            if (transactionQueue.isEmpty()) {
                return;
            }
            /*
             * Not done, grab the next transaction.
             */
            transaction = transactionQueue.peek();
        } else if (transaction.state == MatrixI2cTransaction.I2cTransactionState.PENDING_READ_DONE) {
            /*
             * The read is done, pull our data out of the buffer.
             */
            handleReadDone(transaction);

            transaction = transactionQueue.poll();
            if (transactionQueue.isEmpty()) {
                return;
            }
            transaction = transactionQueue.peek();
        }

        switch (transaction.property) {
        case PROPERTY_POSITION:
            offset = motorPositionOffset[transaction.motor];
            len = 4;
            /*
             * Unused for reads, but Android Studio complains so...
             */
            buffer = new byte[1];
            buffer[0] = 0x0;
            break;
        case PROPERTY_BATTERY:
            offset = BATTERY_OFFSET;
            buffer = new byte[1];
            buffer[0] = 0x0;
            len = 1;
            break;
        case PROPERTY_TIMEOUT:
            offset = TIMEOUT_OFFSET;
            buffer = new byte[1];
            buffer[0] = (byte)transaction.value;
            len = 1;
            break;
        case PROPERTY_START:
            offset = START_FLAG_OFFSET;
            buffer = new byte[1];
            buffer[0] = (byte)transaction.value;
            len = 1;
            break;
        case PROPERTY_SPEED:
            offset = motorSpeedOffset[transaction.motor];
            buffer = new byte[1];
            buffer[0] = (byte)transaction.value;
            len = 1;
            break;
        case PROPERTY_TARGET:
            offset = motorTargetOffset[transaction.motor];
            buffer = TypeConversion.intToByteArray(transaction.value);
            len = 4;
            break;
        case PROPERTY_MODE:
            offset = motorModeOffset[transaction.motor];
            buffer = new byte[1];
            buffer[0] = (byte)transaction.value;
            len = 1;
            break;
        case PROPERTY_MOTOR_BATCH:
            offset = motorPositionOffset[transaction.motor];
            ByteBuffer bb = ByteBuffer.allocate(10);
            /*
             * TODO: Do we really need to write position? (Probably not)
             */
            bb.put(TypeConversion.intToByteArray(0));
            bb.put(TypeConversion.intToByteArray(transaction.target));
            bb.put(transaction.speed);
            bb.put(transaction.mode);
            buffer = bb.array();
            len = 10;
            break;
        case PROPERTY_SERVO:
            offset = servoSpeedOffset[transaction.servo];
            buffer = new byte[2];
            buffer[0] = transaction.speed;
            buffer[1] = (byte)transaction.target;
            len = 2;
            break;
        case PROPERTY_SERVO_ENABLE:
            offset = SERVO_ENABLE_OFFSET;
            buffer = new byte[1];
            buffer[0] = (byte)transaction.value;
            len = 1;
            break;
        default:
            offset = 0x00;
            buffer = new byte[1];
            buffer[0] = (byte)transaction.value;
            len = 1;
        }

        try {
            /*
             * Do the I2C transaction.
             */
            if (transaction.write) {
                legacyModule.setWriteMode(physicalPort, MATRIX_CONTROLLER_I2C_ADDR, offset);
                legacyModule.setData(physicalPort, buffer, len);
                transaction.state = MatrixI2cTransaction.I2cTransactionState.PENDING_I2C_WRITE;
            } else {
                legacyModule.setReadMode(physicalPort, MATRIX_CONTROLLER_I2C_ADDR, offset, len);
                transaction.state = MatrixI2cTransaction.I2cTransactionState.PENDING_I2C_READ;
            }
            legacyModule.setI2cPortActionFlag(physicalPort);
            legacyModule.writeI2cCacheToModule(physicalPort);
        } catch (IllegalArgumentException e) {
            RobotLog.e(e.getMessage());
        }
        buginf(transaction.toString());
    }

    /*
     * A convenience function for turning off/on local debugs.
     */
    protected void buginf(String s)
    {
        if (debug) {
            RobotLog.i(s);
        }
    }

}
