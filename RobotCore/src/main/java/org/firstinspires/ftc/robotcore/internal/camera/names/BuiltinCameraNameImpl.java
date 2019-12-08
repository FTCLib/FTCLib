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

import androidx.annotation.NonNull;

import org.firstinspires.ftc.robotcore.external.hardware.camera.BuiltinCameraName;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;

@SuppressWarnings("WeakerAccess")
public class BuiltinCameraNameImpl extends CameraNameImplBase implements BuiltinCameraName
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected final VuforiaLocalizer.CameraDirection cameraDirection;

    @Override public String toString()
        {
        return "BuiltinCamera:" + cameraDirection;
        }

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    private BuiltinCameraNameImpl(@NonNull VuforiaLocalizer.CameraDirection cameraDirection)
        {
        this.cameraDirection = cameraDirection;
        }

    public static BuiltinCameraName forCameraDirection(@NonNull VuforiaLocalizer.CameraDirection cameraDirection)
        {
        return new BuiltinCameraNameImpl(cameraDirection);
        }

    //----------------------------------------------------------------------------------------------
    // Equality
    //----------------------------------------------------------------------------------------------

    @Override public boolean equals(Object o)
        {
        if (o instanceof BuiltinCameraNameImpl)
            {
            BuiltinCameraNameImpl them = (BuiltinCameraNameImpl)o;
            return cameraDirection.equals(them.cameraDirection);
            }
        return super.equals(o);
        }

    @Override public int hashCode()
        {
        return cameraDirection.hashCode();
        }

    //----------------------------------------------------------------------------------------------
    // CameraName
    //----------------------------------------------------------------------------------------------

    @Override public boolean isCameraDirection()
        {
        return true;
        }

    //----------------------------------------------------------------------------------------------
    // BuiltinCameraName
    //----------------------------------------------------------------------------------------------

    @Override public VuforiaLocalizer.CameraDirection getCameraDirection()
        {
        return cameraDirection;
        }
    }
