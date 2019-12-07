/*
 * Copyright (c) 2018 Craig MacFarlane
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * (subject to the limitations in the disclaimer below) provided that the following conditions are
 * met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the
 * distribution.
 *
 * Neither the name of Craig MacFarlane nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS LICENSE. THIS
 * SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.firstinspires.ftc.robotcore.internal.system;

import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.ThreadPool;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * A watchdog class that, if not stroked often enough barks, executes some action, and then dies.
 *
 * Emphasis: Will only bark once.  To bark again, it must be explicitly restarted.
 */
public class Watchdog {

    private final static String TAG = "Watchdog";

    private long timeout;
    private int period;
    private TimeUnit unit;
    private Runnable bark;
    private Runnable growl;
    private int growlTime;
    private Deadline deadline;
    private ScheduledExecutorService executorService;
    private ScheduledFuture<?> futureTask;
    private boolean alreadyGrowled;

    /**
     * Creates a watchdog instance
     *
     * @param bark The action to take should the watchdog not get stroked.
     * @param period The interval, in time units between checking the dog.
     * @param timeout The timeout defining not often enough.
     * @param unit The unit of time for the time values.
     */
    public Watchdog(Runnable bark, int period, long timeout, TimeUnit unit)
    {
        this.period = period;
        this.timeout = timeout;
        this.growlTime = 0;
        this.unit = unit;
        this.bark = bark;
        this.growl = null;

        this.deadline = null;
        this.executorService = null;
        this.futureTask = null;
    }

    /**
     * Creates a watchdog instance that will growl a warning that it's about to bark
     *
     * @param bark The action to take should the watchdog not get stroked.
     * @param growl The action to take should the watchdog growl.
     * @param growlTime The amount of time before a bark that the watchdog will growl.
     * @param period The interval, in time units between checking the dog.
     * @param timeout The timeout defining not often enough.
     * @param unit The unit of time for the time values.
     */
    public Watchdog(Runnable bark, Runnable growl, int growlTime, int period, long timeout, TimeUnit unit)
    {
        this.period = period;
        this.timeout = timeout;
        this.unit = unit;
        this.bark = bark;
        this.growl = growl;
        this.growlTime = growlTime;

        this.deadline = null;
        this.executorService = null;
        this.futureTask = null;
    }

    /**
     * Start the watchdog.
     */
    public synchronized void start()
    {
        if (deadline != null) {
            RobotLog.ee(TAG, "Don't start the same watchdog twice");
            return;
        }

        deadline = new Deadline(timeout, unit);
        executorService = ThreadPool.newScheduledExecutor(1, "Watchdog");
        futureTask = executorService.scheduleAtFixedRate(new WatchdogPeriodic(), period, period, unit);
        alreadyGrowled = false;
    }

    /**
     * stroke
     *
     * Keeps the dog happy.   Prevents it from barking.
     */
    public synchronized void stroke()
    {
        if (deadline != null) {
            deadline.reset();
        } else {
            RobotLog.ii(TAG, "The dog was stroked after it was euthanized.");
            start();
        }
        alreadyGrowled = false;
    }

    /**
     * euthanize
     *
     * Kills the watchdog.  May be resurrected with start() without creating a
     * new instance.
     */
    public synchronized void euthanize()
    {
        deadline = null;
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }
    }

    public boolean isRunning()
    {
        return (executorService != null);
    }

    /**
     * checkDog
     *
     * Have we gotten any attention lately?  If not, bark.
     */
    protected synchronized void checkDog()
    {
        if (deadline == null) {
            RobotLog.ww(TAG, "Checking a dog that is not alive.");
            return;
        }

        if (deadline.hasExpired()) {
            bark.run();
            euthanize();
        } else if ((growl != null) && (!alreadyGrowled) && (deadline.timeRemaining(unit) <= growlTime)) {
            growl.run();
            alreadyGrowled = true;
        }
    }

    private class WatchdogPeriodic implements Runnable {
        @Override
        public void run()
        {
            checkDog();
        }
    }

}
