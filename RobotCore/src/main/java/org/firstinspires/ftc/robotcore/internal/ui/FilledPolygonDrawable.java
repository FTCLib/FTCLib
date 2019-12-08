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
package org.firstinspires.ftc.robotcore.internal.ui;

import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import androidx.annotation.ColorInt;

/**
 * {@link FilledPolygonDrawable} is a simple utility that draws a filled (regular) polygon
 * in a designated color.
 */
@SuppressWarnings("WeakerAccess")
public class FilledPolygonDrawable extends PaintedPathDrawable
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected int numSides;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public FilledPolygonDrawable(@ColorInt int color, int numSides)
        {
        super(color);
        this.paint.setStyle(Paint.Style.FILL);
        this.numSides = numSides;
        }

    //----------------------------------------------------------------------------------------------
    // Drawing
    //----------------------------------------------------------------------------------------------

    @Override
    protected void computePath(Rect bounds)
        {
        path = new Path();
        path.setFillType(Path.FillType.EVEN_ODD);
        path.addPath(computePath(Math.min(bounds.width(), bounds.height()), bounds.centerX(), bounds.centerY()));
        }

    protected Path computePath(double size, int centerX, int centerY)
        {
        final double section = (2.0 * Math.PI / numSides);
        double radius = size / 2;
        Path result = new Path();
        result.moveTo((float)(centerX + radius * Math.cos(0)), (float)(centerY + radius * Math.sin(0)));

        for (int i = 1; i < numSides; i++)
            {
            result.lineTo((float)(centerX + radius * Math.cos(section * i)), (float)(centerY + radius * Math.sin(section * i)));
            }

        result.close();
        return result;
        }
    }