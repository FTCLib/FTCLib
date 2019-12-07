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

/**
 * {@link NavUtil} is a collection of utilities that provide useful manipulations of
 * objects related to navigation.
 */
public class NavUtil
    {
    //----------------------------------------------------------------------------------------------
    // Arithmetic: some handy helpers
    //----------------------------------------------------------------------------------------------

    public static Position plus(Position a, Position b)
        {
        return new Position(a.unit,
                a.x      + a.unit.fromUnit(b.unit, b.x),
                a.y      + a.unit.fromUnit(b.unit, b.y),
                a.z      + a.unit.fromUnit(b.unit, b.z),
                Math.max(a.acquisitionTime, b.acquisitionTime));
        }
    public static Velocity plus(Velocity a, Velocity b)
        {
        return new Velocity(a.unit,
                a.xVeloc + a.unit.fromUnit(b.unit, b.xVeloc),
                a.yVeloc + a.unit.fromUnit(b.unit, b.yVeloc),
                a.zVeloc + a.unit.fromUnit(b.unit, b.zVeloc),
                Math.max(a.acquisitionTime, b.acquisitionTime));
        }
    public static Acceleration plus(Acceleration a, Acceleration b)
        {
        return new Acceleration(a.unit,
                a.xAccel + a.unit.fromUnit(b.unit, b.xAccel),
                a.yAccel + a.unit.fromUnit(b.unit, b.yAccel),
                a.zAccel + a.unit.fromUnit(b.unit, b.zAccel),
                Math.max(a.acquisitionTime, b.acquisitionTime));
        }

    public static Position minus(Position a, Position b)
        {
        return new Position(a.unit,
                a.x      - a.unit.fromUnit(b.unit, b.x),
                a.y      - a.unit.fromUnit(b.unit, b.y),
                a.z      - a.unit.fromUnit(b.unit, b.z),
                Math.max(a.acquisitionTime, b.acquisitionTime));
        }
    public static Velocity minus(Velocity a, Velocity b)
        {
        return new Velocity(a.unit,
                a.xVeloc - a.unit.fromUnit(b.unit, b.xVeloc),
                a.yVeloc - a.unit.fromUnit(b.unit, b.yVeloc),
                a.zVeloc - a.unit.fromUnit(b.unit, b.zVeloc),
                Math.max(a.acquisitionTime, b.acquisitionTime));
        }
    public static Acceleration minus(Acceleration a, Acceleration b)
        {
        return new Acceleration(a.unit,
                a.xAccel - a.unit.fromUnit(b.unit, b.xAccel),
                a.yAccel - a.unit.fromUnit(b.unit, b.yAccel),
                a.zAccel - a.unit.fromUnit(b.unit, b.zAccel),
                Math.max(a.acquisitionTime, b.acquisitionTime));
        }

    public static Position scale(Position p, double scale)
        {
        return new Position(p.unit,
                p.x * scale,
                p.y * scale,
                p.z * scale,
                p.acquisitionTime);
        }
    public static Velocity scale(Velocity v, double scale)
        {
        return new Velocity(v.unit,
                v.xVeloc * scale,
                v.yVeloc * scale,
                v.zVeloc * scale,
                v.acquisitionTime);
        }
    public static Acceleration scale(Acceleration a, double scale)
        {
        return new Acceleration(a.unit,
                a.xAccel * scale,
                a.yAccel * scale,
                a.zAccel * scale,
                a.acquisitionTime);
        }

    public static Position integrate(Velocity v, double dt)
        {
        return new Position(v.unit,
                v.xVeloc * dt,
                v.yVeloc * dt,
                v.zVeloc * dt,
                v.acquisitionTime);
        }
    public static Velocity integrate(Acceleration a, double dt)
        {
        return new Velocity(a.unit,
                a.xAccel * dt,
                a.yAccel * dt,
                a.zAccel * dt,
                a.acquisitionTime);
        }

    //----------------------------------------------------------------------------------------------
    // Integration
    //----------------------------------------------------------------------------------------------

    /**
     * Integrate between two velocities to determine a change in position using an assumption
     * that the mean of the velocities has been acting the entire interval.

     * @param cur    the current velocity
     * @param prev   the previous velocity
     * @return       an approximation to the change in position over the interval
     *
     * @see <a href="https://en.wikipedia.org/wiki/Simpson%27s_rule">Simpson's Rule</a>
     */
    public static Position meanIntegrate(Velocity cur, Velocity prev)
        {
        double duration = (cur.acquisitionTime - prev.acquisitionTime) * 1e-9;
        Velocity meanVelocity = scale(plus(cur, prev), 0.5);
        return integrate(meanVelocity, duration);
        }

    /**
     * Integrate between two accelerations to determine a change in velocity using an assumption
     * that the mean of the accelerations has been acting the entire interval.
     *
     * @param cur    the current acceleration
     * @param prev   the previous acceleration
     * @return       an approximation to the change in velocity over the interval
     *
     * @see <a href="https://en.wikipedia.org/wiki/Simpson%27s_rule">Simpson's Rule</a>
     */
    public static Velocity meanIntegrate(Acceleration cur, Acceleration prev)
        {
        double duration = (cur.acquisitionTime - prev.acquisitionTime) * 1e-9;
        Acceleration meanAcceleration = scale(plus(cur,prev), 0.5);
        return integrate(meanAcceleration, duration);
        }

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    private NavUtil() { }
    }
