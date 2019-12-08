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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.firstinspires.ftc.robotcore.internal.system.NativeObject;

/**
 * {@link UvcStreamCtrl} is the java manifestation of uvc_stream_ctrl_t*
 */
@SuppressWarnings("WeakerAccess")
public class UvcStreamCtrl extends NativeObject<UvcDeviceHandle>
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = UvcStreamCtrl.class.getSimpleName();
    public String getTag() { return TAG; }

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public UvcStreamCtrl(@NonNull UvcDeviceHandle deviceHandle)
        {
        super();
        allocateMemory(getStructSize());
        setParent(deviceHandle);
        }

    //----------------------------------------------------------------------------------------------
    // Accessing
    //----------------------------------------------------------------------------------------------

    protected long getPointer()
        {
        return this.pointer;
        }

    public String toStringVerbose()
        {
        return nativePrint(pointer, parent.getUvcContext().getPointer());
        }

    public int getHint()
        {
        return getUShort(Fields.bmHint.offset());
        }
    public int getFormatIndex()
        {
        return getUByte(Fields.bFormatIndex.offset());
        }
    public int getFrameIndex()
        {
        return getUByte(Fields.bFrameIndex.offset());
        }
    public long getNsFrameInterval()
        {
        return getUInt(Fields.dwFrameInterval.offset());
        }
    public int getKeyFrameRate()
        {
        return getUShort(Fields.wKeyFrameRate.offset());
        }
    /** @return the rate in frames per second */
    public int getFrameRate()
        {
        return getUShort(Fields.wPFrameRate.offset());
        }
    public int getCompQuality()
        {
        return getUShort(Fields.wCompQuality.offset());
        }
    public int getCompWindowSize()
        {
        return getUShort(Fields.wCompWindowSize.offset());
        }
    public int getDelay()
        {
        return getUShort(Fields.wDelay.offset());
        }
    public long getMaxVideoFrameSize()
        {
        return getUInt(Fields.dwMaxVideoFrameSize.offset());
        }
    public long getMaxPayloadTransferSize()
        {
        return getUInt(Fields.dwMaxPayloadTransferSize.offset());
        }
    public long getClockFrequency()
        {
        return getUInt(Fields.dwClockFrequency.offset());
        }
    public int getFramingInfo()
        {
        return getUByte(Fields.bmFramingInfo.offset());
        }
    public int getPreferredVersion()
        {
        return getUByte(Fields.bPreferredVersion.offset());
        }
    public int getMinVersion()
        {
        return getUByte(Fields.bMinVersion.offset());
        }
    public int getMaxVersion()
        {
        return getUByte(Fields.bMaxVersion.offset());
        }
    public int getInterfaceNumber()
        {
        return getUByte(Fields.bInterfaceNumber.offset());
        }

    //----------------------------------------------------------------------------------------------
    // Streaming
    //----------------------------------------------------------------------------------------------

    /**
     * Opens a stream handle to this control, which can then be used to start streaming.
     * Note that the returned stream handle is allowed to outlive this control object.
     */
    public @Nullable UvcStreamHandle open()
        {
        long pointerHandle = nativeOpen(getParent().getPointer(), pointer);
        return pointerHandle != 0 ? new UvcStreamHandle(pointerHandle, getParent()) : null;
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
        bmHint,
        bFormatIndex,
        bFrameIndex,
        dwFrameInterval,
        wKeyFrameRate,
        wPFrameRate,
        wCompQuality,
        wCompWindowSize,
        wDelay,
        dwMaxVideoFrameSize,
        dwMaxPayloadTransferSize,
        dwClockFrequency,
        bmFramingInfo,
        bPreferredVersion,
        bMinVersion,
        bMaxVersion,
        bInterfaceNumber;
        public int offset() { return fieldOffsets[this.ordinal()]; }
        }

    protected native static int[] nativeGetFieldOffsets(int count);

    protected native static String nativePrint(long pointer, long pointerUvcContex);
    protected native static long nativeOpen(long pointerDeviceHandle, long pointerStreamControl);
    }
