package com.arcrobotics.ftclib.drivebase;

import com.arcrobotics.ftclib.geometry.Vector2d;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class MecanumDriveTest {

    public double[] driveFieldCentric(double strafeSpeed, double forwardSpeed,
                                      double turnSpeed, double gyroAngle) {
        strafeSpeed = clipRange(strafeSpeed);
        forwardSpeed = clipRange(forwardSpeed);
        turnSpeed = clipRange(turnSpeed);

        Vector2d input = (new Vector2d(strafeSpeed, forwardSpeed)).rotateBy(-gyroAngle);

        double theta = input.angle();

        double[] wheelSpeeds = new double[4];
        wheelSpeeds[RobotDrive.MotorType.kFrontLeft.value] = Math.sin(theta + Math.PI / 4);
        wheelSpeeds[RobotDrive.MotorType.kFrontRight.value] = Math.sin(theta - Math.PI / 4);
        wheelSpeeds[RobotDrive.MotorType.kBackLeft.value] = Math.sin(theta - Math.PI / 4);
        wheelSpeeds[RobotDrive.MotorType.kBackRight.value] = Math.sin(theta + Math.PI / 4);

        normalize(wheelSpeeds, input.magnitude());

        wheelSpeeds[RobotDrive.MotorType.kFrontLeft.value] += turnSpeed;
        wheelSpeeds[RobotDrive.MotorType.kFrontRight.value] -= turnSpeed;
        wheelSpeeds[RobotDrive.MotorType.kBackLeft.value] += turnSpeed;
        wheelSpeeds[RobotDrive.MotorType.kBackRight.value] -= turnSpeed;

        normalize(wheelSpeeds);
        return wheelSpeeds;
    }

    /**
     * Returns minimum range value if the given value is less than
     * the set minimum. If the value is greater than the set maximum,
     * then the method returns the maximum value.
     *
     * @param value The value to clip.
     */
    public double clipRange(double value) {
        return value <= -1 ? -1
                : value >= 1 ? 1
                : value;
    }

    /**
     * Normalize the wheel speeds
     */
    protected void normalize(double[] wheelSpeeds, double magnitude) {
        double maxMagnitude = Math.abs(wheelSpeeds[0]);
        for (int i = 1; i < wheelSpeeds.length; i++) {
            double temp = Math.abs(wheelSpeeds[i]);
            if (maxMagnitude < temp) {
                maxMagnitude = temp;
            }
        }
        for (int i = 0; i < wheelSpeeds.length; i++) {
            wheelSpeeds[i] = (wheelSpeeds[i] / maxMagnitude) * magnitude;
        }

    }

    /**
     * Normalize the wheel speeds
     */
    protected void normalize(double[] wheelSpeeds) {
        double maxMagnitude = Math.abs(wheelSpeeds[0]);
        for (int i = 1; i < wheelSpeeds.length; i++) {
            double temp = Math.abs(wheelSpeeds[i]);
            if (maxMagnitude < temp) {
                maxMagnitude = temp;
            }
        }
        if (maxMagnitude > 1) {
            for (int i = 0; i < wheelSpeeds.length; i++) {
                wheelSpeeds[i] = (wheelSpeeds[i] / maxMagnitude);
            }
        }
    }

    @Test
    public void testFieldCentric() {
        double[] speeds = {1, 1, 1, 1};
        assertArrayEquals(speeds, driveFieldCentric(0, 1, 0, 0), 0.05);
        speeds = new double[]{-1, 1, -1, 1};
        assertArrayEquals(speeds, driveFieldCentric(0, 0, -1, 0), 0.05);
        speeds = new double[]{-1, 1, 1, -1};
        assertArrayEquals(speeds, driveFieldCentric(-1, 0, 0, 0), 0.05);
        assertArrayEquals(speeds, driveFieldCentric(0, 1, 0, -90), 0.05);
    }

}
