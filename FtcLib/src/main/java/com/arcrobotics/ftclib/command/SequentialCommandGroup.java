package com.arcrobotics.ftclib.command;

import java.util.ArrayList;

/**
 * Allows you to combine multiple commands into one.
 */
public abstract class SequentialCommandGroup implements Command {

    private ArrayList<Command> commands;
    private int numCommandsFinished;
    private int totalNumCommands;
    private int currentCommand;
    private void addCommand(Command command) {
        commands.add(command);
    }

    @Override
    public void initialize() {
        totalNumCommands = commands.size();
        numCommandsFinished = 0;
        currentCommand = 0;
        commands.get(currentCommand).initialize();
    }

    @Override
    public void execute() {
        commands.get(currentCommand).execute();

        if(commands.get(currentCommand).isFinished()) {
            commands.get(currentCommand).end();
            currentCommand++;
            numCommandsFinished++;
            if(numCommandsFinished < totalNumCommands) {
                commands.get(currentCommand).initialize();
            }
        }

    }

    @Override
    public void end() {

    }

    @Override
    public boolean isFinished() {
        return numCommandsFinished == totalNumCommands;
    }
}
