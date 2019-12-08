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

@SuppressWarnings("WeakerAccess")
public class CloseableRefCounted extends RefCounted
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected boolean closeCalled = false;
    protected int closeCount = 0;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    protected CloseableRefCounted()
        {
        super();
        }

    protected CloseableRefCounted(TraceLevel debugLevel)
        {
        super(debugLevel);
        }

    //----------------------------------------------------------------------------------------------
    // Explicit Closeability
    //----------------------------------------------------------------------------------------------

    /**
     * A check that the <em>only</em> references on us are owned by the responsibility to
     * call close(). Note that this only works in the constructor().
     */
    protected final boolean ctorOnlyCloseNeededToDestruct()
        {
        synchronized (lock)
            {
            return closeCount==1 && refCount.get()==1; // Y2
            }
        }

    /**
     * Enable external close support on this object. You can think about enableExternalClose()
     * and close() as much like a different kind of addRef() / release(), only spelled and
     * pronounced differently.
     */
    protected final void enableClose()
        {
        synchronized (lock)
            {
            if (closeCalled)
                {
                throw new IllegalStateException("enableClose() on an already closed object: " + this);
                }
            else if (closeCount++ == 0)
                {
                addRef();   // ref: Y2
                }
            }
        }

    /** to be called only in ctor */
    protected final void enableOnlyClose()
        {
        enableClose();                      // to honor semantics of the Camera interface & Camera.StateCallback.onOpened
        releaseRef();                       // removes original creation ref so that *only* close() is needed
        Assert.assertTrue(ctorOnlyCloseNeededToDestruct());
        }

    public final void close()
        {
        synchronized (lock)
            {
            if (closeCount == 0)
                {
                // close() is idempotent; ignore
                }
            else
                {
                if (--closeCount == 0)
                    {
                    closeCalled = true;
                    preClose();
                    doClose();
                    postClose();
                    releaseRef();   // ref: Y2
                    }
                }
            }
        }

    @CallSuper protected void preClose()
        {
        }

    /** Do whatever work should be done when close is called */
    @CallSuper protected void doClose()
        {
        // subclass hook
        }

    @CallSuper protected void postClose()
        {
        }
    }
