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
import com.qualcomm.hardware.lynx.commands.LynxDatagram;
import com.qualcomm.hardware.lynx.commands.LynxInterfaceResponse;
import com.qualcomm.hardware.lynx.commands.LynxResponse;
import com.qualcomm.robotcore.exception.RobotCoreException;

import java.nio.ByteBuffer;

/**
 * Created by bob on 2016-03-07.
 */
public class LynxGetADCCommand extends LynxDekaInterfaceCommand<LynxGetADCResponse>
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    private final static int cbPayload = 2;

    private byte channel;
    private byte mode;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public enum Channel {
        USER0(0), USER1(1), USER2(2), USER3(3), GPIO_CURRENT(4), I2C_BUS_CURRENT(5),
        SERVO_CURRENT(6), BATTERY_CURRENT(7), MOTOR0_CURRENT(8), MOTOR1_CURRENT(9), MOTOR2_CURRENT(10),
        MOTOR3_CURRENT(11), FIVE_VOLT_MONITOR(12), BATTERY_MONITOR(13), CONTROLLER_TEMPERATURE(14);
        public final byte bVal; Channel(int bVal) {this.bVal = (byte)bVal; }
        public static Channel user(int port)
            {
            switch (port)
                {
                case 0: return USER0;
                case 1: return USER1;
                case 2: return USER2;
                case 3: return USER3;
                default: throw new IllegalArgumentException(String.format("illegal user port %d", port));
                }
            }
        }

    public enum Mode
        {
        /** units are in millivolts, milliamps, or degC as appropriate */
        ENGINEERING(0),
        /** units are raw counts */
        RAW(1);
        public final byte bVal; Mode(int bVal) {this.bVal = (byte)bVal; }
        };

    public LynxGetADCCommand(LynxModuleIntf module)
        {
        super(module);
        this.response = new LynxGetADCResponse(module);
        }

    public LynxGetADCCommand(LynxModuleIntf module, Channel channel, Mode mode)
        {
        this(module);
        this.channel = channel.bVal;
        this.mode    = mode.bVal;
        }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    public static Class<? extends LynxInterfaceResponse> getResponseClass()
        {
        return LynxGetADCResponse.class;
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
        buffer.put(this.channel);
        buffer.put(this.mode);
        return buffer.array();
        }

    @Override
    public void fromPayloadByteArray(byte[] rgb)
        {
        ByteBuffer buffer = ByteBuffer.wrap(rgb).order(LynxDatagram.LYNX_ENDIAN);
        this.channel = buffer.get();
        this.mode = buffer.get();
        }

    }
