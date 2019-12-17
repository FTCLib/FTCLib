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

package com.qualcomm.robotcore.util;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Debug;
import android.os.Handler;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.LongSparseArray;

import org.firstinspires.ftc.robotcore.internal.system.Assert;
import org.firstinspires.ftc.robotcore.internal.system.Deadline;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.WeakHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The ThreadPool class manages thread creation and shutdown in the SDK. We centralize things here
 * mostly so as to provide robust logging services that will aid in postmortem analysis of system
 * issues.
 */
@SuppressWarnings("WeakerAccess")
public class ThreadPool
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "ThreadPool";

    /** maps Thread.id to OS notion of thread id */
    private static LongSparseArray<Integer> threadIdMap = new LongSparseArray<Integer>();

    //----------------------------------------------------------------------------------------------
    // Singleton
    //----------------------------------------------------------------------------------------------

    /**
     * Singletons are helpers that gatekeep execution of work on a service to a single runnable
     * at a time. Submitting a runnable will return a SingletonResult, either from a newly started
     * item or one that was previously running. That result can be waited upon for completion.
     * Alternately, the Singleton itself can be waited upon to await the completion of the
     * currently running item, if any.
     * @see SingletonResult
     */
    public static class Singleton<T>
        {
        public static int INFINITE_TIMEOUT = -1;

        private       ExecutorService    service     = null;
        private final Object             lock        = new Object();
        private       SingletonResult<T> result      = null;
        private       boolean            inFlight    = false;

        public Singleton()
            {
            }

        public void setService(ExecutorService service)
            {
            this.service = service;
            }

        public void reset()
            {
            synchronized (lock)
                {
                this.result = null;
                this.inFlight = false;
                }
            }

        /**
         * Submits a runnable to the service of this singleton, but only if there's not some
         * other runnable currently submitted thereto.
         *
         * @param runnable  the code to run if something's not already running
         * @return          a result that can be waited upon for completion
         * @see #await(long)
         * @see #submit(int, Callable)
         * @see SingletonResult#await(long)
         */
        public SingletonResult<T> submit(final int msAwaitDefault, final Runnable runnable)
            {
            return this.submit(msAwaitDefault, new Callable<T>()
                {
                @Override public T call() throws Exception
                    {
                    runnable.run();
                    return null;
                    }
                });
            }

        public SingletonResult<T> submit(Runnable runnable)
            {
            return submit(Singleton.INFINITE_TIMEOUT, runnable);
            }

        public SingletonResult<T> submit(final int msAwaitDefault, final Callable<T> callable)
            {
            synchronized (lock)
                {
                // Have we already got someone running?
                if (!inFlight)
                    {
                    if (service == null)
                        {
                        throw new IllegalArgumentException("Singleton service must be set before work is submitted");
                        }

                    // Record us as busy *before* we actually try to run!
                    inFlight = true;

                    // Run and remember the future for later
                    result = new SingletonResult<T>(msAwaitDefault, this.service.submit(new Callable<T>()
                        {
                        @Override public T call() throws Exception
                            {
                            try {
                                return callable.call();
                                }
                            catch (InterruptedException ignored)
                                {
                                Thread.currentThread().interrupt();
                                return null;
                                }
                            catch (Exception e)
                                {
                                RobotLog.ee(TAG, e, "exception thrown during Singleton.submit()");
                                return null;
                                }
                            finally
                                {
                                // Note that we will allow another guy in the next time someone asks
                                synchronized (lock)
                                    {
                                    inFlight = false;
                                    }
                                }
                            }
                        }));
                    }
                return result;
                }
            }

        public SingletonResult<T> submit(Callable<T> callable)
            {
            return submit(Singleton.INFINITE_TIMEOUT, callable);
            }


        /**
         * Returns the result from the extant or previous work item, if any; otherwise, null.
         * @return the result from the extant or previous work item, if any; otherwise, null.
         */
        public SingletonResult<T> getResult()
            {
            synchronized (lock)
                {
                return result;
                }
            }

        /**
         * Awaits the completion of the extant work item, if one exists
         * @see #getResult()
         */
        public @Nullable T await(long ms) throws InterruptedException
            {
            SingletonResult<T> result = getResult();
            if (result != null)
                {
                return result.await(ms);
                }
            else
                return null;
            }

        public @Nullable T await() throws InterruptedException
            {
            SingletonResult<T> result = getResult();
            if (result != null)
                {
                return result.await();
                }
            else
                return null;
            }
        }

    /**
     * SingletonResults are returned from {@link Singleton Singletons} as a token
     * that can be used to await the completion of submitted work.
     * @see Singleton#submit(int, Callable)
     */
    public static class SingletonResult<T>
        {
        private Future<T> future = null;
        private long      nsDeadline;

        public SingletonResult(int msAwaitDefault, Future<T> future)
            {
            this.future = future;
            this.nsDeadline = msAwaitDefault==Singleton.INFINITE_TIMEOUT
                    ? -1
                    : System.nanoTime() + msAwaitDefault * ElapsedTime.MILLIS_IN_NANO;
            }

        public SingletonResult()
            {
            this(0, null);
            }

        public void setFuture(Future<T> future)
            {
            this.future = future;
            }

        /**
         * Awaits the completion of the work item associated with this result.
         * @param ms    the duration in milliseconds to wait
         * @return the result of the execution, or null of that was not available
         * @see Singleton#submit(int, Runnable)
         */
        public @Nullable T await(long ms) throws InterruptedException
            {
            try
                {
                if (this.future != null)
                    {
                    return this.future.get(ms, TimeUnit.MILLISECONDS);
                    }
                }
            catch (ExecutionException e)
                {
                RobotLog.ee(TAG, e, "singleton threw ExecutionException");
                }
            catch (TimeoutException e)
                {
                RobotLog.ee(TAG, e, "singleton timed out");
                }
            return null;
            }

        /**
         * Awaits (until deadline, forever, or until interruption) the completion of
         * the work item associated with this result.
         * @return the result of the execution, or null of that was not available
         * @see Singleton#submit(int, Runnable)
         */
        public @Nullable T await() throws InterruptedException
            {
            if (nsDeadline >= 0)
                {
                long nsNow = System.nanoTime();
                long nsRemaining = Math.max(0, nsDeadline - nsNow);
                return await(nsRemaining / ElapsedTime.MILLIS_IN_NANO);
                }
            else
                {
                try
                    {
                    if (this.future != null)
                        {
                        return this.future.get();
                        }
                    }
                catch (ExecutionException e)
                    {
                    RobotLog.ee(TAG, e, "singleton threw ExecutionException");
                    }
                return null;
                }
            }
        }


    //----------------------------------------------------------------------------------------------
    // Construction API
    //----------------------------------------------------------------------------------------------

    public static ExecutorService getDefault()
        {
        synchronized (ThreadPool.class)
            {
            if (defaultThreadPool == null)
                {
                defaultThreadPool = newCachedThreadPool("default threadpool");
                }
            return defaultThreadPool;
            }
        }

    public static ExecutorService getDefaultSerial()
        {
        synchronized (ThreadPool.class)
            {
            if (defaultSerialThreadPool == null)
                {
                defaultSerialThreadPool = newSingleThreadExecutor("default serial threadpool");
                }
            return defaultSerialThreadPool;
            }
        }

    public static ScheduledExecutorService getDefaultScheduler()
        {
        synchronized (ThreadPool.class)
            {
            if (defaultScheduler == null)
                {
                // Note: the size of the pool here is a compromise. The pool is effectively
                // fixed size, so we waste threads if it's too big. But it needs to be big
                // enough so as to prevent deadlock scenarios. The current value is a finger
                // in the wind. The ideal sol'n is to implement a dynamically sized scheduler.
                defaultScheduler = newScheduledExecutor(24, "default scheduler");
                }
            return defaultScheduler;
            }
        }

    /**
     * Creates an Executor that uses a single worker thread operating off an unbounded queue.
     * @see Executors#newSingleThreadExecutor()
     */
    public static ExecutorService newSingleThreadExecutor(@Nullable String nameRoot)
        {
        RecordingThreadPool result = new RecordingThreadPool(1, 1,
                                    0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
        if (nameRoot != null) result.setNameRootForThreads(nameRoot);
        noteNewExecutor(result);
        return result;
        }

    /**
     * Creates a thread pool that reuses a fixed number of threads operating off a shared unbounded
     * queue. At any point, at most {@code numberOfThreads} threads will be active processing tasks.
     * @see Executors#newFixedThreadPool(int)
     */
    public static ExecutorService newFixedThreadPool(int numberOfThreads, @Nullable String nameRoot)
        {
        RecordingThreadPool result = new RecordingThreadPool(numberOfThreads, numberOfThreads,
                                    0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
        if (nameRoot != null) result.setNameRootForThreads(nameRoot);
        noteNewExecutor(result);
        return result;
        }

    /**
     * Creates a thread pool that creates new threads as needed, but will reuse previously constructed
     * threads when they are available.
     * @see Executors#newCachedThreadPool()
     */
    public static ExecutorService newCachedThreadPool(@Nullable String nameRoot)
        {
        RecordingThreadPool result = new RecordingThreadPool(0, Integer.MAX_VALUE,
                                      30L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
        if (nameRoot != null) result.setNameRootForThreads(nameRoot);
        noteNewExecutor(result);
        return result;
        }

    /**
     * Creates an executor that can schedule commands to run after a given delay, or
     * to execute periodically. The pool has a minimum size of 1, but will expand as
     * necessary to accommodate its workload.
     * @see Executors#newSingleThreadScheduledExecutor
     */
    public static RecordingScheduledExecutor newScheduledExecutor(int maxWorkerThreadCount, @Nullable String nameRoot)
        {
        RecordingScheduledExecutor result = new RecordingScheduledExecutor(maxWorkerThreadCount);
        if (nameRoot != null) result.setNameRootForThreads(nameRoot);
        noteNewExecutor(result);
        return result;
        }

    /**
     * We use the keys of the weak hash map to remember the executors we've instantiated. Because
     * those keys are weak references, they won't keep our executors alive longer than they otherwise
     * would live, yet allows us to find them and iterate over them when we need to.
     */
    private       static Map<ExecutorService,Integer> extantExecutors     = new WeakHashMap<ExecutorService,Integer>();
    private final static Object                       extantExecutorsLock = new Object();
    private       static ExecutorService              defaultThreadPool   = null;
    private       static ExecutorService              defaultSerialThreadPool = null;
    private       static ScheduledExecutorService     defaultScheduler    = null;

    private static void noteNewExecutor(ExecutorService executor)
        {
        synchronized (extantExecutorsLock)
            {
            extantExecutors.put(executor,1);
            }
        }

    //----------------------------------------------------------------------------------------------
    // Operational API
    //----------------------------------------------------------------------------------------------

    private static void noteTID(Thread thread, int tid)
        {
        synchronized (ThreadPool.class)
            {
            threadIdMap.put(thread.getId(), tid);
            }
        }
    private static void removeTID(Thread thread)
        {
        synchronized (ThreadPool.class)
            {
            threadIdMap.remove(thread.getId());
            }
        }

    /** Returns the OS-level thread id for the given thread */
    public static int getTID(Thread thread)
        {
        return getTID(thread.getId());
        }

    /** Returns the OS-level thread id for the indicated Android-level thread id */
    public static int getTID(long threadId)
        {
        synchronized (ThreadPool.class)
            {
            return threadIdMap.get(threadId, 0);
            }
        }

    /**
     * Waits for the indicated timeout for the indicated executor service to shutdown. Will return
     * early if an interrupt occurs. If the shutdown is particularly lengthy, then at periodic intervals
     * (on the order of seconds) messages are recorded to the log to aid in postmortem analyses.
     *
     * @param executorService   the service to shutdown
     * @param timeout           the duration to wait for the shutdown
     * @param unit              the units in which the timeout parameter is provided
     * @param serviceName       the name of the service as it is to be recorded in the log
     * @return                  whether or not the service was seen to terminate within the allotted timeout
     */
    public static boolean awaitTermination(ExecutorService executorService, long timeout, TimeUnit unit, String serviceName) throws InterruptedException
        {
        verifyNotOnExecutorThread(executorService);

        Deadline deadline  = new Deadline(timeout, unit);
        int msRetry        = 2500;
        boolean terminated = false;
        //
        for (int iAttempt = 0; ; iAttempt++)
            {
            terminated = executorService.isTerminated();
            if (terminated)
                {
                // RobotLog.vv(TAG, "service %s terminated", serviceName);
                break;
                }

            RobotLog.vv(TAG, "waiting for service %s", serviceName);
            if (executorService.awaitTermination(Math.min(msRetry, deadline.timeRemaining(TimeUnit.MILLISECONDS)), TimeUnit.MILLISECONDS))
                {
                /** awaitTermination() is documented thusly: "@return {@code true} if this executor terminated and
                 *  {@code false} if the timeout elapsed before termination" */
                Assert.assertTrue(executorService.isTerminated());
                terminated = true;
                RobotLog.vv(TAG, "service %s terminated in awaitTermination()", serviceName);
                break;
                }

            // If we're past our due date, then we're not going to wait any longer
            if (deadline.hasExpired())
                {
                RobotLog.ee(TAG, "deadline expired waiting for service termination: %s", serviceName);
                break;
                }

            // It's taking a while. Log that fact
            RobotLog.vv(TAG, "awaiting shutdown: thread pool=\"%s\" attempt=%d", serviceName, iAttempt+1);

            // If we can, dump the stacks of each of the threads in the service. That might help
            // give some indication as to what on earth is going on inside the service that's
            // making it take so long.
            logThreadStacks(executorService);

            // (Re)interrupt any of these threads as an attempt to work around interrupt-eating bugs.
            // Caller should have interrupted threads / executed 'shutdownNow()' on service before
            // calling us, but even if he did, there are lurking bugs in, e.g., the FTDI infrastructure
            // that might need repeated pokes to get things working.
            interruptThreads(executorService);
            }

        if (terminated)
            {
            RobotLog.vv(TAG, "executive service %s(0x%08x) is terminated", serviceName, executorService.hashCode());
            }
        else
            {
            RobotLog.vv(TAG, "executive service %s(0x%08x) is NOT terminated", serviceName, executorService.hashCode());

            // Log the stacks of EVERYTHING we know about in the hope that that will give
            // us more information.
            synchronized (extantExecutorsLock)
                {
                // Run the collector in an attempt to clean up extantExecutors
                System.gc();

                // Log all the stacks that we know about.
                for (ExecutorService e : extantExecutors.keySet())
                    {
                    logThreadStacks(e);
                    }
                }
            }

        return terminated;
        }

    private static void logThreadStacks(ExecutorService executorService)
        {
        if (executorService instanceof ContainerOfThreads)
            {
            ContainerOfThreads container = (ContainerOfThreads)executorService;
            for (Thread thread : container)
                {
                if (thread.isAlive())
                    {
                    RobotLog.logStackTrace(thread, "");
                    }
                }
            }
        }

    private static void interruptThreads(ExecutorService executorService)
        {
        if (executorService instanceof ContainerOfThreads)
            {
            ContainerOfThreads container = (ContainerOfThreads)executorService;
            for (Thread thread : container)
                {
                if (thread.isAlive())
                    {
                    if (thread.getId() == Thread.currentThread().getId())
                        RobotLog.vv(TAG, "interrupting current thread");
                    thread.interrupt();
                    }
                }
            }
        }

    private static void verifyNotOnExecutorThread(ExecutorService executorService)
        {
        if (executorService instanceof ContainerOfThreads)
            {
            ContainerOfThreads container = (ContainerOfThreads)executorService;
            for (Thread thread : container)
                {
                if (thread == Thread.currentThread())
                    {
                    Assert.assertFailed();
                    }
                }
            }
        }

    /**
     * Awaits the termination of the indicated service for the indicated timeout. If the service
     * terminates, then the function returns. If the service does not terminate within the alloted
     * time, then a bold message is written to the log and the application is terminated.
     *
     * @param executorService   the executor service whose termination we are to await
     * @param timeout           the timeout we are to permit
     * @param unit              the units in which timeout is expressed
     * @param serviceName       a human-understandable name for the executor service
     * @param message           the message to print if we terminate the app
     */
    public static void awaitTerminationOrExitApplication(ExecutorService executorService, long timeout, TimeUnit unit, String serviceName, String message)
        {
        try {
            if (!awaitTermination(executorService, timeout, unit, serviceName))
                {
                exitApplication(serviceName, message);
                }
            }
        catch (InterruptedException e)
            {
            RobotLog.vv(TAG, "awaitTerminationOrExitApplication %s; interrupt thrown", serviceName);
            try {
                Thread.sleep(100);
                }
            catch (InterruptedException ignoredInterrupt)
                {
                Thread.currentThread().interrupt();
                }
            if (!executorService.isTerminated())
                {
                RobotLog.vv(TAG, "awaitTerminationOrExitApplication %s; exiting application after interrupt", serviceName);
                exitApplication(serviceName, message);
                }
            }
        }

    /** @return whether we know the future has stopped its work or not */
    public static boolean awaitFuture(Future future, long timeout, @NonNull TimeUnit unit)
        {
        boolean result = true;
        try {
            future.get(timeout, unit);
            }
        catch (CancellationException|ExecutionException e)
            {
            }
        catch (TimeoutException e)
            {
            result = false;
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            result = false;
            }
        return result;
        }

    public static void cancelFutureOrExitApplication(Future future, long timeout, TimeUnit unit, String serviceName, String message)
        {
        try {
            // Try to cancel (return value is useless: false returned both if already completed and if can't cancel)
            future.cancel(true);
            // Try to the the result in the allocated amount of time
            future.get(timeout, unit);
            }
        catch (CancellationException e)
            {
            // It cancelled. We're done
            }
        catch (ExecutionException e)
            {
            // The computation threw an exception. We figure that that's as good as
            // cancelling, and move on.
            RobotLog.logExceptionHeader(e, "exception thrown in future; ignoring");
            }
        catch (TimeoutException e)
            {
            // Couldn't shut down: we're out of here
            exitApplication(serviceName, message);
            }
        catch (InterruptedException e)
            {
            // We ourselves were interrupted while waiting. Follow standard operating procedure
            Thread.currentThread().interrupt();
            }
        }

    public static void exitApplication(String serviceName, String message)
        {
        RobotLog.ee(TAG, "*****************************************************************");
        RobotLog.ee(TAG, "%s took too long to exit; emergency killing app.", serviceName);
        RobotLog.ee(TAG, "%s", message);
        RobotLog.ee(TAG, "*****************************************************************");

        // If a debugger is attached, then don't actually exit. This allows us to debug
        // conditions that happen in timed shutdown paths.
        while (Debug.isDebuggerConnected())
            {
            Thread.yield();
            }

        System.exit(-1);
        }

    /**
     * Robustly log thread startup and shutdown
     */
    public static void logThreadLifeCycle(final String name, final Runnable runnable)
        {
        try
            {
            Thread.currentThread().setName(name);
            RobotLog.v(String.format("thread: '%s' starting...", name));
            runnable.run();
            }
        finally
            {
            RobotLog.v(String.format("thread: ...terminating '%s'", name));
            }
        }

    //----------------------------------------------------------------------------------------------
    // Types
    //----------------------------------------------------------------------------------------------

    public interface ContainerOfThreads extends Iterable<Thread>
        {
        void setNameRootForThreads(@NonNull String nameRootForThreads);
        void setPriorityForThreads(@NonNull Integer priorityForThreads);
        void noteNewThread(Thread thread);
        void noteFinishedThread(Thread thread);
        }

    /**
     * ThreadFactoryImpl notifies the container that it's passed on construction of all the
     * threads that get made.
     */
    static class ThreadFactoryImpl implements ThreadFactory
        {
        //------------------------------------------------------------------------------------------
        // State
        //------------------------------------------------------------------------------------------

        final ThreadFactory      threadFactory;
        final ContainerOfThreads container;

        //------------------------------------------------------------------------------------------
        // Construction
        //------------------------------------------------------------------------------------------

        ThreadFactoryImpl(ContainerOfThreads container)
            {
            this.threadFactory = Executors.defaultThreadFactory();
            this.container     = container;
            }

        //------------------------------------------------------------------------------------------
        // Operations
        //------------------------------------------------------------------------------------------

        @Override
        public Thread newThread(final Runnable runUserCode)
            {
            Thread thread = this.threadFactory.newThread(new Runnable()
                {
                @Override public void run()
                    {
                    noteTID(Thread.currentThread(), Process.myTid());
                    try {
                        runUserCode.run();
                        }
                    finally
                        {
                        ThreadFactoryImpl.this.container.noteFinishedThread(Thread.currentThread());
                        removeTID(Thread.currentThread());
                        }
                    }
                });
            this.container.noteNewThread(thread);
            return thread;
            }
        }

    /**
     * ContainerOfThreadsImpl serves as base for our executors, and records the threads used
     * in each.
     */
    static class ContainerOfThreadsRecorder implements ContainerOfThreads
        {
        //------------------------------------------------------------------------------------------
        // State
        //------------------------------------------------------------------------------------------

        Queue<Thread> threads;
        String        nameRootForThreads = null;
        AtomicInteger threadCount        = new AtomicInteger(0);
        Integer       priorityForThreads = null;

        //------------------------------------------------------------------------------------------
        // Construction
        //------------------------------------------------------------------------------------------

        ContainerOfThreadsRecorder()
            {
            this.threads = new ConcurrentLinkedQueue<Thread>();
            }

        //------------------------------------------------------------------------------------------
        // Operations
        //------------------------------------------------------------------------------------------

        @Override public void setNameRootForThreads(@NonNull String nameRootForThreads)
            {
            this.nameRootForThreads = nameRootForThreads;
            }

        @Override public void setPriorityForThreads(@NonNull Integer priorityForThreads)
            {
            this.priorityForThreads = priorityForThreads;
            }

        @SuppressLint("DefaultLocale") @Override
        public void noteNewThread(Thread thread)
            {
            this.threads.add(thread);
            if (this.nameRootForThreads != null)
                {
                thread.setName(String.format("%s-#%d", this.nameRootForThreads, threadCount.getAndIncrement()));
                }
            if (this.priorityForThreads != null)
                {
                thread.setPriority(this.priorityForThreads);
                }
            logThread(thread, "added");
            }

        @Override public void noteFinishedThread(Thread thread)
            {
            logThread(thread, "removed");
            this.threads.remove(thread);
            }

        protected void logThread(Thread thread, String action)
            {
            RobotLog.vv(TAG, "container(0x%08x%s) %s id=%d TID=%d count=%d",
                    this.hashCode(),
                    (nameRootForThreads==null ?"" : ": " + nameRootForThreads),
                    action,
                    thread.getId(),
                    getTID(thread),
                    this.threads.size());
            }

        @Override
        public Iterator<Thread> iterator()
            {
            return this.threads.iterator();
            }
        }

    /** @see ThreadPoolExecutor#afterExecute(Runnable, Throwable), after which this logic is modelled */
    protected static Throwable retrieveUserException(Runnable r, Throwable t)
        {
        if (t == null && r instanceof Future<?>)
            {
            try
                {
                /**
                 * In at least one case we've seen a call to get() deadlock if the runnable
                 * was not done.  Hence the protection here.
                 */
                if (((Future<?>) r).isDone())
                    {
                    Object result = ((Future<?>) r).get();
                    }
                }
            catch (CancellationException ce)
                {
                t = null; // not a user error; don't report
                }
            catch (ExecutionException ee)
                {
                t = ee.getCause();
                }
            catch (InterruptedException ie)
                {
                Thread.currentThread().interrupt();
                }
            }
        return t;
        }

    /**
     * Indicates a thread pool that is at times capable of using guest worker threads
     */
    public interface ThreadBorrowable
        {
        /**
         * Caller guarantees that thread is a typical utility 'worker' thread (in contrast to,
         * for example, the dedicated UI thread, or other threads on which work is dispatched
         * through a {@link Handler}). Answer whether we are ok to dispatch here instead of one of
         * our own worker threads. Caller must additionally assure themselves from their contextual
         * knowledge that taking advantage of this function will not lead to deadlocks that otherwise
         * would not occur.
         */
        boolean canBorrowThread(Thread thread);
        }

    public static class RecordingThreadPool extends ContainerOfThreadsRecorder implements ExecutorService
        {
        //------------------------------------------------------------------------------------------
        // State
        //------------------------------------------------------------------------------------------

        ThreadPoolExecutor executor;

        //------------------------------------------------------------------------------------------
        // Construction
        //------------------------------------------------------------------------------------------

        RecordingThreadPool(int nThreadsCore, int nThreadsMax, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue)
            {
            this.executor = new ThreadPoolExecutor(nThreadsCore, nThreadsMax,
                                      keepAliveTime, unit,
                                      workQueue,
                                      new ThreadFactoryImpl(this))
                {
                @Override protected void afterExecute(Runnable r, Throwable t)
                    {
                    super.afterExecute(r, t);
                    t = retrieveUserException(r,t);
                    if (t != null)
                        {
                        RobotLog.ee(TAG, t, "exception thrown in thread pool; ignored");
                        }
                    }
                };
            }

        //------------------------------------------------------------------------------------------
        // Executor
        //------------------------------------------------------------------------------------------

        @Override public void execute(Runnable command)
            {
            this.executor.execute(command);
            }

        //------------------------------------------------------------------------------------------
        // ExecutorService
        //------------------------------------------------------------------------------------------

        @Override public void shutdown()
            {
            this.executor.shutdown();
            }

        @Override public List<Runnable> shutdownNow()
            {
            return this.executor.shutdownNow();
            }

        @Override public boolean isShutdown()
            {
            return this.executor.isShutdown();
            }

        @Override public boolean isTerminated()
            {
            return this.executor.isTerminated();
            }

        @Override public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException
            {
            return this.executor.awaitTermination(timeout, unit);
            }

        @Override public <T> Future<T> submit(Callable<T> task)
            {
            return this.executor.submit(task);
            }

        @Override public <T> Future<T> submit(Runnable task, T result)
            {
            return this.executor.submit(task, result);
            }

        @Override public Future<?> submit(Runnable task)
            {
            return this.executor.submit(task);
            }

        @Override public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException
            {
            return this.executor.invokeAll(tasks);
            }

        @Override public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException
            {
            return this.executor.invokeAll(tasks, timeout, unit);
            }

        @Override public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException
            {
            return this.executor.invokeAny(tasks);
            }

        @Override public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
            {
            return this.executor.invokeAny(tasks, timeout, unit);
            }
        }

    public static class RecordingScheduledExecutor extends ContainerOfThreadsRecorder implements ScheduledExecutorService
        {
        //------------------------------------------------------------------------------------------
        // State
        //------------------------------------------------------------------------------------------

        protected ScheduledThreadPoolExecutor executor;

        //------------------------------------------------------------------------------------------
        // Construction
        //------------------------------------------------------------------------------------------

        RecordingScheduledExecutor(int numberOfThreads)
            {
            this.executor = new ScheduledThreadPoolExecutor(numberOfThreads, new ThreadFactoryImpl(this))
                {
                @Override protected void afterExecute(Runnable r, Throwable t)
                    {
                    super.afterExecute(r, t);
                    t = retrieveUserException(r,t);
                    if (t != null)
                        {
                        RobotLog.ee(TAG, t, "exception thrown in thread pool; ignored");
                        }
                    }
                };

            /**
             * Amazingly, threads which are scheduled but then cancelled are NOT automatically
             * removed from 'working' and made again available for subsequent work. Instead, one
             * has to set the 'remove on cancel policy' to make that happen.
             *
             * Unfortunately, while that API exists on KitKat, it is buggy. Presumably that's
             * why Android Studio and the Android docs report that API 21 is needed to use it.
             * We set that when we can so as to improve thread management on those platforms, but
             * on KitKat, we just live with it. So beware if you schedule a lot of actions that
             * you ultimately cancel.
             */
            if (Build.VERSION.SDK_INT >= 21)
                {
                this.executor.setRemoveOnCancelPolicy(true);
                }
            }

        public void setKeepAliveTime(long time, TimeUnit timeUnit)
            {
            this.executor.setKeepAliveTime(time, timeUnit);
            }

        public void allowCoreThreadTimeOut(boolean allow)
            {
            this.executor.allowCoreThreadTimeOut(allow);
            }

        //------------------------------------------------------------------------------------------
        // Executor
        //------------------------------------------------------------------------------------------

        @Override public void execute(Runnable command)
            {
            this.executor.execute(command);
            }

        //------------------------------------------------------------------------------------------
        // ExecutorService
        //------------------------------------------------------------------------------------------

        @Override public void shutdown()
            {
            this.executor.shutdown();
            }

        @Override public List<Runnable> shutdownNow()
            {
            return this.executor.shutdownNow();
            }

        @Override public boolean isShutdown()
            {
            return this.executor.isShutdown();
            }

        @Override public boolean isTerminated()
            {
            return this.executor.isTerminated();
            }

        @Override public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException
            {
            return this.executor.awaitTermination(timeout, unit);
            }

        @Override public <T> Future<T> submit(Callable<T> task)
            {
            return this.executor.submit(task);
            }

        @Override public <T> Future<T> submit(Runnable task, T result)
            {
            return this.executor.submit(task, result);
            }

        @Override public Future<?> submit(Runnable task)
            {
            return this.executor.submit(task);
            }

        @Override public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException
            {
            return this.executor.invokeAll(tasks);
            }

        @Override public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException
            {
            return this.executor.invokeAll(tasks, timeout, unit);
            }

        @Override public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException
            {
            return this.executor.invokeAny(tasks);
            }

        @Override public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
            {
            return this.executor.invokeAny(tasks, timeout, unit);
            }

        //------------------------------------------------------------------------------------------
        // ScheduledExecutorService
        //------------------------------------------------------------------------------------------

        @Override public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit)
            {
            return this.executor.schedule(command, delay, unit);
            }

        @Override public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit)
            {
            return this.executor.schedule(callable, delay, unit);
            }

        @Override public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit)
            {
            return this.executor.scheduleAtFixedRate(command, initialDelay, period, unit);
            }

        @Override public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit)
            {
            return this.executor.scheduleWithFixedDelay(command, initialDelay, delay, unit);
            }

        }

    }
