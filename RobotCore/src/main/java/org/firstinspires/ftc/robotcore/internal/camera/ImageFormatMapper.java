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
package org.firstinspires.ftc.robotcore.internal.camera;

import android.graphics.ImageFormat;
import android.graphics.PixelFormat;

import com.qualcomm.robotcore.util.ClassUtil;
import com.vuforia.PIXEL_FORMAT;

import org.firstinspires.ftc.robotcore.internal.vuforia.externalprovider.FrameFormat;
import org.firstinspires.ftc.robotcore.internal.camera.libuvc.constants.UvcFrameFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * {@link ImageFormatMapper} manages conversions between various image formats identifiers
 *
 * @see <a href="https://library.vuforia.com/reference/api/cpp/namespaceVuforia.html#a8f7511b96bcb33bc2ea176a1a8dafb59">Vuforia PIXEL_FORMAT</a>
 */
@SuppressWarnings("WeakerAccess")
public class ImageFormatMapper
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static class Format
        {
        public final String name;
        /** A value from {@link PixelFormat} or {@link ImageFormat}*/ public final int android;
        /** A value from {@link UvcFrameFormat} */ public final UvcFrameFormat uvc;
        /** A value from {@link PIXEL_FORMAT} */ public final int vuforiaPixelFormat;
        public final FrameFormat vuforiaWebcam;
        public final UUID guid;
        public final String fourCC;

        public Format(String name, int android, UvcFrameFormat uvc, int vuforiaPixelFormat, FrameFormat vuforiaWebcam, UUID guid, String fourCC)
            {
            this.name = name;
            this.android = android;
            this.uvc = uvc;
            this.vuforiaPixelFormat = vuforiaPixelFormat;
            this.vuforiaWebcam = vuforiaWebcam;
            this.guid = guid;
            this.fourCC = fourCC;
            }
        }

    // https://www.loc.gov/preservation/digital/formats/fdd/fdd000364.shtml
    // https://www.fourcc.org/pixel-format/yuv-yuy2/
    // https://www.fourcc.org/pixel-format/yuv-y800/
    // https://msdn.microsoft.com/en-us/library/windows/desktop/dd757532(v=vs.85).aspx
    // https://msdn.microsoft.com/en-us/library/ee495731.aspx
    protected static Format[] formats = new Format[]
        {
        new Format("YUY2",      ImageFormat.YUY2,        UvcFrameFormat.YUY2,    PIXEL_FORMAT.UNKNOWN_FORMAT,   FrameFormat.YUYV,    UUID.fromString("32595559-0000-0010-8000-00AA00389B71"), "YUY2"),
        new Format("H264",      ImageFormat.UNKNOWN,     UvcFrameFormat.UNKNOWN, PIXEL_FORMAT.UNKNOWN_FORMAT,   FrameFormat.UNKNOWN, UUID.fromString("34363248-0000-0010-8000-00AA00389B71"), "H264"),
        new Format("MJPG",      ImageFormat.UNKNOWN,     UvcFrameFormat.UNKNOWN, PIXEL_FORMAT.UNKNOWN_FORMAT,   FrameFormat.UNKNOWN, UUID.fromString("47504A4D-0000-0000-0000-000000000000"), "MJPG"),    // seen in the wild
        new Format("RGB565",    PixelFormat.RGB_565,     UvcFrameFormat.UNKNOWN, PIXEL_FORMAT.RGB565,           FrameFormat.UNKNOWN, UUID.fromString("e436eb7b-524f-11ce-9f53-0020af0ba770"), null),
        new Format("RGB888",    PixelFormat.RGB_888,     UvcFrameFormat.RGB,     PIXEL_FORMAT.RGB888,           FrameFormat.UNKNOWN, UUID.fromString("e436eb7d-524f-11ce-9f53-0020af0ba770"), null),
        new Format("RGB8888",   PixelFormat.RGBA_8888,   UvcFrameFormat.UNKNOWN, PIXEL_FORMAT.RGBA8888,         FrameFormat.UNKNOWN, UUID.fromString("e436eb7e-524f-11ce-9f53-0020af0ba770"), null),
        new Format("Y8",        getImageFormatConst("Y8"), UvcFrameFormat.GRAY8, PIXEL_FORMAT.GRAYSCALE,  FrameFormat.UNKNOWN, UUID.fromString("20203859-0000-0010-8000-00AA00389B71"), "Y8"),
        new Format("Y800",      getImageFormatConst("Y8"), UvcFrameFormat.GRAY8, PIXEL_FORMAT.GRAYSCALE,  FrameFormat.UNKNOWN, UUID.fromString("30303859-0000-0010-8000-00AA00389B71"), "Y800"),
        new Format("GREY",      getImageFormatConst("Y8"), UvcFrameFormat.GRAY8, PIXEL_FORMAT.GRAYSCALE,  FrameFormat.UNKNOWN, UUID.fromString("59455247-0000-0010-8000-00AA00389B71"), "GREY"),
        new Format("YV12",      ImageFormat.YV12,        UvcFrameFormat.UNKNOWN, PIXEL_FORMAT.UNKNOWN_FORMAT,   FrameFormat.UNKNOWN, UUID.fromString("32315659-0000-0010-8000-00AA00389B71"), "YV12"),
        new Format("NV12",      ImageFormat.UNKNOWN,     UvcFrameFormat.UNKNOWN, PIXEL_FORMAT.UNKNOWN_FORMAT,   FrameFormat.UNKNOWN, UUID.fromString("3231564E-0000-0010-8000-00AA00389B71"), "NV12"),
        new Format("M420",      ImageFormat.UNKNOWN,     UvcFrameFormat.UNKNOWN, PIXEL_FORMAT.UNKNOWN_FORMAT,   FrameFormat.UNKNOWN, UUID.fromString("3032344D-0000-0010-8000-00AA00389B71"), "M420"),
        new Format("I420",      ImageFormat.UNKNOWN,     UvcFrameFormat.UNKNOWN, PIXEL_FORMAT.UNKNOWN_FORMAT,   FrameFormat.UNKNOWN, UUID.fromString("30323449-0000-0010-8000-00AA00389B71"), "I420"),
        };

    protected static int getImageFormatConst(String name)
        {
        try {
            return ClassUtil.getDeclaredField(ImageFormat.class, name).getInt(null);
            }
        catch (IllegalAccessException|RuntimeException e)
            {
            throw new RuntimeException("internal error", e);
            }
        }

    //----------------------------------------------------------------------------------------------
    // Accessing
    //----------------------------------------------------------------------------------------------

    public static Format[] all()
        {
        return formats;
        }

    public static List<Format> allFromGuid(UUID uuid)
        {
        List<Format> result = new ArrayList<>();
        for (Format format : formats)
            {
            if (format.guid.equals(uuid))
                {
                result.add(format);
                }
            }
        return result;
        }

    public static List<Format> allFromAndroid(int android)
        {
        List<Format> result = new ArrayList<>();
        for (Format format : formats)
            {
            if (format.android == android)
                {
                result.add(format);
                }
            }
        return result;
        }

    public static List<Format> allFromUvc(UvcFrameFormat uvc)
        {
        List<Format> result = new ArrayList<>();
        for (Format format : formats)
            {
            if (format.uvc == uvc)
                {
                result.add(format);
                }
            }
        return result;
        }

    public static List<Format> allFromVuforiaPixelFormat(int vuforiaPixelFormat)
        {
        List<Format> result = new ArrayList<>();
        for (Format format : formats)
            {
            if (format.vuforiaPixelFormat == vuforiaPixelFormat)
                {
                result.add(format);
                }
            }
        return result;
        }

    public static List<Format> allFromVuforiaWebcam(FrameFormat vuforiaWebcam)
        {
        List<Format> result = new ArrayList<>();
        for (Format format : formats)
            {
            if (format.vuforiaWebcam == vuforiaWebcam)
                {
                result.add(format);
                }
            }
        return result;
        }


    public static UvcFrameFormat uvcFromAndroid(int android)
        {
        for (Format format : formats)
            {
            if (format.android == android)
                {
                return format.uvc;
                }
            }
        return UvcFrameFormat.UNKNOWN;
        }

    public static FrameFormat vuforiaWebcamFromUvc(UvcFrameFormat uvc)
        {
        for (Format format : formats)
            {
            if (format.uvc == uvc)
                {
                return format.vuforiaWebcam;
                }
            }
        return FrameFormat.UNKNOWN;
        }

    public static FrameFormat vuforiaWebcamFromAndroid(int android)
        {
        for (Format format : formats)
            {
            if (format.android == android)
                {
                return format.vuforiaWebcam;
                }
            }
        return FrameFormat.UNKNOWN;
        }

    public static int androidFromVuforiaWebcam(FrameFormat vuforiaWebcam)
        {
        for (Format format : formats)
            {
            if (format.vuforiaWebcam == vuforiaWebcam)
                {
                return format.android;
                }
            }
        return ImageFormat.UNKNOWN;
        }

    public static int androidFromVuforiaPixelFormat(int vuforiaPixelFormat)
        {
        for (Format format : formats)
            {
            if (format.vuforiaPixelFormat == vuforiaPixelFormat)
                {
                return format.android;
                }
            }
        return ImageFormat.UNKNOWN;
        }

    }
