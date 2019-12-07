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
package org.firstinspires.ftc.robotcore.external.navigation;

import java.util.Locale;

/**
 * Instances of {@link Velocity} represent the derivative of {@link Position} over time.
 *
 * @see Position
 * @see Acceleration
 */
public class Velocity
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    /**
     * The distance units in which this velocity is expressed. The time unit is always "per second".
     */
    public DistanceUnit unit;

    public double xVeloc;
    public double yVeloc;
    public double zVeloc;

    /**
     * the time on the System.nanoTime() clock at which the data was acquired. If no
     * timestamp is associated with this particular set of data, this value is zero.
     */
    public long acquisitionTime;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public Velocity()
        {
        this(DistanceUnit.MM, 0, 0, 0, 0);
        }

    public Velocity(DistanceUnit unit, double xVeloc, double yVeloc, double zVeloc, long acquisitionTime)
        {
        this.unit = unit;
        this.xVeloc = xVeloc;
        this.yVeloc = yVeloc;
        this.zVeloc = zVeloc;
        this.acquisitionTime = acquisitionTime;
        }

    public Velocity toUnit(DistanceUnit distanceUnit)
        {
        if (distanceUnit != this.unit)
            {
            return new Velocity(distanceUnit,
                    distanceUnit.fromUnit(this.unit, xVeloc),
                    distanceUnit.fromUnit(this.unit, yVeloc),
                    distanceUnit.fromUnit(this.unit, zVeloc),
                    this.acquisitionTime);
            }
        else
            return this;
        }

    //----------------------------------------------------------------------------------------------
    // Formatting
    //----------------------------------------------------------------------------------------------

    @Override public String toString()
        {
        return String.format(Locale.getDefault(), "(%.3f %.3f %.3f)%s/s", xVeloc, yVeloc, zVeloc, unit.toString());
        }
    }
