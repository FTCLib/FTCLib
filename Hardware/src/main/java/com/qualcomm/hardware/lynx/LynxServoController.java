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
import androidx.annotation.NonNull;

import com.qualcomm.hardware.R;
import com.qualcomm.hardware.lynx.commands.core.LynxGetServoEnableCommand;
import com.qualcomm.hardware.lynx.commands.core.LynxGetServoEnableResponse;
import com.qualcomm.hardware.lynx.commands.core.LynxGetServoPulseWidthCommand;
import com.qualcomm.hardware.lynx.commands.core.LynxGetServoPulseWidthResponse;
import com.qualcomm.hardware.lynx.commands.core.LynxSetServoConfigurationCommand;
import com.qualcomm.hardware.lynx.commands.core.LynxSetServoEnableCommand;
import com.qualcomm.hardware.lynx.commands.core.LynxSetServoPulseWidthCommand;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.PwmControl;
import com.qualcomm.robotcore.hardware.ServoController;
import com.qualcomm.robotcore.hardware.ServoControllerEx;
import com.qualcomm.robotcore.hardware.configuration.LynxConstants;
import com.qualcomm.robotcore.hardware.configuration.typecontainers.ServoConfigurationType;
import com.qualcomm.robotcore.util.LastKnown;
import com.qualcomm.robotcore.util.Range;

/**
 * Created by bob on 2016-03-12.
 */
@SuppressWarnings("WeakerAccess")
public class LynxServoController extends LynxController implements ServoController, ServoControllerEx
    {
    //----------------------------------------------------------------------------------------------
    // Constants
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "LynxServoController";
    @Override protected String getTag() { return TAG; }

    public static final int apiServoFirst = LynxConstants.INITIAL_SERVO_PORT;
    public static final int apiServoLast = apiServoFirst + LynxConstants.NUMBER_OF_SERVO_CHANNELS -1;
    public static final double apiPositionFirst = 0.0;
    public static final double apiPositionLast = 1.0;

    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected final LastKnown<Double>[]     lastKnownCommandedPosition;
    protected final LastKnown<Boolean>[]    lastKnownEnabled;
    protected       PwmControl.PwmRange[]   pwmRanges;
    protected       PwmControl.PwmRange[]   defaultPwmRanges;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public LynxServoController(final Context context, final LynxModule module)
            throws RobotCoreException, InterruptedException
        {
        super(context, module);
        this.lastKnownCommandedPosition = LastKnown.createArray(LynxConstants.NUMBER_OF_SERVO_CHANNELS);
        this.lastKnownEnabled           = LastKnown.createArray(LynxConstants.NUMBER_OF_SERVO_CHANNELS);
        this.pwmRanges                  = new PwmControl.PwmRange[LynxConstants.NUMBER_OF_SERVO_CHANNELS];
        this.defaultPwmRanges           = new PwmControl.PwmRange[LynxConstants.NUMBER_OF_SERVO_CHANNELS];

        // Paranoia: *always* initialize to something reasonable to as to avoid null pointer issues
        for (int i = 0; i < this.pwmRanges.length; i++)
            {
            this.pwmRanges[i] = PwmControl.PwmRange.defaultRange;
            this.defaultPwmRanges[i] = PwmControl.PwmRange.defaultRange;
            }

        this.finishConstruction();
        }

    @Override public void initializeHardware()
        {
        for (int servo = apiServoFirst; servo <= apiServoLast; servo++)
            {
            this.pwmRanges[servo-apiServoFirst] = null;     // clear so that setServoPwmRange will always transmit
            setServoPwmRange(servo, defaultPwmRanges[servo-apiServoFirst]);
            }
        floatHardware();
        forgetLastKnown();
        }

    @Override public void floatHardware()
        {
        pwmDisable();
        }

    @Override
    public void forgetLastKnown()
        {
        LastKnown.invalidateArray(lastKnownCommandedPosition);
        LastKnown.invalidateArray(lastKnownEnabled);
        }

    //----------------------------------------------------------------------------------------------
    // HardwareDevice interface
    //----------------------------------------------------------------------------------------------

    @Override
    public String getDeviceName()
        {
        return this.context.getString(R.string.lynxServoControllerDisplayName);
        }

    //------------------------------------------------------------------------------------------------
    // ServoController interface
    //------------------------------------------------------------------------------------------------

    @Override
    public synchronized void pwmEnable()
        {
        for (int servoZ = 0; servoZ < LynxConstants.NUMBER_OF_SERVO_CHANNELS; servoZ++)
            {
            internalSetPwmEnable(servoZ, true);
            }
        }

    @Override
    public synchronized void pwmDisable()
        {
        for (int servoZ = 0; servoZ < LynxConstants.NUMBER_OF_SERVO_CHANNELS; servoZ++)
            {
            internalSetPwmEnable(servoZ, false);
            }
        }

    @Override
    public synchronized PwmStatus getPwmStatus()
        {
        Boolean enabled = null;
        for (int servoZ = 0; servoZ < LynxConstants.NUMBER_OF_SERVO_CHANNELS; servoZ++)
            {
            boolean localEnabled = internalGetPwmEnable(servoZ);
            if (enabled == null)
                enabled = localEnabled;
            else if (enabled != localEnabled)
                return PwmStatus.MIXED;
            }
        return enabled ? PwmStatus.ENABLED : PwmStatus.DISABLED;
        }

    @Override
    public synchronized void setServoPwmEnable(int servo)
        {
        this.validateServo(servo); servo -= apiServoFirst;
        internalSetPwmEnable(servo, true);
        }

    @Override
    public synchronized void setServoPwmDisable(int servo)
        {
        this.validateServo(servo); servo -= apiServoFirst;
        internalSetPwmEnable(servo, false);
        }

    @Override
    public synchronized boolean isServoPwmEnabled(int servo)
        {
        this.validateServo(servo); servo -= apiServoFirst;
        return internalGetPwmEnable(servo);
        }

    @Override
    public void setServoType(int servo, ServoConfigurationType servoType)
        {
        this.validateServo(servo); servo -= apiServoFirst;
        PwmControl.PwmRange newDefaultPwmRange = new PwmControl.PwmRange(servoType.getUsPulseLower(), servoType.getUsPulseUpper(), servoType.getUsFrame());
        this.defaultPwmRanges[servo] = newDefaultPwmRange;
        setServoPwmRange(servo, newDefaultPwmRange);
        }

    private void internalSetPwmEnable(int servoZ, boolean enable)
        {
        // Don't change state if we know we are already there
        if (lastKnownEnabled[servoZ].updateValue(enable))
            {
            // If we're disabling, then make sure that next setServoPosition will reenable
            if (!enable)
                {
                lastKnownCommandedPosition[servoZ].invalidate();
                }

            LynxSetServoEnableCommand command = new LynxSetServoEnableCommand(this.getModule(), servoZ, enable);
            try {
                command.send();
                }
            catch (InterruptedException|RuntimeException|LynxNackException e)
                {
                handleException(e);
                }
            }
        }

    private boolean internalGetPwmEnable(int servoZ)
        {
        // If we have a cached value, then use that
        Boolean result = lastKnownEnabled[servoZ].getValue();
        if (result != null)
            {
            return result;
            }

        // Actually talk to the device
        LynxGetServoEnableCommand command = new LynxGetServoEnableCommand(this.getModule(), servoZ);
        try {
            LynxGetServoEnableResponse response = command.sendReceive();
            result = response.isEnabled();
            lastKnownEnabled[servoZ].setValue(result);
            return result;
            }
        catch (InterruptedException|RuntimeException|LynxNackException e)
            {
            handleException(e);
            }
        return LynxUsbUtil.makePlaceholderValue(true);
        }

    @Override
    public synchronized void setServoPosition(int servo, double position)
        {
        this.validateServo(servo); servo -= apiServoFirst;
        this.validateApiServoPosition(position);
        if (lastKnownCommandedPosition[servo].updateValue(position))
            {
            double pwm = Range.scale(position, apiPositionFirst, apiPositionLast, pwmRanges[servo].usPulseLower, pwmRanges[servo].usPulseUpper);
            pwm = Range.clip(pwm, LynxSetServoPulseWidthCommand.apiPulseWidthFirst, LynxSetServoPulseWidthCommand.apiPulseWidthLast);
            LynxSetServoPulseWidthCommand command = new LynxSetServoPulseWidthCommand(this.getModule(), servo, (int)pwm);
            try {
                command.send();
                }
            catch (InterruptedException|RuntimeException|LynxNackException e)
                {
                handleException(e);
                }

            // Auto-enable after setting position to match historical behavior (and because it's handy)
            this.internalSetPwmEnable(servo, true);
            }
        }

    @Override
    public synchronized double getServoPosition(int servo)
        {
        this.validateServo(servo); servo -= apiServoFirst;

        // Use cached value if we have it
        Double result = lastKnownCommandedPosition[servo].getValue();
        if (result != null)
            {
            return result;
            }

        // Actually go ask the hardware
        LynxGetServoPulseWidthCommand command = new LynxGetServoPulseWidthCommand(this.getModule(), servo);
        try {
            LynxGetServoPulseWidthResponse response = command.sendReceive();
            result = Range.scale(response.getPulseWidth(), pwmRanges[servo].usPulseLower, pwmRanges[servo].usPulseUpper, apiPositionFirst, apiPositionLast);
            result = Range.clip(result, apiPositionFirst, apiPositionLast); // paranoia
            lastKnownCommandedPosition[servo].setValue(result);
            return result;
            }
        catch (InterruptedException|RuntimeException|LynxNackException e)
            {
            handleException(e);
            }
        return LynxUsbUtil.makePlaceholderValue(0.0);
        }

    @Override
    public synchronized void setServoPwmRange(int servo, @NonNull PwmControl.PwmRange range)
        {
        this.validateServo(servo); servo -= apiServoFirst;
        if (!range.equals(pwmRanges[servo]))
            {
            pwmRanges[servo] = range;
            LynxSetServoConfigurationCommand command = new LynxSetServoConfigurationCommand(this.getModule(), servo, (int)range.usFrame);
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
    public synchronized @NonNull PwmControl.PwmRange getServoPwmRange(int servo)
        {
        this.validateServo(servo); servo -= apiServoFirst;
        return pwmRanges[servo];
        }

    //----------------------------------------------------------------------------------------------
    // Utility
    //----------------------------------------------------------------------------------------------

    private void validateServo(int servo)
        {
        if (servo < apiServoFirst || servo > apiServoLast)
            {
            throw new IllegalArgumentException(String.format("Servo %d is invalid; valid servos are %d..%d", servo, apiServoFirst, apiServoLast));
            }
        }

    private void validateApiServoPosition(double position)
        {
        if (apiPositionFirst <= position && position <= apiPositionLast)
            {
            }
        else
            throw new IllegalArgumentException(String.format("illegal servo position %f; must be in interval [%f,%f]", position, apiPositionFirst, apiPositionLast));
        }
    }
