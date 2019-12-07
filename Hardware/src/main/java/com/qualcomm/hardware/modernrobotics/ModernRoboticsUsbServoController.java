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
import com.qualcomm.hardware.modernrobotics.comm.ReadWriteRunnableStandard;
import com.qualcomm.robotcore.eventloop.SyncdDevice;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.ServoController;
import com.qualcomm.robotcore.hardware.configuration.ModernRoboticsConstants;
import com.qualcomm.robotcore.hardware.usb.RobotUsbDevice;
import com.qualcomm.robotcore.util.LastKnown;
import com.qualcomm.robotcore.util.Range;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.SerialNumber;

/**
 * Modern Robotics USB Servo Controller
 * <p>
 * This is an implementation of {@link ServoController}.
 * <p>
 * Use {@link HardwareDeviceManager} to create an instance of this class
 */
public final class ModernRoboticsUsbServoController extends ModernRoboticsUsbController implements ServoController
    {
    //----------------------------------------------------------------------------------------------
    // Constants
    //----------------------------------------------------------------------------------------------

    public final static String TAG = "MRServoController";
    @Override protected String getTag() { return TAG; }

    /**
     * Enable DEBUG_LOGGING logging
     */
    public static final boolean DEBUG_LOGGING = false;

    /*
     * const values used by this class
     */
    public static final int MONITOR_LENGTH = 0x09;
    protected static final int SERVO_FIRST = ModernRoboticsConstants.INITIAL_SERVO_PORT;
    protected static final int SERVO_LAST = ModernRoboticsConstants.INITIAL_SERVO_PORT + ModernRoboticsConstants.NUMBER_OF_SERVOS -1;

    /*
     * const values used by controller
     */
    public static final byte PWM_ENABLE = (byte) 0x00;
    public static final byte PWM_ENABLE_WITHOUT_TIMEOUT = (byte) 0xaa;
    public static final byte PWM_DISABLE = (byte) 0xff;

    public static final byte START_ADDRESS = 0x40;

    /*
     * memory addresses used by controller
     */
    public static final int ADDRESS_CHANNEL1 = 0x42;
    public static final int ADDRESS_CHANNEL2 = 0x43;
    public static final int ADDRESS_CHANNEL3 = 0x44;
    public static final int ADDRESS_CHANNEL4 = 0x45;
    public static final int ADDRESS_CHANNEL5 = 0x46;
    public static final int ADDRESS_CHANNEL6 = 0x47;
    public static final int ADDRESS_PWM      = 0x48;

    public static final int ADDRESS_UNUSED = -1;

    /*
     * map of servo channels to memory addresses
     */
    public static final byte[] ADDRESS_CHANNEL_MAP = {
            ADDRESS_UNUSED,
            ADDRESS_CHANNEL1,
            ADDRESS_CHANNEL2,
            ADDRESS_CHANNEL3,
            ADDRESS_CHANNEL4,
            ADDRESS_CHANNEL5,
            ADDRESS_CHANNEL6
    };

    protected static final double apiPositionMin   = Servo.MIN_POSITION;
    protected static final double apiPositionMax   = Servo.MAX_POSITION;
    protected static final double servoPositionMin =   0.0;
    protected static final double servoPositionMax = 255.0;

    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected LastKnown<Double>[] commandedServoPositions;
    protected LastKnown<Boolean>  lastKnownPwmEnabled;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    /*
     * Use HardwareDeviceManager to create an instance of this class
     *
     * @see HardwareDeviceManager
     * @param serialNumber USB serial number
     * @param device FTDI device
     * @throws InterruptedException
     */
    public ModernRoboticsUsbServoController(final Context context, final SerialNumber serialNumber, OpenRobotUsbDevice openRobotUsbDevice, SyncdDevice.Manager manager)
            throws RobotCoreException, InterruptedException
        {
        super(context, serialNumber, manager, openRobotUsbDevice, new CreateReadWriteRunnable() 
            {
            @Override public ReadWriteRunnable create(RobotUsbDevice device)
                {
                return new ReadWriteRunnableStandard(context, serialNumber, device, MONITOR_LENGTH, START_ADDRESS, DEBUG_LOGGING);
                }
            });

        this.commandedServoPositions = LastKnown.createArray(ADDRESS_CHANNEL_MAP.length);
        this.lastKnownPwmEnabled = new LastKnown<Boolean>();
        }

    @Override
    public void initializeHardware()
        {
        // start off with PWM off for safety reasons
        floatHardware();
        }

    //----------------------------------------------------------------------------------------------
    // Arming and disarming
    //----------------------------------------------------------------------------------------------
    
    @Override protected void doArm() throws RobotCoreException, InterruptedException
        {
        doArmOrPretend(true);
        }
    @Override protected void doPretend() throws RobotCoreException, InterruptedException
        {
        doArmOrPretend(false);
        }

    private void doArmOrPretend(boolean isArm) throws RobotCoreException, InterruptedException
        {
        RobotLog.d("arming modern servo controller %s%s...", HardwareFactory.getDeviceDisplayName(context, this.serialNumber), (isArm ? "" : " (pretend)"));

        if (isArm)
            this.armDevice();
        else
            this.pretendDevice();

        RobotLog.d("...arming modern servo controller %s complete", HardwareFactory.getDeviceDisplayName(context, this.serialNumber));
        }

    @Override protected void doDisarm() throws RobotCoreException, InterruptedException
        {
        RobotLog.d("disarming modern servo controller \"%s\"...", HardwareFactory.getDeviceDisplayName(context, this.serialNumber));

        this.disarmDevice();

        RobotLog.d("...disarming modern servo controller %s complete", HardwareFactory.getDeviceDisplayName(context, this.serialNumber));
        }

    @Override protected void doCloseFromArmed()
        {
        floatHardware();
        doCloseFromOther();
        }

    @Override protected void doCloseFromOther()
        {
        try
            {
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
    // HardwareDeviceInterface
    //----------------------------------------------------------------------------------------------

    @Override public Manufacturer getManufacturer()
        {
        return Manufacturer.ModernRobotics;
        }

    @Override
    public String getDeviceName()
        {
        return String.format("%s %s", context.getString(R.string.moduleDisplayNameServoController), this.robotUsbDevice.getFirmwareVersion());
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
        }

    //----------------------------------------------------------------------------------------------
    // ServoController interface
    //----------------------------------------------------------------------------------------------

    @Override
    synchronized public void pwmEnable()
        {
        if (lastKnownPwmEnabled.updateValue(true))
            {
            write8(ADDRESS_PWM, PWM_ENABLE);
            }
        }

    @Override
    synchronized public void pwmDisable()
        {
        if (lastKnownPwmEnabled.updateValue(false))
            {
            write8(ADDRESS_PWM, PWM_DISABLE);

            // Make any subsequent positioning actually actuate
            for (LastKnown<Double> commandedPosition : commandedServoPositions)
                {
                commandedPosition.invalidate();
                }
            }
        }

    @Override
    synchronized public PwmStatus getPwmStatus()
        {
        byte[] resp = read(ADDRESS_PWM, 1);
        if (resp[0] == PWM_DISABLE)
            {
            return PwmStatus.DISABLED;
            }
        else
            {
            return PwmStatus.ENABLED;
            }
        }

    @Override
    synchronized public void setServoPosition(int servo, double position)
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
    synchronized public double getServoPosition(int servo)
    // One would think we could just read the servo position registers. But they always report as zero
        {
        validateServo(servo);
        Double commanded = this.commandedServoPositions[servo].getRawValue();
        return commanded==null ? Double.NaN : commanded;
        }

    //----------------------------------------------------------------------------------------------
    // Utility
    //----------------------------------------------------------------------------------------------

    protected void floatHardware()
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
