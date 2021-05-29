package com.arcrobotics.ftclib.util;

import com.qualcomm.robotcore.util.ElapsedTime;

/**
 * Too many people use sleep(...) where it doesn't need to be.
 * This is a simple util that runs an action until a time is reached and then performs a specified stop action.
 * This is basically the same as a chain of commands with a deadline WaitCommand but without command-based.
 *
 * @author Jackson
 */
public class TimedAction {

    private ElapsedTime timer;
    private State state = State.IDLE;

    private final Runnable onRun, onEnd;
    private final double waitTime, endTime;
    private final boolean symmetric;

    /**
     * Sets up an asymmetric timed action.
     *
     * @param onRun        the action to run during the time period
     * @param onEnd        the action to run after the time period is up
     * @param milliseconds the wait time
     */
    public TimedAction(Runnable onRun, Runnable onEnd, double milliseconds) {
        this(onRun, onEnd, milliseconds, false);
    }

    /**
     * Sets up a timed action that runs two actions depending on the time
     * and is non-blocking.
     *
     * @param onRun        the action to run during the time period
     * @param onEnd        the action to run after the time period is up
     * @param milliseconds the wait time
     * @param symmetric    true if the timed action runs symmetric
     */
    public TimedAction(Runnable onRun, Runnable onEnd, double milliseconds, boolean symmetric) {
        timer = new ElapsedTime();

        this.onRun = onRun;
        this.onEnd = onEnd;

        waitTime = milliseconds;
        endTime = milliseconds;
        this.symmetric = symmetric;
    }

    public TimedAction(Runnable onRun, Runnable onEnd, double runTime, double endTime) {
        timer = new ElapsedTime();

        this.onRun = onRun;
        this.onEnd = onEnd;

        waitTime = runTime;
        this.endTime = endTime;
        this.symmetric = true;
    }

    enum State {
        IDLE, RUNNING, SYMMETRIC
    }

    /**
     * @return true if the timed action is currently running
     */
    public boolean running() {
        return state != State.IDLE;
    }

    /**
     * Resets the timed action to the RUNNING state
     */
    public void reset() {
        timer.reset();
        state = State.RUNNING;
    }

    /**
     * Runs the timed action in a non-blocking FSM
     */
    public void run() {
        switch (state) {
            case IDLE:
                break;
            case RUNNING:
                if (timer.milliseconds() <= waitTime) {
                    onRun.run();
                } else {
                    timer.reset();
                    onEnd.run();
                    state = symmetric ? State.SYMMETRIC : State.IDLE;
                }
                break;
            case SYMMETRIC:
                if (timer.milliseconds() <= endTime) {
                    onEnd.run();
                } else {
                    timer.reset();
                    state = State.IDLE;
                }
                break;
        }
    }

}
