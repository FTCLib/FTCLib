/*
Copyright (c) 2017 Robert Atkinson

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
package com.qualcomm.robotcore.hardware.configuration;

import com.google.gson.annotations.Expose;
import com.qualcomm.robotcore.util.TypeConversion;

import org.firstinspires.ftc.robotcore.internal.system.Assert;

import java.io.Serializable;

/**
 * {@link ModernRoboticsMotorControllerParamsState} contains metadata transcribed from {@link ModernRoboticsMotorControllerParams}
 */
@SuppressWarnings("WeakerAccess")
public class ModernRoboticsMotorControllerParamsState implements Serializable, Cloneable
    {
    public @Expose int ratio = 0;
    public @Expose int p = 0;
    public @Expose int i = 0;
    public @Expose int d = 0;

    public ModernRoboticsMotorControllerParamsState()
        {
        Assert.assertTrue(this.isDefault());
        }

    public ModernRoboticsMotorControllerParamsState(ModernRoboticsMotorControllerParams params)
        {
        this.ratio = params.ratio();
        this.p = params.P();
        this.i = params.I();
        this.d = params.D();
        }

    public ModernRoboticsMotorControllerParamsState clone()
        {
        try {
            return (ModernRoboticsMotorControllerParamsState)super.clone();
            }
        catch (CloneNotSupportedException e)
            {
            throw new RuntimeException("internal error: Parameters not cloneable");
            }
        }

    public static ModernRoboticsMotorControllerParamsState fromByteArray(byte[] controllerData)
        {
        ModernRoboticsMotorControllerParamsState result = new ModernRoboticsMotorControllerParamsState();
        result.ratio = TypeConversion.unsignedByteToInt(controllerData[0]);
        result.p = TypeConversion.unsignedByteToInt(controllerData[1]);
        result.i = TypeConversion.unsignedByteToInt(controllerData[2]);
        result.d = TypeConversion.unsignedByteToInt(controllerData[3]);
        return result;
        }

    public byte[] toByteArray()
        {
        byte[] result = new byte[4];
        result[0] = (byte) ratio;
        result[1] = (byte) p;
        result[2] = (byte) i;
        result[3] = (byte) d;
        return result;
        }

    public boolean isDefault()
        {
        return ratio == 0 && p == 0 && i == 0 && d == 0;
        }

    @Override public boolean equals(Object o)
        {
        if (o instanceof ModernRoboticsMotorControllerParamsState)
            {
            ModernRoboticsMotorControllerParamsState them = (ModernRoboticsMotorControllerParamsState) o;
            return this.ratio == them.ratio && this.p == them.p && this.i == them.i && this.d == them.d;
            }
        else
            return false;
        }

    @Override public int hashCode()
        {
        return ratio ^ (p << 3) ^ (i << 6) ^ (d << 9) ^ 0xFAD11234;
        }

    @Override public String toString()
        {
        return String.format("ratio=%d,p=%d,i=%d,d=%d", ratio, p, i, d);
        }
    }
