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
package org.firstinspires.ftc.robotcore.internal.opmode;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.robocol.TelemetryMessage;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.Func;
import org.firstinspires.ftc.robotcore.external.Predicate;
import org.firstinspires.ftc.robotcore.external.Telemetry;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * {@link TelemetryImpl} is the system-provided implementation of the {@link Telemetry} interface.
 */
public class TelemetryImpl implements Telemetry, TelemetryInternal
    {
    //----------------------------------------------------------------------------------------------
    // Types
    //----------------------------------------------------------------------------------------------

    /** A {@link Value} is what gets displayed to the right of the captions on the driver station */
    protected class Value<T>
        {
        //------------------------------------------------------------------------------------------
        // State
        //------------------------------------------------------------------------------------------

        protected String        format          = null;
        protected Object[]      formatArgs      = null;
        protected Object        value           = null;
        protected Func<T>       valueProducer   = null;
        protected String        composed        = null;

        //------------------------------------------------------------------------------------------
        // Construction
        //------------------------------------------------------------------------------------------

        Value(String format, Object... formatArgs)
            {
            this.format = format;
            this.formatArgs = formatArgs;
            }

        Value(String format, Func<T> valueProducer)
            {
            this.format = format;
            this.valueProducer = valueProducer;
            }

        Value(Object value)
            {
            this.value = value;
            }

        Value(Func<T> valueProducer)
            {
            this.valueProducer = valueProducer;
            }

        //------------------------------------------------------------------------------------------
        // Operations
        //------------------------------------------------------------------------------------------

        boolean isProducer()
            {
            return this.valueProducer != null;
            }

        @NonNull String getComposed(boolean recompose)
            {
            if (recompose || composed==null)
                {
                composed = compose();
                }
            return composed;
            }

        protected String compose()
            {
            if (format != null)
                {
                if (this.formatArgs != null)      return String.format(this.format, this.formatArgs);
                if (this.valueProducer != null)   return String.format(this.format, this.valueProducer.value());
                }
            else
                {
                if (this.value != null)          return this.value.toString();
                if (this.valueProducer != null)  return this.valueProducer.value().toString();
                }

            return "";
            }
        }

    protected interface Lineable
        {
        String getComposed(boolean recompose);
        }

    protected class LineableContainer implements Iterable<Lineable>
        {
        private ArrayList<Lineable> list = new ArrayList<Lineable>();

        void boundedAddToList(int index, Lineable data)
            {
            // Our aim here is merely to prevent *unbounded* growth. Even with the limits here
            // imposed, when actual transmission ultimately occurs, failure is possible/likely
            // due to limits of the transmission mechanism. That's fine with us for now: we just
            // want to avoid eating unbounded amounts of memory should users forget to call
            // update() frequently enough.

            // Using the max # of data items that can be actually transmitted, ever, seems like
            // a practical choice as an upper bound.
            if (list.size() < TelemetryMessage.cCountMax)
                {
                list.add(index, data);
                }
            }

        @Override public Iterator<Lineable> iterator()
            {
            synchronized (theLock)
                {
                return list.iterator();
                }
            }

        Line addLineAfter(Lineable prev, String lineCaption)
            {
            synchronized (theLock)
                {
                onAddData();

                LineImpl result = new LineImpl(lineCaption, this);
                int index = prev==null ? list.size() : list.indexOf(prev) + 1;  // nb: if prev is absent, index will be zero, which is as good as any other choice
                boundedAddToList(index, result);
                return result;
                }
            }

        Item addItemAfter(Lineable prev, String caption, Value value)
            {
            synchronized (theLock)
                {
                onAddData();

                ItemImpl result = new ItemImpl(this, caption, value);
                int index = prev==null ? list.size() : list.indexOf(prev) + 1; // nb: if prev is absent, index will be zero, which is as good as any other choice
                boundedAddToList(index, result);
                return result;
                }
            }

        boolean isEmpty()
            {
            synchronized (theLock)
                {
                return list.isEmpty();
                }
            }

        int size()
            {
            synchronized (theLock)
                {
                return list.size();
                }
            }

        boolean remove(Lineable lineable)
            {
            synchronized (theLock)
                {
                for (int i = 0; i < list.size(); i++)
                    {
                    if (list.get(i)==lineable)
                        {
                        list.remove(i);
                        return true;
                        }
                    }
                return false;
                }
            }

        boolean removeAllRecurse(Predicate<ItemImpl> predicate)
            {
            synchronized (theLock)
                {
                boolean result = false;
                for (int i = 0; i < list.size(); )
                    {
                    Lineable cur = list.get(i);
                    if (cur instanceof LineImpl)
                        {
                        LineImpl line = (LineImpl)cur;
                        line.lineables.removeAllRecurse(predicate);

                        // Remove the line itself if it's empty
                        if (line.lineables.isEmpty())
                            {
                            list.remove(i);
                            result = true;
                            }
                        else
                            i++;
                        }
                    else if (cur instanceof ItemImpl)
                        {
                        if (predicate.test((ItemImpl)cur))
                            {
                            list.remove(i);
                            result = true;
                            }
                        else
                            i++;
                        }
                    else
                        i++;
                    }
                return result;
                }
            }
        }

    protected class ItemImpl implements Item, Lineable
        {
        //------------------------------------------------------------------------------------------
        // State
        //------------------------------------------------------------------------------------------

        final LineableContainer parent;
        String  caption  = null;
        Value   value    = null;
        Boolean retained = null;

        //------------------------------------------------------------------------------------------
        // Construction
        //------------------------------------------------------------------------------------------

        ItemImpl(LineableContainer parent, String caption, Value value)
            {
            this.parent = parent;
            this.caption = caption;
            this.value = value;
            this.retained = null;
            }

        //------------------------------------------------------------------------------------------
        // Operations
        //------------------------------------------------------------------------------------------

        @Override public String getComposed(boolean recompose)
            {
            synchronized (theLock)
                {
                return String.format("%s%s%s", this.caption, getCaptionValueSeparator(), this.value.getComposed(recompose));
                }
            }

        @Override public String getCaption()
            {
            return this.caption;
            }

        @Override public Item setCaption(String caption)
            {
            this.caption = caption;
            return this;
            }

        @Override public boolean isRetained()
            {
            synchronized (theLock)
                {
                return this.retained != null
                        ? this.retained
                        : this.isProducer();
                }
            }

        @Override public Item setRetained(@Nullable Boolean retained)
            {
            synchronized (theLock)
                {
                this.retained = retained;
                return this;
                }
            }

        boolean isProducer()
            {
            synchronized (theLock)
                {
                return this.value.isProducer();
                }
            }

        void internalSetValue(Value value)
            {
            synchronized (theLock)
                {
                this.value = value;
                }
            }

        @Override public Item setValue(String format, Object... args)
            {
            internalSetValue(new Value(format, args));
            return this;
            }

        @Override public Item setValue(Object value)
            {
            internalSetValue(new Value(value));
            return this;
            }

        @Override public <T> Item setValue(Func<T> valueProducer)
            {
            internalSetValue(new Value<T>(valueProducer));
            return this;
            }

        @Override public <T> Item setValue(String format, Func<T> valueProducer)
            {
            internalSetValue(new Value<T>(format, valueProducer));
            return this;
            }

        @Override public Item addData(String caption, String format, Object... args)
            {
            return parent.addItemAfter(this, caption, new Value(format, args));
            }

        @Override public Item addData(String caption, Object value)
            {
            return parent.addItemAfter(this, caption, new Value(value));
            }

        @Override public <T> Item addData(String caption, Func<T> valueProducer)
            {
            return parent.addItemAfter(this, caption, new Value<T>(valueProducer));
            }

        @Override public <T> Item addData(String caption, String format, Func<T> valueProducer)
            {
            return parent.addItemAfter(this, caption, new Value<T>(format, valueProducer));
            }
        }

    protected class LineImpl implements Line, Lineable
        {
        //------------------------------------------------------------------------------------------
        // State
        //------------------------------------------------------------------------------------------

        final LineableContainer parent;
              String            lineCaption;
              LineableContainer lineables;

        //------------------------------------------------------------------------------------------
        // Construction
        //------------------------------------------------------------------------------------------

        LineImpl(String lineCaption, LineableContainer parent)
            {
            this.parent = parent;
            this.lineCaption = lineCaption;
            this.lineables = new LineableContainer();
            }

        //------------------------------------------------------------------------------------------
        // Operations
        //------------------------------------------------------------------------------------------

        @Override public String getComposed(boolean recompose)
            {
            StringBuilder result = new StringBuilder();
            result.append(this.lineCaption);
            boolean firstTime = true;
            for (Lineable lineable : lineables)
                {
                if (!firstTime)
                    {
                    result.append(getItemSeparator());
                    }
                result.append(lineable.getComposed(recompose));
                firstTime = false;
                }
            return result.toString();
            }

        @Override public Item addData(String caption, String format, Object... args)
            {
            return lineables.addItemAfter(null, caption, new Value(format, args));
            }

        @Override public Item addData(String caption, Object value)
            {
            return lineables.addItemAfter(null, caption, new Value(value));
            }

        @Override public <T> Item addData(String caption, Func<T> valueProducer)
            {
            return lineables.addItemAfter(null, caption, new Value<T>(valueProducer));
            }

        @Override public <T> Item addData(String caption, String format, Func<T> valueProducer)
            {
            return lineables.addItemAfter(null, caption, new Value<T>(format, valueProducer));
            }
        }

    protected class LogImpl implements Log
        {
        //------------------------------------------------------------------------------------------
        // State
        //------------------------------------------------------------------------------------------
        List<String> entries;
        int          capacity;
        DisplayOrder displayOrder;
        boolean      isDirty;

        //------------------------------------------------------------------------------------------
        // Construction
        //------------------------------------------------------------------------------------------

        LogImpl()
            {
            reset();
            }

        //------------------------------------------------------------------------------------------
        // Accessors
        //------------------------------------------------------------------------------------------

        void markDirty()
            {
            this.isDirty = true;
            }

        void markClean()
            {
            this.isDirty = false;
            }

        boolean isDirty()
            {
            return this.isDirty;
            }

        //------------------------------------------------------------------------------------------
        // Internal Operations
        //------------------------------------------------------------------------------------------

        // Use the outer class to mindlessly avoid any potential deadlocks
        Object getLock()
            {
            return TelemetryImpl.this;
            }

        int size()
            {
            return entries.size();
            }

        String get(int index)
            {
            return entries.get(index);
            }

        void prune()
            {
            synchronized (getLock())
                {
                while (this.entries.size() > this.capacity && this.entries.size() > 0)
                    {
                    this.entries.remove(0);
                    }
                }
            }

        void reset()
            {
            this.entries    = new ArrayList<>();
            this.capacity   = 9;
            this.isDirty    = false;
            this.displayOrder = DisplayOrder.OLDEST_FIRST;
            }

        //------------------------------------------------------------------------------------------
        // Public Operations
        //------------------------------------------------------------------------------------------

        @Override public int getCapacity()
            {
            return this.capacity;
            }

        @Override public void setCapacity(int capacity)
            {
            synchronized (getLock())
                {
                this.capacity = capacity;
                prune();
                }
            }

        @Override public DisplayOrder getDisplayOrder()
            {
            return this.displayOrder;
            }

        @Override public void setDisplayOrder(DisplayOrder displayOrder)
            {
            synchronized (getLock())
                {
                this.displayOrder = displayOrder;
                }
            }

        @Override public void add(String format, Object... args)
            {
            synchronized (getLock())
                {
                String datum = String.format(format, args);
                this.entries.add(datum);
                this.markDirty();
                this.prune();

                // Maybe provoke an update
                tryUpdate(UpdateReason.LOG);
                }
            }

        @Override public void add(String entry)
            {
            this.add("%s", entry);
            }

        @Override public void clear()
            {
            synchronized (getLock())
                {
                this.entries.clear();
                this.markDirty();
                }
            }
        }

    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected final Object theLock = new Object();
    protected LineableContainer   lines;
    protected List<String>        composedLines;
    protected List<Runnable>      actions;
    protected LogImpl             log;
    protected ElapsedTime         transmissionTimer;
    protected boolean             isDirty;
    protected boolean             clearOnAdd;
    protected OpMode              opMode;
    protected boolean             isAutoClear;
    protected int                 msTransmissionInterval;
    protected String              captionValueSeparator;
    protected String              itemSeparator;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public TelemetryImpl(OpMode opMode)
        {
        this.opMode = opMode;
        this.log = new LogImpl();
        resetTelemetryForOpMode();
        }

    @Override
    public void resetTelemetryForOpMode()
        {
        this.lines   = new LineableContainer();
        this.composedLines = new ArrayList<String>();
        this.actions = new LinkedList<Runnable>();
        log.reset(); // Reuse the log instance in case the user stores a reference to it
        this.transmissionTimer = new ElapsedTime();
        this.isDirty     = false;
        this.clearOnAdd  = false;
        this.isAutoClear = true;
        this.msTransmissionInterval = 250;
        this.captionValueSeparator  = " : ";
        this.itemSeparator          = " | ";
        }

    //----------------------------------------------------------------------------------------------
    // Accessors
    //----------------------------------------------------------------------------------------------

    void markDirty()
        {
        this.isDirty = true;
        }

    void markClean()
        {
        this.isDirty = false;
        }

    boolean isDirty()
        {
        return this.isDirty;
        }

    //----------------------------------------------------------------------------------------------
    // Updating
    //----------------------------------------------------------------------------------------------

    protected static String getKey(int iLine)
        {
        // Keys must be unique. If they start with nul, then they're not shown on the driver display.
        // Historically, they were always shown, and sorted, so we used an *increasing sequence*
        // of unrenderable strings.
        return String.format("\0%c", 0x180 + iLine);
        }

    @Override public boolean update()
        {
        return tryUpdate(UpdateReason.USER);
        }

    @Override public boolean tryUpdateIfDirty()
        {
        return tryUpdate(UpdateReason.IFDIRTY);
        }

    protected enum UpdateReason { USER, LOG, IFDIRTY }

    protected boolean tryUpdate(UpdateReason updateReason)
        {
        synchronized (theLock)
            {
            boolean result = false;

            boolean intervalElapsed = this.transmissionTimer.milliseconds() > msTransmissionInterval;

            boolean wantToTransmit  = updateReason==UpdateReason.USER
                                        || updateReason==UpdateReason.LOG
                                        || (updateReason==UpdateReason.IFDIRTY && (isDirty() || log.isDirty()));

            boolean recompose = updateReason==UpdateReason.USER
                                        || isDirty();         // only way we get dirty is from a previous UpdateReason.USER

            if (intervalElapsed && wantToTransmit)
                {
                // Evaluate any delayed actions we've been asked to do
                for (Runnable action : this.actions)
                    {
                    action.run();
                    }

                // Build an object to cary our telemetry data
                TelemetryMessage transmitter = new TelemetryMessage();
                this.saveToTransmitter(recompose, transmitter);

                // Transmit if there's anything to transmit
                if (transmitter.hasData())
                    {
                    OpModeManagerImpl.updateTelemetryNow(this.opMode, transmitter);
                    }

                // We've definitely got nothing lingering to transmit
                this.log.markClean();
                markClean();

                // Update for the next time around
                this.transmissionTimer.reset();
                result = true;
                }
            else if (updateReason==UpdateReason.USER)
                {
                // Next time we get an IFDIRTY update, we'll try again. Note that the log has its
                // own independent dirty status; thus if *it* is dirty, then an IFDIRTY update will
                // automatically try again.
                this.markDirty();
                }

            // In all cases, if it's a user requesting the update, then the next add clears
            if (updateReason==UpdateReason.USER)
                {
                // Postponing the clear vs doing it right now allows future log updates to
                // transmit before the user does more addData()
                this.clearOnAdd = isAutoClear();
                }

            return result;
            }
        }

    protected void saveToTransmitter(boolean recompose, TelemetryMessage transmitter)
        {
        transmitter.setSorted(false);

        // When we recompose, we save the composed lines. Thus, they will stick around
        // even after we might get clear()'d. In that way, they'll still be there to
        // transmit if a log() write should happen to occur after the clear() but before
        // a subsequent user update().
        if (recompose)
            {
            this.composedLines = new ArrayList<String>();
            for (Lineable lineable : this.lines)
                {
                this.composedLines.add(lineable.getComposed(recompose));
                }
            }

        // Add in the composed lines
        int iLine = 0;
        for (iLine = 0; iLine < this.composedLines.size(); iLine++)
            {
            transmitter.addData(getKey(iLine), this.composedLines.get(iLine));
            }

        // Add in the log
        int size = this.log.size();
        for (int i = 0; i < size; i++)
            {
            String s = this.log.getDisplayOrder()==Log.DisplayOrder.OLDEST_FIRST
                    ? this.log.get(i)
                    : this.log.get(size-1 -i);
            transmitter.addData(getKey(iLine), s);
            iLine++;
            }
        }

    //----------------------------------------------------------------------------------------------
    // Accessors
    //----------------------------------------------------------------------------------------------

    @Override public Log log()
        {
        return this.log;
        }

    @Override public boolean isAutoClear()
        {
        return this.isAutoClear;
        }

    @Override public void setAutoClear(boolean autoClear)
        {
        synchronized (theLock)
            {
            this.isAutoClear = autoClear;
            }
        }

    @Override public int getMsTransmissionInterval()
        {
        return this.msTransmissionInterval;
        }

    @Override public void setMsTransmissionInterval(int msTransmissionInterval)
        {
        synchronized (theLock)
            {
            this.msTransmissionInterval = msTransmissionInterval;
            }
        }

    @Override public String getItemSeparator()
        {
        return this.itemSeparator;
        }

    @Override public void setItemSeparator(String itemSeparator)
        {
        synchronized (theLock)
            {
            this.itemSeparator = itemSeparator;
            }
        }

    @Override public String getCaptionValueSeparator()
        {
        return this.captionValueSeparator;
        }

    @Override public void setCaptionValueSeparator(String captionValueSeparator)
        {
        synchronized (theLock)
            {
            this.captionValueSeparator = captionValueSeparator;
            }
        }

    //----------------------------------------------------------------------------------------------
    // Adding and removing data
    //----------------------------------------------------------------------------------------------

    @Override public Object addAction(Runnable action)
        {
        synchronized (theLock)
            {
            this.actions.add(action);
            return action;
            }
        }

    @Override public boolean removeAction(Object token)
        {
        synchronized (theLock)
            {
            return this.actions.remove((Runnable)token);
            }
        }

    @Override public Item addData(String caption, String format, Object... args)
        {
        return this.lines.addItemAfter(null, caption, new Value(format, args));
        }
    @Override public Item addData(String caption, Object value)
        {
        return this.lines.addItemAfter(null, caption, new Value(value));
        }
    @Override public <T> Item addData(String caption, Func<T> valueProducer)
        {
        return this.lines.addItemAfter(null, caption, new Value<T>(valueProducer));
        }
    @Override public <T> Item addData(String caption, String format, Func<T> valueProducer)
        {
        return this.lines.addItemAfter(null, caption, new Value<T>(format, valueProducer));
        }

    @Override public Line addLine()
        {
        return this.lines.addLineAfter(null, "");
        }
    @Override public Line addLine(String lineCaption)
        {
        return this.lines.addLineAfter(null, lineCaption);
        }

    @Override public boolean removeItem(Item item)
        {
        if (item instanceof ItemImpl)
            {
            ItemImpl itemImpl = (ItemImpl)item;
            return itemImpl.parent.remove(itemImpl);
            }
        return false;
        }

    @Override public boolean removeLine(Line line)
        {
        if (line instanceof LineImpl)
            {
            LineImpl lineImpl = (LineImpl)line;
            return lineImpl.parent.remove(lineImpl);
            }
        return false;
        }

    protected void onAddData()
        {
        if (this.clearOnAdd)
            {
            clear();
            this.clearOnAdd = false;
            }

        // We no longer have anything to dirty-transmit when the timer expires
        markClean();
        }

    @Override public void clear()
        {
        synchronized (theLock)
            {
            this.clearOnAdd = false;
            markClean();
            //
            this.lines.removeAllRecurse(new Predicate<ItemImpl>()
                {
                @Override public boolean test(ItemImpl item)
                    {
                    return !item.isRetained();
                    }
                });
            }
        }

    @Override public void clearAll()
        {
        synchronized (theLock)
            {
            this.clearOnAdd = false;
            markClean();
            //
            this.actions.clear();
            this.lines.removeAllRecurse(new Predicate<ItemImpl>()
                {
                @Override public boolean test(ItemImpl item)
                    {
                    return true;
                    }
                });
            }
        }
    }