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
package org.firstinspires.ftc.robotcore.internal.hardware.android;

import com.qualcomm.robotcore.hardware.SwitchableLight;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * {@link DragonboardIndicatorLED} provides a means to access the LEDs on the Qualcomm
 * Dragonboard 410c. The logic we have here *might* be generalizable to other systems,
 * as it uses simple Linux APIs at its core, but we have not attempted to do so as yet.
 *
 * The Dragonboard has four user LEDs. On the silkscreen, these are labeled 1, 2, 3, and 4.
 * Internally, they are known as led1, led2, led3, and boot. The last of these is used as
 * a boot indicator: it turns on (green) early in the boot process and turns off once the
 * boot is complete. See (root)\kernel\arch\arm\boot\dts\qcom\apq8016-sbc.dtsi where these
 * are defined.
 *
 * Each LED appears as a directory in the Android device tree:
 *
 * root@msm8916_64:/sys/class/leds # ls
 *    boot
 *    bt
 *    lcd-backlight
 *    led1
 *    led2
 *    led3
 *    wlan
 *
 * root@msm8916_64:/sys/class/leds # cd led1
 *
 * root@msm8916_64:/sys/class/leds/led1 # ls
 *    brightness
 *    device
 *    max_brightness
 *    power
 *    subsystem
 *    trigger
 *    uevent
 *
 * Brightness, for example, is controlled by writing a '1' or '0' to the 'brightness' file. Other
 * Other aspects of the LEDs can be controlled similarly, but we haven't explored same in detail.

 * The stock Dragonboard Android had perms on the LEDs of 0755. In our FTCAndroid build, we've
 * changed that to uniformly be 0777. 'brightness', 'max_brightness', 'trigger', and 'uevent'
 * were 644; they're now 666. 'power' was 755, which we haven't bothered yet to change.
 * See ...\qcom_common\rootdir\etc\init.qcom.post_boot.sh
 */
@SuppressWarnings("WeakerAccess")
public class DragonboardIndicatorLED implements SwitchableLight
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public final static String TAG = "DBLED";
    public final static int LED_FIRST = 1;
    public final static int LED_LAST  = 4;

    protected static String[] ledNames = { "dummy", "led1", "led2", "led3", "boot"}; // 1-indexed
    protected static DragonboardIndicatorLED[] instances = new DragonboardIndicatorLED[LED_LAST+1]; // 1-indexed
    static
        {
        for (int i = LED_FIRST; i <= LED_LAST; i++)
            {
            instances[i] = new DragonboardIndicatorLED(i);
            }
        }

    protected String ledName;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public static DragonboardIndicatorLED forIndex(int index)
        {
        if (index < LED_FIRST || index > LED_LAST) throw new IllegalArgumentException("illegal LED index: " + index);
        return instances[index];
        }

    protected DragonboardIndicatorLED(int index)
        {
        this.ledName = ledNames[index];
        }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    @Override public synchronized boolean isLightOn()
        {
        try {
            String brightness = readAspect("brightness");
            return Integer.parseInt(brightness) != 0;
            }
        catch (IOException ignored)
            {
            return false;
            }
        }

    @Override public synchronized void enableLight(boolean enabled)
        {
        try {
            // To avoid contention, eliminate any *automatic* enablement of this LED
            writeAspect("trigger", "none");
            // Actually change the level
            writeAspect("brightness", enabled ? "1" : "0");
            }
        catch (IOException ignored)
            {
            }
        }

    //----------------------------------------------------------------------------------------------
    // Utility
    //----------------------------------------------------------------------------------------------

    protected String readAspect(String aspect) throws IOException
        {
        File aspectFile = new File(getLEDPath(), aspect);
        try (BufferedReader reader = new BufferedReader(new FileReader(aspectFile)))
            {
            return reader.readLine();
            }
        }

    protected void writeAspect(String aspect, String value) throws IOException
        {
        File aspectFile = new File(getLEDPath(), aspect);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(aspectFile)))
            {
            writer.write(value);
            }
        }

    protected File getLEDsPath()
        {
        return new File("/sys/class/leds");
        }

    protected File getLEDPath()
        {
        return new File(getLEDsPath(), ledName);
        }

    }
