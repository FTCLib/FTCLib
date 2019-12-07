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
import com.qualcomm.hardware.lynx.commands.core.LynxGetPWMConfigurationCommand;
import com.qualcomm.hardware.lynx.commands.core.LynxGetPWMConfigurationResponse;
import com.qualcomm.hardware.lynx.commands.core.LynxGetPWMEnableCommand;
import com.qualcomm.hardware.lynx.commands.core.LynxGetPWMEnableResponse;
import com.qualcomm.hardware.lynx.commands.core.LynxGetPWMPulseWidthCommand;
import com.qualcomm.hardware.lynx.commands.core.LynxGetPWMPulseWidthResponse;
import com.qualcomm.hardware.lynx.commands.core.LynxSetPWMConfigurationCommand;
import com.qualcomm.hardware.lynx.commands.core.LynxSetPWMEnableCommand;
import com.qualcomm.hardware.lynx.commands.core.LynxSetPWMPulseWidthCommand;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.PWMOutputController;
import com.qualcomm.robotcore.hardware.PWMOutputControllerEx;
import com.qualcomm.robotcore.hardware.PwmControl;
import com.qualcomm.robotcore.hardware.configuration.LynxConstants;
import com.qualcomm.robotcore.util.LastKnown;
import com.qualcomm.robotcore.util.SerialNumber;

/**
 * Created by bob on 2016-03-12.
 */
@SuppressWarnings("WeakerAccess")
@Deprecated
public class LynxPwmOutputController extends LynxController implements PWMOutputController, PWMOutputControllerEx
    {
    //----------------------------------------------------------------------------------------------
    // Constants
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "LynxPwmOutputController";
    @Override protected String getTag() { return TAG; }

    public static final int apiPortFirst = 0;
    public static final int apiPortLast = apiPortFirst + LynxConstants.NUMBER_OF_PWM_CHANNELS -1;

    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected LastKnown<Integer>[]  lastKnownOutputTimes;
    protected LastKnown<Integer>[]  lastKnownPulseWidthPeriods;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public LynxPwmOutputController(final Context context, final LynxModule module)
            throws RobotCoreException, InterruptedException
        {
        super(context, module);
        this.lastKnownOutputTimes       = LastKnown.createArray(LynxConstants.NUMBER_OF_PWM_CHANNELS);
        this.lastKnownPulseWidthPeriods = LastKnown.createArray(LynxConstants.NUMBER_OF_PWM_CHANNELS);
        this.finishConstruction();
        }

    @Override public void initializeHardware()
        {
        for (int port = apiPortFirst; port <= apiPortLast; port++)
            {
            this.setPwmDisable(port);
            this.internalSetPulseWidthPeriod(port - apiPortFirst, (int) PwmControl.PwmRange.usFrameDefault);
            }
        }

    @Override public void floatHardware()
        {
        for (int port = apiPortFirst; port <= apiPortLast; port++)
            {
            this.setPwmDisable(port);
            }
        }

    //----------------------------------------------------------------------------------------------
    // HardwareDevice
    //----------------------------------------------------------------------------------------------

    @Override
    public String getDeviceName()
        {
        return this.context.getString(R.string.lynxPwmOutputControllerDisplayName);
        }

    //----------------------------------------------------------------------------------------------
    // PWMOutputController
    //----------------------------------------------------------------------------------------------

    @Override
    public SerialNumber getSerialNumber()
        {
        return this.getModule().getSerialNumber();
        }

    @Override
    public synchronized void setPulseWidthOutputTime(int port, int usDuration)
        {
        validatePort(port); port -= apiPortFirst;
        internalSetPulseWidthOutputTime(port, usDuration);
        setPwmEnable(port + apiPortFirst);
        }

    void internalSetPulseWidthOutputTime(int portZ, int usDuration)
        {
        if (lastKnownOutputTimes[portZ].updateValue(usDuration))
            {
            LynxSetPWMPulseWidthCommand command = new LynxSetPWMPulseWidthCommand(this.getModule(), portZ, usDuration);
            try {
                command.send();
                }
            catch (InterruptedException|RuntimeException|LynxNackException e)
                {
                handleException(e);
                }
            }
        }

    @Override
    public synchronized void setPulseWidthPeriod(int port, int usPeriod)
        {
        validatePort(port); port -= apiPortFirst;
        internalSetPulseWidthPeriod(port, usPeriod);
        setPwmEnable(port + apiPortFirst);
        }

    void internalSetPulseWidthPeriod(int portZ, int usPeriod)
        {
        if (lastKnownOutputTimes[portZ].updateValue(usPeriod))
            {
            LynxSetPWMConfigurationCommand command = new LynxSetPWMConfigurationCommand(this.getModule(), portZ, usPeriod);
            try {
                command.send();
                }
            catch (InterruptedException|RuntimeException|LynxNackException e)
                {
                handleException(e);
                }
            }
        }

    @Override
    public synchronized int getPulseWidthOutputTime(int port)
        {
        validatePort(port); port -= apiPortFirst;
        LynxGetPWMPulseWidthCommand command = new LynxGetPWMPulseWidthCommand(this.getModule(), port);
        try {
            LynxGetPWMPulseWidthResponse response = command.sendReceive();
            return response.getPulseWidth();
            }
        catch (InterruptedException|RuntimeException|LynxNackException e)
            {
            handleException(e);
            }
        return LynxUsbUtil.makePlaceholderValue(0);
        }

    @Override
    public synchronized int getPulseWidthPeriod(int port)
        {
        validatePort(port); port -= apiPortFirst;
        LynxGetPWMConfigurationCommand command = new LynxGetPWMConfigurationCommand(this.getModule(), port);
        try {
            LynxGetPWMConfigurationResponse response = command.sendReceive();
            return response.getFramePeriod();
            }
        catch (InterruptedException|RuntimeException|LynxNackException e)
            {
            handleException(e);
            }
        return LynxUsbUtil.makePlaceholderValue(0);
        }

    @Override
    public synchronized void setPwmEnable(int port)
        {
        validatePort(port); port -= apiPortFirst;
        internalSetPwmEnable(port, true);
        }

    @Override
    public synchronized void setPwmDisable(int port)
        {
        validatePort(port); port -= apiPortFirst;
        internalSetPwmEnable(port, false);
        }

    @Override
    public synchronized boolean isPwmEnabled(int port)
        {
        validatePort(port); port -= apiPortFirst;
        return internalGetPwmEnable(port);
        }

    private void internalSetPwmEnable(int portZ, boolean enable)
        {
        LynxSetPWMEnableCommand command = new LynxSetPWMEnableCommand(this.getModule(), portZ, enable);
        try {
            command.send();
            }
        catch (InterruptedException|RuntimeException|LynxNackException e)
            {
            handleException(e);
            }
        }

    private boolean internalGetPwmEnable(int portZ)
        {
        LynxGetPWMEnableCommand command = new LynxGetPWMEnableCommand(this.getModule(), portZ);
        try {
            LynxGetPWMEnableResponse response = command.sendReceive();
            return response.isEnabled();
            }
        catch (InterruptedException|RuntimeException|LynxNackException e)
            {
            handleException(e);
            }
        return LynxUsbUtil.makePlaceholderValue(true);
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
