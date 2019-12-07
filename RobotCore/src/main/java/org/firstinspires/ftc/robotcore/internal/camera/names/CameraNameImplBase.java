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
package org.firstinspires.ftc.robotcore.internal.camera.names;

import android.content.Context;

import com.qualcomm.robotcore.util.ThreadPool;

import org.firstinspires.ftc.robotcore.external.function.Consumer;
import org.firstinspires.ftc.robotcore.external.function.Continuation;
import org.firstinspires.ftc.robotcore.external.function.ContinuationResult;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCharacteristics;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraName;
import org.firstinspires.ftc.robotcore.internal.camera.libuvc.api.UvcApiCameraCharacteristics;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.system.ContinuationSynchronizer;
import org.firstinspires.ftc.robotcore.internal.system.Deadline;

/**
 * Represents the name of an available camera: builtin, USB-attached, etc
 */
@SuppressWarnings("WeakerAccess")
public abstract class CameraNameImplBase implements CameraName
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static boolean TRACE = true;

    //----------------------------------------------------------------------------------------------
    // CameraName
    //----------------------------------------------------------------------------------------------

    @Override public boolean isWebcam()
        {
        return false;
        }

    @Override public boolean isCameraDirection()
        {
        return false;
        }

    @Override public boolean isUnknown()
        {
        return false;
        }

    @Override public boolean isSwitchable()
        {
        return false;
        }

    @Override public void asyncRequestCameraPermission(Context context, Deadline deadline, Continuation<? extends Consumer<Boolean>> continuation)
        {
        continuation.dispatch(new ContinuationResult<Consumer<Boolean>>()
            {
            @Override public void handle(Consumer<Boolean> booleanConsumer)
                {
                booleanConsumer.accept(true);
                }
            });
        }

    @Override public boolean requestCameraPermission(Deadline deadline)
        {
        final ContinuationSynchronizer<Boolean> synchronizer = new ContinuationSynchronizer<>(deadline, TRACE, false);
        asyncRequestCameraPermission(AppUtil.getDefContext(), deadline, Continuation.create(ThreadPool.getDefault(), new Consumer<Boolean>()
            {
            @Override public void accept(Boolean permissionGranted)
                {
                synchronizer.finish("permission request complete", permissionGranted);
                }
            }));
        try {
            synchronizer.await();
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            }
        return synchronizer.getValue();
        }

    @Override public CameraCharacteristics getCameraCharacteristics()
        {
        return new UvcApiCameraCharacteristics();
        }

    }
