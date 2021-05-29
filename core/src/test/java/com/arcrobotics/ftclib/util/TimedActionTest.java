package com.arcrobotics.ftclib.util;

import com.qualcomm.robotcore.util.ElapsedTime;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TimedActionTest {

    public static int x = 3;
    public static TimedAction action;
    private ElapsedTime timer = new ElapsedTime();

    @BeforeAll
    public static void setup() {
        action = new TimedAction(
                () -> x = 5,
                () -> x = 3,
                100,
                true
        );
    }

    @Test
    public void testSimple() throws InterruptedException {
        assertEquals(3, x);
        action.reset();
        assertTrue(action.running());
        action.run();
        assertEquals(5, x);
        Thread.sleep(101);
        assertEquals(5, x);
        action.run();
        assertEquals(3, x);
        assertTrue(action.running());
        Thread.sleep(101);
        action.run();
        assertEquals(3, x);
        assertFalse(action.running());
    }

    @Test
    public void testLoop() {
        assertEquals(3, x);
        action.reset();
        timer.reset();
        while (timer.milliseconds() < 300) {
            action.run();
            if (timer.milliseconds() < 100) assertEquals(5, x);
            else assertEquals(3, x);
        }
        assertFalse(action.running());
        assertEquals(3, x);
    }

}
