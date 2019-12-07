/*===============================================================================
Copyright (c) 2016 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other
countries.
===============================================================================*/
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.content.res.AssetManager;

import org.firstinspires.ftc.robotcore.internal.opengl.models.MeshObject;


public class SavedMeshObject extends MeshObject
    {
    private ByteBuffer verts;
    private ByteBuffer textCoords;
    private ByteBuffer norms;
    int numVerts = 0;

    public void loadModel(AssetManager assetManager, String filename) throws IOException
        {
        InputStream is = null;
        try
            {
            is = assetManager.open(filename);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            String line = reader.readLine();

            int floatsToRead = Integer.parseInt(line);
            numVerts = floatsToRead / 3;

            verts = ByteBuffer.allocateDirect(floatsToRead * 4);
            verts.order(ByteOrder.nativeOrder());
            for (int i = 0; i < floatsToRead; i++)
                {
                verts.putFloat(Float.parseFloat(reader.readLine()));
                }
            verts.rewind();

            line = reader.readLine();
            floatsToRead = Integer.parseInt(line);

            norms = ByteBuffer.allocateDirect(floatsToRead * 4);
            norms.order(ByteOrder.nativeOrder());
            for (int i = 0; i < floatsToRead; i++)
                {
                norms.putFloat(Float.parseFloat(reader.readLine()));
                }
            norms.rewind();

            line = reader.readLine();
            floatsToRead = Integer.parseInt(line);

            textCoords = ByteBuffer.allocateDirect(floatsToRead * 4);
            textCoords.order(ByteOrder.nativeOrder());
            for (int i = 0; i < floatsToRead; i++)
                {
                textCoords.putFloat(Float.parseFloat(reader.readLine()));
                }
            textCoords.rewind();

            }
        finally
            {
            if (is != null)
                is.close();
            }
        }

    @Override
    public Buffer getBuffer(BUFFER_TYPE bufferType)
        {
        Buffer result = null;
        switch (bufferType)
            {
            case BUFFER_TYPE_VERTEX:
                result = verts;
                break;
            case BUFFER_TYPE_TEXTURE_COORD:
                result = textCoords;
                break;
            case BUFFER_TYPE_NORMALS:
                result = norms;
            default:
                break;
            }
        return result;
        }

    @Override
    public int getNumObjectVertex()
        {
        return numVerts;
        }

    @Override
    public int getNumObjectIndex()
        {
        return 0;
        }
    }
