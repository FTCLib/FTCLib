package org.firstinspires.ftc.teamcode;

import com.arcrobotics.ftclib.controller.PIDFController;
import com.arcrobotics.ftclib.geometry.Pose2d;
import com.arcrobotics.ftclib.kinematics.DifferentialOdometry;
import com.arcrobotics.ftclib.util.Direction;
import com.arcrobotics.ftclib.Robot;
import com.arcrobotics.ftclib.util.Safety;
import com.arcrobotics.ftclib.drivebase.DifferentialDrive;
import com.arcrobotics.ftclib.drivebase.RobotDrive;

/**
 * A robot with just a specified {@link RobotDrive} and no
 * subsystems. Built for defense or pushing objects around.
 * This is a sample classfile. Do not use this for your own code.
 */
public class HardwarePushbot extends Robot {

    private DifferentialDrive driveTrain;
    private DifferentialOdometry odometry;

    public HardwarePushbot(RobotDrive driveTrain) {
        this.driveTrain = (DifferentialDrive)driveTrain;
        odometry = new DifferentialOdometry();
    }

    public HardwarePushbot(Safety safetyMode, RobotDrive driveTrain) {
        this(driveTrain);
        setSafetyMode(safetyMode);
    }

    public void driveRobot(double forward, double turn) {
        driveTrain.arcadeDrive(forward, turn);
    }

    public void turn(Direction direction) {
        driveTrain.arcadeDrive(0,
                direction == Direction.LEFT ? -0.75 : direction == Direction.RIGHT ? 0.75 : 0);
    }

    public void turn(Safety mode, Direction direction) throws Exception {
        if (mode.value == 0) {
            driveTrain.arcadeDrive(0,
                    direction == Direction.LEFT ? -1 : direction == Direction.RIGHT ? 1 : 0);
        } else if (mode.value == 1) {
            driveTrain.arcadeDrive(0,
                    direction == Direction.LEFT ? -0.5 : direction == Direction.RIGHT ? 0.5 : 0);
        } else if (mode.value == 2) {
            turn(direction);
        } else throw new Exception("Illegal mode of safety for this action.");
    }

    public void drive(Direction direction) {
        driveTrain.arcadeDrive(direction == Direction.FORWARD ? 0.75 :
                direction == Direction.BACKWARDS ? -0.75 : 0, 0);
    }

    public void drive(Safety mode, Direction direction) throws Exception {
        if (mode.value == 0) {
            driveTrain.arcadeDrive(direction == Direction.FORWARD ? 1 :
                    direction == Direction.BACKWARDS ? -1 : 0, 0);
        } else if (mode.value == 1) {
            driveTrain.arcadeDrive(direction == Direction.FORWARD ? 0.5 :
                    direction == Direction.BACKWARDS ? -0.5 : 0, 0);
        } else if (mode.value == 2) {
            drive(direction);
        } else throw new Exception("Illegal mode of safety for this action.");
    }

    /**
     * The difference between the modes is nill. How do we make it significant?
     * You need to make your own custom way to control the speeds of the motors
     * for the drivetrain.
     */
    public void stop(Safety mode) throws Exception {
        if (mode.value == 1 || mode.value == 2 || mode.value == 3) {
            driveTrain.stopMotor();
        } else throw new Exception("Illegal mode of safety for this action.");
    }

    public Pose2d getRobotPosition() {
        return odometry.robotPose;
    }

    public void updateRobotPosition(double heading, double leftInches, double rightInches) {
        odometry.update(heading, leftInches, rightInches);
    }

}
