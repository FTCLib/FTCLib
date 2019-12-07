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
 * {@link I2cDeviceSynch} is an interface that exposes functionality for interacting with I2c
 * devices. Its methods are synchronous, in that they complete their action before returning to the
 * caller.
 *
 * <p>Methods are provided to read and write data simply and straightforwardly. Singleton bytes
 * or larger quantities of data can be read or written using {@link #read8(int) read8()} and
 * {@link #write8(int, int) write8()} or {@link #read(int, int) read()} and {@link #write(int, byte[]) write()}
 * respectively. No attention to 'read mode' or 'write mode' is required. Simply call reads
 * and writes as you need them, and the right thing happens.
 *
 * <p>For devices that automatically shutdown if no communication is received within a certain
 * duration, a heartbeat facility is optionally provided.</p>
 *
 * <p>On causality: regarding the sequencing of reads and writes, two important points are
 * worthy of mention. First, reads and writes are ultimately issued to the controller in the same
 * chronological order they were received by the device client instance. Second, even more
 * importantly, reads *always* see the effect of preceding writes on the controller. That is,
 * a read that follows a write will ensure that, first, the write gets issued to the controller, and
 * then, second, a read from the controller is subsequently issued. By contrast, absent such
 * constraints, a read might quickly return freshly-read data already present without having to
 * interact with the controller. Which brings us to...</p>
 *
 * <p>Reading data. The simplest way to read data is to call one of the variations of the
 * {@link #read(int, int) read()} method. This is always correct, and will return data as accessed
 * from the I2c device. However, in many situations, reads can be significantly optimized by use
 * of a <em>read window</em>. With a read window, a larger chunk of the device's I2c register space
 * can be automatically read even if only a portion of it is needed to service a particular read()
 * call; this can make subsequent read()s significantly faster. Further, depending on the mode
 * of {@link com.qualcomm.robotcore.hardware.I2cDeviceSynch.ReadWindow ReadWindow} used, read
 * operations will occur in the background, and read()s will be serviced (subject to causality) out
 * of cached data updated and maintained by the background processing without any synchronous
 * communication with the I2c device itself. For sensors in particular, this mode of operation
 * can be particularly advantageous.</p>
 *
 * <p>Three modes of {@link com.qualcomm.robotcore.hardware.I2cDeviceSynch.ReadWindow ReadWindow} are
 * available:</p>
 * <ol>
 *     <li>{@link ReadMode#REPEAT REPEAT}. In this
 *     mode, background reads are always scheduled and cached data updated whenever there's no other
 *     (writing) work to do. This is the mode commonly used by sensors, who mostly execute reads
 *     and only issue a write relatively infrequently.</li>
 *     <li>{@link ReadMode#BALANCED BALANCED}. In this
 *     mode, background reads are also performed, but only if the user initiates a manual read first after
 *     a write operation. The object does not automatically start background reads on its own, as the
 *     transition from writing to reading can be a relatively expensive one. This mode can be useful
 *     for objects like motor and servo controllers which exhibit a balanced mix of both write and
 *     read operations</li>
 *     <li>{@link ReadMode#ONLY_ONCE ONLY_ONCE}. In this
 *     mode, no background reading is performed; data is always retrieved with a synchronous
 *     communication to the i2c device.</li>
 * </ol>
 *
 * <p>An {@link I2cDeviceSynch} has only one read window in effect at any particular time. This can
 * be updated with {@link #setReadWindow(ReadWindow)} or {@link #ensureReadWindow(ReadWindow, ReadWindow) ensureReadWindow()}.
 * When a read() is made, if the registers requested therein are contained within the current
 * read window, then that whole window of data is read from the i2c device (if the cache is out
 * of date) and the appropriate portion thereof returned. In contrast, if the requested registers
 * are not so contained, then, in effect, a one-off {@link ReadMode#ONLY_ONCE ONLY_ONCE}
 * is used without disturbing the window maintained by {@link #setReadWindow(ReadWindow)}.
 * </p>
 */
@SuppressWarnings("WeakerAccess")
public interface I2cDeviceSynch extends I2cDeviceSynchSimple, Engagable
    {
    //----------------------------------------------------------------------------------------------
    // ReadWindow management
    //----------------------------------------------------------------------------------------------

    /**
     * Set the set of registers that we will read and read and read again on every hardware cycle
     *
     * @param window    the register window to read. May be null, indicating that no reads are to occur.
     * @see #getReadWindow()
     */
    void setReadWindow(ReadWindow window);

    /**
     * Returns the current register window used for reading.
     * @return the current read window
     * @see #setReadWindow(ReadWindow)
     */
    ReadWindow getReadWindow();

    /**
     * Ensure that the current register window covers the indicated set of registers.
     *
     * If there is currently a non-null register window, and windowNeeded is non-null,
     * and the current register window entirely contains windowNeeded, then do nothing.
     * Otherwise, set the current register window to windowToSet.
     *
     * @param windowNeeded Test the current register window, if any, against this window
     *                     to see if an update to the current register window is needed in
     *                     order to cover it. May be null, indicating that an update to the
     *                     current register window is <I>always</I> needed
     * @param windowToSet  If an update to the current register window is needed, then this
     *                     is the window to which it will be set. May be null.
     *
     * @see #setReadWindow(ReadWindow)
     * @see #read8(int)
     */
    void ensureReadWindow(ReadWindow windowNeeded, ReadWindow windowToSet);

    //----------------------------------------------------------------------------------------------
    // Reading
    //----------------------------------------------------------------------------------------------

    /**
     * Advanced: Atomically calls ensureReadWindow() with the last two parameters and then
     * readTimeStamped() with the first two without the possibility of a concurrent client
     * interrupting in the middle.
     *
     * @param ireg              the register number of the first byte register to read
     * @param creg              the number of bytes / registers to read
     * @param readWindowNeeded  the read window we require
     * @param readWindowSet     the read window to set if the required read window is not current
     * @return                  the data that was read, together with the timestamp
     *
     * @see #ensureReadWindow(ReadWindow, ReadWindow)
     * @see #readTimeStamped(int, int)
     */
    TimestampedData readTimeStamped(int ireg, int creg, ReadWindow readWindowNeeded, ReadWindow readWindowSet);

    //----------------------------------------------------------------------------------------------
    // Heartbeats
    //----------------------------------------------------------------------------------------------

    /**
     * Sets the interval within which communication must be received by the I2C device lest
     * a timeout may occur. The default heartbeat interval is zero, signifying that no heartbeat
     * is maintained.
     *
     * @param ms the new hearbeat interval, in milliseconds
     * @see #getHeartbeatInterval()
     */
    void setHeartbeatInterval(int ms);

    /**
     * Returns the interval within which communication must be received by the I2C device lest
     * a timeout occur.
     *
     * @return  the current heartbeat interval, in milliseconds
     * @see #setHeartbeatInterval(int)
     */
    int getHeartbeatInterval();

    /**
     * Sets the action to take when the current heartbeat interval expires.
     * The default action is null; thus, to be useful, an action must always
     * be explicitly specified.
     *
     * @param action the action to take at each heartbeat.
     * @see #getHeartbeatAction()
     * @see #setHeartbeatInterval(int)
     */
    void setHeartbeatAction(HeartbeatAction action);

    /**
     * Returns the current action, if any, to take upon expiration of the heartbeat interval.
     * @return the current heartbeat action. May be null
     * @see #setHeartbeatAction(HeartbeatAction)
     */
    HeartbeatAction getHeartbeatAction();

    /**
     * Instances of HeartBeatAction indicate what action to carry out to perform
     * a heartbeat should that become necessary. The actual action to take is indicated
     * by one of several prioritized possibilities. When a heartbeat is needed, these
     * are considered in order, and the first one applicable given the state of the
     * I2C device at the time will be applied.
     */
    class HeartbeatAction
        {
        /** Priority #1: re-issue the last I2C read operation, if possible.  */
        public final boolean      rereadLastRead;

        /** Priority #2: re-issue the last I2C write operation, if possible. */
        public final boolean      rewriteLastWritten;

        /** Priority #3: explicitly read a given register window             */
        public final ReadWindow   heartbeatReadWindow;

        /** instantiates a new HeartbeatAction. */
        public HeartbeatAction(boolean rereadLastRead, boolean rewriteLastWritten, ReadWindow readWindow)
            {
            this.rereadLastRead      = rereadLastRead;
            this.rewriteLastWritten  = rewriteLastWritten;
            this.heartbeatReadWindow = readWindow;
            }
        }

    //----------------------------------------------------------------------------------------------
    // RegWindow
    //----------------------------------------------------------------------------------------------

    /**
     * {@link ReadMode} controls whether when asked to read we read only once or read multiple times.
     *
     * In all modes, it is guaranteed that a read() which follows a write() operation will
     * see the state of the device <em>after</em> the write has had effect.
     */
    enum ReadMode
        {
        /**
         * Continuously issue I2C reads whenever there's nothing else needing to be done.
         * In this mode, {@link #read(int, int) read()} will not necessarily execute an I2C transaction
         * for every call but might instead return data previously read from the I2C device.
         * This mode is most useful in a device that spends most of its time doing read operations
         * and only very infrequently writes, if ever.
         *
         * @see #read(int, int)
         */
        REPEAT,

        /**
         * Continuously issue I2C reads as in REPEAT when we can, but do <em>not</em> automatically
         * transition back to read-mode following a write operation in order to do so. This mode is
         * most useful in a device which has a balanced mix of read() and write() operations, such
         * as a motor controller. Like {@link #REPEAT}, this mode might return data that was
         * previously read a short while ago.
         */
        BALANCED,

        /**
         * Only issue a single I2C read, then set the read window to null to disable further reads.
         * Executing a {@link #read(int, int) read()} in this mode will always get fresh data
         * from the I2C device.
         */
        ONLY_ONCE
        };


    /**
     * RegWindow is a utility class for managing the window of I2C register bytes that
     * are read from our I2C device on every hardware cycle
     */
    class ReadWindow
        {
        //------------------------------------------------------------------------------------------
        // State
        //------------------------------------------------------------------------------------------

        /**
         * enableI2cReadMode and enableI2cWriteMode both impose a maximum length
         * on the size of data that can be read or written at one time. {@link #READ_REGISTER_COUNT_MAX}
         * and {@link #WRITE_REGISTER_COUNT_MAX} indicate those maximum sizes.
         * @see #WRITE_REGISTER_COUNT_MAX
         */
        public static final int READ_REGISTER_COUNT_MAX = 26;   // No, not 27: the CDIM can't handle 27
        /** @see #READ_REGISTER_COUNT_MAX */
        public static final int WRITE_REGISTER_COUNT_MAX = 26;  // the CDIM might be able to do 27, not just 26, but we're paranoid

        /**
         * The first register in the window
         */
        private final int iregFirst;
        /**
         * The number of registers in the window
         */
        private final int creg;
        /**
         * The mode of the window
         */
        private final ReadMode readMode;
        /**
         * Whether a read has been issued for this window or not
         */
        private boolean usedForRead;


        /**
         * Returns the first register in the window
         * @return the first register in the window
         */
        public int getRegisterFirst() { return this.iregFirst; }
        /**
         * Returns the first register NOT in the window
         * @return the first register NOT in the window
         */
        public int getRegisterMax()   { return this.iregFirst + this.creg; }
        /**
         * Returns the number of registers in the window
         * @return the number of registers in the window
         */
        public int getRegisterCount()      { return this.creg; }
        /**
         * Returns the mode of the window
         * @return the mode of the window
         */
        public ReadMode getReadMode() { return this.readMode; }
        /**
         * Returns whether a read has ever been issued for this window or not
         * @return whether a read has ever been issued for this window or not
         */
        public boolean hasWindowBeenUsedForRead() { return this.usedForRead; }
        /**
         * Sets that a read has in fact been issued for this window
         */
        public void noteWindowUsedForRead() { this.usedForRead = true; }

        /**
         * Answers as to whether we're allowed to read using this window. This will return
         * false for ONLY_ONCE windows after {@link #noteWindowUsedForRead()} has been called on them.
         * @return whether it is permitted to perform a read for this window.
         */
        public boolean canBeUsedToRead()
            {
            return !this.usedForRead || this.readMode != ReadMode.ONLY_ONCE;
            }

        /**
         * Answers as to whether this window in its present state ought to cause a transition
         * to read-mode when there's nothing else for the device to be doing.
         * @return whether this device should cause a read mode transition
         */
        public boolean mayInitiateSwitchToReadMode()
            {
            return !this.usedForRead || this.readMode == ReadMode.REPEAT;
            }

        //------------------------------------------------------------------------------------------
        // Construction
        //------------------------------------------------------------------------------------------

        /**
         * Create a new register window with the indicated starting register and register count
         *
         * @param iregFirst the index of the first register to read
         * @param creg      the number of registers to read
         * @param readMode  whether to repeat-read or read only once
         */
        public ReadWindow(int iregFirst, int creg, ReadMode readMode)
            {
            this.readMode   = readMode;
            this.usedForRead = false;
            this.iregFirst  = iregFirst;
            this.creg       = creg;
            if (creg < 0 || creg > READ_REGISTER_COUNT_MAX)
                throw new IllegalArgumentException(String.format("buffer length %d invalid; max is %d", creg, READ_REGISTER_COUNT_MAX));
            }

        /**
         * Returns a copy of this window but with the {@link #usedForRead} flag clear
         * @return a fresh copy of the window into which data can actually be read.
         */
        public ReadWindow readableCopy()
            {
            return new ReadWindow(this.iregFirst, this.creg, this.readMode);
            }

        //------------------------------------------------------------------------------------------
        // Operations
        //------------------------------------------------------------------------------------------

        /**
         * Do the receiver and the indicated register window cover exactly the
         * same set of registers and have the same modality?
         * @param him   the other window to compare to
         * @return the result of the comparison
         */
        public boolean sameAsIncludingMode(ReadWindow him)
            {
            if (him == null)
                return false;

            return this.getRegisterFirst() == him.getRegisterFirst()
                    && this.getRegisterCount() == him.getRegisterCount()
                    && this.getReadMode() == him.getReadMode();
            }

        /**
         * Answers as to whether the receiver wholly contains the indicated window.
         *
         * @param him   the window we wish to see whether we contain
         * @return      whether or not we contain the window
         * @see #contains(int, int)
         */
        public boolean contains(ReadWindow him)
            {
            if (him==null)
                return false;

            return this.getRegisterFirst() <= him.getRegisterFirst() && him.getRegisterMax() <= this.getRegisterMax();
            }

        /**
         * Answers as to whether the receiver wholly contains the indicated window
         * and also has the same modality.
         *
         * @param him   the window we wish to see whether we contain
         * @return      whether or not we contain the window
         */
        public boolean containsWithSameMode(ReadWindow him)
            {
            return contains(him) && (this.getReadMode() == him.getReadMode());
            }

        /**
         * Answers as to whether the receiver wholly contains the indicated set of registers.
         *
         * @param ireg  the first register of interest
         * @param creg  the number of registers of interest
         * @return whether or not the receiver contains this set of registers
         */
        public boolean contains(int ireg, int creg)
            {
            return this.containsWithSameMode(new ReadWindow(ireg, creg, this.getReadMode()));
            }
        }
    }
