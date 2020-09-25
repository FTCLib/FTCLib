package com.arcrobotics.ftclib.geometry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Pose2dTest {
    Pose2d base = new Pose2d(new Translation2d(0,0),new Rotation2d(0));

    @Test
    void testPlus() {
        Pose2d output = base.plus(new Transform2d(new Translation2d(1,1),new Rotation2d(90)));
        Pose2d expected = new Pose2d(new Translation2d(1,1),new Rotation2d(90));
        assertEquals(output,expected);
    }

    @Test
    void testMinus() {
    }

    @Test
    void testGetTranslation() {
    }

    @Test
    void testGetRotation() {
    }

    @Test
    void testTransformBy() {
    }

    @Test
    void testRelativeTo() {
    }

    @Test
    void testExp() {
    }

    @Test
    void testLog() {
    }

    @Test
    void testToString() {
    }

    @Test
    void testEquals() {
    }

    @Test
    void testRotate() {
    }

    @Test
    void testGetHeading() {
    }
}