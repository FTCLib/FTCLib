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
package com.qualcomm.hardware.lynx.commands;

import com.qualcomm.hardware.lynx.LynxModule;

import org.firstinspires.ftc.robotcore.internal.system.Assert;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by bob on 2016-03-06.
 */
public class LynxInterface
    {
    //----------------------------------------------------------------------------------------------
    // Constants
    //----------------------------------------------------------------------------------------------

    public static final int ERRONEOUS_COMMAND_NUMBER = 0;
    public static final int ERRONEOUS_INDEX          = 0;

    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    private String                                                  interfaceName;
    private Integer                                                 baseCommandNumber;
    private Class<? extends LynxInterfaceCommand>[]                 commands;
    private Map<Class<? extends LynxInterfaceCommand>, Integer>     commandIndices;
    private Map<Class<? extends LynxInterfaceResponse>, Integer>    responseIndices;
    private boolean                                                 wasNacked;
    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public LynxInterface(String interfaceName, Class<? extends LynxInterfaceCommand>... commands)
        {
        this.baseCommandNumber = ERRONEOUS_COMMAND_NUMBER;
        this.interfaceName     = interfaceName;
        this.commands          = commands;
        this.commandIndices    = new HashMap<Class<? extends LynxInterfaceCommand>, Integer>();
        this.responseIndices   = new HashMap<Class<? extends LynxInterfaceResponse>, Integer>();
        this.wasNacked         = false;
        for (int i = 0; i < this.commands.length; i++)
            {
            Class<? extends LynxInterfaceCommand> commandClass = this.commands[i];

            if (commandClass == null)
                continue;   // filler

            // Remember the index of this command
            this.commandIndices.put(commandClass, i);
            try {
                // Find the corresponding response
                Class<? extends LynxInterfaceResponse> responseClass = (Class<? extends LynxInterfaceResponse>)LynxCommand.getResponseClass(commandClass);
                Assert.assertTrue(responseClass != null);

                // Remember the same index for the response
                this.responseIndices.put(responseClass, i);

                // Note which response goes with which command
                LynxModule.correlateResponse(commandClass, responseClass);
                }
            catch (Exception ignore)
                {
                // Probably doesn't have a response
                }
            }
        }

    //----------------------------------------------------------------------------------------------
    // Accessors
    //----------------------------------------------------------------------------------------------

    public String getInterfaceName()
        {
        return this.interfaceName;
        }

    public int getCommandCount()
        {
        return this.commands.length;
        }

    public void setBaseCommandNumber(Integer baseCommandNumber)
        {
        this.baseCommandNumber = baseCommandNumber;
        }

    public void setWasNacked(boolean nacked)
        {
            this.wasNacked = nacked;
        }

    public boolean wasNacked()
        {
            return this.wasNacked;
        }

    public Integer getBaseCommandNumber()
        {
        return this.baseCommandNumber;
        }

    /** Returns the index of this command class within the interface */
    public int getCommandIndex(Class<? extends LynxInterfaceCommand> clazz)
        {
        return this.commandIndices.get(clazz);
        }

    /** Returns the index of this response class within the interface */
    public int getResponseIndex(Class<? extends LynxInterfaceResponse> clazz)
        {
        return this.responseIndices.get(clazz);
        }

    public List<Class<? extends LynxInterfaceCommand>> getCommandClasses()
        {
        return Arrays.asList(this.commands);
        }
    }
