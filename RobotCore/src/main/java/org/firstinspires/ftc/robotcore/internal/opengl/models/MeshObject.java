/*===============================================================================
Copyright (c) 2016 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other
countries.
===============================================================================*/

package org.firstinspires.ftc.robotcore.internal.opengl.models;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public abstract class MeshObject
    {
    public enum BUFFER_TYPE
        {
            BUFFER_TYPE_VERTEX, BUFFER_TYPE_TEXTURE_COORD, BUFFER_TYPE_NORMALS, BUFFER_TYPE_INDICES
        }

    public Buffer getVertices()
        {
        return getBuffer(BUFFER_TYPE.BUFFER_TYPE_VERTEX);
        }

    public Buffer getTexCoords()
        {
        return getBuffer(BUFFER_TYPE.BUFFER_TYPE_TEXTURE_COORD);
        }

    public Buffer getNormals()
        {
        return getBuffer(BUFFER_TYPE.BUFFER_TYPE_NORMALS);
        }

    public Buffer getIndices()
        {
        return getBuffer(BUFFER_TYPE.BUFFER_TYPE_INDICES);
        }

    protected Buffer fillBuffer(double[] array)
        {
        // Convert to floats because OpenGL doesn't work on doubles, and manually
        // casting each input value would take too much time.
        // Each float takes 4 bytes
        ByteBuffer bb = ByteBuffer.allocateDirect(4 * array.length);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (double d : array)
            bb.putFloat((float) d);
        bb.rewind();

        return bb;
        }

    protected Buffer fillBuffer(float[] array)
        {
        // Each float takes 4 bytes
        ByteBuffer bb = ByteBuffer.allocateDirect(4 * array.length);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (float d : array)
            bb.putFloat(d);
        bb.rewind();

        return bb;
        }

    protected Buffer fillBuffer(short[] array)
        {
        // Each short takes 2 bytes
        ByteBuffer bb = ByteBuffer.allocateDirect(2 * array.length);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (short s : array)
            bb.putShort(s);
        bb.rewind();

        return bb;
        }

    public abstract Buffer getBuffer(BUFFER_TYPE bufferType);

    public abstract int getNumObjectVertex();

    public abstract int getNumObjectIndex();
    }

