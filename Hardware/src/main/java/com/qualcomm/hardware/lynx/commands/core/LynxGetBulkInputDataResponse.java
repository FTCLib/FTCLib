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
import com.qualcomm.robotcore.hardware.configuration.LynxConstants;
import com.qualcomm.hardware.lynx.commands.LynxDatagram;

import java.nio.ByteBuffer;

/**
 * Created by bob on 2016-03-06.
 */
public class LynxGetBulkInputDataResponse extends LynxDekaInterfaceResponse
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    /*
        uint8_t     digitalInputs;
        int32_t     motor0position_enc;
        int32_t     motor1position_enc;
        int32_t     motor2position_enc;
        int32_t     motor3position_enc;
        uint8_t     motorStatus;
        int16_t     motor0velocity_cps;  // counts per second
        int16_t     motor1velocity_cps;
        int16_t     motor2velocity_cps;
        int16_t     motor3velocity_cps;
        int16_t     analog0_mV;
        int16_t     analog1_mV;
        int16_t     analog2_mV;
        int16_t     analog3_mV;
     */
    public final int cbPayload = 1
            + LynxConstants.NUMBER_OF_MOTORS * 4
            + 1
            + LynxConstants.NUMBER_OF_MOTORS * 2
            + LynxConstants.NUMBER_OF_ANALOG_INPUTS * 2;

    byte        digitalInputs   = 0;
    int[]       encoders        = new int[LynxConstants.NUMBER_OF_MOTORS];
    byte        motorStatus     = 0;
    short[]     velocities      = new short[LynxConstants.NUMBER_OF_MOTORS];
    short[]     analogInputs    = new short[LynxConstants.NUMBER_OF_ANALOG_INPUTS];

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public LynxGetBulkInputDataResponse(LynxModuleIntf module)
        {
        super(module);
        }

    //----------------------------------------------------------------------------------------------
    // Accessors
    //----------------------------------------------------------------------------------------------

    public boolean getDigitalInput(int digitalInputZ)
        {
        LynxConstants.validateDigitalIOZ(digitalInputZ);
        int bit = 1<<digitalInputZ;
        return (this.digitalInputs&bit) != 0;
        }

    public int getEncoder(int motorZ)
        {
        LynxConstants.validateMotorZ(motorZ);
        return this.encoders[motorZ];
        }

    /** Returns (signed) motor velocity in encoder counts per second */
    public int getVelocity(int motorZ)
        {
        LynxConstants.validateMotorZ(motorZ);
        return this.velocities[motorZ];
        }

    public boolean isAtTarget(int motorZ)
        {
        LynxConstants.validateMotorZ(motorZ);
        int bit = (1<<(motorZ+4));
        return (this.motorStatus&bit) != 0;
        }

    public boolean isOverCurrent(int motorZ)
        {
        LynxConstants.validateMotorZ(motorZ);
        int bit = (1<<motorZ);
        return (this.motorStatus&bit) != 0;
        }

    /** Returns the analog input in mV */
    public int getAnalogInput(int inputZ)
        {
        LynxConstants.validateAnalogInputZ(inputZ);
        return this.analogInputs[inputZ];
        }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    @Override
    public byte[] toPayloadByteArray()
        {
        ByteBuffer buffer = ByteBuffer.allocate(cbPayload).order(LynxDatagram.LYNX_ENDIAN);

        buffer.put(this.digitalInputs);
        for (int i = 0; i < this.encoders.length; i++)
            {
            buffer.putInt(this.encoders[i]);
            }
        buffer.put(this.motorStatus);
        for (int i = 0; i < this.velocities.length; i++)
            {
            buffer.putShort(this.velocities[i]);
            }
        for (int i = 0; i < this.analogInputs.length; i++)
            {
            buffer.putShort(this.analogInputs[i]);
            }

        return buffer.array();
        }

    @Override
    public void fromPayloadByteArray(byte[] rgb)
        {
        ByteBuffer buffer = ByteBuffer.wrap(rgb).order(LynxDatagram.LYNX_ENDIAN);

        this.digitalInputs = buffer.get();
        for (int i = 0; i < this.encoders.length; i++)
            {
            this.encoders[i] = buffer.getInt();
            }
        this.motorStatus = buffer.get();
        for (int i = 0; i < this.velocities.length; i++)
            {
            this.velocities[i] = buffer.getShort();
            }
        for (int i = 0; i < this.analogInputs.length; i++)
            {
            this.analogInputs[i] = buffer.getShort();
            }
        }
    }
