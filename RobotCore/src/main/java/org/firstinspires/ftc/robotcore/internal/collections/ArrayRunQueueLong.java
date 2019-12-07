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
package org.firstinspires.ftc.robotcore.internal.collections;

import java.util.NoSuchElementException;

/**
 * {@link ArrayRunQueueLong} maintains a deque of elements but in a manner that encodes the
 * runs of repeats of the same element very efficiently.
 */
@SuppressWarnings("WeakerAccess")
public class ArrayRunQueueLong
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected CircularLongBuffer elements;
    protected CircularIntBuffer  counts;
    protected int                size;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public ArrayRunQueueLong()
        {
        this(8);
        }

    public ArrayRunQueueLong(int initialCapacity)
        {
        elements = new CircularLongBuffer(initialCapacity);
        counts   = new CircularIntBuffer(initialCapacity);
        clear();
        }

    //----------------------------------------------------------------------------------------------
    // Accessing
    //----------------------------------------------------------------------------------------------

    protected int computedSize()
        {
        int total = 0;
        for (int i = 0; i < counts.size(); i++)
            {
            total += counts.get(i);
            }
        return total;
        }

    protected void verifyInvariants()
        {
        // Assert.assertTrue(computedSize()==size, "computed=%d size=%d", computedSize(), size);    // is slow, so disable for now
        }

    public boolean isEmpty()
        {
        return counts.isEmpty();
        }

    public int size()
        {
        return size;
        }

    public long getFirst()
        {
        if (!isEmpty())
            {
            return elements.getFirst();
            }
        throw new NoSuchElementException("getFirst");
        }

    public long getLast()
        {
        if (!isEmpty())
            {
            return elements.getLast();
            }
        throw new NoSuchElementException("getLast");
        }

    //----------------------------------------------------------------------------------------------
    // Adding
    //----------------------------------------------------------------------------------------------

    public boolean offerLast(long e)
        {
        addLast(e);
        return true;
        }

    public void addLast(long element)
        {
        addLast(element, 1);
        }

    public void addLast(long element, int count)
        {
        try {
            if (count < 0) throw new IllegalArgumentException(String.format("count must be >= 0: %d", count));
            if (count > 0)
                {
                int sizeAfter = size() + count;
                if (!isEmpty() && (elements.getLast() == element))
                    {
                    int index = counts.size() - 1;
                    counts.put(index, counts.get(index) + count);
                    }
                else
                    {
                    elements.addLast(element);
                    counts.addLast(count);
                    }
                size = sizeAfter;
                }
            }
        finally
            {
            verifyInvariants();
            }
        }

    //----------------------------------------------------------------------------------------------
    // Removing
    //----------------------------------------------------------------------------------------------

    public void clear()
        {
        elements.clear();
        counts.clear();
        size = 0;
        verifyInvariants();
        }

    /** @return the last element removed, or NoSuchElementException if nothing was removed */
    public long removeFirstCount(int countToRemove)
        {
        try {
            if (countToRemove < 0)      throw new IllegalArgumentException(String.format("count must be >= 0: %d", countToRemove));
            if (countToRemove > size)   throw new NoSuchElementException(String.format("count must be <= size: count=%d size=%d", countToRemove, size));

            long elementLastRemoved = 0;
            if (countToRemove > 0)
                {
                int sizeAfter = size() - countToRemove;
                while (countToRemove > 0)
                    {
                    int firstCount = counts.get(0);
                    if (firstCount <= countToRemove)
                        {
                        countToRemove -= firstCount;
                        counts.removeFirst();
                        elementLastRemoved = elements.removeFirst();
                        }
                    else
                        {
                        counts.put(0, firstCount - countToRemove);
                        countToRemove = 0;
                        elementLastRemoved = elements.getFirst();
                        }
                    }
                size = sizeAfter;
                }
            return elementLastRemoved;
            }
        finally
            {
            verifyInvariants();
            }
        }
    }
