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

/**
 * {link FlashLoaderDatagram} represents the core packet structure of transmissions to and from
 * the flash loader.
 */
@SuppressWarnings("WeakerAccess")
public class FlashLoaderDatagram
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final byte ACK = (byte)0xCC;
    public static final byte NAK = (byte)0x33;

    public static final int CB_HEADER  = 2;
    public static final int IB_LENGTH  = 0;
    public static final int IB_XSUM    = 1;
    public static final int IB_PAYLOAD = 2;

    public byte[] data;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public FlashLoaderDatagram(int cbPayload)
        {
        data = new byte[CB_HEADER + cbPayload];
        data[IB_LENGTH] = (byte)(CB_HEADER + cbPayload);
        }

    public FlashLoaderDatagram(byte[] payload)
        {
        this(payload.length);
        System.arraycopy(payload, 0, data, IB_PAYLOAD, payload.length);
        computeChecksum();
        }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    protected byte computeChecksum()
        {
        int xsum = 0;
        for (int ib = IB_PAYLOAD; ib < data.length; ib++)
            {
            xsum += data[ib];
            }
        return (byte)xsum;
        }

    protected void updateChecksum()
        {
        data[IB_XSUM] = computeChecksum();
        }

    protected boolean isChecksumValid()
        {
        return data[IB_XSUM] == computeChecksum();
        }

    }
