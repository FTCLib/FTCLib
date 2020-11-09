package com.arcrobotics.ftclib.command;

import com.arcrobotics.ftclib.drivebase.MecanumDrive;

public class MecanumSubsystem extends SubsystemBase {

    private MecanumDrive m_drive;

    public MecanumSubsystem(MecanumDrive drivebase) {
        m_drive = drivebase;
    }

    public void drive(double strafeSpeed, double forwardSpeed, double turnSpeed, double heading) {
        m_drive.driveFieldCentric(strafeSpeed, forwardSpeed, turnSpeed, heading);
    }

    public void drive(double strafeSpeed, double forwardSpeed, double turnSpeed) {
        this.drive(strafeSpeed, forwardSpeed, turnSpeed, 0);
    }

    public void stop() {
        m_drive.stop();
    }

}
