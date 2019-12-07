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

import com.qualcomm.robotcore.util.WeakReferenceSet;

import java.util.Set;

/**
 * A {@link LightMultiplexor} adapts a second {@link SwitchableLight} by adding reference
 * counting to {@link SwitchableLight#enableLight(boolean)}: the light will be lit if the net
 * number of enables is greater than zero.
 */
@SuppressWarnings("WeakerAccess")
public class LightMultiplexor implements SwitchableLight
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected static final Set<LightMultiplexor> extantMultiplexors = new WeakReferenceSet<LightMultiplexor>();

    protected final SwitchableLight target;
    protected       int             enableCount;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public synchronized static LightMultiplexor forLight(SwitchableLight target)
        {
        for (LightMultiplexor multiplexor : extantMultiplexors)
            {
            if (multiplexor.target.equals(target))
                {
                return multiplexor;
                }
            }

        LightMultiplexor result = new LightMultiplexor(target);
        extantMultiplexors.add(result);
        return result;
        }

    protected LightMultiplexor(SwitchableLight target)
        {
        this.target = target;
        this.enableCount = 0;
        }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    @Override public boolean isLightOn()
        {
        return target.isLightOn();
        }

    @Override public synchronized void enableLight(boolean enable)
        {
        if (enable)
            {
            if (enableCount++ == 0)
                {
                target.enableLight(true);
                }
            }
        else
            {
            if (enableCount > 0 && --enableCount==0)
                {
                target.enableLight(false);
                }
            }
        }
    }
