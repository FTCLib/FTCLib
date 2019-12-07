/*
Copyright (c) 2015 Robert Atkinson

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

package com.qualcomm.robotcore.robocol;

import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.TypeConversion;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RobocolParsableBase is an implementation base class for Robocol elements, providing
 * functionality that is common to all such elements.
 */
public abstract class RobocolParsableBase implements RobocolParsable
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected int   sequenceNumber;
    protected long  nanotimeTransmit;

    // Interval between retransmissions of a given one parsable
    protected static final long nanotimeTransmitInterval = 200L * ElapsedTime.MILLIS_IN_NANO;

    protected static AtomicInteger nextSequenceNumber = new AtomicInteger();

    /** A utility function that helps us separate driver station from robot controller packets */
    public static void initializeSequenceNumber(int sequenceNumber)
        {
        nextSequenceNumber = new AtomicInteger(sequenceNumber);
        }

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public RobocolParsableBase()
        {
        setSequenceNumber();
        nanotimeTransmit = 0;
        }

    //----------------------------------------------------------------------------------------------
    // RobocolParsable
    //----------------------------------------------------------------------------------------------

    @Override public int getSequenceNumber()
        {
        return this.sequenceNumber;
        }

    public void setSequenceNumber(short sequenceNumber)
        {
        // We only transmit sequence numbers as two byte values, but we maintain as unsigned
        // for easier human interpretation
        this.sequenceNumber = TypeConversion.unsignedShortToInt(sequenceNumber);
        }

    @Override public void setSequenceNumber()
        {
        setSequenceNumber((short)nextSequenceNumber.getAndIncrement());
        }

    /** Serialize, but also record timestamp if for transmission */
    @Override public byte[] toByteArrayForTransmission() throws RobotCoreException
        {
        byte[] result = toByteArray();
        this.nanotimeTransmit = System.nanoTime();
        return result;
        }

    @Override public boolean shouldTransmit(long nanotimeNow)
        {
        return this.nanotimeTransmit==0 || (nanotimeNow - this.nanotimeTransmit > nanotimeTransmitInterval);
        }

    //----------------------------------------------------------------------------------------------
    // Utility
    //----------------------------------------------------------------------------------------------

    protected ByteBuffer allocateWholeWriteBuffer(int overallSize)
        {
        return ByteBuffer.allocate(overallSize);
        }

    protected ByteBuffer getWholeReadBuffer(byte[] byteArray)
        {
        return ByteBuffer.wrap(byteArray);
        }

    protected ByteBuffer getWriteBuffer(int payloadSize)
        {
        ByteBuffer result = allocateWholeWriteBuffer(HEADER_LENGTH + payloadSize);
        //
        result.put(getRobocolMsgType().asByte());
        result.putShort((short)payloadSize);
        result.putShort((short)this.sequenceNumber);
        //
        return result;
        }

    protected ByteBuffer getReadBuffer(byte[] byteArray)
        {
        int cbHeaderWithoutSeqNum = HEADER_LENGTH - 2;
        ByteBuffer result = ByteBuffer.wrap(byteArray, cbHeaderWithoutSeqNum, byteArray.length - cbHeaderWithoutSeqNum);
        //
        setSequenceNumber(result.getShort());
        //
        return result;
        }
    }
