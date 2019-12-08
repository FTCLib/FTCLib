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
package org.firstinspires.ftc.robotcore.internal.camera.libuvc.api;

import android.graphics.ImageFormat;
import androidx.annotation.Nullable;
import android.util.Pair;
import android.util.SparseArray;

import org.firstinspires.ftc.robotcore.external.android.util.Size;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCharacteristics;
import org.firstinspires.ftc.robotcore.internal.camera.ImageFormatMapper;
import org.firstinspires.ftc.robotcore.internal.camera.libuvc.nativeobject.UvcFormatDesc;
import org.firstinspires.ftc.robotcore.internal.camera.libuvc.nativeobject.UvcFrameDesc;
import org.firstinspires.ftc.robotcore.internal.camera.libuvc.nativeobject.UvcStreamingInterface;
import org.firstinspires.ftc.robotcore.internal.system.DestructOnFinalize;
import org.firstinspires.ftc.robotcore.internal.system.Misc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by bob on 2017-06-17.
 */
@SuppressWarnings("WeakerAccess")
public class UvcApiCameraCharacteristicsBuilder extends DestructOnFinalize
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected List<UvcStreamingInterface> streamingInterfaces;
    UvcApiCameraCharacteristics built = null;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public UvcApiCameraCharacteristicsBuilder()
        {
        streamingInterfaces = new ArrayList<>();
        }

    public void addStream(UvcStreamingInterface streamingInterface)
        {
        synchronized (lock)
            {
            streamingInterfaces.add(streamingInterface);
            streamingInterface.addRef();
            }
        }

    @Override protected void destructor()
        {
        releaseStreams();
        super.destructor();
        }

    public void releaseStreams()
        {
        for (UvcStreamingInterface streamingInterface : streamingInterfaces)
            {
            streamingInterface.releaseRef();
            }
        streamingInterfaces.clear();
        }

    /** Disconnect ourselves from the underlying native data so that we have
     * a more normal-behaving Java object that users don't have to worry about
     * closing, or ref counting, or lifetime interaction issues with the rest
     * of the underlying UVC infrastructure */
    public UvcApiCameraCharacteristics build()
        {
        synchronized (lock)
            {
            if (null == built)
                {
                built = internalBuild();
                releaseStreams();
                }
            return built;
            }
        }

    /** call with lock held */
    protected UvcApiCameraCharacteristics internalBuild()
        {
        UvcApiCameraCharacteristics result = built;

        // Make building idempotent
        if (result == null)
            {
            result = new UvcApiCameraCharacteristics();
            SparseArray<String> formatNameMap = new SparseArray<>();
            for (UvcStreamingInterface uvcStreamingInterface : streamingInterfaces)
                {
                for (UvcFormatDesc formatDesc : uvcStreamingInterface.getFormatDescriptors())
                    {
                    try {
                        for (ImageFormatMapper.Format format : ImageFormatMapper.allFromGuid(formatDesc.getGuidFormat()))
                            {
                            if (format.android != ImageFormat.UNKNOWN)
                                {
                                if (formatNameMap.indexOfKey(format.android) < 0) // use only first name we find
                                    {
                                    formatNameMap.put(format.android, format.name);
                                    }
                                break;
                                }
                            }
                        }
                    finally
                        {
                        formatDesc.releaseRef();
                        }
                    }
                }

            int[] outputAndroidFormats = new int[formatNameMap.size()];
            String[] outputFormatNames = new String[formatNameMap.size()];
            for (int i = 0; i < formatNameMap.size(); i++)
                {
                outputAndroidFormats[i] = formatNameMap.keyAt(i);
                outputFormatNames[i] = formatNameMap.get(outputAndroidFormats[i]);
                }
            result.setOutputFormats(outputAndroidFormats, outputFormatNames);

            SparseArray<Size[]> outputSizesMap = new SparseArray<Size[]>();
            SparseArray<Size> defaultOutputSizeMap = new SparseArray<Size>();
            Map<Pair<Integer,Size>, UvcApiCameraCharacteristics.FormatSizeInfo> formatSizeInfoMap = new HashMap<>();

            for (int androidFormat : outputAndroidFormats)
                {
                Size[] outputSizes = getSizesOfAndroidFormat(androidFormat);
                outputSizesMap.put(androidFormat, outputSizes);
                defaultOutputSizeMap.put(androidFormat, getDefaultSizeOfAndroidFormat(androidFormat));
                for (Size size : outputSizes)
                    {
                    long nsMinDuration = getNsMinFrameDurationOfAndroidFormat(androidFormat, size);
                    if (nsMinDuration != 0)   // no point in storing those
                        {
                        formatSizeInfoMap.put(Pair.create(androidFormat, size), new UvcApiCameraCharacteristics.FormatSizeInfo(nsMinDuration));
                        }
                    }
                }

            result.setMaps(outputSizesMap, defaultOutputSizeMap, formatSizeInfoMap);
            }

        return result;
        }

    public static  UvcApiCameraCharacteristics buildFromModes(@Nullable Collection<CameraCharacteristics.CameraMode> allCameraModes)
        {
        if (allCameraModes==null)
            {
            allCameraModes = new ArrayList<>();
            }

        SparseArray<List<Size>> outputSizesListMap = new SparseArray<>();
        SparseArray<String> formatNameMap = new SparseArray<>();
        SparseArray<Size> defaultOutputSizeMap = new SparseArray<Size>();
        Map<Pair<Integer,Size>, UvcApiCameraCharacteristics.FormatSizeInfo> formatSizeInfoMap = new HashMap<>();

        for (CameraCharacteristics.CameraMode mode : allCameraModes)
            {
            formatSizeInfoMap.put(new Pair<>(mode.androidFormat, mode.size), new UvcApiCameraCharacteristics.FormatSizeInfo(mode.nsFrameDuration));

            List<Size> sizes = outputSizesListMap.get(mode.androidFormat, null);
            if (sizes == null) sizes = new ArrayList<>();
            sizes.add(mode.size);
            outputSizesListMap.put(mode.androidFormat, sizes);

            if (mode.isDefaultSize)
                {
                defaultOutputSizeMap.put(mode.androidFormat, mode.size);
                }

            for (ImageFormatMapper.Format format : ImageFormatMapper.allFromAndroid(mode.androidFormat))
                {
                if (format.android != ImageFormat.UNKNOWN)
                    {
                    if (formatNameMap.indexOfKey(format.android) < 0) // use only first name we find
                        {
                        formatNameMap.put(format.android, format.name);
                        }
                    break;
                    }
                }
            }

        SparseArray<Size[]> outputSizesMap = new SparseArray<Size[]>();
        for (int iKey = 0; iKey < outputSizesListMap.size(); iKey++)
            {
            int androidFormat = outputSizesListMap.keyAt(iKey);
            List<Size> sizeList = outputSizesListMap.get(androidFormat);
            Size[] sizeArray = sizeList.toArray(new Size[sizeList.size()]);
            outputSizesMap.put(androidFormat, sizeArray);
            }

        int[] outputAndroidFormats = new int[formatNameMap.size()];
        String[] outputFormatNames = new String[formatNameMap.size()];
        for (int i = 0; i < formatNameMap.size(); i++)
            {
            outputAndroidFormats[i] = formatNameMap.keyAt(i);
            outputFormatNames[i] = formatNameMap.get(outputAndroidFormats[i]);
            }

        UvcApiCameraCharacteristics result = new UvcApiCameraCharacteristics();
        result.setOutputFormats(outputAndroidFormats, outputFormatNames);
        result.setMaps(outputSizesMap, defaultOutputSizeMap, formatSizeInfoMap);
        return result;
        }

    @Override public String toString()
        {
        synchronized (lock)
            {
            // Use internalBuild() so as to not free the streams
            return internalBuild().toString(getTag());
            }
        }

    //----------------------------------------------------------------------------------------------
    // CameraCharacteristics
    //----------------------------------------------------------------------------------------------

    protected Size[] getSizesOfAndroidFormat(int androidFormat)
        {
        Map<Size,Boolean> result = new LinkedHashMap<>();   // preserve order but remove duplicates
        for (UvcStreamingInterface uvcStreamingInterface : streamingInterfaces)
            {
            boolean found = false;
            for (UvcFormatDesc formatDesc : uvcStreamingInterface.getFormatDescriptors())
                {
                try {
                    if (!found && formatDesc.isAndroidFormat(androidFormat))
                        {
                        for (UvcFrameDesc frameDesc : formatDesc.getFrameDescriptors())
                            {
                            result.put(frameDesc.getSize(), true);
                            frameDesc.releaseRef();
                            }
                        found = true;
                        }
                    }
                finally
                    {
                    formatDesc.releaseRef();
                    }
                }
            }
        return Misc.toArray(new Size[result.keySet().size()], result.keySet());
        }

    protected Size getDefaultSizeOfAndroidFormat(int androidFormat)
        {
        Size result = null;
        for (UvcStreamingInterface uvcStreamingInterface : streamingInterfaces)
            {
            boolean found = false;
            for (UvcFormatDesc formatDesc : uvcStreamingInterface.getFormatDescriptors())
                {
                try {
                    if (!found && formatDesc.isAndroidFormat(androidFormat))
                        {
                        UvcFrameDesc frameDesc = formatDesc.getDefaultFrameDesc();
                        result = frameDesc.getSize();
                        frameDesc.releaseRef();
                        found = true;
                        }
                    }
                finally
                    {
                    formatDesc.releaseRef();
                    }
                }
            }
        return result;
        }

    protected long getNsMinFrameDurationOfAndroidFormat(int androidFormat, Size size)
        {
        long nsMinDuration = Long.MAX_VALUE;
        for (UvcStreamingInterface uvcStreamingInterface : streamingInterfaces)
            {
            boolean found = false;
            for (UvcFormatDesc formatDesc : uvcStreamingInterface.getFormatDescriptors())
                {
                try {
                    if (!found && formatDesc.isAndroidFormat(androidFormat))
                        {
                        for (UvcFrameDesc frameDesc : formatDesc.getFrameDescriptors())
                            {
                            if (frameDesc.getSize().equals(size))
                                {
                                nsMinDuration = Math.min(nsMinDuration, frameDesc.getMinFrameInterval()/*in 100ns*/ * 100);
                                }
                            frameDesc.releaseRef();
                            }
                        found = true;
                        }
                    }
                finally
                    {
                    formatDesc.releaseRef();
                    }
                }
            }
        return nsMinDuration==Long.MAX_VALUE ? 0 : nsMinDuration;
        }
    }
