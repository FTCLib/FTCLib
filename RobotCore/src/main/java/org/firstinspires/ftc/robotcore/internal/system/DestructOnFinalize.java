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

import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;

import com.qualcomm.robotcore.util.RobotLog;

/**
 * {@link DestructOnFinalize} instances will be (internally) closed on finalization, if they haven't
 * already been closed beforehand. In this way, underlying resources can be guaranteed to be
 * reclaimed.
 *
 * Deterministic closure is facilitated using reference counting. If used, then
 * internal closure happens when the reference count reaches zero. The reference count of a
 * newly constructed object is one. Use of reference counting instead of <em>relying</em>
 * on finalization is <em>highly</em> recommended, though it is optional.
 */
@SuppressWarnings("WeakerAccess")
public abstract class DestructOnFinalize<ParentType extends RefCounted> extends RefCounted implements Finalizable
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected Finalizer     finalizer = Finalizer.forTarget(this);
    protected boolean       inFinalize = false;
    protected ParentType    parent = null;
    protected boolean       ownParentRef = false;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    protected DestructOnFinalize()
        {
        super();
        }

    protected DestructOnFinalize(TraceLevel debugLevel)
        {
        super(debugLevel);
        }

    protected void setParent(@Nullable ParentType newParent)
        {
        synchronized (lock)
            {
            // AddRefs before releases: What if newParent is same as old?
            if (parent != newParent)
                {
                if (this.parent != null)
                    {
                    this.parent.releaseRef();
                    ownParentRef = false;
                    }
                if (newParent != null)
                    {
                    newParent.addRef();
                    ownParentRef = true;
                    }
                this.parent = newParent;
                }
            }
        }

    /**
     * Returns the parent of this object.
     */
    protected ParentType getParent()
        {
        return parent;
        }

    public void doFinalize()
        {
        synchronized (lock)
            {
            inFinalize = true;
            try {
                doLockAndDestruct();
                }
            finally
                {
                inFinalize = false;
                }
            }
        }

    protected void suppressFinalize()
        {
        synchronized (lock)
            {
            if (this.finalizer != null)
                {
                this.finalizer.dispose();
                this.finalizer = null;
                }
            }
        }

    //----------------------------------------------------------------------------------------------
    // Internal Reference Counting
    //----------------------------------------------------------------------------------------------

    protected void preDestructor()
        {
        if (traceDtor() && inFinalize)
            {
            RobotLog.vv(getTag(), "finalize(%s)", getTraceIdentifier());
            }
        suppressFinalize();
        super.preDestructor();
        }

    /** subclasses carry out the bulk of their work here, safely single threaded, and called only once */
    @CallSuper @Override
    protected void destructor()
        {
        if (ownParentRef)
            {
            parent.releaseRef();
            ownParentRef = false;
            }
        super.destructor();
        }

    //----------------------------------------------------------------------------------------------
    // Debugging & tracing
    //----------------------------------------------------------------------------------------------

    protected boolean traceDtor()
        {
        return super.traceDtor() || inFinalize;
        }
    }
