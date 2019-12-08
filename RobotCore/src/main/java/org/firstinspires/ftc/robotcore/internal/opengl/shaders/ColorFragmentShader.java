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
package org.firstinspires.ftc.robotcore.internal.opengl.shaders;

import android.graphics.Color;
import android.opengl.GLES20;
import androidx.annotation.ColorInt;
import androidx.annotation.IdRes;
import androidx.annotation.RawRes;

import com.qualcomm.robotcore.R;

import static android.opengl.GLES20.glUniform4f;

public class ColorFragmentShader
    {
    protected final int u_Color;

    @RawRes public static final int resourceId = R.raw.color_fragment_shader;

    public ColorFragmentShader(int programId)
        {
        u_Color = GLES20.glGetUniformLocation(programId, "u_Color");
        }

    public void setColor(float r, float g, float b, float a)
        {
        glUniform4f(u_Color, r, g, b, a);
        }
    public void setColor(float r, float g, float b)
        {
        setColor(r, g, b, 1.0f);
        }
    public void setColor(@ColorInt int color)
        {
        setColor(rescale(Color.red(color)),
                rescale(Color.green(color)),
                rescale(Color.blue(color)),
                rescale(Color.alpha(color)));
        }

    private static float rescale(int colorComponent)
        {
        return (float)colorComponent / 255.0f;
        }
    }
