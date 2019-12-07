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

import android.util.Pair;
import android.util.SparseArray;

import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.android.util.Size;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCharacteristics;
import org.firstinspires.ftc.robotcore.internal.system.Misc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link UvcApiCameraCharacteristics} is the internal implementation of {@link CameraCharacteristics}
 */
@SuppressWarnings("WeakerAccess")
public class UvcApiCameraCharacteristics implements CameraCharacteristics
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = UvcApiCameraCharacteristics.class.getSimpleName();

    protected int[]                                   outputAndroidFormats = new int[0];
    protected String[]                                outputFormatNames = new String[0];
    protected SparseArray<Size[]>                     outputSizesMap = new SparseArray<Size[]>();
    protected SparseArray<Size>                       defaultOutputSizeMap = new SparseArray<Size>();
    protected Map<Pair<Integer,Size>, FormatSizeInfo> formatSizeInfoMap = new HashMap<>();

    public static class FormatSizeInfo
        {
        public final long nsMinFrameDuration;

        public FormatSizeInfo(long nsMinFrameDuration)
            {
            this.nsMinFrameDuration = nsMinFrameDuration;
            }
        }

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public UvcApiCameraCharacteristics() // for use by UvcCameraCharacteristicsBuilder
        {
        }

    @Override public String toString()
        {
        return toString(TAG);
        }

    public String toString(String tag)
        {
        StringBuilder result = new StringBuilder();
        List<CameraMode> cameraModes = getAllCameraModes();
        result.append(Misc.formatInvariant("%s size=%d\n", tag, cameraModes.size()));
        for (CameraMode cameraMode : cameraModes)
            {
            result.append("    ");
            result.append(cameraMode.toString());
            result.append("\n");
            }
        return result.toString();
        }

    public void setOutputFormats(int[] androidFormats, String[] formatNames)
       {
       this.outputAndroidFormats = androidFormats;
       this.outputFormatNames = formatNames;
       }

    public void setMaps(SparseArray<Size[]> outputSizesMap, SparseArray<Size> defaultOutputSizeMap, Map<Pair<Integer,Size>, FormatSizeInfo> formatSizeInfoMap)
        {
        this.outputSizesMap = outputSizesMap;
        this.defaultOutputSizeMap = defaultOutputSizeMap;
        this.formatSizeInfoMap = formatSizeInfoMap;
        }

    //----------------------------------------------------------------------------------------------
    // Comparison
    //----------------------------------------------------------------------------------------------

    @Override public boolean equals(Object o)
        {
        if (o instanceof CameraCharacteristics)
            {
            CameraCharacteristics them = (CameraCharacteristics)o;
            List<CameraMode> ourModes = getAllCameraModes();
            List<CameraMode> theirModes = them.getAllCameraModes();
            if (ourModes.size() == theirModes.size())
                {
                for (CameraMode ourMode : ourModes)
                    {
                    if (!theirModes.contains(ourMode))
                        {
                        return false;
                        }
                    }
                return true;
                }
            return false;
            }
        else
            return super.equals(o);
        }

    @Override public int hashCode()
        {
        // It's necessary, but not sufficient, that two equal characteristics have the same keys
        int result = 0x897198;
        for (Pair<Integer, Size> pair : formatSizeInfoMap.keySet())
            {
            result ^= pair.hashCode();
            }
        return result;
        }

    //----------------------------------------------------------------------------------------------
    // CameraCharacteristics
    //----------------------------------------------------------------------------------------------

    @Override public int[] getAndroidFormats()
        {
        return outputAndroidFormats;
        }

    @Override public Size[] getSizes(int androidFormat)
        {
        return outputSizesMap.get(androidFormat, new Size[0]);
        }

    @Override public Size getDefaultSize(int androidFormat)
        {
        Size result = defaultOutputSizeMap.get(androidFormat, null);
        if (result == null) // paranoia
            {
            Size[] sizes = getSizes(androidFormat);
            if (sizes.length > 0)
                {
                result = sizes[0];
                }
            }
        return result;
        }

    @Override public long getMinFrameDuration(int androidFormat, Size size)
        {
        FormatSizeInfo info = formatSizeInfoMap.get(Pair.create(androidFormat, size));
        if (info != null)
            {
            return info.nsMinFrameDuration;
            }
        return 0;
        }

    @Override public int getMaxFramesPerSecond(int androidFormat, Size size)
        {
        long nsPerFrame = getMinFrameDuration(androidFormat, size);
        return nsPerFrame == 0 ? 0 : (int)Math.round((double)ElapsedTime.SECOND_IN_NANO / nsPerFrame); // billion * frames / ns
        }

    @Override public List<CameraMode> getAllCameraModes()
        {
        List<CameraMode> result = new ArrayList<>();
        for (Pair<Integer, Size> key : formatSizeInfoMap.keySet())
            {
            FormatSizeInfo info = formatSizeInfoMap.get(key);
            boolean isDefaultSize = getDefaultSize(key.first).equals(key.second);
            CameraMode mode = new CameraMode(key.first, key.second, info.nsMinFrameDuration, isDefaultSize);
            result.add(mode);
            }
        return result;
        }
    }
