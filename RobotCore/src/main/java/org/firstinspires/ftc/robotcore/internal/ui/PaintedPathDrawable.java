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

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import androidx.annotation.ColorInt;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * {@link PaintedPathDrawable} is a helper class for implementing a {@link Drawable} that uses a
 * {@link Paint} to draw a {@link Path}
 */
@SuppressWarnings("WeakerAccess")
public abstract class PaintedPathDrawable extends Drawable
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected Paint paint;
    protected Path path;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    protected PaintedPathDrawable(@ColorInt int color)
        {
        this.paint = new Paint();
        this.paint.setAntiAlias(true);
        this.paint.setColor(color);
        }

    //----------------------------------------------------------------------------------------------
    // Drawing
    //----------------------------------------------------------------------------------------------

    @Override
    public void setAlpha(@IntRange(from = 0, to = 255) int alpha)
        {
        paint.setAlpha(alpha);
        }

    @Override public int getAlpha()
        {
        return paint.getAlpha();
        }

    @Override
    public void setColorFilter(ColorFilter cf)
        {
        paint.setColorFilter(cf);
        }

    @Nullable @Override public ColorFilter getColorFilter()
        {
        return paint.getColorFilter();
        }

    @Override
    public int getOpacity()
        {
        return PixelFormat.TRANSLUCENT;
        }

    @Override
    public void draw(@NonNull Canvas canvas)
        {
        canvas.drawPath(path, paint);
        }

    @Override
    protected void onBoundsChange(Rect bounds)
        {
        super.onBoundsChange(bounds);
        computePath(bounds);
        invalidateSelf();
        }

    protected abstract void computePath(Rect bounds);
    }
