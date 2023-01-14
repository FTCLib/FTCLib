package com.arcrobotics.ftclib.command;

public class DelayedCommand extends SequentialCommandGroup {

    /** A command which executes after a specified period. Useful for command groups.
     *
     * @param command the command to execute
     * @param delay the time until execution of the command occurs, in milliseconds
     */
    public DelayedCommand(Command command, long delay){
        addCommands(
                new WaitCommand(delay),
                command
        );

        addRequirements(command.getRequirements().toArray(new Subsystem[0]));
    }
}
