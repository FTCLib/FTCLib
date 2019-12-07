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
package com.qualcomm.hardware.lynx.commands;

import com.qualcomm.hardware.lynx.LynxUnsupportedCommandException;
import com.qualcomm.hardware.lynx.LynxModuleIntf;
import com.qualcomm.hardware.lynx.LynxNackException;
import com.qualcomm.hardware.lynx.commands.standard.LynxAck;
import com.qualcomm.hardware.lynx.commands.standard.LynxNack;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.internal.hardware.TimeWindow;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A LynxRespondable is a message that will generate a response from the module to which it is
 * transmitted. A positive response is either a full-fledged response message (if the message
 * expects a response) or a LynxAck if not; a negative response is in the form of a LynxNack.
 */
@SuppressWarnings("WeakerAccess")
public abstract class LynxRespondable<RESPONSE extends LynxMessage> extends LynxMessage
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected boolean        isAckOrResponseReceived;
    protected LynxNack       nackReceived;
    protected CountDownLatch ackOrNackReceived;
    protected int            retransmissionsRemaining;
    protected CountDownLatch responseOrNackReceived;
    protected RESPONSE       response;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public LynxRespondable(LynxModuleIntf module)
        {
        super(module);
        this.isAckOrResponseReceived = false;
        this.nackReceived = null;
        this.ackOrNackReceived = new CountDownLatch(1);
        this.retransmissionsRemaining = 5;
        this.responseOrNackReceived = new CountDownLatch(1);
        this.response = null;
        }

    //----------------------------------------------------------------------------------------------
    // Transmission
    //----------------------------------------------------------------------------------------------

    @Override
    public void onPretendTransmit() throws InterruptedException
        {
        super.onPretendTransmit();
        this.pretendFinish();
        }

    public boolean isRetransmittable()
        {
        return this.retransmissionsRemaining > 0;
        }

    public void setUnretransmittable()
        {
        this.retransmissionsRemaining = 0;
        }

    @Override
    public void noteRetransmission()
        {
        this.retransmissionsRemaining--;
        if (this.retransmissionsRemaining < 0) this.retransmissionsRemaining = 0;
        }

    //----------------------------------------------------------------------------------------------
    // Accessors
    //----------------------------------------------------------------------------------------------

    public boolean hasBeenAcknowledged()
        {
        return this.isAckOrResponseReceived() || this.isNackReceived();
        }
    public boolean isAckOrResponseReceived()
        {
        return this.isAckOrResponseReceived;
        }
    public boolean isNackReceived()
        {
        return this.nackReceived != null;
        }
    public LynxNack getNackReceived()
        {
        return this.nackReceived;
        }
    @Override
    public boolean isAckable()
        {
        return true;
        }

    //----------------------------------------------------------------------------------------------
    // Completions
    //----------------------------------------------------------------------------------------------

    public void pretendFinish() throws InterruptedException
        {
        // Pretend we got an ack or response
        this.isAckOrResponseReceived = true;

        if (isResponseExpected())
            {
            /*
             * We'll just use the (phony) response object with which we initialized ourselves
             * Fix up our time window.  It was initialized to null.
             */
            this.response.setPayloadTimeWindow(new TimeWindow());
            onResponseReceived();
            }

        // Finish bookkeeping
        if (this.module != null) this.module.finishedWithMessage(this);

        // Wake up waiters
        this.ackOrNackReceived.countDown();
        }

    // called on datagram receive thread
    public void onAckReceived(LynxAck ack)
        {
        if (!this.isAckOrResponseReceived)    // paranoia
            {
            this.isAckOrResponseReceived = true;
            if (ack.isAttentionRequired())
                {
                this.noteAttentionRequired();
                }
            this.ackOrNackReceived.countDown();
            }
        }

    protected void noteAttentionRequired()
        {
        this.module.noteAttentionRequired();
        }

    // Called on the datagram receive thread
    public void onResponseReceived(LynxMessage response)
        {
        this.response = (RESPONSE)response;
        this.onResponseReceived();
        }

    // Called internally, here
    private void onResponseReceived()
        {
        if (isResponseExpected())
            {
            this.isAckOrResponseReceived = true;
            this.responseOrNackReceived.countDown();
            }
        else
            {
            RobotLog.e("internal error: unexpected response received for msg#=%d", this.getMessageNumber());
            }
        }

    // called on the datagram receive thread, or internally, here
    public void onNackReceived(LynxNack nack)
        {
        // Avoid logging 'expected' nacks
        switch (nack.getNackReasonCode())
            {
            case COMMAND_IMPL_PENDING:
            case I2C_NO_RESULTS_PENDING:
            case I2C_OPERATION_IN_PROGRESS:
                break;
            default:
                RobotLog.v("nack rec'd mod=%d msg#=%d ref#=%d reason=%s:%d", this.getModuleAddress(), this.getMessageNumber(), this.getReferenceNumber(), nack.getNackReasonCode().toString(), nack.getNackReasonCode().getValue());
                break;
            }
        this.nackReceived = nack;
        this.ackOrNackReceived.countDown();
        this.responseOrNackReceived.countDown();
        }

    //----------------------------------------------------------------------------------------------
    // Waits
    //----------------------------------------------------------------------------------------------

    public void send() throws InterruptedException, LynxNackException
        {
        acquireNetworkLock();
        try {
            try {
                this.module.sendCommand(this);
                }
            catch (LynxUnsupportedCommandException e)
                {
                // The module doesn't actually support this command, as it has an older sense of some interface,
                // or doesn't know about the interface at all. Act like we got a nack from the module
                throwNackForUnsupportedCommand(e);
                }
            awaitAckResponseOrNack();
            throwIfNack();
            }
        finally
            {
            releaseNetworkLock();
            }
        }

    public RESPONSE sendReceive() throws InterruptedException, LynxNackException
        {
        acquireNetworkLock();
        try {
            try {
                this.module.sendCommand(this);
                awaitAckResponseOrNack();
                return responseOrThrow();
                }
            catch (LynxNackException e)
                {
                if (e.getNack().getNackReasonCode().isUnsupportedReason())
                    {
                    // The module SAID he supported this command, but it turns out he didn't (liar).
                    // Deal with it as if he had told us in the first place that he hadn't supported it.
                    if (usePretendResponseIfRealModuleDoesntSupport() && this.response != null)
                        {
                        return this.response;
                        }
                    }
                throw e;
                }
            catch (LynxUnsupportedCommandException e)
                {
                // The module doesn't actually support this command, as it has an older sense of some interface.
                // Return the default response for the command, if any; otherwise, act like we got a nack from the module.
                if (usePretendResponseIfRealModuleDoesntSupport() && this.response != null)
                    {
                    return this.response;
                    }
                throwNackForUnsupportedCommand(e);
                return null;    // not reached
                }
            }
        finally
            {
            releaseNetworkLock();
            }
        }

    /**
     * Command normally pre-create responses that get used when the usb device is in pretend mode.
     * Normally, those responses are *only* used in pretend mode. However, on a case-by-case basis
     * those pretend responses can *also* be used when armed in the situation where the module in
     * question doesn't in fact support the command (perhaps it has an older, shorter notion of a
     * particular interface, for example).
     */
    protected boolean usePretendResponseIfRealModuleDoesntSupport()
        {
        return false;
        }

    protected void throwNackForUnsupportedCommand(LynxUnsupportedCommandException e) throws LynxNackException
        {
        this.nackReceived = new LynxNack(this.getModule(), LynxNack.ReasonCode.PACKET_TYPE_ID_UNKNOWN);
        throw new LynxNackException(this, "%s: command %s(#0x%04x) not supported by mod#=%d",
                this.getClass().getSimpleName(),
                e.getClazz().getSimpleName(),
                e.getCommandNumber(),
                this.getModuleAddress());
        }

    protected RESPONSE responseOrThrow() throws LynxNackException
        {
        if (this.isNackReceived())
            throw new LynxNackException(this, "%s: nack received: %s:%d",
                    this.getClass().getSimpleName(),
                    this.nackReceived.getNackReasonCode().toString(),
                    this.nackReceived.getNackReasonCode().getValue());
        return this.response;
        }

    protected void throwIfNack() throws LynxNackException
        {
        if (this.isNackReceived())
            throw new LynxNackException(this, "%s: nack received: %s:%d",
                    this.getClass().getSimpleName(),
                    this.nackReceived.getNackReasonCode().toString(),
                    this.nackReceived.getNackReasonCode().getValue());
        }

    protected int getMsAwaitInterval()
        {
        return 250;
        }

    protected int getMsRetransmissionInterval()
        {
        return 100;
        }

    protected void awaitAndRetransmit(CountDownLatch latch, LynxNack.ReasonCode nackCode, String message) throws InterruptedException
        {
        final long nsDeadline = System.nanoTime() + getMsAwaitInterval() * ElapsedTime.MILLIS_IN_NANO;
        final int msWaitInterval = getMsAwaitInterval();
        final int msRetransmit = getMsRetransmissionInterval();

        for (;;)
            {
            long nsRemaining = nsDeadline - System.nanoTime();
            if (nsRemaining <= 0)
                {
                // Timed out. Pretend we got a nack.
                RobotLog.v("timeout: abandoning waiting %dms for %s: cmd=%s mod=%d msg#=%d", msWaitInterval, message, this.getClass().getSimpleName(), this.getModuleAddress(), this.getMessageNumber());
                this.onNackReceived(new LynxNack(this.module, nackCode));
                return;
                }

            int  msRemaining = (int)(nsRemaining / ElapsedTime.MILLIS_IN_NANO);
            int  msWait      = Math.min(msRemaining, msRetransmit);

            if (latch.await(msWait, TimeUnit.MILLISECONDS))
                {
                // all is well
                return;
                }

            // Retransmit
            this.module.retransmit(this);
            }
        }

    protected void awaitAckResponseOrNack() throws InterruptedException
        {
        if (this.isResponseExpected())
            {
            awaitAndRetransmit(this.responseOrNackReceived, LynxNack.ReasonCode.ABANDONED_WAITING_FOR_RESPONSE, "response");
            }
        else
            {
            awaitAndRetransmit(this.ackOrNackReceived, LynxNack.ReasonCode.ABANDONED_WAITING_FOR_ACK, "ack");
            }
        }
    }
