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

import org.firstinspires.ftc.robotcore.external.android.util.Size;
import org.firstinspires.ftc.robotcore.internal.system.Misc;
import org.firstinspires.ftc.robotcore.internal.system.NativeObject;
import org.firstinspires.ftc.robotcore.internal.camera.ImageFormatMapper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Java manifestation of uvc_format_desc_t
 */
@SuppressWarnings("WeakerAccess")
public class UvcFormatDesc extends NativeObject<UvcStreamingInterface>
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected static Charset charset = Charset.forName("UTF8");

    public enum Subtype
        {
        UNDEFINED(0x00),
        INPUT_HEADER(0x01),
        OUTPUT_HEADER(0x02),
        STILL_IMAGE_FRAME(0x03),
        FORMAT_UNCOMPRESSED(0x04),
        FRAME_UNCOMPRESSED(0x05),
        FORMAT_MJPEG(0x06),
        FRAME_MJPEG(0x07),
        FORMAT_MPEG2TS(0x0a),
        FORMAT_DV(0x0c),
        COLORFORMAT(0x0d),
        FORMAT_FRAME_BASED(0x10),   // Table 3-1, USB Device Class Definition for Video Devices: Frame Based Payload
        FRAME_FRAME_BASED(0x11),    // Table 3-2, USB Device Class Definition for Video Devices: Frame Based Payload
        FORMAT_STREAM_BASED(0x12);
        byte value;
        Subtype(int value) { this.value = (byte)value;}
        public static Subtype from(int value)
            {
            for (Subtype subtype : Subtype.values())
                {
                if (subtype.value == value) return subtype;
                }
            return UNDEFINED;
            }
        };

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public UvcFormatDesc(long pointer, UvcStreamingInterface parent)
        {
        super(pointer, TraceLevel.VeryVerbose);
        setParent(parent);
        }

    //----------------------------------------------------------------------------------------------
    // Accessing
    //----------------------------------------------------------------------------------------------

    public Subtype getSubtype()
        {
        return Subtype.from(getUByte(fieldOffsets[Fields.bDescriptorSubtype.ordinal()]));
        }

    /** The bFormatIndex field contains the one-based index of this format descriptor, and is used
     * by requests from the host to set and get the current video format */
    public int getFormatIndex()
        {
        return getUByte(fieldOffsets[Fields.bFormatIndex.ordinal()]);
        }

    public int getFrameDescriptorCount()
        {
        return getUByte(fieldOffsets[Fields.bNumFrameDescriptors.ordinal()]);
        }

    public UUID getGuidFormat()
        {
        return Misc.uuidFromBytes(getBytes(fieldOffsets[Fields.guidFormat.ordinal()], 16), ByteOrder.LITTLE_ENDIAN);
        }

    public String getFourCCFormat()
        {
        byte[] bytes = getBytes(fieldOffsets[Fields.fourccFormat.ordinal()], 4);
        // Eliminate trailing nuls (if any)
        while (bytes.length > 0 && bytes[bytes.length-1] == 0)
            {
            bytes = Arrays.copyOfRange(bytes, 0, bytes.length-1);
            }
        return charset.decode(ByteBuffer.wrap(bytes)).toString();
        }

    public boolean isAndroidFormat(int androidFormat)
        {
        for (ImageFormatMapper.Format format : ImageFormatMapper.allFromGuid(getGuidFormat()))
            {
            if (format.android == androidFormat)
                {
                return true;
                }
            }
        return false;
        }

    /** Number of bits per pixel used to specify color in the decoded video frame. May be zero if not applicable */
    public int getBitsPerPixel()
        {
        return getUByte(fieldOffsets[Fields.bBitsPerPixel.ordinal()]);
        }

    /** Flags for JPEG stream */
    public int getFlags()
        {
        return getUByte(fieldOffsets[Fields.bmFlags.ordinal()]);
        }

    /** Optimum Frame Index (used to select resolution) for this stream */
    public int getDefaultFrameIndex()
        {
        return getUByte(fieldOffsets[Fields.bDefaultFrameIndex.ordinal()]);
        }

    public UvcFrameDesc getDefaultFrameDesc()
        {
        UvcFrameDesc result = null;
        int defaultFrameIndex = getDefaultFrameIndex();
        for (UvcFrameDesc uvcFrameDesc : getFrameDescriptors())
            {
            if (uvcFrameDesc.getFrameIndex() == defaultFrameIndex)
                {
                result = uvcFrameDesc;
                result.addRef();
                }
            uvcFrameDesc.releaseRef();
            }
        return result;
        }

    /** The X dimension of the picture aspect ratio */
    public int getAspectRatioX()
        {
        return getUByte(fieldOffsets[Fields.bAspectRatioX.ordinal()]);
        }

    /** The Y dimension of the picture aspect ratio. */
    public int getAspectRatioY()
        {
        return getUByte(fieldOffsets[Fields.bAspectRatioY.ordinal()]);
        }

    public Size getAspectRatio()
        {
        return new Size(getAspectRatioX(), getAspectRatioY());
        }

    /** Specifies interlace information. If the scanning mode control in the Camera Terminal is supported
     * for this stream, this field shall reflect the field format used in interlaced mode. See
     * Table 3-1 of USB Device Class Definition for Video Devices: Frame Based Payload */
    public int getInterlaceFlags()
        {
        return getUByte(fieldOffsets[Fields.bmInterlaceFlags.ordinal()]);
        }

    /** Specifies whether duplication of the video stream is restricted */
    public boolean getCopyProtect()
        {
        return getUByte(fieldOffsets[Fields.bCopyProtect.ordinal()]) != 0;
        }

    /** Specifies whether the data within the frame is of variable length from frame to frame. */
    public boolean getVariableSize()
        {
        return getUByte(fieldOffsets[Fields.bVariableSize.ordinal()]) != 0;
        }

    /** Available frame descriptors for this format */
    public List<UvcFrameDesc> getFrameDescriptors()
        {
        List<UvcFrameDesc> result = new ArrayList<>();
        for (long pFramePointer : nativeGetLinkedList(pointer, Fields.frame_descs.offset()))
            {
            result.add(new UvcFrameDesc(pFramePointer, this));
            }
        return result;
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
        bFormatIndex,
        bNumFrameDescriptors,
        guidFormat,
        fourccFormat,
        bBitsPerPixel,
        bmFlags,
        bDefaultFrameIndex,
        bAspectRatioX,
        bAspectRatioY,
        bmInterlaceFlags,
        bCopyProtect,
        bVariableSize,
        frame_descs;
        public int offset() { return fieldOffsets[this.ordinal()]; }
        }

    protected native static int[] nativeGetFieldOffsets(int cFieldExpected);
    }
