package com.arcrobotics.ftclib.command;

/**
 * This is the Robot class. This will make your command-based robot code a lot smoother
 * and easier to understand.
 */
public class Robot {

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

}
