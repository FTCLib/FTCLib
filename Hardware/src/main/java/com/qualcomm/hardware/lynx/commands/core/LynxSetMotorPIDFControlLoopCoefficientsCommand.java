/*
Copyright (c) 2018 Robert Atkinson

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
import com.qualcomm.hardware.lynx.commands.LynxDatagram;
import com.qualcomm.hardware.lynx.commands.standard.LynxAck;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.MotorControlAlgorithm;
import com.qualcomm.robotcore.hardware.configuration.LynxConstants;

import java.nio.ByteBuffer;

/**
 * @see LynxGetMotorPIDControlLoopCoefficientsCommand
 */
public class LynxSetMotorPIDFControlLoopCoefficientsCommand extends LynxDekaInterfaceCommand<LynxAck>
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    /* typedef enum // one-byte in size
        {
           MotorControlAlgorithmFirst,
           MotorControlAlgorithmLegacyPID=MotorControlAlgorithmFirst,
           MotorControlAlgorithmPIDF,
           MotorControlAlgorithmMax,
           MotorControlAlgorithmNotSet=0xFF,
        } ...  */
    public enum InternalMotorControlAlgorithm
        {
        First(0), LegacyPID(0), PIDF(1), Max(2), NotSet(0xff);

        private byte value;

        public byte getValue() { return value; }

        InternalMotorControlAlgorithm(int value) { this.value = (byte)value; }

        public static InternalMotorControlAlgorithm fromExternal(com.qualcomm.robotcore.hardware.MotorControlAlgorithm algorithm)
            {
            switch (algorithm)
                {
                default:        return NotSet;
                case LegacyPID: return LegacyPID;
                case PIDF:      return PIDF;
                }
            }

        public static InternalMotorControlAlgorithm fromByte(byte bVal)
            {
            if (bVal == LegacyPID.getValue()) return LegacyPID;
            if (bVal == PIDF.getValue()) return PIDF;
            return NotSet;
            }

        public MotorControlAlgorithm toExternal()
            {
            switch (this)
                {
                default:        return MotorControlAlgorithm.Unknown;
                case LegacyPID: return MotorControlAlgorithm.LegacyPID;
                case PIDF:      return MotorControlAlgorithm.PIDF;
                }
            }
        }

    /*  typedef struct
    {
        uint8_t channel;
        uint8_t motorMode;
        int32_t proportional_16q16; // signed
        int32_t integral_16q16;     // signed
        int32_t derivative_16q16;   // signed
        int32_t feedforward_16q16;  // signed
        uint8_t motorControlAlgorithm; // see MotorControlAlgorithm
    } ... */
    private final static int cbPayload = 2 + 4*4 + 1;

    private byte motor; // aka channel
    private byte mode;
    private int  p;
    private int  i;
    private int  d;
    private int  f;
    private byte motorControlAlgorithm;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public LynxSetMotorPIDFControlLoopCoefficientsCommand(LynxModuleIntf module)
        {
        super(module);
        }

    public LynxSetMotorPIDFControlLoopCoefficientsCommand(LynxModuleIntf module, int motorZ, DcMotor.RunMode mode, int p, int i, int d, int f, InternalMotorControlAlgorithm motorControlAlgorithm)
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
        this.f = f;
        this.motorControlAlgorithm = (byte)motorControlAlgorithm.getValue();
        }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    @Override public boolean isResponseExpected()
        {
        return false;
        }

    @Override public byte[] toPayloadByteArray()
        {
        ByteBuffer buffer = ByteBuffer.allocate(cbPayload).order(LynxDatagram.LYNX_ENDIAN);
        buffer.put(this.motor);
        buffer.put(this.mode);
        buffer.putInt(this.p);
        buffer.putInt(this.i);
        buffer.putInt(this.d);
        buffer.putInt(this.f);
        buffer.put(this.motorControlAlgorithm);
        return buffer.array();
        }

    @Override public void fromPayloadByteArray(byte[] rgb)
        {
        ByteBuffer buffer = ByteBuffer.wrap(rgb).order(LynxDatagram.LYNX_ENDIAN);
        this.motor = buffer.get();
        this.mode  = buffer.get();
        this.p     = buffer.getInt();
        this.i     = buffer.getInt();
        this.d     = buffer.getInt();
        this.f     = buffer.getInt();
        this.motorControlAlgorithm = motorControlAlgorithm;
        }
    }
