package com.arcrobotics.ftclib.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static com.arcrobotics.ftclib.util.Timing.Timer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TimingTest {
    private Timer timer;
    private final int seconds = 3;

    /**
     * Assert that the supplied runnable runs for between minMillis and minMillis+margin milliseconds.
     *
     * @param minMillis The minimum amount of time in which the runnable should run, in ms.
     * @param margin    The difference between the minimum and maximum time in which the runnable should run.
     *                  This is to account for inaccuracies in cpu time slices.
     * @param runnable  The runnable to run. This should include the timing and waiting code.
     */
    private void assertElapsedWithin(long minMillis, long margin, Runnable runnable) {
        Thread curr = Thread.currentThread();
        Thread interrupt = new Thread(() -> {
            try {
                Thread.sleep(minMillis + margin);
                curr.interrupt();
            } catch (InterruptedException e) {
                // ignored
            }
        });
        interrupt.start();
        long start = System.currentTimeMillis();
        runnable.run();
        interrupt.interrupt();
        long end = System.currentTimeMillis();
        assertTrue(end - start >= minMillis);
    }

    @BeforeEach
    public void reset() {
        timer = new Timer(seconds, TimeUnit.SECONDS);
    }

    @Test
    public void startAtZeroTest() {
        assertEquals(0, timer.elapsedTime());
    }

    @Test
    public void pauseTest() throws InterruptedException {
        timer.start();
        Thread.sleep(1005);
        timer.pause();
        long currTime = timer.elapsedTime();
        assertTrue(currTime >= 1);
        Thread.sleep(500);
        assertEquals(currTime, timer.elapsedTime());
    }

    @Test
    public void pauseResumeTest() throws InterruptedException {
        int sleepTime = 1000;
        int margin = 150;
        timer.start();
        Thread.sleep(sleepTime);
        timer.pause();
        Thread.sleep(1000);
        // assert that the remaining timer action happens in between 1850-2150ms
        // (this accounts for imperfect time slices)
        assertElapsedWithin(seconds * 1000 - sleepTime - margin, 2 * margin, () -> {
            timer.resume();
            while (!timer.done()) {
                Thread.yield();
            }
        });
    }

    @Test
    public void elapsedTimeTest() {
        boolean[] success = new boolean[1];
        assertElapsedWithin(seconds * 1000, 1500, () -> {
            timer.start();
            while (!Thread.interrupted()) {
                if (timer.done()) {
                    success[0] = true;
                    break;
                }
                Thread.yield();
            }
        });
        assertTrue(success[0]);
    }
}
