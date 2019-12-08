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

import androidx.annotation.NonNull;

import java.util.Stack;

/**----------------------------------------------------------------------------------------------
// Finalization
//
// *Most* of the time finalization isn't going to be necessary, as we manually close using
// reference counting. In .Net, we'd call GC.suppressFinalize() when that happens, but here
// in Java we don't have that mechanism. Yet we want to minimize the number of finalization
// calls that happen when they don't actually need to, as finalizable objects are significantly
// more expensive than non-finalizable ones. Hence, the mechanism below.
//----------------------------------------------------------------------------------------------*/
@SuppressWarnings("WeakerAccess")
public class Finalizer
    {
    public static String getTag() { return Finalizer.class.getSimpleName(); }

    Finalizable target;

    static int cacheSizeMax = 50;   // a moderate estimate based on some limited perf analysis
    final static Stack<Finalizer> cache = new Stack<>();

    static Finalizer forTarget(@NonNull Finalizable target)
        {
        synchronized (cache)
            {
            Finalizer result = cache.isEmpty() ?  new Finalizer() : cache.pop();
            result.target = target;
            return result;
            }
        }

    public void dispose()
        {
        synchronized (cache)
            {
            if (target != null) // guard against double-dispose
                {
                target = null;
                if (cache.size() < cacheSizeMax)
                    {
                    // reincarnate the object
                    cache.push(this);
                    }
                }
            }
        }

    @Override protected void finalize() throws Throwable
        {
        Finalizable target = this.target;
        if (target != null) // paranoia
            {
            target.doFinalize();
            }
        dispose();
        super.finalize();
        }
    }
