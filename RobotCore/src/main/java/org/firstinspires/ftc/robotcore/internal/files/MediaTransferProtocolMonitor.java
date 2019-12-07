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

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;

import com.qualcomm.robotcore.util.ReadWriteFile;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.ThreadPool;

import org.firstinspires.ftc.robotcore.external.Predicate;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.io.Closeable;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * {@link MediaTransferProtocolMonitor} helps keep MTP views on desktops in sync
 * with the contents of the actual file system on the phone. We introduce some latency
 * to the scanning so as to minimize overhead.
 */
@SuppressWarnings("WeakerAccess")
public class MediaTransferProtocolMonitor implements Closeable
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "MTPMonitor";
    public static final String tmpMTPExtension = ".mtpmgr.tmp";

    protected final Context             context;
    protected final Object              concurrentClientLock = new Object();
    protected       Set<String>         pendingPaths = new HashSet<String>();
    protected       ExecutorService     executorService = null;
    protected       int                 msMinimumScanInterval = 5000;
    protected       RecursiveFileObserver dirObserver = null;

    protected static class InstanceHolder
        {
        public static MediaTransferProtocolMonitor theInstance = new MediaTransferProtocolMonitor();
        }
    public static MediaTransferProtocolMonitor getInstance()
        {
        return InstanceHolder.theInstance;
        }

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public MediaTransferProtocolMonitor()
        {
        this.context = AppUtil.getDefContext();
        }

    @Override public void close()
        {
        stop();
        }

    //----------------------------------------------------------------------------------------------
    // Starting and stopping
    //----------------------------------------------------------------------------------------------

    protected void start()
        {
        synchronized (concurrentClientLock)
            {
            stop();
            startNotifications();
            startObserver();
            }
        }

    protected void stop()
        {
        synchronized (concurrentClientLock)
            {
            stopObserver();
            stopNotifications();
            }
        }

    protected void startNotifications()
        {
        executorService = ThreadPool.newSingleThreadExecutor(TAG);
        executorService.submit(new Runnable()
            {
            @Override public void run()
                {
                while (!Thread.currentThread().isInterrupted())
                    {
                    try { Thread.sleep(msMinimumScanInterval); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    if (Thread.currentThread().isInterrupted())
                        {
                        break;
                        }
                    notifyMTP();
                    }
                }
            });
        }

    protected void stopNotifications()
        {
        if (executorService != null)
            {
            executorService.shutdownNow();
            ThreadPool.awaitTerminationOrExitApplication(executorService, 2, TimeUnit.SECONDS, TAG, "internal error");
            executorService = null;
            }
        }

    // Set up a file observer to know when things are created in the external directory so that
    // they can be made publicly visible over MTP (Media Transfer Protocol). Note that this is all
    // just a temporary thing: *everything* will be made visible the next time the device reboots.
    protected void startObserver()
        {
        final File directory = AppUtil.ROOT_FOLDER;
        RobotLog.vv(TAG, "observing: %s", directory.getAbsolutePath());

        int access = RecursiveFileObserver.IN_Q_OVERFLOW | RecursiveFileObserver.CREATE | RecursiveFileObserver.DELETE | RecursiveFileObserver.MOVED_FROM | RecursiveFileObserver.MOVED_TO;
        access |= RecursiveFileObserver.CLOSE_WRITE;    // this might, conceivably, maybe, alter how the scanning works. so we include
        dirObserver = new RecursiveFileObserver(directory.getAbsolutePath(), access, RecursiveFileObserver.Mode.RECURSIVE, new RecursiveFileObserver.Listener()
            {
            @Override public void onEvent(int event, File observedFile)
                {
                // if (!isIndicatorFile(observedFile)) RobotLog.vv(TAG, "observed (1): event=0x%02x %s", event, observedFile);

                if ((event & RecursiveFileObserver.ALL_FILE_OBSERVER_EVENTS) != 0)
                    {
                    // if (!isIndicatorFile(observedFile)) RobotLog.vv(TAG, "observed: event=0x%02x %s", event, observedFile);
                    noteFile(observedFile);
                    }
                if ((event & RecursiveFileObserver.IN_Q_OVERFLOW) != 0)
                    {
                    // Scan everything we have
                    RobotLog.vv(TAG, "observed OVERFLOW: event=0x%02x %s", event, observedFile);
                    List<File> files = AppUtil.getInstance().filesUnder(directory, new Predicate<File>()
                        {
                        @Override public boolean test(File file)
                            {
                            return true;
                            }
                        });
                    noteFiles(files);
                    }
                }
            });
        dirObserver.startWatching();
        }

    protected void stopObserver()
        {
        if (dirObserver != null)
            {
            dirObserver.stopWatching();
            dirObserver = null;
            }
        }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    public static void makeIndicatorFile(File directory)
        {
        final File tempFile = new File(directory, UUID.randomUUID().toString() + tmpMTPExtension);
        ReadWriteFile.writeFile(tempFile, "internal system utility file - you can delete this file");
        }

    public static boolean isIndicatorFile(File file)
        {
        return file.getName().endsWith(tmpMTPExtension);
        }

    public static void renoticeIndicatorFiles(File directory)
        {
        getInstance().noteFiles(AppUtil.getInstance().filesIn(directory, new Predicate<File>()
            {
            @Override public boolean test(File file)
                {
                return isIndicatorFile(file);
                }
            }));
        }

    /**
     * A public file has been updated or created. Inform the MediaScanner of this
     * fact so that it will show up in Media Transfer Protocol UIs on connected
     * desktop computers. This is necessary due to a very-long-standing bug in Android.
     *
     * @param file the file or directory that is to be noted
     * @see <a href="https://code.google.com/p/android/issues/detail?id=195362">Android bug</a>
     */
    public void noteFile(File file)
        {
        noteFiles(Collections.singletonList(file));
        }

    public void noteFiles(List<File> files)
        {
        // Note: this has issues if files are being created and deleted with high
        // frequency. Be aware. Usually that's ok since this is just trying to keep
        // any opened desktop UI up to date, and it's acceptable to err once in a while.
        //
        ArrayList<String> scanList = new ArrayList<String>();
        for (File file : files)
            {
            if (file.exists())
                {
                if (!file.isDirectory())
                    {
                    scanList.add(file.getAbsolutePath());
                    }
                else
                    {
                    // We hear odd things about running this on directories, so for now at least we don't.
                    }
                }
            else
                {
                if (file.getParentFile().exists())
                    {
                    // Attempt to remove the (now) non-existent file from MTP
                    scanList.add(file.getAbsolutePath());
                    }
                }
            }

        synchronized (concurrentClientLock)
            {
            pendingPaths.addAll(scanList);
            }
        }

    protected void notifyMTP()
        {
        final List<String> captured = new ArrayList<String>();
        synchronized (concurrentClientLock)
            {
            captured.addAll(pendingPaths);
            pendingPaths = new HashSet<String>();
            }
        if (!captured.isEmpty())
            {
            String[] scanArray = captured.toArray(new String[captured.size()]);

            // for (String s : scanArray) { RobotLog.vv(TAG, "scanning: %s" , s); }

            MediaScannerConnection.scanFile(
                context,
                scanArray,
                null,
                new MediaScannerConnection.OnScanCompletedListener()
                    {
                    @Override public void onScanCompleted(String path, Uri uri)
                        {
                        File file = new File(path);
                        if (uri == null)
                            {
                            // Scanning failed; try again later if there's actually something there
                            if (file.exists())
                                {
                                RobotLog.ww(TAG, "scanning failed; retrying later: %s", path);
                                }
                            }
                        else
                            {
                            // Once they've been scanned, remove indicator files.
                            if (isIndicatorFile(file))
                                {
                                file.delete();
                                }
                            }
                        }
                    });
            }
        }
    }
