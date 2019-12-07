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
package com.qualcomm.robotcore.hardware.configuration;

import android.os.Build;
import android.support.annotation.Nullable;

import com.qualcomm.robotcore.util.SerialNumber;

import org.firstinspires.ftc.robotcore.internal.system.SystemProperties;

/**
 * LynxConstants contains information regarding the configured size of various aspects of
 * a Lynx module.
 */
@SuppressWarnings("WeakerAccess")
public class LynxConstants
    {
    public static final String TAG = "LynxConstants";

    public static final int DRAGONBOARD_CH_VERSION = 0;
    private static final String DRAGONBOARD_MODEL = "FIRST Control Hub";
    private static final String EMPTY_STRING = "";

    /** Are we running on an Android / Lynx combo device */
    public static boolean isRevControlHub()
        {
        return SystemProperties.getBoolean("persist.ftcandroid.serialasusb", false);
        }

    /** Returns the version of the Control Hub for the purpose of identifying software compatibility.
     *
     *  Increasing the CH version number in the OS means that new versions of the SDK and AP service
     *  will need to be released. Specifically, AndroidBoard.getInstance must be modified to support
     *  the new version number, and the value of the `org.firstinspires.latestSupportedControlHubVersion`
     *  meta-data tag in the manifests must be increased. */
    public static int getControlHubVersion()
        {
        int version = SystemProperties.getInt("ro.ftcandroid.controlhubversion", -1);
        if (version == -1)
            {
            if (Build.MODEL.equalsIgnoreCase(DRAGONBOARD_MODEL))
                {
                version = DRAGONBOARD_CH_VERSION; // The Dragonboard doesn't have ro.ftcandroid.controlhubversion set.
                }
            }
        return version;
        }

    /** Get the Control Hub OS version. Returns null if OS version property is not set.
     *
     * Value is human-readable, do not attempt to parse.
     * Use ro.controlhub.os.versionnum (added in OS version 1.0.1) to differentiate between OS
     * versions programmatically. */
    @Nullable public static String getControlHubOsVersion()
        {
        String chOsVersion = SystemProperties.get("ro.controlhub.os.version", EMPTY_STRING);
        if(EMPTY_STRING.equals(chOsVersion)) chOsVersion = null;
        return chOsVersion;
        }

    /** When running on a Dragonboard / Lynx combo device, should the Dragonboard pretend
     * that it's not there and thus allow the Lynx to be used as an stand-alone extension? */
    public static boolean shouldDisableAndroidBoard()
        {
        return SystemProperties.getBoolean("persist.ftcandroid.db.disable", false);
        }

    public static boolean isEmbeddedSerialNumber(SerialNumber serialNumber)
        {
        return SERIAL_NUMBER_EMBEDDED.equals(serialNumber);
        }

    public static boolean autorunRobotController()
        {
        return SystemProperties.getBoolean("persist.ftcandroid.autorunrc", false);
        }

    /** Whether to use the tiny little indicator LEDs on the Dragonboard itself for signalling */
    public static boolean useIndicatorLEDS()
        {
        return SystemProperties.getBoolean("persist.ftcandroid.rcuseleds", false);
        }
    public final static int INDICATOR_LED_ROBOT_CONTROLLER_ALIVE = 1;
    public final static int INDICATOR_LED_INVITE_DIALOG_ACTIVE = 2;
    public final static int INDICATOR_LED_BOOT = 4;

    public static final int SERIAL_MODULE_BAUD_RATE = 460800;
    public static final SerialNumber SERIAL_NUMBER_EMBEDDED = SerialNumber.createEmbedded();

    public final static int USB_BAUD_RATE = 460800;
    public final static int LATENCY_TIMER = 1;

    public final static int INITIAL_MOTOR_PORT = 0;
    public final static int INITIAL_SERVO_PORT = 0;

    public final static int NUMBER_OF_MOTORS = 4;
    public final static int NUMBER_OF_SERVO_CHANNELS = 6;
    public final static int NUMBER_OF_PWM_CHANNELS = 4;
    public final static int NUMBER_OF_ANALOG_INPUTS = 4;
    public final static int NUMBER_OF_DIGITAL_IOS = 8;
    public final static int NUMBER_OF_I2C_BUSSES = 4;
    public final static int EMBEDDED_IMU_BUS = 0;

    public final static int DEFAULT_TARGET_POSITION_TOLERANCE = 5;

    public final static int MAX_NUMBER_OF_MODULES = 254;
    public final static int MAX_MODULES_DISCOVER = MAX_NUMBER_OF_MODULES;   // see LynxDiscoveryCommand: parent uses this count, so we must mirror
    public final static int MAX_MODULE_ADDRESS_CHOICE = 10;
    public final static int DEFAULT_PARENT_MODULE_ADDRESS = 1;              // try to avoid using: query the device instead

    public static final String EMBEDDED_IMU_XML_TAG = "LynxEmbeddedIMU";

    public static void validateMotorZ(int motorZ)
        {
        if (motorZ < 0 || motorZ >= NUMBER_OF_MOTORS)
            throw new IllegalArgumentException(String.format("invalid motor: %d", motorZ));
        }
    public static void validatePwmChannelZ(int channelZ)
        {
        if (channelZ < 0 || channelZ >= NUMBER_OF_PWM_CHANNELS)
            throw new IllegalArgumentException(String.format("invalid pwm channel: %d", channelZ));
        }
    public static void validateServoChannelZ(int channelZ)
        {
        if (channelZ < 0 || channelZ >= NUMBER_OF_SERVO_CHANNELS)
            throw new IllegalArgumentException(String.format("invalid servo channel: %d", channelZ));
        }
    public static void validateI2cBusZ(int busZ)
        {
        if (busZ < 0 || busZ >= NUMBER_OF_I2C_BUSSES)
            throw new IllegalArgumentException(String.format("invalid i2c bus: %d", busZ));
        }
    public static void validateAnalogInputZ(int analogInputZ)
        {
        if (analogInputZ < 0 || analogInputZ >= NUMBER_OF_ANALOG_INPUTS)
            throw new IllegalArgumentException(String.format("invalid analog input: %d", analogInputZ));
        }
    public static void validateDigitalIOZ(int digitalIOZ)
        {
        if (digitalIOZ < 0 || digitalIOZ >= NUMBER_OF_DIGITAL_IOS)
            throw new IllegalArgumentException(String.format("invalid digital pin: %d", digitalIOZ));
        }

    }
