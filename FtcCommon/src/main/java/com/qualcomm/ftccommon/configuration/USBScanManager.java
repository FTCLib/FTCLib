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
package com.qualcomm.ftccommon.configuration;

import android.content.Context;
import android.support.annotation.NonNull;

import com.qualcomm.ftccommon.CommandList;
import com.qualcomm.hardware.HardwareDeviceManager;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.DeviceManager;
import com.qualcomm.robotcore.hardware.LynxModuleMetaList;
import com.qualcomm.robotcore.hardware.RobotCoreLynxUsbDevice;
import com.qualcomm.robotcore.hardware.ScannedDevices;
import com.qualcomm.robotcore.robocol.Command;
import com.qualcomm.robotcore.util.NextLock;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.SerialNumber;
import com.qualcomm.robotcore.util.ThreadPool;

import org.firstinspires.ftc.robotcore.external.function.Supplier;
import org.firstinspires.ftc.robotcore.internal.network.NetworkConnectionHandler;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * {@link USBScanManager} is responsible for issuing scans of USB devices. Scanning is
 * carried out asynchronously. When a scan is requested, any extant scan that is already in
 * progress is joined; otherwise a new scan is started. Scans are carried out either locally
 * or remotely, in a manner (largely) transparent to clients.
 *
 * We also analogously now carry out remote Lynx module discovery.
 */
@SuppressWarnings("WeakerAccess")
public class USBScanManager
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = FtcConfigurationActivity.TAG;

    public static final int msWaitDefault = 4000;

    protected Context                               context;
    protected boolean                               isRemoteConfig;
    protected ExecutorService                       executorService = null;
    protected ThreadPool.Singleton<ScannedDevices>  scanningSingleton = new ThreadPool.Singleton<ScannedDevices>();
    protected DeviceManager                         deviceManager;
    protected NextLock                              scanResultsSequence;
    protected final Object                          remoteScannedDevicesLock = new Object();
    protected ScannedDevices                        remoteScannedDevices;
    protected NetworkConnectionHandler              networkConnectionHandler = NetworkConnectionHandler.getInstance();
    protected final Map<String, LynxModuleDiscoveryState> lynxModuleDiscoveryStateMap = new ConcurrentHashMap<String, LynxModuleDiscoveryState>();  // concurrency is paranoia

    protected class LynxModuleDiscoveryState
        {
        protected SerialNumber              serialNumber;
        protected LynxModuleMetaList        remoteLynxModules;
        protected NextLock                  lynxDiscoverySequence = new NextLock();
        protected final Object              remoteLynxDiscoveryLock = new Object();
        protected ThreadPool.Singleton<LynxModuleMetaList> lynxDiscoverySingleton = new ThreadPool.Singleton<LynxModuleMetaList>();

        protected LynxModuleDiscoveryState(SerialNumber serialNumber)
            {
            this.serialNumber = serialNumber;
            this.remoteLynxModules = new LynxModuleMetaList(serialNumber);
            startExecutorService();
            }

        protected void startExecutorService()
            {
            ExecutorService executorService = USBScanManager.this.executorService;
            if (executorService != null)
                {
                this.lynxDiscoverySingleton.reset();
                this.lynxDiscoverySingleton.setService(executorService);
                }
            }
        }

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public USBScanManager(Context context, boolean isRemoteConfig) throws RobotCoreException
        {
        this.context = context;
        this.isRemoteConfig = isRemoteConfig;
        this.scanResultsSequence = new NextLock();

        if (!isRemoteConfig)
            {
            deviceManager = new HardwareDeviceManager(context, null);
            }
        }

    public void startExecutorService()
        {
        this.executorService = ThreadPool.newCachedThreadPool("USBScanManager");
        this.scanningSingleton.reset();
        this.scanningSingleton.setService(this.executorService);

        for (LynxModuleDiscoveryState state : lynxModuleDiscoveryStateMap.values())
            {
            state.startExecutorService();
            }
        }

    public void stopExecutorService()
        {
        this.executorService.shutdownNow();
        ThreadPool.awaitTerminationOrExitApplication(this.executorService, 5, TimeUnit.SECONDS, "USBScanManager service", "internal error");
        this.executorService = null;
        }

    //----------------------------------------------------------------------------------------------
    // Accessors
    //----------------------------------------------------------------------------------------------

    public ExecutorService getExecutorService()
        {
        return this.executorService;
        }

    public DeviceManager getDeviceManager()
        {
        return this.deviceManager;
        }

    //----------------------------------------------------------------------------------------------
    // Lynx Module discovery
    //----------------------------------------------------------------------------------------------

    LynxModuleDiscoveryState getDiscoveryState(SerialNumber serialNumber)
        {
        synchronized (lynxModuleDiscoveryStateMap)
            {
            LynxModuleDiscoveryState result = lynxModuleDiscoveryStateMap.get(serialNumber.getString());
            if (result == null)
                {
                result = new LynxModuleDiscoveryState(serialNumber);
                lynxModuleDiscoveryStateMap.put(serialNumber.getString(), result);
                }
            return result;
            }
        }

    public Supplier<LynxModuleMetaList> getLynxModuleMetaListSupplier(final SerialNumber serialNumber)
        {
        return new Supplier<LynxModuleMetaList>()
            {
            @Override public LynxModuleMetaList get()
                {
                LynxModuleMetaList result = null;
                try
                    {
                    result = startLynxModuleEnumerationIfNecessary(serialNumber).await();
                    }
                catch (InterruptedException e)
                    {
                    Thread.currentThread().interrupt();
                    }
                return result;
                }
            };
        }

    public ThreadPool.SingletonResult<LynxModuleMetaList> startLynxModuleEnumerationIfNecessary(final SerialNumber serialNumber)
        {
        final LynxModuleDiscoveryState discoveryState = getDiscoveryState(serialNumber);

        return discoveryState.lynxDiscoverySingleton.submit(msWaitDefault, new Callable<LynxModuleMetaList>()
            {
            @Override public LynxModuleMetaList call() throws InterruptedException
                {
                if (isRemoteConfig)
                    {
                    // Figure out whom we have to wait for
                    NextLock.Waiter waiter = discoveryState.lynxDiscoverySequence.getNextWaiter();

                    // Send a command to the RC to do a scan
                    RobotLog.vv(TAG, "sending remote lynx module discovery request...");
                    networkConnectionHandler.sendCommand(new Command(CommandList.CMD_DISCOVER_LYNX_MODULES, serialNumber.getString()));

                    // Wait for the result (forever, or until interrupted)
                    waiter.awaitNext();
                    RobotLog.vv(TAG, "...remote scan lynx module discovery completed.");

                    // Return same
                    synchronized (discoveryState.remoteLynxDiscoveryLock)
                        {
                        return discoveryState.remoteLynxModules;
                        }
                    }
                else
                    {
                    RobotLog.vv(TAG, "discovering lynx modules on lynx device=%s...", serialNumber);
                    LynxModuleMetaList localResult = null;
                    try
                        {
                        RobotCoreLynxUsbDevice lynxUsbDevice = null;
                        try {
                            lynxUsbDevice = deviceManager.createLynxUsbDevice(serialNumber, null);
                            localResult = lynxUsbDevice.discoverModules();
                            }
                        finally
                            {
                            if (lynxUsbDevice != null) lynxUsbDevice.close();
                            }
                        }
                    catch (RobotCoreException e)
                        {
                        RobotLog.ee(TAG, "discovering lynx modules threw exception: " + e.toString());
                        localResult = null;
                        }
                    finally
                        {
                        RobotLog.vv(TAG, "...discovering lynx modules complete: %s", localResult==null?"null":localResult.toString());
                        }
                    return localResult;
                    }
                }
            });
        }

    //----------------------------------------------------------------------------------------------
    // Scanning
    //----------------------------------------------------------------------------------------------

    public ThreadPool.SingletonResult<ScannedDevices> startDeviceScanIfNecessary()
        {
        return scanningSingleton.submit(msWaitDefault, new Callable<ScannedDevices>()
            {
            @Override public ScannedDevices call() throws InterruptedException
                {
                if (isRemoteConfig)
                    {
                    // Figure out whom we have to wait for
                    NextLock.Waiter waiter = scanResultsSequence.getNextWaiter();

                    // Send a command to the RC to do a scan
                    RobotLog.vv(TAG, "sending remote scan request...");
                    networkConnectionHandler.sendCommand(new Command(CommandList.CMD_SCAN));

                    // Wait for the result (forever, or until interrupted)
                    waiter.awaitNext();
                    RobotLog.vv(TAG, "...remote scan request completed.");

                    // Return same
                    synchronized (remoteScannedDevicesLock)
                        {
                        return remoteScannedDevices;
                        }
                    }
                else
                    {
                    RobotLog.vv(TAG, "scanning USB bus...");
                    ScannedDevices localResult = null;
                    try
                        {
                        localResult = deviceManager.scanForUsbDevices();
                        }
                    catch (RobotCoreException e)
                        {
                        RobotLog.ee(TAG, e,"USB bus scan threw exception");
                        localResult = null;
                        }
                    finally
                        {
                        RobotLog.vv(TAG, ".. scanning complete: %s", localResult==null?"null":localResult.keySet().toString());
                        }
                    return localResult;
                    }
                }
            });
        }

    public @NonNull ScannedDevices awaitScannedDevices() throws InterruptedException
        {
        ScannedDevices result = this.scanningSingleton.await();
        if (result == null)
            {
            RobotLog.vv(TAG, "USBScanManager.await() returning made-up scan result");
            result = new ScannedDevices();
            }
        return result;
        }

    public @NonNull LynxModuleMetaList awaitLynxModules(SerialNumber serialNumber) throws InterruptedException
        {
        LynxModuleDiscoveryState discoveryState = getDiscoveryState(serialNumber);
        LynxModuleMetaList result = discoveryState.lynxDiscoverySingleton.await();
        if (result == null)
            {
            RobotLog.vv(TAG, "USBScanManager.awaitLynxModules() returning made-up result");
            result = new LynxModuleMetaList(serialNumber);
            }
        return result;
        }

    public String packageCommandResponse(ScannedDevices scannedDevices)
        {
        return scannedDevices.toSerializationString();
        }

    public String packageCommandResponse(LynxModuleMetaList lynxModules)
        {
        return lynxModules.toSerializationString();
        }

    public void handleCommandScanResponse(String extra) throws RobotCoreException
        {
        RobotLog.vv(TAG, "handleCommandScanResponse()...");
        ScannedDevices scannedDevices = ScannedDevices.fromSerializationString(extra);
        synchronized (this.remoteScannedDevicesLock)
            {
            this.remoteScannedDevices = scannedDevices;
            this.scanResultsSequence.advanceNext();
            }
        RobotLog.vv(TAG, "...handleCommandScanResponse()");
        }

    public void handleCommandDiscoverLynxModulesResponse(String extra) throws RobotCoreException
        {
        RobotLog.vv(TAG, "handleCommandDiscoverLynxModulesResponse()...");
        LynxModuleMetaList lynxModules = LynxModuleMetaList.fromSerializationString(extra);
        LynxModuleDiscoveryState discoveryState = getDiscoveryState(lynxModules.serialNumber);
        synchronized (discoveryState.remoteLynxDiscoveryLock)
            {
            discoveryState.remoteLynxModules = lynxModules;
            discoveryState.lynxDiscoverySequence.advanceNext();
            }
        RobotLog.vv(TAG, "...handleCommandDiscoverLynxModulesResponse()");
        }
    }
