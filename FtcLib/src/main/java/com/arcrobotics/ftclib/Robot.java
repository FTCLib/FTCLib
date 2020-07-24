package com.arcrobotics.ftclib;

import com.arcrobotics.ftclib.command.Command;
import com.arcrobotics.ftclib.command.CommandScheduler;
import com.arcrobotics.ftclib.command.Subsystem;
import com.arcrobotics.ftclib.util.Safety;

/**
 * This is the Robot class. This will make your command-based robot code a lot smoother
 * and easier to understand.
 */
public class Robot {

    /**
     * The enabled safety of the robot. The default safety
     * mode is DEFAULT.
     */
    protected Safety m_safety = Safety.DEFAULT;

    /**
     * Sets the safety mode for the robot.
     *
     * @param safety The requested mode of safety for the robot.
     */
    public void setSafetyMode(Safety safety) {
        m_safety = safety;
    }

    /**
     * @return the safety mode for the robot
     */
    public Safety getSafetyMode() {
        return m_safety;
    }

    /**
     * Cancels all previous commands
     */
    public void reset() {
        CommandScheduler.getInstance().cancelAll();
    }

    /**
     * Runs the {@link CommandScheduler} instance
     */
    public void run() {
        CommandScheduler.getInstance().run();
    }

    /**
     * Schedules {@link com.arcrobotics.ftclib.command.Command} objects to the scheduler
     */
    public void schedule(Command... commands) {
        CommandScheduler.getInstance().schedule(commands);
    }

    /**
     * Registers {@link com.arcrobotics.ftclib.command.Subsystem} objects to the scheduler
     */
    public void register(Subsystem... subsystems) {
        CommandScheduler.getInstance().registerSubsystem(subsystems);
    }

    /**
     * Adds a button binding to the scheduler
     */
    public void addButton(Runnable button) {
        CommandScheduler.getInstance().addButton(button);
    }

}
