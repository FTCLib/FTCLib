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
package org.firstinspires.ftc.robotcore.internal.camera.libuvc.nativeobject;

import org.firstinspires.ftc.robotcore.internal.system.NativeObject;

/**
 * The Java manifestation of a native Vuforia::ExternalProvider::CameraFrame
 */
@SuppressWarnings("WeakerAccess")
public class VuforiaExternalProviderCameraFrame extends NativeObject
    {
    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public VuforiaExternalProviderCameraFrame()
        {
        super(TraceLevel.None);
        allocateMemory(getStructSize());
        }

    public long getPointer()
        {
        return pointer;
        }

    //----------------------------------------------------------------------------------------------
    // Accessing
    //----------------------------------------------------------------------------------------------

    public long getTimestamp()
        {
        return getLong(Fields.timestamp.offset());
        }
    public void setTimestamp(long value)
        {
        setLong(Fields.timestamp.offset(), value);
        }

    public long getExposureTime()
        {
        return getLong(Fields.exposureTime.offset());
        }
    public void setExposureTime(long value)
        {
        setLong(Fields.exposureTime.offset(), value);
        }

    public long getBuffer()
        {
        return getPointer(Fields.buffer.offset());
        }
    public void setBuffer(long pBuffer)
        {
        setPointer(Fields.buffer.offset(), pBuffer);
        }

    public int getBufferSize()
        {
        return getInt(Fields.bufferSize.offset());
        }
    public void setBufferSize(int cb)
        {
        setInt(Fields.bufferSize.offset(), cb);
        }

    public int getFrameIndex()
        {
        return getInt(Fields.index.offset());
        }
    public void setFrameIndex(int frameIndex)
        {
        setInt(Fields.index.offset(), frameIndex);
        }

    public int getWidth()
        {
        return getInt(Fields.width.offset());
        }
    public void setWidth(int value)
        {
        setInt(Fields.width.offset(), value);
        }

    public int getHeight()
        {
        return getInt(Fields.height.offset());
        }
    public void setHeight(int value)
        {
        setInt(Fields.height.offset(), value);
        }

    public int getStride()
        {
        return getInt(Fields.stride.offset());
        }
    public void setStride(int cb)
        {
        setInt(Fields.stride.offset(), cb);
        }

    public int getFormat()
        {
        return getInt(Fields.format.offset());
        }
    public void setFormat(int format)
        {
        setInt(Fields.format.offset(), format);
        }

    protected int cbIntrinsics()
        {
        return getStructSize() - Fields.intrinsics.offset();
        }
    // We don't here (for now) support setting and getting the intrinsics array

    //----------------------------------------------------------------------------------------------
    // Native
    //----------------------------------------------------------------------------------------------

    protected static final int[] fieldOffsets = nativeGetFieldOffsets(Fields.values().length);

    protected int getStructSize()
        {
        return fieldOffsets[Fields.sizeof.ordinal()];
        }

    protected enum Fields
        {
        sizeof,
        timestamp,
        exposureTime,
        buffer,
        bufferSize,
        index,
        width,
        height,
        stride,
        format,
        intrinsics;
        public int offset() { return fieldOffsets[this.ordinal()]; }
        }

    protected native static int[] nativeGetFieldOffsets(int cFieldExpected);
    }
