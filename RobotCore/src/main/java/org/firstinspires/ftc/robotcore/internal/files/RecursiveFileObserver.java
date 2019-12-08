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
import androidx.annotation.NonNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * A FileObserver that (optionally) observes all the files/folders within given directory
 * recursively. It automatically starts/stops monitoring new folders/files
 * created after starting the watch. It can also monitor individual files. We also
 * make the paths easier to deal with by ALWAYS dealing in the currency of absolute paths.
 *
 * Adapted from https://gist.github.com/gitanuj/888ef7592be1d3f617f6
 */
@SuppressWarnings("WeakerAccess")
public class RecursiveFileObserver
    {
    //----------------------------------------------------------------------------------------------
    // Constants
    //----------------------------------------------------------------------------------------------

    public static final int ACCESS = FileObserver.ACCESS;
    public static final int MODIFY = FileObserver.MODIFY;
    public static final int ATTRIB = FileObserver.ATTRIB;
    public static final int CLOSE_WRITE = FileObserver.CLOSE_WRITE;
    public static final int CLOSE_NOWRITE = FileObserver.CLOSE_NOWRITE;
    public static final int OPEN = FileObserver.OPEN;
    public static final int MOVED_FROM = FileObserver.MOVED_FROM;
    public static final int MOVED_TO = FileObserver.MOVED_TO;
    public static final int CREATE = FileObserver.CREATE;
    public static final int DELETE = FileObserver.DELETE;
    public static final int DELETE_SELF = FileObserver.DELETE_SELF;
    public static final int MOVE_SELF = FileObserver.MOVE_SELF;
    public static final int ALL_FILE_OBSERVER_EVENTS = FileObserver.ALL_EVENTS;

    // FileObserver is a thin wrapper around inotify. However, there are events
    // in inotify that we need to see that FileObserver doesn't directly document:
    //  see http://man7.org/linux/man-pages/man7/inotify.7.html
    // In particular, for robustness we need to deal with the overflow condition.

    // the following are legal events.  they are sent as needed to any watch
    public static final int IN_UNMOUNT      = 0x00002000;	// Backing fs was unmounted
    public static final int IN_Q_OVERFLOW   = 0x00004000;	// Event queued overflowed
    public static final int IN_IGNORED		= 0x00008000;	// File was ignored

    // special flags
    /*public static final int IN_ONLYDIR	= 0x01000000;	// only watch the path if it is a directory
    public static final int IN_DONT_FOLLOW	= 0x02000000;	// don't follow a sym link
    public static final int IN_EXCL_UNLINK	= 0x04000000;	// exclude events on unlinked objects
    public static final int IN_MASK_ADD		= 0x20000000;	// add to the mask of an already existing watch
    public static final int IN_ISDIR		= 0x40000000;	// event occurred against dir
    public static final int IN_ONESHOT		= 0x80000000;	// only send event once*/

    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "FileSystemObserver";

    /** maps absolute path to the watcher we have for same, if any */
    protected final Map<String, SingleDirOrFileObserver> observers = new HashMap<>();
    protected final String                     rootPath;
    protected final int                        mask;
    protected final Mode                       mode;
    protected @NonNull final Listener          listener;

    public interface Listener
        {
        /**
         * @param file  the absolute path to the file on which the event occurred
         */
        void onEvent(int event, File file);
        }

    public enum Mode
        {
        RECURSIVE, NONRECURSVIVE;
        }

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public RecursiveFileObserver(File file, int mask, Mode mode, @NonNull Listener listener)
        {
        this(file.getAbsolutePath(), mask, mode, listener);
        }

    public RecursiveFileObserver(String path, int mask, Mode mode, @NonNull Listener listener)
        {
        this.rootPath = path;
        this.mask = mask;
        this.mode = mode;
        this.listener = listener;
        }

    //----------------------------------------------------------------------------------------------
    // FileObserver implementation
    //----------------------------------------------------------------------------------------------

    public void startWatching()
        {
        Stack<String> stack = new Stack<>();
        stack.push(rootPath);

        // Recursively watch all child directories
        while (!stack.empty())
            {
            String parent = stack.pop();
            startWatching(parent.equals(rootPath), parent);

            if (mode == Mode.RECURSIVE)
                {
                File path = new File(parent);
                File[] files = path.listFiles();
                if (files != null)
                    {
                    for (File file : files)
                        {
                        if (isWatchableDirectory(file))
                            {
                            stack.push(file.getAbsolutePath());
                            }
                        }
                    }
                }
            }
        }

    public void stopWatching()
        {
        synchronized (observers)
            {
            for (SingleDirOrFileObserver observer : observers.values())
                {
                observer.stopWatching();
                }
            observers.clear();
            }
        }

    protected class FileObserverListener implements FileObserverManager.Listener
        {
        @Override public void onEvent(int event, String path)
            {
            File file;
            if (path == null)
                {
                file = new File(RecursiveFileObserver.this.rootPath);
                }
            else
                {
                file = new File(RecursiveFileObserver.this.rootPath, path);
                }
            RecursiveFileObserver.this.notify(event, file);
            }
        }

    //----------------------------------------------------------------------------------------------
    // internal implementation
    //----------------------------------------------------------------------------------------------

    protected void startWatching(boolean isRoot, String path)
        {
        synchronized (observers)
            {
            stopWatching(path);
            SingleDirOrFileObserver observer = new SingleDirOrFileObserver(isRoot, path);
            observer.startWatching();
            observers.put(path, observer);
            }
        }

    protected static boolean isWatchableDirectory(File file)
        {
        return file.isDirectory() && !file.getName().equals(".") && !file.getName().equals("..");
        }

    protected void stopWatching(String path)
        {
        synchronized (observers)
            {
            SingleDirOrFileObserver observer = observers.remove(path);
            if (observer != null)
                {
                observer.stopWatching();
                }
            }
        }

    protected void notify(int event, File file)
        {
        listener.onEvent(event & FileObserver.ALL_EVENTS, file);
        }

    protected class SingleDirOrFileObserver implements FileObserverManager.Listener
        {
        protected final FileObserver fileObserver;
        protected final boolean isRoot;
        protected final String thisPath;

        public SingleDirOrFileObserver(boolean isRoot, String thisPath)
            {
            int mask = RecursiveFileObserver.this.mask | CREATE | DELETE_SELF;
            this.fileObserver = FileObserverManager.from(thisPath, mask, this);
            this.isRoot = isRoot;
            this.thisPath = thisPath;
            }

        public void startWatching()
            {
            fileObserver.startWatching();
            }

        public void stopWatching()
            {
            fileObserver.stopWatching();
            }

        @Override
        public void onEvent(int event, String path)
            {
            File file;
            if (path == null || path.isEmpty()) // empty check is paranoia, null will (I think) occur
                {
                file = new File(thisPath);
                }
            else
                {
                file = new File(thisPath, path);
                }

            switch (event & FileObserver.ALL_EVENTS)
                {
                case DELETE_SELF:
                    RecursiveFileObserver.this.stopWatching(thisPath);
                    // For non roots, our parent will see the delete: no point in notifying twice
                    if (isRoot)
                        {
                        RecursiveFileObserver.this.notify(event, file);
                        }
                    break;
                case CREATE:
                    if (RecursiveFileObserver.this.mode==Mode.RECURSIVE && isWatchableDirectory(file))
                        {
                        RecursiveFileObserver.this.startWatching(false, file.getAbsolutePath());
                        }
                    /* fall through */
                default:
                    RecursiveFileObserver.this.notify(event, file);
                }
            }
        }
    }