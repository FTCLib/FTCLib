package com.arcrobotics.ftclib.geometry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class Twist2dTest {
    private static final double kEpsilon = 1E-9;

    @Test
    void testStraightLineTwist() {
        Twist2d straight = new Twist2d(5.0, 0.0, 0.0);
        Pose2d straightPose = new Pose2d().exp(straight);

        assertAll(
                () -> assertEquals(straightPose.getX(), 5.0, kEpsilon),
                () -> assertEquals(straightPose.getY(), 0.0, kEpsilon),
                () -> assertEquals(straightPose.getRotation().getRadians(), 0.0, kEpsilon)
        );
    }

    @Test
    void testQuarterCirleTwist() {
        Twist2d quarterCircle = new Twist2d(5.0 / 2.0 * Math.PI, 0, Math.PI / 2.0);
        Pose2d quarterCirclePose = new Pose2d().exp(quarterCircle);

        assertAll(
                () -> assertEquals(quarterCirclePose.getX(), 5.0, kEpsilon),
                () -> assertEquals(quarterCirclePose.getY(), 5.0, kEpsilon),
                () -> assertEquals(quarterCirclePose.getRotation().getDegrees(), 90.0, kEpsilon)
        );
    }

    @Test
    void testDiagonalNoDtheta() {
        Twist2d diagonal = new Twist2d(2.0, 2.0, 0.0);
        Pose2d diagonalPose = new Pose2d().exp(diagonal);

        assertAll(
                () -> assertEquals(diagonalPose.getX(), 2.0, kEpsilon),
                () -> assertEquals(diagonalPose.getY(), 2.0, kEpsilon),
                () -> assertEquals(diagonalPose.getRotation().getDegrees(), 0.0, kEpsilon)
        );
    }

    @Test
    void testEquality() {
        Twist2d one = new Twist2d(5, 1, 3);
        Twist2d two = new Twist2d(5, 1, 3);
        assertEquals(one, two);
    }

    @Test
    void testInequality() {
        Twist2d one = new Twist2d(5, 1, 3);
        Twist2d two = new Twist2d(5, 1.2, 3);
        assertNotEquals(one, two);
    }

    @Test
    void testPose2dLog() {
        final Pose2d start = new Pose2d();
        final Pose2d end = new Pose2d(5.0, 5.0, Rotation2d.fromDegrees(90.0));

        final Twist2d twist = start.log(end);

        assertAll(
                () -> assertEquals(twist.dx, 5.0 / 2.0 * Math.PI, kEpsilon),
                () -> assertEquals(twist.dy, 0.0, kEpsilon),
                () -> assertEquals(twist.dtheta, Math.PI / 2.0, kEpsilon)
        );
    }
}