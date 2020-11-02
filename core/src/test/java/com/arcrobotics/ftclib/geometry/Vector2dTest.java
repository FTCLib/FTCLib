package com.arcrobotics.ftclib.geometry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Vector2dTest {

    Vector2d vec = new Vector2d(1, 0);

    @Test
    void testRotateBy() {
        assertEquals(Math.PI / 2, vec.rotateBy(Math.toDegrees(Math.PI / 2)).angle());
    }

}
