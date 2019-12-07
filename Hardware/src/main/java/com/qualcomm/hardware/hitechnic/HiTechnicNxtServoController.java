/*
 * Copyright (c) 2014, 2015 Qualcomm Technologies Inc
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * (subject to the limitations in the disclaimer below) provided that the following conditions are
 * met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the
 * distribution.
 *
 * Neither the name of Qualcomm Technologies Inc nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS LICENSE. THIS
 * SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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
package com.qualcomm.hardware.hitechnic;

import android.content.Context;

import com.qualcomm.hardware.R;
import com.qualcomm.robotcore.hardware.I2cAddr;
import com.qualcomm.robotcore.hardware.I2cController;
import com.qualcomm.robotcore.hardware.I2cDeviceSynch;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.ServoController;
import com.qualcomm.robotcore.hardware.configuration.ModernRoboticsConstants;
import com.qualcomm.robotcore.util.LastKnown;
import com.qualcomm.robotcore.util.Range;

/**
 * HiTechnic NXT Servo Controller
 */
public final class HiTechnicNxtServoController extends HiTechnicNxtController implements ServoController
    {
    //------------------------------------------------------------------------------------------------
    // Constants
    //------------------------------------------------------------------------------------------------

    protected static final I2cAddr I2C_ADDRESS = I2cAddr.create8bit(2);
    protected static final int SERVO_FIRST = ModernRoboticsConstants.INITIAL_SERVO_PORT;
    protected static final int SERVO_LAST = ModernRoboticsConstants.INITIAL_SERVO_PORT + ModernRoboticsConstants.NUMBER_OF_SERVOS -1;

    protected static final byte PWM_ENABLE = (byte) 0x00;
    protected static final byte PWM_ENABLE_WITHOUT_TIMEOUT = (byte) 0xaa;
    protected static final byte PWM_DISABLE = (byte) 0xff;

    protected static final byte[] ADDRESS_CHANNEL_MAP = new byte[]{(byte)-1/*not used*/, (byte)0x42, (byte)0x43, (byte)0x44, (byte)0x45, (byte)0x46, (byte)0x47};
    protected static final int ADDRESS_PWM = 0x48;

    protected static final int iRegWindowFirst = 0x40;
    protected static final int iRegWindowMax   = 0x48+1;  // first register not included

    protected static final double apiPositionMin   = Servo.MIN_POSITION;
    protected static final double apiPositionMax   = Servo.MAX_POSITION;
    protected static final double servoPositionMin =   0.0;
    protected static final double servoPositionMax = 255.0;

    //------------------------------------------------------------------------------------------------
    // State
    //------------------------------------------------------------------------------------------------

    protected LastKnown<Double>[] commandedServoPositions;
    protected LastKnown<Boolean>  lastKnownPwmEnabled;

    //------------------------------------------------------------------------------------------------
    // Construction
    //------------------------------------------------------------------------------------------------

    public HiTechnicNxtServoController(final Context context, I2cController module, int physicalPort)
        {
        super(context, module, physicalPort, I2C_ADDRESS);
        this.commandedServoPositions = LastKnown.createArray(ADDRESS_CHANNEL_MAP.length);
        this.lastKnownPwmEnabled = new LastKnown<Boolean>();

        // The NXT HiTechnic servo controller will time out if it doesn't receive any I2C communication for
        // a while. So we set up a heartbeat request to try to prevent that. We try to use
        // heartbeats which are as minimally disruptive as possible.
        I2cDeviceSynch.HeartbeatAction heartbeatAction = new I2cDeviceSynch.HeartbeatAction(true, true,
            new I2cDeviceSynch.ReadWindow(ADDRESS_CHANNEL_MAP[1], 1, I2cDeviceSynch.ReadMode.ONLY_ONCE));

        // Per the HiTechnic servo controller spec, there is a ten second timeout
        this.i2cDeviceSynch.setHeartbeatAction(heartbeatAction);
        this.i2cDeviceSynch.setHeartbeatInterval(9000);
        this.i2cDeviceSynch.enableWriteCoalescing(true);   // it's useful to us, at least in theory, if several positions must be set. And it is harmless, here.

        // Also: set up a read-window. We make it BALANCED to avoid unnecessary ping-ponging
        // between read mode and write mode, since servos are read way less than they are
        // written, but we make it relatively large so that least that when we DO go
        // into read mode and possibly do more than one read we will use this window
        // and won't have to fiddle with the 'switch to read mode' each and every time.
        this.i2cDeviceSynch.setReadWindow(new I2cDeviceSynch.ReadWindow(iRegWindowFirst, iRegWindowMax - iRegWindowFirst, I2cDeviceSynch.ReadMode.BALANCED));

        finishConstruction();
        }

    protected void controllerNowArmedOrPretending()
        {
        adjustHookingToMatchEngagement();
        }

    @Override
    protected void doHook()
        {
        this.i2cDeviceSynch.engage();
        }

    @Override
    public void initializeHardware()
        {
        pwmDisable();
        }

    @Override
    protected void doUnhook()
        {
        this.i2cDeviceSynch.disengage();
        }

    //------------------------------------------------------------------------------------------------
    // HardwareDevice interface
    //------------------------------------------------------------------------------------------------

    @Override public Manufacturer getManufacturer()
        {
        return Manufacturer.HiTechnic;
        }

    @Override
    public String getDeviceName()
        {
        return context.getString(R.string.nxtServoControllerName);
        }

    @Override
    public String getConnectionInfo()
        {
        return String.format(context.getString(R.string.controllerPortConnectionInfoFormat), controller.getConnectionInfo(), this.physicalPort);
        }

    @Override
    public void resetDeviceConfigurationForOpMode()
        {
        floatHardware();
        }

    @Override
    public int getVersion()
        {
        return 2;
        }

    //------------------------------------------------------------------------------------------------
    // ServoController interface
    //------------------------------------------------------------------------------------------------

    @Override
    public synchronized void pwmEnable()
        {
        // Avoid doing this repeatedly as we call it each and every position change
        if (lastKnownPwmEnabled.updateValue(true))
            {
            this.write8(ADDRESS_PWM, PWM_ENABLE);
            }
        }

    @Override
    public synchronized void pwmDisable()
        {
        if (lastKnownPwmEnabled.updateValue(false))
            {
            this.write8(ADDRESS_PWM, PWM_DISABLE);

            // Make any subsequent positioning actually actuate
            for (LastKnown<Double> commandedPosition : commandedServoPositions)
                {
                commandedPosition.invalidate();
                }
            }
        }

    @Override
    public synchronized PwmStatus getPwmStatus()
        {
        return this.read8(ADDRESS_PWM) == PWM_DISABLE
            ? PwmStatus.DISABLED
            : PwmStatus.ENABLED;
        }

    @Override
    public synchronized void setServoPosition(int servo, double position)
        {
        validateServo(servo);
        position = Range.clip(position, apiPositionMin, apiPositionMax);
        validateApiPosition(position);  // will catch NaNs, for example

        // Don't update if we know we're already there
        if (commandedServoPositions[servo].updateValue(position))
            {
            byte bPosition = (byte)Range.scale(position, apiPositionMin, apiPositionMax, servoPositionMin, servoPositionMax);
            this.write8(ADDRESS_CHANNEL_MAP[servo], bPosition);
            this.pwmEnable();
            }
        }

    @Override
    public synchronized double getServoPosition(int servo)
    // One would think we could just read the servo position registers. But they always report as zero
        {
        validateServo(servo);
        Double commanded = this.commandedServoPositions[servo].getRawValue();
        return commanded==null ? Double.NaN : commanded;
        }

    //------------------------------------------------------------------------------------------------
    // Utility
    //------------------------------------------------------------------------------------------------

    @Override protected void floatHardware()
        {
        this.pwmDisable();
        }

    private void validateServo(int servo)
        {
        if (servo < SERVO_FIRST || servo > SERVO_LAST)
            {
            throw new IllegalArgumentException(String.format("Servo %d is invalid; valid servos are %d..%d", servo, SERVO_FIRST, SERVO_LAST));
            }
        }

    private void validateApiPosition(double position)
        {
        if (apiPositionMin <= position && position <= apiPositionMax)
            {
            // all is well
            }
        else
            throw new IllegalArgumentException(String.format("illegal servo position %f; must be in interval [%f,%f]", position, apiPositionMin, apiPositionMax));
        }
    }
