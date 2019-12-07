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
package com.qualcomm.robotcore.hardware;

import org.firstinspires.ftc.robotcore.internal.collections.EvictingBlockingQueue;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * {@link I2cDeviceSynchReadHistoryImpl} is a helper class providing an implementation
 * of the I2c read history queue
 */
@SuppressWarnings("WeakerAccess")
public class I2cDeviceSynchReadHistoryImpl implements I2cDeviceSynchReadHistory
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected final Object                       historyQueueLock = new Object();
    protected BlockingQueue<TimestampedI2cData>  historyQueue;
    protected int                                historyQueueCapacity;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public I2cDeviceSynchReadHistoryImpl()
        {
        setHistoryQueueCapacity(0);
        }

    //----------------------------------------------------------------------------------------------
    // I2cDeviceSyncReadHistory implementation
    //----------------------------------------------------------------------------------------------

    @Override public BlockingQueue<TimestampedI2cData> getHistoryQueue()
        {
        synchronized (this.historyQueueLock)
            {
            return historyQueue;
            }
        }

    @Override public void setHistoryQueueCapacity(int capacity)
        {
        synchronized (this.historyQueueLock)
            {
            this.historyQueueCapacity = Math.max(0, capacity);
            if (capacity <= 0)
                {
                this.historyQueue = new ArrayBlockingQueue<TimestampedI2cData>(1); // dummy, never actually written to
                }
            else
                {
                this.historyQueue = new EvictingBlockingQueue<TimestampedI2cData>(new ArrayBlockingQueue<TimestampedI2cData>(capacity));
                }
            }
        }

    @Override public int getHistoryQueueCapacity()
        {
        synchronized (this.historyQueueLock)
            {
            return this.historyQueueCapacity;
            }
        }

    //----------------------------------------------------------------------------------------------
    // Queue maintenance
    //----------------------------------------------------------------------------------------------

    public void addToHistoryQueue(TimestampedI2cData data)
        {
        synchronized (historyQueueLock)
            {
            if (historyQueueCapacity > 0)
                {
                historyQueue.add(data);
                }
            }
        }
    }
