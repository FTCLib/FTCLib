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

import androidx.annotation.CallSuper;

import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.TypeConversion;

import java.nio.ByteOrder;


/**
 * A {@link NativeObject} is the Java manifestation of some object living in native memory.
 *
 * We use reference counting to support the child-parent liveness relationships necessary
 * when dealing with native child data that is allocated as part of a larger parent whole.
 */
@SuppressWarnings("WeakerAccess")
public class NativeObject<ParentType extends RefCounted> extends DestructOnFinalize<ParentType>
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public enum MemoryAllocator
        {
        UNKNOWN,    // memory allocation mechanism is unknown
        MALLOC,     // memory is allocated with malloc / free
        EXTERNAL    // memory is allocated & freed by external APIs (ie: something other than malloc & free)
        }

    protected ByteOrder       byteOrder = ByteOrder.LITTLE_ENDIAN;
    protected long            pointer;
    protected MemoryAllocator memoryAllocator;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    protected NativeObject(long pointer, MemoryAllocator memoryAllocator)
        {
        this(pointer, memoryAllocator, defaultTraceLevel);
        }

    protected NativeObject(long pointer, MemoryAllocator memoryAllocator, TraceLevel debugLevel)
        {
        // Make sure *we* do the ctor tracing, not super
        super(TraceLevel.None);
        this.traceLevel = debugLevel;
        //
        if (pointer == 0) throw new IllegalArgumentException("pointer must not be null");
        this.pointer = pointer;
        this.memoryAllocator = memoryAllocator;
        if (traceCtor()) RobotLog.vv(getTag(), "construct(%s)", getTraceIdentifier());
        }

    protected NativeObject(long pointer)
        {
        this(pointer, MemoryAllocator.UNKNOWN);
        }

    protected NativeObject(long pointer, TraceLevel debugLevel)
        {
        this(pointer, MemoryAllocator.UNKNOWN, debugLevel);
        }

    protected NativeObject()
        {
        this(defaultTraceLevel);
        }

    protected NativeObject(TraceLevel debugLevel)
        {
        super(debugLevel);
        this.pointer = 0;
        this.memoryAllocator = MemoryAllocator.UNKNOWN;
        }

    public String getTraceIdentifier()
        {
        return Misc.formatInvariant("pointer=0x%08x", pointer);
        }

    @Override @CallSuper
    protected void destructor()
        {
        freeMemory();
        super.destructor();
        }

    protected static long checkAlloc(long pointer)
        {
        if (pointer == 0) throw new OutOfMemoryError();
        return pointer;
        }

    public void allocateMemory(long cbAlloc)
        {
        synchronized (lock)
            {
            freeMemory();
            if (cbAlloc > 0)
                {
                pointer = checkAlloc(nativeAllocMemory(cbAlloc));
                setMemoryAllocator(MemoryAllocator.MALLOC);
                }
            else if (cbAlloc < 0)
                {
                throw new IllegalArgumentException("cbAlloc must be >= 0");
                }
            }
        }

    public void freeMemory()
        {
        synchronized (lock)
            {
            if (memoryAllocator == MemoryAllocator.MALLOC)
                {
                nativeFreeMemory(pointer);
                clearPointer();
                }
            }
        }

    protected void clearPointer()
        {
        synchronized (lock)
            {
            pointer = 0;
            }
        }

    //----------------------------------------------------------------------------------------------
    // Accessing
    //----------------------------------------------------------------------------------------------

    protected void setMemoryAllocator(MemoryAllocator memoryAllocator)
        {
        synchronized (lock)
            {
            this.memoryAllocator = memoryAllocator;
            }
        }

    protected byte getByte(int ib)
        {
        return nativeGetBytes(pointer, ib, 1)[0];
        }
    protected int getUByte(int ib)
        {
        return TypeConversion.unsignedByteToInt(getByte(ib));
        }
    protected short getShort(int ib)
        {
        return TypeConversion.byteArrayToShort(nativeGetBytes(pointer, ib, 2), byteOrder);
        }
    protected int getUShort(int ib)
        {
        return TypeConversion.unsignedShortToInt(getShort(ib));
        }
    protected int getInt(int ib)
        {
        return TypeConversion.byteArrayToInt(nativeGetBytes(pointer, ib, 4), byteOrder);
        }
    protected void setInt(int ib, int value)
        {
        byte[] rgb = TypeConversion.intToByteArray(value, byteOrder);
        nativeSetBytes(pointer, ib, rgb);
        }

    protected long getUInt(int ib)
        {
        return TypeConversion.unsignedIntToLong(getInt(ib));
        }
    protected void setUInt(int ib, long value)
        {
        setInt(ib, (int)value);
        }

    protected int getSizet(int ib)
        {
        return getInt(ib);
        }

    protected long getLong(int ib)
        {
        return TypeConversion.byteArrayToLong(nativeGetBytes(pointer, ib, 8), byteOrder);
        }
    protected void setLong(int ib, long value)
        {
        byte[] rgb = TypeConversion.longToByteArray(value, byteOrder);
        nativeSetBytes(pointer, ib, rgb);
        }

    protected String getString(int ib)
        {
        return nativeGetString(pointer, ib);
        }

    protected byte[] getBytes(int ib, int cb)
        {
        return nativeGetBytes(pointer, ib, cb);
        }
    protected void setBytes(int ib, byte[] rgb)
        {
        nativeSetBytes(pointer, ib, rgb);
        }

    protected long getPointer(int ib)
        {
        return nativeGetPointer(pointer, ib);
        }
    protected void setPointer(int ib, long pVoid)
        {
        nativeSetPointer(pointer, ib, pVoid);
        }


    //----------------------------------------------------------------------------------------------
    // Native
    //----------------------------------------------------------------------------------------------

    protected native static byte[] nativeGetBytes(long pointer, int ib, int cb);
    protected native static void   nativeSetBytes(long pointer, int ib, byte[] rgb);
    protected native static String nativeGetString(long pointer, int ib);
    protected native static long[] nativeGetLinkedList(long pointer, int ib);
    protected native static long[] nativeGetNullTerminatedList(long pointer, int ib, int cbStride);
    protected native static long   nativeGetPointer(long pointer, int ib);
    protected native static void   nativeSetPointer(long pointer, int ib, long value);

    protected native static long nativeAllocMemory(long cbAlloc);
    protected native static void nativeFreeMemory(long pointer);

    static
        {
        System.loadLibrary("RobotCore");
        }
    }
