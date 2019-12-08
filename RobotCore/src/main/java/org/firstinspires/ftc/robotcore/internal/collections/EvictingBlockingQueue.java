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
package org.firstinspires.ftc.robotcore.internal.collections;

import androidx.annotation.NonNull;

import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.function.Consumer;
import org.firstinspires.ftc.robotcore.internal.system.Assert;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * {@link EvictingBlockingQueue} is a {@link BlockingQueue} that evicts old elements
 * rather than failing when new data is added to the queue.
 */
@SuppressWarnings("WeakerAccess")
public class EvictingBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E>
    {
    //----------------------------------------------------------------------------------------------
    // State
    //
    // The central implementation idea is that we must hold theLock to make any additions, and
    // must then with lock held ensure capacity by evicting if necessary before doing any addition.
    // Removals also take the lock so we don't evict data unncessarily.
    //----------------------------------------------------------------------------------------------

    protected final Object     theLock = new Object();
    protected BlockingQueue<E> targetQueue;
    protected Consumer<E>      evictAction = null;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    /**
     * Constructs an EvictingBlockingQueue using the target queue as an implementation. The
     * target queue must have a capacity of at least one.
     * @param targetQueue the underlying implementation queue from which we will auto-evict as needed
     */
    public EvictingBlockingQueue(BlockingQueue<E> targetQueue)
        {
        this.targetQueue = targetQueue;
        }

    public void setEvictAction(Consumer<E> evictAction)
        {
        synchronized (theLock)
            {
            this.evictAction = evictAction;
            }
        }

    //----------------------------------------------------------------------------------------------
    // AbstractCollection
    //----------------------------------------------------------------------------------------------

    @Override public @NonNull Iterator<E> iterator()
        {
        return targetQueue.iterator();
        }

    @Override public int size()
        {
        return targetQueue.size();
        }

    //----------------------------------------------------------------------------------------------
    // Core: the hard parts
    //----------------------------------------------------------------------------------------------

    @Override public boolean offer(@NonNull E e)
        {
        synchronized (theLock)
            {
            if (targetQueue.remainingCapacity() == 0)
                {
                E evicted = targetQueue.poll();
                Assert.assertNotNull(evicted);
                if (evictAction != null)
                    {
                    evictAction.accept(evicted);
                    }
                }
            boolean result = targetQueue.offer(e);
            Assert.assertTrue(result);
            theLock.notifyAll(); // pending polls/takes are worth trying again
            return result;
            }
        }

    @Override public E take() throws InterruptedException
        {
        synchronized (theLock)
            {
            for (;;)
                {
                // Can we get something? Return if we can.
                E result = poll();
                if (result != null)
                    return result;

                // Punt if we've been asked to
                if (Thread.currentThread().isInterrupted())
                    throw new InterruptedException();

                // Wait and then try again
                theLock.wait();
                }
            }
        }

    @Override public E poll(long timeout, @NonNull TimeUnit unit) throws InterruptedException
        {
        synchronized (theLock)
            {
            final long deadline = System.nanoTime() + unit.toNanos(timeout);
            for (;;)
                {
                // Can we get something? Return if we can.
                E result = poll();
                if (result != null)
                    return result;

                // Punt if we've been asked to
                if (Thread.currentThread().isInterrupted())
                    throw new InterruptedException();

                // How much longer can we wait?
                long remaining = deadline - System.nanoTime();
                if (remaining > 0)
                    {
                    // Wait up to that much and then try again
                    long ms = remaining / ElapsedTime.MILLIS_IN_NANO;
                    long ns = remaining - ms * ElapsedTime.MILLIS_IN_NANO;
                    theLock.wait(ms, (int)ns);
                    }
                else
                    return null;
                }
            }
        }

    //----------------------------------------------------------------------------------------------
    // Remaining parts
    //----------------------------------------------------------------------------------------------

    @Override public E poll()
        {
        synchronized (theLock)
            {
            return targetQueue.poll();
            }
        }

    @Override public E peek()
        {
        return targetQueue.peek();
        }

    @Override public void put(E e) throws InterruptedException
        {
        offer(e);
        }

    @Override public boolean offer(E e, long timeout, @NonNull TimeUnit unit) throws InterruptedException
        {
        // We will never block because we're full, so the timeouts are unnecessary
        return offer(e);
        }

    @Override public int remainingCapacity()
        {
        // We *always* have capacity
        return Math.max(targetQueue.remainingCapacity(), 1);
        }

    @Override public int drainTo(@NonNull Collection<? super E> c)
        {
        synchronized (theLock)
            {
            return targetQueue.drainTo(c);
            }
        }

    @Override public int drainTo(@NonNull Collection<? super E> c, int maxElements)
        {
        synchronized (theLock)
            {
            return targetQueue.drainTo(c, maxElements);
            }
        }
    }
