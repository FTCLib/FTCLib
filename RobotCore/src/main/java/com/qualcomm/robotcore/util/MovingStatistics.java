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

import java.util.LinkedList;
import java.util.Queue;

/**
 * MovingStatistics keeps statistics on the most recent samples in a data set, automatically
 * removing old samples as the size of the data exceeds a fixed capacity. This class is *not*
 * thread-safe.
 */
public class MovingStatistics
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    final Statistics      statistics;
    final int             capacity;
    final Queue<Double>   samples;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public MovingStatistics(int capacity)
        {
        if (capacity <= 0) throw new IllegalArgumentException("MovingStatistics capacity must be positive");
        this.statistics = new Statistics();
        this.capacity   = capacity;
        this.samples    = new LinkedList<Double>();
        }

    //----------------------------------------------------------------------------------------------
    // Accessing
    //----------------------------------------------------------------------------------------------

    /**
     * Returns the current number of samples
     * @return the number of samples
     */
    public int getCount()
        {
        return this.statistics.getCount();
        }

    /**
     * Returns the mean of the current set of samples
     * @return the mean of the samples
     */
    public double getMean()
        {
        return this.statistics.getMean();
        }

    /**
     * Returns the sample variance of the current set of samples
     * @return the variance of the samples
     */
    public double getVariance()
        {
        return this.statistics.getVariance();
        }

    /**
     * Returns the sample standard deviation of the current set of samples
     * @return the standard deviation of the samples
     */
    public double getStandardDeviation()
        {
        return this.statistics.getStandardDeviation();
        }

    //----------------------------------------------------------------------------------------------
    // Modifying
    //----------------------------------------------------------------------------------------------

    /**
     * Resets the statistics to an empty state
     */
    public void clear()
        {
        this.statistics.clear();
        this.samples.clear();
        }

    /**
     * Adds a new sample to the statistics, possibly also removing the oldest.
     * @param x the sample to add
     */
    public void add(double x)
        {
        this.statistics.add(x);
        this.samples.add(x);
        if (this.samples.size() > capacity)
            {
            this.statistics.remove(this.samples.remove());
            }
        }
    }
