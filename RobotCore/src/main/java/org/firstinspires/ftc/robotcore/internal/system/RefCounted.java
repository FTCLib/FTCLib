/*
Copyright (c) 2018 Robert Atkinson

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

import androidx.annotation.CallSuper;

import com.qualcomm.robotcore.util.RobotLog;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link RefCounted} is the flagship of a suite of base classes that includes {@link Closeable},
 * {@link CloseableDestructOnFinalize}, {@link CloseableRefCounted}, {@link CloseableOnFinalize},
 * and {@link DestructOnFinalize}. These provide variations on mixtures of a few core ideas:
 *
 * 1. The idea of reference counting. Ref counting provides for <em>deterministic destruction</em>
 *    in an environment where an object is shared amongst clients. Deterministic destruction can
 *    be contrasted with the non-deterministic finalization provided by the system garbage collector.
 *    Finalization is great for the Java-only world, but when the Java objects are fronts for resources
 *    on the outside (think files that need to be closed when done, or video cameras that need to
 *    have streaming stopped), it's important to be able deterministically know when the external
 *    resource is reclaimed. Ref counting allows this decision to be made in a collective, shared
 *    manner.
 * 2. The idea of 'close'ing an object. Closing also provides deterministic destruction, but in a
 *    a non-shared manner: the guy who calls close better know somehow that no one else is using
 *    the object at the time. For many situations, this is entirely reasonable. It is important to
 *    note that the close() method is <em>always</em> idempotent: calling close() more than once
 *    has no additional effect beyond calling it the first time (constrast with reference counting).
 * 3. The idea of finalization. While deterministic destruction is all well and good, it is the case
 *    that the garbage collector will, if we ask it nicely, allow our code to run when the collector
 *    has perceived that the object is no longer reachable in the Java space. We use that here as a
 *    safety net: if somehow a the reference counting on an object is messed up, or close() forgets
 *    to be called, the finalization will (some seconds later) at least allow underlying resources
 *    to <em>eventually</em> be cleaned up rather than being lost forever.
 */
@SuppressWarnings("WeakerAccess")
public class RefCounted
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public String getTag() { return this.getClass().getSimpleName(); }

    protected AtomicInteger refCount = new AtomicInteger(1); // Note: initial count of one
    protected final Object  lock = new Object();
    protected TraceLevel traceLevel;

    protected boolean destroyed = false;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    protected RefCounted()
        {
        this(defaultTraceLevel);
        }

    protected RefCounted(TraceLevel traceLevel)
        {
        this.traceLevel = traceLevel;
        if (traceCtor()) RobotLog.vv(getTag(), "construct(0x%08x)", hashCode());
        }

    //----------------------------------------------------------------------------------------------
    // Internal Reference Counting
    //----------------------------------------------------------------------------------------------

    public void addRef()
        {
        int after = refCount.incrementAndGet();
        if (traceRefCount())
            {
            doTraceRefCnt(Misc.formatInvariant("ref:add(after=%d)", after));
            }
        }

    public int releaseRef()
        {
        int remaining = refCount.decrementAndGet();
        if (traceRefCount())
            {
            doTraceRefCnt(Misc.formatInvariant("ref:release(after=%d)", remaining));
            }
        if (remaining == 0)
            {
            doLockAndDestruct();
            }
        return remaining;
        }

    /** Warning: actually carrying out the traces can be VERY expensive, due to the
     * need to create stack traces. Do not use in production code. */
    protected void doTraceRefCnt(String action)
        {
        String log = AppUtil.getInstance().findCaller(Misc.formatInvariant("%s(%s)", action, getTraceIdentifier()), 1);
        RobotLog.vv(getTag(), log);
        }

    public String getTraceIdentifier()
        {
        return Misc.formatInvariant("hash=0x%08x", hashCode());
        }

    protected final void doLockAndDestruct()
        {
        synchronized (lock)
            {
            if (!destroyed)  // guard against effects of ref counting after closure
                {
                destroyed = true;
                preDestructor();
                if (traceDtor()) RobotLog.vv(getTag(), "destroy(%s)", getTraceIdentifier());
                destructor();
                postDestructor();
                }
            }
        }


    @CallSuper protected void preDestructor()
        {
        }

    /** subclasses carry out the bulk of their work here, safely single threaded, and called only once */
    @CallSuper protected void destructor()
        {
        }

    @CallSuper protected void postDestructor()
        {
        }

    //----------------------------------------------------------------------------------------------
    // Debugging & tracing (this could use more thought, really)
    //----------------------------------------------------------------------------------------------

    public static class TraceLevel
        {
        public static final TraceLevel Normal = new TraceLevel(10);
        public static final TraceLevel Verbose = new TraceLevel(20);
        public static final TraceLevel VeryVerbose = new TraceLevel(30);
        public static final TraceLevel None = new TraceLevel(Integer.MAX_VALUE);

        public final int value;
        public final boolean traceRefCount;

        public TraceLevel(int value)
            {
            this(value, false);
            }
        public TraceLevel(int value, boolean traceRefCount)
            {
            this.value = value;
            this.traceRefCount = traceRefCount;
            }
        public TraceLevel traceRefCnt()
            {
            return new TraceLevel(this.value, true);
            }
        }

    public static TraceLevel currentTraceLevel = TraceLevel.Normal;
    public static TraceLevel defaultTraceLevel = TraceLevel.Normal;
    public static boolean traceCtor = true;
    public static boolean traceDtor = true;
    public static boolean traceRefCount = true;

    protected boolean isTraceArmed()
        {
        return traceLevel.value <= currentTraceLevel.value && currentTraceLevel.value != TraceLevel.None.value;
        }
    protected boolean traceCtor()
        {
        return traceCtor && isTraceArmed();
        }
    protected boolean traceDtor()
        {
        return (traceDtor && isTraceArmed());
        }
    protected boolean traceRefCount()
        {
        return traceRefCount && isTraceArmed() && traceLevel.traceRefCount;
        }

    }
