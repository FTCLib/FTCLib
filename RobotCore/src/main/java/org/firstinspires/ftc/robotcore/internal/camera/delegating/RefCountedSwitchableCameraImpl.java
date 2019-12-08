/*
Copyright (c) 2018 Robert Atkinson

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
package org.firstinspires.ftc.robotcore.internal.camera.delegating;

import androidx.annotation.NonNull;

import org.firstinspires.ftc.robotcore.external.android.util.Size;
import org.firstinspires.ftc.robotcore.external.function.Continuation;
import org.firstinspires.ftc.robotcore.external.hardware.camera.Camera;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraName;
import org.firstinspires.ftc.robotcore.internal.camera.CameraManagerImpl;
import org.firstinspires.ftc.robotcore.internal.camera.CameraManagerInternal;
import org.firstinspires.ftc.robotcore.internal.camera.CameraState;
import org.firstinspires.ftc.robotcore.internal.system.Assert;
import org.firstinspires.ftc.robotcore.internal.system.Misc;
import org.firstinspires.ftc.robotcore.internal.camera.calibration.CameraCalibration;
import org.firstinspires.ftc.robotcore.internal.camera.calibration.CameraCalibrationIdentity;
import org.firstinspires.ftc.robotcore.internal.camera.calibration.CameraCalibrationManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A {@link RefCountedSwitchableCameraImpl} is a camera implementation that can dynamically
 * switch between actual camera instances on the fly while the camera is open. This works best
 * if all the cameras in question are of the same camera model.
 */
@SuppressWarnings("WeakerAccess")
public class RefCountedSwitchableCameraImpl extends DelegatingCamera implements RefCountedSwitchableCamera
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "SwitchableCamImpl";
    public String getTag() { return TAG; }

    protected final SwitchableCameraName    selfSwitchableCameraName;
    protected CountDownLatch                awaitAllCamerasOpenOrOpenFailed;

    /** NB: We don't ourselves use {@link #delegatedCamera}, much, anyway. */
    protected CameraName                    activeCameraName;
    protected final Map<CameraName, SwitchableMemberInfo> cameraInfos = new HashMap<>();

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public RefCountedSwitchableCameraImpl(
            CameraManagerInternal cameraManager,
            SwitchableCameraName switchableCameraName,
            CameraName[] cameraNames,
            @NonNull final Continuation<? extends Camera.StateCallback> userContinuation)
        {
        super(cameraManager, switchableCameraName, userContinuation);
        this.selfSwitchableCameraName = switchableCameraName;
        for (CameraName cameraName : cameraNames)
            {
            cameraInfos.put(cameraName, new SwitchableMemberInfo(this, cameraName));
            }
        activeCameraName = cameraNames[0]; // as reasonable default as any
        }

    @Override protected void closeDelegatedCameras()
        {
        synchronized (outerLock)
            {
            for (SwitchableMemberInfo cameraInfo : cameraInfos.values())
                {
                cameraInfo.closeCamera();
                }
            }
        }

    //----------------------------------------------------------------------------------------------
    // Opening and closing
    //----------------------------------------------------------------------------------------------

    @Override protected void createSelfCamera()
        {
        selfCamera = new SwitchableCameraImpl(this); // takes one ref on us, which will be released when selfCamera is closed
        }

    /**
     * Called by {@link CameraManagerImpl}.
     */
    public void openAssumingPermission(long duration, TimeUnit timeUnit)
        {
        synchronized (outerLock)
            {
            switch (selfState)
                {
                case FailedOpen:
                case Closed:
                case OpenNotStarted:
                case OpenAndStarted:
                case Disconnected:
                    throw Misc.illegalStateException("attempt to open camera %s in state %s", selfCameraName, selfState);
                case Nascent:
                    Assert.assertNull(delegatedCamera);
                    Camera.OpenFailure failureReason = Camera.OpenFailure.None;
                    awaitAllCamerasOpenOrOpenFailed = new CountDownLatch(cameraInfos.size());
                    try {
                        // Open all the cameras
                        for (SwitchableMemberInfo cameraInfo : cameraInfos.values())
                            {
                            tracer.trace("async opening %s", cameraInfo.getCameraName());
                            cameraManager.asyncOpenCameraAssumingPermission(cameraInfo.getCameraName(), Continuation.create(serialThreadPool, cameraInfo), duration, timeUnit);
                            }

                        // Wait for all the async requests to either open or fail to open
                        awaitAllCamerasOpenOrOpenFailed.await();

                        // Ok, did everything open?
                        if (allCamerasOpen())
                            {
                            changeDelegatedCamera(getCurrentInfo().getCamera());
                            openSelfAndReport();
                            }
                        }
                    catch (InterruptedException e)
                        {
                        tracer.traceError(e, "failure opening camera: %s", selfCameraName);
                        Thread.currentThread().interrupt();
                        }
                    catch (RuntimeException e)
                        {
                        failureReason = Camera.OpenFailure.InternalError;
                        tracer.traceError(e, "failure opening camera: %s", selfCameraName);
                        }
                    finally
                        {
                        if (selfState != CameraState.OpenNotStarted)
                            {
                            reportOpenFailed(failureReason);
                            closeDelegatedCameras();
                            }
                        }
                    break;
                }
            }
        }

    public void memberOpenedOrFailedOpen(SwitchableMemberInfo info)
        {
        awaitAllCamerasOpenOrOpenFailed.countDown();
        }

    public void memberClosed(SwitchableMemberInfo info)
        {
        synchronized (outerLock)
            {
            closeDelegatedCameras();   // If any one of them closes, we close them all
            if (!anyCamerasOpen())
                {
                reportSelfClosed();
                }
            }
        }

    public void memberError(SwitchableMemberInfo info,  Camera.Error error)
        {
        reportError(error);
        }

    protected boolean anyCamerasOpen()
        {
        synchronized (outerLock)
            {
            for (SwitchableMemberInfo cameraInfo : cameraInfos.values())
                {
                if (cameraInfo.isOpen())
                    return true;
                }
            }
        return false;
        }

    protected boolean allCamerasOpen()
        {
        synchronized (outerLock)
            {
            for (SwitchableMemberInfo cameraInfo : cameraInfos.values())
                {
                if (!cameraInfo.isOpen())
                    return false;
                }
            }
        return true;
        }

    //----------------------------------------------------------------------------------------------
    // CameraInternal interface
    //----------------------------------------------------------------------------------------------

    protected SwitchableMemberInfo getCurrentInfo()
        {
        synchronized (outerLock)
            {
            return cameraInfos.get(activeCameraName);
            }
        }

    @Override public boolean hasCalibration(CameraCalibrationManager manager, Size size)
        {
        synchronized (outerLock)
            {
            return getCurrentInfo().hasCalibration(manager, size);
            }
        }

    @Override public CameraCalibration getCalibration(CameraCalibrationManager manager, Size size)
        {
        synchronized (outerLock)
            {
            return getCurrentInfo().getCalibration(manager, size);
            }
        }

    @Override public CameraCalibrationIdentity getCalibrationIdentity()
        {
        synchronized (outerLock)
            {
            return getCurrentInfo().getCalibrationIdentity();
            }
        }

    //----------------------------------------------------------------------------------------------
    // CameraControls
    //----------------------------------------------------------------------------------------------

    @Override protected void constructControls()
        {
        delegatingCameraControls.add(new SwitchableFocusControl(this));
        delegatingCameraControls.add(new SwitchableExposureControl(this));
        }

    //----------------------------------------------------------------------------------------------
    // SwitchableCamera interface
    //----------------------------------------------------------------------------------------------

    @Override public CameraName[] getMembers()
        {
        synchronized (outerLock)
            {
            return selfSwitchableCameraName.getMembers();
            }
        }

    @Override public CameraName getActiveCamera()
        {
        synchronized (outerLock)
            {
            return activeCameraName;
            }
        }

    @Override public void setActiveCamera(@NonNull CameraName cameraName)
        {
        synchronized (outerLock)
            {
            if (cameraInfos.containsKey(cameraName))
                {
                activeCameraName = cameraName;
                changeDelegatedCamera(getCurrentInfo().getCamera());
                }
            else
                throw Misc.illegalArgumentException("%s is not one of the cameras in this switcher", cameraName);
            }
        }

    }
