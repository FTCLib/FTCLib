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
package org.firstinspires.ftc.robotcore.internal.system;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * This is nothing more than a clone of {@link java.util.concurrent.CountDownLatch} that
 * additionally allows the count to be reset. We do this because we wish to use these in
 * high volume and thus seek to avoid unnecessary memory pressure.
 */
@SuppressWarnings("WeakerAccess")
public class ReusableCountDownLatch
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected static final class Sync extends AbstractQueuedSynchronizer
        {
        protected Sync(int count)
            {
            setState(count);
            }

        protected int getCount()
            {
            return getState();
            }

        protected void setCount(int count)
            {
            setState(count);
            }

        protected int tryAcquireShared(int acquires)
            {
            return (getState() == 0) ? 1 : -1;
            }

        protected boolean tryReleaseShared(int releases)
            {
            // Decrement count; signal when transition to zero
            for (;;)
                {
                int c = getState();
                if (c == 0)
                    return false;
                int nextc = c - 1;
                if (compareAndSetState(c, nextc))
                    return nextc == 0;
                }
            }
        }

    protected final Sync sync;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public ReusableCountDownLatch(int count)
        {
        if (count < 0) throw new IllegalArgumentException("count < 0");
        this.sync = new Sync(count);
        }

    /** note: there's no concurrency control here on the reset */
    public void reset(int count)
        {
        this.sync.setCount(count);
        }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    public void await() throws InterruptedException
        {
        sync.acquireSharedInterruptibly(1);
        }

    public boolean await(long timeout, TimeUnit unit)
            throws InterruptedException
        {
        return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
        }

    /** countDown() is idempotent if starting count is one or zero
     * @return true if we hit zero for the first time.
     */
    public boolean countDown()
        {
        return sync.releaseShared(1);
        }

    public long getCount()
        {
        return sync.getCount();
        }

    public String toString()
        {
        return super.toString() + "[Count = " + sync.getCount() + "]";
        }
    }
