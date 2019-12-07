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

import org.firstinspires.ftc.robotcore.external.hardware.camera.controls.FocusControl;

@SuppressWarnings("WeakerAccess")
public class SwitchableFocusControl extends CachingFocusControl
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected final RefCountedSwitchableCameraImpl switchableCamera;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public SwitchableFocusControl(RefCountedSwitchableCameraImpl switchableCamera)
        {
        this.switchableCamera = switchableCamera;
        }

    //----------------------------------------------------------------------------------------------
    // FocusControl
    //----------------------------------------------------------------------------------------------

    @Override protected void initializeLimits()
        {
        for (FocusControl.Mode mode : FocusControl.Mode.values())
            {
            if (mode==Mode.Unknown) continue;
            supportedModes.put(mode, aggregatedIsModeSupported(mode));
            }
        minFocusLength = aggregatedMinFocus();
        maxFocusLength = aggregatedMaxFocus();
        isFocusLengthSupported = aggregatedIsFocusSupported();
        }

    protected double aggregatedMinFocus()
        {
        synchronized (lock)
            {
            double result = Double.MIN_VALUE;
            for (SwitchableMemberInfo info : switchableCamera.cameraInfos.values())
                {
                FocusControl focusLengthControl = info.getControl(FocusControl.class);
                if (focusLengthControl != null)
                    {
                    double theirs = focusLengthControl.getMinFocusLength();
                    if (!isUnknownFocusLength(theirs))
                        {
                        result = Math.max(result, theirs);
                        }
                    }
                }
            return result==Double.MIN_VALUE ? unknownFocusLength : result;
            }
        }

    protected double aggregatedMaxFocus()
        {
        synchronized (lock)
            {
            double result = Double.MAX_VALUE;
            for (SwitchableMemberInfo info : switchableCamera.cameraInfos.values())
                {
                FocusControl focusLengthControl = info.getControl(FocusControl.class);
                if (focusLengthControl != null)
                    {
                    double theirs = focusLengthControl.getMaxFocusLength();
                    if (!isUnknownFocusLength(theirs))
                        {
                        result = Math.min(result, theirs);
                        }
                    }
                }
            return result==Double.MAX_VALUE ? unknownFocusLength : result;
            }
        }

    protected boolean aggregatedIsModeSupported(FocusControl.Mode mode)
        {
        synchronized (lock)
            {
            for (SwitchableMemberInfo info : switchableCamera.cameraInfos.values())
                {
                FocusControl focusLengthControl = info.getControl(FocusControl.class);
                if (focusLengthControl != null)
                    {
                    if (!focusLengthControl.isModeSupported(mode))
                        {
                        return false;
                        }
                    }
                }
            return true;
            }
        }

    protected boolean aggregatedIsFocusSupported()
        {
        synchronized (lock)
            {
            for (SwitchableMemberInfo info : switchableCamera.cameraInfos.values())
                {
                FocusControl focusLengthControl = info.getControl(FocusControl.class);
                if (focusLengthControl != null)
                    {
                    if (!focusLengthControl.isFocusLengthSupported())
                        {
                        return false;
                        }
                    }
                }
            return true;
            }
        }
    }
