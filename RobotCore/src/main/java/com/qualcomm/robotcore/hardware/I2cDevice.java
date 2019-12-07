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

import org.firstinspires.ftc.robotcore.internal.hardware.TimeWindow;

import java.util.concurrent.locks.Lock;

/**
 * The {@link I2cDevice} interface abstracts the engine used to interact on with a specific I2c device
 * on a port of an {@link I2cController}.
 *
 * @see I2cDeviceSynch
 */
public interface I2cDevice extends I2cControllerPortDevice, HardwareDevice
    {
    //----------------------------------------------------------------------------------------------
    // Mode management
    //----------------------------------------------------------------------------------------------

    /**
     * Enable read mode for this I2C device. This simply sets bytes in the header of the write-cache;
     * it does not enqueue or transmit any data.
     *
     * @param i2cAddr   the address of the device on the I2c bus which should be read
     * @param register      I2c register number within the device at which to start reading
     * @param count         number of bytes to read
     * @see #writeI2cCacheToController()
     */
    void enableI2cReadMode(I2cAddr i2cAddr, int register, int count);

    /**
     * Enable write mode for this I2C device. This simply sets bytes in the header of the write-cache;
     * it does not enqueue or transmit any data.
     *
     * @param i2cAddr   the address of the device on the I2c bus which should be written
     * @param register      mem address at which to start writing
     * @param count         number of bytes to write
     * @see #writeI2cCacheToController()
     */
    void enableI2cWriteMode(I2cAddr i2cAddr, int register, int count);

    /**
    * Queries whether or not the controller has reported that it is in read mode.
    * @return whether or not this port is in read mode
    */
    boolean isI2cPortInReadMode();

    /**
    * Queries whether or not the controller has reported that it is in write mode.
    * @return whether or not this port is in write mode
    */
    boolean isI2cPortInWriteMode();

    //----------------------------------------------------------------------------------------------
    // Causing communication with hardware
    //----------------------------------------------------------------------------------------------

    /**
     * Enqueue a request to the controller to read the range of data from the HW device that
     * was previously indicated in {@link #enableI2cReadMode(I2cAddr, int, int)} and subsequently written
     * to the controller.
     */
    void readI2cCacheFromController();

    /**
     * Enqueue a request to the controller to write the current contents of the write cache
     * to the HW device.
     */
    void writeI2cCacheToController();

    /**
     * Enqueue a request to the controller to reissue the previous i2c transaction to the HW device.
     */
    void writeI2cPortFlagOnlyToController();

    //----------------------------------------------------------------------------------------------
    // Transaction flag
    //----------------------------------------------------------------------------------------------

    /**
     * Set the flag in the write cache that indicates that when the write cache is next transferred
     * to the controller an i2c transaction should take place. This will be either a read transaction
     * or a write transaction according to whether the {@link #enableI2cReadMode(I2cAddr, int, int)} or
     * {@link #enableI2cWriteMode(I2cAddr, int, int)} has most recently been called.
     * @see #clearI2cPortActionFlag()
     */
    void setI2cPortActionFlag();

    /**
     * Returns whether the action flag is set in the read cache. This is rarely what is actually
     * desired by the {@link I2cDevice} client; it's use generally should be avoided.
     * @return the value of the action flag in the read cache.
     * @deprecated this method returns a value that is rarely that which is expected
     */
    @Deprecated
    boolean isI2cPortActionFlagSet();

    /**
     * Clears the flag that {@link #setI2cPortActionFlag()} sets
     * @see #setI2cPortActionFlag()
     */
    void clearI2cPortActionFlag();

    //----------------------------------------------------------------------------------------------
    // Caching and locking
    //----------------------------------------------------------------------------------------------

    /**
     * Returns access to the read-cache into which data from the controller is read. The returned
     * byte array may be retained for repeated use; {@link #getI2cReadCache()} need not be repeatedly
     * called. The lock returned by {@link #getI2cReadCacheLock()} must be held whenever the data
     * in the returned byte array is accessed. Note that the returned byte array contains an initial
     * header section, four bytes in size, which contains the information manipulated by
     * {@link #enableI2cReadMode(I2cAddr, int, int)} and {@link #enableI2cWriteMode(I2cAddr, int, int)}.
     *
     * @return the read-cache of this I2cDevice
     *
     * @see #getI2cReadCacheLock()
     * @see #getCopyOfReadBuffer()
     */
    byte[] getI2cReadCache();

    /**
     * Returns the time window object into which time stamps are written when the read cache is updated
     * @return the time window object into which time stamps are written when the read cache is updated
     * @see #getI2cReadCache()
     */
    TimeWindow getI2cReadCacheTimeWindow();

    /**
     * Returns access to the lock controlling the read-cache. This lock must be held a while
     * the data accessible from {@link #getI2cReadCache()} is accessed.
     * @return the lock gating access to the read cache
     */
    Lock getI2cReadCacheLock();

    /**
     * Returns access to the write-cache from which data is written to the controller. The returned
     * byte array may be retained for repeated use; {@link #getI2cWriteCache()} need not be repeatedly
     * called. The lock returned by {@link #getI2cWriteCacheLock()} must be held whenever the data
     * in the returned byte array is accessed or written. Note that the returned byte array contains
     * an inital header section, four bytes in size, which contains the information manipulated by
     * {@link #enableI2cReadMode(I2cAddr, int, int)} and {@link #enableI2cWriteMode(I2cAddr, int, int)}.
     *
     * @return the write-cache of this I2cDevice
     *
     * @see #getI2cWriteCacheLock()
     * @see #getCopyOfWriteBuffer()
     */
    byte[] getI2cWriteCache();

    /**
     * Returns access to the lock controlling the write-cache. This lock must be held a while
     * the data accessible from {@link #getI2cWriteCache()} is accessed
     * @return the lock gating access to the write cache
     */
    Lock getI2cWriteCacheLock();

    /**
     * Atomically returns a copy of that portion of the read-cache which does not include the
     * initial four-byte header section: that contains the read payload most recently read from
     * the controller. The read-cache lock need not be held while executing this method.
     *
     * @return a copy of the read payload
     * @see #getI2cReadCache()
     */
    byte[] getCopyOfReadBuffer();

    /**
     * Atomically returns a copy that portion of the write-cache which does not include the
     * initial four-byte header section. The write-cache lock need not be held to execute this
     * method.
     *
     * @return a copy of the user-data portion of the write-cache
     * @see #getI2cWriteCache()
     */
    byte[] getCopyOfWriteBuffer();

    /**
     * Atomically copies the provided buffer into the user portion of the write cache, beginning
     * immediately following the four-byte header. The write-cache lock need not be held to execute this
     * method.
     *
     * @param buffer the data to copy into the write cache
     * @see #getI2cWriteCache()
     */
    void copyBufferIntoWriteBuffer(byte[] buffer);

    //----------------------------------------------------------------------------------------------
    // Callbacks / notifications
    //----------------------------------------------------------------------------------------------

    /**
     * Returns the maximum interval, in milliseconds, from when the controller receives an I2c write
     * transmission over USB to when that write is actually issued to the I2c device.
     *
     * @return the maximum interval, in milliseconds, from when the controller receives an I2c write
     * transmission over USB to when that write is actually issued to the I2c device.
     */
    int getMaxI2cWriteLatency();

    /**
     * Registers an object to get {@link I2cController.I2cPortReadyCallback#portIsReady(int) portIsReady()}
     * callbacks at regular intervals from the {@link I2cDevice}. Only one object may be registered for a callback
     * with an {@link I2cDevice} at any given time.
     * @param callback
     * @see #getI2cPortReadyCallback()
     * @see #deregisterForPortReadyCallback()
     */
    void registerForI2cPortReadyCallback(I2cController.I2cPortReadyCallback callback);

    /**
     * Returns the callback previously registered with {@link #registerForI2cPortReadyCallback}, or
     * null if no callback is currently registered.
     * @return the currently registered callback
     */
    I2cController.I2cPortReadyCallback getI2cPortReadyCallback();

    /**
     * Unregisters any callback currently registered.
     * @see #registerForI2cPortReadyCallback(I2cController.I2cPortReadyCallback)
     */
    void deregisterForPortReadyCallback();

    /**
     * Returns the number of callbacks ever experienced by this I2cDevice instance, whether or not
     * they were ever seen by a registered callback. This method is mostly useful for debugging and
     * gathering of statistics.
     * @return the number of port-is-ready callbacks ever experienced by this {@link I2cDevice} instance
     */
    int getCallbackCount();

    /**
     * Returns whether the I2cDevice instance has experienced a callback since the last issuance
     * of work to the controller. It is difficult to use this method effectively; in most
     * circumstances, one is better off registering a {@link I2cController.I2cPortReadyCallback#portIsReady(int) portIsReady()}
     * callback and putting your processing logic there. Inside the callback, the port is, by definition, ready.
     * Alternately, consider using the {@link I2cDeviceSynch} interface instead.
     *
     * @return whether the port is ready for new work
     * @see I2cDeviceSynch
     * @see #registerForI2cPortReadyCallback(I2cController.I2cPortReadyCallback)
     */
    boolean isI2cPortReady();

    /**
     * Register for notifications as to when {@link I2cController.I2cPortReadyCallback#portIsReady(int) portIsReady()}
     * begin and end. Only one object may be registered for such notifications with an I2cDevice at
     * any given time.
     * @param callback
     * @see #getPortReadyBeginEndCallback()
     * @see #deregisterForPortReadyBeginEndCallback()
     */
    void registerForPortReadyBeginEndCallback(I2cController.I2cPortReadyBeginEndNotifications callback);

    /**
     * Returns the object, if any, currently registered for portIsReady() begin / end notifications
     * @return the currently registered notification receiver, or null if none exists
     * @see #registerForPortReadyBeginEndCallback(I2cController.I2cPortReadyBeginEndNotifications)
     */
    I2cController.I2cPortReadyBeginEndNotifications getPortReadyBeginEndCallback();

    /**
     * Unregisters any portIsReady() begin / end notifications object if any is present.
     * @see #registerForPortReadyBeginEndCallback(I2cController.I2cPortReadyBeginEndNotifications)
     */
    void deregisterForPortReadyBeginEndCallback();

    //----------------------------------------------------------------------------------------------
    // Arming and disarming
    //----------------------------------------------------------------------------------------------

    /**
     * Returns whether, as of this instant, this I2cDevice is alive and operational in
     * its normally expected mode; that is, whether it is currently in communication
     * with its underlying hardware or whether it is in some other state
     *
     * @return the arming status of this I2cDevice
     */
    boolean isArmed();

    //----------------------------------------------------------------------------------------------
    // Deprecated
    //----------------------------------------------------------------------------------------------

    /** @deprecated Use of {@link #getI2cController()} is suggested instead */
    @Deprecated I2cController getController();

    /** @deprecated Use of {@link #readI2cCacheFromController()} is suggested instead */
    @Deprecated void readI2cCacheFromModule();

    /** @deprecated Use of {@link #writeI2cCacheToController()} is suggested instead */
    @Deprecated void writeI2cCacheToModule();

    /** @deprecated Use of {@link #writeI2cPortFlagOnlyToController()} is suggested instead */
    @Deprecated void writeI2cPortFlagOnlyToModule();
    }
