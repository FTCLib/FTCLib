/*
 * Copyright (c) 2018 Craig MacFarlane
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
 * Neither the name of Craig MacFarlane nor the names of its contributors may be used to
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
package com.qualcomm.hardware.rev;

import com.qualcomm.hardware.R;
import com.qualcomm.robotcore.hardware.ControlSystem;
import com.qualcomm.robotcore.hardware.HardwareDevice;
import com.qualcomm.robotcore.hardware.ServoControllerEx;
import com.qualcomm.robotcore.hardware.configuration.ServoFlavor;
import com.qualcomm.robotcore.hardware.configuration.annotations.DeviceProperties;
import com.qualcomm.robotcore.hardware.configuration.annotations.ServoType;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

/**
 * Support for the REV Robotics Blinkin LED Driver.
 *
 * For full details see: http://www.revrobotics.com/content/docs/REV-11-1105-UM.pdf
 *
 * To use a Blinkin, connect your Blinkin to a servo port, then on the Robot Controller or
 * Driver Station, configure the port you connected the Blinkin to as a REV Blinkin LED Driver.
 *
 * See the SampleRevBlinkinLedDriver.java sample for an example of how to use this class.
 */
@ServoType(flavor = ServoFlavor.CUSTOM, usPulseLower = 500, usPulseUpper = 2500)
@DeviceProperties(xmlTag = "RevBlinkinLedDriver", name = "@string/rev_blinkin_name", description = "@string/rev_blinkin_description",
        builtIn = true, compatibleControlSystems = ControlSystem.REV_HUB)
public class RevBlinkinLedDriver implements HardwareDevice {

    public enum BlinkinPattern {
        /*
         * Fixed Palette Pattern
         */
        RAINBOW_RAINBOW_PALETTE,
        RAINBOW_PARTY_PALETTE,
        RAINBOW_OCEAN_PALETTE,
        RAINBOW_LAVA_PALETTE,
        RAINBOW_FOREST_PALETTE,
        RAINBOW_WITH_GLITTER,
        CONFETTI,
        SHOT_RED,
        SHOT_BLUE,
        SHOT_WHITE,
        SINELON_RAINBOW_PALETTE,
        SINELON_PARTY_PALETTE,
        SINELON_OCEAN_PALETTE,
        SINELON_LAVA_PALETTE,
        SINELON_FOREST_PALETTE,
        BEATS_PER_MINUTE_RAINBOW_PALETTE,
        BEATS_PER_MINUTE_PARTY_PALETTE,
        BEATS_PER_MINUTE_OCEAN_PALETTE,
        BEATS_PER_MINUTE_LAVA_PALETTE,
        BEATS_PER_MINUTE_FOREST_PALETTE,
        FIRE_MEDIUM,
        FIRE_LARGE,
        TWINKLES_RAINBOW_PALETTE,
        TWINKLES_PARTY_PALETTE,
        TWINKLES_OCEAN_PALETTE,
        TWINKLES_LAVA_PALETTE,
        TWINKLES_FOREST_PALETTE,
        COLOR_WAVES_RAINBOW_PALETTE,
        COLOR_WAVES_PARTY_PALETTE,
        COLOR_WAVES_OCEAN_PALETTE,
        COLOR_WAVES_LAVA_PALETTE,
        COLOR_WAVES_FOREST_PALETTE,
        LARSON_SCANNER_RED,
        LARSON_SCANNER_GRAY,
        LIGHT_CHASE_RED,
        LIGHT_CHASE_BLUE,
        LIGHT_CHASE_GRAY,
        HEARTBEAT_RED,
        HEARTBEAT_BLUE,
        HEARTBEAT_WHITE,
        HEARTBEAT_GRAY,
        BREATH_RED,
        BREATH_BLUE,
        BREATH_GRAY,
        STROBE_RED,
        STROBE_BLUE,
        STROBE_GOLD,
        STROBE_WHITE,
        /*
         * CP1: Color 1 Pattern
         */
        CP1_END_TO_END_BLEND_TO_BLACK,
        CP1_LARSON_SCANNER,
        CP1_LIGHT_CHASE,
        CP1_HEARTBEAT_SLOW,
        CP1_HEARTBEAT_MEDIUM,
        CP1_HEARTBEAT_FAST,
        CP1_BREATH_SLOW,
        CP1_BREATH_FAST,
        CP1_SHOT,
        CP1_STROBE,
        /*
         * CP2: Color 2 Pattern
         */
        CP2_END_TO_END_BLEND_TO_BLACK,
        CP2_LARSON_SCANNER,
        CP2_LIGHT_CHASE,
        CP2_HEARTBEAT_SLOW,
        CP2_HEARTBEAT_MEDIUM,
        CP2_HEARTBEAT_FAST,
        CP2_BREATH_SLOW,
        CP2_BREATH_FAST,
        CP2_SHOT,
        CP2_STROBE,
        /*
         * CP1_2: Color 1 and 2 Pattern
         */
        CP1_2_SPARKLE_1_ON_2,
        CP1_2_SPARKLE_2_ON_1,
        CP1_2_COLOR_GRADIENT,
        CP1_2_BEATS_PER_MINUTE,
        CP1_2_END_TO_END_BLEND_1_TO_2,
        CP1_2_END_TO_END_BLEND,
        CP1_2_NO_BLENDING,
        CP1_2_TWINKLES,
        CP1_2_COLOR_WAVES,
        CP1_2_SINELON,
        /*
         * Solid color
         */
        HOT_PINK,
        DARK_RED,
        RED,
        RED_ORANGE,
        ORANGE,
        GOLD,
        YELLOW,
        LAWN_GREEN,
        LIME,
        DARK_GREEN,
        GREEN,
        BLUE_GREEN,
        AQUA,
        SKY_BLUE,
        DARK_BLUE,
        BLUE,
        BLUE_VIOLET,
        VIOLET,
        WHITE,
        GRAY,
        DARK_GRAY,
        BLACK;

        private static BlinkinPattern[] elements = values();

        public static BlinkinPattern fromNumber(int number)
        {
            return elements[number % elements.length];
        }

        public BlinkinPattern next()
        {
            return elements[(this.ordinal() + 1) % elements.length];
        }

        public BlinkinPattern previous()
        {
            return elements[(this.ordinal() - 1) < 0 ? elements.length - 1 : this.ordinal() - 1];
        }
    };

    protected final static String TAG = "RevBlinkinLedDriver";

    /*
     * Values are for REV Expansion Hub.  Note that the expansion hub will output a
     * range of 500 to 2500 mS.  The first pattern expects 1005mS, ergo the position
     * for the first pattern is 0.2525.
     */
    protected final static double PULSE_WIDTH_INCREMENTOR = 0.0005;
    protected final static double BASE_SERVO_POSITION = 505 * PULSE_WIDTH_INCREMENTOR;
    protected final static int PATTERN_OFFSET = 10;

    protected ServoControllerEx controller;
    private final int port;

    /**
     * RevBlinkinLedDriver
     *
     * @param controller A REV servo controller
     * @param port the port that the driver is connected to
     */
    public RevBlinkinLedDriver(ServoControllerEx controller, int port)
    {
        this.controller = controller;
        this.port = port;
    }

    /**
     * setPattern
     *
     * @param pattern the BlinkinPattern to display
     */
    public void setPattern(BlinkinPattern pattern)
    {
        double pwm = BASE_SERVO_POSITION + ((PATTERN_OFFSET * pattern.ordinal()) * PULSE_WIDTH_INCREMENTOR);
        RobotLog.vv(TAG, "Pattern: %s, %d, %f", pattern.toString(), pattern.ordinal(), pwm);
        controller.setServoPosition(port, pwm);
    }

    // HardwareDevice stuff
    @Override
    public Manufacturer getManufacturer()
    {
        return Manufacturer.Lynx;
    }

    @Override
    public String getDeviceName()
    {
        return AppUtil.getDefContext().getString(R.string.rev_blinkin_name);
    }

    @Override
    public String getConnectionInfo()
    {
        return controller.getConnectionInfo() + "; port " + port;
    }

    @Override
    public int getVersion()
    {
        return 1;
    }

    @Override
    public void resetDeviceConfigurationForOpMode() { }

    @Override
    public void close() { }
}
