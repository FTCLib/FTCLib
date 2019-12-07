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
package org.firstinspires.ftc.robotcore.internal.network;

import android.os.Handler;
import android.os.Looper;

import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.ThreadPool;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * {@link CallbackLooper} provides a looper thread which is *not* the main app thread.
 * This at times helps us receive and wait for callbacks without causing deadlock.
 */
@SuppressWarnings("WeakerAccess") public class CallbackLooper
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "CallbackLooper";

    // It's useful to have a default, but at times others may want their own, too
    protected static class InstanceHolder
        {
        public static final CallbackLooper theInstance = new CallbackLooper();
        static {
            theInstance.start();
            }
        }
    public static CallbackLooper getDefault() { return InstanceHolder.theInstance; }

    protected final static ThreadLocal<CallbackLooper> tls = new ThreadLocal<>();

    protected ExecutorService   executorService;
    protected Looper            looper;
    protected Handler           handler;
    protected Thread            thread;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public CallbackLooper()
        {
        executorService = null;
        looper = null;
        handler = null;
        thread = null;
        }

    //----------------------------------------------------------------------------------------------
    // Accessing
    //----------------------------------------------------------------------------------------------

    public synchronized void post(Runnable runnable)
        {
        handler.post(runnable);
        }

    public synchronized Looper getLooper()
        {
        return looper;
        }

    public synchronized Handler getHandler()
        {
        return handler;
        }

    /** Is the current thread a looper thread from any CallbackLooper instance? */
    public static boolean isLooperThread()
        {
        return tls.get() != null;
        }

    //----------------------------------------------------------------------------------------------
    // Start / stop
    //----------------------------------------------------------------------------------------------

    public synchronized void start()
        {
        if (executorService == null)
            {
            // Start a new thread so that callbacks happen on some thread that no one else knows about
            // and so don't cause deadlock. This is a good thing.
            executorService = ThreadPool.newSingleThreadExecutor("CallbackLooper");
            final CountDownLatch latch = new CountDownLatch(1);
            executorService.submit(new Runnable()
                {
                @Override public void run()
                    {
                    thread = Thread.currentThread();
                    thread.setName("callback looper");
                    RobotLog.vv(TAG, "thread=%d", thread.getId());
                    //
                    Looper.prepare();
                    looper = Looper.myLooper();
                    handler = new Handler(looper);
                    //
                    tls.set(CallbackLooper.this);
                    //
                    latch.countDown();  // let start() return
                    //
                    Looper.loop();      // doesn't return until we stop()
                    }
                });
            try { latch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        }

    public synchronized void stop()
        {
        if (executorService != null)
            {
            executorService.shutdownNow();
            try { ThreadPool.awaitTermination(executorService, 3, TimeUnit.SECONDS, "CallbackLooper"); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            executorService = null;
            looper = null;
            handler = null;
            thread = null;
            }
        }

    }
