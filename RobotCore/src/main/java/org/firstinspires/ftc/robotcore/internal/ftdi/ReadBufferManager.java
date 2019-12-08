/*
Copyright (c) 2017 Robert Atkinson

All rights reserved.

Derived in part from information in various resources, including FTDI, the
Android Linux implementation, FreeBsc, UsbSerial, and others.

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
package org.firstinspires.ftc.robotcore.internal.ftdi;

import androidx.annotation.Nullable;

import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.external.function.Consumer;
import org.firstinspires.ftc.robotcore.internal.collections.ArrayRunQueueLong;
import org.firstinspires.ftc.robotcore.internal.collections.CircularByteBuffer;
import org.firstinspires.ftc.robotcore.internal.collections.EvictingBlockingQueue;
import org.firstinspires.ftc.robotcore.internal.collections.MarkedItemQueue;
import org.firstinspires.ftc.robotcore.internal.hardware.TimeWindow;
import org.firstinspires.ftc.robotcore.internal.system.Assert;
import org.firstinspires.ftc.robotcore.internal.system.Deadline;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Manager of bulk data received from USB
 */
@SuppressWarnings("WeakerAccess")
public class ReadBufferManager extends FtConstants
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "ReadBufferManager";

    private final    FtDeviceManagerParams               mParams;
    private          Deadline                            mReadDeadline;
    private final    int                                 mAvailableInBuffersCapacity;
    private final    int                                 mAvailableOutBuffersCapacity;
    private final    ArrayList<BulkPacketBufferIn>       mAvailableInBuffers;   // finite capacity
    private final    ArrayList<BulkPacketBufferOut>      mAvailableOutBuffers;  // finite capacity
    private final    ArrayList<BulkPacketBufferIn>       mReadableBuffers;      // infinite capacity
    private final    EvictingBlockingQueue<BulkPacketBuffer> mRetainedBuffers;
    private final    int                                 mEndpointMaxPacketSize;
    private final    FtDevice                            mDevice;
    private final    CircularByteBuffer                  mCircularBuffer;
    private final    MarkedItemQueue                     mMarkedItemQueue;
    private final    ArrayRunQueueLong                   mTimestamps;
    private          boolean                             mReadBulkInDataInterruptRequested;
    private volatile Thread                              mReadBulkInDataThread;
    private volatile boolean                             mProcessBulkInDataCallInFlight;
    private          boolean                             mIsOpen;
    private          boolean                             mDebugRetainBuffers;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public ReadBufferManager(FtDevice dev, boolean debugRetainBuffers) throws IOException, InterruptedException
        {
        this.mIsOpen                    = true;
        this.mDevice                    = dev;
        this.mParams                    = this.mDevice.getDriverParameters();
        this.mReadDeadline              = new Deadline(this.mParams.getBulkInReadTimeout(), TimeUnit.MILLISECONDS);
        this.mEndpointMaxPacketSize     = this.mDevice.getEndpointMaxPacketSize();
        this.mCircularBuffer            = new CircularByteBuffer(this.mEndpointMaxPacketSize * 5 /* a guess */, this.mParams.getMaxReadBufferSize());
        this.mMarkedItemQueue           = new MarkedItemQueue();
        this.mTimestamps                = new ArrayRunQueueLong();
        this.mAvailableInBuffersCapacity  = this.mParams.getPacketBufferCacheSize();
        this.mAvailableOutBuffersCapacity = Math.min(this.mAvailableInBuffersCapacity,mParams.getRetainedBufferCapacity());
        this.mAvailableInBuffers        = new ArrayList<BulkPacketBufferIn>();
        this.mAvailableOutBuffers       = new ArrayList<BulkPacketBufferOut>();
        this.mReadableBuffers           = new ArrayList<BulkPacketBufferIn>(); // need effectively infinite capacity to be able to keep servicing USB w/o losing data
        this.mRetainedBuffers           = new EvictingBlockingQueue<BulkPacketBuffer>(new ArrayBlockingQueue<BulkPacketBuffer>(mParams.getRetainedBufferCapacity())); // don't need the blocking, but do need the eviction action
        this.mRetainedBuffers.setEvictAction(new RecentPacketEvicted());
        this.mReadBulkInDataThread             = null;
        this.mReadBulkInDataInterruptRequested = false;
        this.mProcessBulkInDataCallInFlight    = false;
        this.mDebugRetainBuffers               = debugRetainBuffers;
        verifyInvariants("ctor");
        }

    //----------------------------------------------------------------------------------------------
    // Accessing
    //----------------------------------------------------------------------------------------------

    public boolean isReadBufferFull()
        {
        synchronized (mCircularBuffer)
            {
            return mCircularBuffer.remainingCapacity() == 0;
            }
        }

    public int getReadBufferSize()
        {
        synchronized (mCircularBuffer)
            {
            return mCircularBuffer.size();
            }
        }

    public FtDeviceManagerParams getParams()
        {
        return this.mParams;
        }

    //----------------------------------------------------------------------------------------------
    // Buffer management
    //----------------------------------------------------------------------------------------------

    // For sufficiently small buffers, we use a fixed allocation size so as to facilitate
    // instance reuse and reduce pressure on the GC
    public BulkPacketBufferOut newOutputBuffer(byte[] data, int ibFirst, int cbToWrite)
        {
        BulkPacketBufferOut result = null;
        //
        if (cbToWrite <= mEndpointMaxPacketSize)
            {
            synchronized (mAvailableOutBuffers)
                {
                if (!mAvailableOutBuffers.isEmpty())
                    {
                    result = mAvailableOutBuffers.remove(mAvailableOutBuffers.size()-1); // nit: LIFO for better cache locality
                    }
                }
            if (result == null)
                {
                result = new BulkPacketBufferOut(mEndpointMaxPacketSize);
                }
            }
        else
            result = new BulkPacketBufferOut(cbToWrite);
        //
        result.copyFrom(data, ibFirst, cbToWrite);
        return result;
        }

    private void offerAvailableBufferOut(BulkPacketBufferOut packetBuffer)
        {
        if (packetBuffer.capacity() == mEndpointMaxPacketSize)
            {
            synchronized (mAvailableOutBuffers)
                {
                // keep a few around around so as to reduce GC pressure
                if (mAvailableOutBuffers.size() < mAvailableOutBuffersCapacity)
                    {
                    mAvailableOutBuffers.add(packetBuffer);
                    }
                }
            }
        }

    //--------------

    public BulkPacketBufferIn acquireWritableInputBuffer()
        {
        BulkPacketBufferIn result = null;
        synchronized (mAvailableInBuffers)
            {
            if (!mAvailableInBuffers.isEmpty())
                {
                result = mAvailableInBuffers.remove(mAvailableInBuffers.size()-1); // nit: LIFO for better cache locality
                }
            }
        if (result == null)
            {
            /**
             * The issue of how big of packet buffer to use is surprisingly complicated. The best
             * treatise on this so far located is the FTDI application note entitled:
             *
             *  "AN232B-04 Data Throughput, Latency and Handshaking"
             *
             * To cut to the chase: the best trade-off is to use the endpoint packet size, which is
             * usually (perhaps always?) 64 bytes. If one uses larger than this, if the data comes
             * in at just the most inopportune rate, you can wait multiple frames to get your data,
             * increasing latency perhaps significantly (see Section 3.3, which illustrates how a
             * hypothetical 4KB buffer at a 38.75kbaud could take the full 1.06 before the chip
             * returned data to the driver).
             *
             * Note that FTDI chips seem to have an internal receive buffer of 256 or 512 bytes
             * depending on the model.
             */
            result = new BulkPacketBufferIn(mEndpointMaxPacketSize);
            }
        return result;
        }

    public void releaseReadableBuffer(BulkPacketBufferIn packetBuffer)
        {
        synchronized (mReadableBuffers)
            {
            mReadableBuffers.add(packetBuffer); // adds at end
            mReadableBuffers.notifyAll();
            }
        }

    public BulkPacketBufferIn acquireReadableInputBuffer() throws InterruptedException
        {
        for (;;)
            {
            synchronized (mReadableBuffers)
                {
                if (!mReadableBuffers.isEmpty())
                    {
                    return mReadableBuffers.remove(0);
                    }
                mReadableBuffers.wait();
                }
            }
        }

    public void releaseWritableInputBuffer(BulkPacketBufferIn packetBuffer)
        {
        // Don't retain buffers that have no user data at all
        if (packetBuffer.getCurrentLength() <= MODEM_STATUS_SIZE || !retainRecentBuffer(packetBuffer))
            {
            offerAvailableBufferIn(packetBuffer);
            }
        }

    private void offerAvailableBufferIn(BulkPacketBufferIn packetBuffer)
        {
        synchronized (mAvailableInBuffers)
            {
            // keep a few around around so as to reduce GC pressure
            if (mAvailableInBuffers.size() < mAvailableInBuffersCapacity)
                {
                mAvailableInBuffers.add(packetBuffer);
                }
            }
        }

    private class RecentPacketEvicted implements Consumer<BulkPacketBuffer>
        {
        @Override public void accept(BulkPacketBuffer bulkPacketBuffer)
            {
            if (bulkPacketBuffer instanceof BulkPacketBufferIn)
                {
                offerAvailableBufferIn((BulkPacketBufferIn)bulkPacketBuffer);
                }
            else
                {
                offerAvailableBufferOut((BulkPacketBufferOut)bulkPacketBuffer);
                }
            }
        }

    public void setDebugRetainBuffers(boolean retainRecentBuffers)
        {
        synchronized (mRetainedBuffers)
            {
            mDebugRetainBuffers = retainRecentBuffers;
            if (!mDebugRetainBuffers)
                {
                mRetainedBuffers.clear();
                }
            }
        }

    public boolean getDebugRetainBuffers()
        {
        return mDebugRetainBuffers;
        }

    public boolean retainRecentBuffer(BulkPacketBuffer buffer)
        {
        synchronized (mRetainedBuffers)
            {
            if (mDebugRetainBuffers)
                {
                mRetainedBuffers.add(buffer);
                // RobotLog.logBytes(TAG, "retained", buffer.arrayOffset(), buffer.getCurrentLength(), buffer.array());
                return true;
                }
            }
        return false;
        }

    public void logRetainedBuffers(long nsOrigin, long nsTimerExpire, String tag, String format, Object...args)
        {
        synchronized (mRetainedBuffers)
            {
            RobotLog.vv(tag, format, args);
            //
            BulkPacketBuffer firstBuffer = null;
            TimeUnit timeUnitReport = TimeUnit.NANOSECONDS;
            double timeUnitScale = 1.0 / ElapsedTime.MILLIS_IN_NANO;
            for (;;)
                {
                BulkPacketBuffer buffer = mRetainedBuffers.poll();
                if (buffer == null)
                    break;
                if (firstBuffer == null)
                    {
                    firstBuffer = buffer;
                    if (nsOrigin == 0)
                        {
                        nsOrigin = firstBuffer.getTimestamp(TimeUnit.NANOSECONDS);
                        }
                    }

                double ns = buffer.getTimestamp(timeUnitReport) - timeUnitReport.convert(nsOrigin, TimeUnit.NANOSECONDS);
                String bufferCaption = String.format("%s (ts=%.3f)", buffer instanceof BulkPacketBufferIn ? "read " : "write", ns * timeUnitScale);
                RobotLog.logBytes(tag, bufferCaption, buffer.array(), buffer.arrayOffset(), buffer.getCurrentLength());
                }

            if (nsTimerExpire > 0)
                {
                RobotLog.vv(tag, "timer expired (ts=%.3f)", timeUnitReport.convert(nsTimerExpire - nsOrigin, TimeUnit.NANOSECONDS) * timeUnitScale);
                }
            }
        }

    //----------------------------------------------------------------------------------------------
    // I/O
    //----------------------------------------------------------------------------------------------

    /** Called on {@link ReadBufferWorker} thread */
    public void processBulkInData(BulkPacketBufferIn packetBuffer) throws FtDeviceIOException, InterruptedException
        {
        if (isOpen() && packetBuffer.getCurrentLength() > 0)
            {
            mProcessBulkInDataCallInFlight = true;

            try {
                final int cbBuffer = packetBuffer.getCurrentLength();
                if (cbBuffer < MODEM_STATUS_SIZE)
                    {
                    return;
                    }

                // Wait until we have enough room in the buffer to write the incoming data
                // TODO: reexamine policy about when to toss data: where does the back pressure go?
                synchronized (mCircularBuffer)
                    {
                    for (;;)
                        {
                        int cbFree   = mCircularBuffer.remainingCapacity();
                        int cbNeeded = cbBuffer - MODEM_STATUS_SIZE;
                        if (cbNeeded <= cbFree)
                            break;

                        // Wait until the state of the buffer changes
                        mCircularBuffer.wait();

                        // Get out of Dodge if things have closed while we were waiting
                        if (!isOpen()) return;
                        }
                    }

                this.extractReadData(packetBuffer);
                }
            finally
                {
                mProcessBulkInDataCallInFlight = false;
                }
            }
        }

    /** called on {@link ReadBufferWorker} thread */
    private void extractReadData(BulkPacketBufferIn packetBuffer) throws InterruptedException
        {
        final int cbBuffer = packetBuffer.getCurrentLength();
        if (cbBuffer > 0)
            {
            verifyInvariants("->extractReadData");
            try {
                short   signalEvents = 0;
                short   signalLineEvents = 0;
                boolean signalRxChar = false;

                final int packetCount = cbBuffer / this.mEndpointMaxPacketSize + (cbBuffer % this.mEndpointMaxPacketSize > 0 ? 1 : 0);
                // RobotLog.dd(TAG, "packetCount=%d cb=%d", packetCount, cbBuffer);

                ByteBuffer byteBuffer = packetBuffer.getByteBuffer();
                int cbExtracted = 0;
                for (int iPacket = 0; iPacket < packetCount; ++iPacket)
                    {
                    int ibFirst;
                    int ibMax;
                    if (iPacket == packetCount - 1)
                        {
                        // Last packet : use modem status at start of packet
                        ibFirst = iPacket * this.mEndpointMaxPacketSize;
                        ibMax = cbBuffer;
                        setBufferBounds(byteBuffer, ibFirst, ibMax);
                        //
                        byte b0 = byteBuffer.get(); // Assert.assertTrue(b0 == 0x01, "b0==0x%02x", b0);
                        signalEvents = (short) (this.mDevice.mDeviceInfo.modemStatus ^ (short) (b0 & 0xF0));
                        this.mDevice.mDeviceInfo.modemStatus = (short) (b0 & 0xF0); // this sign extends, which probably isn't what's desired
                        //
                        byte b1 = byteBuffer.get(); // Assert.assertTrue(b1==0x60 || b1==0x00, "b1==0x%02x", b1);
                        this.mDevice.mDeviceInfo.lineStatus = (short) (b1 & 0xFF);  // this sign extends, which probably isn't what's desired
                        //
                        ibFirst += MODEM_STATUS_SIZE;
                        //
                        if (byteBuffer.hasRemaining())
                            {
                            signalLineEvents = (short) (this.mDevice.mDeviceInfo.lineStatus & 0x1E);
                            }
                        else
                            {
                            signalLineEvents = 0;
                            }
                        }
                    else
                        {
                        // Not the last packet : ignore modem status at start of packet
                        ibFirst = iPacket * this.mEndpointMaxPacketSize + MODEM_STATUS_SIZE;
                        ibMax = (iPacket + 1) * this.mEndpointMaxPacketSize;
                        setBufferBounds(byteBuffer, ibFirst, ibMax);
                        }

                    Assert.assertTrue(byteBuffer.remaining() == ibMax - ibFirst);
                    int cbPacket = ibMax - ibFirst;
                    if (cbPacket > 0)
                        {
                        synchronized (mCircularBuffer)
                            {
                            // Remember the bytes in our linear array of bytes
                            cbExtracted += mCircularBuffer.write(byteBuffer);

                            // The first of those was at the start a packet (ie: followed modem status
                            // bytes) while the remainder were not
                            mMarkedItemQueue.addMarkedItem();
                            mMarkedItemQueue.addUnmarkedItems(cbPacket-1);

                            // Remember when these packets came in
                            mTimestamps.addLast(packetBuffer.getTimestamp(TimeUnit.NANOSECONDS), cbPacket);
                            }
                        }
                    }

                if (cbExtracted > 0)
                    {
                    signalRxChar = true;
                    wakeReadBulkInData();
                    }

                byteBuffer.clear();
                this.processEventChars(signalRxChar, signalEvents, signalLineEvents);
                }
            finally
                {
                verifyInvariants("<-extractReadData");
                }
            }
        }

    private void setBufferBounds(ByteBuffer buffer, int ibFirst, int ibMax)
        {
        buffer.clear();             // don't assume positions: position <- 0, limit <- capacity
        buffer.position(ibFirst);
        buffer.limit(ibMax);
        }

    private void verifyInvariants(String context)
        {
        /*synchronized (mCircularBuffer)
            {
            int cbData = mCircularBuffer.size();
            int cMarked = mMarkedItemQueue.size();
            int cTimestamp = mTimestamps.size();
            Assert.assertTrue(cbData == cMarked);
            Assert.assertTrue(cbData == cTimestamp);
            }*/
        }

    /**
     * Attempt to read cbToRead bytes from the device, subject to a timeout
     *
     * @param data          the buffer into which the data is to be placed, starting at the beginning
     * @param cbToRead      the number of bytes to read
     * @param msTimeout     the number of milliseconds to wait for the result
     * @param timeWindow    optional place into which to record the timestamps that cover the duration of the read data
     * @return              the number of bytes read (zero means the timeout is reached w/o returning the data
     *                      unless the number of bytes to read was itself zero)
     *                      FT_Device.RC_DEVICE_CLOSED : the device was closed
     * @throws InterruptedException
     */
    public int readBulkInData(final byte[] data, final int ibFirst, final int cbToRead, long msTimeout, @Nullable TimeWindow timeWindow) throws InterruptedException
        {
        if (mReadBulkInDataInterruptRequested)
            {
            throw new InterruptedException("interrupted in readBulkInData()");
            }
        else if (cbToRead > 0 && isOpen())
            {
            mReadBulkInDataThread = Thread.currentThread();
            try {
                verifyInvariants("->readBulkInData");
                final Deadline readDeadline = getReadDeadline(msTimeout);

                // Loop until we get the amount of data we came for
                while (isOpen())
                    {
                    // Stop if we've timed out
                    if (readDeadline.hasExpired())
                        {
                        return 0;
                        }

                    // If we've been poked, then poke our callers
                    if (Thread.interrupted())
                        {
                        throw new InterruptedException("interrupted reading USB data");
                        }

                    // Is there enough data there for us to read?
                    synchronized (mCircularBuffer)
                        {
                        if (mCircularBuffer.size() >= cbToRead)
                            {
                            // Yes, read it
                            int cbRead = mCircularBuffer.read(data, ibFirst, cbToRead);
                            if (cbRead > 0)
                                {
                                mMarkedItemQueue.removeItems(cbRead);
                                //
                                if (timeWindow != null)
                                    {
                                    timeWindow.setNanosecondsFirst(mTimestamps.getFirst());
                                    timeWindow.setNanosecondsLast(mTimestamps.removeFirstCount(cbRead));
                                    }
                                else
                                    {
                                    mTimestamps.removeFirstCount(cbRead);    // just discard
                                    }
                                //
                                mCircularBuffer.notifyAll();
                                }
                            return cbRead;
                            }

                        // Not enough data. Wait for more data to come in. In art, the wait system
                        // complains to the log if you use a non-integer wait interval, so we cap.
                        long msRemaining = Math.min(readDeadline.timeRemaining(TimeUnit.MILLISECONDS), Integer.MAX_VALUE);
                        if (msRemaining > 0)
                            {
                            mCircularBuffer.wait(msRemaining);
                            }
                        }
                    }

                // The device was closed while we were waiting
                return FtDevice.RC_DEVICE_CLOSED;
                }
            finally
                {
                verifyInvariants("<-readBulkInData");
                mReadBulkInDataThread = null;
                }
            }
        else
            {
            return 0;
            }
        }

    /** We cache in member variable to as to avoid creating oodles of short-lived objects */
    protected Deadline getReadDeadline(long msTimeout)
        {
        if (msTimeout == 0)
            {
            msTimeout = this.mParams.getBulkInReadTimeout();
            }
        if (mReadDeadline.getDuration(TimeUnit.MILLISECONDS) == msTimeout)
            {
            mReadDeadline.reset();
            }
        else
            {
            mReadDeadline = new Deadline(msTimeout, TimeUnit.MILLISECONDS);
            }
        return mReadDeadline;
        }

    public boolean mightBeAtUsbPacketStart()
        {
        // If it's empty, then the next data will be the start of a packet, by definition
        return mMarkedItemQueue.isAtMarkedItem() || mMarkedItemQueue.isEmpty();
        }

    public void skipToLikelyUsbPacketStart()
        {
        synchronized (mCircularBuffer)
            {
            int cbSkip = mMarkedItemQueue.removeUpToNextMarkedItemOrEnd();
            if (cbSkip > 0)
                {
                mTimestamps.removeFirstCount(cbSkip);
                mCircularBuffer.skip(cbSkip);
                mCircularBuffer.notifyAll();
                }
            }
        }

    private boolean isOpen()
        {
        return mIsOpen && FtDevice.isOpen(this.mDevice);
        }

    private void wakeReadBulkInData()
        {
        synchronized (this.mCircularBuffer)
            {
            this.mCircularBuffer.notifyAll();
            }
        }

    private boolean extantReadBulkInDataCall()
        {
        return mReadBulkInDataThread != null;
        }

    public void requestReadInterrupt(boolean requested)
        {
        if (requested)
            {
            mReadBulkInDataInterruptRequested = true;
            Thread thread = mReadBulkInDataThread;
            if (thread != null)
                {
                thread.interrupt();
                }
            }
        else
            mReadBulkInDataInterruptRequested = false;
        }

    private void spinWaitNoReadBulkInData()
        {
        for (;;)
            {
            if (!extantReadBulkInDataCall())
                {
                return;
                }
            Thread.yield();
            }
        }

    private boolean extantProcessBulkInData()
        {
        return mProcessBulkInDataCallInFlight;
        }

    public void purgeInputData()
        {
        synchronized (mCircularBuffer)
            {
            synchronized (mReadableBuffers)
                {
                mReadableBuffers.clear();
                }
            mCircularBuffer.clear();
            mMarkedItemQueue.clear();
            mTimestamps.clear();
            }
        }

    /**
     * The original implementation of processEventChars used an in-process broadcast as notification
     * mechanism. That required a whole new external library (implementing LocalBroadcastManager)
     * which we'd very much not like to have to link in here. And since we presently don't need such
     * a notification mechanism, we'll do without it for now. Recommendation: if we ever DO need
     * such a thing, use a better mechanism :-).
     *
     * From the FTDI D2XX Programmer's Guide:
     *
     * "The least significant byte of the lpdwModemStatus value holds the modem status. On Windows and
     * Windows CE, the line status is held in the second least significant byte of the lpdwModemStatus value.
     *
     * The modem status is bit-mapped as follows: Clear To Send (CTS) = 0x10, Data Set Ready (DSR) = 0x20,
     * Ring Indicator (RI) = 0x40, Data Carrier Detect (DCD) = 0x80.
     *
     * The line status is bit-mapped as follows: Overrun Error (OE) = 0x02, Parity Error (PE) = 0x04,
     * Framing Error (FE) = 0x08, Break Interrupt (BI) = 0x10."
     */
    public int processEventChars(boolean fRxChar, short sEvents, short slEvents) throws InterruptedException
        {
        return 0;
        }

    void close()
        {
        if (mIsOpen)
            {
            // Set a flag so that anyone who notices will get out of the way
            mIsOpen = false;

            // Caller should ensure that the ProcessRequestWorker is stopped before closing us here
            Assert.assertFalse(extantProcessBulkInData());

            // If there's any reader out there, wake them up and wait until they leave
            wakeReadBulkInData();
            spinWaitNoReadBulkInData();
            }
        }
    }
