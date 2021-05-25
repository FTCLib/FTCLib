package com.arcrobotics.ftclib.geometry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class Translation2dTest {
    private static final double kEpsilon = 1E-9;

    @Test
    void testSum() {
        Translation2d one = new Translation2d(1.0, 3.0);
        Translation2d two = new Translation2d(2.0, 5.0);

        Translation2d sum = one.plus(two);

        assertAll(
                () -> assertEquals(sum.getX(), 3.0, kEpsilon),
                () -> assertEquals(sum.getY(), 8.0, kEpsilon)
        );
    }

    @Test
    void testDifference() {
        Translation2d one = new Translation2d(1.0, 3.0);
        Translation2d two = new Translation2d(2.0, 5.0);

        Translation2d difference = one.minus(two);

        assertAll(
                () -> assertEquals(difference.getX(), -1.0, kEpsilon),
                () -> assertEquals(difference.getY(), -2.0, kEpsilon)
        );
    }

    @Test
    void testRotateBy() {
        Translation2d another = new Translation2d(3.0, 0.0);
        Translation2d rotated = another.rotateBy(Rotation2d.fromDegrees(90.0));

        assertAll(
                () -> assertEquals(rotated.getX(), 0.0, kEpsilon),
                () -> assertEquals(rotated.getY(), 3.0, kEpsilon)
        );
    }

    @Test
    void testMultiplication() {
        Translation2d original = new Translation2d(3.0, 5.0);
        Translation2d mult = original.times(3);

        assertAll(
                () -> assertEquals(mult.getX(), 9.0, kEpsilon),
                () -> assertEquals(mult.getY(), 15.0, kEpsilon)
        );
    }

    @Test
    void testDivision() {
        Translation2d original = new Translation2d(3.0, 5.0);
        Translation2d div = original.div(2);

        assertAll(
                () -> assertEquals(div.getX(), 1.5, kEpsilon),
                () -> assertEquals(div.getY(), 2.5, kEpsilon)
        );
    }

    @Test
    void testNorm() {
        Translation2d one = new Translation2d(3.0, 5.0);
        assertEquals(one.getNorm(), Math.hypot(3.0, 5.0), kEpsilon);
    }

    @Test
    void testDistance() {
        Translation2d one = new Translation2d(1, 1);
        Translation2d two = new Translation2d(6, 6);
        assertEquals(one.getDistance(two), 5 * Math.sqrt(2), kEpsilon);
    }

    @Test
    void testUnaryMinus() {
        Translation2d original = new Translation2d(-4.5, 7);
        Translation2d inverted = original.unaryMinus();

        assertAll(
                () -> assertEquals(inverted.getX(), 4.5, kEpsilon),
                () -> assertEquals(inverted.getY(), -7, kEpsilon)
        );
    }

    @Test
    void testEquality() {
        Translation2d one = new Translation2d(9, 5.5);
        Translation2d two = new Translation2d(9, 5.5);
        assertEquals(one, two);
    }

    @Test
    void testInequality() {
        Translation2d one = new Translation2d(9, 5.5);
        Translation2d two = new Translation2d(9, 5.7);
        assertNotEquals(one, two);
    }
}