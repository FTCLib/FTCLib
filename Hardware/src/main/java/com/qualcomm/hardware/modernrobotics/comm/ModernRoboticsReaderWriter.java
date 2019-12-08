/*
 * Copyright (c) 2014, 2015, 2016 Qualcomm Technologies Inc
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
 * Neither the name of Qualcomm Technologies Inc nor the names of its contributors may be used to
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
 /*
Copyright (c) 2016-2017 Robert Atkinson

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

package com.qualcomm.hardware.modernrobotics.comm;

import android.annotation.SuppressLint;
import androidx.annotation.Nullable;

import com.qualcomm.robotcore.hardware.usb.RobotUsbDevice;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.internal.system.Deadline;
import org.firstinspires.ftc.robotcore.internal.hardware.TimeWindow;
import org.firstinspires.ftc.robotcore.internal.usb.exception.RobotUsbException;
import org.firstinspires.ftc.robotcore.internal.usb.exception.RobotUsbProtocolException;
import org.firstinspires.ftc.robotcore.internal.usb.exception.RobotUsbTimeoutException;
import org.firstinspires.ftc.robotcore.internal.usb.exception.RobotUsbTooManySequentialErrorsException;

import java.util.concurrent.TimeUnit;

@SuppressWarnings("WeakerAccess")
@SuppressLint("DefaultLocale")
public class ModernRoboticsReaderWriter
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "MRReaderWriter";
    public static boolean DEBUG = false;

    /**
     * The timeouts defined by the MR USB spec are subtle. Our historical timeout of 100ms
     * to read the header and the 100ms to read the payload was demonstrably insufficient, as teams
     * were hitting the payload timeout in the wild. Per the MR spec, the payload timeout should
     * be payload-length-dependent.
     *
     * Here's the skinny:
     *
     * The MR spec speaks thusly: "In order to detect a failure of too few Data payload bytes being
     * received, a timeout is used. This timeout is set to 10mS. This timeout will start as a soon
     * as the initial 0x55 is received. It is reset upon the arrival of each subsequent byte. [...]
     * A similar timeout process may be performed by the host. The host may also use a timeout of
     * 50mS for receipt of a response. If no response is received within 50mS, then it is assumed
     * that either the USB VCP communication link is non-operational, or the controller power is
     * not present"
     */
    public static int MS_INTER_BYTE_TIMEOUT            = 10;
    public static int MS_USB_HUB_LATENCY               = 2;    // this is probably way overkill
    public static int MS_REQUEST_RESPONSE_TIMEOUT      = 50 + MS_USB_HUB_LATENCY * 2;

    /**
     * We run on a garbage collected system. That doesn't run very often, and doesn't run back to
     * back, only one at a time, but when it does run it shuts things down for 15ms-40ms. One of
     * those can happen in the middle of any of our timeouts. So we need to allow for same.
     */
    public static int MS_GARBAGE_COLLECTION_SPURT      = 40;

    /** We made this up: we want to be very generous in trying to recover from failures as we
     * read through possibly-old data trying to synchronize, and there's little harm in doing so */
    public static int MS_RESYNCH_TIMEOUT               = 1000;

    public static int MS_FAILURE_WAIT                  = 40;   // per email from MR
    public static int MS_COMM_ERROR_WAIT               = 100;  // historical. we made this up
    public static int MS_MAX_TIMEOUT                   = 100;  // we made this up.

    public static int MAX_SEQUENTIAL_USB_ERROR_COUNT   = 5;    // we made this up. Used to be 10, but can tighten with the better sync logic we now have

    public final static String COMM_FAILURE_READ  = "comm failure read";
    public final static String COMM_FAILURE_WRITE = "comm failure write";
    public final static String COMM_TIMEOUT_READ  = "comm timeout awaiting response (read)";
    public final static String COMM_TIMEOUT_WRITE = "comm timeout awaiting response (write)";
    public final static String COMM_ERROR_READ    = "comm error read";
    public final static String COMM_ERROR_WRITE   = "comm error write";
    public final static String COMM_SYNC_LOST     = "comm sync lost";
    public final static String COMM_PAYLOAD_ERROR_READ    = "comm payload error read";
    public final static String COMM_PAYLOAD_ERROR_WRITE   = "comm payload error write";
    public final static String COMM_TYPE_ERROR_READ    = "comm type error read";
    public final static String COMM_TYPE_ERROR_WRITE   = "comm type error write";

    protected final RobotUsbDevice  device;
    protected int                   usbSequentialCommReadErrorCount = 0;
    protected int                   usbSequentialCommWriteErrorCount = 0;
    protected int                   usbReadRetryCount = 4;
    protected int                   usbWriteRetryCount = 4;
    protected int                   msUsbReadRetryInterval = 20;
    protected int                   msUsbWriteRetryInterval = 20;
    protected boolean               isSynchronized = false;
    protected Deadline              responseDeadline = new Deadline(MS_RESYNCH_TIMEOUT, TimeUnit.MILLISECONDS);
    protected ModernRoboticsDatagram.AllocationContext<ModernRoboticsRequest>  requestAllocationContext  = new ModernRoboticsDatagram.AllocationContext<ModernRoboticsRequest>();
    protected ModernRoboticsDatagram.AllocationContext<ModernRoboticsResponse> responseAllocationContext = new ModernRoboticsDatagram.AllocationContext<ModernRoboticsResponse>();

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public ModernRoboticsReaderWriter(RobotUsbDevice device)
        {
        this.device = device;
        this.device.setDebugRetainBuffers(true);
        }

    public void throwIfTooManySequentialCommErrors() throws RobotUsbTooManySequentialErrorsException
        {
        if (device.isOpen())
            {
            if (this.usbSequentialCommReadErrorCount > MAX_SEQUENTIAL_USB_ERROR_COUNT || this.usbSequentialCommWriteErrorCount > MAX_SEQUENTIAL_USB_ERROR_COUNT)
                {
                throw new RobotUsbTooManySequentialErrorsException("%s: too many sequential USB comm errors on device", device.getSerialNumber());
                }
            }
        }

    public void close()
        {
        this.device.close();
        }

    //----------------------------------------------------------------------------------------------
    // Reading and writing
    //----------------------------------------------------------------------------------------------

    public void read(boolean retry, int address, byte[] buffer, @Nullable TimeWindow payloadTimeWindow) throws RobotUsbException, InterruptedException
        {
        if (DEBUG) RobotLog.vv(TAG, "%s: read(addr=%d cb=%d)", device.getSerialNumber(), address, buffer.length);

        RobotUsbException exception = null;

        for (int i = 0; i < usbReadRetryCount; i++)
            {
            if (i > 0)
                {
                RobotLog.ee(TAG, "%s: retry #%d read(addr=%d cb=%d)", device.getSerialNumber(), i, address, buffer.length);
                }

            try {
                readOnce(address, buffer, payloadTimeWindow);
                }
            catch (RobotUsbException e)
                {
                // read failed
                if (!this.device.isOpen())
                    {
                    return; // silent
                    }
                else if (!retry)
                    {
                    RobotLog.ee(TAG, "%s: ignoring failed read(addr=%d cb=%d)", device.getSerialNumber(), address, buffer.length);
                    return; // ignore failure if we're not asked to retry
                    }

                Thread.sleep(msUsbReadRetryInterval);   // always sleep in order to give MR device chance to write to its FTDI chip before we reset same
                this.device.resetAndFlushBuffers();
                exception = e;
                continue;
                }

            // read succeeded
            return;
            }

        if (exception != null) throw exception;
        }

    protected void readOnce(int address, byte[] buffer, @Nullable TimeWindow payloadTimeWindow) throws RobotUsbException, InterruptedException
        {
        ModernRoboticsRequest request = null;
        ModernRoboticsResponse response = null;
        try {
            // Create and send a read request
            request = ModernRoboticsRequest.newInstance(requestAllocationContext, 0);
            request.setRead(0);
            request.setAddress(address);
            request.setPayloadLength(buffer.length);
            this.device.write(request.data);

            try {
                // Read the response
                response = readResponse(request, payloadTimeWindow);
                if (response.isFailure())
                    {
                    // In order to avoid having the MR firmware think we might be about to try to update
                    // it with a new version, we wait a prescribed delay per MR information
                    Thread.sleep(MS_FAILURE_WAIT);
                    logAndThrowProtocol(request, response, COMM_FAILURE_READ);
                    }
                else if (response.isRead()
                        && response.getFunction()==0
                        && response.getAddress()==address
                        && response.getPayloadLength()==buffer.length)
                    {
                    // Success! All is well.
                    this.usbSequentialCommReadErrorCount = 0;
                    System.arraycopy(response.data, ModernRoboticsDatagram.CB_HEADER, buffer, 0, buffer.length);
                    }
                else
                    {
                    // Historical code had a sleep here. We don't believe that it's actually
                    // necessary, but it's harmless, so we keep it (for now) as it's harmless
                    // and only very rarely happens.
                    Thread.sleep(MS_COMM_ERROR_WAIT);
                    logAndThrowProtocol(request, response, COMM_ERROR_READ);
                    }
                }
            catch (RobotUsbTimeoutException e)
                {
                // In *some cases* historical code had a sleep here, too, namely the 'incorrect sync
                // bytes' case. In the present code paths, that ends up timing out instead, as our
                // readResponse() intrinsically synchronizes. But we add the sleep here for good measure
                // as it's harmless, and only very rarely happens.
                Thread.sleep(MS_COMM_ERROR_WAIT);
                logAndRethrowTimeout(e, request, timeoutMessage(COMM_TIMEOUT_READ, e));
                }
            }
        catch (RobotUsbException e)
            {
            ++usbSequentialCommReadErrorCount;
            throw e;
            }
        finally
            {
            // proactively reclaim instances
            if (response != null) response.close();
            if (request != null) request.close();
            }
        }

    public void write(int address, byte[] buffer) throws RobotUsbException, InterruptedException
        {
        if (DEBUG) RobotLog.vv(TAG, "%s: write(addr=%d cb=%d)", device.getSerialNumber(), address, buffer.length);

        // Retry a handful of times before giving up. The thing we have to deal with most here
        // is USB transmission errors, which tend in our experience to be intermittent and not
        // bursty, so retrying a couple of times seems worthwhile.
        //
        // Note that the correctness here of doing the write relies on the fact that writes
        // are idempotent : some of the writes that appear to fail might instead actually succeed
        // w/o our knowing, and there's nothing we can do about that.
        //
        RobotUsbException exception = null;

        for (int i = 0; i < usbWriteRetryCount; i++)
            {
            if (i > 0)
                {
                RobotLog.ee(TAG, "%s: retry #%d write(addr=%d cb=%d)", device.getSerialNumber(), i, address, buffer.length);
                }

            try {
                writeOnce(address, buffer);
                }
            catch (RobotUsbException e)
                {
                // write failed
                if (!this.device.isOpen())
                    {
                    return; // silent
                    }

                Thread.sleep(msUsbWriteRetryInterval);  // same comment as in read()
                this.device.resetAndFlushBuffers();
                exception = e;
                continue;
                }

            // write succeeded
            return;
            }

        // If that didn't fix things, then fail : the write will (presumably) change
        // state if it succeeds, so we need to eithe make the change happen or let the
        // caller know that it won't
        if (exception != null) throw exception;
        }

    protected void writeOnce(int address, byte[] buffer) throws RobotUsbException, InterruptedException
        {
        ModernRoboticsRequest request = null;
        ModernRoboticsResponse response = null;
        try {
            // Create and send a write request
            request = ModernRoboticsRequest.newInstance(requestAllocationContext, buffer.length);
            request.setWrite(0);
            request.setAddress(address);
            request.setPayload(buffer);
            this.device.write(request.data);

            try {
                // Read the response
                response = readResponse(request, null);
                if (response.isFailure())
                    {
                    // In order to avoid having the MR firmware think we might be about to try to update
                    // it with a new version, we wait a prescribed delay per MR information
                    Thread.sleep(MS_FAILURE_WAIT);
                    this.logAndThrowProtocol(request, response, COMM_FAILURE_WRITE);
                    }
                else if (response.isWrite()
                        && response.getFunction()==0
                        && response.getAddress()==address
                        && response.getPayloadLength()==0)
                    {
                    // All is well
                    this.usbSequentialCommWriteErrorCount = 0;
                    }
                else
                    {
                    // Historical code had a sleep here. We don't believe that it's actually
                    // necessary, but it's harmless, so we keep it (for now) as it's harmless
                    // and only very rarely happens.
                    Thread.sleep(MS_COMM_ERROR_WAIT);
                    this.logAndThrowProtocol(request, response, COMM_ERROR_WRITE);
                    }
                }
            catch (RobotUsbTimeoutException e)
                {
                // In *some cases* historical code had a sleep here, too, namely the 'incorrect sync
                // bytes' case. In the present code paths, that ends up timing out instead, as our
                // readResponse() intrinsically synchronizes. But we add the sleep here for good measure
                // as it's harmless, and only very rarely happens.
                Thread.sleep(MS_COMM_ERROR_WAIT);
                this.logAndRethrowTimeout(e, request, timeoutMessage(COMM_TIMEOUT_WRITE, e));
                }
            }
        catch (RobotUsbException e)
            {
            ++this.usbSequentialCommWriteErrorCount;
            throw e;
            }
        finally
            {
            if (response != null) response.close();
            if (request != null) request.close();
            }
        }

    //----------------------------------------------------------------------------------------------
    // Response reading
    //----------------------------------------------------------------------------------------------

    protected ModernRoboticsResponse readResponse(ModernRoboticsRequest request, @Nullable TimeWindow payloadTimeWindow) throws RobotUsbException, InterruptedException
        {
        responseDeadline.reset();
        while (!responseDeadline.hasExpired())
            {
            // Synchronization bytes by their nature occur only at the start of USB packets. So if
            // we're not currently there, then we can't possibly be synchronized.
            if (!device.mightBeAtUsbPacketStart())
                {
                isSynchronized = false;
                }

            // Read the header, synchronizing carefully if we need to
            ModernRoboticsResponse header = ModernRoboticsResponse.newInstance(responseAllocationContext, 0);
            try {
                if (!isSynchronized)
                    {
                    byte[] singleByte = new byte[1];
                    byte[] headerSuffix = new byte[ModernRoboticsDatagram.CB_HEADER-2];

                    // Synchronization bytes by their nature occur only at the start of USB packets.
                    // So if we know we're not at such a boundary, skip along until the next one.
                    device.skipToLikelyUsbPacketStart();

                    // Synchronize by looking for the first synchronization byte
                    if (readSingleByte(singleByte, MS_REQUEST_RESPONSE_TIMEOUT, null, "sync0") != ModernRoboticsResponse.syncBytes[0])
                        {
                        continue;
                        }

                    // Having found the first, if we don't next see the second, then go back to looking for the first
                    if (readSingleByte(singleByte, 0, null, "sync1") != ModernRoboticsResponse.syncBytes[1])
                        {
                        continue;
                        }

                    // Read the remaining header bytes
                    readIncomingBytes(headerSuffix, 0, headerSuffix.length, 0, null, "syncSuffix");

                    // Assemble the header from the pieces
                    System.arraycopy(ModernRoboticsResponse.syncBytes, 0, header.data, 0, 2);
                    System.arraycopy(headerSuffix,                     0, header.data, 2, headerSuffix.length);
                    }
                else
                    {
                    readIncomingBytes(header.data, 0, header.data.length, MS_REQUEST_RESPONSE_TIMEOUT, null, "header");
                    if (!header.syncBytesValid())
                        {
                        // We've lost synchronization, yet we received *some* data. Thus, since there
                        // is no retransmission in the USB protocol, we're never going to get the response
                        // that we were looking for. So get out of Dodge with an error.
                        logAndThrowProtocol(request, header, COMM_SYNC_LOST);
                        }
                    }

                // Make sure the request and response types match
                if (!header.isFailure())
                    {
                    if (request.isRead() != header.isRead() || request.getFunction() != header.getFunction())
                        {
                        logAndThrowProtocol(request, header, request.isWrite() ? COMM_TYPE_ERROR_WRITE: COMM_TYPE_ERROR_READ);
                        }
                    }

                // How big of a payload is expected in the response? It should match the expectations
                // set in the request. If it doesn't, get out of Dodge.
                int cbPayloadExpected = header.isFailure()
                        ? 0
                        : request.isWrite()
                            ? 0
                            : request.getPayloadLength();
                if (cbPayloadExpected != header.getPayloadLength())
                    {
                    // We're out of sync some how
                    logAndThrowProtocol(request, header, request.isWrite() ? COMM_PAYLOAD_ERROR_WRITE: COMM_PAYLOAD_ERROR_READ);
                    }

                // Assemble a response and read the payload data thereinto
                ModernRoboticsResponse result = ModernRoboticsResponse.newInstance(responseAllocationContext, header.getPayloadLength());
                System.arraycopy(header.data, 0, result.data, 0, header.data.length);
                readIncomingBytes(result.data, header.data.length, header.getPayloadLength(), 0, payloadTimeWindow, "payload");

                // We're ok to go more quickly the next time
                isSynchronized = true;
                return result;
                }
            finally
                {
                if (header != null) header.close();
                }
            }

        throw new RobotUsbTimeoutException(responseDeadline.startTimeNanoseconds(), "timeout waiting %d ms for response", responseDeadline.getDuration(TimeUnit.MILLISECONDS));
        }

    protected void readIncomingBytes(byte[] buffer, int ibFirst, int cbToRead, int msExtraTimeout, @Nullable TimeWindow timeWindow, String debugContext) throws RobotUsbException, InterruptedException
        {
        if (cbToRead > 0)
            {
            // In theory, per the MR spec, we might see MS_INTER_BYTE_TIMEOUT elapse
            // between each incoming byte. For a long-ish read response, that can amount
            // to some significant time: we routinely read 31 byte swaths of data on the
            // motor controller, for example, which would amount to nearly a third of a second
            // to see the response. Now that doesn't happen in practice: most inter-byte intervals
            // are actually much smaller than permitted. We could in theory do the MS_INTER_BYTE_TIMEOUT
            // timeouts on each individual byte, but that's cumbersome and inefficient given the read
            // API we have to work with. So instead we specify the pessimistic maximum, plus a little
            // extra to help avoid false hits, and thus see any communications failure a little
            // later than we otherwise would see (but not ridiculously later).
            //
            // Additionally, we allow for any extra timeout duration that has to do with other protocol
            // aspects unrelated to inter-byte timing.
            //
            // Finally, we allow for a garbage collection.
            //
            long msReadTimeout = MS_INTER_BYTE_TIMEOUT * (cbToRead + 2 /* just being generous */) + msExtraTimeout + MS_GARBAGE_COLLECTION_SPURT;
            //
            // That all said, that leads to rediculously long timeouts. We got by for a long time with
            // a fixed 100ms timeout, and the retry logic above will help us even more. So we prune that
            // down
            //
            msReadTimeout = Math.min(msReadTimeout, MS_MAX_TIMEOUT);

            long nsTimerStart = System.nanoTime();
            int cbRead = device.read(buffer, ibFirst, cbToRead, msReadTimeout, timeWindow);
            if (cbRead == cbToRead)
                {
                // We got all the data we came for. Just return gracefully
                }
            else if (cbRead == 0)
                {
                // Couldn't read all the data in the time allotted.
                throw new RobotUsbTimeoutException(nsTimerStart, "%s: unable to read %d bytes in %d ms", debugContext, cbToRead, msReadTimeout);
                }
            else
                {
                // An unexpected error occurred. We'll classify as a protocol error as a good guess, as
                // we did get *some* data, just not all we were expecting.
                logAndThrowProtocol("readIncomingBytes(%s) cbToRead=%d cbRead=%d", debugContext, cbToRead, cbRead);
                }
            }
        }

    protected byte readSingleByte(byte[] buffer, int msExtraTimeout, @Nullable TimeWindow timeWindow, String debugContext) throws RobotUsbException, InterruptedException
        {
        readIncomingBytes(buffer, 0, 1, msExtraTimeout, timeWindow, debugContext);
        return buffer[0];
        }

    //----------------------------------------------------------------------------------------------
    // Utility
    //----------------------------------------------------------------------------------------------

    protected String timeoutMessage(String root, RobotUsbTimeoutException e)
        {
        return String.format("%s: %s", root, e.getMessage());
        }

    protected void doExceptionBookkeeping()
        {
        isSynchronized = false;
        }

    protected void logAndRethrowTimeout(RobotUsbTimeoutException e, ModernRoboticsRequest request, String message) throws RobotUsbTimeoutException
        {
        ModernRoboticsRequest requestHeader = ModernRoboticsRequest.newInstance(requestAllocationContext, 0);  // Don't log the payload data
        System.arraycopy(request.data, 0, requestHeader.data, 0, requestHeader.data.length);
        //
        RobotLog.ee(TAG, "%s: %s request=%s", device.getSerialNumber(), message, bufferToString(requestHeader.data));
        device.logRetainedBuffers(e.nsTimerStart, e.nsTimerExpire, TAG, "recent data on %s", device.getSerialNumber());
        doExceptionBookkeeping();
        throw e;
        }

    protected void logAndThrowProtocol(String format, Object... args) throws RobotUsbProtocolException
        {
        String message = String.format(format, args);
        RobotLog.ee(TAG, "%s: %s", device.getSerialNumber(), message);
        doExceptionBookkeeping();
        throw new RobotUsbProtocolException(message);
        }

    protected void logAndThrowProtocol(ModernRoboticsRequest request, ModernRoboticsResponse response, String message) throws RobotUsbProtocolException
        {
        ModernRoboticsRequest requestHeader = ModernRoboticsRequest.newInstance(requestAllocationContext, 0);  // Don't log the payload data
        System.arraycopy(request.data, 0, requestHeader.data, 0, requestHeader.data.length);
        //
        ModernRoboticsResponse responseHeader = ModernRoboticsResponse.newInstance(responseAllocationContext, 0);  // Don't log the payload data
        System.arraycopy(response.data, 0, responseHeader.data, 0, responseHeader.data.length);
        //
        RobotLog.ee(TAG, "%s: %s: request:%s response:%s", device.getSerialNumber(), message, bufferToString(requestHeader.data), bufferToString(responseHeader.data));
        doExceptionBookkeeping();
        throw new RobotUsbProtocolException(message);
        }

    protected static String bufferToString(byte[] buffer)
        {
        StringBuilder result = new StringBuilder();
        result.append("[");
        if (buffer.length > 0)
            {
            result.append(String.format("%02x", buffer[0]));
            }

        int cbMax = 16;
        int cb = Math.min(buffer.length, cbMax);

        for (int ib = 1; ib < cb; ++ib)
            {
            result.append(String.format(" %02x", buffer[ib]));
            }

        if (cb < buffer.length)
            {
            result.append(" ...");
            }

        result.append("]");
        return result.toString();
        }
    }
