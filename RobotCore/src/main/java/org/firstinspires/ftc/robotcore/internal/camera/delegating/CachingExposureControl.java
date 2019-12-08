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

import org.firstinspires.ftc.robotcore.external.function.Supplier;
import org.firstinspires.ftc.robotcore.external.hardware.camera.Camera;
import org.firstinspires.ftc.robotcore.external.hardware.camera.controls.ExposureControl;
import org.firstinspires.ftc.robotcore.internal.collections.MutableReference;
import org.firstinspires.ftc.robotcore.internal.system.Tracer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * An {@link ExposureControl} that caches state from another exposure control
 */
@SuppressWarnings("WeakerAccess")
public class CachingExposureControl implements ExposureControl, DelegatingCameraControl
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "CachingExposureControl";
    public String getTag() { return TAG; }
    public static boolean TRACE = true;
    protected Tracer tracer = Tracer.create(getTag(), TRACE);

    protected static boolean isUnknownExposure(long exposure) { return exposure == unknownExposure || exposure < 0; }

    protected final Object lock = new Object();
    protected Camera camera = null;
    protected @NonNull ExposureControl delegatedExposureControl;
    protected final ExposureControl fakeExposureControl;

    protected Mode mode = null;
    protected long nsMinExposure = unknownExposure;
    protected long nsMaxExposure = unknownExposure;
    protected long nsExposure = unknownExposure;
    protected boolean isExposureSupported = false;
    protected Map<Mode, Boolean> supportedModes = new HashMap<>();
    protected boolean limitsInitialized = false;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public CachingExposureControl()
        {
        fakeExposureControl = new ExposureControl()
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
            @Override public long getMinExposure(TimeUnit resultUnit)
                {
                return unknownExposure;
                }
            @Override public long getMaxExposure(TimeUnit resultUnit)
                {
                return unknownExposure;
                }
            @Override public long getExposure(TimeUnit resultUnit)
                {
                return unknownExposure;
                }
            @Override public long getCachedExposure(final TimeUnit resultUnit, final MutableReference<Boolean> refreshed, final long permittedStaleness, final TimeUnit permittedStalenessUnit)
                {
                refreshed.setValue(false);
                return unknownExposure;
                }
            @Override public boolean setExposure(long duration, TimeUnit durationUnit)
                {
                return false;
                }
            @Override public boolean isExposureSupported()
                {
                return false;
                }
            };
        delegatedExposureControl = fakeExposureControl;
        }

    @Override public void onCameraChanged(final @Nullable Camera newCamera)
        {
        synchronized (lock)
            {
            tracer.trace(tracer.format("onCameraChanged(%s->%s)", camera, newCamera), new Runnable()
                {
                @Override public void run()
                    {
                    if (camera != newCamera)
                        {
                        camera = newCamera;
                        if (camera != null)
                            {
                            //noinspection ConstantConditions
                            delegatedExposureControl = camera.getControl(ExposureControl.class);
                            if (delegatedExposureControl == null)
                                {
                                delegatedExposureControl = fakeExposureControl;
                                }
                            if (!limitsInitialized)
                                {
                                initializeLimits();
                                if (delegatedExposureControl != fakeExposureControl)
                                    {
                                    limitsInitialized = true;
                                    }
                                }
                            write();
                            read();
                            }
                        else
                            delegatedExposureControl = fakeExposureControl;
                        }

                    }
                });
            }
        }

    //----------------------------------------------------------------------------------------------
    // FocusControl
    //----------------------------------------------------------------------------------------------

    protected void write() // this needs testing: don't want to STALL if camera is in wrong mode. Darn!
        {
        if (mode != null && isModeSupported(mode))
            {
            delegatedExposureControl.setMode(mode);
            }
        if (!isUnknownExposure(nsExposure) && isExposureSupported)
            {
            // 4.2.2.1.4 Exposure Time (Absolute) Control
            // When the Auto-Exposure Mode control is in Auto mode or Aperture Priority mode attempts
            // to programmatically set this control shall result in a protocol STALL and an error code
            // of bRequestErrorCode = 'Wrong state'.
            if (mode != null && mode != Mode.Unknown && mode != Mode.Auto && mode != Mode.AperturePriority)
                {
                delegatedExposureControl.setExposure(nsExposure, TimeUnit.NANOSECONDS);
                }
            }
        }

    protected void read()
        {
        mode = delegatedExposureControl.getMode();
        if (!limitsInitialized || isExposureSupported)
            {
            nsExposure = delegatedExposureControl.getExposure(TimeUnit.NANOSECONDS);
            }
        }

    //----------------------------------------------------------------------------------------------

    protected void initializeLimits()
        {
        for (Mode mode : Mode.values())
            {
            if (mode==Mode.Unknown) continue;
            supportedModes.put(mode, delegatedExposureControl.isModeSupported(mode));
            }
        isExposureSupported = delegatedExposureControl.isExposureSupported();
        if (isExposureSupported)
            {
            nsMinExposure = delegatedExposureControl.getMinExposure(TimeUnit.NANOSECONDS);
            nsMaxExposure = delegatedExposureControl.getMaxExposure(TimeUnit.NANOSECONDS);
            }
        }

    @Override public boolean isModeSupported(final Mode mode)
        {
        return tracer.traceResult(tracer.format("isModeSupported(%s)", mode), new Supplier<Boolean>()
            {
            @Override public Boolean get()
                {
                return TypeConversion.toBoolean(supportedModes.get(mode));
                }
            });
        }

    @Override public long getMinExposure(TimeUnit resultUnit)
        {
        return resultUnit.convert(nsMinExposure, TimeUnit.NANOSECONDS);
        }

    @Override public long getMaxExposure(TimeUnit resultUnit)
        {
        return resultUnit.convert(nsMaxExposure, TimeUnit.NANOSECONDS);
        }

    @Override public boolean isExposureSupported()
        {
        return isExposureSupported;
        }

    //----------------------------------------------------------------------------------------------
    
    @Override public Mode getMode()
        {
        synchronized (lock)
            {
            mode = delegatedExposureControl.getMode();
            return mode;
            }
        }

    @Override public boolean setMode(Mode newMode)
        {
        synchronized (lock)
            {
            if (isExposureSupported)
                {
                if (delegatedExposureControl.setMode(newMode))
                    {
                    mode = newMode;
                    return true;
                    }
                }
            return false;
            }
        }

    @Override public long getExposure(TimeUnit resultUnit)
        {
        synchronized (lock)
            {
            if (isExposureSupported)
                {
                nsExposure = delegatedExposureControl.getExposure(TimeUnit.NANOSECONDS);
                return resultUnit.convert(nsExposure, TimeUnit.NANOSECONDS);
                }
            return ExposureControl.unknownExposure;
            }
        }

    @Override public long getCachedExposure(final TimeUnit resultUnit, final MutableReference<Boolean> refreshed, final long permittedStaleness, final TimeUnit permittedStalenessUnit)
        {
        synchronized (lock)
            {
            if (isExposureSupported)
                {
                refreshed.setValue(false);
                long result = delegatedExposureControl.getCachedExposure(TimeUnit.NANOSECONDS, refreshed, permittedStaleness, permittedStalenessUnit);
                if (refreshed.getValue())
                    {
                    nsExposure = result;
                    }
                return resultUnit.convert(result, TimeUnit.NANOSECONDS);
                }
            return ExposureControl.unknownExposure;
            }
        }

    @Override public boolean setExposure(long duration, TimeUnit durationUnit)
        {
        if (duration > 0)
            {
            synchronized (lock)
                {
                if (isExposureSupported)
                    {
                    if (delegatedExposureControl.setExposure(duration, durationUnit))
                        {
                        nsExposure = TimeUnit.NANOSECONDS.convert(duration, durationUnit);
                        return true;
                        }
                    }
                }
            }
        return false;
        }
    }

