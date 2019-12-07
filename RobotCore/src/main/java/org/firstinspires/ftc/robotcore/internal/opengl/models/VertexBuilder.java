/***
 * Excerpted from "OpenGL ES for Android",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material,
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose.
 * Visit http://www.pragmaticprogrammer.com/titles/kbogla for more book information.
 ***/
package org.firstinspires.ftc.robotcore.internal.opengl.models;

import static android.opengl.GLES20.GL_TRIANGLE_FAN;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.glDrawArrays;

import java.util.ArrayList;
import java.util.List;

import org.firstinspires.ftc.robotcore.internal.opengl.models.Geometry.Circle;
import org.firstinspires.ftc.robotcore.internal.opengl.models.Geometry.Cylinder;
import org.firstinspires.ftc.robotcore.internal.opengl.models.Geometry.Point3;

public class VertexBuilder
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    private static final int coordinatesPerVertex = 3;

    private final float[] vertexData;
    private final List<DrawCommand> drawList = new ArrayList<DrawCommand>();
    private int offset = 0;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    private VertexBuilder(int sizeInVertices)
        {
        vertexData = new float[sizeInVertices * coordinatesPerVertex];
        }

    public static GeneratedData createSolidCylinder(Cylinder cylinder, int numPoints)
        {
        int size = 2*sizeOfCircleInVertices(numPoints) + sizeOfOpenCylinderInVertices(numPoints);

        VertexBuilder builder = new VertexBuilder(size);

        Circle puckTop    = new Circle(cylinder.center.translateY(cylinder.height / 2f), cylinder.radius);
        Circle puckBottom = new Circle(cylinder.center.translateY(-cylinder.height / 2f), cylinder.radius);

        builder.appendCircle(puckTop,        numPoints, false);
        builder.appendCircle(puckBottom,     numPoints, true);
        builder.appendOpenCylinder(cylinder, numPoints);

        return builder.build();
        }

    public static GeneratedData createMallet(Point3 center, float radius, float height, int numPoints)
        {
        int size = sizeOfCircleInVertices(numPoints) * 2 + sizeOfOpenCylinderInVertices(numPoints) * 2;

        VertexBuilder builder = new VertexBuilder(size);

        // First, generate the mallet base.
        float baseHeight = height * 0.25f;

        Circle baseCircle = new Circle(center.translateY(-baseHeight), radius);
        Cylinder baseCylinder = new Cylinder(baseCircle.center.translateY(-baseHeight / 2f), radius, baseHeight);

        builder.appendCircle(baseCircle, numPoints, true);
        builder.appendOpenCylinder(baseCylinder, numPoints);

        // Now generate the mallet handle.
        float handleHeight = height * 0.75f;
        float handleRadius = radius / 3f;

        Circle handleCircle = new Circle(center.translateY(height * 0.5f), handleRadius);
        Cylinder handleCylinder = new Cylinder(handleCircle.center.translateY(-handleHeight / 2f), handleRadius, handleHeight);

        builder.appendCircle(handleCircle, numPoints, true);
        builder.appendOpenCylinder(handleCylinder, numPoints);

        return builder.build();
        }

    //----------------------------------------------------------------------------------------------
    // Types
    //----------------------------------------------------------------------------------------------

    public interface DrawCommand
        {
        void draw();
        }

    public static class GeneratedData
        {
        public final float[] vertexData;
        public final List<DrawCommand> drawList;

        GeneratedData(float[] vertexData, List<DrawCommand> drawList)
            {
            this.vertexData = vertexData;
            this.drawList = drawList;
            }
        }

    //----------------------------------------------------------------------------------------------
    // Implementation
    //----------------------------------------------------------------------------------------------

    private static int sizeOfCircleInVertices(int numPoints)
        {
        return 1 + (numPoints + 1);
        }

    private static int sizeOfOpenCylinderInVertices(int numPoints)
        {
        return (numPoints + 1) * 2;
        }

    private void appendCircle(Circle circle, int numPoints, boolean forward)
        {
        final int startVertex = offset / coordinatesPerVertex;
        final int numVertices = sizeOfCircleInVertices(numPoints);

        // Center point of fan
        vertexData[offset++] = circle.center.x;
        vertexData[offset++] = circle.center.y;
        vertexData[offset++] = circle.center.z;

        // Fan around center point. <= is used because we want to generate
        // the point at the starting angle twice to complete the fan.
        for (int j = 0; j <= numPoints; j++)
            {
            int i = forward ? j : numPoints - j;
            float angleInRadians = ((float) i / (float) numPoints) * ((float) Math.PI * 2f);
            vertexData[offset++] = circle.center.x + circle.radius * (float) Math.cos(angleInRadians);
            vertexData[offset++] = circle.center.y;
            vertexData[offset++] = circle.center.z + circle.radius * (float) Math.sin(angleInRadians);
            }
        drawList.add(new DrawCommand()
            {
            @Override
            public void draw()
                {
                glDrawArrays(GL_TRIANGLE_FAN, startVertex, numVertices);
                }
            });
        }

    private void appendOpenCylinder(Cylinder cylinder, int numPoints)
        {
        final int startVertex = offset / coordinatesPerVertex;
        final int numVertices = sizeOfOpenCylinderInVertices(numPoints);
        final float yStart = cylinder.center.y - (cylinder.height / 2f);
        final float yEnd   = cylinder.center.y + (cylinder.height / 2f);

        // Generate strip around center point. <= is used because we want to
        // generate the points at the starting angle twice, to complete the
        // strip.
        for (int i = 0; i <= numPoints; i++)
            {
            float angleInRadians = ((float) i / (float) numPoints) * ((float) Math.PI * 2f);

            float xPosition = cylinder.center.x + cylinder.radius * (float) Math.cos(angleInRadians);
            float zPosition = cylinder.center.z + cylinder.radius * (float) Math.sin(angleInRadians);

            vertexData[offset++] = xPosition;
            vertexData[offset++] = yStart;
            vertexData[offset++] = zPosition;

            vertexData[offset++] = xPosition;
            vertexData[offset++] = yEnd;
            vertexData[offset++] = zPosition;
            }
        drawList.add(new DrawCommand() { @Override
            public void draw()
                {
                glDrawArrays(GL_TRIANGLE_STRIP, startVertex, numVertices);
                }});
        }

    private GeneratedData build()
        {
        return new GeneratedData(vertexData, drawList);
        }
    }
