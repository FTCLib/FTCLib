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

import androidx.annotation.Nullable;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link TimestampedI2cData} extends {@link TimestampedData} so as to provide an indication
 * of the I2c source from which the data was retrieved.
 */
public class TimestampedI2cData extends TimestampedData
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    /** the I2c address from which the data was read */
    public I2cAddr  i2cAddr;

    /** the starting register address from which the data was retrieved */
    public int      register;

    /** internal: keeps track of */
    protected static AtomicInteger healthStatusSuppressionCount = new AtomicInteger(0);

    //----------------------------------------------------------------------------------------------
    // Fake data / device health management
    //----------------------------------------------------------------------------------------------

    /**
     * Creates and returns fake I2C data for use in situations where data must be returned but actual
     * data is unavailable. Optionally, records that the device in question is having difficulties.
     */
    public static TimestampedI2cData makeFakeData(@Nullable Object deviceHavingProblems, I2cAddr i2cAddr, int ireg, int creg)
        {
        if (healthStatusSuppressionCount.get() == 0)
            {
            if (deviceHavingProblems != null && deviceHavingProblems instanceof HardwareDeviceHealth)
                {
                ((HardwareDeviceHealth) deviceHavingProblems).setHealthStatus(HardwareDeviceHealth.HealthStatus.UNHEALTHY);
                }
            }

        TimestampedI2cData result = new TimestampedI2cData();
        result.data     = new byte[creg];       // all zeros
        result.nanoTime = System.nanoTime();
        result.i2cAddr  = i2cAddr;
        result.register = ireg;
        return result;
        }

    public static void suppressNewHealthWarningsWhile(Runnable runnable)
        {
        healthStatusSuppressionCount.getAndIncrement();
        try {
            runnable.run();
            }
        finally
            {
            healthStatusSuppressionCount.getAndDecrement();
            }
        }

    public static void suppressNewHealthWarnings(boolean suppress)
        {
        if (suppress)
            healthStatusSuppressionCount.getAndIncrement();
        else
            healthStatusSuppressionCount.getAndDecrement();
        }
    }
