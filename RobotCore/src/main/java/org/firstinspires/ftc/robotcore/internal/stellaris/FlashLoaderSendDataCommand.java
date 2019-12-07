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

import java.util.Arrays;

/**
 * This command should only follow a COMMAND_DOWNLOAD command or another
 * COMMAND_SEND_DATA command if more data is needed. Consecutive send data commands
 * automatically increment address and continue programming from the previous location. The caller
 * should limit transfers of data to a maximum 8 bytes of packet data to allow the flash to program
 * successfully and not overflow input buffers of the serial interfaces. The command terminates
 * programming once the number of bytes indicated by the COMMAND_DOWNLOAD command has been
 * received. Each time this function is called it should be followed by a COMMAND_GET_STATUS to
 * ensure that the data was successfully programmed into the flash. If the flash loader sends a NAK
 * to this command, the flash loader does not increment the current address to allow retransmission
 * of the previous data.
 */
@SuppressWarnings("WeakerAccess")
public class FlashLoaderSendDataCommand extends FlashLoaderCommand
    {
    // The documentation says that the quantum should be 8, but in practice, the LM Flash
    // Programmer actually uses 60 bytes. sflash uses 8 (by default). Hmmm... we'll use
    // a small number, to be on the safe side. The firmware update will run more slowly,
    // but (probably) has less chance of bricking.
    public static final int QUANTUM = 16;

    public FlashLoaderSendDataCommand(byte[] data)
        {
        this(data, 0);
        }

    public FlashLoaderSendDataCommand(byte[] data, int ibFirst)
        {
        super(0x24, makePayload(data, ibFirst));
        }

    protected static byte[] makePayload(byte[] data, int ibFirst)
        {
        int cb = Math.min(QUANTUM, data.length - ibFirst);
        return Arrays.copyOfRange(data, ibFirst /*inclusive*/, ibFirst + cb /*exclusive!*/);
        }
    }
