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
package org.firstinspires.ftc.robotcore.internal.camera;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.external.android.util.Size;
import org.firstinspires.ftc.robotcore.external.function.Continuation;
import org.firstinspires.ftc.robotcore.external.hardware.camera.Camera;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCaptureRequest;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCaptureSession;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraException;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraName;
import org.firstinspires.ftc.robotcore.external.hardware.camera.controls.CameraControl;
import org.firstinspires.ftc.robotcore.internal.system.Assert;
import org.firstinspires.ftc.robotcore.internal.system.CloseableDestructOnFinalize;
import org.firstinspires.ftc.robotcore.internal.system.Misc;
import org.firstinspires.ftc.robotcore.internal.system.RefCounted;
import org.firstinspires.ftc.robotcore.internal.camera.calibration.CameraCalibration;
import org.firstinspires.ftc.robotcore.internal.camera.calibration.CameraCalibrationIdentity;
import org.firstinspires.ftc.robotcore.internal.camera.calibration.CameraCalibrationManager;

import static org.firstinspires.ftc.robotcore.internal.camera.CameraManagerImpl.TAG;

/**
 * A {@link CameraImpl} owns exactly one ref on a {@link RefCountedCamera}. When the {@link Camera}
 * is closed, that reference is released.
 */
@SuppressWarnings("WeakerAccess")
public class CameraImpl extends CloseableDestructOnFinalize implements Camera, CameraInternal
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected RefCountedCamera refCountedCamera;
    protected boolean ownExternalRef;

    @Override public String toString()
        {
        return Misc.formatInvariant("%s(%s)", getClass().getSimpleName(), refCountedCamera);
        }

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public CameraImpl(@NonNull RefCountedCamera refCountedCamera)
        {
        this.refCountedCamera = refCountedCamera;
        this.refCountedCamera.addRefExternal();     // alakjfal;ja;lkjasfd
        ownExternalRef = true;
        enableOnlyClose();                  // to honor semantics of the Camera interface & Camera.StateCallback.onOpened
        }

    @Override protected void destructor()
        {
        refCountedCamera.getTracer().trace("CameraImpl.destructor()", new Runnable()
            {
            @Override public void run()
                {
                Assert.assertTrue(ownExternalRef);
                if (ownExternalRef)
                    {
                    ownExternalRef = false;
                    refCountedCamera.releaseRefExternal();  // alakjfal;ja;lkjasfd
                    }
                CameraImpl.super.destructor();
                }
            });
        }

    //----------------------------------------------------------------------------------------------
    // Camera
    //----------------------------------------------------------------------------------------------

    @NonNull @Override public CameraName getCameraName()
        {
        synchronized (lock)
            {
            return refCountedCamera.getCameraName();
            }
        }

    @Override public CameraCaptureRequest createCaptureRequest(int androidFormat, Size size, int fps) throws CameraException
        {
        synchronized (lock)
            {
            return refCountedCamera.createCaptureRequest(androidFormat, size, fps);
            }
        }

    @Override @NonNull public CameraCaptureSession createCaptureSession(Continuation<? extends CameraCaptureSession.StateCallback> continuation) throws CameraException
        {
        synchronized (lock)
            {
            return refCountedCamera.createCaptureSession(continuation);
            }
        }

    @Override public Camera dup()
        {
        RobotLog.vv(getTag(), "dup()");
        return new CameraImpl(refCountedCamera);
        }

    //----------------------------------------------------------------------------------------------
    // CameraControls
    //----------------------------------------------------------------------------------------------

    @Override public @Nullable <T extends CameraControl> T getControl(Class<T> controlType)
        {
        synchronized (lock)
            {
            return refCountedCamera.getControl(controlType);
            }
        }

    //----------------------------------------------------------------------------------------------
    // CameraInternal
    //----------------------------------------------------------------------------------------------

    @Override public boolean hasCalibration(CameraCalibrationManager manager, Size size)
        {
        return refCountedCamera.hasCalibration(manager, size);
        }

    @Override public CameraCalibration getCalibration(CameraCalibrationManager manager, Size size)
        {
        return refCountedCamera.getCalibration(manager, size);
        }

    @Override public CameraCalibrationIdentity getCalibrationIdentity()
        {
        return refCountedCamera.getCalibrationIdentity();
        }

    //----------------------------------------------------------------------------------------------
    // Utility
    //----------------------------------------------------------------------------------------------

    public static void addRefCamera(@NonNull Camera camera)
        {
        if (camera instanceof RefCounted)
            {
            ((RefCounted) camera).addRef();
            }
        }

    public static void releaseRefCamera(@NonNull Camera camera)
        {
        if (camera instanceof RefCounted)
            {
            ((RefCounted) camera).releaseRef();
            }
        }

    public static void closeCamera(String caller, @NonNull Camera camera)
        {
        RobotLog.vv(TAG, "%s closing camera: %s", caller, camera);
        camera.close();
        }
    }
