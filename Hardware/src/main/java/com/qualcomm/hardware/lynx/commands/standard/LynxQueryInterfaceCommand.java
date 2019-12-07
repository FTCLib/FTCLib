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
import com.qualcomm.robotcore.exception.RobotCoreException;

import java.nio.charset.Charset;

/**
 * Created by bob on 2016-03-06.
 */
public class LynxQueryInterfaceCommand extends LynxStandardCommand<LynxQueryInterfaceResponse>
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    private String interfaceName;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public LynxQueryInterfaceCommand(LynxModule module)
        {
        super(module);
        this.response = new LynxQueryInterfaceResponse(module);
        }

    public LynxQueryInterfaceCommand(LynxModule module, String interfaceName)
        {
        this(module);
        this.interfaceName = interfaceName;
        }

    //----------------------------------------------------------------------------------------------
    // Accessors
    //----------------------------------------------------------------------------------------------

    void setInterfaceName(String interfaceName)
        {
        this.interfaceName = interfaceName;
        // remove any terminating null
        if (this.interfaceName != null && this.interfaceName.length() > 0 && this.interfaceName.charAt(this.interfaceName.length()-1) == '\0')
            {
            this.interfaceName = this.interfaceName.substring(0, this.interfaceName.length()-1);
            }
        }

    String getInterfaceName()
        {
        return this.interfaceName;
        }

    String getNullTerminatedInterfaceName()
        {
        return getInterfaceName() + "\0";
        }

    public LynxQueryInterfaceResponse getResponse()
        {
        return this.response;
        }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    public static int getStandardCommandNumber()
        {
        return COMMAND_NUMBER_QUERY_INTERFACE;
        }

    public static Class<? extends LynxResponse> getResponseClass()
        {
        return LynxQueryInterfaceResponse.class;
        }

    @Override
    public boolean isResponseExpected()
        {
        return true;
        }

    @Override
    public int getCommandNumber()
        {
        return getStandardCommandNumber();
        }

    @Override
    public byte[] toPayloadByteArray()
        {
        return this.getNullTerminatedInterfaceName().getBytes(Charset.forName("UTF-8"));
        }

    @Override
    public void fromPayloadByteArray(byte[] rgb)
        {
        this.setInterfaceName(new String(rgb, Charset.forName("UTF-8")));
        }

    }
