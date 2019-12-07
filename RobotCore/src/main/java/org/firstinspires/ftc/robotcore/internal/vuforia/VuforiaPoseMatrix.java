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
package org.firstinspires.ftc.robotcore.internal.vuforia;

import com.vuforia.*;

import org.firstinspires.ftc.robotcore.external.matrices.GeneralMatrixF;
import org.firstinspires.ftc.robotcore.external.matrices.MatrixF;
import org.firstinspires.ftc.robotcore.external.matrices.OpenGLMatrix;
import org.firstinspires.ftc.robotcore.external.matrices.RowMajorMatrixF;

/**
 * {@link VuforiaPoseMatrix} is a matrix that is returned from, e.g., the
 * {@link TrackableResult#getPose()} API.
 */
public class VuforiaPoseMatrix extends RowMajorMatrixF
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    float[] data;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public VuforiaPoseMatrix()
        {
        super(3,4);
        this.data = new float[3*4];
        }

    public VuforiaPoseMatrix(Matrix34F matrix)
        {
        super(3,4);
        this.data = matrix.getData();
        }

    @Override public MatrixF emptyMatrix(int numRows, int numCols)
        {
        if (numRows==3 && numCols==4)
            {
            return new VuforiaPoseMatrix();
            }
        else
            {
            return new GeneralMatrixF(numRows, numCols);
            }
        }

    //----------------------------------------------------------------------------------------------
    // Accessing
    //----------------------------------------------------------------------------------------------

    @Override public float[] getData()
        {
        return this.data;
        }

    public Matrix34F getMatrix34F()
        {
        Matrix34F result = new Matrix34F();
        result.setData(this.data);
        return result;
        }

    public OpenGLMatrix toOpenGL()
        {
        Matrix34F matrix34F = getMatrix34F();
        Matrix44F matrix44F = com.vuforia.Tool.convertPose2GLMatrix(matrix34F);
        return new OpenGLMatrix(matrix44F);
        }
    }
