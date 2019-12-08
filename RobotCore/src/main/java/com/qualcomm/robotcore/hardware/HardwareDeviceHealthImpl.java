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
package com.qualcomm.robotcore.hardware;

import androidx.annotation.Nullable;

import java.util.concurrent.Callable;

/**
 * {@link HardwareDeviceHealthImpl} provides a delegatable-to implemenatation of HardwareDeviceHealth
 */
@SuppressWarnings("WeakerAccess")
public class HardwareDeviceHealthImpl implements HardwareDeviceHealth
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected String        tag;
    protected HealthStatus  healthStatus;
    protected Callable<HealthStatus> override;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public HardwareDeviceHealthImpl(String tag)
        {
        this(tag, null);
        }

    public HardwareDeviceHealthImpl(String tag, @Nullable Callable<HealthStatus> override)
        {
        this.tag = tag;
        this.healthStatus = HealthStatus.UNKNOWN;
        this.override = override;
        }

    public void close()
        {
        setHealthStatus(HealthStatus.CLOSED);
        }

    //----------------------------------------------------------------------------------------------
    // HardwareDeviceHealth
    //----------------------------------------------------------------------------------------------

    @Override
    public void setHealthStatus(HealthStatus status)
        {
        synchronized (this)
            {
            // Once closed, don't accidentally go back
            if (this.healthStatus != HealthStatus.CLOSED)
                {
                this.healthStatus = status;
                }
            }
        }

    @Override
    public HealthStatus getHealthStatus()
        {
        synchronized (this)
            {
            if (this.override != null)
                {
                try {
                    HealthStatus result = override.call();
                    if (result != HealthStatus.UNKNOWN)
                        {
                        return result;
                        }
                    }
                catch (Exception e)
                    {
                    // ignore
                    }
                }
            return this.healthStatus;
            }
        }
    }
