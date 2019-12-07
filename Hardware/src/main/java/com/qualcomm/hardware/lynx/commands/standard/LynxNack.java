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
package com.qualcomm.hardware.lynx.commands.standard;

import com.qualcomm.hardware.lynx.LynxModuleIntf;
import com.qualcomm.hardware.lynx.commands.LynxMessage;
import com.qualcomm.robotcore.util.TypeConversion;

import org.firstinspires.ftc.robotcore.internal.system.Assert;

/**
 * Created by bob on 2016-03-04.
 */
public class LynxNack extends LynxMessage
    {
    //----------------------------------------------------------------------------------------------
    // Types
    //----------------------------------------------------------------------------------------------

    public enum ReasonCode
        {
        PARAM0(0), PARAM1(1), PARAM2(2), PARAM3(3), PARAM4(4), PARAM5(5), PARAM6(6), PARAM7(7), PARAM8(8), PARAM9(9),
            GPIO_OUT0(10), GPIO_OUT1(11), GPIO_OUT2(12), GPIO_OUT3(13), GPIO_OUT4(14), GPIO_OUT5(15), GPIO_OUT6(16), GPIO_OUT7(17), GPIO_NO_OUTPUT(18),
            GPIO_IN0(20), GPIO_IN1(21), GPIO_IN2(22), GPIO_IN3(23), GPIO_IN4(24), GPIO_IN5(25), GPIO_IN6(26), GPIO_IN7(27), GPIO_NO_INPUT(28),
            SERVO_NOT_CONFIG_BEFORE_ENABLED(30),
            BATTERY_TOO_LOW_TO_RUN_SERVO(31),
            I2C_MASTER_BUSY(40),                // command rejected
            I2C_OPERATION_IN_PROGRESS(41),      // poll again for completion status
            I2C_NO_RESULTS_PENDING(42),         // read results were requested but there's no oustanding read request
            I2C_QUERY_MISMATCH(43),             // query doesn't match last operation (read query for write req, or visa versa)
            MOTOR_NOT_CONFIG_BEFORE_ENABLED(50),
            COMMAND_INVALID_FOR_MOTOR_MODE(51),
            BATTERY_TOO_LOW_TO_RUN_MOTOR(52),
            COMMAND_IMPL_PENDING(253),          // software diagnostic; command is known and properly delivered but implementation is not complete
            COMMAND_ROUTING_ERROR(254),         // software diagnostic; command is known but not handled by receiving subsystem
            PACKET_TYPE_ID_UNKNOWN(255),        // protocol failure; no unknown commands should be sent if discovery was performed properly

            // codes bigger than a byte are internal here to the SDK; they must never be transmitted
            ABANDONED_WAITING_FOR_RESPONSE(256),
            ABANDONED_WAITING_FOR_ACK(257);

        private int iVal;
        ReasonCode(int i) { this.iVal = i; }
        public int getValue() { return this.iVal; }
        public static ReasonCode fromInt(int i)
            {
            // handle a couple specially, for speed
            if (i == I2C_OPERATION_IN_PROGRESS.getValue())  return I2C_OPERATION_IN_PROGRESS;
            if (i == I2C_MASTER_BUSY.getValue())            return I2C_MASTER_BUSY;
            for (ReasonCode reasonCode : ReasonCode.values())
                {
                if (i == reasonCode.getValue())
                    return reasonCode;
                }
            return PACKET_TYPE_ID_UNKNOWN;
            }

        public boolean isUnsupportedReason()
            {
            switch (this)
                {
                case COMMAND_IMPL_PENDING:
                case COMMAND_ROUTING_ERROR:
                case PACKET_TYPE_ID_UNKNOWN:
                    return true;
                default:
                    return false;
                }
            }
        }

    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    private int nackReasonCode;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public LynxNack(LynxModuleIntf module)
        {
        super(module);
        }

    public LynxNack(LynxModuleIntf module, int nackReasonCode)
        {
        this(module);
        this.nackReasonCode = nackReasonCode;
        }

    public LynxNack(LynxModuleIntf module, ReasonCode reasonCode)
        {
        this(module, reasonCode.getValue());
        }

    //----------------------------------------------------------------------------------------------
    // Accessors
    //----------------------------------------------------------------------------------------------

    public ReasonCode getNackReasonCode()
        {
        return ReasonCode.fromInt(this.nackReasonCode);
        }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    public static int getStandardCommandNumber()
        {
        return LynxStandardCommand.COMMAND_NUMBER_NACK;
        }

    @Override
    public int getCommandNumber()
        {
        return getStandardCommandNumber();
        }


    @Override
    public byte[] toPayloadByteArray()
        {
        Assert.assertTrue((byte)this.nackReasonCode == this.nackReasonCode);
        return new byte[] { (byte)this.nackReasonCode };
        }

    @Override
    public void fromPayloadByteArray(byte[] rgb)
        {
        this.nackReasonCode = TypeConversion.unsignedByteToInt(rgb[0]);
        }

    @Override
    public boolean isNack()
        {
        return true;
        }
    }