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

import java.util.concurrent.BlockingQueue;

/**
 * {@link I2cDeviceSynchReadHistory} provides a means by which one can be guaranteed
 * to be informed of all data read by through an {@link I2cDeviceSynch}. This is
 * provided by means of a queue into which all data retrieved is (optionally) be stored.
 * 
 * <p>This functionality can be useful in I2c devices in which the mere act of reading data causes
 * state transitions in the actual device itself. For such devices, the history queue can (e.g.)
 * assist the software driver layer for the device in reliably tracking the device state.</p>
 *
 * <p>Implementations of {@link I2cDeviceSynchReadHistory} are commonly retrieved by casting from
 * an implementation of {@link I2cDeviceSynch} or {@link I2cDeviceSynchSimple} (but don't forget
 * to first test using {@code instanceOf}).</p>
 */
@SuppressWarnings("WeakerAccess")
public interface I2cDeviceSynchReadHistory
    {
    /**
     * Sets the maximum number of {@link TimestampedI2cData}s that will simultaneously be stored in the
     * history queue. If the queue is full and new {@link TimestampedI2cData}s become available, older
     * data will be discarded. The history queue initially has a capacity of zero.
     *
     * <p>Note that calling this method invalidates any history queue retrieved previously
     * through {@link #getHistoryQueue()}.</p>
     *
     * @param capacity the maximum number of items that may be stored in the history queue
     * @see #getHistoryQueue()
     * @see #getHistoryQueueCapacity()
     */
    void setHistoryQueueCapacity(int capacity);

    /**
     * Returns the current capacity of the history queue.
     * @return the current capacity of the history queue.
     * @see #setHistoryQueueCapacity(int)
     * @see #getHistoryQueue()
     */
    int getHistoryQueueCapacity();

    /**
     * (Advanced) Returns a queue into which, if requested, {@link TimestampedI2cData}s are (optionally)
     * placed as they become available.
     *
     * <p>To access these {@link TimestampedI2cData}s, call {@link #setHistoryQueueCapacity(int)} to enable
     * the history queue. Once enabled, the history queue can be accessed using {@link #getHistoryQueue()}
     * and the methods thereon used to access {@link TimestampedI2cData}s as they become available.</p>
     *
     * <p>When {@link #setHistoryQueueCapacity(int)} is called, any history queue returned previously by
     * {@link #getHistoryQueue()} becomes invalid and must be re-fetched.</p>
     *
     * @return a queue through which I2c {@link TimestampedI2cData}s may be retrieved.
     *
     * @see #setHistoryQueueCapacity(int)
     */
    BlockingQueue<TimestampedI2cData> getHistoryQueue();
    }
