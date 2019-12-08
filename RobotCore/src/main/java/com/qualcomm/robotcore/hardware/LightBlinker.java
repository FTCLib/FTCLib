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

import android.graphics.Color;
import androidx.annotation.ColorInt;

import com.qualcomm.robotcore.util.ThreadPool;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * A {@link LightBlinker} is a handy utility that will flash a {@link com.qualcomm.robotcore.hardware.SwitchableLight}
 * in a pattern of timed durations, and, optionally, colors, if the light supports same (NYI)
 */
@SuppressWarnings("WeakerAccess")
public class LightBlinker implements Blinker
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "LightBlinker";

    protected final SwitchableLight         light;
    protected       ArrayList<Step>         currentSteps;
    protected       Deque<ArrayList<Step>>  previousSteps;
    protected       ScheduledFuture<?>      future;
    protected       int                     nextStep;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public LightBlinker(SwitchableLight light)
        {
        this.light = light;
        this.currentSteps = new ArrayList<Step>();
        this.previousSteps = new ArrayDeque<ArrayList<Step>>();
        this.future = null;
        }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    @Override public void setConstant(@ColorInt int color)
        {
        Step step = new Step(color, 1, TimeUnit.SECONDS);
        List<Step> steps = new ArrayList<Step>();
        steps.add(step);
        setPattern(steps);
        }

    @Override public void stopBlinking()
        {
        setConstant(Color.BLACK);
        }

    @Override public int getBlinkerPatternMaxLength()
        {
        return Integer.MAX_VALUE;
        }

    @Override public synchronized void pushPattern(Collection<Step> steps)
        {
        this.previousSteps.push(this.currentSteps);
        setPattern(steps);
        }

    @Override public synchronized boolean patternStackNotEmpty()
        {
        return this.previousSteps.size() > 0;
        }

    @Override public synchronized boolean popPattern()
        {
        try {
            setPattern(previousSteps.pop());
            return true;
            }
        catch (NoSuchElementException e)
            {
            setPattern(null);
            }
        return false;
        }

    @Override public synchronized void setPattern(Collection<Step> steps)
        {
        if (isCurrentPattern(steps)) // for this implementation, there's simply no point otherwise
            {
            stop();
            if (steps == null || steps.size() == 0)
                {
                this.currentSteps = new ArrayList<Step>();
                this.light.enableLight(false);
                }
            else
                {
                this.currentSteps = new ArrayList<Step>(steps);
                if (steps.size() == 1)
                    {
                    this.light.enableLight(this.currentSteps.get(0).isLit());
                    }
                else
                    {
                    this.nextStep = 0;
                    scheduleNext();
                    }
                }
            }
        }

    protected boolean isCurrentPattern(Collection<Step> steps)
        {
        if (steps.size() != this.currentSteps.size()) return false;
        int i = 0;
        for (Step theirStep : steps)
            {
            Step ourStep = this.currentSteps.get(i++);
            if (!theirStep.equals(ourStep)) return false;
            }
        return true;
        }

    @Override public synchronized Collection<Step> getPattern()
        {
        // Return a copy so caller can't mess with us
        return new ArrayList<Step>(this.currentSteps);
        }

    protected synchronized void scheduleNext()
        {
        // Dig out this step, and advance
        final Step thisStep = this.currentSteps.get(this.nextStep++);
        if (this.nextStep >= currentSteps.size()) this.nextStep = 0;

        // Light this step appropriately
        boolean isLit = thisStep.isLit();
        // RobotLog.vv(TAG, "light is lit: %s", isLit);
        this.light.enableLight(isLit);

        // Wait this step's duration, and then do the next one
        this.future = ThreadPool.getDefaultScheduler().schedule(new Runnable()
            {
            @Override public void run()
                {
                scheduleNext();
                }
            }, thisStep.getDurationMs(), TimeUnit.MILLISECONDS);
        }

    protected synchronized void stop()
        {
        if (this.future != null)
            {
            this.future.cancel(false);
            this.future = null;
            }
        }
    }
