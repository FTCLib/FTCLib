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
package com.qualcomm.hardware.modernrobotics;

import android.content.Context;

import com.qualcomm.hardware.HardwareDeviceManager;
import com.qualcomm.hardware.HardwareFactory;
import com.qualcomm.hardware.R;
import com.qualcomm.hardware.modernrobotics.comm.ReadWriteRunnable;
import com.qualcomm.hardware.modernrobotics.comm.ReadWriteRunnableSegment;
import com.qualcomm.hardware.modernrobotics.comm.ReadWriteRunnableStandard;
import com.qualcomm.robotcore.eventloop.SyncdDevice;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorController;
import com.qualcomm.robotcore.hardware.VoltageSensor;
import com.qualcomm.robotcore.hardware.configuration.ModernRoboticsConstants;
import com.qualcomm.robotcore.hardware.configuration.ModernRoboticsMotorControllerParamsState;
import com.qualcomm.robotcore.hardware.configuration.typecontainers.MotorConfigurationType;
import com.qualcomm.robotcore.hardware.usb.RobotUsbDevice;
import com.qualcomm.robotcore.util.DifferentialControlLoopCoefficients;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.LastKnown;
import com.qualcomm.robotcore.util.Range;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.SerialNumber;
import com.qualcomm.robotcore.util.TypeConversion;

import org.firstinspires.ftc.robotcore.internal.system.Assert;

import java.nio.ByteOrder;

/**
 * Modern Robotics USB DC Motor Controller
 * <p>
 * This is an implementation of {@link DcMotorController}
 * <p>
 * Modern Robotics USB DC Motor Controllers have a Voltage Sensor that measures the
 * current voltage of the main robot battery.
 * <p>
 * Use {@link HardwareDeviceManager} to create an instance of this class
 */
@SuppressWarnings("unused,WeakerAccess")
public final class ModernRoboticsUsbDcMotorController extends ModernRoboticsUsbController implements DcMotorController, VoltageSensor
    {
    //----------------------------------------------------------------------------------------------
    // Constants
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "MRMotorController";
    @Override protected String getTag() { return TAG; }

    /**
     * Enable DEBUG_LOGGING logging
     */
    protected static final boolean DEBUG_LOGGING = false;

    /**
     * const values used by this class
     */
    protected static final int MONITOR_LENGTH = 0x1e;
    protected static final int MOTOR_FIRST = ModernRoboticsConstants.INITIAL_MOTOR_PORT;                                                // first valid motor number value
    protected static final int MOTOR_LAST  = ModernRoboticsConstants.INITIAL_MOTOR_PORT + ModernRoboticsConstants.NUMBER_OF_MOTORS -1;  // last valid motor number value
    protected static final int MOTOR_MAX   = MOTOR_LAST + 1;    // first invalid motor number value

    /**
     * const values used by motor controller
     */
    protected static final byte bPowerMax   = (byte)100;
    protected static final byte bPowerBrake = 0;
    protected static final byte bPowerMin   = (byte)-100;
    protected static final byte bPowerFloat = (byte)-128;
    protected static final byte RATIO_MIN = -0x80;
    protected static final byte RATIO_MAX = 0x7f;

    protected static final double apiPowerMin = -1.0;
    protected static final double apiPowerMax = 1.0;

    protected static final int DIFFERENTIAL_CONTROL_LOOP_COEFFICIENT_MAX = 0xff;
    protected static final int BATTERY_MAX_MEASURABLE_VOLTAGE_INT = 1023; // (2^10)-1
    protected static final double BATTERY_MAX_MEASURABLE_VOLTAGE = 20.4;

    protected static final byte DEFAULT_P_COEFFICIENT = (byte) 0x80;
    protected static final byte DEFAULT_I_COEFFICIENT = (byte) 0x40;
    protected static final byte DEFAULT_D_COEFFICIENT = (byte) 0xb8;

    protected static final byte START_ADDRESS = 0x40;

    /**
     * channel mode masks used by controller
     */
    protected static final int CHANNEL_MODE_MASK_SELECTION  = 0x03;
    protected static final int CHANNEL_MODE_MASK_LOCK       = 0x04;
    protected static final int CHANNEL_MODE_MASK_REVERSE    = 0x08;
    protected static final int CHANNEL_MODE_MASK_NO_TIMEOUT = 0x10;
    protected static final int CHANNEL_MODE_MASK_EMPTY_D5   = 0x20;
    protected static final int CHANNEL_MODE_MASK_ERROR      = 0x40;
    protected static final int CHANNEL_MODE_MASK_BUSY       = 0x80;

    /**
     * channel mode flags used by controller
     */
    protected static final byte CHANNEL_MODE_FLAG_SELECT_RUN_POWER_CONTROL_ONLY = (byte) 0x0;
    protected static final byte CHANNEL_MODE_FLAG_SELECT_RUN_CONSTANT_SPEED = (byte) 0x1;
    protected static final byte CHANNEL_MODE_FLAG_SELECT_RUN_TO_POSITION = (byte) 0x2;
    protected static final byte CHANNEL_MODE_FLAG_SELECT_RESET = (byte) 0x03;
    protected static final byte CHANNEL_MODE_FLAG_LOCK = (byte) 0x04;
    protected static final byte CHANNEL_MODE_FLAG_REVERSE = (byte) 0x8;
    protected static final byte CHANNEL_MODE_FLAG_NO_TIMEOUT = (byte) 0x10;
    protected static final byte CHANNEL_MODE_FLAG_UNUSED = (byte) 0x20;
    protected static final byte CHANNEL_MODE_FLAG_ERROR = (byte) 0x40;
    protected static final byte CHANNEL_MODE_FLAG_BUSY = (byte) 0x80;
    protected static final byte CHANNEL_MODE_UNKNOWN = (byte) 0xFF; // not a real mode

    /**
     * "I2c register addresses" used in the controller hardware
     */
    protected static final int  ADDRESS_PID_PARAMS_LOCK             = 0x03;
    protected static final byte PID_PARAMS_LOCK_DISABLE             = (byte)0xBB;
    protected static final byte PID_PARAMS_LOCK_ENABLE              = 0x00;
    //
    protected static final int ADDRESS_MOTOR1_TARGET_ENCODER_VALUE  = 0x40;
    protected static final int ADDRESS_MOTOR1_MODE                  = 0x44;
    protected static final int ADDRESS_MOTOR1_POWER                 = 0x45;
    protected static final int ADDRESS_MOTOR2_POWER                 = 0x46;
    protected static final int ADDRESS_MOTOR2_MODE                  = 0x47;
    protected static final int ADDRESS_MOTOR2_TARGET_ENCODER_VALUE  = 0x48;
    protected static final int ADDRESS_MOTOR1_CURRENT_ENCODER_VALUE = 0x4c;
    protected static final int ADDRESS_MOTOR2_CURRENT_ENCODER_VALUE = 0x50;
    protected static final int ADDRESS_BATTERY_VOLTAGE              = 0x54;
    protected static final int ADDRESS_MOTOR1_GEAR_RATIO            = 0x56;
    protected static final int ADDRESS_MOTOR1_P_COEFFICIENT         = 0x57;
    protected static final int ADDRESS_MOTOR1_I_COEFFICIENT         = 0x58;
    protected static final int ADDRESS_MOTOR1_D_COEFFICIENT         = 0x59;
    protected static final int ADDRESS_MOTOR2_GEAR_RATIO            = 0x5a;
    protected static final int ADDRESS_MOTOR2_P_COEFFICIENT         = 0x5b;
    protected static final int ADDRESS_MOTOR2_I_COEFFICIENT         = 0x5c;
    protected static final int ADDRESS_MOTOR2_D_COEFFICIENT         = 0x5d;

    protected static final int ADDRESS_UNUSED = 0xff;

    /**
     * map of motors to memory addresses
     */
    protected static final int[] ADDRESS_MOTOR_POWER_MAP                               = { ADDRESS_UNUSED, ADDRESS_MOTOR1_POWER, ADDRESS_MOTOR2_POWER};
    protected static final int[] ADDRESS_MOTOR_MODE_MAP                                = { ADDRESS_UNUSED, ADDRESS_MOTOR1_MODE, ADDRESS_MOTOR2_MODE};
    protected static final int[] ADDRESS_MOTOR_TARGET_ENCODER_VALUE_MAP                = { ADDRESS_UNUSED, ADDRESS_MOTOR1_TARGET_ENCODER_VALUE, ADDRESS_MOTOR2_TARGET_ENCODER_VALUE};
    protected static final int[] ADDRESS_MOTOR_CURRENT_ENCODER_VALUE_MAP               = { ADDRESS_UNUSED, ADDRESS_MOTOR1_CURRENT_ENCODER_VALUE, ADDRESS_MOTOR2_CURRENT_ENCODER_VALUE};
    protected static final int[] ADDRESS_MOTOR_GEAR_RATIO_MAP                          = { ADDRESS_UNUSED, ADDRESS_MOTOR1_GEAR_RATIO, ADDRESS_MOTOR2_GEAR_RATIO};
    protected static final int[] ADDRESS_MAX_DIFFERENTIAL_CONTROL_LOOP_COEFFICIENT_MAP = { ADDRESS_UNUSED, ADDRESS_MOTOR1_P_COEFFICIENT, ADDRESS_MOTOR2_P_COEFFICIENT};

    public final static int BUSY_THRESHOLD = 5;
    protected static final byte cbEncoder  = 4;
    protected static final int cbRatioPIDParams = 4;

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
        int                         modeSwitchWaitCount        = 0;
        int                         modeSwitchWaitCountMax     = 4;
        DcMotor.RunMode             prevRunMode                = null;
        double                      prevPower;
        MotorConfigurationType      motorType                  = MotorConfigurationType.getUnspecifiedMotorType();
        MotorConfigurationType      internalMotorType          = null;
        }

    final MotorProperties[] motors = new MotorProperties[MOTOR_MAX];
    protected ReadWriteRunnableSegment pidParamsLockSegment;
    protected ReadWriteRunnableSegment[] rgPidParamsSegment;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    /**
     * Use HardwareDeviceManager to create an instance of this class
     */
    public ModernRoboticsUsbDcMotorController(final Context context, final SerialNumber serialNumber, final OpenRobotUsbDevice openRobotUsbDevice, SyncdDevice.Manager manager)
            throws RobotCoreException, InterruptedException
        {
        super(context, serialNumber, manager, openRobotUsbDevice, new CreateReadWriteRunnable()
            {
            @Override public ReadWriteRunnable create(RobotUsbDevice device)
                {
                return new ReadWriteRunnableStandard(context, serialNumber, device, MONITOR_LENGTH, START_ADDRESS, DEBUG_LOGGING);
                }
            });
        for (int motor = 0; motor < motors.length; motor++)
            {
            motors[motor] = new MotorProperties();
            }
        }

    @Override
    public void initializeHardware()
        {
        // set all motors to float for safety reasons
        floatHardware();
        setDifferentialControlLoopCoefficientsToDefault();
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
            motors[motor].lastKnownPowerByte.invalidate();
            motors[motor].lastKnownMode.invalidate();
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

    protected void createSegments()
        {
        pidParamsLockSegment = readWriteRunnable.createSegment(0, ADDRESS_PID_PARAMS_LOCK, 1);
        rgPidParamsSegment = new ReadWriteRunnableSegment[MOTOR_MAX];
        for (int motor = MOTOR_FIRST; motor <= MOTOR_LAST; motor++)
            {
            rgPidParamsSegment[motor] = readWriteRunnable.createSegment(motor, ADDRESS_MOTOR_GEAR_RATIO_MAP[motor], cbRatioPIDParams);
            }
        }

    @Override protected void doArm() throws RobotCoreException, InterruptedException
        {
        doArmOrPretend(true);
        for (int motor = MOTOR_FIRST; motor <= MOTOR_LAST; motor++)
            {
            updateMotorParamsIfNecessary(motor);
            }
        }
    @Override protected void doPretend() throws RobotCoreException, InterruptedException
        {
        doArmOrPretend(false);
        }

    private void doArmOrPretend(boolean isArm) throws RobotCoreException, InterruptedException
        {
        RobotLog.d("arming modern motor controller %s%s...", HardwareFactory.getDeviceDisplayName(context, this.serialNumber), (isArm ? "" : " (pretend)"));

        forgetLastKnown();
        if (isArm)
            this.armDevice();
        else
            this.pretendDevice();

        createSegments();

        RobotLog.d("...arming modern motor controller %s complete", HardwareFactory.getDeviceDisplayName(context, this.serialNumber));
        }

    @Override protected void doDisarm() throws RobotCoreException, InterruptedException
        {
        RobotLog.d("disarming modern motor controller %s...", HardwareFactory.getDeviceDisplayName(context, this.serialNumber));

        this.disarmDevice();
        forgetLastKnown();  // perhaps unneeded, but harmless

        RobotLog.d("...disarming modern motor controller %s complete", HardwareFactory.getDeviceDisplayName(context, this.serialNumber));
        }

    @Override protected void doCloseFromArmed()
        {
        floatHardware();
        doCloseFromOther();
        }

    @Override protected void doCloseFromOther()
        {
        try {
            this.doDisarm();
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            }
        catch (RobotCoreException ignore)
            {
            // ignore, won't actually happen
            }
        }

    //----------------------------------------------------------------------------------------------
    // HardwareDevice interface
    //----------------------------------------------------------------------------------------------

    @Override public Manufacturer getManufacturer()
        {
        return Manufacturer.ModernRobotics;
        }

    /**
     * Device Name
     *
     * @return device name
     */
    @Override
    public String getDeviceName()
        {
        return String.format("%s %s", context.getString(R.string.moduleDisplayNameMotorController), this.robotUsbDevice.getFirmwareVersion());
        }

    @Override
    public String getConnectionInfo()
        {
        return "USB " + getSerialNumber();
        }

    @Override
    public void resetDeviceConfigurationForOpMode()
        {
        floatHardware();
        runWithoutEncoders();
        brakeAllAtZero();
        forgetLastKnown();
        }

    /**
     * Close this device
     */
    @Override public void doClose()
        {
        floatHardware();
        super.doClose();
        }

    //------------------------------------------------------------------------------------------------
    // DcMotorController interface
    //------------------------------------------------------------------------------------------------

    @Override public void resetDeviceConfigurationForOpMode(int motor)
        {
        this.validateMotor(motor);
        if (motors[motor].internalMotorType!=null)
            {
            if (motors[motor].motorType != motors[motor].internalMotorType)
                {
                setMotorType(motor, motors[motor].internalMotorType);
                }
            }
        }

    @Override public synchronized void setMotorType(int motor, MotorConfigurationType motorType)
        {
        this.validateMotor(motor);
        motors[motor].motorType = motorType;
        if (motors[motor].internalMotorType==null)
            {
            // First one is the system setting the type
            motors[motor].internalMotorType = motorType;
            }
        updateMotorParamsIfNecessary(motor);
        }

    @Override public synchronized MotorConfigurationType getMotorType(int motor)
        {
        this.validateMotor(motor);
        return motors[motor].motorType;
        }

    protected void updateMotorParamsIfNecessary(int motor)
        {
        if (this.robotUsbDevice != null && this.robotUsbDevice.getFirmwareVersion() != null && this.robotUsbDevice.getFirmwareVersion().majorVersion >= 2)
            {
            if (motors[motor].motorType.hasModernRoboticsParams())
                {
                ModernRoboticsMotorControllerParamsState newParams = motors[motor].motorType.getModernRoboticsParams();
                ModernRoboticsMotorControllerParamsState oldParams = readMotorParams(motor);
                //
                if (oldParams.equals(newParams))
                    {
                    RobotLog.vv(TAG, "motor params already correct: #=%d params=%s", motor, oldParams);
                    }
                else
                    {
                    // We model the update procedure from our observations of the Core Device Discovery
                    // tool (it's not like we have an actual spec, or something; sigh).
                    //
                    // We observe the following of note:
                    //  * The CDD seems to have a ~1500 ms delay before starts trying to write the PID params
                    //    Up till then, it's just been sitting there a-reading away the motor power etc.
                    //  * It then unlocks, writes, and locks in quick succession, taking no more than
                    //    40ms in total to do same. This seems to be at full speed.
                    //  * It then waits about 100ms before initiating its regular read cycle. This allows the 
                    //    write to the EEPROM to take place in peace.
                    // 
                    // During the update, and the wait, there's only the bare-bones minimally necessary to carry
                    // out the update going on. No extraneous reads or whatnot.
                    //
                    // We infer that the actual update to the EEPROM doesn't take place until the lock
                    // byte is re-locked.
                    //
                    RobotLog.vv(TAG, "updating motor params: #=%d old=%s new=%s", motor, oldParams, newParams);
                    readWriteRunnable.suppressReads(true);
                    try {
                        // paranoidSleep(1600); // we're thinking we don't really need the initial quiescence,
                        // that it's just an internal artifact of the CDD tool
                        setEEPromLock(false);
                        try {
                            writeSegment(rgPidParamsSegment[motor], newParams.toByteArray());
                            }
                        finally
                            {
                            setEEPromLock(true);
                            paranoidSleep(150); // 150ms == 100ms plus some extra slack just to be cautious
                            }
                        }
                    finally
                        {
                        readWriteRunnable.suppressReads(false);
                        waitForNextReadComplete();  // wait for read data to refresh
                        }
                    //
                    if (isArmed())
                        {
                        ModernRoboticsMotorControllerParamsState updatedParams = readMotorParams(motor);
                        Assert.assertTrue(updatedParams.equals(newParams));
                        }
                    }
                }
            }
        }

    ModernRoboticsMotorControllerParamsState readMotorParams(int motor)
        {
        byte[] gearPid = read(ADDRESS_MOTOR_GEAR_RATIO_MAP[motor], cbRatioPIDParams);
        return ModernRoboticsMotorControllerParamsState.fromByteArray(gearPid);
        }

    protected void setEEPromLock(boolean enable)
        {
        byte[] data = new byte[] { enable ? PID_PARAMS_LOCK_ENABLE : PID_PARAMS_LOCK_DISABLE };
        writeSegment(pidParamsLockSegment, data);
        }

    protected void writeSegment(ReadWriteRunnableSegment segment, byte[] data)
        {
        synchronized (concurrentClientLock)
            {
            synchronized (callbackLock)
                {
                segment.getWriteLock().lock();
                try {
                    this.writeStatus = WRITE_STATUS.DIRTY;
                    byte[] writeBuffer = segment.getWriteBuffer();
                    System.arraycopy(data, 0, writeBuffer, 0, Math.min(data.length, writeBuffer.length));
                    }
                finally
                    {
                    segment.getWriteLock().unlock();
                    }
                }
            readWriteRunnable.queueSegmentWrite(segment.getKey());
            }
        waitForNextReadComplete();
        }

    // Execute a wait that we include merely out of paranoia due to the fact that we don't actually 
    // have in hand the specification that would tell us whether such a wait is needed or not
    protected void paranoidSleep(int ms)
        {
        if (ms > 0)
            {
            try {
                Thread.sleep(ms);
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                }
            }
        }

    @Override public synchronized void setMotorMode(int motor, DcMotor.RunMode mode)
        {
        mode = mode.migrate();
        this.validateMotor(motor);
        finishModeSwitchIfNecessary(motor);

        DcMotor.RunMode prevMode = motors[motor].lastKnownMode.getNonTimedValue();
        if (motors[motor].lastKnownMode.updateValue(mode))
            {
            // Set us up so that we'll await the completion of the mode switch before doing
            // anything else with this motor. We just won't take that time *right*now*.
            motors[motor].modeSwitchCompletionNeeded = true;
            motors[motor].modeSwitchWaitCount = 0;
            motors[motor].prevRunMode = prevMode;
            motors[motor].prevPower = internalGetCachedOrQueriedMotorPower(motor);

            byte bNewMode = modeToByte(mode);
            this.write8(ADDRESS_MOTOR_MODE_MAP[motor], bNewMode);
            }
        }

    void finishModeSwitchIfNecessary(int motor)
    // Here, we implement the logic that completes the mode switch of the motor. We separate
    // that out from setMotorChannelMode itself so as to allow parallel mode switching of motors.
    // A common paradigm where this happens is that of resetting the encoders across all the
    // motors on one's bot. Having this separate like this speeds that up, somewhat.
        {
        // If there's nothing we need to do, then get out
        if (!motors[motor].modeSwitchCompletionNeeded)
            {
            return;
            }

        try {
            DcMotor.RunMode mode = internalGetCachedOrQueriedRunMode(motor);
            DcMotor.RunMode prevMode = motors[motor].prevRunMode;
            byte bNewMode = modeToByte(mode);
            byte bRunWithoutEncoderMode = modeToByte(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

            // The mode switch doesn't happen instantaneously. Wait for it,
            // so that the programmer's model is that he just needs to set the
            // mode and be done.
            for (;;)
                {
                if (!this.isArmed()) break;

                byte bCurrentMode = (byte)(this.read8(ADDRESS_MOTOR_MODE_MAP[motor]) & CHANNEL_MODE_MASK_SELECTION);
                if (bCurrentMode == bNewMode)
                    break;

                // Modern Robotics USB DC motor controllers with firmware >= 2.0 (note: only 2.0 exists
                // as of this writing) have a different behavior when switching to STOP_AND_RESET_ENCODER
                // mode: they only transiently enter that mode, then auto-switch to RUN_WITHOUT_ENCODER.
                // Note that the manufacturer informs us that this >= 2.0 behavior is in fact the behavior
                // on all firmware versions, but that's not consistent with our observations. For
                // robustness, however, we allow that transition on all firmware versions.
                if (mode==DcMotor.RunMode.STOP_AND_RESET_ENCODER && bCurrentMode==bRunWithoutEncoderMode)
                    break;

                // If we're waiting too long, then resend the mode switch. The theory is that
                // if switches are happening too quickly then one switch might have been missed.
                // Not a perfectly-understood theory, mind you, but there you go.
                if (motors[motor].modeSwitchWaitCount++ >= motors[motor].modeSwitchWaitCountMax)
                    {
                    RobotLog.dd(TAG, "mode resend: motor=[%s,%d] wait=%d from=%d to=%d cur=%d",
                            getSerialNumber(), motor, motors[motor].modeSwitchWaitCount-1,
                            (prevMode==null ? CHANNEL_MODE_UNKNOWN : modeToByte(prevMode)), bNewMode, bCurrentMode);
                    this.write8(ADDRESS_MOTOR_MODE_MAP[motor], bNewMode);
                    motors[motor].modeSwitchWaitCount = 0;
                    }

                // The above read8() reads from cache. To avoid flooding the system,
                // we wait for the next read cycle before we try again: the cache
                // isn't going to change until then.
                if (!waitForNextReadComplete()) break;
                }

            if (mode.isPIDMode() && (prevMode== null || !prevMode.isPIDMode()))
                {
                double power = motors[motor].prevPower;
                if (mode == DcMotor.RunMode.RUN_TO_POSITION)
                    {
                    // Enforce that in RUN_TO_POSITION, we always need *positive* power. DCMotor will
                    // take care of that if we set power *after* we set the mode, but not the other way
                    // around. So we handle that here.
                    //
                    // Unclear that this is needed. The motor controller might take the absolute value
                    // automatically. But harmless in that uncertainty.
                    power = Math.abs(power);
                    }
                internalSetMotorPower(motor, power);
                }

            else if (mode == DcMotor.RunMode.RUN_TO_POSITION)
                {
                double power = internalQueryMotorPower(motor);
                if (power < 0)
                    internalSetMotorPower(motor, Math.abs(power));
                }

            if (mode == DcMotor.RunMode.STOP_AND_RESET_ENCODER)
                {
                // If the mode is 'reset encoders', we don't want to return until the encoders have actually reset
                //      http://ftcforum.usfirst.org/showthread.php?4924-Use-of-RUN_TO_POSITION-in-LineraOpMode&highlight=reset+encoders
                //      http://ftcforum.usfirst.org/showthread.php?4567-Using-and-resetting-encoders-in-MIT-AI&p=19303&viewfull=1#post19303
                // For us, here, we believe we'll always *immediately* have that be true, as our writes
                // to the USB device actually happen when we issue them.
                // Unclear if this is needed, but anecdotes from (e.g.) Dryw Wade seem to indicate that it is

                long nsSendInterval = 100 * ElapsedTime.MILLIS_IN_NANO;
                long nsResendDeadline = System.nanoTime() + nsSendInterval;
                while (this.internalQueryMotorCurrentPosition(motor) != 0)
                    {
                    // Robustness: resend mode if we can't see zero'd encoders after basically forever
                    long nsNow = System.nanoTime();
                    if (nsNow > nsResendDeadline)
                        {
                        RobotLog.dd(TAG, "mode resend: motor=[%s,%d] mode=%s", getSerialNumber(), motor, mode);
                        this.write8(ADDRESS_MOTOR_MODE_MAP[motor], bNewMode);
                        nsResendDeadline = nsNow + nsSendInterval;
                        }

                    if (!this.isArmed()) break;
                    if (!waitForNextReadComplete()) break;
                    }
                }
            }
        finally
            {
            // On the legacy controller (at least, not sure about the modern one), when in RESET_ENCODERs
            // writes to power are ignored: the byte power value is always zero (off, braked) while in that
            // mode. So, transitioning either into or out of that mode, what we last thought we knew about
            // the power level is perhaps wrong, so we just forget what we thought we knew.
            //
            // Moreover, with the post-v2-firmware spontaneous mode switching, our cache of the modes
            // is suspect too. So, for robustness, we flush all our caches.
            forgetLastKnown();

            // Ok, this mode switch is done!
            motors[motor].modeSwitchCompletionNeeded = false;
            }
        }

    @Override public synchronized DcMotor.RunMode getMotorMode(int motor)
        {
        this.validateMotor(motor);
        finishModeSwitchIfNecessary(motor);
        return internalGetCachedOrQueriedRunMode(motor);
        }

    DcMotor.RunMode internalQueryRunMode(int motor)
        {
        byte b = this.read8(ADDRESS_MOTOR_MODE_MAP[motor]);
        DcMotor.RunMode result = modeFromByte(b);
        motors[motor].lastKnownMode.setValue(result);
        return result;
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
        byte bPower = this.read8(ADDRESS_MOTOR_POWER_MAP[motor]);
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
            double power = Range.scale(bPower, bPowerMin, bPowerMax, apiPowerMin, apiPowerMax);
            return power;
            }
        }

    @Override public synchronized double getMotorPower(int motor)
        {
        this.validateMotor(motor);
        finishModeSwitchIfNecessary(motor);
        return internalQueryMotorPower(motor);
        }

    // From the HiTechnic Motor Controller specification (the Modern Robotics motor controller is
    // understood to have the self-same issue):
    //
    //      The Run to position command will cause the firmware to run the motor to make the current encoder
    //      value to become equal to the target encoder value. It will do this using a maximum rotation rate
    //      as defined by the motor power byte. It will hold this position in a servo like mode until the Run
    //      to position command is changed or the target encoder value is changed. While the Run to position
    //      command is executing, the Busy bit will be set. Once the target position is achieved, the Busy bit
    //      will be cleared. There may be a delay of up to 50mS after a Run to position command is initiated
    //      before the Busy bit will be set.
    //
    // Our task here is to work around that 50ms issue.

    @Override
    public boolean isBusy(int motor)
        {
        validateMotor(motor);
        finishModeSwitchIfNecessary(motor);

        // Compare current and target positions to determine if RunToPosition is still busy.
        return (Math.abs(getMotorTargetPosition(motor) - getMotorCurrentPosition(motor)) > BUSY_THRESHOLD);
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

        byte bPower = this.read8(ADDRESS_MOTOR_POWER_MAP[motor]);
        return bPower == bPowerFloat;
        }

    @Override public synchronized void setMotorTargetPosition(int motor, int position)
        {
        this.validateMotor(motor);
        finishModeSwitchIfNecessary(motor);

        if (motors[motor].lastKnownTargetPosition.updateValue(position))
            {
            // We rely here on the fact that sizeof(int) == cbEncoder
            this.write(ADDRESS_MOTOR_TARGET_ENCODER_VALUE_MAP[motor], TypeConversion.intToByteArray(position, ByteOrder.BIG_ENDIAN));
            }
        }

    @Override public synchronized int getMotorTargetPosition(int motor)
        {
        this.validateMotor(motor);
        finishModeSwitchIfNecessary(motor);
        return internalQueryMotorTargetPosition(motor);
        }

    int internalQueryMotorTargetPosition(int motor)
        {
        byte[] rgbPosition = this.read(ADDRESS_MOTOR_TARGET_ENCODER_VALUE_MAP[motor], cbEncoder);
        int result = TypeConversion.byteArrayToInt(rgbPosition, ByteOrder.BIG_ENDIAN);
        motors[motor].lastKnownTargetPosition.setValue(result);
        return result;
        }

    @Override public synchronized int getMotorCurrentPosition(int motor)
        {
        this.validateMotor(motor);
        finishModeSwitchIfNecessary(motor);
        return internalQueryMotorCurrentPosition(motor);
        }

    int internalQueryMotorCurrentPosition(int motor)
        {
        byte[] bytes = this.read(ADDRESS_MOTOR_CURRENT_ENCODER_VALUE_MAP[motor], cbEncoder);
        return TypeConversion.byteArrayToInt(bytes, ByteOrder.BIG_ENDIAN);
        }

    //----------------------------------------------------------------------------------------------
    // VoltageSensor
    //----------------------------------------------------------------------------------------------

    /**
     * Get battery voltage. Measurements range from 0 to BATTERY_MAX_VOLTAGE. Measurement resolution
     * is 20mV.
     *
     * @return voltage
     */
    @Override
    public double getVoltage()
        {
        byte[] data = read(ADDRESS_BATTERY_VOLTAGE, 2);

        // data is in an unusual format, only the top 8 bits and bottom 2 bits count { XXXXXXXX, 000000XX }
        int voltage = TypeConversion.unsignedByteToInt(data[0]);
        voltage = voltage << 2;
        voltage += TypeConversion.unsignedByteToInt(data[1]) & 0x03;
        voltage = voltage & BATTERY_MAX_MEASURABLE_VOLTAGE_INT;

        // now calculate the percentage, relative to the max reading
        double percent = (double) (voltage) / (double) (BATTERY_MAX_MEASURABLE_VOLTAGE_INT);

        // scale to max value and return
        return percent * BATTERY_MAX_MEASURABLE_VOLTAGE;
        }

    public void setGearRatio(int motor, double ratio)
        {
        validateMotor(motor);
        Range.throwIfRangeIsInvalid(ratio, -1, 1);

        write(ADDRESS_MOTOR_GEAR_RATIO_MAP[motor], new byte[]{(byte) (ratio * RATIO_MAX)});
        }

    public double getGearRatio(int motor)
        {
        validateMotor(motor);

        byte[] data = read(ADDRESS_MOTOR_GEAR_RATIO_MAP[motor], 1);
        return (double) data[0] / (double) RATIO_MAX;
        }

    public void setDifferentialControlLoopCoefficients(int motor,
                                                       DifferentialControlLoopCoefficients pid)
        {
        validateMotor(motor);

        if (pid.p > DIFFERENTIAL_CONTROL_LOOP_COEFFICIENT_MAX)
            {
            pid.p = DIFFERENTIAL_CONTROL_LOOP_COEFFICIENT_MAX;
            }

        if (pid.i > DIFFERENTIAL_CONTROL_LOOP_COEFFICIENT_MAX)
            {
            pid.i = DIFFERENTIAL_CONTROL_LOOP_COEFFICIENT_MAX;
            }

        if (pid.d > DIFFERENTIAL_CONTROL_LOOP_COEFFICIENT_MAX)
            {
            pid.d = DIFFERENTIAL_CONTROL_LOOP_COEFFICIENT_MAX;
            }

        write(ADDRESS_MAX_DIFFERENTIAL_CONTROL_LOOP_COEFFICIENT_MAP[motor],
                new byte[]{(byte) pid.p, (byte) pid.i, (byte) pid.d});
        }

    public DifferentialControlLoopCoefficients getDifferentialControlLoopCoefficients(int motor)
        {
        validateMotor(motor);

        DifferentialControlLoopCoefficients pid = new DifferentialControlLoopCoefficients();
        byte[] data = read(ADDRESS_MAX_DIFFERENTIAL_CONTROL_LOOP_COEFFICIENT_MAP[motor], 3);
        pid.p = data[0];
        pid.i = data[1];
        pid.d = data[2];

        return pid;
        }

    //----------------------------------------------------------------------------------------------
    // Utility
    //----------------------------------------------------------------------------------------------

    public static byte modeToByte(DcMotor.RunMode mode)
        {
        switch (mode.migrate())
            {
            case RUN_USING_ENCODER:         return CHANNEL_MODE_FLAG_SELECT_RUN_CONSTANT_SPEED;
            case RUN_WITHOUT_ENCODER:       return CHANNEL_MODE_FLAG_SELECT_RUN_POWER_CONTROL_ONLY;
            case RUN_TO_POSITION:           return CHANNEL_MODE_FLAG_SELECT_RUN_TO_POSITION;
            case STOP_AND_RESET_ENCODER:    return CHANNEL_MODE_FLAG_SELECT_RESET;
            }
        return CHANNEL_MODE_FLAG_SELECT_RUN_CONSTANT_SPEED;
        }

    public static DcMotor.RunMode modeFromByte(byte flag)
        {
        switch (flag & CHANNEL_MODE_MASK_SELECTION)
            {
            case CHANNEL_MODE_FLAG_SELECT_RUN_CONSTANT_SPEED:       return DcMotor.RunMode.RUN_USING_ENCODER;
            case CHANNEL_MODE_FLAG_SELECT_RUN_POWER_CONTROL_ONLY:   return DcMotor.RunMode.RUN_WITHOUT_ENCODER;
            case CHANNEL_MODE_FLAG_SELECT_RUN_TO_POSITION:          return DcMotor.RunMode.RUN_TO_POSITION;
            case CHANNEL_MODE_FLAG_SELECT_RESET:                    return DcMotor.RunMode.STOP_AND_RESET_ENCODER;
            }
        return DcMotor.RunMode.RUN_WITHOUT_ENCODER;
        }

    private void floatHardware()
        {
        for (int motor = MOTOR_FIRST; motor <= MOTOR_LAST; motor++)
            {
            this.setMotorPowerFloat(motor);
            }
        }

    private void runWithoutEncoders()
        {
        for (int motor = MOTOR_FIRST; motor <= MOTOR_LAST; motor++)
            {
            this.setMotorMode(motor, DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            }
        }

    private void setDifferentialControlLoopCoefficientsToDefault()
        {
        for (int motor = MOTOR_FIRST; motor <= MOTOR_LAST; motor++)
            {
            write(ADDRESS_MAX_DIFFERENTIAL_CONTROL_LOOP_COEFFICIENT_MAP[motor],
                    new byte[]{DEFAULT_P_COEFFICIENT, DEFAULT_I_COEFFICIENT, DEFAULT_D_COEFFICIENT});
            }
        }

    private void validateMotor(int motor)
        {
        if (motor < MOTOR_FIRST || motor > MOTOR_LAST)
            {
            throw new IllegalArgumentException(
                    String.format("Motor %d is invalid; valid motors are %d..%d", motor, MOTOR_FIRST, MOTOR_LAST));
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
    }
