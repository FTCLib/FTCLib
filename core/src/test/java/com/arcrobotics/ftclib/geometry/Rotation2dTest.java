package com.arcrobotics.ftclib.geometry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class Rotation2dTest {
    private static final double kEpsilon = 1E-9;

    @Test
    void testRadiansToDegrees() {
        Rotation2d one = new Rotation2d(Math.PI / 3);
        Rotation2d two = new Rotation2d(Math.PI / 4);

        assertAll(
                () -> assertEquals(one.getDegrees(), 60.0, kEpsilon),
                () -> assertEquals(two.getDegrees(), 45.0, kEpsilon)
        );
    }

    @Test
    void testRadiansAndDegrees() {
        Rotation2d one = Rotation2d.fromDegrees(45.0);
        Rotation2d two = Rotation2d.fromDegrees(30.0);

        assertAll(
                () -> assertEquals(one.getRadians(), Math.PI / 4, kEpsilon),
                () -> assertEquals(two.getRadians(), Math.PI / 6, kEpsilon)
        );
    }

    @Test
    void testRotateByFromZero() {
        Rotation2d zero = new Rotation2d();
        Rotation2d rotated = zero.rotateBy(Rotation2d.fromDegrees(90.0));

        assertAll(
                () -> assertEquals(rotated.getRadians(), Math.PI / 2.0, kEpsilon),
                () -> assertEquals(rotated.getDegrees(), 90.0, kEpsilon)
        );
    }

    @Test
    void testRotateByNonZero() {
        Rotation2d rot = Rotation2d.fromDegrees(90.0);
        rot = rot.plus(Rotation2d.fromDegrees(30.0));

        assertEquals(rot.getDegrees(), 120.0, kEpsilon);
    }

    @Test
    void testMinus() {
        Rotation2d one = Rotation2d.fromDegrees(70.0);
        Rotation2d two = Rotation2d.fromDegrees(30.0);

        assertEquals(one.minus(two).getDegrees(), 40.0, kEpsilon);
    }

    @Test
    void testEquality() {
        Rotation2d one = Rotation2d.fromDegrees(43.0);
        Rotation2d two = Rotation2d.fromDegrees(43.0);
        assertEquals(one, two);
    }

    @Test
    void testInequality() {
        Rotation2d one = Rotation2d.fromDegrees(43.0);
        Rotation2d two = Rotation2d.fromDegrees(43.5);
        assertNotEquals(one, two);
    }
}
