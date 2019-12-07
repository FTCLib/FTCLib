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
 * Instances of {@link Acceleration} represent the second derivative of {@link Position} over time. This
 * is also to say that {@code Position} is a double integration of {@code Acceleration} with respect
 * to time.
 *
 * @see Velocity
 * @see Position
 */
public class Acceleration
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    /** The (nominal) acceleration due to Earth's gravity
     * The units are in m/s^2
     */
    public static final double earthGravity = 9.80665;

    /**
     * The distance units in which this acceleration is expressed. The time unit is always "per second per second".
     */
    public DistanceUnit unit;

    public double xAccel;
    public double yAccel;
    public double zAccel;

    /**
     * the time on the System.nanoTime() clock at which the data was acquired. If no
     * timestamp is associated with this particular set of data, this value is zero.
     */
    public long acquisitionTime;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public Acceleration()
        {
        this(DistanceUnit.MM, 0, 0, 0, 0);
        }

    public Acceleration(DistanceUnit unit, double xAccel, double yAccel, double zAccel, long acquisitionTime)
        {
        this.unit = unit;
        this.xAccel = xAccel;
        this.yAccel = yAccel;
        this.zAccel = zAccel;
        this.acquisitionTime = acquisitionTime;
        }

    /**
     * Returns an acceleration constructed from measures in units of earth's gravity
     * rather than explicit distance units.
     */
    public static Acceleration fromGravity(double gx, double gy, double gz, long acquisitionTime)
        {
        return new Acceleration(DistanceUnit.METER, gx * earthGravity, gy * earthGravity, gz * earthGravity, acquisitionTime);
        }

    public Acceleration toUnit(DistanceUnit distanceUnit)
        {
        if (distanceUnit != this.unit)
            {
            return new Acceleration(distanceUnit,
                    distanceUnit.fromUnit(this.unit, xAccel),
                    distanceUnit.fromUnit(this.unit, yAccel),
                    distanceUnit.fromUnit(this.unit, zAccel),
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
        return String.format(Locale.getDefault(), "(%.3f %.3f %.3f)%s/s^2", xAccel, yAccel, zAccel, unit.toString());
        }
    }
