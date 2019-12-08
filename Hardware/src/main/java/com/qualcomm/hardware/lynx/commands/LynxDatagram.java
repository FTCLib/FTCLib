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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.qualcomm.hardware.lynx.LynxUnsupportedCommandException;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.util.TypeConversion;

import org.firstinspires.ftc.robotcore.internal.hardware.TimeWindow;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * A {@link LynxDatagram} represents the quantum of transmission of Lynx data between host
 * and controller module.
 */
@SuppressWarnings("WeakerAccess")
public class LynxDatagram
    {
    //----------------------------------------------------------------------------------------------
    // Constants
    //----------------------------------------------------------------------------------------------

    /**
     * All integral data is exchanged in 'little endian' format, least significant byte first (at
     * lowest address/offset.
     */
    public static final ByteOrder LYNX_ENDIAN = ByteOrder.LITTLE_ENDIAN;

    /** How much are the frame bytes and packet length accounted for in the overall packet length? */
    public static final int cbFrameBytesAndPacketLength = 4;

    /**
     * Two particular bytes identify the start of a valid Controller Module data packet
     */
    public static final byte[] frameBytes = new byte[] { 0x44, 0x4b };

    /**
     * Does the indicated data begin with the framing bytes?
     */
    public static boolean beginsWithFraming(byte[] data)
        {
        return data.length >= frameBytes.length && data[0] == frameBytes[0] && data[1] == frameBytes[1];
        }
    public static boolean beginsWithFraming(ByteBuffer buffer)
        {
        return buffer.get()==frameBytes[0] && buffer.get()==frameBytes[1];
        }

    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    /**
     * The Packet Length byte indicates the total size of the transmission, including Frame
     * Bytes and Checksum.
     */
    private short packetLength;

    /**
     * Every Controller Module in a system, regardless of where in the physical network topology it
     * is located, has a unique Module Address number. There may be up to 254 such modules (numbered
     * 1 ~ 254). Address 255 is reserved for broadcast and address zero is not valid.
     */
    private byte destModuleAddress;

    private byte sourceModuleAddress;

    /**
     * The Message Number is incremented for each new transmission from Host or Controller Module.
     * If an ACK is not received for any transmission within 100ms, the message should be re-transmitted
     * with the same Message Number to identify it as a re-transmission. It is anticipated that
     * messaging transactions are much faster than this limit. The limit exists to make transactions
     * bounded and provide for a reasonably prompt recovery from a transient failure.
     */
    private byte messageNumber;

    private byte referenceNumber;

    /**
     * This field indicates the purpose of the message. Any given packet type may or may not have
     * Payload Data. Response messages will have the same Command Number as the request command with
     * the addition of the 15th bit set.
     */
    private short packetId;

    /**
     * If a Command Code has Payload Data, that data is presented in this field
     */
    private byte[] payloadData;

    /**
     * A simple 8-bit (overflowing) checksum of the message packet bytes exclusive of the Frame
     * Bytes and the Checksum field itself.
     */
    private byte checksum;

    /**
     * If non-null, then this is the time window over which the payload of the datagram was received
     */
    private @Nullable TimeWindow payloadTimeWindow;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public LynxDatagram()
        {
        this.destModuleAddress = 0;
        this.sourceModuleAddress = 0;
        this.messageNumber = 0;
        this.referenceNumber = 0;
        this.packetId = 0;
        this.payloadData = new byte[0];
        }

    public LynxDatagram(LynxMessage command) throws LynxUnsupportedCommandException
        {
        this();

        int commandNumber = command.getCommandNumber();
        command.getModule().validateCommand(command);

        this.setDestModuleAddress(command.getDestModuleAddress());
        this.setMessageNumber(command.getMessageNumber());
        this.setReferenceNumber(command.getReferenceNumber());
        this.setPacketId(commandNumber);
        this.setPayloadData(command.toPayloadByteArray());
        }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    public void setPayloadTimeWindow(TimeWindow payloadTimeWindow)
        {
        this.payloadTimeWindow = payloadTimeWindow;
        }

    public @NonNull TimeWindow getPayloadTimeWindow()
        {
        return payloadTimeWindow == null ? new TimeWindow() : payloadTimeWindow;
        }

    public int getPacketLength()
        {
        return TypeConversion.unsignedShortToInt(this.packetLength);
        }
    public void setPacketLength(int value)
        {
        this.packetLength = (byte)value;
        }
    public static int getFixedPacketLength()
        {
        return 11;
        }
    public int updatePacketLength()
        {
        int cb = getFixedPacketLength() + payloadData.length;
        setPacketLength(cb);
        return cb;
        }

    public int getDestModuleAddress()
        {
        return TypeConversion.unsignedByteToInt(this.destModuleAddress);
        }
    public void setDestModuleAddress(int value)
        {
        this.destModuleAddress = (byte)value;
        }

    public int getSourceModuleAddress()
        {
        return TypeConversion.unsignedByteToInt(this.sourceModuleAddress);
        }
    public void setSourceModuleAddress(int value)
        {
        this.sourceModuleAddress = (byte)value;
        }

    public int getMessageNumber()
        {
        return TypeConversion.unsignedByteToInt(this.messageNumber);
        }
    public void setMessageNumber(int value)
        {
        this.messageNumber = (byte)value;
        }

    public int getReferenceNumber()
        {
        return TypeConversion.unsignedByteToInt(this.referenceNumber);
        }
    public void setReferenceNumber(int value)
        {
        this.referenceNumber = (byte)value;
        }

    public int getPacketId()
        {
        return TypeConversion.unsignedShortToInt(this.packetId);
        }
    public void setPacketId(int value)
        {
        this.packetId = (short)value;
        }
    public boolean isResponse()
        {
        return getPacketId() >= LynxResponse.RESPONSE_BIT;
        }

    /** Note that we clear the response bit. */
    public int getCommandNumber()
        {
        return getPacketId() & ~LynxResponse.RESPONSE_BIT;
        }

    public byte[] getPayloadData()
        {
        return this.payloadData;
        }
    public void setPayloadData(byte[] data)
        {
        this.payloadData = data;
        }

    public int getChecksum()
        {
        return TypeConversion.unsignedByteToInt(this.checksum);
        }
    public void setChecksum(int value)
        {
        this.checksum = (byte)value;
        }
    public byte computeChecksum()
        {
        byte result = 0;
        result = checksumBytes(result, frameBytes);
        result = checksumBytes(result, TypeConversion.shortToByteArray(this.packetLength, LYNX_ENDIAN));
        result += this.destModuleAddress;
        result += this.sourceModuleAddress;
        result += this.messageNumber;
        result += this.referenceNumber;
        result = checksumBytes(result, TypeConversion.shortToByteArray(this.packetId, LYNX_ENDIAN));
        result = checksumBytes(result, this.payloadData);
        return result;
        }
    private static byte checksumBytes(byte result, byte[] data)
        {
        for (int ib = 0; ib < data.length; ib++)
            {
            result += data[ib];
            }
        return result;
        }
    public boolean isChecksumValid()
        {
        return this.checksum == computeChecksum();
        }

    //----------------------------------------------------------------------------------------------
    // Transmission and reception
    //----------------------------------------------------------------------------------------------

    public byte[] toByteArray()
        {
        int cb = updatePacketLength();
        setChecksum(computeChecksum());

        ByteBuffer buffer = ByteBuffer.allocate(cb);
        buffer.order(LYNX_ENDIAN);

        buffer.put(frameBytes);
        buffer.putShort(this.packetLength);
        buffer.put(this.destModuleAddress);
        buffer.put(this.sourceModuleAddress);
        buffer.put(this.messageNumber);
        buffer.put(this.referenceNumber);
        buffer.putShort(this.packetId);
        buffer.put(this.payloadData);
        buffer.put(this.checksum);

        return buffer.array();
        }

    public void fromByteArray(byte[] byteArray) throws RobotCoreException
        {
        ByteBuffer buffer = ByteBuffer.wrap(byteArray);
        buffer.order(LYNX_ENDIAN);

        try {
            if (!beginsWithFraming(buffer)) throw illegalDatagram();
            this.packetLength  = buffer.getShort();
            this.destModuleAddress = buffer.get();
            this.sourceModuleAddress = buffer.get();
            this.messageNumber = buffer.get();
            this.referenceNumber = buffer.get();
            this.packetId = buffer.getShort();
            //
            int cbPayload = this.getPacketLength() - getFixedPacketLength();
            this.payloadData = new byte[cbPayload];
            buffer.get(this.payloadData);
            //
            this.checksum = buffer.get();
            }
        catch (BufferUnderflowException e)
            {
            throw RobotCoreException.createChained(e, "Lynx datagram buffer underflow");
            }
        }

    private RobotCoreException illegalDatagram()
        {
        return new RobotCoreException("illegal Lynx datagram format");
        }
    }
