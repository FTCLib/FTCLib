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
package com.qualcomm.hardware.lynx;

import android.content.Context;
import android.support.annotation.Nullable;

import com.qualcomm.hardware.R;
import com.qualcomm.hardware.lynx.commands.LynxCommand;
import com.qualcomm.hardware.lynx.commands.core.LynxI2cReadMultipleBytesCommand;
import com.qualcomm.hardware.lynx.commands.core.LynxI2cReadSingleByteCommand;
import com.qualcomm.hardware.lynx.commands.core.LynxI2cReadStatusQueryCommand;
import com.qualcomm.hardware.lynx.commands.core.LynxI2cReadStatusQueryResponse;
import com.qualcomm.hardware.lynx.commands.core.LynxI2cWriteMultipleBytesCommand;
import com.qualcomm.hardware.lynx.commands.core.LynxI2cWriteReadMultipleBytesCommand;
import com.qualcomm.hardware.lynx.commands.core.LynxI2cWriteSingleByteCommand;
import com.qualcomm.hardware.lynx.commands.core.LynxI2cWriteStatusQueryCommand;
import com.qualcomm.hardware.lynx.commands.core.LynxI2cWriteStatusQueryResponse;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.I2cAddr;
import com.qualcomm.robotcore.hardware.I2cDeviceSynchReadHistory;
import com.qualcomm.robotcore.hardware.I2cDeviceSynchReadHistoryImpl;
import com.qualcomm.robotcore.hardware.I2cDeviceSynchSimple;
import com.qualcomm.robotcore.hardware.I2cWaitControl;
import com.qualcomm.robotcore.hardware.TimestampedData;
import com.qualcomm.robotcore.hardware.TimestampedI2cData;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.Util;

import java.util.concurrent.BlockingQueue;

/**
 * Created by bob on 2016-03-12.
 */
public abstract class LynxI2cDeviceSynch extends LynxController implements I2cDeviceSynchSimple, I2cDeviceSynchReadHistory
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "LynxI2cDeviceSynch";
    @Override protected String getTag() { return TAG; }

    protected I2cAddr   i2cAddr;
    protected int       bus;
    private boolean     loggingEnabled;
    private String      loggingTag;
    private String      name;
    private int         msBusyWait = 3;
    private final I2cDeviceSynchReadHistoryImpl readHistory = new I2cDeviceSynchReadHistoryImpl();

    protected LynxUsbUtil.Placeholder<TimestampedData> readTimeStampedPlaceholder = new LynxUsbUtil.Placeholder<TimestampedData>(TAG, "readTimestamped");
    private LynxUsbUtil.Placeholder<TimestampedData> readStatusQueryPlaceholder = new LynxUsbUtil.Placeholder<TimestampedData>(TAG, "readStatusQuery");

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    protected LynxI2cDeviceSynch(final Context context, final LynxModule module, int bus)
        {
        super(context, module);
        this.bus = bus;
        this.i2cAddr = I2cAddr.zero();
        this.loggingEnabled = false;
        this.loggingTag = TAG;
        this.finishConstruction();
        }

    //----------------------------------------------------------------------------------------------
    // HardwareDevice
    //----------------------------------------------------------------------------------------------

    @Override
    public String getDeviceName()
        {
        return this.context.getString(R.string.lynxI2cDeviceSynchDisplayName);
        }

    @Override
    public String getConnectionInfo()
        {
        return String.format("%s; bus %d; addr7=0x%02x", this.getModule().getConnectionInfo(), this.bus, this.i2cAddr.get7Bit());
        }

    @Override
    public void resetDeviceConfigurationForOpMode()
        {
        super.resetDeviceConfigurationForOpMode();
        readTimeStampedPlaceholder.reset();
        readStatusQueryPlaceholder.reset();
        }

    public void close()
        {
        setHealthStatus(HealthStatus.CLOSED);
        super.close();
        }

    //----------------------------------------------------------------------------------------------
    // I2cDeviceSynch administrative methods
    //----------------------------------------------------------------------------------------------

    @Override
    public boolean isArmed()
        {
        return super.isArmed();
        }

    @Override
    public void setI2cAddress(I2cAddr i2cAddr)
        {
        this.i2cAddr = i2cAddr;
        }

    @Override
    public void setI2cAddr(I2cAddr i2cAddr)
        {
        this.i2cAddr = i2cAddr;
        }

    @Override
    public I2cAddr getI2cAddress()
        {
        return this.i2cAddr;
        }

    @Override
    public I2cAddr getI2cAddr()
        {
        return this.i2cAddr;
        }

    @Override
    public void setUserConfiguredName(@Nullable String name)
        {
        this.name = name;
        }

    @Override
    @Nullable public String getUserConfiguredName()
        {
        return this.name;
        }

    @Override
    public void setLogging(boolean enabled)
        {
        this.loggingEnabled = enabled;
        }

    @Override public boolean getLogging()
        {
        return this.loggingEnabled;
        }

    @Override
    public void setLoggingTag(String loggingTag)
        {
        this.loggingTag = loggingTag;
        }

    @Override public String getLoggingTag()
        {
        return this.loggingTag;
        }

    //----------------------------------------------------------------------------------------------
    // I2cDeviceSynchReadHistory API methods
    //----------------------------------------------------------------------------------------------

    @Override public void setHistoryQueueCapacity(int capacity)
        {
        readHistory.setHistoryQueueCapacity(capacity);
        }

    @Override public int getHistoryQueueCapacity()
        {
        return readHistory.getHistoryQueueCapacity();
        }

    @Override public BlockingQueue<TimestampedI2cData> getHistoryQueue()
        {
        return readHistory.getHistoryQueue();
        }

    //----------------------------------------------------------------------------------------------
    // I2cDeviceSynch API methods
    //----------------------------------------------------------------------------------------------

    @Override
    public  byte[] read(int ireg, int creg)
        {
        return this.readTimeStamped(ireg, creg).data;
        }

    @Override
    public synchronized byte read8(final int ireg)
        {
        final LynxI2cWriteSingleByteCommand writeTx = new LynxI2cWriteSingleByteCommand(this.getModule(), this.bus, this.i2cAddr, ireg);
        try {
            return acquireI2cLockWhile(new Supplier<Byte>()
                {
                @Override public Byte get() throws InterruptedException, RobotCoreException, LynxNackException
                    {
                    sendI2cWriteTx(writeTx);

                    LynxI2cReadSingleByteCommand readTx = new LynxI2cReadSingleByteCommand(getModule(), bus, i2cAddr);
                    readTx.send();

                    return pollForReadResult(i2cAddr, ireg, 1).data[0];
                    }
                });
            }
        catch (InterruptedException|LynxNackException|RobotCoreException|RuntimeException e)
            {
            handleException(e);
            }
        return LynxUsbUtil.makePlaceholderValue((byte)0);
        }

    @Override
    public abstract TimestampedData readTimeStamped(final int ireg, final int creg);

    //-------- writing

    @Override
    public void write(int ireg, byte[] data)
        {
        internalWrite(ireg, data, I2cWaitControl.ATOMIC);
        }

    @Override
    public synchronized void write8(int ireg, int bVal)
        {
        internalWrite(ireg, new byte[] {(byte)bVal}, I2cWaitControl.ATOMIC);
        }

    @Override
    public synchronized void write8(int ireg, int bVal, I2cWaitControl waitControl)
        {
        internalWrite(ireg, new byte[] {(byte)bVal}, waitControl);
        }

    @Override
    public synchronized void write(int ireg, byte[] data, I2cWaitControl waitControl)
        {
        internalWrite(ireg, data, waitControl);
        }

    private void internalWrite(int ireg, byte[] data, final I2cWaitControl waitControl)
        {
        if (data.length > 0) // paranoia, but safe
            {
            // For register-based I2c devices: convention: first byte in a write is the initial register number
            byte[] payload = Util.concatenateByteArrays(new byte[] {(byte)ireg}, data);

            // We use the single-byte case when we can out of paranoia about the LynxI2cWriteMultipleBytesCommand
            // not being able to handle a byte count of one (that has not been verified with the firmware
            // programmers, but the corresponding read case has been)
            final LynxCommand<?> writeTx = payload.length==1
                    ? new LynxI2cWriteSingleByteCommand(this.getModule(), this.bus, this.i2cAddr, payload[0])
                    : new LynxI2cWriteMultipleBytesCommand(this.getModule(), this.bus, this.i2cAddr, payload);
            try {
                acquireI2cLockWhile(new Supplier<Object>()
                    {
                    @Override public Object get() throws InterruptedException, RobotCoreException, LynxNackException
                        {
                        sendI2cWriteTx(writeTx);
                        internalWaitForWriteCompletions(waitControl);
                        return null;
                        }
                    });
                }
            catch (InterruptedException|LynxNackException|RobotCoreException|RuntimeException e)
                {
                handleException(e);
                }
            }
        }

    @Override
    public synchronized void waitForWriteCompletions(final I2cWaitControl waitControl)
        {
        try {
            acquireI2cLockWhile(new Supplier<Object>()
                {
                @Override public Object get() throws InterruptedException, RobotCoreException, LynxNackException
                    {
                    internalWaitForWriteCompletions(waitControl);
                    return null;
                    }
                });
            }
        catch (InterruptedException|LynxNackException|RobotCoreException|RuntimeException e)
            {
            handleException(e);
            }
        }

    @Override
    public void enableWriteCoalescing(boolean enable)
        {
        // nothing to do
        }

    @Override
    public boolean isWriteCoalescingEnabled()
        {
        return false;
        }

    //----------------------------------------------------------------------------------------------
    // I2cDeviceSynch API support methods
    //----------------------------------------------------------------------------------------------

    protected void sendI2cWriteTx(LynxCommand writeTx) throws LynxNackException, InterruptedException
        {
        for (;;)
            {
            try {
                writeTx.send();
                break;
                }
            catch (LynxNackException e)
                {
                switch (e.getNack().getNackReasonCode())
                    {
                    case I2C_MASTER_BUSY:
                    case I2C_OPERATION_IN_PROGRESS:
                        Thread.sleep(msBusyWait);
                        break;
                    default:
                        throw e;
                    }
                }
            }
        }

    protected <T> T acquireI2cLockWhile(Supplier<T> supplier) throws InterruptedException, RobotCoreException, LynxNackException
        {
        return this.getModule().acquireI2cLockWhile(supplier);
        }

    protected void internalWaitForWriteCompletions(I2cWaitControl waitControl)
        {
        /** Note: called with i2c lock held!
         *
         * For {@link I2cWaitControl#NONE} and {@link I2cWaitControl#ATOMIC}, we have nothing to do
         * because we transmit synchronously to USB in the original write call. For
         * {@link I2cWaitControl#WRITTEN}, we have work to do.
         */
        if (waitControl == I2cWaitControl.WRITTEN)
            {
            boolean keepTrying = true;
            while (keepTrying)
                {
                final LynxI2cWriteStatusQueryCommand writeStatus = new LynxI2cWriteStatusQueryCommand(this.getModule(), this.bus);
                try {
                    LynxI2cWriteStatusQueryResponse response = writeStatus.sendReceive();
                    if (response.isStatusOk())
                        {
                        setHealthyIfArmed();
                        }
                    }
                catch (LynxNackException e)
                    {
                    switch (e.getNack().getNackReasonCode())
                        {
                        case I2C_NO_RESULTS_PENDING:
                            return;
                        case I2C_OPERATION_IN_PROGRESS:
                            try { Thread.sleep(msBusyWait); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                            continue;
                        default:
                            handleException(e);
                            keepTrying = false;
                            break;
                        }
                    }
                catch (InterruptedException|RuntimeException e)
                    {
                    handleException(e);
                    keepTrying = false;
                    }
                }
            }
        }

    protected TimestampedData pollForReadResult(I2cAddr i2cAddr, int ireg, int creg)
        {
        // Poll until the data is available
        boolean keepTrying = true;
        LynxI2cDeviceSynch deviceHavingProblems = null;

        while (keepTrying)
            {
            LynxI2cReadStatusQueryCommand readStatus = new LynxI2cReadStatusQueryCommand(this.getModule(), this.bus, creg);
            try {
                LynxI2cReadStatusQueryResponse response = readStatus.sendReceive();
                long now = System.nanoTime();
                response.logResponse();
                //
                TimestampedI2cData result = new TimestampedI2cData();
                result.data = response.getBytes();
                result.nanoTime = response.getPayloadTimeWindow().isCleared() ? now : response.getPayloadTimeWindow().getNanosecondsLast();
                result.i2cAddr = i2cAddr;
                result.register = ireg;

                // Return real data if we've got it
                if (result.data.length == creg)
                    {
                    readStatusQueryPlaceholder.reset();
                    readHistory.addToHistoryQueue(result);
                    setHealthyIfArmed();
                    return result;
                    }

                // Log the error and return placeholder data if we don't
                RobotLog.ee(loggingTag, "readStatusQuery: cbExpected=%d cbRead=%d", creg, result.data.length);
                deviceHavingProblems = this;
                keepTrying = false;
                }
            catch (LynxNackException e)
                {
                switch (e.getNack().getNackReasonCode())
                    {
                    case I2C_MASTER_BUSY:               // TODO: REVIEW: is this ever actually returned in this situation?
                    case I2C_OPERATION_IN_PROGRESS:
                        try { Thread.sleep(msBusyWait); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                        continue;
                    case I2C_NO_RESULTS_PENDING:
                        // This is an internal error of some sort
                        handleException(e);
                        keepTrying = false;
                        deviceHavingProblems = this;
                        break;
                    default:
                        handleException(e);
                        keepTrying = false;
                        deviceHavingProblems = this;
                        break;
                    }
                }
            catch (InterruptedException|RuntimeException e)
                {
                handleException(e);
                keepTrying = false;
                }
            }
        return readStatusQueryPlaceholder.log(TimestampedI2cData.makeFakeData(deviceHavingProblems, i2cAddr, ireg, creg));
        }

    }
