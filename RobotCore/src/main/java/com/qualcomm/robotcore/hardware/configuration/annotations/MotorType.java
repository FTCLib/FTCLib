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
package com.qualcomm.robotcore.hardware.configuration.annotations;

import org.firstinspires.ftc.robotcore.external.navigation.Rotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@link MotorType} is an annotation with which a class or interface can be decorated in
 * order to define a new kind of motor that can be configured in the robot configuration user
 * interface.
 * <p>
 * Must be accompanied by {@link DeviceProperties} annotation
 */
@SuppressWarnings("WeakerAccess")
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MotorType {
    /**
     * Returns the number of encoder ticks per revolution of the output shaft of
     * the gearmotor. Surprisingly, this can be a non-integer value; this can easily arise
     * for motors which have encoders mounted <em>before</em> the gearbox rather than after.
     *
     * @return the number of encoder ticks per revolution of the output shaft of the gearmotor
     */
    double ticksPerRev();

    /**
     * Returns the number of revolutions of the actual motor for each revolution of the output shaft
     *
     * @return the number of revolutions of the actual motor for each revolution of the output shaft
     */
    double gearing();

    /**
     * Returns the rated maximum no-load RPM of the motor output shaft
     *
     * @return the rated maximum no-load RPM of the motor output shaft
     */
    double maxRPM();

    /**
     * Returns the fraction of maxRPM which can be achieved in closed loop control
     *
     * @return the fraction of maxRPM which can be achieved in closed loop control
     */
    double achieveableMaxRPMFraction() default 0.85;

    /**
     * Indicates the direction of rotation in which encoder counts increase when looking down
     * the motor shaft towards the motor.
     */
    Rotation orientation() default Rotation.CW;
}
