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
 * Created by bob on 2016-03-07.
 */
public class LynxSetMotorChannelModeCommand extends LynxDekaInterfaceCommand<LynxAck>
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public final int cbPayload = 3;

    private byte motor;
    private byte mode;
    private byte floatAtZero;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public LynxSetMotorChannelModeCommand(LynxModuleIntf module)
        {
        super(module);
        }

    public LynxSetMotorChannelModeCommand(LynxModuleIntf module, int motorZ, DcMotor.RunMode mode, DcMotor.ZeroPowerBehavior floatOrBrake)
        {
        this(module);
        LynxConstants.validateMotorZ(motorZ);
        this.motor = (byte)motorZ;
        switch (mode)
            {
            case RUN_WITHOUT_ENCODER:  this.mode = 0; break;
            case RUN_USING_ENCODER:    this.mode = 1; break;
            case RUN_TO_POSITION:      this.mode = 2; break;
            default: throw new IllegalArgumentException(String.format("illegal mode %s", mode.toString()));
            }
        this.floatAtZero = floatOrBrake== DcMotor.ZeroPowerBehavior.BRAKE ? (byte)0 : (byte)1;
        }

    //----------------------------------------------------------------------------------------------
    // Accessing
    //----------------------------------------------------------------------------------------------

    public DcMotor.RunMode getMode()
        {
        switch (mode)
            {
            default:
            case 0: return DcMotor.RunMode.RUN_WITHOUT_ENCODER;
            case 1: return DcMotor.RunMode.RUN_USING_ENCODER;
            case 2: return DcMotor.RunMode.RUN_TO_POSITION;
            }
        }

    public DcMotor.ZeroPowerBehavior getZeroPowerBehavior()
        {
        return floatAtZero==0 ? DcMotor.ZeroPowerBehavior.BRAKE : DcMotor.ZeroPowerBehavior.FLOAT;
        }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

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
        buffer.put(this.floatAtZero);
        return buffer.array();
        }

    @Override
    public void fromPayloadByteArray(byte[] rgb)
        {
        ByteBuffer buffer = ByteBuffer.wrap(rgb).order(LynxDatagram.LYNX_ENDIAN);
        this.motor = buffer.get();
        this.mode = buffer.get();
        this.floatAtZero = buffer.get();
        }

    }
