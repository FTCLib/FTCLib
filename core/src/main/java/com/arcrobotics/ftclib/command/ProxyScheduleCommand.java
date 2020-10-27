/*----------------------------------------------------------------------------*/
/* Copyright (c) 2018-2019 FIRST. All Rights Reserved.                        */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package com.arcrobotics.ftclib.command;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Schedules the given commands when this command is initialized, and ends when all the commands are
 * no longer scheduled.  Useful for forking off from CommandGroups.  If this command is interrupted,
 * it will cancel all of the commands.
 *
 * @author Jackson
 */
public class ProxyScheduleCommand extends CommandBase {
    private final Set<Command> m_toSchedule;
    private boolean m_finished;

    /**
     * Creates a new ProxyScheduleCommand that schedules the given commands when initialized,
     * and ends when they are all no longer scheduled.
     *
     * @param toSchedule the commands to schedule
     */
    public ProxyScheduleCommand(Command... toSchedule) {
        m_toSchedule = new HashSet<Command>(Arrays.asList(toSchedule));
    }

    @Override
    public void initialize() {
        for (Command command : m_toSchedule) {
            command.schedule();
        }
    }

    @Override
    public void end(boolean interrupted) {
        if (interrupted) {
            for (Command command : m_toSchedule) {
                command.cancel();
            }
        }
    }

    @Override
    public void execute() {
        m_finished = true;
        for (Command command : m_toSchedule) {
            m_finished &= !command.isScheduled();
        }
    }

    @Override
    public boolean isFinished() {
        return m_finished;
    }
}