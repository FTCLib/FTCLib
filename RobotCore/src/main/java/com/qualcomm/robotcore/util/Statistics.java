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
 * This handy utility class supports the ongoing calculation of mean and variance of a 
 * series of numbers. This class is *not* thread-safe.
 *
 * @see <a href="http://en.wikipedia.org/wiki/Algorithms_for_calculating_variance#On-line_algorithm">Wikipedia</a>
 */
public class Statistics
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    int    n;
    double mean;
    double m2;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public Statistics()
        {
        this.clear();
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
        return n;
        }

    /**
     * Returns the mean of the current set of samples
     * @return the mean of the samples
     */
    public double getMean()
        {
        return mean;
        }

    /**
     * Returns the sample variance of the current set of samples
     * @return the variance of the samples
     */
    public double getVariance()
        {
        return m2 / (n - 1);
        }

    /**
     * Returns the sample standard deviation of the current set of samples
     * @return the standard deviation of the samples
     */
    public double getStandardDeviation()
        {
        return Math.sqrt(this.getVariance());
        }

    //----------------------------------------------------------------------------------------------
    // Modifying
    //----------------------------------------------------------------------------------------------

    /**
     * Resets the statistics to an empty state
     */
    public void clear()
        {
        n    = 0;
        mean = 0;
        m2   = 0;
        }

    /**
     * Adds a new sample to the statistics
     * @param x the sample to add
     */
    public void add(double x)
        {
        n = n + 1;
        double delta = x - mean;
        mean = mean + delta / n;
        m2 = m2 + delta*(x - mean);
        }

    /**
     * Removes a sample from the statistics
     * @param x the sample to remove
     */
    public void remove(double x)
        {
        int nPrev = n-1;
        double delta = x - mean;
        double deltaPrev = n * delta / nPrev;
        m2 = m2 - deltaPrev * delta;
        mean = (mean * n - x) / nPrev;
        n = nPrev;
        }
    }

