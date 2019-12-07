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
 * Instances of {@link TempUnit} enumerate a known different temperature scales
 */
public enum TempUnit
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    CELSIUS(0), FARENHEIT(1), KELVIN(2);
    public final byte bVal;

    public static final double zeroCelsiusK = 273.15;
    public static final double zeroCelsiusF = 32;
    public static final double CperF        = 5.0 / 9.0;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    TempUnit(int i)
        {
        this.bVal = (byte)i;
        }

    //----------------------------------------------------------------------------------------------
    // Primitive operations
    //----------------------------------------------------------------------------------------------

    public double fromCelsius(double celsius)
        {
        switch (this)
            {
            default:
            case CELSIUS:   return celsius;
            case KELVIN:    return celsius + zeroCelsiusK;
            case FARENHEIT: return celsius / CperF + zeroCelsiusF;
            }
        }

    public double fromKelvin(double kelvin)
        {
        switch (this)
            {
            default:
            case CELSIUS:   return kelvin - zeroCelsiusK;
            case KELVIN:    return kelvin;
            case FARENHEIT: return fromCelsius(CELSIUS.fromKelvin(kelvin));
            }
        }

    public double fromFarenheit(double farenheit)
        {
        switch (this)
            {
            default:
            case CELSIUS:   return (farenheit - zeroCelsiusF) * CperF;
            case KELVIN:    return fromCelsius(CELSIUS.fromFarenheit(farenheit));
            case FARENHEIT: return farenheit;
            }
        }

    public double fromUnit(TempUnit him, double his)
        {
        switch (him)
            {
            default:
            case CELSIUS:   return this.fromCelsius(his);
            case KELVIN:    return this.fromKelvin(his);
            case FARENHEIT: return this.fromFarenheit(his);
            }
        }

    //----------------------------------------------------------------------------------------------
    // Derived operations
    //----------------------------------------------------------------------------------------------

    double toCelsius(double inOurUnits)
        {
        switch (this)
            {
            default:
            case CELSIUS:   return CELSIUS.fromCelsius(inOurUnits);
            case KELVIN:    return CELSIUS.fromKelvin(inOurUnits);
            case FARENHEIT: return CELSIUS.fromFarenheit(inOurUnits);
            }
        }

    double toKelvin(double inOurUnits)
        {
        switch (this)
            {
            default:
            case CELSIUS:   return KELVIN.fromCelsius(inOurUnits);
            case KELVIN:    return KELVIN.fromKelvin(inOurUnits);
            case FARENHEIT: return KELVIN.fromFarenheit(inOurUnits);
            }
        }

    double toFarenheit(double inOurUnits)
        {
        switch (this)
            {
            default:
            case CELSIUS:   return FARENHEIT.fromCelsius(inOurUnits);
            case KELVIN:    return FARENHEIT.fromKelvin(inOurUnits);
            case FARENHEIT: return FARENHEIT.fromFarenheit(inOurUnits);
            }
        }
    }
