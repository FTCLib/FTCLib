package com.example.ftclibexamples.CommandSample;

import com.arcrobotics.ftclib.command.SequentialCommandGroup;

/**
 * A complex auto command that drives forward, releases a stone, and then drives backward.
 */
public class ReleaseAndBack extends SequentialCommandGroup {

    private static final double INCHES = 3.0;
    private static final double SPEED = 0.5;

    /**
     * Creates a new ReleaseAndBack command group.
     *
     * @param drive The drive subsystem this command will run on
     * @param grip  The gripper subsystem this command will run on
     */
    public ReleaseAndBack(DriveSubsystem drive, GripperSubsystem grip) {
        addCommands(
                new DriveDistance(INCHES, SPEED, drive),
                new ReleaseStone(grip),
                new DriveDistance(INCHES, SPEED, drive)
        );
    }

}