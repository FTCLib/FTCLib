/***
 * Excerpted from "OpenGL ES for Android",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material,
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose.
 * Visit http://www.pragmaticprogrammer.com/titles/kbogla for more book information.
 ***/
package org.firstinspires.ftc.robotcore.internal.opengl.shaders;

import static android.opengl.GLES20.glUseProgram;

import android.content.Context;

import org.firstinspires.ftc.robotcore.internal.opengl.TextResourceReader;
import org.firstinspires.ftc.robotcore.internal.opengl.shaders.ShaderHelper;

public abstract class ShaderProgram
    {
    // Shader program
    protected final int program;

    protected ShaderProgram(Context context, int vertexShaderResourceId, int fragmentShaderResourceId)
        {
        // Compile the shaders and link the program.
        program = ShaderHelper.buildProgram(
                TextResourceReader.readTextFileFromResource(context, vertexShaderResourceId),
                TextResourceReader.readTextFileFromResource(context, fragmentShaderResourceId));
        }

    public void useProgram()
        {
        // Set the current OpenGL shader program to this program.
        glUseProgram(program);
        }
    }
