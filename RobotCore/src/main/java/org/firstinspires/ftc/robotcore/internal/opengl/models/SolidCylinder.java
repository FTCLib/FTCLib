/***
 * Excerpted from "OpenGL ES for Android",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material,
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose.
 * Visit http://www.pragmaticprogrammer.com/titles/kbogla for more book information.
 ***/
package org.firstinspires.ftc.robotcore.internal.opengl.models;

import java.util.List;

import org.firstinspires.ftc.robotcore.internal.opengl.models.VertexBuilder.DrawCommand;
import org.firstinspires.ftc.robotcore.internal.opengl.models.VertexBuilder.GeneratedData;
import org.firstinspires.ftc.robotcore.internal.opengl.models.Geometry.Cylinder;
import org.firstinspires.ftc.robotcore.internal.opengl.models.Geometry.Point3;

import org.firstinspires.ftc.robotcore.internal.opengl.shaders.PositionAttributeShader;

public class SolidCylinder
    {
    private static final int coordinatesPerVertex = 3;

    public final float radius, height;

    private final VertexArray vertexArray;
    private final List<DrawCommand> drawList;

    public SolidCylinder(float radius, float height, int numPointsAroundPuck)
        {
        GeneratedData generatedData = VertexBuilder.createSolidCylinder(new Cylinder(new Point3(0f, 0f, 0f), radius, height), numPointsAroundPuck);
        this.radius = radius;
        this.height = height;

        vertexArray = new VertexArray(generatedData.vertexData);
        drawList = generatedData.drawList;
        }

    public void bindData(PositionAttributeShader shader)
        {
        vertexArray.setVertexAttribPointer(0, shader.getPositionAttributeLocation(), coordinatesPerVertex, 0);
        }

    public void draw()
        {
        for (DrawCommand drawCommand : drawList)
            {
            drawCommand.draw();
            }
        }
    }