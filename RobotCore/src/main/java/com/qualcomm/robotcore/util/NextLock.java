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
 * A {@link NextLock} is a concurrency manager that allows one to await the next occurrence of
 * an event in a (possibly infinite) sequence that follows after the juncture at which one
 * chooses to pay attention.
 */
public class NextLock
    {
    protected final Object lock  = this;
    protected long         count = 0;

    public NextLock()
        {
        }

    /**
     * {@link Waiter} instances are returned from {@link #getNextWaiter()}, and can
     * be used to await the next {@link #advanceNext()} call in lock from which they
     * were retrieved.
     */
    public class Waiter
        {
        long nextCount;

        Waiter(long nextCount)
            {
            this.nextCount = nextCount;
            }

        /**
         * Awaits the next {@link #advanceNext()} call in the associated {@link NextLock}.
         * @throws InterruptedException
         * @see #advanceNext()
         */
        public void awaitNext() throws InterruptedException
            {
            synchronized (lock)
                {
                do {
                   lock.wait();
                   }
                while (count < nextCount);
                }
            }

        /**
         * Awaits the next {@link #advanceNext()} call in the associated {@link NextLock},
         * but only up to a maximum indicated amount of time.
         * @param millis the maximum number of milliseconds to wait.
         * @throws InterruptedException
         */
        /*
        public void awaitNext(long millis) throws InterruptedException
            {
            synchronized (lock)
                {
                do {
                   lock.wait(millis);   // TODO bug
                   }
                while (count < nextCount);
                }
            }
        */
        }

    /**
     * Returns a {@link Waiter} that will await the next {@link #advanceNext()}.
     * @return a {@link Waiter} that will await the next {@link #advanceNext()}.
     * @see #advanceNext()
     */
    public Waiter getNextWaiter()
        {
        synchronized (lock)
            {
            return new Waiter(this.count + 1);
            }
        }

    /**
     * Advances to the next event in the sequence.
     * @see #getNextWaiter()
     */
    public void advanceNext()
        {
        synchronized (lock)
            {
            count += 1;
            lock.notifyAll();
            }
        }
    }
