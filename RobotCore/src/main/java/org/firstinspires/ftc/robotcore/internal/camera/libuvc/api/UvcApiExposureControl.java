package org.firstinspires.ftc.robotcore.internal.camera.libuvc.api;

import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.function.Supplier;
import org.firstinspires.ftc.robotcore.external.hardware.camera.controls.ExposureControl;
import org.firstinspires.ftc.robotcore.internal.camera.libuvc.nativeobject.UvcDeviceHandle;
import org.firstinspires.ftc.robotcore.internal.collections.MutableReference;
import org.firstinspires.ftc.robotcore.internal.system.Tracer;
import org.firstinspires.ftc.robotcore.internal.vuforia.externalprovider.ExtendedExposureMode;

import java.util.concurrent.TimeUnit;

@SuppressWarnings("WeakerAccess")
public class UvcApiExposureControl implements ExposureControl
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "UvcApiExposureControl";
    public String getTag() { return TAG; }
    public static boolean TRACE = true;
    public static boolean TRACE_VERBOSE = false;
    protected Tracer tracer = Tracer.create(getTag(), TRACE);
    protected Tracer verboseTracer = Tracer.create(getTag(), TRACE_VERBOSE);

    protected UvcDeviceHandle uvcDeviceHandle; // no ref
    protected final Object lock = new Object();
    protected long cachedExposureNs = ExposureControl.unknownExposure;
    protected ElapsedTime cachedExposureRefresh = null;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public UvcApiExposureControl(UvcDeviceHandle uvcDeviceHandle)
        {
        this.uvcDeviceHandle = uvcDeviceHandle;
        }

    //----------------------------------------------------------------------------------------------
    // ExposureControl
    //----------------------------------------------------------------------------------------------

    @Override public Mode getMode()
        {
        return tracer.traceResult("getMode()", new Supplier<Mode>()
            {
            @Override public Mode get()
                {
                return fromVuforia(uvcDeviceHandle.getVuforiaExposureMode());
                }
            });
        }

    @Override public boolean setMode(final Mode mode)
        {
        return tracer.traceResult(tracer.format("setMode(%s)", mode), new Supplier<Boolean>()
            {
            @Override public Boolean get()
                {
                return uvcDeviceHandle.setVuforiaExposureMode(toVuforia(mode));
                }
            });
        }

    @Override public boolean isModeSupported(final Mode mode)
        {
        return tracer.traceResult(tracer.format("isModeSupported(%s)", mode), new Supplier<Boolean>()
            {
            @Override public Boolean get()
                {
                return uvcDeviceHandle.isVuforiaExposureModeSupported(toVuforia(mode));
                }
            });
        }

    @Override public long getMinExposure(final TimeUnit resultUnit)
        {
        return tracer.traceResult(tracer.format("getMinExposure(%s)", resultUnit), new Supplier<Long>()
            {
            @Override public Long get()
                {
                return resultUnit.convert(uvcDeviceHandle.getMinExposure(), TimeUnit.NANOSECONDS);
                }
            });
        }

    @Override public long getMaxExposure(final TimeUnit resultUnit)
        {
        return tracer.traceResult(tracer.format("getMaxExposure(%s)", resultUnit), new Supplier<Long>()
            {
            @Override public Long get()
                {
                return resultUnit.convert(uvcDeviceHandle.getMaxExposure(), TimeUnit.NANOSECONDS);
                }
            });
        }

    @Override public long getExposure(final TimeUnit resultUnit)
        {
        return verboseTracer.traceResult(verboseTracer.format("getExposure(%s)", resultUnit), new Supplier<Long>()
            {
            @Override public Long get()
                {
                synchronized (lock)
                    {
                    setCachedExposureNs(uvcDeviceHandle.getExposure());
                    return resultUnit.convert(cachedExposureNs, TimeUnit.NANOSECONDS);
                    }
                }
            });
        }

    @Override public long getCachedExposure(final TimeUnit resultUnit, final MutableReference<Boolean> refreshed, final long permittedStaleness, final TimeUnit permittedStalenessUnit)
        {
        return verboseTracer.traceResult(verboseTracer.format("getCachedExposure(%s)", resultUnit), new Supplier<Long>()
            {
            @Override public Long get()
                {
                synchronized (lock)
                    {
                    refreshed.setValue(false);
                    boolean refreshNeeded;
                    if (cachedExposureRefresh==null)
                        {
                        refreshNeeded = true;
                        }
                    else
                        {
                        long nsPermittedStaleness = TimeUnit.NANOSECONDS.convert(permittedStaleness, permittedStalenessUnit);
                        refreshNeeded = cachedExposureRefresh.nanoseconds() >= nsPermittedStaleness;
                        }
                    if (refreshNeeded)
                        {
                        getExposure(TimeUnit.NANOSECONDS); // will set cache
                        refreshed.setValue(true);
                        }
                    return resultUnit.convert(cachedExposureNs, TimeUnit.NANOSECONDS);
                    }
                }
            });
        }

    @Override public boolean setExposure(final long duration, final TimeUnit durationUnit)
        {
        return tracer.traceResult(tracer.format("setExposure(%s %s)", duration, durationUnit), new Supplier<Boolean>()
            {
            @Override public Boolean get()
                {
                synchronized (lock)
                    {
                    long nsExposure = TimeUnit.NANOSECONDS.convert(duration, durationUnit);
                    boolean result = uvcDeviceHandle.setExposure(nsExposure);
                    if (result)
                        {
                        setCachedExposureNs(nsExposure);
                        }
                    return result;
                    }
                }
            });
        }

    @Override public boolean isExposureSupported()
        {
        return tracer.traceResult("isExposureSupported()", new Supplier<Boolean>()
            {
            @Override public Boolean get()
                {
                return uvcDeviceHandle.isExposureSupported();
                }
            });
        }

    protected void setCachedExposureNs(long cachedExposureNs)
        {
        this.cachedExposureNs = cachedExposureNs;
        this.cachedExposureRefresh = new ElapsedTime();
        }

    //----------------------------------------------------------------------------------------------
    // Utility
    //----------------------------------------------------------------------------------------------

    public static Mode fromVuforia(int vuforia)
        {
        return fromVuforia(ExtendedExposureMode.from(vuforia));
        }

    public static Mode fromVuforia(ExtendedExposureMode vuforia)
        {
        switch (vuforia)
            {
            case APERTURE_PRIORITY: return Mode.AperturePriority;
            case AUTO:              return Mode.Auto;
            case CONTINUOUS_AUTO:   return Mode.ContinuousAuto;
            case MANUAL:            return Mode.Manual;
            case SHUTTER_PRIORITY:  return Mode.ShutterPriority;
            case UNKNOWN:
            default:
                return Mode.Unknown;
            }
        }

    public static ExtendedExposureMode toVuforia(Mode mode)
        {
        switch (mode)
            {
            case AperturePriority:  return ExtendedExposureMode.APERTURE_PRIORITY;
            case Auto:              return ExtendedExposureMode.AUTO;
            case ContinuousAuto:    return ExtendedExposureMode.CONTINUOUS_AUTO;
            case Manual:            return ExtendedExposureMode.MANUAL;
            case ShutterPriority:   return ExtendedExposureMode.SHUTTER_PRIORITY;
            case Unknown:
            default:
                return ExtendedExposureMode.UNKNOWN;
            }
        }
    }
