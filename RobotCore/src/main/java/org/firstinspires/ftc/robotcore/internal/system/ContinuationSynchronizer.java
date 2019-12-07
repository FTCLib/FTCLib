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

import android.text.TextUtils;

import org.firstinspires.ftc.robotcore.external.function.ThrowingRunnable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("WeakerAccess")
public class ContinuationSynchronizer<T>
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static String TAG = "ContinuationSynchronizer";
    protected Tracer tracer;

    protected boolean enableTrace = false;
    protected final Object lock = new Object();
    protected final Deadline deadline;
    protected final CountDownLatch latch;
    protected boolean isFinished;
    protected T value;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public ContinuationSynchronizer(long duration, TimeUnit unit, boolean enableTrace)
        {
        this(new Deadline(duration, unit), enableTrace);
        }
    public ContinuationSynchronizer(long duration, TimeUnit unit, boolean enableTrace, T initialValue)
        {
        this(new Deadline(duration, unit), enableTrace, initialValue);
        }
    public ContinuationSynchronizer(Deadline deadline, boolean enableTrace)
        {
        this(deadline, enableTrace, null);
        }
    public ContinuationSynchronizer(Deadline deadline, boolean enableTrace, T initialValue)
        {
        tracer = Tracer.create(TAG, enableTrace);
        this.deadline = deadline;
        latch = new CountDownLatch(1);
        isFinished = false;
        value = initialValue;
        this.enableTrace = enableTrace;
        }

    //----------------------------------------------------------------------------------------------
    // Access
    //----------------------------------------------------------------------------------------------

    public T getValue()
        {
        return value;
        }

    public Deadline getDeadline()
        {
        return deadline;
        }


    public boolean isFinished()
        {
        return isFinished;
        }

    public boolean isSuccessful()
        {
        return value != null;
        }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    public void finish(final T value)
        {
        finish("", value);
        }

    public void finish(String reason, final T value)
        {
        String reasonPart = TextUtils.isEmpty(reason) ? "" : ("\"" + reason + "\": ");
        tracer.trace("finish(" + reasonPart + value + ")", new Runnable()
            {
            @Override
            public void run()
                {
                synchronized (lock)
                    {
                    if (isFinished)
                        {
                        if (ContinuationSynchronizer.this.value == null)
                            {
                            ContinuationSynchronizer.this.value = value;
                            }
                        }
                    else
                        {
                        ContinuationSynchronizer.this.value = value;
                        }
                    isFinished = true;
                    deadline.expire();
                    latch.countDown();
                    }
                }
            });
        }

    public void await() throws InterruptedException
        {
        if (!deadline.await(latch))
            {
            tracer.traceError("deadline expired during await()");
            }
        }

    public void await(String message) throws InterruptedException
        {
        tracer.trace(tracer.format("awaiting(%s)", message), new ThrowingRunnable<InterruptedException>()
            {
            @Override public void run() throws InterruptedException
                {
                if (!deadline.await(latch))
                    {
                    tracer.traceError("deadline expired during await()");
                    }
                }
            });
        }
    }
