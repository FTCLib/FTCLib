/***
 * Excerpted from "OpenGL ES for Android",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material,
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose.
 * Visit http://www.pragmaticprogrammer.com/titles/kbogla for more book information.
 ***/
package org.firstinspires.ftc.robotcore.internal.opengl.models;

import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glVertexAttribPointer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class VertexArray
    {
    public static final int bytesPerFloat = 4;

    private final FloatBuffer vertexBuffer;

    public VertexArray(float[] vertexData)
        {
        vertexBuffer = ByteBuffer
                .allocateDirect(vertexData.length * bytesPerFloat)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);
        }

    public void setVertexAttribPointer(int dataOffset, int attributeLocation, int coordinatesPerVertex, int vertexStride)
        {
        vertexBuffer.position(dataOffset);
        glVertexAttribPointer(attributeLocation, coordinatesPerVertex, GL_FLOAT, false, vertexStride, vertexBuffer);
        glEnableVertexAttribArray(attributeLocation);
        vertexBuffer.position(0);
        }
    }
