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
package com.qualcomm.robotcore.hardware;

import androidx.annotation.NonNull;

import com.qualcomm.robotcore.hardware.configuration.typecontainers.ServoConfigurationType;

/**
 * ServoControllerEx is an optional servo controller interface supported by some hardware
 * that provides enhanced servo functionality.
 * @see PwmControl
 */
public interface ServoControllerEx extends ServoController
    {
    /**
     * Sets the PWM range of the indicated servo.
     * @param servo the servo port number on the controller
     * @param range the new range for the servo
     * @see #getServoPwmRange(int)
     * @see PwmControl#setPwmRange(PwmControl.PwmRange)
     */
    void setServoPwmRange(int servo, @NonNull PwmControl.PwmRange range);

    /**
     * Returns the PWM range of the indicated servo on this controller.
     * @param servo the servo port number on the controller
     * @return the PWM range of the indicated servo on this controller.
     * @see #setServoPwmRange(int, PwmControl.PwmRange)
     * @see PwmControl#getPwmRange()
     */
    @NonNull PwmControl.PwmRange getServoPwmRange(int servo);

    /**
     * Individually energizes the PWM for a particular servo
     * @param servo the servo port number on the controller
     * @see PwmControl#setPwmEnable()
     */
    void setServoPwmEnable(int servo);

    /**
     * Individually de-energizes the PWM for a particular servo
     * @param servo the servo port number on the controller
     * @see PwmControl#setPwmDisable()
     */
    void setServoPwmDisable(int servo);

    /**
     * Returns whether the PWM is energized for this particular servo
     * @param servo the servo port number on the controller
     * @see PwmControl#isPwmEnabled()
     */
    boolean isServoPwmEnabled(int servo);

    /**
     * Sets the servo type for a particular servo
     * @param servo the servo port number on the controller
     * @param servoType the ServoConfigurationType instance to set
     */
    void setServoType(int servo, ServoConfigurationType servoType);
    }
