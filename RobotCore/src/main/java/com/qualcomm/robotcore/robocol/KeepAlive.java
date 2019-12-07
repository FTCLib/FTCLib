/*
 * Copyright (c) 2018, Craig MacFarlane
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
 * Neither the name of Craig MacFarlane nor the names of his contributors may be used to
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

package com.qualcomm.robotcore.robocol;

import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.RobotLog;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

/**
 * KeepAlive message
 * <p>
 * Not acked.  Used as a workaround for 2.4/5Ghz dual band Motorola phones that take an exceedingly long
 * time to do a wifi scan.
 */
@SuppressWarnings("unused,WeakerAccess")
public class KeepAlive extends RobocolParsableBase  {

    //------------------------------------------------------------------------------------------------
    // Sizing
    //------------------------------------------------------------------------------------------------

    public static final short BASE_PAYLOAD_SIZE = 1;

    protected int cbPayload()
    {
        return BASE_PAYLOAD_SIZE;
    }

    //------------------------------------------------------------------------------------------------
    // State
    //------------------------------------------------------------------------------------------------

    private long timestamp;
    private byte id;

    //------------------------------------------------------------------------------------------------
    // Construction
    //------------------------------------------------------------------------------------------------

    public KeepAlive()
    {
        timestamp = 0;
        id = 0;
    }

    public static KeepAlive createWithTimeStamp()
    {
        KeepAlive result = new KeepAlive();
        result.timestamp = System.nanoTime();
        return result;
    }

    //------------------------------------------------------------------------------------------------
    // Operations
    //------------------------------------------------------------------------------------------------

    /**
    * Number of seconds since KeepAlive was created
    * <p>
    * Device dependent, cannot compare across devices
    * @return elapsed time
    */
    public double getElapsedSeconds()
    {
        return (System.nanoTime() - timestamp) / (double)ElapsedTime.SECOND_IN_NANO;
    }

    /**
    * Get Robocol message type
    * @return RobocolParsable.MsgType.KEEPALIVE
    */
    @Override
    public MsgType getRobocolMsgType()
    {
        return MsgType.KEEPALIVE;
    }

    //------------------------------------------------------------------------------------------------
    // Serialization
    //------------------------------------------------------------------------------------------------

    /**
    * Convert this KeepAlive into a byte array
    */
    @Override
    public byte[] toByteArray() throws RobotCoreException
    {
        ByteBuffer buffer = getWriteBuffer(cbPayload());
        try {
            buffer.put(id);
        } catch (BufferOverflowException e) {
            RobotLog.logStackTrace(e);
        }
        return buffer.array();
    }

    /**
    * Populate this KeepAlive from a byte array.
    */
    @Override
    public void fromByteArray(byte[] byteArray) throws RobotCoreException
    {
        try {
            ByteBuffer byteBuffer = getReadBuffer(byteArray);
            id = byteBuffer.get();
        } catch (BufferUnderflowException e) {
            throw RobotCoreException.createChained(e, "incoming packet too small");
        }
    }

    //------------------------------------------------------------------------------------------------
    // Pretty Printing
    //------------------------------------------------------------------------------------------------

    @Override
    public String toString()
    {
        return String.format("KeepAlive - time: %d", timestamp);
    }

}
