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

/**
 * ModernRoboticsConstants documents the various number of attachments that can be made
 * to modern robotics controllers
 */
@SuppressWarnings("WeakerAccess")
public class ModernRoboticsConstants
    {
    public final static int USB_BAUD_RATE = 250000;
    public final static int LATENCY_TIMER = 1;      // historically, was 2ms, but there's no reason not to cut that down (is there?)

    public final static int INITIAL_MOTOR_PORT = 1; // note: not yet used in all places it should be
    public final static int INITIAL_SERVO_PORT = 1; // note: not yet used in all places it should be
    public final static int NUMBER_OF_MOTORS = 2;
    public final static int NUMBER_OF_SERVOS = 6;
    public final static int NUMBER_OF_I2C_CHANNELS = 6;
    public final static int NUMBER_OF_LEGACY_MODULE_PORTS = 6;
    public final static int NUMBER_OF_PWM_CHANNELS = 2;
    public final static int NUMBER_OF_ANALOG_INPUTS = 8;
    public final static int NUMBER_OF_DIGITAL_IOS = 8;
    public final static int NUMBER_OF_ANALOG_OUTPUTS = 2;

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
        if (channelZ < 0 || channelZ >= NUMBER_OF_SERVOS)
            throw new IllegalArgumentException(String.format("invalid servo channel: %d", channelZ));
        }
    public static void validateI2cChannelZ(int channelZ)
        {
        if (channelZ < 0 || channelZ >= NUMBER_OF_I2C_CHANNELS)
            throw new IllegalArgumentException(String.format("invalid i2c channel: %d", channelZ));
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