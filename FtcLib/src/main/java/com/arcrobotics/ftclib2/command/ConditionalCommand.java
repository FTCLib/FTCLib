/*----------------------------------------------------------------------------*/
/* Copyright (c) 2018-2019 FIRST. All Rights Reserved.                        */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package com.arcrobotics.ftclib2.command;

import java.util.function.BooleanSupplier;
import static com.arcrobotics.ftclib2.command.CommandGroupBase.requireUngrouped;


/**
 * Runs one of two commands, depending on the value of the given condition when this command is
 * initialized. Does not actually schedule the selected command - rather, the command is run
 * through this command; this ensures that the command will behave as expected if used as part of a
 * CommandGroup. Requires the requirements of both commands, again to ensure proper functioning
 * when used in a CommandGroup. If this is undesired, consider using {@link ScheduleCommand}.
 *
 * <p>As this command contains multiple component commands within it, it is technically a command
 * group; the command instances that are passed to it cannot be added to any other groups, or
 * scheduled individually.
 *
 * <p>As a rule, CommandGroups require the union of the requirements of their component commands.
 *
 * @author Jackson
 */
public class ConditionalCommand extends CommandBase {

    private final Command m_onTrue;
    private final Command m_onFalse;
    private final BooleanSupplier m_condition;
    private Command m_selectedCommand;

    /**
     * Creates a new ConditionalCommand.
     *
     * @param onTrue    the command to run if the condition is true
     * @param onFalse   the command to run if the condition is false
     * @param condition the condition to determine which command to run
     */
    public ConditionalCommand(Command onTrue, Command onFalse, BooleanSupplier condition) {
        requireUngrouped(onTrue, onFalse);

        CommandGroupBase.registerGroupedCommands(onTrue, onFalse);

        m_onTrue = onTrue;
        m_onFalse = onFalse;
        m_condition = condition;
        m_requirements.addAll(m_onTrue.getRequirements());
        m_requirements.addAll(m_onFalse.getRequirements());
    }

    @Override
    public void initialize() {
        if (m_condition.getAsBoolean()) {
            m_selectedCommand = m_onTrue;
        } else {
            m_selectedCommand = m_onFalse;
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
        return m_onTrue.runsWhenDisabled() && m_onFalse.runsWhenDisabled();
    }

}
