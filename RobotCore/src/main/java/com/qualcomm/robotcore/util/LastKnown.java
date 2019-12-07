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

package com.qualcomm.robotcore.util;

/**
 * Instances of LastKnown can keep track of a last known value for a certain datum, together
 * with whether in fact any such last value is known at all. Values can become unknown either
 * due to explicit invalidation, or by not being fresh enough. The requirement that values be
 * 'fresh' helps guard against an error situation in which the underlying setting has actually
 * been lost and the user's code is simply trying to apply it. Were we to not have this, such
 * error situations might never be repaired.
 *
 * This class is NOT thread-safe.
 */
public class LastKnown<T>
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected T           value;
    protected boolean     isValid;
    protected ElapsedTime timer;
    protected double      msFreshness;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public LastKnown()
        {
        this(500);
        }

    public LastKnown(double msFreshness)
        {
        this.value       = null;
        this.isValid     = false;
        this.timer       = new ElapsedTime();
        this.msFreshness = msFreshness;
        }

    public static <X> LastKnown<X>[] createArray(int length)
        {
        LastKnown<X>[] result = new LastKnown[length];
        for (int i = 0; i < length; i++)
            result[i] = new LastKnown<X>();
        return result;
        }

    public static <X> void invalidateArray(LastKnown<X>[] array)
        {
        for (int i = 0; i < array.length; i++)
            {
            array[i].invalidate();
            }
        }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    /**
     * Marks the last known value as invalid. However, the value internally stored is uneffected.
     * @see #getRawValue()
     * @see #getValue()
     */
    public void invalidate()
        {
        this.isValid = false;
        }

    /**
     * Returns whether a last value is currently known and is fresh enough. Note that a value
     * which is valid may spontaneously become invalid (because it expires) but a value which
     * is invalid will never spontaneously become valid.
     * @return whether a last value is currently known and is fresh enough
     */
    public boolean isValid()
        {
        return this.isValid && this.timer.milliseconds() <= msFreshness;
        }

    /**
     * Returns the last known value, or null if not valid
     * @return the last known value
     */
    public T getValue()
        {
        return this.isValid() ? this.value : null;
        }

    /**
     * Returns the stored value, w/o using a timer to invalidate
     * @return the raw stored value.
     */
    public T getNonTimedValue()
        {
        return this.isValid ? this.value : null;
        }

    /**
     * Returns the stored value, whether or not it is valid
     * @return the raw stored value.
     */
    public T getRawValue()
        {
        return this.value;
        }

    /**
     * If non-null, sets the current value to be the indicated (known) value and resets
     * the freshness timer. If null, this is equivalent to {@link #invalidate()}.
     * @return the previous value.
     */
    public T setValue(T value)
        {
        T prevValue = this.value;
        this.value = value;
        this.isValid = true;
        if (null == value)
            invalidate();
        else
            this.timer.reset();
        return prevValue;
        }

    /**
     * Answers whether the last known value is both valid and equal to the value indicated. Note
     * that the .equals() method is used to make the comparison.
     * @param valueQ the value queried
     * @return whether the last known value is both valid and equal to the value indicated
     */
    public boolean isValue(T valueQ)
        {
        if (this.isValid())
            return this.value.equals(valueQ);
        else
            return false;
        }

    /**
     * If the last known value is not both valid and equal to the indicated value, updates it to be
     * same and returns true; otherwise, returns false.
     * @param valueQ the value queried
     * @return whether the value was just updated
     */
    public boolean updateValue(T valueQ)
        {
        if (!isValue(valueQ))
            {
            setValue(valueQ);
            return true;
            }
        else
            return false;
        }

    }
