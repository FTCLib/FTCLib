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

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AngularVelocity;
import org.firstinspires.ftc.robotcore.external.navigation.Axis;

import java.util.Set;

/**
 * The {@link Gyroscope} interface exposes core, fundamental functionality that
 * is applicable to <em>all</em> gyroscopes: that of reporting angular rotation rate.
 */
public interface Gyroscope
    {
    /**
     * Returns the axes on which the gyroscope measures angular velocity. Some gyroscopes
     * measure angular velocity on all three axes (X, Y, & Z) while others measure on only
     * a subset, typically the Z axis. This method allows you to determine what information
     * is usefully returned through {@link #getAngularVelocity(AngleUnit)}.
     * @return the axes on which the gyroscope measures angular velocity.
     * @see #getAngularVelocity(AngleUnit)
     */
    Set<Axis> getAngularVelocityAxes();

    /**
     * Returns the angular rotation rate across all the axes measured by the gyro. Axes
     * on which angular velocity is not measured are reported as zero.
     * @param unit the unit in which the rotation rates are to be returned (the time
     *             dimension is always inverse-seconds).
     * @return the angular rotation rate across all the supported axes
     * @see #getAngularVelocityAxes()
     */
    AngularVelocity getAngularVelocity(AngleUnit unit);
    }
