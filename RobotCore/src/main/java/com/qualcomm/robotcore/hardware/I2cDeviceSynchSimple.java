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

/**
 * I2cDeviceSyncSimple is an interface that provides simple synchronous read and write
 * functionality to an I2c device.
 * @see I2cDeviceSynch
 * @see I2cDeviceSynchImplOnSimple
 */
public interface I2cDeviceSynchSimple extends HardwareDevice, HardwareDeviceHealth, I2cAddrConfig, RobotConfigNameable
    {
    //----------------------------------------------------------------------------------------------
    // Reading
    //----------------------------------------------------------------------------------------------

    /**
     * Read the byte at the indicated register. See {@link #readTimeStamped(int, int)} for a
     * complete description.
     *
     * @param ireg  the register number to read
     * @return      the byte that was read
     *
     * @see #read(int, int)
     * @see #readTimeStamped(int, int)
     * @see I2cDeviceSynch#ensureReadWindow(I2cDeviceSynch.ReadWindow, I2cDeviceSynch.ReadWindow)
     */
    byte read8(int ireg);

    /**
     * Read a contiguous set of device I2C registers. See {@link #readTimeStamped(int, int)} for a
     * complete description.
     *
     * @param ireg  the register number of the first byte register to read
     * @param creg  the number of bytes / registers to read
     * @return      the data which was read
     *
     * @see #read8(int)
     * @see #readTimeStamped(int, int)
     * @see I2cDeviceSynch#ensureReadWindow(I2cDeviceSynch.ReadWindow, I2cDeviceSynch.ReadWindow)
     */
    byte[] read(int ireg, int creg);

    /**
     * Reads and returns a contiguous set of device I2C registers, together with a best-available
     * timestamp of when the actual I2C read occurred. Note that this can take many tens of
     * milliseconds to execute, and thus should not be called from the loop() thread.
     *
     * <p>You can always just call this method without worrying at all about
     * {@link I2cDeviceSynch.ReadWindow read windows},
     * that will work, but usually it is more efficient to take some thought and care as to what set
     * of registers the I2C device controller is being set up to read, as adjusting that window
     * of registers incurs significant extra time.</p>
     *
     * <p>If the current read window can't be used to read the requested registers, then
     * a new read window will automatically be created as follows. If the current read window is non
     * null and wholly contains the registers to read but can't be read because it is a used-up
     * {@link com.qualcomm.robotcore.hardware.I2cDeviceSynch.ReadMode#ONLY_ONCE ReadMode#ONLY_ONCE} window,
     * a new read fresh window will be created with the same set of registers. Otherwise, a
     * window that exactly covers the requested set of registers will be created.</p>
     *
     * @param ireg  the register number of the first byte register to read
     * @param creg  the number of bytes / registers to read
     * @return      the data which was read, together with the timestamp
     *
     * @see #read(int, int)
     * @see #read8(int)
     * @see I2cDeviceSynch#ensureReadWindow(I2cDeviceSynch.ReadWindow, I2cDeviceSynch.ReadWindow)
     */
    TimestampedData readTimeStamped(int ireg, int creg);

    //----------------------------------------------------------------------------------------------
    // Writing
    //----------------------------------------------------------------------------------------------

    /**
     * Writes a byte to the indicated register using {@link I2cWaitControl#ATOMIC} semantics.
     *
     * @param ireg      the register number that is to be written
     * @param bVal      the byte which is to be written to that register
     *
     * @see #write(int, byte[], I2cWaitControl)
     */
    void write8(int ireg, int bVal);

    /**
     * Writes data to a set of registers, beginning with the one indicated, using
     * {@link I2cWaitControl#ATOMIC} semantics.
     *
     * @param ireg      the first of the registers which is to be written
     * @param data      the data which is to be written to the registers
     *
     * @see #write(int, byte[], I2cWaitControl)
     */
    void write(int ireg, byte[] data);

    /**
     * Writes a byte to the indicated register. See also
     * {@link #write(int, byte[], I2cWaitControl)}.
     *
     * @param ireg                  the register number that is to be written
     * @param bVal                  the byte which is to be written to that register
     * @param waitControl           controls the behavior of waiting for the completion of the write
     *
     * @see #write(int, byte[], I2cWaitControl)
     */
    void write8(int ireg, int bVal, I2cWaitControl waitControl);

    /**
     * Writes data to a set of registers, beginning with the one indicated. The data will be
     * written to the I2C device in an expeditious manner. Once data is accepted by this API,
     * it is guaranteed that (barring catastrophic failure) the data will be transmitted to the
     * USB controller module before the I2cDeviceSync is closed. The call itself may or may block
     * until the data has been transmitted to the USB controller module according to a caller-provided
     * parameter.
     *
     * @param ireg                  the first of the registers which is to be written
     * @param data                  the data which is to be written to the registers
     * @param waitControl           controls the behavior of waiting for the completion of the write
     */
    void write(int ireg, byte[] data, I2cWaitControl waitControl);

    /**
     * Waits for the most recent write to complete according to the behavior specified in writeControl.
     *
     * @param waitControl           controls the behavior of waiting for the completion of the write
     *                              Note that a value of {@link I2cWaitControl#NONE} is essentially a no-op.
     */
    void waitForWriteCompletions(I2cWaitControl waitControl);

    /**
     * Enables or disables an optimization wherein writes to two sets of adjacent register
     * ranges may be coalesced into a single I2c transaction if the second write comes along
     * while the first is still queued for writing. By default, write coalescing is disabled.
     * @param enable whether to enable write coalescing or not
     *
     * @see #isWriteCoalescingEnabled()
     */
    void enableWriteCoalescing(boolean enable);

    /**
     * Answers as to whether write coalescing is currently enabled on this device.
     * @return whether write coalescing is currently enabled or not.
     *
     * @see #enableWriteCoalescing(boolean)
     */
    boolean isWriteCoalescingEnabled();

    //----------------------------------------------------------------------------------------------
    // Monitoring, debugging, and life cycle management
    //----------------------------------------------------------------------------------------------

    /**
     * Returns whether, as of this instant, this device client is alive and operational in
     * its normally expected mode; that is, whether it is currently in communication
     * with its underlying hardware or whether it is in some other state. Note that a device
     * client which is not engaged will never report as armed.
     *
     * @return the arming state of this device client
     * @see Engagable#engage()
     */
    boolean isArmed();

    /**
     * Sets the I2C address of the underlying client. If necessary, the client may be briefly
     * disengaged (and then automatically reengaged) in the process.
     * @param i2cAddr the new I2C address
     * @deprecated Use {@link #setI2cAddress(I2cAddr)} instead.
     */
    @Deprecated
    void setI2cAddr(I2cAddr i2cAddr);

    /**
     * Returns the I2C address currently being used by this device client
     * @return the current I2C address
     * @deprecated Use {@link #getI2cAddress()} instead.
     */
    @Deprecated
    I2cAddr getI2cAddr();

    //----------------------------------------------------------------------------------------------
    // Debugging
    //----------------------------------------------------------------------------------------------

    /**
     * Turn logging on or off. Logging output can be viewed using the Android Logcat tools.
     * @param enabled     whether to enable logging or not
     */
    void setLogging(boolean enabled);

    /** @see #setLogging(boolean)  */
    boolean getLogging();

    /**
     * Set the tag to use when logging is on.
     * @param loggingTag    the logging tag to sue
     */
    void setLoggingTag(String loggingTag);

    /** @see #setLoggingTag(String) */
    String getLoggingTag();
    }
