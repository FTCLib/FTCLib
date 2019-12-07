/*
Copyright (c) 2017 Robert Atkinson

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
package org.firstinspires.ftc.robotcore.internal.stellaris;

import com.qualcomm.robotcore.util.TypeConversion;
import com.qualcomm.robotcore.util.Util;

import java.nio.ByteOrder;

/**
 * This command is sent to the flash loader to indicate where to store data and how many bytes will
 * be sent by the COMMAND_SEND_DATA commands that follow. The command consists of two 32-bit
 * values that are both transferred MSB first. The first 32-bit value is the address to start programming
 * data into, while the second is the 32-bit size of the data that will be sent. This command also triggers
 * an erase of the full area to be programmed so this command takes longer than other commands.
 * This results in a longer time to receive the ACK/NAK back from the board. This command should
 * be followed by a COMMAND_GET_STATUS to ensure that the Program Address and Program size
 * are valid for the device running the flash loader.
 */
@SuppressWarnings("WeakerAccess")
public class FlashLoaderDownloadCommand extends FlashLoaderCommand
    {
    public FlashLoaderDownloadCommand(int address, int length)
        {
        super(0x21, makePayload(address, length));
        }

    protected static byte[] makePayload(int address, int length)
        {
        ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
        return Util.concatenateByteArrays(
                TypeConversion.intToByteArray(address, byteOrder),
                TypeConversion.intToByteArray(length, byteOrder));
        }
    }
