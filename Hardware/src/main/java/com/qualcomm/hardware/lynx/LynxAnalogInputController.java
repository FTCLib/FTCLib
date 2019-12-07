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
package com.qualcomm.hardware.lynx;

import android.content.Context;

import com.qualcomm.hardware.R;
import com.qualcomm.hardware.lynx.commands.core.LynxGetADCCommand;
import com.qualcomm.hardware.lynx.commands.core.LynxGetADCResponse;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.AnalogInputController;
import com.qualcomm.robotcore.hardware.configuration.LynxConstants;
import com.qualcomm.robotcore.util.SerialNumber;

/**
 * Created by bob on 2016-03-12.
 */
@SuppressWarnings("WeakerAccess")
public class LynxAnalogInputController extends LynxController implements AnalogInputController
    {
    //----------------------------------------------------------------------------------------------
    // Constants
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "LynxAnalogInputController";
    @Override protected String getTag() { return TAG; }

    public static final int apiPortFirst = 0;
    public static final int apiPortLast = apiPortFirst + LynxConstants.NUMBER_OF_ANALOG_INPUTS -1;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public LynxAnalogInputController(final Context context, final LynxModule module)
            throws RobotCoreException, InterruptedException
        {
        super(context, module);
        this.finishConstruction();
        }

    //----------------------------------------------------------------------------------------------
    // HardwareDevice
    //----------------------------------------------------------------------------------------------

    @Override
    public String getDeviceName()
        {
        return this.context.getString(R.string.lynxAnalogInputControllerDisplayName);
        }

    //----------------------------------------------------------------------------------------------
    // AnalogInputController
    //----------------------------------------------------------------------------------------------

    @Override
    public SerialNumber getSerialNumber()
        {
        return this.getModule().getSerialNumber();
        }

    @Override
    public double getAnalogInputVoltage(int port)
        {
        validatePort(port); port -= apiPortFirst;
        LynxGetADCCommand command = new LynxGetADCCommand(this.getModule(), LynxGetADCCommand.Channel.user(port), LynxGetADCCommand.Mode.ENGINEERING);
        try {
            LynxGetADCResponse response = command.sendReceive();
            return response.getValue() * 0.001;
            }
        catch (InterruptedException|RuntimeException|LynxNackException e)
            {
            handleException(e);
            }
        return LynxUsbUtil.makePlaceholderValue(0.0);
        }

    @Override
    public double getMaxAnalogInputVoltage()
        {
        return 3.3;
        }

    //----------------------------------------------------------------------------------------------
    // Utility
    //----------------------------------------------------------------------------------------------

    private void validatePort(int port)
        {
        if (port < apiPortFirst || port > apiPortLast)
            {
            throw new IllegalArgumentException(String.format("port %d is invalid; valid ports are %d..%d", port, apiPortFirst, apiPortLast));
            }
        }
    }
