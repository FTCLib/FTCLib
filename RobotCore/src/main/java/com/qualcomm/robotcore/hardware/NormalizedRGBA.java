/*
Copyright (c) 2016 Robert Atkinson

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
package com.qualcomm.robotcore.hardware;

import android.graphics.Color;
import androidx.annotation.ColorInt;

import com.qualcomm.robotcore.util.Range;

/**
 * {@link NormalizedRGBA} instances represent a set of normalized color values.
 * @see NormalizedColorSensor
 */
public class NormalizedRGBA
    {
    /** normalized red value, in range [0,1) */
    public float red;

    /** normalized green value, in range [0,1) */
    public float green;

    /** normalized blue value, in range [0,1) */
    public float blue;

    /** normalized alpha value, in range [0,1) */
    public float alpha;

    /** Converts the normalized colors into an Android color integer
     * @see Color */
    public @ColorInt int toColor()
        {
        float scale = 256; int min = 0, max = 255;
        return Color.argb(
                Range.clip((int)(alpha * scale), min, max),
                Range.clip((int)(red   * scale), min, max),
                Range.clip((int)(green * scale), min, max),
                Range.clip((int)(blue  * scale), min, max));
        }
    }
