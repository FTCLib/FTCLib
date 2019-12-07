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
package com.qualcomm.hardware.lynx.commands.core;

import com.qualcomm.hardware.lynx.LynxModuleIntf;
import com.qualcomm.hardware.lynx.commands.standard.LynxAck;
import com.qualcomm.robotcore.hardware.I2cAddr;
import com.qualcomm.robotcore.hardware.configuration.LynxConstants;
import com.qualcomm.hardware.lynx.commands.LynxDatagram;
import com.qualcomm.robotcore.util.TypeConversion;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by bob on 2016-03-09.
 */
public class LynxI2cWriteMultipleBytesCommand extends LynxDekaInterfaceCommand<LynxAck>
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public final static int cbFixed = 3;
    public final static int cbPayloadFirst = 1;
    public final static int cbPayloadLast  = 100;

    private byte i2cBus;
    private byte i2cAddr7Bit;
    private byte[] payload;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public LynxI2cWriteMultipleBytesCommand(LynxModuleIntf module)
        {
        super(module);
        }

    public LynxI2cWriteMultipleBytesCommand(LynxModuleIntf module, int busZ, I2cAddr i2cAddr, byte[] payload)
        {
        this(module);
        LynxConstants.validateI2cBusZ(busZ);
        if (payload.length < cbPayloadFirst || payload.length > cbPayloadLast) throw new IllegalArgumentException(String.format("illegal payload length: %d", payload.length));
        this.i2cBus      = (byte)busZ;
        this.i2cAddr7Bit = (byte)i2cAddr.get7Bit();
        this.payload     = Arrays.copyOf(payload, payload.length);
        }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    @Override
    public byte[] toPayloadByteArray()
        {
        ByteBuffer buffer = ByteBuffer.allocate(cbFixed + payload.length).order(LynxDatagram.LYNX_ENDIAN);
        buffer.put(this.i2cBus);
        buffer.put(this.i2cAddr7Bit);
        buffer.put((byte)payload.length);
        buffer.put(this.payload);
        return buffer.array();
        }

    @Override
    public void fromPayloadByteArray(byte[] rgb)
        {
        ByteBuffer buffer = ByteBuffer.wrap(rgb).order(LynxDatagram.LYNX_ENDIAN);
        this.i2cBus = buffer.get();
        this.i2cAddr7Bit = buffer.get();
        int cbPayload = TypeConversion.unsignedByteToInt(buffer.get());
        this.payload = new byte[cbPayload];
        buffer.get(this.payload);
        }
    }
