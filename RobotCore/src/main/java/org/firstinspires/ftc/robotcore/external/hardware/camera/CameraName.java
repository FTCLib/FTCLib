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
package org.firstinspires.ftc.robotcore.external.hardware.camera;

import android.content.Context;

import com.qualcomm.robotcore.hardware.HardwareDevice;

import org.firstinspires.ftc.robotcore.external.function.Consumer;
import org.firstinspires.ftc.robotcore.external.function.Continuation;
import org.firstinspires.ftc.robotcore.internal.camera.delegating.SwitchableCameraName;
import org.firstinspires.ftc.robotcore.internal.system.Deadline;

/**
 * {@link CameraName} identifies a {@link HardwareDevice} which is a camera.
 */
public interface CameraName
    {
    /**
     * Returns whether or not this name is that of a webcam. If true, then the
     * {@link CameraName} can be cast to a {@link WebcamName}.
     *
     * @return whether or not this name is that of a webcam
     * @see WebcamName
     */
    boolean isWebcam();

    /**
     * Returns whether or not this name is that of a builtin phone camera. If true, then the
     * {@link CameraName} can be cast to a {@link BuiltinCameraName}.
     *
     * @return whether or not this name is that of a builtin phone camera
     * @see BuiltinCameraName
     */
    boolean isCameraDirection();

    /**
     * Returns whether this name is one representing the ability to switch amongst a
     * series of member cameras. If true, then the receiver can be cast to a
     * {@link SwitchableCameraName}.
     *
     * @return whether this is a {@link SwitchableCameraName}
     */
    boolean isSwitchable();


    /**
     * Returns whether or not this name represents that of an unknown or indeterminate camera.
     * @return whether or not this name represents that of an unknown or indeterminate camera
     */
    boolean isUnknown();

    /**
     * Requests from the user permission to use the camera if same has not already been granted.
     * This may take a long time, as interaction with the user may be necessary. When the outcome
     * is known, the reportResult continuation is called with the result. The report may occur either
     * before or after the call to {@link #asyncRequestCameraPermission} has itself returned. The report will
     * be delivered using the indicated {@link Continuation}
     *
     * @param context       the context in which the permission request should run
     * @param deadline      the time by which the request must be honored or given up as ungranted.
     *                      If this {@link Deadline} is cancelled while the request is outstanding,
     *                      then the permission request will be aborted and false reported as
     *                      the result of the request.
     * @param continuation  the dispatcher used to deliver results of the permission request
     *
     * @throws IllegalArgumentException if the cameraName does not match any known camera device.
     *
     * @see #requestCameraPermission
     */
    void asyncRequestCameraPermission(Context context, Deadline deadline, final Continuation<? extends Consumer<Boolean>> continuation);

    /**
     * Requests from the user permission to use the camera if same has not already been granted.
     * This may take a long time, as interaction with the user may be necessary. The call is made
     * synchronously: the calling thread blocks until an answer is obtained.
     *
     * @param deadline      the time by which the request must be honored or given up as ungranted
     * @return              whether or not permission to use the camera has been granted.
     *
     * @see #asyncRequestCameraPermission
     */
    boolean requestCameraPermission(Deadline deadline);

    /**
     * <p>Query the capabilities of a camera device. These capabilities are
     * immutable for a given camera.</p>
     *
     * @return The properties of the given camera. A degenerate empty set of properties is returned on error.
     */
    CameraCharacteristics getCameraCharacteristics();
    }
