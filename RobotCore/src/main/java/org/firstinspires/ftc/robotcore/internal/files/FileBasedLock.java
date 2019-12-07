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
package org.firstinspires.ftc.robotcore.internal.files;

import com.qualcomm.robotcore.util.ReadWriteFile;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.ThreadPool;

import org.firstinspires.ftc.robotcore.external.Supplier;
import org.firstinspires.ftc.robotcore.external.ThrowingCallable;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * {@link FileBasedLock} provides non-recursive exclusive-lock semantics across threads and
 * across processes by means of file-system artifacts: a directory, files, and timestamps.
 * Recovery for crashed processes is provided.
 */
@SuppressWarnings("WeakerAccess")
public class FileBasedLock
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "FileBasedLock";

    protected File rootDir;
    protected File lockFile;
    protected final Random random = new Random();
    protected final int msDeadlineInterval = 4000;
    protected final int msRefreshInterval  = 1000;
    protected final int msClockSlop        = 2000;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public FileBasedLock(File rootDir)
        {
        this.rootDir = rootDir.getAbsoluteFile();
        this.lockFile = new File(this.rootDir, "lock.dat");

        AppUtil.getInstance().ensureDirectoryExists(rootDir);
        }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    protected class NeverThrown extends Exception {};

    public void lockWhile(final Runnable runnable) throws InterruptedException
        {
        lockWhile(new Supplier<Void>()
            {
            @Override public Void get()
                {
                runnable.run();
                return null;
                }
            });
        }

    public <T> T lockWhile(final Supplier<T> supplier) throws InterruptedException
        {
        try {
            return lockWhile(new ThrowingCallable<T, NeverThrown>()
                {
                @Override public T call()
                    {
                    return supplier.get();
                    }
                });
            }
        catch (NeverThrown throwable)
            {
            throw AppUtil.getInstance().unreachable(TAG, throwable);
            }
        }

    public <T,E extends Throwable> T lockWhile(ThrowingCallable<T,E> throwingCallable) throws InterruptedException, E
        {
        try {
            return lockWhile(Long.MAX_VALUE, TimeUnit.MILLISECONDS, throwingCallable);
            }
        catch (TimeoutException e)
            {
            throw AppUtil.getInstance().unreachable(TAG, e);
            }
        }

    public <T,E extends Throwable> T lockWhile(long timeout, TimeUnit timeUnit, ThrowingCallable<T, E> throwingCallable) throws TimeoutException, InterruptedException, E
        {
        T result = null;

        // Try to acquire the lock
        lock(timeout, timeUnit);

        try {
            // Set a watcher a-going that updates our intended deadline while the user's code runs
            Future deadlineUpdater = ThreadPool.getDefault().submit(new Runnable()
                {
                @Override public void run()
                    {
                    // Wait a while, stopping when asked
                    while (!Thread.currentThread().isInterrupted())
                        {
                        try {
                            // Err a bit on the side of safety wrt how often we update the deadline
                            Thread.sleep(msRefreshInterval, 0);
                            }
                        catch (InterruptedException e)
                            {
                            return;
                            }

                        // Update the deadline on the lock file
                        RobotLog.vv(TAG, "refreshing lock %s", lockFile.getPath());
                        refreshDeadline(lockFile);
                        }
                    }
                });

            // Run the user's code
            try {
                result = throwingCallable.call();
                }
            finally
                {
                deadlineUpdater.cancel(true);
                }
            }
        finally
            {
            unlock();
            }
        return result;
        }

    protected void lock(long timeout, TimeUnit timeUnit) throws TimeoutException, InterruptedException
        {
        if (timeout < 0) throw new IllegalArgumentException(String.format("timeout must be >= 0: %d", timeout));

        // Create the file we will use to carry out our lock attempt. The timestamp on this file
        // will be the time at which we intend to give up the lock. When renamed to 'lockFile',
        // we will own the lock.
        File progenetor = newTempFile();
        try {
            FileOutputStream outputStream = new FileOutputStream(progenetor);
            outputStream.close();
            }
        catch (IOException e)
            {
            throw new RuntimeException(String.format("unable to create %s", progenetor.getPath()), e);
            }

        // Establish the deadline. Be careful of REALLY big timeouts
        long msTimeout = timeUnit.toMillis(timeout);
        long msNow     = msNow();
        long timeoutDeadline = msTimeout + msNow >= msNow ? msTimeout + msNow : Long.MAX_VALUE;

        // Try to rename progenetor to lock file
        for (;;)
            {
            if (Thread.interrupted())
                {
                throw new InterruptedException(String.format("interrupt while acquiring lock %s", rootDir.getPath()));
                }

            if (msNow() > timeoutDeadline)
                {
                //noinspection ResultOfMethodCallIgnored
                progenetor.delete();
                throw new TimeoutException(String.format("unable to acquire lock %s", rootDir.getPath()));
                }

            // Initialize the deadline
            refreshDeadline(progenetor);

            // Attempt a rename to acquire the lock
            if (progenetor.renameTo(lockFile))
                {
                RobotLog.vv(TAG, "locked %s", lockFile.getPath());
                return; // Acquired the lock
                }

            // Didn't acquire the lock. Should we break the lock? Note that an ACTIVE lock
            // holder will never let the deadline get close enough to triggering so as to
            // be deleted while they're active. So we only ever break DEAD lock holders. Which
            // means that the race inherent in multiple simultaneous break attempts doesn't matter.
            long msRecordedDeadline = getDeadline(lockFile);
            if (msRecordedDeadline != 0)    // zero means error. race on deletion? either way, try again
                {
                msRecordedDeadline += msClockSlop;  // clock slop allows for network time sync issues (not well tested)
                if (msRecordedDeadline > msNow())
                    {
                    // We're past the intended deadline. Break the lock
                    RobotLog.vv(TAG, "breaking lock %s", lockFile.getPath());
                    releaseLock();
                    }
                }

            // Be gracious
            Thread.yield();
            }
        }

    protected File newTempFile()
        {
        return new File(rootDir, UUID.randomUUID().toString() + ".tmp");
        }

    protected void unlock()
        {
        RobotLog.vv(TAG, "unlocking %s", lockFile.getPath());
        releaseLock();
        }

    protected void refreshDeadline(File file)
        {
        // Be aware that file systems can have quite coarse granularity with which they
        // store modification stamps. DOS was notorious for two-second granularity, for instance.
        // Android seems intrinsically to be able to do no better than 1000ms granularity on ANY
        // file system.
        //
        // It's even worse:
        //      https://issuetracker.google.com/issues/36906542
        //      https://issuetracker.google.com/issues/36906982
        //      https://issuetracker.google.com/issues/36930892
        //      https://issuetracker.google.com/issues/36940415
        // setLastModified() isn't supported on 'internal storage' or SDCard locations !!!
        //
        // So: we use the file contents, and hope/check (?) we don't see torn writes
        //
        long deadline = msNow() + msDeadlineInterval;
        int token = random.nextInt();   // could use a UUID, but would be larger, so longer write times, more torn writes, maybe
        String contents = String.format(Locale.getDefault(), "%d|%d|%d", token, deadline, token);
        ReadWriteFile.writeFile(file, contents);
        }

    protected long getDeadline(File file)
        {
        for (;;)
            {
            String contents;
            try {
                contents = ReadWriteFile.readFileOrThrow(file);
                }
            catch (IOException e)
                {
                return 0;
                }
            String[] splits = contents.split("\\|");
            if (splits.length == 3)
                {
                int firstToken = Integer.valueOf(splits[0]);
                int lastToken = Integer.valueOf(splits[2]);
                if (firstToken == lastToken)
                    {
                    return Long.valueOf(splits[1]);
                    }
                }
            }
        }

    protected void releaseLock()
        {
        File toDelete = newTempFile();
        if (lockFile.renameTo(toDelete))
            {
            if (!toDelete.delete())
                {
                RobotLog.ee(TAG, "unable to delete %s", toDelete.getPath());
                }
            }
        else
            {
            if (lockFile.exists())
                {
                RobotLog.ee(TAG, "unable to rename %s to %s for deletion", lockFile.getPath(), toDelete.getPath());
                }
            }
        }

    protected long msNow()
        {
        return System.currentTimeMillis();
        }
    }
