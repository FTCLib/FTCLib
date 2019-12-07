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

import android.os.FileObserver;

import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.WeakReferenceSet;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link FileObserverManager} is a dispenser for {@link android.os.FileObserver} objects.
 * Use {@link FileObserverManager#from(String, int, Listener)} here to make FileObservers instead
 * of instantiating (subclasses of) FileObserver directly.
 *
 * The underlying intent here is to work around the observation that having multiple FileObserver
 * instances on any given inode path seems to only notify on one of them. We haven't analyzed
 * exactly what the issue is, but we avoid the problem by not doing that.
 */
@SuppressWarnings("WeakerAccess")
public class FileObserverManager
    {
    //----------------------------------------------------------------------------------------------
    // Types
    //----------------------------------------------------------------------------------------------

    public interface Listener
        {
        void onEvent(int event, String path);
        }

    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public final static String TAG = FileObserverManager.class.getSimpleName();

    protected final static Map<String, WeakReference<OmniscientObserver>> omnicientObservers = new HashMap<>();

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public static FileObserver from(final String inodePath, final int mask, final Listener listener)
        {
        OmniscientObserver omniscientObserver = null;

        synchronized (omnicientObservers)
            {
            File file = new File(inodePath);
            String path;
            try {
                path = file.getCanonicalPath();
                }
            catch (IOException e)
                {
                RobotLog.ww(TAG, "canonical path failed; using absolute instead: abspath=%s", file.getAbsolutePath());
                path = file.getAbsolutePath();
                }

            WeakReference<OmniscientObserver> weakReference = omnicientObservers.get(path);
            if (weakReference != null)
                {
                omniscientObserver = weakReference.get();
                }
            if (omniscientObserver == null)
                {
                omniscientObserver = new OmniscientObserver(path);
                omnicientObservers.put(path, new WeakReference<OmniscientObserver>(omniscientObserver));
                }
            }

        return new FakeObserver(omniscientObserver, mask, listener);
        }

    //----------------------------------------------------------------------------------------------
    // FakeObserver: fake in that it delegates all public function and uses nothing in FileObserver
    //----------------------------------------------------------------------------------------------

    protected static class FakeObserver extends FileObserver
        {
        protected final OmniscientObserver omniscientObserver;
        protected final int mask;
        protected final Listener listener;
        protected boolean isWatching;

        protected FakeObserver(OmniscientObserver omniscientObserver, int mask, Listener listener)
            {
            super("/dev/null", 0);
            this.omniscientObserver = omniscientObserver;
            this.mask = mask;
            this.listener = listener;
            this.isWatching = false;

            omniscientObserver.addFakeObserver(this);
            }

        @Override public void onEvent(int event, String path)
            {
            if ((event & mask) != 0)
                {
                listener.onEvent(event, path);
                }
            }

        @Override public void startWatching()
            {
            omniscientObserver.startWatching(this);
            }

        @Override public void stopWatching()
            {
            omniscientObserver.stopWatching(this);
            }

        @Override protected void finalize()
            {
            omniscientObserver.removeFakeObserver(this);
            super.finalize();
            }
        }

    //----------------------------------------------------------------------------------------------
    // OmniscientObserver
    //----------------------------------------------------------------------------------------------

    protected static class OmniscientObserver
        {
        //------------------------------------------------------------------------------------------
        // State
        //------------------------------------------------------------------------------------------

        protected final String inodePath;
        protected final AtomicInteger startCount = new AtomicInteger(0);
        protected final WeakReferenceSet<FakeObserver> fakeObservers = new WeakReferenceSet<>();

        protected FileObserver fileObserver;
        protected int mask;

        // We avoid MODIFY and ACCESS for performance reasons, but at the cost of perhaps missing events
        // we might otherwise not if we upgrade. It seems the right tradeoff.
        protected static final int defaultMask = FileObserver.ALL_EVENTS & ~(FileObserver.MODIFY | FileObserver.ACCESS);

        //------------------------------------------------------------------------------------------
        // Construction
        //------------------------------------------------------------------------------------------

        public OmniscientObserver(String inodePath)
            {
            // RobotLog.vv(TAG, "OmniscientObserver(%s)", inodePath);
            this.inodePath = inodePath;
            this.mask = defaultMask;
            this.fileObserver = newFileObserver(inodePath, this.mask);
            }

        @Override protected void finalize() throws Throwable
            {
            synchronized (omnicientObservers)
                {
                omnicientObservers.remove(inodePath);
                }
            super.finalize();
            }

        protected FileObserver newFileObserver(String path, int mask)
            {
            return new FileObserver(path, mask)
                {
                @Override public void onEvent(int event, String path)
                    {
                    OmniscientObserver.this.onEvent(event, path);
                    }
                };
            }

        public void addFakeObserver(FakeObserver fakeObserver)
            {
            synchronized (fakeObservers)
                {
                // RobotLog.vv(TAG, "adding fakeObserver inodePath=%s id=0x%08x", inodePath, fakeObserver.hashCode());

                fakeObservers.add(fakeObserver);
                if ((fakeObserver.mask & ~this.mask) != 0)
                    {
                    // He wants more things that we we have so far chosen to monitor. Upgrade.
                    // Wish we could do w/o possibly missing events, but we don't know how.
                    int newMask = fakeObserver.mask | this.mask;
                    boolean amWatching = startCount.get() > 0;
                    if (amWatching)
                        {
                        RobotLog.ww(TAG, "upgrading mask: path=%s old=0x%08x new=0x%08x: might possibly miss event", inodePath, this.mask, newMask);
                        fileObserver.stopWatching();
                        }
                    fileObserver = newFileObserver(inodePath, newMask);
                    if (amWatching)
                        {
                        fileObserver.startWatching();
                        }
                    }
                }
            }

        public void removeFakeObserver(FakeObserver fakeObserver)
            {
            synchronized (fakeObservers)
                {
                stopWatching(fakeObserver);
                fakeObservers.remove(fakeObserver);
                }
            }

        //------------------------------------------------------------------------------------------
        // Signalling
        //------------------------------------------------------------------------------------------

        protected void startWatching(FakeObserver observer)
            {
            synchronized (fakeObservers)
                {
                if (!observer.isWatching)
                    {
                    observer.isWatching = true;
                    if (startCount.getAndIncrement() == 0)
                        {
                        // RobotLog.vv(TAG, "OmniscientObserver(%s).startWatching()", inodePath);
                        fileObserver.startWatching();
                        }
                    }
                }
            }

        protected void stopWatching(FakeObserver observer)
            {
            synchronized (fakeObservers)
                {
                if (observer.isWatching)
                    {
                    if (startCount.decrementAndGet() == 0)
                        {
                        // RobotLog.vv(TAG, "OmniscientObserver(%s).stopWatching()", inodePath);
                        fileObserver.stopWatching();
                        }
                    observer.isWatching = false;
                    }
                }
            }

        protected void onEvent(int event, String path)
            {
            synchronized (fakeObservers)
                {
                // if ((event & FileObserver.MODIFY) != 0) RobotLog.vv(TAG, "OmniscientObserver(%s).onEvent(0x%08x, %s)", inodePath, event, path);
                for (FakeObserver fakeObserver : fakeObservers)
                    {
                    fakeObserver.onEvent(event, path);
                    }
                }
            }
        }

    }
