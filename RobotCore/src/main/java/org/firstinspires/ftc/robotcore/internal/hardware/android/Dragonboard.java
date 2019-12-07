/*
Copyright (c) 2018 Noah Andrews

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of Noah Andrews nor the names of his contributors may be used to
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

import com.qualcomm.robotcore.hardware.DigitalChannel;

import java.io.File;

public class Dragonboard extends AndroidBoard {
    // Don't allow instantiation outside of our package
    protected Dragonboard() {}

    // GPIO pins
    private static final DigitalChannel ANDROID_BOARD_IS_PRESENT_PIN =
            new GpioPin(921, true, GpioPin.Active.LOW, AndroidBoard.ANDROID_BOARD_IS_PRESENT_PIN_NAME);

    private static final DigitalChannel LYNX_MODULE_RESET_PIN =
            new GpioPin(915, false, GpioPin.Active.LOW, AndroidBoard.LYNX_MODULE_RESET_PIN_NAME);

    private static final DigitalChannel PROGRAMMING_PIN =
            new GpioPin(938, false, GpioPin.Active.LOW, PROGRAMMING_PIN_NAME);

    private static final DigitalChannel USER_BUTTON_PIN = new GpioPin(919, USER_BUTTON_PIN_NAME);

    // UART file

    private static final File UART_FILE = findSerialDevTty();

    // Public Methods

    @Override
    public String getDeviceType() {
        return "Dragonboard 410c";
    }

    @Override
    public DigitalChannel getAndroidBoardIsPresentPin() {
        return ANDROID_BOARD_IS_PRESENT_PIN;
    }

    @Override
    public DigitalChannel getProgrammingPin() {
        return PROGRAMMING_PIN;
    }

    @Override
    public DigitalChannel getLynxModuleResetPin() {
        return LYNX_MODULE_RESET_PIN;
    }

    @Override
    public DigitalChannel getUserButtonPin() {
        return USER_BUTTON_PIN;
    }

    @Override
    public File getUartLocation() {
        return UART_FILE;
    }

    @Override public boolean supports5GhzAp() {
        return false;
    }

    @Override public boolean hasControlHubUpdater() {
        return false;
    }

    // Private methods

    private static File findSerialDevTty()
    {
        // Older versions of Dragonboard software have the serial port named '/dev/ttyHS0', while new
        // versions have the name '/dev/ttyHS4'. Try that guy explicitly, first.
        File result = new File("/dev/ttyHS4");
        if (result.exists())
        {
            return result;
        }

        // If we can't find that guy, that'd be odd, but let's just see who we *can* find
        // and hope for the best.
        for (int i = 0; i <= 255; i++) // per AOSP\kernel\Documentation\devicetree\bindings\tty\serial\msm_serial_hs.txt
        {
            String path = "/dev/ttyHS" + i;
            result = new File(path);
            if (result.exists())
            {
                return result;
            }
        }
        throw new RuntimeException("unable to locate UART communication file for Dragonboard /dev/ttyHSx");
    }
}
