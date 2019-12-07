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
package com.qualcomm.hardware.lynx;

import android.support.annotation.NonNull;

import com.qualcomm.hardware.lynx.commands.LynxMessage;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.internal.opmode.OpModeManagerImpl;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * MessageKeyedLock is a recursively-acquirable lock that is keyed by a LynxMessage
 */
public class MessageKeyedLock
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    private final    String        name;
    private final    Lock          lock;
    private final    Lock          acquisitionsLock;
    private volatile boolean       tryingToHangAcquisitions;
    private final    Condition     condition;
    private volatile LynxMessage   lockOwner;
    private          int           lockCount;
    private          long          lockAquisitionTime;
    private          long          nanoLockAquisitionTimeMax;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public MessageKeyedLock(String name)
        {
        this(name, 500);
        }

    public MessageKeyedLock(String name, int msAquisitionTimeout)
        {
        /**
         * Note: this lock needs to be "fair" (threads are served in a first-come, first-serve manner)
         * to prevent a rogue user OpMode from continually "barging in" on the lock and thus never letting
         * anyone else grab it. The primary use case for this lock needing to be fair is to allow the code
         * that gives the robot controller a "lethal injection" (I love Bob's comments :D) to bust its way
         * in here and try to send failsafe commands to all the Lynx modules as a last ditch effort to
         * shutdown the robot before the app is restarted. The Java documentation states that a "fair" lock
         * is considerably slower than a non-fair one, but I tested loop speed both before this modification
         * and after, and found no quantifiable difference.
         *
         * Also see: {@link OpModeManagerImpl.OpModeStuckCodeMonitor.Runner#run()} // Arrghh 'protected' breaks JavaDocs
         *           {@link #lockAcquisitions()}
         */
        this.acquisitionsLock = new ReentrantLock(true);
        this.tryingToHangAcquisitions = false;
        this.lock       = new ReentrantLock();
        this.name       = name;
        this.condition  = this.lock.newCondition();
        this.lockOwner  = null;
        this.lockCount  = 0;
        this.lockAquisitionTime        = 0;
        this.nanoLockAquisitionTimeMax = msAquisitionTimeout * ElapsedTime.MILLIS_IN_NANO;
        }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    private void logv(String format, Object... args)
        {
        RobotLog.v("%s: %s", this.name, String.format(format, args));
        }
    private void loge(String format, Object... args)
        {
        RobotLog.e("%s: %s", this.name, String.format(format, args));
        }

    public void reset() throws InterruptedException
        {
        this.lock.lockInterruptibly();
        try {
            this.lockOwner = null;
            this.lockCount = 0;
            this.lockAquisitionTime = 0;
            this.condition.signalAll();  // probably not needed, but harmless
            }
        finally
            {
            this.lock.unlock();
            }
        }

    public void acquire(@NonNull LynxMessage message) throws InterruptedException
        {
        if (message == null) throw new IllegalArgumentException("MessageKeyedLock.acquire: null message");

        /*
         * We check whether we're supposed to lock 'acquisitionsLock' interruptibly or
         * not. Basically, we want user code to be able to gracefully exit when interrupted
         * whenever possible. However, in the case of a rogue OpMode (that we're attempting
         * to block from sending further commands to the Lynx module), rather than letting
         * it burn a ridiculous amount of CPU cycles continually eating interrupted exceptions,
         * we choose to instead lock un-interruptibly, so as to hang it here peacefully until
         * the JVM kills it when we restart the app.
         *
         */
        if(tryingToHangAcquisitions)
        {
            this.acquisitionsLock.lock();
        }
        else
        {
            this.acquisitionsLock.lockInterruptibly();
        }

        try
        {
            this.lock.lockInterruptibly();
        }
        catch (Exception e)
        {
            this.acquisitionsLock.unlock();
            throw e;
        }

        try {
            if (this.lockOwner != message)
                {
                while (this.lockOwner != null)
                    {
                    long now = System.nanoTime();
                    if (now - this.lockAquisitionTime > nanoLockAquisitionTimeMax)
                        {
                        // Something really odd has happened with the locking logic: it's taking way
                        // too long. Reset and get out rather than locking up forever.
                        loge("#### abandoning lock: old=%s(%d)", this.lockOwner.getClass().getSimpleName(), this.lockOwner.getMessageNumber());
                        loge("                      new=%s(%d)", message.getClass().getSimpleName(), message.getMessageNumber());
                        break;
                        }
                    this.condition.await(nanoLockAquisitionTimeMax / 4, TimeUnit.NANOSECONDS);
                    }
                this.lockCount = 0;
                this.lockAquisitionTime = System.nanoTime();
                this.lockOwner = message;
                if (LynxUsbDeviceImpl.DEBUG_LOG_DATAGRAMS_LOCK) logv("lock %s msg#=%d", this.lockOwner.getClass().getSimpleName(), this.lockOwner.getMessageNumber());
                }
            else
                {
                logv("lock recursively acquired");
                }

            this.lockCount++;
            }
        finally
            {
            this.lock.unlock();
            this.acquisitionsLock.unlock();
            }
        }

    public void release(@NonNull LynxMessage message) throws InterruptedException
        {
        if (message == null) throw new IllegalArgumentException("MessageKeyedLock.release: null message");

        /**
         * NOTE: because lockInterruptibly() is used, this has the potential to cause
         * a message to never release its lock, because if we were interrupted while a
         * command was in flight, then ann InterruptedException will be thrown and we
         * will not ever get to the part where we clear the lock owner and release.
         *
         * This is never really an issue, since any messages after interruption are
         * simply suppressed - see https://github.com/FIRST-Tech-Challenge/SkyStone/issues/20
         * (Since the lockInterruptibly() call in {@link #acquire(LynxMessage)} would immediately throw).
         *
         * However, now that we try to send a failsafe command restarting the app if the user code
         * doesn't exit on time, (see {@link OpModeManagerImpl.OpModeStuckCodeMonitor.Runner#run()}),
         * one might think this would be an issue. (I.e. that the last ditch effort failsafe would
         * have to wait until an old lock was abandoned). However, that is actually not the case,
         * because the last ditch effort failsafe only runs 1000ms after a stop is requested, and
         * the timeout for abandoning a lock is 500ms, so the lock will be immediately abandoned.
         */
        this.lock.lockInterruptibly();
        try {
            if (this.lockOwner == message)
                {
                if (--this.lockCount == 0)
                    {
                    if (LynxUsbDeviceImpl.DEBUG_LOG_DATAGRAMS_LOCK) logv("unlock %s msg#=%d", this.lockOwner.getClass().getSimpleName(), this.lockOwner.getMessageNumber());
                    this.lockOwner = null;
                    this.condition.signalAll();
                    }
                else
                    {
                    logv("lock recursively released");
                    }
                }
            else
                {
                if (this.lockOwner != null)
                    {
                    loge("#### incorrect owner releasing message keyed lock: ignored: old=%s(%d:%d)",
                            this.lockOwner.getClass().getSimpleName(),
                            this.lockOwner.getModuleAddress(),
                            this.lockOwner.getMessageNumber());
                    loge("                                                            new=%s(%d:%d)",
                            message.getClass().getSimpleName(),
                            message.getModuleAddress(),
                            message.getMessageNumber());
                    }
                else
                    {
                    loge("#### releasing ownerless message keyed lock: ignored: %s", message.getClass().getSimpleName());
                    }
                }
            }
        finally
            {
            this.lock.unlock();
            }
        }

    public void lockAcquisitions()
        {
        if(LynxUsbDeviceImpl.DEBUG_LOG_DATAGRAMS_LOCK)
            {
            logv("***ALL FUTURE ACQUISITION ATTEMPTS FROM THREADS OTHER THAN %s WILL NOW HANG!***", Thread.currentThread().getName());
            }
        this.acquisitionsLock.lock();
        this.tryingToHangAcquisitions = true;
        }

    }
