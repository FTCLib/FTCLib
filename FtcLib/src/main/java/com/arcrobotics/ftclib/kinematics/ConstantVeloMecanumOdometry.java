package com.arcrobotics.ftclib.kinematics;

import com.arcrobotics.ftclib.geometry.Pose2d;
import com.arcrobotics.ftclib.geometry.Rotation2d;
import com.arcrobotics.ftclib.geometry.Twist2d;

public class ConstantVeloMecanumOdometry {
    double prevLeftEncoder, prevRightEncoder, prevCenterEncoder;
    Rotation2d previousAngle, gyroOffset;
    private Pose2d pose;
    double trackwidth;

    public ConstantVeloMecanumOdometry(Rotation2d gyroAngle, Pose2d initialPose, double trackwidth) {
        pose = initialPose;
        gyroOffset = pose.getRotation().minus(gyroAngle);
        previousAngle = initialPose.getRotation();
        this.trackwidth = trackwidth;
    }

    public ConstantVeloMecanumOdometry(Rotation2d gyroAngle, double trackwidth) {
        this(gyroAngle, new Pose2d(), trackwidth);
    }

    public void resetPosition(Pose2d pose, Rotation2d gyroAngle) {
        previousAngle = pose.getRotation();
        this.pose = pose;
        gyroOffset = pose.getRotation().minus(gyroAngle);

        prevLeftEncoder = 0;
        prevRightEncoder = 0;
        prevCenterEncoder = 0;
    }

    public Pose2d getPose() {
        return pose;
    }

    public Pose2d update(Rotation2d gyroAngle, double leftEncoderPos, double rightEncoderPos, double centerEncoderPos) {
        double deltaLeftEncoder = leftEncoderPos - prevLeftEncoder;
        double deltaRightEncoder = rightEncoderPos - prevRightEncoder;
        double deltaCenterEncoder = centerEncoderPos - prevCenterEncoder;

        Rotation2d angle = gyroAngle.plus(gyroOffset);

        prevLeftEncoder = leftEncoderPos;
        prevRightEncoder = rightEncoderPos;
        prevCenterEncoder = centerEncoderPos;

        double dx = (deltaLeftEncoder + deltaRightEncoder) / 2;
        double dy = deltaCenterEncoder;
        // Averaging encoder method with gyro method
        double dw = (angle.minus(previousAngle).getRadians());
        Twist2d twist2d = new Twist2d(dx, dy, dw);

        Pose2d newPose = pose.exp(twist2d);

        previousAngle = angle;

        pose = new Pose2d(newPose.getTranslation(), angle);
        return pose;
    }

}
