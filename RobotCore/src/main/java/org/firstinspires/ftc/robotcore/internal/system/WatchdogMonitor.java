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

import android.support.annotation.NonNull;

import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.ThreadPool;

import org.firstinspires.ftc.robotcore.internal.opmode.OpModeManagerImpl;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * {@link WatchdogMonitor} is a simple class that will fire an action after a certain interval
 * if not cancelled first. We use it rather than ThreadPool.getDefault().schedule() (for which
 * we are a drop in replacement in limited contexts) because the latter is horrendously poor
 * at thread management, especially in KitKat.
 *
 * This is similar to {@link OpModeManagerImpl.OpModeStuckCodeMonitor}
 * and probably could be merged therewith.
 */
@SuppressWarnings("WeakerAccess")
public class WatchdogMonitor
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "WatchdogMonitor";

    protected       ExecutorService         executorService      = ThreadPool.newSingleThreadExecutor("WatchdogMonitor");
    protected       Runner                  runner               = new Runner();
    protected       Thread                  monitoredThread      = null;
    protected final Object                  startStopLock        = new Object();

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public void close(boolean awaitTermination)
        {
        synchronized (startStopLock)
            {
            if (executorService != null)
                {
                if (awaitTermination)
                    {
                    executorService.shutdownNow();
                    ThreadPool.awaitTerminationOrExitApplication(executorService, 1, TimeUnit.SECONDS, "WatchdogMonitor", "internal error");
                    }
                else
                    {
                    executorService.shutdown();
                    }
                executorService = null;
                }
            }
        }

    //----------------------------------------------------------------------------------------------
    // Simple usage
    //----------------------------------------------------------------------------------------------

    /**
     * Executes an action requiring monitoring on the current thread. Simultaneously, on a different
     * thread, schedules a monitoring action to run after a timeout. The two actions, of course, will
     * race.
     */
    public <V> V monitor(Callable<V> actionToMonitor, Callable<V> actionOnTimeout, long timeout, TimeUnit unit) throws ExecutionException, InterruptedException
        {
        monitoredThread = Thread.currentThread();
        Future<V> future = schedule(actionOnTimeout, timeout, unit);
        V result = null;
        try {
            result = actionToMonitor.call();
            }
        catch (Exception e)
            {
            throw new ExecutionException("exception while monitoring", e);
            }
        finally
            {
            if (future.cancel(false))
                {
                // cancelled ok
                }
            else
                {
                // could not cancel
                result = future.get();
                }
            monitoredThread = null;
            }
        return result;
        }

    public Thread getMonitoredThread()
        {
        return monitoredThread;
        }

    //----------------------------------------------------------------------------------------------
    // More complex usage
    //----------------------------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    protected <V> Future<V> schedule(Callable<V> callable, long timeout, TimeUnit unit)
        {
        // Wait for any previous monitoring to drain
        try { runner.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        //
        runner.initialize(callable, unit.toMillis(timeout));
        try {
            executorService.submit(runner);
            }
        catch (RuntimeException e) // RejectedExecutionException in particular may be thrown
            {
            RobotLog.ee(TAG, e, "executorService.submit() failed");
            runner.noteRunComplete();
            }
        return runner;
        }

    protected class Runner<V> implements Runnable, Future<V>
        {
        final ReusableCountDownLatch  runComplete          = new ReusableCountDownLatch(0);
        final ReusableCountDownLatch  cancelInterlock      = new ReusableCountDownLatch(0);
        final ReusableCountDownLatch  isCancelledAvailable = new ReusableCountDownLatch(0);

        Callable<V>          callable;
        long                 msTimeout;
        V                    callableResult;
        ExecutionException   executionException;
        boolean              isCancelled;
        boolean              done;

        public void initialize(Callable<V> callable, long msTimeout)
            {
            this.callable = callable;
            this.msTimeout = msTimeout;
            runComplete.reset(1);
            cancelInterlock.reset(1);
            isCancelledAvailable.reset(1);

            callableResult = null;
            executionException = null;
            isCancelled = false;
            done = false;
            }

        protected void noteRunComplete()
            {
            isCancelledAvailable.countDown(); // paranoia: make *certain* cancel is available; cancel will be false unless we actually got a cancel
            done = true;
            runComplete.countDown();
            }

        public void await() throws InterruptedException
            {
            runComplete.await();
            }

        public void await(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException
            {
            if (!runComplete.await(timeout, unit))
                {
                throw new TimeoutException("timeout awaiting watchdog timer");
                }
            }

        @Override public boolean cancel(boolean mayInterruptIfRunning)
            {
            // Wake up the run() to one result or the other. Note that countDown() here is
            // idempotent, since we only gave it a count of one to begin with.
            cancelInterlock.countDown();

            // Wait for the runner to wake up and set the result
            try { isCancelledAvailable.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

            // Return the result
            return isCancelled;
            }

        @Override public boolean isCancelled()
            {
            // Meaningless if called before isDone()?
            return isCancelled;
            }

        @Override public void run()
            {
            try {
                if (cancelInterlock.await(msTimeout, TimeUnit.MILLISECONDS))
                    {
                    // We cancelled before we timed out
                    isCancelled = true;
                    isCancelledAvailable.countDown();
                    }
                else
                    {
                    // Timeout hit before cancel
                    isCancelled = false;
                    isCancelledAvailable.countDown();

                    try {
                        callableResult = callable.call();
                        }
                    catch (Exception e)
                        {
                        executionException = new ExecutionException("exception during watchdog timer", e);
                        }
                    }
                }
            catch (InterruptedException e)
                {
                // Ignore: we're cleaning up here pronto anyway
                }
            finally
                {
                noteRunComplete();
                }
            }

        @Override public boolean isDone()
            {
            return done;
            }

        @Override public V get() throws InterruptedException, ExecutionException
            {
            runner.await();
            if (runner.executionException != null)
                {
                throw runner.executionException;
                }
            return callableResult;
            }

        @Override public V get(long timeout, @NonNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
            {
            runner.await(timeout, unit);
            if (runner.executionException != null)
                {
                throw runner.executionException;
                }
            return callableResult;
            }
        }
    }
