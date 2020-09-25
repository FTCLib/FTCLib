package com.arcrobotics.ftclib.geometry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Pose2dTest {
    Pose2d base = new Pose2d(new Translation2d(0,0),new Rotation2d(0));

    @Test
    void plus() {
        Pose2d output = base.plus(new Transform2d(new Translation2d(1,1),new Rotation2d(90)));
        Pose2d expected = new Pose2d(new Translation2d(1,1),new Rotation2d(90));
        assertEquals(output,expected);
    }

    @Test
    void minus() {
    }

    @Test
    void getTranslation() {
    }

    @Test
    void getRotation() {
    }

    @Test
    void transformBy() {
    }

    @Test
    void relativeTo() {
    }

    @Test
    void exp() {
    }

    @Test
    void log() {
    }

    @Test
    void testToString() {
    }

    @Test
    void testEquals() {
    }

    @Test
    void rotate() {
    }

    @Test
    void getHeading() {
    }
}