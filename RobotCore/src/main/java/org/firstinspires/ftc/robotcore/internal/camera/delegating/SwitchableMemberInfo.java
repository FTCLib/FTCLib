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
import androidx.annotation.Nullable;

import org.firstinspires.ftc.robotcore.external.android.util.Size;
import org.firstinspires.ftc.robotcore.external.hardware.camera.Camera;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraControls;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraName;
import org.firstinspires.ftc.robotcore.external.hardware.camera.controls.CameraControl;
import org.firstinspires.ftc.robotcore.internal.camera.CameraInternal;
import org.firstinspires.ftc.robotcore.internal.camera.CameraState;
import org.firstinspires.ftc.robotcore.internal.camera.calibration.CameraCalibration;
import org.firstinspires.ftc.robotcore.internal.camera.calibration.CameraCalibrationIdentity;
import org.firstinspires.ftc.robotcore.internal.camera.calibration.CameraCalibrationManager;

@SuppressWarnings("WeakerAccess")
class SwitchableMemberInfo implements Camera.StateCallback, CameraControls
    {
    //------------------------------------------------------------------------------------------
    // State
    //------------------------------------------------------------------------------------------

    private RefCountedSwitchableCameraImpl switchableCamera;
    private final Object          localLock = new Object();   // take outerLock first if you need to take both
    private final CameraName      cameraName;
    private       Camera          camera = null;              // once non-null, never null again until we destruct
    private       CameraState     cameraState = CameraState.Nascent;

    //------------------------------------------------------------------------------------------
    // Construction
    //------------------------------------------------------------------------------------------

    public SwitchableMemberInfo(RefCountedSwitchableCameraImpl switchableCamera, CameraName cameraName)
        {
        this.switchableCamera = switchableCamera;
        this.cameraName = cameraName;
        }

    public void closeCamera() // idempotent, of course
        {
        synchronized (localLock)
            {
            if (camera != null)
                {
                camera.close();
                camera = null;
                }
            }
        }

    //------------------------------------------------------------------------------------------
    // Accessing
    //------------------------------------------------------------------------------------------

    public boolean hasCalibration(CameraCalibrationManager manager, Size size)
        {
        synchronized (localLock)
            {
            if (camera!=null && camera instanceof CameraInternal)
                {
                return ((CameraInternal)camera).hasCalibration(manager, size);
                }
            return false;
            }
        }

    public CameraCalibration getCalibration(CameraCalibrationManager manager, Size size)
        {
        synchronized (localLock)
            {
            if (camera != null && camera instanceof CameraInternal)
                {
                return ((CameraInternal)camera).getCalibration(manager, size);
                }
            return null;
            }
        }

    public CameraCalibrationIdentity getCalibrationIdentity()
        {
        synchronized (localLock)
            {
            if (camera!=null && camera instanceof CameraInternal)
                {
                return ((CameraInternal)camera).getCalibrationIdentity();
                }
            return null;
            }
        }

    @Override public @Nullable <T extends CameraControl> T getControl(Class<T> controlType)
        {
        synchronized (localLock)
            {
            if (camera != null)
                {
                return camera.getControl(controlType);
                }
            return null;
            }
        }

    public boolean isOpen()
        {
        synchronized (localLock)
            {
            switch (cameraState)
                {
                case OpenNotStarted:
                case OpenAndStarted:
                    return true;
                default:
                    return false;
                }
            }
        }

    public Camera getCamera()
        {
        synchronized (localLock)
            {
            return camera;
            }
        }

    public CameraName getCameraName()
        {
        return cameraName;
        }

    //------------------------------------------------------------------------------------------
    // Camera Callback
    //------------------------------------------------------------------------------------------

    @Override public void onOpened(@NonNull Camera camera)
        {
        synchronized (localLock)
            {
            this.camera = camera;
            cameraState = CameraState.OpenNotStarted;
            }
        switchableCamera.memberOpenedOrFailedOpen(this);
        }

    @Override public void onOpenFailed(@NonNull CameraName cameraName, @NonNull Camera.OpenFailure failureReason)
        {
        synchronized (localLock)
            {
            this.camera = null;
            cameraState = CameraState.FailedOpen;
            }
        switchableCamera.memberOpenedOrFailedOpen(this);
        }

    @Override public void onClosed(@NonNull Camera camera)
        {
        synchronized (localLock)
            {
            cameraState = CameraState.Closed;
            }
        switchableCamera.memberClosed(this);
        }

    @Override public void onError(@NonNull Camera camera, Camera.Error error)
        {
        switchableCamera.memberError(this, error);
        }
    }
