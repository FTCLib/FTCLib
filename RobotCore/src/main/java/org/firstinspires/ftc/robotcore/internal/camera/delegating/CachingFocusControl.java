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

import com.qualcomm.robotcore.util.TypeConversion;

import org.firstinspires.ftc.robotcore.external.hardware.camera.Camera;
import org.firstinspires.ftc.robotcore.external.hardware.camera.controls.FocusControl;
import org.firstinspires.ftc.robotcore.internal.system.Tracer;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link FocusControl} that caches state from another focus control
 */
@SuppressWarnings("WeakerAccess")
public class CachingFocusControl implements FocusControl, DelegatingCameraControl
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "CachingFocusControl";
    public String getTag() { return TAG; }
    public static boolean TRACE = true;
    protected Tracer tracer = Tracer.create(getTag(), TRACE);

    public static boolean isUnknownFocusLength(double focusLength) { return focusLength < 0; }

    protected final Object lock = new Object();
    protected Camera camera = null;
    protected @NonNull FocusControl delegatedFocusControl;
    protected final FocusControl fakeFocusControl;

    protected Mode mode = null;
    protected double minFocusLength = unknownFocusLength;
    protected double maxFocusLength = unknownFocusLength;
    protected double focusLength = unknownFocusLength;
    protected boolean isFocusLengthSupported = false;
    protected Map<Mode, Boolean> supportedModes = new HashMap<>();
    protected boolean limitsInitialized = false;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public CachingFocusControl()
        {
        fakeFocusControl = new FocusControl()
            {
            @Override public Mode getMode()
                {
                return Mode.Unknown;
                }
            @Override public boolean setMode(Mode mode)
                {
                return false;
                }
            @Override public boolean isModeSupported(Mode mode)
                {
                return false;
                }
            @Override public double getMinFocusLength()
                {
                return unknownFocusLength;
                }
            @Override public double getMaxFocusLength()
                {
                return unknownFocusLength;
                }
            @Override public double getFocusLength()
                {
                return unknownFocusLength;
                }
            @Override public boolean setFocusLength(double focusLength)
                {
                return false;
                }
            @Override public boolean isFocusLengthSupported()
                {
                return false;
                }
            };
        delegatedFocusControl = fakeFocusControl;
        }

    @Override public void onCameraChanged(@Nullable Camera newCamera)
        {
        synchronized (lock)
            {
            if (camera != newCamera)
                {
                camera = newCamera;
                if (camera != null)
                    {
                    //noinspection ConstantConditions
                    delegatedFocusControl = camera.getControl(FocusControl.class);
                    if (delegatedFocusControl == null)
                        {
                        delegatedFocusControl = fakeFocusControl;
                        }
                    if (!limitsInitialized)
                        {
                        initializeLimits();
                        if (delegatedFocusControl != fakeFocusControl)
                            {
                            limitsInitialized = true;
                            }
                        }
                    write();
                    read();
                    }
                else
                    delegatedFocusControl = fakeFocusControl;
                }
            }
        }

    //----------------------------------------------------------------------------------------------
    // FocusControl
    //----------------------------------------------------------------------------------------------

    protected void write()
        {
        if (mode != null && isModeSupported(mode))
            {
            delegatedFocusControl.setMode(mode);
            }
        if (!isUnknownFocusLength(focusLength) && isFocusLengthSupported)
            {
            // 4.2.2.1.6 Focus (Absolute) Control
            // When the Auto-Focus Mode control is enabled, attempts to programmatically set this control
            // shall result in a protocol STALL and an error code of bRequestErrorCode = 'Wrong state'.
            if (mode != null && mode != Mode.Unknown && mode != Mode.Auto && mode != Mode.Fixed)
                {
                delegatedFocusControl.setFocusLength(focusLength);
                }
            }
        }

    protected void read()
        {
        mode = delegatedFocusControl.getMode();
        if (!limitsInitialized || isFocusLengthSupported)
            {
            focusLength = delegatedFocusControl.getFocusLength();
            }
        }

    //----------------------------------------------------------------------------------------------

    void initializeLimits()
        {
        for (Mode mode : Mode.values())
            {
            if (mode==Mode.Unknown) continue;
            supportedModes.put(mode, delegatedFocusControl.isModeSupported(mode));
            }
        isFocusLengthSupported = delegatedFocusControl.isFocusLengthSupported();
        if (isFocusLengthSupported)
            {
            minFocusLength = delegatedFocusControl.getMinFocusLength();
            maxFocusLength = delegatedFocusControl.getMaxFocusLength();
            }
        }

    @Override public boolean isModeSupported(final Mode mode)
        {
        return TypeConversion.toBoolean(supportedModes.get(mode));
        }

    @Override public double getMinFocusLength()
        {
        return minFocusLength;
        }

    @Override public double getMaxFocusLength()
        {
        return maxFocusLength;
        }

    @Override public boolean isFocusLengthSupported()
        {
        return isFocusLengthSupported;
        }

    //----------------------------------------------------------------------------------------------

    @Override public Mode getMode()
        {
        synchronized (lock)
            {
            mode = delegatedFocusControl.getMode();
            return mode;
            }
        }

    @Override public boolean setMode(Mode newMode)
        {
        synchronized (lock)
            {
            if (isModeSupported(newMode))
                {
                if (delegatedFocusControl.setMode(newMode))
                    {
                    mode = newMode;
                    return true;
                    }
                }
            return false;
            }
        }

    @Override public double getFocusLength()
        {
        synchronized (lock)
            {
            if (isFocusLengthSupported)
                {
                focusLength = delegatedFocusControl.getFocusLength();
                }
            return focusLength;
            }
        }

    @Override public boolean setFocusLength(double newFocusLength)
        {
        if (newFocusLength >= 0)
            {
            synchronized (lock)
                {
                if (isFocusLengthSupported)
                    {
                    if (delegatedFocusControl.setFocusLength(newFocusLength))
                        {
                        focusLength = newFocusLength;
                        return true;
                        }
                    }
                }
            }
        return false;
        }
    }

