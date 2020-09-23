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
 * Schedules the given commands when this command is initialized.  Useful for forking off from
 * CommandGroups.  Note that if run from a CommandGroup, the group will not know about the status
 * of the scheduled commands, and will treat this command as finishing instantly.
 *
 * @author Jackson
 */
public class ScheduleCommand extends CommandBase {

    private final Set<Command> m_toSchedule;

    /**
     * Creates a new ScheduleCommand that schedules the given commands when initialized.
     *
     * @param toSchedule the commands to schedule
     */
    public ScheduleCommand(Command... toSchedule) {
        m_toSchedule = new HashSet<Command>(Arrays.asList(toSchedule));
    }

    @Override
    public void initialize() {
        for (Command command : m_toSchedule) {
            command.schedule();
        }
    }

    @Override
    public boolean isFinished() {
        return true;
    }

    @Override
    public boolean runsWhenDisabled() {
        return true;
    }

}
