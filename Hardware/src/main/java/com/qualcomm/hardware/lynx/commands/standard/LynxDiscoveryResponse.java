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
import com.qualcomm.hardware.lynx.commands.LynxResponse;
import com.qualcomm.robotcore.util.TypeConversion;

/**
 * The response message sent in reply to a LynxDiscoveryCommand. Note that the source
 * module address is a critical part of the information in the response.
 *
 * @see LynxDiscoveryCommand
 */
public class LynxDiscoveryResponse extends LynxStandardResponse
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    /** 1 = my discovery packet arrived via USB; 0 = via EIA485 */
    byte parentIndicator;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public LynxDiscoveryResponse()
        {
        super(null);
        }

    //----------------------------------------------------------------------------------------------
    // Accessors
    //----------------------------------------------------------------------------------------------

    public boolean isParent()
        {
        return !isChild();
        }

    public boolean isChild()
        {
        return getParentIndicator() == 0;
        }

    public int getParentIndicator()
        {
        return TypeConversion.unsignedByteToInt(this.parentIndicator);
        }

    public int getDiscoveredModuleAddress()
        {
        return this.serialization.getSourceModuleAddress();
        }

    //----------------------------------------------------------------------------------------------
    // Transmission
    //----------------------------------------------------------------------------------------------

    @Override
    public boolean isAckable()
        {
        return false;
        }

    @Override
    public boolean isRetransmittable()
        {
        return false;
        }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    public static int getStandardCommandNumber()
        {
        return LynxDiscoveryCommand.getStandardCommandNumber() | LynxResponse.RESPONSE_BIT;
        }

    @Override
    public int getCommandNumber()
        {
        return getStandardCommandNumber();
        }

    @Override
    public byte[] toPayloadByteArray()
        {
        return new byte[] { this.parentIndicator};
        }

    @Override
    public void fromPayloadByteArray(byte[] rgb)
        {
        this.parentIndicator = rgb[0];
        }
    }
