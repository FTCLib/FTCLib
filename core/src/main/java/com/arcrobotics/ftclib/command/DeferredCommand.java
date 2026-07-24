/*----------------------------------------------------------------------------*/
/* Copyright (c) 2018-2019 FIRST. All Rights Reserved.                        */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package com.arcrobotics.ftclib.command;

import java.util.function.Supplier;

/**
 * Defers construction of the inner {@link Command} until it is initialized, at which point it is
 * supplied by the given {@link Supplier}.  This is useful for commands whose behavior depends on
 * state that is not known until the command is scheduled, such as the robot's current position.
 *
 * <p>Because the inner command does not exist until this command is initialized, its requirements
 * cannot be inferred at construction time.  The requirements must therefore be provided explicitly
 * so that the {@link CommandScheduler} can de-conflict this command with others that share a
 * subsystem.
 *
 * @author Justin
 */
public class DeferredCommand extends CommandBase {

    private final Supplier<Command> m_supplier;
    private Command m_command;

    /**
     * Creates a new DeferredCommand that will construct its inner command from the given supplier
     * when it is initialized.
     *
     * @param supplier     the supplier of the command to run
     * @param requirements the subsystems required by the supplied command
     */
    public DeferredCommand(Supplier<Command> supplier, Subsystem... requirements) {
        m_supplier = supplier;

        addRequirements(requirements);
    }

    @Override
    public void initialize() {
        m_command = m_supplier.get();
        if (m_command != null) {
            m_command.initialize();
        }
    }

    @Override
    public void execute() {
        if (m_command != null) {
            m_command.execute();
        }
    }

    @Override
    public boolean isFinished() {
        return m_command == null || m_command.isFinished();
    }

    @Override
    public void end(boolean interrupted) {
        if (m_command != null) {
            m_command.end(interrupted);
        }
        m_command = null;
    }

}
