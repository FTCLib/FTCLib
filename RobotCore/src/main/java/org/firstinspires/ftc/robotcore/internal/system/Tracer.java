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
package org.firstinspires.ftc.robotcore.internal.system;

import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.external.function.InterruptableThrowingRunnable;
import org.firstinspires.ftc.robotcore.external.function.Supplier;
import org.firstinspires.ftc.robotcore.external.function.ThrowingRunnable;
import org.firstinspires.ftc.robotcore.external.function.ThrowingSupplier;

import java.util.concurrent.Callable;

@SuppressWarnings("WeakerAccess")
public class Tracer
    {
    //------------------------------------------------------------------------------------------
    // State
    //------------------------------------------------------------------------------------------

    public String tag;
    public boolean enableTrace;
    public boolean enableErrorTrace;

    public String getTag()
        {
        return tag;
        }

    //------------------------------------------------------------------------------------------
    // Construction
    //------------------------------------------------------------------------------------------

    private Tracer(String tag, boolean enableTrace, boolean enableErrorTrace)
        {
        this.tag = tag;
        this.enableTrace = enableTrace;
        this.enableErrorTrace = enableErrorTrace;
        }

    private Tracer(String tag, boolean enableTrace)
        {
        this(tag, enableTrace, true);
        }

    public static Tracer create(String tag, boolean enableTrace)
        {
        return new Tracer(tag, enableTrace);
        }
    public static Tracer create(String tag, boolean enableTrace, boolean enableErrorTrace)
        {
        return new Tracer(tag, enableTrace, enableErrorTrace);
        }

    //------------------------------------------------------------------------------------------
    // Logging & tracing
    //------------------------------------------------------------------------------------------

    public String format(String format, Object...args)
        {
        if (enableTrace)
            return Misc.formatInvariant(format, args);
        else
            return ""; // don't waste cycles
        }

    protected void log(String format, Object... args)
        {
        if (enableTrace)
            {
            RobotLog.dd(getTag(), format, args);
            }
        }

    protected void logError(String format, Object... args)
        {
        if (enableErrorTrace)
            {
            RobotLog.ee(getTag(), format, args);
            }
        }
    protected void logError(Throwable throwable,  String format, Object... args)
        {
        if (enableErrorTrace)
            {
            RobotLog.ee(getTag(), throwable, format, args);
            }
        }

    public void trace(String format, Object... args)
        {
        log(format, args);
        }

    public void traceError(String format, Object... args)
        {
        logError(format, args);
        }

    public void traceError(Throwable throwable, String format, Object... args)
        {
        logError(throwable, format, args);
        }

    public void trace(String name, Runnable runnable)
        {
        log("%s...", name);
        try
            {
            runnable.run();
            }
        finally
            {
            log("...%s", name);
            }
        }

    public <T> T trace(String name, Callable<T> callable) throws Exception
        {
        log("%s...", name);
        try
            {
            return callable.call();
            }
        finally
            {
            log("...%s", name);
            }
        }

    public <T> T trace(String name, Supplier<T> supplier)
        {
        log("%s...", name);
        try
            {
            return supplier.get();
            }
        finally
            {
            log("...%s", name);
            }
        }

    public <T> T traceResult(String name, Supplier<T> supplier)
        {
        T t = null;
        log("%s...", name);
        try
            {
            t = supplier.get();
            }
        finally
            {
            log("...%s: %s", name, t);
            }
        return t;
        }


    public <T,E extends Throwable> T trace(String name, ThrowingSupplier<T,E> func) throws E
        {
        log("%s...", name);
        try
            {
            return func.get();
            }
        finally
            {
            log("...%s", name);
            }
        }

    public <E extends Throwable> void trace(String name, ThrowingRunnable<E> func) throws E
        {
        log("%s...", name);
        try
            {
            func.run();
            }
        finally
            {
            log("...%s", name);
            }
        }

    public <E extends Throwable> void trace(String name, InterruptableThrowingRunnable<E> func) throws E, InterruptedException
        {
        log("%s...", name);
        try
            {
            func.run();
            }
        finally
            {
            log("...%s", name);
            }
        }
}
