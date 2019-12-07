package org.firstinspires.ftc.robotcore.internal.camera.libuvc.api;

import org.firstinspires.ftc.robotcore.external.function.Supplier;
import org.firstinspires.ftc.robotcore.external.hardware.camera.controls.FocusControl;
import org.firstinspires.ftc.robotcore.internal.camera.libuvc.nativeobject.UvcDeviceHandle;
import org.firstinspires.ftc.robotcore.internal.system.Tracer;
import org.firstinspires.ftc.robotcore.internal.vuforia.externalprovider.FocusMode;

@SuppressWarnings("WeakerAccess")
public class UvcApiFocusControl implements FocusControl
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "UvcApiFocusControl";
    public String getTag() { return TAG; }
    public static boolean TRACE = true;
    protected Tracer tracer = Tracer.create(getTag(), TRACE);

    protected UvcDeviceHandle uvcDeviceHandle; // no ref

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public UvcApiFocusControl(UvcDeviceHandle uvcDeviceHandle)
        {
        this.uvcDeviceHandle = uvcDeviceHandle;
        }

    //----------------------------------------------------------------------------------------------
    // FocusControl
    //----------------------------------------------------------------------------------------------

    @Override public Mode getMode()
        {
        return fromVuforia(uvcDeviceHandle.getVuforiaFocusMode());
        }

    @Override public boolean setMode(final Mode mode)
        {
        return tracer.traceResult(tracer.format("setMode(%s)", mode), new Supplier<Boolean>()
            {
            @Override public Boolean get()
                {
                return uvcDeviceHandle.setVuforiaFocusMode(toVuforia(mode));
                }
            });
        }

    @Override public boolean isModeSupported(final Mode mode)
        {
        return tracer.traceResult(tracer.format("isModeSupported(%s)", mode), new Supplier<Boolean>()
            {
            @Override public Boolean get()
                {
                return uvcDeviceHandle.isVuforiaFocusModeSupported(toVuforia(mode));
                }
            });
        }


    @Override public double getMinFocusLength()
        {
        return tracer.traceResult("getMinFocusLength()", new Supplier<Double>()
            {
            @Override public Double get()
                {
                return uvcDeviceHandle.getMinFocusLength();
                }
            });
        }

    @Override public double getMaxFocusLength()
        {
        return tracer.traceResult("getMaxFocusLength()", new Supplier<Double>()
            {
            @Override public Double get()
                {
                return uvcDeviceHandle.getMaxFocusLength();
                }
            });
        }

    @Override public double getFocusLength()
        {
        return tracer.traceResult("getFocusLength()", new Supplier<Double>()
            {
            @Override public Double get()
                {
                return uvcDeviceHandle.getFocusLength();
                }
            });
        }

    @Override public boolean setFocusLength(final double focusLength)
        {
        return tracer.traceResult(tracer.format("setFocusLength(%s)", focusLength), new Supplier<Boolean>()
            {
            @Override public Boolean get()
                {
                return uvcDeviceHandle.setFocusLength(focusLength);
                }
            });
        }

    @Override public boolean isFocusLengthSupported()
        {
        return tracer.traceResult("isFocusLengthSupported", new Supplier<Boolean>()
            {
            @Override public Boolean get()
                {
                return uvcDeviceHandle.isFocusLengthSupported();
                }
            });
        }

    //----------------------------------------------------------------------------------------------
    // Utility
    //----------------------------------------------------------------------------------------------

    public static Mode fromVuforia(int vuforia)
        {
        return fromVuforia(FocusMode.from(vuforia));
        }

    public static Mode fromVuforia(FocusMode vuforia)
        {
        switch (vuforia)
            {
            case AUTO:              return Mode.Auto;
            case CONTINUOUS_AUTO:   return Mode.ContinuousAuto;
            case MACRO:             return Mode.Macro;
            case INFINITY_FOCUS:    return Mode.Infinity;
            case FIXED:             return Mode.Fixed;
            case UNKNOWN:
            default:
                return Mode.Unknown;
            }
        }

    public static FocusMode toVuforia(Mode mode)
        {
        switch (mode)
            {
            case Auto:           return FocusMode.AUTO;
            case ContinuousAuto: return FocusMode.CONTINUOUS_AUTO;
            case Macro:          return FocusMode.MACRO;
            case Infinity:       return FocusMode.INFINITY_FOCUS;
            case Fixed:          return FocusMode.FIXED;
            case Unknown:
            default:
                return FocusMode.UNKNOWN;
            }
        }

    }
