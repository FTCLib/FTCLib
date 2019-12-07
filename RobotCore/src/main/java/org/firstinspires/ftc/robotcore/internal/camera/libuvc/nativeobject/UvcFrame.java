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
package org.firstinspires.ftc.robotcore.internal.camera.libuvc.nativeobject;

import android.graphics.Bitmap;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.Type;

import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.internal.camera.libuvc.constants.UvcFrameFormat;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.system.Assert;
import org.firstinspires.ftc.robotcore.internal.system.Misc;
import org.firstinspires.ftc.robotcore.internal.system.NativeObject;
import org.firstinspires.ftc.robotcore.internal.camera.ScriptC_format_convert;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

/**
 * The Java manifestation of a native uvc_frame
 */
@SuppressWarnings("WeakerAccess")
public class UvcFrame extends NativeObject<UvcContext>
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected boolean useNativeFormatConversion = true;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public UvcFrame(long pointer, MemoryAllocator memoryAllocator, UvcContext uvcContext)
        {
        super(pointer, memoryAllocator, TraceLevel.VeryVerbose);
        setParent(uvcContext);
        }

    public long getPointer()
        {
        return pointer;
        }

    public UvcFrame copy()
        {
        return new UvcFrame(checkAlloc(nativeCopyFrame(pointer)), MemoryAllocator.EXTERNAL, getParent());
        }

    @Override protected void destructor()
        {
        if (memoryAllocator == MemoryAllocator.EXTERNAL) // some frames might use malloc
            {
            if (pointer != 0)
                {
                nativeFreeFrame(pointer);
                clearPointer();
                }
            }
        super.destructor();
        }

    //----------------------------------------------------------------------------------------------
    // Conversion
    //----------------------------------------------------------------------------------------------

    public void copyToBitmap(Bitmap bitmap)
        {
        switch (getFrameFormat())
            {
            case YUY2:  yuy2ToBitmap(bitmap);
            default:    break; // throw?
            }
        }

    protected Element elementOf(RenderScript rs, Bitmap bitmap)
        {
        switch (bitmap.getConfig())
            {
            case ALPHA_8: return Element.A_8(rs);
            case RGB_565: return Element.RGB_565(rs);
            case ARGB_4444: return Element.RGBA_4444(rs);
            case ARGB_8888: return Element.RGBA_8888(rs);
            }
        throw AppUtil.getInstance().unreachable();
        }

    /*
     * https://msdn.microsoft.com/en-us/library/windows/desktop/dd206750(v=vs.85).aspx
     * formulas: https://msdn.microsoft.com/en-us/library/ms893078.aspx
     * https://github.com/yigalomer/Yuv2RgbRenderScript/blob/master/src/com/example/yuv2rgbrenderscript/RenderScriptHelper.java
     * C:\Android\410c\build\frameworks\rs\cpu_ref\rsCpuIntrinsicYuvToRGB.cpp
     *
     * See also the native version of this in jni_frame.cpp
     */
    protected void yuy2ToBitmap(final Bitmap bitmap)
        {
        if (!getContext().lockRenderScriptWhile(1, TimeUnit.SECONDS, new Runnable()
            {
            @Override public void run()
                {
                if (useNativeFormatConversion)
                    {
                    nativeYuy2ToBitmap(pointer, bitmap);
                    }
                else
                    {
                    RenderScript rs = getContext().getRenderScript();

                    int width = getWidth(); Assert.assertTrue(Misc.isEven(width));
                    int height = getHeight();

                    Type.Builder inTypeBuilder = new Type.Builder(rs, Element.U8_4(rs))
                        .setX(width/2)  // we clump two pixels together horizontally
                        .setY(height);
                    Type inType = inTypeBuilder.create();
                    Allocation aIn = Allocation.createTyped(rs, inType, Allocation.USAGE_SCRIPT);
                    byte[] array = UvcFrame.this.getImageData();
                    aIn.copyFromUnchecked(array);

                    Type.Builder outTypeBuilder = new Type.Builder(rs, elementOf(rs,bitmap))
                        .setX(width)
                        .setY(height);
                    Type outType = outTypeBuilder.create();
                    Allocation aOut = Allocation.createTyped(rs, outType, Allocation.USAGE_SCRIPT);

                    ScriptC_format_convert script = new ScriptC_format_convert(rs);
                    script.set_inputAllocation(aIn);
                    script.set_outputWidth(width);
                    script.set_outputHeight(height);
                    switch (bitmap.getConfig())
                        {
                        case ARGB_8888:
                            script.forEach_yuv2_to_argb8888(aOut);
                            break;
                        default:
                            RobotLog.ww(getTag(), "conversion to %s not yet implemented; ignored", bitmap.getConfig());
                            break;
                        }

                    // https://developer.android.com/guide/topics/renderscript/compute.html#asynchronous-model
                    aOut.copyTo(bitmap); // synchronous
                    }
                }
            }))
            {
            RobotLog.ee(getTag(), "failed to access RenderScript: frameNumber=%d", getFrameNumber());
            }

        }

    protected native static void nativeYuy2ToBitmap(long pointer, Bitmap bitmap);

    //----------------------------------------------------------------------------------------------
    // Accessing
    //----------------------------------------------------------------------------------------------

    public UvcContext getContext()
        {
        return getParent();
        }

    public int getWidth()
        {
        return getInt(Fields.width.offset());
        }

    public int getHeight()
        {
        return getInt(Fields.height.offset());
        }

    public UvcFrameFormat getFrameFormat()
        {
        return UvcFrameFormat.from(getInt(Fields.frameFormat.offset()));
        }

    public int getStride()
        {
        return getInt(Fields.stride.offset());
        }

    /** Frame number. May skip if we drop frames. Is strictly monotonically increasing. */
    public long getFrameNumber()
        {
        return getUInt(Fields.frameNumber.offset());
        }

    public long getCaptureTime()
        {
        return getLong(Fields.captureTime.offset());
        }

    public ByteBuffer getImageByteBuffer()
        {
        ByteBuffer result = (ByteBuffer) nativeGetImageByteBuffer(pointer);
        result.order(this.byteOrder);
        return result;
        }

    public byte[] getImageData()
        {
        return getImageData(new byte[getImageSize()]);
        }

    public byte[] getImageData(byte[] byteArray)
        {
        int cbNeeded = getImageSize();
        if (byteArray.length != cbNeeded)
            {
            byteArray = new byte[cbNeeded];
            }
        nativeCopyImageData(pointer, byteArray, byteArray.length);
        return byteArray;
        }

    public int getImageSize()
        {
        return getSizet(Fields.cbData.offset());
        }

    public long getImageBuffer()
        {
        return getLong(Fields.pbData.offset());
        }

    //----------------------------------------------------------------------------------------------
    // Native
    //----------------------------------------------------------------------------------------------

    protected static int[] fieldOffsets = nativeGetFieldOffsets(Fields.values().length);

    protected int getStructSize()
        {
        return fieldOffsets[Fields.sizeof.ordinal()];
        }

    protected enum Fields
        {
        sizeof,
        pbData,
        cbData,
        cbAllocated,
        width,
        height,
        frameFormat,
        stride,
        frameNumber,
        pts,
        captureTime,
        sourceClockReference,
        pContext;
        public int offset() { return fieldOffsets[this.ordinal()]; }
        }

    protected native static int[] nativeGetFieldOffsets(int cFieldExpected);
    protected native static Object nativeGetImageByteBuffer(long pointer);
    protected native static void nativeCopyImageData(long pointer, byte[] byteArray, int byteArrayLength);
    protected native static long nativeCopyFrame(long pointer);
    protected native static void nativeFreeFrame(long pointer);
    }
