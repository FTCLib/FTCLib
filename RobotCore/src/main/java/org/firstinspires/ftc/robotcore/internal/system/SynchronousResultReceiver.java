/*
Copyright (c) 2018 Noah Andrews

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of Noah Andrews nor the names of his contributors may be used to
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

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import androidx.annotation.NonNull;

import com.qualcomm.robotcore.util.RobotLog;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * ResultReceiver that can be used synchronously.
 */
public abstract class SynchronousResultReceiver<T> extends ResultReceiver {
    private final BlockingQueue<T> resultQueue;
    private final String tag;

    /**
     * Constructor
     *
     * @param queueCapacity How many results can be in the queue before further results get dropped
     */
    public SynchronousResultReceiver(int queueCapacity, String tag, Handler handler) {
        super(handler);
        resultQueue = new ArrayBlockingQueue<>(queueCapacity);
        this.tag = tag;
    }

    /**
     * Convert the raw resultCode and Bundle into the result type
     *
     * This will be called immediately when onReceiveResult is called, so you can pretend this is a
     * normal ResultReceiver from this method, should that be useful.
     *
     * This method will get called even if the queue is full.
     *
     * If a Handler that runs on the main Looper was provided to the constructor, this runs on the
     * UI thread. In this case, spawn a worker thread for long-running operations.
     */
    protected abstract @NonNull T provideResult(int resultCode, Bundle resultData);

    /**
     * Synchronously get a result from the ResultReceiver. May be called multiple times to get multiple results.
     *
     * @param timeout The number of units to wait before timing out
     * @param timeoutUnit The timeout unit
     * @return The result
     * @throws InterruptedException If the thread is interrupted while waiting
     * @throws TimeoutException If the request times out without receiving a result
     */
    public final @NonNull T awaitResult(long timeout, TimeUnit timeoutUnit) throws InterruptedException, TimeoutException {
        T result = resultQueue.poll(timeout, timeoutUnit);
        if (result == null) throw new TimeoutException();
        return result;
    }

    @Override
    protected final void onReceiveResult(int resultCode, Bundle resultData) {
        boolean queueOverflowed = !resultQueue.offer(provideResult(resultCode, resultData));
        if (queueOverflowed) RobotLog.ww(tag, "The queue is full! Ignoring the result we just received.");
    }
}
