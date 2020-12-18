/*----------------------------------------------------------------------------*/
/* Copyright (c) 2018-2019 FIRST. All Rights Reserved.                        */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/
package com.arcrobotics.ftclib.command;

import androidx.annotation.NonNull;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Runs one of a selection of commands, either using a selector and a key to command mapping, or a
 * supplier that returns the command directly at runtime.  Does not actually schedule the selected
 * command - rather, the command is run through this command; this ensures that the command will
 * behave as expected if used as part of a CommandGroup.  Requires the requirements of all included
 * commands, again to ensure proper functioning when used in a CommandGroup.  If this is undesired,
 * consider using {@link ScheduleCommand}.
 *
 * <p>As this command contains multiple component commands within it, it is technically a command
 * group; the command instances that are passed to it cannot be added to any other groups, or
 * scheduled individually.
 *
 * <p>As a rule, CommandGroups require the union of the requirements of their component commands.
 */
public class SelectCommand extends CommandBase {
    private final Map<Object, Command> m_commands;
    private final Supplier<Object> m_selector;
    private final Supplier<Command> m_toRun;
    private Command m_selectedCommand;

    /**
     * Creates a new selectcommand.
     *
     * @param commands the map of commands to choose from
     * @param selector the selector to determine which command to run
     */
    public SelectCommand(@NonNull Map<Object, Command> commands, @NonNull Supplier<Object> selector) {
        CommandGroupBase.registerGroupedCommands(commands.values().toArray(new Command[]{}));

        m_commands = commands;
        m_selector = selector;

        m_toRun = null;

        for (Command command : m_commands.values()) {
            m_requirements.addAll(command.getRequirements());
        }
    }

    /**
     * Creates a new selectcommand.
     *
     * @param toRun a supplier providing the command to run
     */
    public SelectCommand(@NonNull Supplier<Command> toRun) {
        m_commands = null;
        m_selector = null;

        m_toRun = toRun;
    }

    @Override
    public void initialize() {
        if (m_selector != null) {
            if (!m_commands.keySet().contains(m_selector.get())) {
                m_selectedCommand = new LogCatCommand(
                        "SelectCommand failure",
                        "SelectCommand selector value does not correspond to" + " any command!");
                return;
            }
            m_selectedCommand = m_commands.get(m_selector.get());
        } else {
            m_selectedCommand = m_toRun.get();
        }
        m_selectedCommand.initialize();
    }

    @Override
    public void execute() {
        m_selectedCommand.execute();
    }

    @Override
    public void end(boolean interrupted) {
        m_selectedCommand.end(interrupted);
    }

    @Override
    public boolean isFinished() {
        return m_selectedCommand.isFinished();
    }

    @Override
    public boolean runsWhenDisabled() {
        if (m_commands != null) {
            boolean runsWhenDisabled = true;
            for (Command command : m_commands.values()) {
                runsWhenDisabled &= command.runsWhenDisabled();
            }
            return runsWhenDisabled;
        } else {
            return m_toRun.get().runsWhenDisabled();
        }
    }
}