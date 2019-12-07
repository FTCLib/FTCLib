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
package com.qualcomm.hardware.bosch;

import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.external.navigation.Acceleration;
import org.firstinspires.ftc.robotcore.external.navigation.Position;
import org.firstinspires.ftc.robotcore.external.navigation.Velocity;

/**
 * {@link JustLoggingAccelerationIntegrator} is an integrator that doesn't actually
 * integrate accelerations, but merely reports them in the logcat log. This is a debugging
 * and demonstration tool, little more.
 */
public class JustLoggingAccelerationIntegrator implements BNO055IMU.AccelerationIntegrator
    {
    BNO055IMU.Parameters parameters;
    Acceleration acceleration;

    @Override public void initialize(BNO055IMU.Parameters parameters, Position initialPosition, Velocity initialVelocity)
        {
        this.parameters = parameters;
        }

    @Override public Position getPosition() { return new Position(); }
    @Override public Velocity getVelocity() { return new Velocity(); }
    @Override public Acceleration getAcceleration()
        {
        return this.acceleration == null ? new Acceleration() : this.acceleration;
        }

    @Override public void update(Acceleration linearAcceleration)
        {
        // We should always be given a timestamp here
        if (linearAcceleration.acquisitionTime != 0)
            {
            if (acceleration != null)
                {
                Acceleration accelPrev = acceleration;
                acceleration = linearAcceleration;
                if (parameters.loggingEnabled)
                    {
                    RobotLog.vv(parameters.loggingTag, "dt=%.3fs accel=%s", (acceleration.acquisitionTime - accelPrev.acquisitionTime)*1e-9, acceleration);
                    }
                }
            else
                acceleration = linearAcceleration;
            }
        }
    }
