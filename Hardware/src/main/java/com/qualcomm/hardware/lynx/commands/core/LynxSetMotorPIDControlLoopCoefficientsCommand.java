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
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.configuration.LynxConstants;
import com.qualcomm.hardware.lynx.commands.LynxDatagram;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.DcMotorController;

import java.nio.ByteBuffer;

/**
 * @see LynxGetMotorPIDControlLoopCoefficientsCommand
 */
public class LynxSetMotorPIDControlLoopCoefficientsCommand extends LynxDekaInterfaceCommand<LynxAck>
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    private final static int cbPayload = 14;

    private byte motor;
    private byte mode;
    private int  p;
    private int  i;
    private int  d;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public LynxSetMotorPIDControlLoopCoefficientsCommand(LynxModuleIntf module)
        {
        super(module);
        }

    public LynxSetMotorPIDControlLoopCoefficientsCommand(LynxModuleIntf module, int motorZ, DcMotor.RunMode mode, int p, int i, int d)
        {
        this(module);
        LynxConstants.validateMotorZ(motorZ);
        this.motor = (byte)motorZ;
        switch (mode)
            {
            case RUN_USING_ENCODER:    this.mode = 1; break;
            case RUN_TO_POSITION:      this.mode = 2; break;
            default: throw new IllegalArgumentException(String.format("illegal mode: %s", mode.toString()));
            }
        this.p = p;
        this.i = i;
        this.d = d;
        }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    public static int internalCoefficientFromExternal(double coefficient)
        {
        /*
        Coefficients are communicated as 32 bit signed integers representing scaled floating point values as follows:
        Coefficient 0.0617 would be represented as parameter value 4044 = 0.0617 * 65536.
        Coefficient -99.3 would be represented as parameter value -6507725 = -99.3 * 65536.
        Coefficient 0.0 would be represented simply as parameter value 0.
        */
        return (int)(Math.abs(coefficient) * 65536.0 + 0.5) * (int)Math.signum(coefficient);
        }

    public static double externalCoefficientFromInternal(int coefficient)
        {
        return ((double)coefficient) / 65536.0;
        }

    @Override
    public boolean isResponseExpected()
        {
        return false;
        }

    @Override
    public byte[] toPayloadByteArray()
        {
        ByteBuffer buffer = ByteBuffer.allocate(cbPayload).order(LynxDatagram.LYNX_ENDIAN);
        buffer.put(this.motor);
        buffer.put(this.mode);
        buffer.putInt(this.p);
        buffer.putInt(this.i);
        buffer.putInt(this.d);
        return buffer.array();
        }

    @Override
    public void fromPayloadByteArray(byte[] rgb)
        {
        ByteBuffer buffer = ByteBuffer.wrap(rgb).order(LynxDatagram.LYNX_ENDIAN);
        this.motor = buffer.get();
        this.mode  = buffer.get();
        this.p     = buffer.getInt();
        this.i     = buffer.getInt();
        this.d     = buffer.getInt();
        }
    }
