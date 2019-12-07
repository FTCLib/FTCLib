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

import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.hardware.lynx.commands.LynxCommand;
import com.qualcomm.hardware.lynx.commands.LynxMessage;
import com.qualcomm.hardware.lynx.commands.LynxResponse;

/**
 * Created by bob on 2016-03-04.
 */
@SuppressWarnings("WeakerAccess")
public abstract class LynxStandardCommand<RESPONSE extends LynxMessage> extends LynxCommand<RESPONSE>
    {
    //----------------------------------------------------------------------------------------------
    // Constants
    //----------------------------------------------------------------------------------------------

    public static final int COMMAND_NUMBER_ACK                     = 0x7f01;
    public static final int COMMAND_NUMBER_NACK                    = 0x7f02;
    public static final int COMMAND_NUMBER_GET_MODULE_STATUS       = 0x7f03;
    public static final int COMMAND_NUMBER_KEEP_ALIVE              = 0x7f04;
    public static final int COMMAND_NUMBER_FAIL_SAFE               = 0x7f05;
    public static final int COMMAND_NUMBER_SET_NEW_MODULE_ADDRESS  = 0x7f06;
    public static final int COMMAND_NUMBER_QUERY_INTERFACE         = 0x7f07;
    public static final int COMMAND_NUMBER_START_DOWNLOAD          = 0x7f08;
    public static final int COMMAND_NUMBER_DOWNLOAD_CHUNK          = 0x7f09;
    public static final int COMMAND_NUMBER_SET_MODULE_LED_COLOR    = 0x7f0a;
    public static final int COMMAND_NUMBER_GET_MODULE_LED_COLOR    = 0x7f0b;
    public static final int COMMAND_NUMBER_SET_MODULE_LED_PATTERN  = 0x7f0c;
    public static final int COMMAND_NUMBER_GET_MODULE_LED_PATTERN  = 0x7f0d;
    public static final int COMMAND_NUMBER_DEBUG_LOG_LEVEL         = 0x7f0e;
    public static final int COMMAND_NUMBER_DISCOVERY               = 0x7f0f;

    public static final int COMMAND_NUMBER_FIRST                   = COMMAND_NUMBER_ACK;
    public static final int COMMAND_NUMBER_LAST                    = COMMAND_NUMBER_DISCOVERY;

    public static boolean isStandardPacketId(int packetId)
        {
        return isStandardCommandNumber(packetId) || isStandardResponseNumber(packetId);
        }
    public static boolean isStandardCommandNumber(int packetId)
        {
        return COMMAND_NUMBER_FIRST <= packetId && packetId <= COMMAND_NUMBER_LAST;
        }
    public static boolean isStandardResponseNumber(int packetId)
        {
        return (LynxResponse.RESPONSE_BIT & packetId) != 0 && isStandardCommandNumber(packetId & ~LynxResponse.RESPONSE_BIT);
        }

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public LynxStandardCommand(LynxModule module)
        {
        super(module);
        }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------
    }
