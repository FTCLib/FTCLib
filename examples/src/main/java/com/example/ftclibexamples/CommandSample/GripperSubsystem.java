package com.example.ftclibexamples.CommandSample;

import com.arcrobotics.ftclib.command.SubsystemBase;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;

/**
 * A gripper mechanism that grabs a stone from the quarry.
 * Centered around the Skystone game for FTC that was done in the 2020
 * to 2021 season.
 */
public class GripperSubsystem extends SubsystemBase {

    private final Servo mechRotation;

    public GripperSubsystem(final HardwareMap hMap, final String name) {
        mechRotation = hMap.get(Servo.class, name);
    }

    /**
     * Grabs a stone.
     */
    public void grab() {
        mechRotation.setPosition(0.76);
    }

    /**
     * Releases a stone.
     */
    public void release() {
        mechRotation.setPosition(0);
    }

}
