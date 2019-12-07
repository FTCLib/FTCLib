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

import com.qualcomm.robotcore.util.TypeConversion;

import org.firstinspires.ftc.robotcore.external.android.util.Size;
import org.firstinspires.ftc.robotcore.internal.system.NativeObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bob on 2017-06-17.
 */
@SuppressWarnings("WeakerAccess")
public class UvcFrameDesc extends NativeObject<UvcFormatDesc>
    {
    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public UvcFrameDesc(long pointer, UvcFormatDesc parent)
        {
        super(pointer, TraceLevel.Verbose);
        setParent(parent);
        }

    //----------------------------------------------------------------------------------------------
    // Accessing
    //----------------------------------------------------------------------------------------------

    /** The frame index field contains the one-based index of this frame descriptor, and is used by
     * requests from the host to set and get the current frame index for the format in use. This
     * index is one-based for each corresponding format descriptor supported by the device */
    public int getFrameIndex()
        {
        return getUByte(Fields.bFrameIndex.offset());
        }

    /** Specifies whether still images are supported at this frame setting. This is only applicable for
     * VS interfaces with an IN video endpoint using Still Image Capture Method 1, and should be set
     * to 0 in all other cases */
    public boolean isStillImageSupported()
        {
        return (getUByte(Fields.bmCapabilities.offset()) & 0x01) != 0;
        }

    /** Specifies whether the device provides a fixed frame rate on a stream associated with this frame
     * descriptor. Set to 1 if fixed rate is enabled; otherwise, set to 0.*/
    public boolean isFixedFrameRate()
        {
        return (getUByte(Fields.bmCapabilities.offset()) & 0x02) != 0;
        }

    /** Width of decoded bitmap frame in pixels */
    public int getWidth()
        {
        return getUShort(Fields.wWidth.offset());
        }
    /** Height of decoded bitmap frame in pixels */
    public int getHeight()
        {
        return getUShort(Fields.wHeight.offset());
        }
    public Size getSize()
        {
        return new Size(getWidth(), getHeight());
        }

    /** Specifies the minimum bit rate at the longest frame interval in units of bps at which the
     * data can be transmitted */
    public long getMinBitRate()
        {
        return getUInt(Fields.dwMinBitRate.offset());
        }

    /** Specifies the maximum bit rate at the shortest frame interval in units of bps at which
     * the data can be transmitted. */
    public long getMaxBitRate()
        {
        return getUInt(Fields.dwMaxBitRate.offset());
        }

    /** Specifies the frame interval the device would like to indicate for use as a default (100ns units)
     * This must be a valid frame interval described in the fields below. */
    public long getDefaultFrameInterval()
        {
        return getUInt(Fields.dwDefaultFrameInterval.offset());
        }

    /** Indicates how the frame interval can be programmed: 0: Continuous frame interval 1..255: The
     * number of discrete frame intervals supported (n) */
    public int getFrameIntervalType()
        {
        return getUByte(Fields.bFrameIntervalType.offset());
        }

    /** Specifies the number of bytes per line of video for packed fixed frame size formats, allowing
     * the receiver to perform stride alignment of the video. If the bVariableSize value (above) is
     * TRUE (1), or if the format does not permit such alignment, this value shall be set to zero (0).*/
    public long getBytesPerLine()
        {
        return getUInt(Fields.dwBytesPerLine.offset());
        }

    /** Available frame intervals, (in 100ns units), shortest to longest */
    public List<Long> getFrameIntervals()
        {
        List<Long> result = new ArrayList<>();
        long pIntervals = nativeGetPointer(pointer, Fields.rgIntervals.offset());
        if (pIntervals != 0)
            {
            // Discrete frame intervals
            for (long frameRate : nativeGetNullTerminatedList(pIntervals, 0, 4))
                {
                result.add(TypeConversion.unsignedIntToLong((int)frameRate));
                }
            }
        else
            {
            // Continuous frame intervals
            long min = getMinFrameIntervalContinuous();
            long max = getMaxFrameIntervalContinuous();
            long step = getFrameIntervalStepContinuous();
            for (long cur = min; cur <= max; cur += step)
                {
                result.add(cur);
                }
            }
        return result;
        }

    public long getMinFrameInterval()
        {
        return hasDiscreteFrameIntervals()
                ? getFrameIntervals().get(0)
                : getMinFrameIntervalContinuous();
        }

    protected long getMaxFrameInterval()
        {
        if (hasDiscreteFrameIntervals())
            {
            List<Long> intervals = getFrameIntervals();
            return intervals.get(intervals.size()-1);
            }
        else
            {
            return getMaxFrameIntervalContinuous();
            }
        }

    public boolean hasDiscreteFrameIntervals()
        {
        return nativeGetPointer(pointer, Fields.rgIntervals.offset()) != 0;
        }

    /** Shortest frame interval supported (at highest frame rate), in 100 ns units */
    public long getMinFrameIntervalContinuous()
        {
        return getUInt(Fields.dwMinFrameInterval.offset());
        }
    /** Longest frame interval supported (at lowest frame rate), in 100 ns units. */
    public long getMaxFrameIntervalContinuous()
        {
        return getUInt(Fields.dwMaxFrameInterval.offset());
        }
    /** Indicates granularity of frame interval range, in 100 ns units. */
    public long getFrameIntervalStepContinuous()
        {
        return getUInt(Fields.dwFrameIntervalStep.offset());
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
        bDescriptorSubtype,
        bFrameIndex,
        bmCapabilities,
        wWidth,
        wHeight,
        dwMinBitRate,
        dwMaxBitRate,
        dwMaxVideoFrameBufferSize,
        dwDefaultFrameInterval,
        dwMinFrameInterval,
        dwMaxFrameInterval,
        dwFrameIntervalStep,
        bFrameIntervalType,
        dwBytesPerLine,
        rgIntervals;
        public int offset() { return fieldOffsets[this.ordinal()]; }
        }

    protected native static int[] nativeGetFieldOffsets(int cFieldExpected);
    }
