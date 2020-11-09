package com.arcrobotics.ftclib.command;

import java.util.function.DoubleSupplier;

public class MecanumDriveCommand extends CommandBase {

    private MecanumSubsystem m_drive;
    private DoubleSupplier m_strafe, m_forward, m_turn, m_heading;

    public MecanumDriveCommand(MecanumSubsystem drivebase,
                               DoubleSupplier strafe, DoubleSupplier forward,
                               DoubleSupplier turn, DoubleSupplier heading) {
        m_drive = drivebase;
        m_strafe = strafe;
        m_forward = forward;
        m_turn = turn;
        m_heading = heading;
        addRequirements(drivebase);
    }

    public MecanumDriveCommand(MecanumSubsystem drivebase, DoubleSupplier strafe,
                               DoubleSupplier forward, DoubleSupplier turn) {
        this(drivebase, strafe, forward, turn, () -> 0);
    }

    @Override
    public void execute() {
        m_drive.drive(m_strafe.getAsDouble(), m_forward.getAsDouble(),
                m_turn.getAsDouble(), m_heading.getAsDouble());
    }
}
