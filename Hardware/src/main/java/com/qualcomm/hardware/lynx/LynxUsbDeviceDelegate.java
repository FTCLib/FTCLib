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
package com.qualcomm.hardware.lynx;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.qualcomm.hardware.lynx.commands.LynxMessage;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.HardwareDeviceCloseOnTearDown;
import com.qualcomm.robotcore.hardware.LynxModuleMetaList;
import com.qualcomm.robotcore.hardware.usb.RobotUsbDevice;
import com.qualcomm.robotcore.hardware.usb.RobotUsbModule;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.SerialNumber;

import org.firstinspires.ftc.robotcore.internal.system.Assert;

/**
 * This delegation class simply forwards calls on, with the single exception that it turns
 * a local close into a delegated reference count.
 */
@SuppressWarnings("WeakerAccess")
public class LynxUsbDeviceDelegate implements LynxUsbDevice, HardwareDeviceCloseOnTearDown
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static String TAG = LynxUsbDeviceImpl.TAG;

    protected LynxUsbDeviceImpl delegate;
    protected boolean           releaseOnClose;
    protected boolean           isOpen;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public LynxUsbDeviceDelegate(LynxUsbDeviceImpl lynxUsbDevice)
        {
        delegate = lynxUsbDevice;
        releaseOnClose = true;
        isOpen = true;
        RobotLog.vv(TAG, "0x%08x on 0x%08x: new delegate to [%s]", hashCode(), delegate.hashCode(), delegate.getSerialNumber());
        }

    @Override public LynxUsbDeviceImpl getDelegationTarget()
        {
        return delegate;
        }

    @Override public synchronized void close()
        {
        if (releaseOnClose)
            {
            RobotLog.vv(TAG, "0x%08x on 0x%08x: releasing delegate to [%s]", hashCode(), delegate.hashCode(), delegate.getSerialNumber());
            releaseOnClose = false;
            delegate.releaseRef();
            isOpen = false;
            }
        else
            {
            RobotLog.ee(TAG, "0x%08x on 0x%08x: closing closed[%s]; ignored", hashCode(), delegate.hashCode(), delegate.getSerialNumber());
            }
        }

    protected void assertOpen()
        {
        if (!isOpen)
            {
            Assert.assertTrue(false, "0x%08x on 0x%08x: closed", hashCode(), delegate.hashCode());
            }
        }

    //----------------------------------------------------------------------------------------------
    // LynxUsbDevice
    //----------------------------------------------------------------------------------------------

    @Override public void disengage()
        {
        assertOpen();
        delegate.disengage();
        }

    @Override public void engage()
        {
        assertOpen();
        delegate.engage();
        }

    @Override public boolean isEngaged()
        {
        assertOpen();
        return delegate.isEngaged();
        }

    @Override public RobotUsbDevice getRobotUsbDevice()
        {
        assertOpen();
        return delegate.getRobotUsbDevice();
        }

    @Override public boolean isSystemSynthetic()
        {
        assertOpen();
        return delegate.isSystemSynthetic();
        }
    @Override public void setSystemSynthetic(boolean systemSynthetic)
        {
        assertOpen();
        delegate.setSystemSynthetic(systemSynthetic);
        }
    @Override public void failSafe()
        {
        assertOpen();
        delegate.failSafe();
        }
    @Override public void lockNetworkLockAcquisitions()
        {
        delegate.lockNetworkLockAcquisitions();
        }
    @Override public void changeModuleAddress(LynxModule module, int newAddress, Runnable runnable)
        {
        assertOpen();
        delegate.changeModuleAddress(module, newAddress, runnable);
        }
    @Override public void noteMissingModule(LynxModule module, String moduleName)
        {
        assertOpen();
        delegate.noteMissingModule(module, moduleName);
        }
    @Override public void addConfiguredModule(LynxModule module) throws RobotCoreException, InterruptedException, LynxNackException
        {
        assertOpen();
        delegate.addConfiguredModule(module);
        }
    @Nullable public LynxModule getConfiguredModule(int moduleAddress)
        {
        assertOpen();
        return delegate.getConfiguredModule(moduleAddress);
        }
    @Override public void removeConfiguredModule(LynxModule module)
        {
        assertOpen();
        delegate.removeConfiguredModule(module);
        }
    @Override public LynxModuleMetaList discoverModules() throws RobotCoreException, InterruptedException
        {
        assertOpen();
        return delegate.discoverModules();
        }
    @Override public void acquireNetworkTransmissionLock(@NonNull LynxMessage message) throws InterruptedException
        {
        assertOpen();
        delegate.acquireNetworkTransmissionLock(message);
        }
    @Override public void releaseNetworkTransmissionLock(@NonNull LynxMessage message) throws InterruptedException
        {
        assertOpen();
        delegate.releaseNetworkTransmissionLock(message);
        }
    @Override public void transmit(LynxMessage message) throws InterruptedException
        {
        assertOpen();
        delegate.transmit(message);
        }

    //----------------------------------------------------------------------------------------------
    // HardwareDevice
    //----------------------------------------------------------------------------------------------

    @Override public String getDeviceName()
        {
        assertOpen();
        return delegate.getDeviceName();
        }
    @Override public String getConnectionInfo()
        {
        assertOpen();
        return delegate.getConnectionInfo();
        }
    @Override public int getVersion()
        {
        assertOpen();
        return delegate.getVersion();
        }
    @Override public Manufacturer getManufacturer()
        {
        assertOpen();
        return delegate.getManufacturer();
        }
    @Override public void resetDeviceConfigurationForOpMode()
        {
        assertOpen();
        delegate.resetDeviceConfigurationForOpMode();
        }

    //----------------------------------------------------------------------------------------------
    // SyncdDevice
    //----------------------------------------------------------------------------------------------

    @Override public ShutdownReason getShutdownReason()
        {
        assertOpen();
        return delegate.getShutdownReason();
        }
    @Override public void setOwner(RobotUsbModule owner)
        {
        assertOpen();
        delegate.setOwner(owner);
        }
    @Override public RobotUsbModule getOwner()
        {
        assertOpen();
        return delegate.getOwner();
        }

    //----------------------------------------------------------------------------------------------
    // RobotArmingStateNotifier
    //----------------------------------------------------------------------------------------------

    @Override public SerialNumber getSerialNumber()
        {
        assertOpen();
        return delegate.getSerialNumber();
        }

    @Override public ARMINGSTATE getArmingState()
        {
        assertOpen();
        return delegate.getArmingState();
        }

    @Override public void registerCallback(Callback callback, boolean doInitialCallback)
        {
        assertOpen();
        delegate.registerCallback(callback, doInitialCallback);
        }

    @Override public void unregisterCallback(Callback callback)
        {
        assertOpen();
        delegate.unregisterCallback(callback);
        }

    //----------------------------------------------------------------------------------------------
    // RobotUsbModule
    //----------------------------------------------------------------------------------------------

    @Override public void arm() throws RobotCoreException, InterruptedException
        {
        assertOpen();
        delegate.arm();
        }
    @Override public void pretend() throws RobotCoreException, InterruptedException
        {
        assertOpen();
        delegate.pretend();
        }
    @Override public void armOrPretend() throws RobotCoreException, InterruptedException
        {
        assertOpen();
        delegate.armOrPretend();
        }
    @Override public void disarm() throws RobotCoreException, InterruptedException
        {
        assertOpen();
        delegate.disarm();
        }

    //----------------------------------------------------------------------------------------------
    // GlobalWarmingSource
    //----------------------------------------------------------------------------------------------

    @Override public String getGlobalWarning()
        {
        assertOpen();
        return delegate.getGlobalWarning();
        }
    @Override public void suppressGlobalWarning(boolean suppress)
        {
        assertOpen();
        delegate.suppressGlobalWarning(suppress);
        }
    @Override public void setGlobalWarning(String warning)
        {
        assertOpen();
        delegate.setGlobalWarning(warning);
        }
    @Override public void clearGlobalWarning()
        {
        assertOpen();
        delegate.clearGlobalWarning();
        }
    }
