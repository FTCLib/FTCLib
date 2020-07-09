/*----------------------------------------------------------------------------*/
/* Copyright (c) 2018-2019 FIRST. All Rights Reserved.                        */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package com.arcrobotics.ftclib2.command;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A CommandGroup that runs a set of commands in parallel, ending only when a specific command
 * (the "deadline") ends, interrupting all other commands that are still running at that point.
 *
 * <p>As a rule, CommandGroups require the union of the requirements of their component commands.
 *
 * @author Jackson
 */
public class ParallelDeadlineGroup extends CommandGroupBase {

    // maps commands in this group to whether they are still running
    private final Map<Command, Boolean> m_commands = new HashMap<>();
    private boolean m_runWhenDisabled = true;
    private Command m_deadline;

    /**
     * Creates a new ParallelDeadlineGroup.  The given commands (including the deadline) will be
     * executed simultaneously.  The CommandGroup will finish when the deadline finishes,
     * interrupting all other still-running commands.  If the CommandGroup is interrupted, only
     * the commands still running will be interrupted.
     *
     * @param deadline the command that determines when the group ends
     * @param commands the commands to be executed
     */
    public ParallelDeadlineGroup(Command deadline, Command... commands) {
        m_deadline = deadline;
        addCommands(commands);
        if (!m_commands.containsKey(deadline)) {
            addCommands(deadline);
        }
    }

    /**
     * Sets the deadline to the given command.  The deadline is added to the group if it is not
     * already contained.
     *
     * @param deadline the command that determines when the group ends
     */
    public void setDeadline(Command deadline) {
        if (!m_commands.containsKey(deadline)) {
            addCommands(deadline);
        }
        m_deadline = deadline;
    }

    @Override
    public void addCommands(Command... commands) {
        requireUngrouped(commands);

        if (m_commands.containsValue(true)) {
            throw new IllegalStateException(
                    "Commands cannot be added to a CommandGroup while the group is running");
        }

        registerGroupedCommands(commands);

        for (Command command : commands) {
            if (!Collections.disjoint(command.getRequirements(), m_requirements)) {
                throw new IllegalArgumentException("Multiple commands in a parallel group cannot"
                        + "require the same subsystems");
            }
            m_commands.put(command, false);
            m_requirements.addAll(command.getRequirements());
            m_runWhenDisabled &= command.runsWhenDisabled();
        }
    }

    @Override
    public void initialize() {
        for (Map.Entry<Command, Boolean> commandRunning : m_commands.entrySet()) {
            commandRunning.getKey().initialize();
            commandRunning.setValue(true);
        }
    }

    @Override
    public void execute() {
        for (Map.Entry<Command, Boolean> commandRunning : m_commands.entrySet()) {
            if (!commandRunning.getValue()) {
                continue;
            }
            commandRunning.getKey().execute();
            if (commandRunning.getKey().isFinished()) {
                commandRunning.getKey().end(false);
                commandRunning.setValue(false);
            }
        }
    }

    @Override
    public void end(boolean interrupted) {
        for (Map.Entry<Command, Boolean> commandRunning : m_commands.entrySet()) {
            if (commandRunning.getValue()) {
                commandRunning.getKey().end(true);
            }
        }
    }

    @Override
    public boolean isFinished() {
        return m_deadline.isFinished();
    }

    @Override
    public boolean runsWhenDisabled() {
        return m_runWhenDisabled;
    }

}
