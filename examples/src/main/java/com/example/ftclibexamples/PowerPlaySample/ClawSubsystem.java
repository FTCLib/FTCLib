package com.example.ftclibexamples.PowerPlaySample;

import com.arcrobotics.ftclib.command.Command;
import com.arcrobotics.ftclib.command.InstantCommand;
import com.arcrobotics.ftclib.command.SubsystemBase;
import com.arcrobotics.ftclib.hardware.ServoEx;

public class ClawSubsystem extends SubsystemBase {
    private final ServoEx claw;

    public static double grabPosition = 0.5;
    public static double releasePosition = 0.7;

    public ClawSubsystem(ServoEx claw) {
        this.claw = claw;
    }

    public Command runGrabCommand() {
        return new InstantCommand(() -> claw.setPosition(grabPosition), this);
    }

    public Command runReleaseCommand() {
        return new InstantCommand(() -> claw.setPosition(releasePosition), this);
    }
}
