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

import com.qualcomm.hardware.lynx.LynxUnsupportedCommandException;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.hardware.lynx.LynxNackException;

/**
 * Performs discovery respecting half-duplex aspect of EIA485 communications link. This message is
 * sent to the broadcast address 0xFF. If this message arrives via USB, the Controller Module knows
 * it is USB-connected (a Parent) and may have children on its 485 bus. The Parent issues its own
 * Discovery Response and then takes ownership of the 485 bus. The Parent sends Discovery packets
 * directly to all possible Child addresses (1 ~ 254) in sequence, waiting 2 msec (1kHz system clock
 * guarantees that to be between 1 ~ 2 msec) between each. Any connected Child should respond to
 * the Discovery packet effectively immediately, taking brief control over the child bus to do so.
 * Discovery completes in about 500msec.
 *
 * Once discovered, a Child requires a Keep Alive addressed directly to it (rather than a broadcast
 * message) within the usual KA interval to lock in the child status. If no Keep Alive is received
 * by the Child in time, it reverts to ready-to-discover mode.
 *
 * The discovery packet is sent from the host to the broadcast address (255). Note that the message
 * number of a broadcast message is completely ignored by receivers, as it cannot be guaranteed
 * to be a non-duplicate across all receivers.
 *
 * Note that sending a LynxDiscoveryCommand can in theory result in up to 254 response messages; thus
 * the 'finished' processing of LynxDiscoveryCommands will be special. First, an incoming
 * LynxDiscoveryResponse should be processed unilaterally; the original LynxDiscoveryCommand will NOT
 * have a response expected (nor will it be ackable). Second, after sending a LynxDiscoveryCommand we
 * will wait for a sufficient time to receive all possible responses before ever sending any further
 * messages.
 */
public class LynxDiscoveryCommand extends LynxStandardCommand<LynxAck /*actually no ack is expected */>
    {
    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public LynxDiscoveryCommand(LynxModule module)
        {
        super(module);
        }

    //----------------------------------------------------------------------------------------------
    // Transmission
    //----------------------------------------------------------------------------------------------

    @Override
    public int getDestModuleAddress()
        {
        // Discovery commands are always transmitted to the broadcast address
        return 0xff;
        }

    @Override public void send() throws LynxNackException, InterruptedException
        {
        try {
            this.module.sendCommand(this);
            }
        catch (LynxUnsupportedCommandException e)
            {
            throwNackForUnsupportedCommand(e);
            }
        }

    @Override
    public LynxAck sendReceive() throws LynxNackException, InterruptedException
        {
        try {
            this.module.sendCommand(this);
            }
        catch (LynxUnsupportedCommandException e)
            {
            throwNackForUnsupportedCommand(e);
            }
        return null;
        }

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

    @Override
    protected void noteAttentionRequired()
        {
        // nothing to do (in fact, discovery commands aren't even ack'd any more)
        }

    @Override
    public void acquireNetworkLock() throws InterruptedException
        {
        // TODO discovery doesn't lock (REVIEW)
        }

    @Override
    public void releaseNetworkLock() throws InterruptedException
        {
        // TODO discovery doesn't lock (REVIEW)
        }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    public static int getStandardCommandNumber()
        {
        return COMMAND_NUMBER_DISCOVERY;
        }

    @Override
    public int getCommandNumber()
        {
        return getStandardCommandNumber();
        }

    @Override
    public byte[] toPayloadByteArray()
        {
        return new byte[] { };
        }

    @Override
    public void fromPayloadByteArray(byte[] rgb)
        {
        }

    }
