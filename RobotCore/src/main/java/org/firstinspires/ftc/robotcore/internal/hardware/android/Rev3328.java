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

import org.firstinspires.ftc.robotcore.internal.system.SystemProperties;

import java.io.File;

public class Rev3328 extends AndroidBoard {
    // Don't allow instantiation outside of our package
    protected Rev3328() {}

    /**
     * To convert 96boards pin numbers to raw GPIO numbers for the REV3328 board
     *
     *  1. Navigate to the final page of the hardware schematic
     *  2. Find the desired pin on CON8600
     *  3. See what GPIO address it maps to (e.g. GPIO1_C2)
     *  4. Use this formula: X1*32 + X2 + A*8 where:
     *          X1 is the first number (1),
     *          X2 is the second number (2),
     *          A is the zero-indexed numerical representation of the letter (C becomes 2)
     *     So, the raw GPIO number for GPIO1_C2 is 50.
     */

    // GPIO pins
    private static final DigitalChannel ANDROID_BOARD_IS_PRESENT_PIN =
            new GpioPin(50, true, GpioPin.Active.LOW, ANDROID_BOARD_IS_PRESENT_PIN_NAME);

    private static final DigitalChannel USER_BUTTON_PIN = new GpioPin(51, USER_BUTTON_PIN_NAME);

    private static final DigitalChannel PROGRAMMING_PIN =
            new GpioPin(66, false, GpioPin.Active.LOW, PROGRAMMING_PIN_NAME);

    private static final DigitalChannel LYNX_MODULE_RESET_PIN =
            new GpioPin(87, false, GpioPin.Active.LOW, LYNX_MODULE_RESET_PIN_NAME);

    // UART file
    private static final File UART_FILE = new File("/dev/ttyS1");

    // Public Methods

    @Override
    public String getDeviceType() {
        return "REV3328";
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
        return true;
    }

    @Override public boolean hasControlHubUpdater() {
        return true;
    }
}
