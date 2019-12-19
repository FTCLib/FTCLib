package com.arcrobotics.ftclib.command;

public interface Command {

    /**
     * The initial subroutine of a command. Called once when the command is initially scheduled.
     */
    void initialize();

    /**
     * The main body of a command. Called repeatedly while the command is scheduled.
     */
    void execute();

    /**
     * The action to take when the command ends.
     * */
    void end();

    /**
     * Whether the command has finished. Once a command finishes, the scheduler will call its
     * end() method.
     *
     * @return whether the command has finished.
     */
    boolean isFinished();

}
