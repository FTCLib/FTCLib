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

import com.qualcomm.hardware.R;
import com.qualcomm.hardware.lynx.commands.standard.LynxNack;
import com.qualcomm.robotcore.exception.TargetPositionNotSetException;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

/**
 * Created by bob on 2016-12-11.
 */
@SuppressWarnings("WeakerAccess")
public class LynxCommExceptionHandler
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected String tag = RobotLog.TAG;

    protected String getTag()
        {
        return tag;
        }

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public LynxCommExceptionHandler()
        {
        }

    public LynxCommExceptionHandler(String tag)
        {
        this.tag = tag;
        }

    //----------------------------------------------------------------------------------------------
    // Exceptions
    //----------------------------------------------------------------------------------------------

    /** Returns true if command was supported (so: exception was something else) or false if the exception indicated that the command wasn't supported */ 
    protected boolean handleException(Exception e)
        {
        boolean commandIsSupported = true;
        if (e instanceof InterruptedException)
            handleSpecificException((InterruptedException)e);
        else if (e instanceof LynxNackException)
            {
            LynxNackException nackException = (LynxNackException)e;
            handleSpecificException(nackException);
            if (nackException.getNack().getNackReasonCode().isUnsupportedReason())
                {
                commandIsSupported = false;
                }
            }
        else if (e instanceof TargetPositionNotSetException) // Must be checked before RuntimeException
            handleSpecificException((TargetPositionNotSetException)e);
        else if (e instanceof RuntimeException)
            handleSpecificException((RuntimeException)e);
        else
            {
            RobotLog.ee(getTag(), e, "unexpected exception thrown during lynx communication");
            }
        return commandIsSupported;
        }

    protected void handleSpecificException(InterruptedException e)
        {
        Thread.currentThread().interrupt();
        }

    protected void handleSpecificException(TargetPositionNotSetException e)
        {
        // We want TargetPositionNotSetExceptions to stop the OpMode and be seen by the user, so we re-throw
        throw e;
        }

    protected void handleSpecificException(RuntimeException e)
        {
        RobotLog.ee(getTag(), e, "exception thrown during lynx communication");
        }

    /** @see LynxNack.ReasonCode#isUnsupportedReason() */
    protected void handleSpecificException(LynxNackException nackException)
        {
        switch (nackException.getNack().getNackReasonCode())
            {
            case ABANDONED_WAITING_FOR_ACK:
                RobotLog.ww(getTag(), "%s was abandoned waiting for ack", nackException.getCommand().getClass().getSimpleName());
                break;
            case ABANDONED_WAITING_FOR_RESPONSE:
                RobotLog.ww(getTag(), "%s was abandoned waiting for response", nackException.getCommand().getClass().getSimpleName());
                break;
            case COMMAND_IMPL_PENDING:
                RobotLog.ww(getTag(), "%s not implemented by lynx hw; ignoring", nackException.getCommand().getClass().getSimpleName());
                break;
            case COMMAND_ROUTING_ERROR:
                RobotLog.ee(getTag(), "%s not delivered in module mod#=%d cmd#=%d", nackException.getCommand().getClass().getSimpleName(), nackException.getNack().getModuleAddress(), nackException.getNack().getCommandNumber());
                break;
            case PACKET_TYPE_ID_UNKNOWN:
                RobotLog.ee(getTag(), "%s not supported by module mod#=%d cmd#=%d", nackException.getCommand().getClass().getSimpleName(), nackException.getNack().getModuleAddress(), nackException.getNack().getCommandNumber());
                break;
            case BATTERY_TOO_LOW_TO_RUN_MOTOR:
                RobotLog.setGlobalWarningMessage(AppUtil.getDefContext().getString(R.string.lynxBatteryToLowMotor));
                break;
            case BATTERY_TOO_LOW_TO_RUN_SERVO:
                RobotLog.setGlobalWarningMessage(AppUtil.getDefContext().getString(R.string.lynxBatteryToLowServo));
                break;
            default:
                RobotLog.ee(getTag(), nackException, "exception thrown during lynx communication");
            }
        }
    }
