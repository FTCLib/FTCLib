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

/**
 * LynxSetDebugLogLevelCommand controls the tracing that is sent out through the debug port.
 */
public class LynxSetDebugLogLevelCommand extends LynxStandardCommand<LynxAck>
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    LynxModule.DebugGroup debugGroup = LynxModule.DebugGroup.MAIN;

    /** 0==off, higher number==more verbose. Supported levels are 0 (none) through 3 (verbose). */
    LynxModule.DebugVerbosity verbosity = LynxModule.DebugVerbosity.OFF;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public LynxSetDebugLogLevelCommand(LynxModule module)
        {
        super(module);
        }

    public LynxSetDebugLogLevelCommand(LynxModule module, LynxModule.DebugGroup debugGroup, LynxModule.DebugVerbosity verbosity)
        {
        this(module);
        this.debugGroup = debugGroup;
        this.verbosity = verbosity;
        }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    public static int getStandardCommandNumber()
        {
        return COMMAND_NUMBER_DEBUG_LOG_LEVEL;
        }

    @Override
    public boolean isResponseExpected()
        {
        return false;
        }

    @Override
    public int getCommandNumber()
        {
        return getStandardCommandNumber();
        }

    @Override
    public byte[] toPayloadByteArray()
        {
        return new byte[] { debugGroup.bVal, verbosity.bVal };
        }

    @Override
    public void fromPayloadByteArray(byte[] rgb)
        {
        this.debugGroup = LynxModule.DebugGroup.fromInt(rgb[0]);
        this.verbosity = LynxModule.DebugVerbosity.fromInt(rgb[1]);
        }
    }
