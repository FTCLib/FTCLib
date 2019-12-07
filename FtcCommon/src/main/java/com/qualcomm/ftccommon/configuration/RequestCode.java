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
package com.qualcomm.ftccommon.configuration;

/**
 * RequestCode centralizes the integer tokens used for connecting child configuration editors
 * with their parents. The symbolic names of these constants can also appear portions of strings
 * in string or string-array resources in order to be able to connect these integers with
 * display strings suitable for display to humans.
 */
public enum RequestCode
    {
    NOTHING(0),
        EDIT_MOTOR_CONTROLLER(1), EDIT_SERVO_CONTROLLER(2), EDIT_LEGACY_MODULE(3),
        EDIT_DEVICE_INTERFACE_MODULE(4), EDIT_MATRIX_CONTROLLER(5),
        EDIT_PWM_PORT(6), EDIT_I2C_PORT(7), EDIT_ANALOG_INPUT(8),
        EDIT_DIGITAL(9), EDIT_ANALOG_OUTPUT(10), EDIT_LYNX_MODULE(11), EDIT_LYNX_USB_DEVICE(12),
        EDIT_I2C_BUS0(13), EDIT_I2C_BUS1(14), EDIT_I2C_BUS2(15), EDIT_I2C_BUS3(16),
        EDIT_MOTOR_LIST(17), EDIT_SERVO_LIST(18), EDIT_SWAP_USB_DEVICES(19),
        EDIT_FILE(20), NEW_FILE(21), AUTO_CONFIGURE(22), CONFIG_FROM_TEMPLATE(23),
        EDIT_USB_CAMERA(24);

    public final int value;

    RequestCode(int value)
        {
        this.value = value;
        }

    public static RequestCode fromString(String string)
        {
        for (RequestCode requestCode : RequestCode.values())
            {
            if (requestCode.toString().equals(string))
                {
                return requestCode;
                }
            }
        return NOTHING;
        }

    public static RequestCode fromValue(int value)
        {
        for (RequestCode requestCode : RequestCode.values())
            {
            if (requestCode.value == value)
                {
                return requestCode;
                }
            }
        return NOTHING;
        }
    }
