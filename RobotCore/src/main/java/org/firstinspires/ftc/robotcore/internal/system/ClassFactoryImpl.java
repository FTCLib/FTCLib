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
package org.firstinspires.ftc.robotcore.internal.system;

import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraManager;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.tfod.TFObjectDetector;
import org.firstinspires.ftc.robotcore.internal.camera.CameraManagerImpl;
import org.firstinspires.ftc.robotcore.internal.tfod.TFObjectDetectorImpl;
import org.firstinspires.ftc.robotcore.internal.vuforia.VuforiaLocalizerImpl;

import java.lang.ref.WeakReference;

/**
 * The system implementation of {@link ClassFactory}.
 */
@SuppressWarnings("WeakerAccess")
public class ClassFactoryImpl extends ClassFactory
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "ClassFactory";

    protected final Object lock = new Object();
    protected WeakReference<CameraManagerImpl> cameraManagerHolder = new WeakReference<CameraManagerImpl>(null);

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public static void onApplicationStart()
        {
        InstanceHolder.theInstance = new ClassFactoryImpl();
        }

    //----------------------------------------------------------------------------------------------
    // Accessing
    //----------------------------------------------------------------------------------------------

    @Override public VuforiaLocalizer createVuforia(VuforiaLocalizer.Parameters parameters)
        {
        return new VuforiaLocalizerImpl(parameters);
        }

    @Override public boolean canCreateTFObjectDetector()
        {
        return TFObjectDetectorImpl.isDeviceCompatible();
        }

    @Override public TFObjectDetector createTFObjectDetector(TFObjectDetector.Parameters parameters, VuforiaLocalizer vuforiaLocalizer)
        {
        if (canCreateTFObjectDetector())
            {
            return new TFObjectDetectorImpl(parameters, vuforiaLocalizer);
            }

        throw new RuntimeException("This Android device is not compatible with TensorFlow Object Detection.");
        }

    /**
     * We weakly retain the device manager so that calling multiple times while a given manager or anything
     * streamed from it is in use will return the same instance, but we will automatically eventually
     * clean up (through finalization) if the cameras go unused.
     *
     * That we want this behavior is the reason that CameraManager has no 'close' method. So, in the
     * simple case, where the manager is used for a while and then not, we *rely* on finalization to
     * clean up. But it does clean up, so that's ok.
     */
    @Override public CameraManager getCameraManager()
        {
        synchronized (lock)
            {
            CameraManagerImpl cameraManager = cameraManagerHolder.get();
            if (null == cameraManager)
                {
                cameraManager = new CameraManagerImpl();
                cameraManagerHolder = new WeakReference<CameraManagerImpl>(cameraManager);
                }
            return cameraManager;
            }
        }
    }
