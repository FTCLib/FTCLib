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

import java.io.File;

/**
 * {@link FileModifyObserver} allows one to monitor for changes to the contents of a file,
 * even across its deletions or recreations
 */
@SuppressWarnings("WeakerAccess")
public class FileModifyObserver
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = FileModifyObserver.class.getSimpleName();

    protected File                  monitoredFile;
    protected RecursiveFileObserver directoryObserver;
    protected RecursiveFileObserver fileObserver;
    protected Listener              listener;

    public interface Listener
        {
        void onFileChanged(int event, File file);
        }

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public FileModifyObserver(final File monitoredFile, final Listener listener)
        {
        this.monitoredFile = monitoredFile;
        this.listener = listener;

        int dirAccess = RecursiveFileObserver.CREATE | RecursiveFileObserver.DELETE | RecursiveFileObserver.MOVED_FROM | RecursiveFileObserver.MOVED_TO;
        this.directoryObserver = new RecursiveFileObserver(monitoredFile.getParentFile(), dirAccess, RecursiveFileObserver.Mode.NONRECURSVIVE, new RecursiveFileObserver.Listener()
            {
            @Override public void onEvent(int event, File file)
                {
                if (file.getName().equals(monitoredFile.getName()))
                    {
                    if ((event & (RecursiveFileObserver.CREATE | RecursiveFileObserver.MOVED_TO)) != 0)    // we may get more notifications than we ask for, so check
                        {
                        fileObserver.stopWatching();    // deal with potential rename
                        fileObserver.startWatching();

                        // Creation counts as a 'modification' here, since
                        // its unclear if we'd actually see a subsequent actual MODIFY
                        listener.onFileChanged(event, file);
                        }
                    else if ((event & (RecursiveFileObserver.DELETE | RecursiveFileObserver.MOVED_FROM)) != 0)
                        {
                        fileObserver.stopWatching();
                        }
                    }
                }
            });

        final int modifyAccess = RecursiveFileObserver.MODIFY;
        final int fileAccess   = modifyAccess;
        this.fileObserver = new RecursiveFileObserver(monitoredFile, fileAccess, RecursiveFileObserver.Mode.NONRECURSVIVE, new RecursiveFileObserver.Listener()
            {
            @Override public void onEvent(int event, File file)
                {
                if ((event & modifyAccess) != 0)
                    {
                    listener.onFileChanged(event, file);
                    }
                }
            });

        this.fileObserver.startWatching();
        this.directoryObserver.startWatching();
        }

    public void close()
        {
        directoryObserver.stopWatching();
        fileObserver.stopWatching();
        }

    //----------------------------------------------------------------------------------------------
    // Tests
    //----------------------------------------------------------------------------------------------

    /*static List<FileModifyObserver> observers = new ArrayList<>(10);

    static { test(); }

    public static void test()
        {
        File testModify = new File(OnBotJavaManager.buildDir, "testModify");
        ReadWriteFile.writeFile(testModify, "before");

        for (int i = 0; i < 2; i++)
            {
            final int j = i;
            observers.add(new FileModifyObserver(testModify, new FileModifyObserver.Listener()
                {
                @Override
                public void onFileChanged(int event, File file)
                    {
                    RobotLog.ww(TAG, "Test " + j + " called event=0x%08x", event);
                    }
                }));
            }

        ReadWriteFile.writeFile(testModify, "after");
        }*/
    }
