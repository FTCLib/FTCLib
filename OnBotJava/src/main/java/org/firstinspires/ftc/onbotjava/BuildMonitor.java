/*
 * Copyright (c) 2018 David Sargent
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted (subject to the limitations in the disclaimer below) provided that
 * the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * Neither the name of David Sargent nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior
 * written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
 * LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.firstinspires.ftc.onbotjava;

import androidx.annotation.NonNull;

import com.qualcomm.robotcore.util.ReadWriteFile;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.internal.files.FileModifyObserver;

import java.io.File;
import java.io.FileNotFoundException;

import fi.iki.elonen.NanoHTTPD;

import static org.firstinspires.ftc.onbotjava.StandardResponses.serverError;

public class BuildMonitor {
    private final Object buildCompletionNotifier = new Object();
    private final Object buildInformationUpdateLock = new Object();

    // Status src
    private static final File buildStartedFile = OnBotJavaManager.buildStartedFile;
    private static final File buildCompleteFile = OnBotJavaManager.buildCompleteFile;

    // Status variables
    private final FileModifyObserver runningObserver;
    private final FileModifyObserver completedObserver;
    private final OnBotJavaBroadcastManager webSocketBroadcastManager;

    /*
     *      <li>status: As the build progresses, several src are written. These src may be
     *                monitored for changes (see {@link FileModifyObserver}) as triggers to take
     *                actions to process the output of the build.
     *                <ol>
     *                      <li>buildStarted.txt:    updated when the build starts</li>
     *                      <li>buildSuccessful.txt: updated when the build has been determined
     *                                               to be successful</li>
     *                      <li>buildComplete.txt:   updated when the build finishes, whether
     *                                               or not it was successful</li>
     *                      <li>buildLog.txt:        contains unstructured output from the compiler
     *                                               and other build tools. Note that after successful
     *                                               builds, this will likely be empty.</li>
     *                </ol>
     *                To this list it might be reasonable to add buildLog.xml that had a structured
     *                version of buildLog.txt, but that has not yet been implemented. Feedback is welcome.
     *                </li>
     */
    // Status variables
    private CurrentBuildStatus buildStatus = CurrentBuildStatus.NOT_STARTED;
    private boolean closed = false;
    private long lastStartedBuild = -1;
    private String TAG = "ONBOTJAVA_BUILD_MONITOR";

    private enum CurrentBuildStatus {
        NOT_STARTED, PENDING, RUNNING, FAILED, SUCCESSFUL;

        /**
         * Returns {@code true} only if this build status denotes the immediate possibility for a build to be running
         */
        boolean isCurrentlyRunning() {
            return this == PENDING || this == RUNNING;
        }
    }

    BuildMonitor(OnBotJavaBroadcastManager broadcastManager) {
        webSocketBroadcastManager = broadcastManager;
        File statusDir = OnBotJavaManager.statusDir;
        if (!statusDir.isDirectory()) {
            statusDir.mkdirs();
        }

        runningObserver = new FileModifyObserver(buildStartedFile,
            new FileModifyObserver.Listener() {
                @Override
                public void onFileChanged(int event, File file) {
                    synchronized (buildInformationUpdateLock) {
                        // Turn this method into a no-op, if the build is already running. This is mainly to prevent
                        // log pollution
                        if (buildStatus == CurrentBuildStatus.RUNNING) {
                            return;
                        }

                        buildStatus = CurrentBuildStatus.RUNNING;
                    }

                    broadcastUpdateToWebsocket();
                    RobotLog.ii(TAG, "Build " + lastStartedBuild + " has successfully started");
                }
            }
        );

        completedObserver = new FileModifyObserver(buildCompleteFile, new
            FileModifyObserver.Listener() {
                @Override
                public void onFileChanged(int event, File file) {
                    final OnBotJavaManager.BuildStatus newBuildStatus = OnBotJavaManager.getBuildStatus();
                    synchronized (buildInformationUpdateLock) {
                        if (buildStatus == CurrentBuildStatus.SUCCESSFUL || buildStatus == CurrentBuildStatus.FAILED) {
                            RobotLog.vv(TAG, "Rejecting the build state transition to " + newBuildStatus + ", as current build state is " + buildStatus);
                            return;
                        }

                        buildStatus = newBuildStatus == OnBotJavaManager.BuildStatus.SUCCESSFUL ? CurrentBuildStatus.SUCCESSFUL : CurrentBuildStatus.FAILED;
                    }

                    synchronized (buildCompletionNotifier) {
                        buildCompletionNotifier.notifyAll();
                    }

                    broadcastUpdateToWebsocket();
                    RobotLog.ii(TAG, "Build " + lastStartedBuild + " completed with status " + newBuildStatus);
                }
            }
        );
    }

    private void broadcastUpdateToWebsocket() {
        webSocketBroadcastManager.broadcast(BuildStatusReportWebsocket.TAG, new BuildStatusReportWebsocket(buildStatus, lastStartedBuild));
    }

    public BuildStatusReport currentBuildStatus() {
        if (closed) {
            throw new IllegalStateException("BuildMonitor has been closed!");
        }
        synchronized (buildInformationUpdateLock) {
            return new BuildStatusReport(lastStartedBuild,
                    buildStatus.isCurrentlyRunning(),
                    buildStatus == CurrentBuildStatus.SUCCESSFUL);
        }
    }

    @NonNull
    public NanoHTTPD.Response currentBuildLog() {
        try {
            return OnBotJavaFileSystemUtils.serveFile(OnBotJavaManager.buildLogFile.getAbsolutePath());
        } catch (FileNotFoundException e) {
            return StandardResponses.serverError();
        }
    }

    public boolean waitForRunningBuildCompletion() {
        try {
            boolean isBuildCurrentlyRunning;
            synchronized (buildInformationUpdateLock) {
                isBuildCurrentlyRunning = buildStatus.isCurrentlyRunning();
                if (!isBuildCurrentlyRunning) {
                    return false;
                }
            }

            do {
                synchronized (buildCompletionNotifier) {
                    buildCompletionNotifier.wait();
                }

                synchronized (buildInformationUpdateLock) {
                    isBuildCurrentlyRunning = buildStatus.isCurrentlyRunning();
                }
            } while (isBuildCurrentlyRunning);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return true;
        }

        return false;
    }

    public void close() {
        closed = true;
        runningObserver.close();
        completedObserver.close();
    }

    @Override
    public void finalize() throws Throwable {
        super.finalize();
        if (closed) return;
        RobotLog.ww("FetchBuildStatus", "Did not call close(), running finalizer");
        close();
    }

    public long launchBuild() {
        final File buildStartFile = OnBotJavaManager.buildRequestFile;
        final long startTime = System.currentTimeMillis();
        synchronized (buildInformationUpdateLock) {
            if (buildStatus.isCurrentlyRunning()) {
                RobotLog.vv(TAG, "Refusing to launch build...another build is already running");
                return lastStartedBuild;
            }

            buildStatus = CurrentBuildStatus.PENDING;
            lastStartedBuild = startTime;
        }

        broadcastUpdateToWebsocket();
        ReadWriteFile.writeFile(buildStartFile, startTime + " - begin build");
        RobotLog.ii(TAG, "Build " + lastStartedBuild + " is pending");
        return startTime;
    }

    public static class BuildStatusReport {
        final boolean completed;
        final boolean running;
        final boolean successful;
        final long timestamp;
        final long startTimestamp;

        private BuildStatusReport(long startTimestamp, boolean running, boolean successful) {
            this.completed = !running;
            this.running = running;
            this.successful = successful;
            this.startTimestamp = startTimestamp;
            this.timestamp = System.nanoTime();
        }
    }

    public static class BuildStatusReportWebsocket {
        public static final transient String TAG = OnBotJavaProgrammingMode.WS_BUILD_STATUS;
        final CurrentBuildStatus status;
        final long startTimestamp;

        public BuildStatusReportWebsocket(CurrentBuildStatus status, long startTimestamp) {
            this.status = status;
            this.startTimestamp = startTimestamp;
        }
    }
}
