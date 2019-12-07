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
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorController;
import com.qualcomm.robotcore.hardware.I2cAddr;
import com.qualcomm.robotcore.hardware.I2cDeviceSynch;
import com.qualcomm.robotcore.hardware.I2cWaitControl;
import com.qualcomm.robotcore.hardware.LegacyModule;
import com.qualcomm.robotcore.hardware.VoltageSensor;
import com.qualcomm.robotcore.hardware.configuration.ModernRoboticsConstants;
import com.qualcomm.robotcore.hardware.configuration.typecontainers.MotorConfigurationType;
import com.qualcomm.robotcore.util.LastKnown;
import com.qualcomm.robotcore.util.Range;
import com.qualcomm.robotcore.util.TypeConversion;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * HiTechnic NXT DC Motor Controller
 */
@SuppressWarnings("unused,WeakerAccess")
public final class HiTechnicNxtDcMotorController extends HiTechnicNxtController implements DcMotorController, VoltageSensor
    {
    //----------------------------------------------------------------------------------------------
    // Constants
    //----------------------------------------------------------------------------------------------

    protected static final int MOTOR_FIRST = ModernRoboticsConstants.INITIAL_MOTOR_PORT;                                                // first valid motor number value
    protected static final int MOTOR_LAST  = ModernRoboticsConstants.INITIAL_MOTOR_PORT + ModernRoboticsConstants.NUMBER_OF_MOTORS -1;  // last valid motor number value
    protected static final int MOTOR_MAX   = MOTOR_LAST + 1;    // first invalid motor number value

    protected static final I2cAddr I2C_ADDRESS  = I2cAddr.create8bit(2);

    protected static final int OFFSET_UNUSED = -1;

    protected static final int CHANNEL_MODE_MASK_SELECTION  = 0x03;
    protected static final int CHANNEL_MODE_MASK_LOCK       = 0x04;
    protected static final int CHANNEL_MODE_MASK_REVERSE    = 0x08;
    protected static final int CHANNEL_MODE_MASK_NO_TIMEOUT = 0x10;
    protected static final int CHANNEL_MODE_MASK_EMPTY_D5   = 0x20;
    protected static final int CHANNEL_MODE_MASK_ERROR      = 0x40;
    protected static final int CHANNEL_MODE_MASK_BUSY       = 0x80;

    protected static final byte CHANNEL_MODE_FLAG_SELECT_RUN_POWER_CONTROL_ONLY_NXT = (byte) 0x0;
    protected static final byte CHANNEL_MODE_FLAG_SELECT_RUN_CONSTANT_SPEED_NXT     = (byte) 0x1;
    protected static final byte CHANNEL_MODE_FLAG_SELECT_RUN_TO_POSITION            = (byte) 0x2;
    protected static final byte CHANNEL_MODE_FLAG_SELECT_RESET                      = (byte) 0x3;

    protected static final byte[] ADDRESS_MOTOR_POWER_MAP                 = new byte[]{OFFSET_UNUSED, (byte)0x45, (byte)0x46};
    protected static final byte[] ADDRESS_MOTOR_MODE_MAP                  = new byte[]{OFFSET_UNUSED, (byte)0x44, (byte)0x47};
    protected static final byte[] ADDRESS_MOTOR_TARGET_ENCODER_VALUE_MAP  = new byte[]{OFFSET_UNUSED, (byte)0x40, (byte)0x48};
    protected static final byte[] ADDRESS_MOTOR_CURRENT_ENCODER_VALUE_MAP = new byte[]{OFFSET_UNUSED, (byte)0x4c, (byte)0x50};

    protected static final int iRegWindowFirst = 0x40;
    protected static final int iRegWindowMax   = 0x56;  // first register not included

    protected static final byte bPowerBrake = 0;
    protected static final byte bPowerFloat = -128;
    protected static final byte bPowerMax   = 100;
    protected static final byte bPowerMin   = -100;
    protected static final byte cbEncoder   = 4;

    protected static final double apiPowerMin = -1.0;
    protected static final double apiPowerMax = 1.0;

    public final static int BUSY_THRESHOLD = 5;

    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    static class MotorProperties
        {
        // We have caches of values that we *could* read from the controller, and need to
        // do so if the cache is invalid
        LastKnown<Byte>             lastKnownPowerByte          = new LastKnown<Byte>();
        LastKnown<Integer>          lastKnownTargetPosition     = new LastKnown<Integer>();
        LastKnown<DcMotor.RunMode>  lastKnownMode               = new LastKnown<DcMotor.RunMode>();

        // The remainder of the data is authoritative, here
        DcMotor.ZeroPowerBehavior   zeroPowerBehavior          = DcMotor.ZeroPowerBehavior.BRAKE;
        boolean                     modeSwitchCompletionNeeded = false;
        DcMotor.RunMode             prevRunMode                = null;
        double                      prevPower;
        MotorConfigurationType      motorType                  = MotorConfigurationType.getUnspecifiedMotorType();
        }

    final MotorProperties[] motors = new MotorProperties[MOTOR_MAX];

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public HiTechnicNxtDcMotorController(final Context context, LegacyModule module, int physicalPort)
        {
        super(context, module, physicalPort, I2C_ADDRESS);
        for (int motor = 0; motor < motors.length; motor++)
            {
            motors[motor] = new MotorProperties();
            }

        // The NXT HiTechnic motor controller will time out if it doesn't receive any I2C communication for
        // 2.5 seconds. So we set up a heartbeat request to try to prevent that. We try to use
        // heartbeats which are as minimally disruptive as possible. Note as a matter of interest
        // that the heartbeat mechanism historically used was analogous to
        // 'rewriteLastWritten'.
        I2cDeviceSynch.HeartbeatAction heartbeatAction = new I2cDeviceSynch.HeartbeatAction(true, true,
            new I2cDeviceSynch.ReadWindow(ADDRESS_MOTOR_CURRENT_ENCODER_VALUE_MAP[1], 1, I2cDeviceSynch.ReadMode.ONLY_ONCE));

        this.i2cDeviceSynch.setHeartbeatAction(heartbeatAction);
        this.i2cDeviceSynch.setHeartbeatInterval(2000);
        this.i2cDeviceSynch.enableWriteCoalescing(true);   // it's useful to us, particularly for setting motor speeds

        // Also: set up a read-window. We make it BALANCED to avoid unnecessary ping-ponging
        // between read mode and write mode, since motors are read about as much as they are
        // written, but we make it relatively large so that least that when we DO go
        // into read mode and possibly do more than one read we will use this window
        // and won't have to fiddle with the 'switch to read mode' each and every time.
        // We include everything from the 'Motor 1 target encoder value' through the battery voltage.
        this.i2cDeviceSynch.setReadWindow(new I2cDeviceSynch.ReadWindow(iRegWindowFirst, iRegWindowMax - iRegWindowFirst, I2cDeviceSynch.ReadMode.BALANCED));

        finishConstruction();
        }

    //----------------------------------------------------------------------------------------------
    // Arming and disarming
    //----------------------------------------------------------------------------------------------

    void brakeAllAtZero()
        {
        for (int motor = MOTOR_FIRST; motor <= MOTOR_LAST; motor++)
            {
            motors[motor].zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE;
            }
        }

    void forgetLastKnown()
        {
        for (int motor = MOTOR_FIRST; motor <= MOTOR_LAST; motor++)
            {
            motors[motor].lastKnownMode.invalidate();
            motors[motor].lastKnownPowerByte.invalidate();
            motors[motor].lastKnownTargetPosition.invalidate();
            }
        }

    void forgetLastKnownPowers()
        {
        for (int motor = MOTOR_FIRST; motor <= MOTOR_LAST; motor++)
            {
            motors[motor].lastKnownPowerByte.invalidate();
            }
        }

    /**
     * Our controller is newly armed or pretending, or we're newly constructed on a controller
     */
    @Override protected void controllerNowArmedOrPretending()
        {
        adjustHookingToMatchEngagement();
        }

    @Override
    protected void doHook()
        {
        forgetLastKnown();
        this.i2cDeviceSynch.engage();
        }

    @Override
    public void initializeHardware()
        {
        initPID();
        floatHardware();
        }

    @Override
    protected void doUnhook()
        {
        this.i2cDeviceSynch.disengage();
        forgetLastKnown();  // perhaps unneeded, but harmless
        }

    //----------------------------------------------------------------------------------------------
    // HardwareDevice
    //----------------------------------------------------------------------------------------------

    @Override
    public String getDeviceName()
        {
        return context.getString(R.string.nxtDcMotorControllerName);
        }

    @Override
    public String getConnectionInfo()
        {
        return String.format(context.getString(R.string.controllerPortConnectionInfoFormat), controller.getConnectionInfo(), this.physicalPort);
        }

    @Override
    public int getVersion()
        {
        return 2;
        }

    @Override
    public void resetDeviceConfigurationForOpMode()
        {
        floatHardware();
        runWithoutEncoders();
        brakeAllAtZero();
        forgetLastKnown();
        }

    //----------------------------------------------------------------------------------------------
    // DCMotorController
    //----------------------------------------------------------------------------------------------

    @Override public void resetDeviceConfigurationForOpMode(int motor)
        {
        validateMotor(motor);
        // Nothing to do
        }

    @Override public synchronized void setMotorType(int motor, MotorConfigurationType motorType)
        {
        validateMotor(motor);
        motors[motor].motorType = motorType;
        }

    @Override public synchronized MotorConfigurationType getMotorType(int motor)
        {
        this.validateMotor(motor);
        return motors[motor].motorType;
        }

    @Override public synchronized void setMotorMode(int motor, DcMotor.RunMode mode)
        {
        mode = mode.migrate();
        this.validateMotor(motor);
        finishModeSwitchIfNecessary(motor);

        DcMotor.RunMode prevMode = motors[motor].lastKnownMode.getNonTimedValue();
        if (motors[motor].lastKnownMode.updateValue(mode))
            {
            byte bNewMode = modeToByte(mode);

            // Set us up so that we'll await the completion of the mode switch before doing
            // anything else with this motor. We just won't take that time *right*now*.
            motors[motor].modeSwitchCompletionNeeded = true;
            motors[motor].prevRunMode = prevMode; // may be null
            motors[motor].prevPower = internalGetCachedOrQueriedMotorPower(motor);

            // We write the whole byte, but only the lower five bits are actually writable
            // and we only ever use the lowest two as non zero.
            this.write8(ADDRESS_MOTOR_MODE_MAP[motor], bNewMode);
            }
        }

    DcMotor.RunMode internalQueryRunMode(int motor)
        {
        byte b = this.i2cDeviceSynch.read8(ADDRESS_MOTOR_MODE_MAP[motor]);
        DcMotor.RunMode result = modeFromByte(b);
        motors[motor].lastKnownMode.setValue(result);
        return result;
        }

    void finishModeSwitchIfNecessary(int motor)
    // Here, we implement the logic that completes the mode switch of the motor. We separate
    // that out from setMotorChannelMode itself so as to allow parallel mode switching of motors.
    // A common paradigm where this happens is that of resetting the encoders across all the
    // motors on one's bot. Having this separate like this speeds that up, somewhat.
        {
        // If there's nothing we need to do, then gt out
        if (!motors[motor].modeSwitchCompletionNeeded)
            {
            return;
            }

        // Find out where we are. If forgetLastKnown has been called in the interim, we
        // might have to get the real mode.
        DcMotor.RunMode mode     = internalGetCachedOrQueriedRunMode(motor);
        DcMotor.RunMode prevMode = motors[motor].prevRunMode;

        byte bNewMode = modeToByte(mode);
        byte bRunWithoutEncoderMode = modeToByte(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        // The mode switch doesn't happen instantaneously. Wait for it,
        // so that the programmer's model is that he just needs to set the
        // mode and be done.
        for (;;)
            {
            if (!this.isArmed()) break;

            byte bCurrentMode = (byte)(this.i2cDeviceSynch.read8(ADDRESS_MOTOR_MODE_MAP[motor]) & CHANNEL_MODE_MASK_SELECTION);
            if (bCurrentMode==bNewMode)
                break;

            /** According to the manufacturer, STOP_AND_RESET_ENCODER mode is supposed to auto-transition
             * to RUN_WITHOUT_ENCODER after performing the reset. We, ourselves, though, have never actually
             * observed this behavior for HiTechnic controllers. However, in the interests of robustness,
             * we allow for that transition. See also {@link ModernRoboticsUsbDcMotorController#finishModeSwitchIfNecessary}.
             */
            if (mode==DcMotor.RunMode.STOP_AND_RESET_ENCODER && bCurrentMode==bRunWithoutEncoderMode)
                break;

            Thread.yield();
            }

        // If the mode is 'reset encoders', we don't want to return until the encoders have actually reset
        //      http://ftcforum.usfirst.org/showthread.php?4924-Use-of-RUN_TO_POSITION-in-LineraOpMode&highlight=reset+encoders
        //      http://ftcforum.usfirst.org/showthread.php?4567-Using-and-resetting-encoders-in-MIT-AI&p=19303&viewfull=1#post19303
        // For us, here, we believe we'll always *immediately* have that be true, as our writes
        // to the I2C device actually happen when we issue them. [old comment, perhaps stale]
        if (mode == DcMotor.RunMode.STOP_AND_RESET_ENCODER)
            {
            // Unclear if this is needed, but anecdotes from (e.g.) Dryw Wade seem to indicate that it is
            while (this.internalQueryMotorCurrentPosition(motor) != 0)
                {
                if (!this.isArmed()) break;
                Thread.yield();
                }
            }
        else
            {
            if (mode.isPIDMode() && (prevMode== null || !prevMode.isPIDMode()))
                {
                double prevPower = motors[motor].prevPower;
                if (mode == DcMotor.RunMode.RUN_TO_POSITION)
                    {
                    // Enforce that in RUN_TO_POSITION, we always need *positive* power. DCMotor will
                    // take care of that if we set power *after* we set the mode, but not the other way
                    // around. So we handle that here.
                    //
                    // Unclear that this is needed. The motor controller might take the absolute value
                    // automatically. But harmless in that uncertainty.
                    prevPower = Math.abs(prevPower);
                    }
                internalSetMotorPower(motor, prevPower);
                }

            else if (mode == DcMotor.RunMode.RUN_TO_POSITION)
                {
                double power = internalGetCachedOrQueriedMotorPower(motor);
                if (power < 0)
                    internalSetMotorPower(motor, Math.abs(power));
                }

            // If we just switched out of RESET_ENCODERS, attempt to restore the previous power setting.
            // Note that the need to do this is apparently confined to the legacy controller, not the
            // modern controller.
            if (DcMotor.RunMode.STOP_AND_RESET_ENCODER.equals(prevMode))
                {
                Byte bPower = motors[motor].lastKnownPowerByte.getRawValue();
                // bPower might be null in extreme shutdown situations when forgetLastKnown has been called
                if (bPower != null)
                    {
                    double power = internalMotorPowerFromByte(motor, bPower, prevMode);
                    internalSetMotorPower(motor, power);
                    }
                }
            }

        // On the legacy controller (at least, not sure about the modern one), when in RESET_ENCODERs
        // writes to power are ignored: the byte power value is always zero (off, braked) while in that
        // mode. So, transitioning either into or out of that mode, what we last thought we knew about
        // the power level is perhaps wrong, so we just forget what we thought we knew.
        forgetLastKnownPowers();

        // Ok, this mode switch is done!
        motors[motor].modeSwitchCompletionNeeded = false;
        }

    @Override public synchronized DcMotor.RunMode getMotorMode(int motor)
        {
        this.validateMotor(motor);
        finishModeSwitchIfNecessary(motor);
        return internalGetCachedOrQueriedRunMode(motor);
        }

    DcMotor.RunMode internalGetCachedOrQueriedRunMode(int motor)
        {
        DcMotor.RunMode mode = motors[motor].lastKnownMode.getNonTimedValue();
        if (mode == null)
            {
            mode = internalQueryRunMode(motor);
            }
        return mode;
        }

    // From the HiTechnic Motor Controller specification
    //
    //      The Run to position command will cause the firmware to run the motor to make the current encoder
    //      value to become equal to the target encoder value. It will do this using a maximum rotation rate
    //      as defined by the motor power byte. It will hold this position in a servo like mode until the Run
    //      to position command is changed or the target encoder value is changed. While the Run to position
    //      command is executing, the Busy bit will be set. Once the target position is achieved, the Busy bit
    //      will be cleared. There may be a delay of up to 50mS after a Run to position command is initiated
    //      before the Busy bit will be set.
    //
    // Our task here is to work around that 50ms issue

    @Override public synchronized boolean isBusy(int motor)
        {
        this.validateMotor(motor);
        finishModeSwitchIfNecessary(motor);

        return (Math.abs(getMotorTargetPosition(motor) - getMotorCurrentPosition(motor)) > BUSY_THRESHOLD);
        }

    @Override public synchronized void setMotorPower(int motor, double power)
        {
        this.validateMotor(motor);
        finishModeSwitchIfNecessary(motor);
        internalSetMotorPower(motor, power);
        }

    void internalSetMotorPower(int motor, double power)
        {
        power = Range.clip(power, apiPowerMin, apiPowerMax);
        this.validateApiMotorPower(power);  // may catch NaNs, for example

        byte bPower = (power == 0.0 && motors[motor].zeroPowerBehavior == DcMotor.ZeroPowerBehavior.FLOAT)
                ? bPowerFloat
                : (byte)Range.scale(power, apiPowerMin, apiPowerMax, bPowerMin, bPowerMax);
        internalSetMotorPower(motor, bPower);
        }

    void internalSetMotorPower(int motor, byte bPower)
        {
        if (motors[motor].lastKnownPowerByte.updateValue(bPower))
            {
            this.write8(ADDRESS_MOTOR_POWER_MAP[motor], bPower);
            }
        }

    double internalQueryMotorPower(int motor)
        {
        byte bPower = this.i2cDeviceSynch.read8(ADDRESS_MOTOR_POWER_MAP[motor]);
        motors[motor].lastKnownPowerByte.setValue(bPower);
        return internalMotorPowerFromByte(motor, bPower);
        }

    double internalGetCachedOrQueriedMotorPower(int motor)
        {
        Byte bPower = motors[motor].lastKnownPowerByte.getNonTimedValue();
        if (bPower != null)
            return internalMotorPowerFromByte(motor, bPower);
        else
            return internalQueryMotorPower(motor);
        }

    double internalMotorPowerFromByte(int motor, byte bPower)
        {
        if (bPower == bPowerFloat)
            return 0.0; // Float counts as zero power
        else
            {
            DcMotor.RunMode mode = internalGetCachedOrQueriedRunMode(motor);
            return internalMotorPowerFromByte(motor, bPower, mode);
            }
        }

    double internalMotorPowerFromByte(int motor, byte bPower, DcMotor.RunMode mode)
        {
        if (bPower == bPowerFloat)
            return 0.0; // Float counts as zero power
        else
            {
            double power = Range.scale(bPower, bPowerMin, bPowerMax, apiPowerMin, apiPowerMax);
            return Range.clip(power, apiPowerMin, apiPowerMax);
            }
        }

    @Override public synchronized double getMotorPower(int motor)
        {
        this.validateMotor(motor);
        finishModeSwitchIfNecessary(motor);
        return internalGetCachedOrQueriedMotorPower(motor);
        }

    @Override public synchronized void setMotorZeroPowerBehavior(int motor, DcMotor.ZeroPowerBehavior zeroPowerBehavior)
        {
        this.validateMotor(motor);
        if (zeroPowerBehavior == DcMotor.ZeroPowerBehavior.UNKNOWN) throw new IllegalArgumentException("zeroPowerBehavior may not be UNKNOWN");
        finishModeSwitchIfNecessary(motor);

        if (motors[motor].zeroPowerBehavior != zeroPowerBehavior)
            {
            motors[motor].zeroPowerBehavior = zeroPowerBehavior;

            // If we're currently stopped, then reissue power to cause new zero behavior to take effect
            if (internalGetCachedOrQueriedMotorPower(motor) == 0.0)
                {
                motors[motor].lastKnownPowerByte.invalidate();  // be sure we reissue
                internalSetMotorPower(motor, 0.0);
                }
            }
        }

    @Override public synchronized DcMotor.ZeroPowerBehavior getMotorZeroPowerBehavior(int motor)
        {
        this.validateMotor(motor);
        finishModeSwitchIfNecessary(motor);

        return motors[motor].zeroPowerBehavior;
        }

    protected synchronized void setMotorPowerFloat(int motor)
        {
        this.validateMotor(motor);
        finishModeSwitchIfNecessary(motor);

        this.write8(ADDRESS_MOTOR_POWER_MAP[motor], bPowerFloat);
        }

    @Override public synchronized boolean getMotorPowerFloat(int motor)
        {
        this.validateMotor(motor);
        finishModeSwitchIfNecessary(motor);

        byte bPower = this.i2cDeviceSynch.read8(ADDRESS_MOTOR_POWER_MAP[motor]);
        return bPower == bPowerFloat;
        }

    @Override public synchronized void setMotorTargetPosition(int motor, int position)
        {
        this.validateMotor(motor);
        finishModeSwitchIfNecessary(motor);

        if (motors[motor].lastKnownTargetPosition.updateValue(position))
            {
            // We rely here on the fact that sizeof(int) == cbEncoder
            byte[] bytes = TypeConversion.intToByteArray(position, ByteOrder.BIG_ENDIAN);
            this.write(ADDRESS_MOTOR_TARGET_ENCODER_VALUE_MAP[motor], bytes);
            }
        }

    int internalQueryMotorTargetPosition(int motor)
        {
        byte[] bytes = this.i2cDeviceSynch.read(ADDRESS_MOTOR_TARGET_ENCODER_VALUE_MAP[motor], cbEncoder);
        int result = TypeConversion.byteArrayToInt(bytes, ByteOrder.BIG_ENDIAN);
        motors[motor].lastKnownTargetPosition.setValue(result);
        return result;
        }

    @Override public synchronized int getMotorTargetPosition(int motor)
        {
        this.validateMotor(motor);
        finishModeSwitchIfNecessary(motor);
        return internalQueryMotorTargetPosition(motor);
        }

    @Override public synchronized int getMotorCurrentPosition(int motor)
        {
        this.validateMotor(motor);
        finishModeSwitchIfNecessary(motor);
        return internalQueryMotorCurrentPosition(motor);
        }

    int internalQueryMotorCurrentPosition(int motor)
        {
        byte[] bytes = this.i2cDeviceSynch.read(ADDRESS_MOTOR_CURRENT_ENCODER_VALUE_MAP[motor], cbEncoder);
        return TypeConversion.byteArrayToInt(bytes, ByteOrder.BIG_ENDIAN);
        }

    //----------------------------------------------------------------------------------------------
    // Utility
    //----------------------------------------------------------------------------------------------

    protected void validateMotor(int motor)
        {
        if (motor < MOTOR_FIRST || motor > MOTOR_LAST)
            {
            throw new IllegalArgumentException(String.format("Motor %d is invalid; valid motors are %d..%d", motor, MOTOR_FIRST, MOTOR_LAST));
            }
        }

    private void validateApiMotorPower(double power)
        {
        if (apiPowerMin <= power && power <= apiPowerMax)
            {
            // all is well
            }
        else
            throw new IllegalArgumentException(String.format("illegal motor power %f; must be in interval [%f,%f]", power, apiPowerMin, apiPowerMax));
        }

    public static DcMotor.RunMode modeFromByte(byte flag)
        {
        switch (flag & CHANNEL_MODE_MASK_SELECTION)
            {
            case CHANNEL_MODE_FLAG_SELECT_RUN_POWER_CONTROL_ONLY_NXT: return DcMotor.RunMode.RUN_WITHOUT_ENCODER;
            case CHANNEL_MODE_FLAG_SELECT_RUN_CONSTANT_SPEED_NXT:     return DcMotor.RunMode.RUN_USING_ENCODER;
            case CHANNEL_MODE_FLAG_SELECT_RUN_TO_POSITION:            return DcMotor.RunMode.RUN_TO_POSITION;
            case CHANNEL_MODE_FLAG_SELECT_RESET:                      return DcMotor.RunMode.STOP_AND_RESET_ENCODER;
            }
        return DcMotor.RunMode.RUN_WITHOUT_ENCODER;
        }

    public static byte modeToByte(DcMotor.RunMode mode)
        {
        switch (mode.migrate())
            {
            case RUN_USING_ENCODER:         return CHANNEL_MODE_FLAG_SELECT_RUN_CONSTANT_SPEED_NXT;
            case RUN_WITHOUT_ENCODER:       return CHANNEL_MODE_FLAG_SELECT_RUN_POWER_CONTROL_ONLY_NXT;
            case RUN_TO_POSITION:           return CHANNEL_MODE_FLAG_SELECT_RUN_TO_POSITION;
            case STOP_AND_RESET_ENCODER:    return CHANNEL_MODE_FLAG_SELECT_RESET;
            }
        return CHANNEL_MODE_FLAG_SELECT_RUN_CONSTANT_SPEED_NXT;
        }

    protected void initPID()
        {
        // nothing to do here, it seems
        }

    @Override protected void floatHardware()
        {
        for (int motor = MOTOR_FIRST; motor <= MOTOR_LAST; motor++)
            {
            this.setMotorPowerFloat(motor);
            }
        i2cDeviceSynch.waitForWriteCompletions(I2cWaitControl.ATOMIC);  // paranoia about safety, probably no longer needed as ReadWriteRunnable's now drain writes on close
        }

    private void runWithoutEncoders()
        {
        for (int motor = MOTOR_FIRST; motor <= MOTOR_LAST; motor++)
            {
            this.setMotorMode(motor, DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            }
        }

    //----------------------------------------------------------------------------------------------
    // VoltageSensor
    //----------------------------------------------------------------------------------------------

    @Override
    public double getVoltage()
        {
        try
            {
            // Register is per the HiTechnic motor controller specification
            byte[] bytes = this.i2cDeviceSynch.read(0x54, 2);

            // "The high byte is the upper 8 bits of a 10 bit value. It may be used as an 8 bit
            // representation of the battery voltage in units of 80mV. This provides a measurement
            // range of 0 – 20.4 volts. The low byte has the lower 2 bits at bit locations 0 and 1
            // in the byte. This increases the measurement resolution to 20mV."
            bytes[1] = (byte) (bytes[1] << 6);
            ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
            int tenBits = (buffer.getShort() >> 6) & 0x3FF;
            double result = ((double) tenBits) * 0.020;
            return result;
            }
        catch (RuntimeException e)
            {
            // Protect our clients from somehow getting an I2c related exception
            return 0;
            }
        }
    }
