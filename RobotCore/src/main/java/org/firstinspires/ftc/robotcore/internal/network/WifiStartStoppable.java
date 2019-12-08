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
package org.firstinspires.ftc.robotcore.internal.network;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pManager;
import androidx.annotation.NonNull;

import com.qualcomm.robotcore.R;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.external.Func;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.system.Assert;

import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

/**
 * {@link WifiStartStoppable} is a utility base class for managing services which can be started
 * and stopped in a reference counting manner.
 */
@SuppressWarnings("WeakerAccess")
public abstract class WifiStartStoppable
    {
    //------------------------------------------------------------------------------------------
    // State
    // Lock order: startStopLock, completionLock
    //------------------------------------------------------------------------------------------

    public abstract String getTag();
    public boolean DEBUG         = true;
    public boolean DEBUG_VERBOSE = false;

    protected final Object          startStopLock               = new Object();
    protected int                   startCount                  = 0;
    protected final WifiDirectAgent wifiDirectAgent;
    protected final StartResult     wifiDirectAgentStarted      = new StartResult();

    private   final ReentrantLock completionLock              = new ReentrantLock();
    private   boolean             completionSuccess           = true;
    private   Semaphore           completionSemaphore         = new Semaphore(0);

    protected @NonNull ActionListenerFailure failureReason  = ActionListenerFailure.UNKNOWN;

    //------------------------------------------------------------------------------------------
    // Construction
    //------------------------------------------------------------------------------------------

    protected WifiStartStoppable(WifiDirectAgent wifiDirectAgent)
        {
        this.wifiDirectAgent = wifiDirectAgent;
        }

    protected WifiStartStoppable(int dummy)
        {
        this.wifiDirectAgent = (WifiDirectAgent)this;
        }

    //------------------------------------------------------------------------------------------
    // Accessing
    //------------------------------------------------------------------------------------------

    public WifiDirectAgent getWifiDirectAgent()
        {
        return wifiDirectAgent;
        }

    //------------------------------------------------------------------------------------------
    // Logging & tracing
    //------------------------------------------------------------------------------------------

    protected void trace(String functionName, Runnable runnable)
        {
        trace(functionName, true, runnable);
        }

    protected void trace(String functionName, boolean enabled, Runnable runnable)
        {
        if (enabled) RobotLog.vv(getTag(), "%s()...", functionName);
        try {
            runnable.run();
            }
        finally
            {
            if (enabled) RobotLog.vv(getTag(), "...%s()", functionName);
            }
        }

    protected <T> T trace(String functionName, boolean enabled, Func<T> func)
        {
        if (enabled) RobotLog.vv(getTag(), "%s()...", functionName);
        try {
            return func.value();
            }
        finally
            {
            if (enabled) RobotLog.vv(getTag(), "...%s()", functionName);
            }
        }

    //------------------------------------------------------------------------------------------
    // Completion management
    //------------------------------------------------------------------------------------------

    protected <T> T lockCompletion(T defaultValue, Func<T> action)
        {
        T result = defaultValue;
        try {
            completionLock.lockInterruptibly();
            try {
                result = action.value();
                }
            finally
                {
                completionLock.unlock();
                }
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            }
        return result;
        }

    protected boolean isCompletionLockHeld()
        {
        return completionLock.isHeldByCurrentThread();
        }

    protected boolean resetCompletion()
        {
        Assert.assertTrue(isCompletionLockHeld());
        completionSuccess = true;
        completionSemaphore = new Semaphore(0);
        return true;
        }

    protected boolean waitForCompletion() throws InterruptedException
        {
        Assert.assertNotNull(wifiDirectAgent);
        Assert.assertFalse(wifiDirectAgent.isLooperThread());
        Assert.assertTrue(isCompletionLockHeld());
        completionSemaphore.acquire();
        return completionSuccess;
        }

    protected void releaseCompletion(boolean success)
        {
        Assert.assertNotNull(wifiDirectAgent);
        Assert.assertTrue(wifiDirectAgent.isLooperThread());
        Assert.assertFalse(isCompletionLockHeld());
        completionSuccess = success;
        completionSemaphore.release();
        }

    protected boolean receivedInterrupt(InterruptedException e)
        {
        Thread.currentThread().interrupt();
        return false;
        }

    protected boolean receivedCompletionInterrupt(InterruptedException e)
        {
        Assert.assertTrue(isCompletionLockHeld());
        return receivedInterrupt(e);
        }

    //------------------------------------------------------------------------------------------
    // Starting
    //------------------------------------------------------------------------------------------

    public StartResult start()
        {
        StartResult startResult = new StartResult();
        if (start(startResult))
            {
            return startResult;
            }
        else
            return null;
        }

    public boolean start(StartResult startResult)
        {
        try
            {
            synchronized (startStopLock)
                {
                Assert.assertTrue(startResult.getStartStoppable()==null || startResult.getStartStoppable()==this);

                boolean startedHere = false;
                boolean localSuccess = true;
                if (DEBUG_VERBOSE) RobotLog.vv(getTag(), "start() count=%d...", startCount);
                if (0 == startCount++ || startIsIdempotent())
                    {
                    startedHere = localSuccess = callDoStart();
                    }
                startResult.setStartStoppable(this);

                if (startedHere) startResult.incrementStartCount();
                if (!startIsRefCounted()) startResult.setStartCount(Math.min(1, startResult.getStartCount()));

                return localSuccess;
                }
            }
        finally
            {
            if (DEBUG_VERBOSE) RobotLog.vv(getTag(), "...start()");
            }
        }

    protected boolean startIsRefCounted()
        {
        return true;
        }

    protected boolean startIsIdempotent()
        {
        return false;
        }

    protected abstract boolean doStart() throws InterruptedException;

    protected boolean callDoStart()
        {
        return trace("doStart", DEBUG, new Func<Boolean>()
            {
            @Override public Boolean value()
                {
                return lockCompletion(false, new Func<Boolean>()
                    {
                    @Override public Boolean value()
                        {
                        try {
                            return doStart();
                            }
                        catch (InterruptedException e)
                            {
                            Thread.currentThread().interrupt();
                            return false;
                            }
                        }
                    });
                }
            });
        }

    //------------------------------------------------------------------------------------------
    // Stopping
    //------------------------------------------------------------------------------------------

    public void stop(StartResult startResult)
        {
        Assert.assertTrue((startResult.getStartStoppable()==null && startResult.getStartCount()==0) || startResult.getStartStoppable()==this);
        synchronized (startStopLock)
            {
            while (startResult.getStartCount() > 0)
                {
                startResult.decrementStartCount();
                internalStop();
                }
            }
        Assert.assertTrue(startResult.getStartCount()==0);  // invariant
        }

    public void terminate()
        {
        synchronized (startStopLock)
            {
            if (startCount > 0)
                {
                startCount = 1;
                internalStop();
                }
            }
        }

    protected void stopDueToFailure()
        {
        internalStop();
        }

    public void internalStop()
        {
        try
            {
            synchronized (startStopLock)
                {
                if (DEBUG_VERBOSE) RobotLog.vv(getTag(), "stop() count=%d...", startCount);
                if (startCount > 0)
                    {
                    if (--startCount == 0)
                        {
                        callDoStop();
                        }
                    }
                }
            }
        finally
            {
            if (DEBUG_VERBOSE) RobotLog.vv(getTag(), "...stop()");
            }
        }

    protected abstract void doStop() throws InterruptedException;

    protected void callDoStop()
        {
        trace("doStop", DEBUG, new Runnable()
            {
            @Override public void run()
                {
                lockCompletion(null, new Func<Void>()
                    {
                    @Override public Void value()
                        {
                        try {
                            doStop();
                            }
                        catch (InterruptedException e)
                            {
                            Thread.currentThread().interrupt();
                            }
                        return null;
                        }
                    });
                }
            });
        }

    //------------------------------------------------------------------------------------------
    // Restarting
    //------------------------------------------------------------------------------------------

    public void restart()
        {
        if (DEBUG) RobotLog.vv(getTag(), "restart()...");
        try
            {
            synchronized (startStopLock)
                {
                if (startCount > 0)
                    {
                    callDoStop();
                    callDoStart();
                    }
                }
            }
        finally
            {
            if (DEBUG) RobotLog.vv(getTag(), "...restart()");
            }
        }

    //----------------------------------------------------------------------------------------------
    // Failures
    //----------------------------------------------------------------------------------------------

    public ActionListenerFailure getActionListenerFailureReason()
        {
        return failureReason;
        }

    public enum ActionListenerFailure
        {
        UNKNOWN, P2P_UNSUPPORTED, ERROR, BUSY, NO_SERVICE_REQUESTS, WIFI_DISABLED;

        public static ActionListenerFailure from(int reason, WifiDirectAgent wifiDirectAgent)
            {
            switch (reason)
                {
                default:                                    return UNKNOWN;
                case WifiP2pManager.P2P_UNSUPPORTED:        return P2P_UNSUPPORTED;
                case WifiP2pManager.ERROR:                  return ERROR;
                case WifiP2pManager.NO_SERVICE_REQUESTS:    return NO_SERVICE_REQUESTS;
                case WifiP2pManager.BUSY:
                    {
                    // Be more helpful when we can be
                    WifiState state = wifiDirectAgent!= null ? wifiDirectAgent.getWifiState() : WifiState.UNKNOWN;
                    boolean disabled = state==WifiState.DISABLED || state==WifiState.DISABLING;
                    return disabled ? WIFI_DISABLED : BUSY;
                    }
                }
            }

        public String toString()
            {
            Context context = AppUtil.getInstance().getApplication();
            switch (this)
                {
                case P2P_UNSUPPORTED:       return context.getString(R.string.actionlistenerfailure_nop2p);
                case WIFI_DISABLED:         return context.getString(R.string.actionlistenerfailure_nowifi);
                case BUSY:                  return context.getString(R.string.actionlistenerfailure_busy);
                case ERROR:                 return context.getString(R.string.actionlistenerfailure_error);
                case NO_SERVICE_REQUESTS:   return context.getString(R.string.actionlistenerfailure_nosevicerequests);
                default:                    return context.getString(R.string.actionlistenerfailure_unknown);
                }
            }
        }
    }
