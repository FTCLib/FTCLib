package com.arcrobotics.ftclib.kinematics;

import com.arcrobotics.ftclib.geometry.Pose2d;
import com.arcrobotics.ftclib.geometry.Rotation2d;
import com.arcrobotics.ftclib.geometry.Twist2d;

public class ConstantVeloMecanumOdometry extends Odometry {
    double prevLeftEncoder, prevRightEncoder, prevCenterEncoder;
    Rotation2d previousAngle, gyroOffset;
    double centerWheelOffset;

    public ConstantVeloMecanumOdometry(Rotation2d gyroAngle, Pose2d initialPose, double trackwidth, double centerWheelOffset) {
        super(initialPose, trackwidth);
        gyroOffset = robotPose.getRotation().minus(gyroAngle);
        previousAngle = initialPose.getRotation();
        this.centerWheelOffset = centerWheelOffset;
    }

    public ConstantVeloMecanumOdometry(Rotation2d gyroAngle, double trackwidth, double centerWheelOffset) {
        this(gyroAngle, new Pose2d(), trackwidth, centerWheelOffset);
    }

    @Override
    public void updatePose(Pose2d pose) {
        previousAngle = pose.getRotation();
        this.robotPose = pose;

        prevLeftEncoder = 0;
        prevRightEncoder = 0;
        prevCenterEncoder = 0;
    }


    public void update(Rotation2d gyroAngle, double leftEncoderPos, double rightEncoderPos, double centerEncoderPos) {
        double deltaLeftEncoder = leftEncoderPos - prevLeftEncoder;
        double deltaRightEncoder = rightEncoderPos - prevRightEncoder;
        double deltaCenterEncoder = centerEncoderPos - prevCenterEncoder;

        Rotation2d angle = gyroAngle.plus(gyroOffset);

        prevLeftEncoder = leftEncoderPos;
        prevRightEncoder = rightEncoderPos;
        prevCenterEncoder = centerEncoderPos;

        
        // Averaging encoder method with gyro method
        double dw = (angle.minus(previousAngle).getRadians());
        
        double dx = (deltaLeftEncoder + deltaRightEncoder) / 2;
        double dy = deltaCenterEncoder - (centerWheelOffset * dw);
        
        Twist2d twist2d = new Twist2d(dx, dy, dw);

        Pose2d newPose = robotPose.exp(twist2d);

        previousAngle = angle;

        robotPose = new Pose2d(newPose.getTranslation(), angle);
    }

}
