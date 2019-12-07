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
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.configuration.LynxConstants;
import com.qualcomm.hardware.lynx.commands.LynxDatagram;
import com.qualcomm.hardware.lynx.commands.LynxInterfaceResponse;
import com.qualcomm.hardware.lynx.commands.LynxResponse;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.DcMotorController;

import java.nio.ByteBuffer;

/**
 * @see LynxSetMotorPIDControlLoopCoefficientsCommand
 * @see LynxGetMotorPIDControlLoopCoefficientsResponse
 */
public class LynxGetMotorPIDControlLoopCoefficientsCommand extends LynxDekaInterfaceCommand<LynxGetMotorPIDControlLoopCoefficientsResponse>
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    private final static int cbPayload = 2;

    private byte motor;
    private byte mode;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public LynxGetMotorPIDControlLoopCoefficientsCommand(LynxModuleIntf module)
        {
        super(module);
        this.response = new LynxGetMotorPIDControlLoopCoefficientsResponse(module);
        }

    public LynxGetMotorPIDControlLoopCoefficientsCommand(LynxModuleIntf module, int motorZ, DcMotor.RunMode mode)
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
        }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    public static Class<? extends LynxInterfaceResponse> getResponseClass()
        {
        return LynxGetMotorPIDControlLoopCoefficientsResponse.class;
        }

    @Override
    public boolean isResponseExpected()
        {
        return true;
        }

    @Override
    public byte[] toPayloadByteArray()
        {
        ByteBuffer buffer = ByteBuffer.allocate(cbPayload).order(LynxDatagram.LYNX_ENDIAN);
        buffer.put(this.motor);
        buffer.put(this.mode);
        return buffer.array();
        }

    @Override
    public void fromPayloadByteArray(byte[] rgb)
        {
        ByteBuffer buffer = ByteBuffer.wrap(rgb).order(LynxDatagram.LYNX_ENDIAN);
        this.motor = buffer.get();
        this.mode  = buffer.get();
        }
    }
