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
package org.firstinspires.ftc.robotcore.external.function;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.qualcomm.robotcore.util.ThreadPool;

import org.firstinspires.ftc.robotcore.internal.system.MemberwiseCloneable;

import java.util.concurrent.Executor;

/**
 * {@link Continuation} provides mechanisms for continuing subsequent, later work on a different
 * thread (either a handler thread or a worker thread) along with a contextual object that will
 * be present at such time. The latter is usually a consumer of some type, that will thus receive
 * the result of some computation with threading all taken care of neat and tidy.
 */
@SuppressWarnings("WeakerAccess")
public class Continuation<T>
    {
    //----------------------------------------------------------------------------------------------
    // Types
    //----------------------------------------------------------------------------------------------

    protected abstract static class Dispatcher<S> extends MemberwiseCloneable<Dispatcher<S>>
        {
        protected Continuation<S> continuation;

        public Dispatcher(Continuation<S> continuation)
            {
            this.continuation = continuation;
            }
        public boolean isTrivial()
            {
            return false;
            }
        public boolean isHandler()
            {
            return false;
            }
        public boolean isExecutor()
            {
            return false;
            }
        public void setContinuation(Continuation<S> continuation)
            {
            this.continuation = continuation;
            }

        public abstract void dispatch(ContinuationResult<? super S> consumer);

        /** Returns a copy of this object (duh). Note that we use 'clone' as in practice we've maybe
         * got anonymous subclasses whose names we don't even know. The alternative would have been
         * to use reflection, but that's slower. */
        @SuppressWarnings("unchecked") public <U> Dispatcher<U> copyAndCast()
            {
            Dispatcher<U> result = (Dispatcher<U>)(memberwiseClone());
            result.continuation = null;     // it's probably the wrong type anyway; caller will deal with setting right value
            return result;
            }
        }

    protected static class HandlerDispatcher<S> extends Dispatcher<S>
        {
        private final Handler handler;

        public HandlerDispatcher(Continuation<S> continuation, @NonNull Handler handler)
            {
            super(continuation);
            this.handler = handler;
            }
        @Override public boolean isHandler()
            {
            return true;
            }
        public Handler getHandler()
            {
            return handler;
            }
        @Override public void dispatch(final ContinuationResult<? super S> consumer)
            {
            final S capturedTarget = continuation.target;
            this.handler.post(new Runnable()
                {
                @Override public void run()
                    {
                    consumer.handle(capturedTarget);
                    }
                });
            }
        }

    protected static class ExecutorDispatcher<S> extends Dispatcher<S>
        {
        private final Executor executor;

        public ExecutorDispatcher(Continuation<S> continuation, @NonNull Executor executor)
            {
            super(continuation);
            this.executor = executor;
            }
        @Override public boolean isExecutor()
            {
            return true;
            }
        public Executor getExecutor()
            {
            return executor;
            }
        @Override public void dispatch(final ContinuationResult<? super S> consumer)
            {
            final S capturedTarget = this.continuation.target;
            executor.execute(new Runnable()
                {
                @Override public void run()
                    {
                    consumer.handle(capturedTarget);
                    }
                });
            }
        }

    protected static class TrivialDispatcher<S> extends Dispatcher<S>
        {
        public TrivialDispatcher(Continuation<S> continuation)
            {
            super(continuation);
            }
        @Override public boolean isTrivial()
            {
            return true;
            }

        @Override public void dispatch(ContinuationResult<? super S> consumer)
            {
            consumer.handle(continuation.target);
            }
        }

    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = Continuation.class.getSimpleName();

    protected final T target;
    protected Dispatcher<T> dispatcher;

    public T getTarget()
        {
        return target;
        }

    public Dispatcher<T> getDispatcher()
        {
        return dispatcher;
        }

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    protected Continuation(T target)
        {
        this.target = target;
        }

    public static @NonNull <T> Continuation<T> createTrivial(T t)
        {
        return new Continuation<T>(t).createTrivialDispatcher();
        }

    public static @NonNull <T> Continuation<T> create(@NonNull Executor executor, T t)
        {
        return new Continuation<T>(t).createExecutorDispatcher(executor);
        }

    public static @NonNull <T> Continuation<T> create(@Nullable Handler handler, T t)
        {
        return new Continuation<T>(t).createHandlerDispatcher(handler);
        }

    /**
     * Return a new continuation that dispatches to the same location but operates on a new
     * target instance of a possibly different target type.
     */
    public @NonNull <U> Continuation<U> createForNewTarget(U newTarget)
        {
        return new Continuation<U>(newTarget).setDispatcher(this.dispatcher.<U>copyAndCast());
        }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    public void dispatch(ContinuationResult<? super T> consumer)
        {
        dispatcher.dispatch(consumer);
        }

    /**
     * Note: be very careful in the use of {@link #dispatchHere(ContinuationResult)}, as it
     * can easily lead to unexpected deadlocks.
     */
    public void dispatchHere(ContinuationResult<? super T> consumer)
        {
        consumer.handle(target);
        }

    public boolean isHandler()
        {
        return dispatcher.isHandler();
        }
    public Handler getHandler()
        {
        return ((HandlerDispatcher<T>)dispatcher).getHandler();
        }

    public boolean isTrivial()
        {
        return dispatcher.isTrivial();
        }

    /**
     * When we dispatch this continuation, is it a synchronous, blocking call, one that
     * will be fully executed before the dispatch returns?
     */
    public boolean isDispatchSynchronous()
        {
        return isTrivial();
        }

    /**
     * Caller guarantees that they're on a typical utility 'worker' thread (in contrast to,
     * for example, the dedicated UI thread, or other threads on which work is dispatched
     * through a {@link Handler}). Answer whether we are ok to dispatch here instead of wherever
     * we might usually dispatch. Caller must additionally assure themselves from their contextual
     * knowledge that taking advantage of this function will not lead to deadlocks that otherwise
     * would not occur.
     */
    public boolean canBorrowThread(Thread thread)
        {
        if (dispatcher.isTrivial())
            return true;

        if (dispatcher.isExecutor())
            {
            ExecutorDispatcher<T> executorDispatcher = (ExecutorDispatcher<T>)dispatcher;
            if (executorDispatcher.getExecutor() instanceof ThreadPool.ThreadBorrowable)
                {
                return ((ThreadPool.ThreadBorrowable)executorDispatcher.getExecutor()).canBorrowThread(thread);
                }
            }

        return false;
        }

    //----------------------------------------------------------------------------------------------
    // Internal
    //----------------------------------------------------------------------------------------------

    protected Continuation<T> createTrivialDispatcher()
        {
        this.dispatcher = new TrivialDispatcher<T>(this);
        return this;
        }
    protected Continuation<T> createHandlerDispatcher(@Nullable Handler handler)
        {
        if (handler==null)
            {
            if (Looper.myLooper() != null)
                {
                // Current thread has a looper; use that
                handler = new Handler();
                }
            else if (Looper.getMainLooper() != null)
                {
                // Try the main UI thread
                handler = new Handler(Looper.getMainLooper());
                }
            }
        if (handler == null)
            {
            throw new IllegalArgumentException("handler is null, but no looper on this thread or main thread");
            }
        this.dispatcher = new HandlerDispatcher<T>(this, handler);
        return this;
        }
    protected Continuation<T> createExecutorDispatcher(Executor threadPool)
        {
        this.dispatcher = new ExecutorDispatcher<T>(this, threadPool);
        return this;
        }
    protected Continuation<T> setDispatcher(Dispatcher<T> dispatcher)
        {
        this.dispatcher = dispatcher;
        this.dispatcher.setContinuation(this);
        return this;
        }
    }
