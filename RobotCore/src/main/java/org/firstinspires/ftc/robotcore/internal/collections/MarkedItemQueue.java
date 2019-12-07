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

import org.firstinspires.ftc.robotcore.internal.system.Assert;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * {@link MarkedItemQueue} conceptually contains an finite set of discrete items, some of
 * which are 'marked', but which are otherwise indistinguishable. Items, both marked and unmarked,
 * can be added to the tail of the queue. The items at the head of the queue can be removed,
 * up to and including the next mark.
 */
@SuppressWarnings("WeakerAccess")
public class MarkedItemQueue
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected Deque<Integer> countsToMarks;
    protected Integer        unmarkedAtEnd;
    protected int            size;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public MarkedItemQueue()
        {
        this(8);
        }

    public MarkedItemQueue(int initialCapacity)
        {
        countsToMarks = new ArrayDeque<Integer>(initialCapacity);
        clear();
        }

    //----------------------------------------------------------------------------------------------
    // Accessing
    //----------------------------------------------------------------------------------------------

    public boolean hasMarkedItem()
        {
        return !countsToMarks.isEmpty();
        }

    public boolean isEmpty()
        {
        return countsToMarks.isEmpty() && unmarkedAtEnd==0;
        }

    public boolean isAtMarkedItem()
        {
        return !countsToMarks.isEmpty() && countsToMarks.peekFirst()==1;
        }

    public int size()
        {
        return size;
        }

    protected int computedSize()
        {
        int computedSize = unmarkedAtEnd;
        for (Integer count : countsToMarks)
            {
            computedSize += count;
            }
        return computedSize;
        }

    protected void verifyInvariants()
        {
        // Assert.assertTrue(computedSize()==size, "computed=%d size=%d", computedSize(), size);
        }

    //----------------------------------------------------------------------------------------------
    // Removing
    //----------------------------------------------------------------------------------------------

    public void clear()
        {
        countsToMarks.clear();
        unmarkedAtEnd = 0;
        size = 0;
        verifyInvariants();
        }

    /**
     * @return the count of items removed
     */
    public int removeUpToNextMarkedItemOrEnd()
        {
        try {
            int itemCountRemoved = 0;

            if (!countsToMarks.isEmpty())
                {
                int nextItems = countsToMarks.removeFirst();
                itemCountRemoved += nextItems-1;
                countsToMarks.addFirst(1);
                Assert.assertTrue(isAtMarkedItem());
                }
            else
                {
                // We contain no marks: eat everything
                itemCountRemoved += unmarkedAtEnd;
                unmarkedAtEnd = 0;
                Assert.assertTrue(isEmpty());
                }

            size -= itemCountRemoved;
            return itemCountRemoved;
            }
        finally
            {
            verifyInvariants();
            }
        }

    public void removeItems(int itemCountToRemove)
        {
        if (itemCountToRemove < 0)      throw new IllegalArgumentException(String.format("remove count must be >=0: %d", itemCountToRemove));
        if (itemCountToRemove > size)   throw new IllegalArgumentException(String.format("remove count must be <= size: count=%d, size=%d", itemCountToRemove, size));

        try {
            while (itemCountToRemove > 0 && !countsToMarks.isEmpty())
                {
                int nextItems = countsToMarks.removeFirst();
                if (itemCountToRemove >= nextItems)
                    {
                    itemCountToRemove -= nextItems;
                    size -= nextItems;
                    }
                else
                    {
                    countsToMarks.addFirst(nextItems - itemCountToRemove);
                    size -= itemCountToRemove;
                    return;
                    }
                }
            //
            int lastItems = Math.min(unmarkedAtEnd, itemCountToRemove);
            unmarkedAtEnd -= lastItems;
            itemCountToRemove -= lastItems;
            size -= lastItems;
            }
        finally
            {
            verifyInvariants();
            }
        }

    //----------------------------------------------------------------------------------------------
    // Adding
    //----------------------------------------------------------------------------------------------

    public void addUnmarkedItems(int count)
        {
        if (count < 0) throw new IllegalArgumentException(String.format("count must be >= 0: %d", count));
        unmarkedAtEnd += count;
        size += count;
        verifyInvariants();
        }

    public void addMarkedItem()
        {
        size += 1;
        countsToMarks.addLast(unmarkedAtEnd + 1);
        unmarkedAtEnd = 0;
        verifyInvariants();
        }
    }
