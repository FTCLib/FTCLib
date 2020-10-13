package com.example.ftclibexamples.CommandSample;

import com.arcrobotics.ftclib.command.CommandBase;

/**
 * A simple command that releases a stone with the {@link GripperSubsystem}.  Written explicitly for
 * pedagogical purposes. Actual code should inline a command this simple with {@link
 * com.arcrobotics.ftclib.command.InstantCommand}.
 */
public class ReleaseStone extends CommandBase {

    // The subsystem the command runs on
    private final GripperSubsystem m_gripperSubsystem;

    public ReleaseStone(GripperSubsystem subsystem) {
        m_gripperSubsystem = subsystem;
        addRequirements(m_gripperSubsystem);
    }

    @Override
    public void initialize() {
        m_gripperSubsystem.release();
    }

    @Override
    public boolean isFinished() {
        return true;
    }

}
