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
package org.firstinspires.ftc.robotcore.internal.opengl.models;

import android.opengl.GLES20;

import org.firstinspires.ftc.robotcore.internal.opengl.shaders.SimpleColorProgram;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static android.opengl.GLES20.GL_FLOAT;

/**
 * A simple utility that draws a line in OpenGl
 * @see <a href="http://stackoverflow.com/questions/16027455/what-is-the-easiest-way-to-draw-line-using-opengl-es-android">Drawing a line in OpenGL-ES</a>
 */
public class SimpleOpenGLLine
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected FloatBuffer vertexBuffer;

    protected static final int coordinatesPerVertex = 3;
    protected static final float defaultCoordinates[] =
            {
            0.0f, 0.0f, 0.0f,
            1.0f, 0.0f, 0.0f
            };

    protected final int vertexCount  = defaultCoordinates.length / coordinatesPerVertex;
    protected final int vertexStride = coordinatesPerVertex * 4; // 4 bytes per float

    // Set color with red, green, blue and alpha (opacity) values
    protected float color[] = {0.0f, 0.0f, 0.0f, 1.0f};

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public SimpleOpenGLLine()
        {
        // initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(vertexCount * vertexStride);

        // use the device hardware's native byte order
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();

        vertexBuffer.put(defaultCoordinates);
        vertexBuffer.position(0);
        }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    public void setVerts(float v0, float v1, float v2, float v3, float v4, float v5)
        {
        float coords[] = new float[vertexCount * coordinatesPerVertex];

        coords[0] = v0;
        coords[1] = v1;
        coords[2] = v2;
        coords[3] = v3;
        coords[4] = v4;
        coords[5] = v5;

        vertexBuffer.put(coords);
        vertexBuffer.position(0);
        }

    public void setColor(float red, float green, float blue, float alpha)
        {
        color[0] = red;
        color[1] = green;
        color[2] = blue;
        color[3] = alpha;
        }

    public void draw(float[] modelViewProjectionMatrix, SimpleColorProgram program)
        {
        program.useProgram();
        program.fragment.setColor(color[0], color[1], color[2], color[3]);

        // 'bind data'
        int attributeLocation = program.vertex.getPositionAttributeLocation();
        GLES20.glVertexAttribPointer(attributeLocation, coordinatesPerVertex, GL_FLOAT, false, vertexStride, vertexBuffer);
        GLES20.glEnableVertexAttribArray(attributeLocation);

        program.vertex.setModelViewProjectionMatrix(modelViewProjectionMatrix);
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, vertexCount);
        program.vertex.disableAttributes();
        }
    }



























