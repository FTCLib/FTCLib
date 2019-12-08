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

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * {@link Blinker} provides the means to control an LED or a light that can be illuminated in a
 * sequenced pattern of colors and durations.
 */
@SuppressWarnings("WeakerAccess")
public interface Blinker
    {
    /**
     * Sets the pattern with which this LED or light should illuminate. If the list of steps is longer
     * than the maximum number supported, then the pattern is truncated.
     * @param steps the pattern of colors and durations that the LED or light should illuminate itself with
     */
    void setPattern(Collection<Step> steps);

    /**
     * Returns the current blinking pattern
     * @return the current blinking pattern
     */
    Collection<Step> getPattern();

    /**
     * Saves the existing pattern such that it can be later restored, then calls setPattern().
     * @param steps the new pattern to be displayed
     */
    void pushPattern(Collection<Step> steps);

    /**
     * Returns whether the pattern stack is currently nonempty.
     * @return whether the pattern stack is currently nonempty.
     */
    boolean patternStackNotEmpty();

    /**
     * Pops the next pattern off of the stack of saved patterns, if any.
     * If the stack is empty, then this sets the blinker to a constant black.
     * @return whether or not a pattern was removed from the stack (ie: whether
     * the pattern stack was not empty prior to the call
     */
    boolean popPattern();

    /**
     * Sets the blinker pattern to be a single, unchanging color
     * @param color the color with which the LED or light should be illuminated
     */
    void setConstant(@ColorInt int color);

    /**
     * Sets the blinker to constant black and frees any internal resources
     */
    void stopBlinking();

    /**
     * Returns the maximum number of {@link Step}s that can be present in a pattern
     * @return the maximum number of {@link Step}s that can be present in a pattern
     */
    int getBlinkerPatternMaxLength();

    /**
     * {@link Step} represents a particular color held for a particular length of time.
     */
    class Step
        {
        //------------------------------------------------------------------------------------------
        // State
        //------------------------------------------------------------------------------------------

        protected @ColorInt int color = 0;
        protected           int msDuration = 0;

        //------------------------------------------------------------------------------------------
        // Construction
        //------------------------------------------------------------------------------------------

        public Step() { }
        public Step(@ColorInt int color, long duration, TimeUnit unit)
            {
            this.color = color & 0xFFFFFF;  // strip alpha so that equals() is robust
            setDuration(duration, unit);
            }
        public static Step nullStep()
            {
            return new Step();
            }

        //------------------------------------------------------------------------------------------
        // Comparing
        //------------------------------------------------------------------------------------------

        @Override public boolean equals(Object them)
            {
            if (them instanceof Step)
                {
                return this.equals((Step)them);
                }
            return false;
            }

        public boolean equals(Step step)
            {
            return this.color == step.color && this.msDuration == step.msDuration;
            }

        @Override public int hashCode()
            {
            return ((this.color << 5) | this.msDuration) ^ 0x2EDA /*arbitrary*/;
            }

        //------------------------------------------------------------------------------------------
        // Accessing
        //------------------------------------------------------------------------------------------

        public boolean isLit()
            {
            return Color.red(color) != 0
                    || Color.green(color) != 0
                    || Color.blue(color) != 0;
            }

        public void setLit(boolean isEnabled)
            {
            setColor(isEnabled ? Color.WHITE : Color.BLACK);
            }

        public @ColorInt int getColor()
            {
            return color;
            }
        public void setColor(@ColorInt int color)
            {
            this.color = color;
            }

        public int getDurationMs()
            {
            return msDuration;
            }
        public void setDuration(long duration, TimeUnit unit)
            {
            this.msDuration = (int)unit.toMillis(duration);
            }
        }
    }
