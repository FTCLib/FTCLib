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
import com.qualcomm.hardware.lynx.commands.core.LynxGetDIODirectionCommand;
import com.qualcomm.hardware.lynx.commands.core.LynxGetDIODirectionResponse;
import com.qualcomm.hardware.lynx.commands.core.LynxGetSingleDIOInputCommand;
import com.qualcomm.hardware.lynx.commands.core.LynxGetSingleDIOInputResponse;
import com.qualcomm.hardware.lynx.commands.core.LynxSetDIODirectionCommand;
import com.qualcomm.hardware.lynx.commands.core.LynxSetSingleDIOOutputCommand;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.DigitalChannel;
import com.qualcomm.robotcore.hardware.DigitalChannelController;
import com.qualcomm.robotcore.hardware.configuration.LynxConstants;
import com.qualcomm.robotcore.util.LastKnown;
import com.qualcomm.robotcore.util.SerialNumber;

/**
 * {@link LynxDigitalChannelController} provides access to the digital IO pins on a Lynx module.
 */
@SuppressWarnings("WeakerAccess")
public class LynxDigitalChannelController extends LynxController implements DigitalChannelController
    {
    //----------------------------------------------------------------------------------------------
    // Constants
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "LynxDigitalChannelController";
    @Override protected String getTag() { return TAG; }

    public static final int apiPinFirst = 0;
    public static final int apiPinLast = apiPinFirst + LynxConstants.NUMBER_OF_DIGITAL_IOS -1;

    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected class PinProperties
        {
        LastKnown<DigitalChannel.Mode> lastKnownMode  = new LastKnown<DigitalChannel.Mode>();
        LastKnown<Boolean>             lastKnownState = new LastKnown<Boolean>();
        }

    protected final PinProperties[] pins = new PinProperties[LynxConstants.NUMBER_OF_DIGITAL_IOS];

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public LynxDigitalChannelController(final Context context, final LynxModule module)
            throws RobotCoreException, InterruptedException
        {
        super(context, module);
        for (int pin = apiPinFirst; pin <= apiPinLast; pin++)
            {
            pins[pin - apiPinFirst] = new PinProperties();
            }
        this.finishConstruction();
        }

    @Override public void initializeHardware()
        {
        forgetLastKnown();
        for (int pin = apiPinFirst; pin <= apiPinLast; pin++)
            {
            internalSetDigitalChannelMode(pin - apiPinFirst, DigitalChannel.Mode.INPUT);
            }
        }

    @Override public void forgetLastKnown()
        {
        for (PinProperties pin : pins)
            {
            pin.lastKnownMode.invalidate();
            pin.lastKnownState.invalidate();
            }
        }

    //----------------------------------------------------------------------------------------------
    // HardwareDevice
    //----------------------------------------------------------------------------------------------

    @Override
    public String getDeviceName()
        {
        return this.context.getString(R.string.lynxDigitalChannelControllerDisplayName);
        }

    //----------------------------------------------------------------------------------------------
    // DigitalChannelController
    //----------------------------------------------------------------------------------------------

    @Override
    public SerialNumber getSerialNumber()
        {
        return this.getModule().getSerialNumber();
        }

    @Override
    public synchronized DigitalChannel.Mode getDigitalChannelMode(int pin)
        {
        validatePin(pin); pin -= apiPinFirst;

        // Do we have a cached answer?
        DigitalChannel.Mode result = pins[pin].lastKnownMode.getValue();
        if (result != null)
            {
            return result;
            }

        // No cache, ask the controller
        LynxGetDIODirectionCommand command = new LynxGetDIODirectionCommand(this.getModule(), pin);
        try {
            LynxGetDIODirectionResponse response = command.sendReceive();
            result = response.getMode();
            pins[pin].lastKnownMode.setValue(result);   // remember for next time
            return result;
            }
        catch (InterruptedException|RuntimeException|LynxNackException e)
            {
            handleException(e);
            }
        return LynxUsbUtil.makePlaceholderValue(DigitalChannel.Mode.INPUT);
        }

    @Override
    public synchronized void setDigitalChannelMode(int pin, DigitalChannel.Mode mode)
        {
        DigitalChannel.Mode existingMode = getDigitalChannelMode(pin);
        validatePin(pin); pin -= apiPinFirst;
        internalSetDigitalChannelMode(pin, mode);

        // If direction is changed from input to output, the output value is initially set to 0 by the FW
        if (existingMode == DigitalChannel.Mode.INPUT && mode == DigitalChannel.Mode.OUTPUT)
            {
            pins[pin].lastKnownState.setValue(false);
            }
        }

    @Override @Deprecated public void setDigitalChannelMode(int pin, Mode mode)
        {
        setDigitalChannelMode(pin, mode.migrate());
        }

    void internalSetDigitalChannelMode(int pinZ, DigitalChannel.Mode mode)
        {
        LynxSetDIODirectionCommand command = new LynxSetDIODirectionCommand(this.getModule(), pinZ, mode);
        try {
            command.send();
            }
        catch (InterruptedException|RuntimeException|LynxNackException e)
            {
            handleException(e);
            pins[pinZ].lastKnownMode.invalidate();
            return;
            }

        pins[pinZ].lastKnownMode.setValue(mode);
        }

    @Override
    public synchronized boolean getDigitalChannelState(int pin)
        {
        DigitalChannel.Mode mode = getDigitalChannelMode(pin);
        validatePin(pin); pin -= apiPinFirst;

        if (mode == DigitalChannel.Mode.OUTPUT)
            {
            // For output pins we don't ask the controller, as he'll just give us a NACK
            return pins[pin].lastKnownState.getNonTimedValue();
            }
        else
            {
            // For input pins, we ask the controller, then remember what he said
            LynxGetSingleDIOInputCommand command = new LynxGetSingleDIOInputCommand(this.getModule(), pin);
            try {
                LynxGetSingleDIOInputResponse response = command.sendReceive();
                boolean result = response.getValue();
                pins[pin].lastKnownState.setValue(result);
                return result;
                }
            catch (InterruptedException|RuntimeException|LynxNackException e)
                {
                handleException(e);
                }
            return LynxUsbUtil.makePlaceholderValue(false);
            }
        }

    @Override
    public synchronized void setDigitalChannelState(int pin, boolean state)
        {
        DigitalChannel.Mode mode = getDigitalChannelMode(pin);
        validatePin(pin); pin -= apiPinFirst;

        // Setting the value of a DIO pin configured for input will result in a NACK, so we don't bother
        if (mode == DigitalChannel.Mode.OUTPUT)
            {
            LynxSetSingleDIOOutputCommand command = new LynxSetSingleDIOOutputCommand(this.getModule(), pin, state);
            try {
                command.send();
                }
            catch (InterruptedException|RuntimeException|LynxNackException e)
                {
                handleException(e);
                pins[pin].lastKnownState.invalidate();
                return;
                }

            pins[pin].lastKnownState.setValue(state);
            }
        }

    //----------------------------------------------------------------------------------------------
    // Utility
    //----------------------------------------------------------------------------------------------

    private void validatePin(int pin)
        {
        if (pin < apiPinFirst || pin > apiPinLast)
            {
            throw new IllegalArgumentException(String.format("pin %d is invalid; valid pins are %d..%d", pin, apiPinFirst, apiPinLast));
            }
        }
    }
