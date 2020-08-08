package com.arcrobotics.ftclib.kinematics;

import com.arcrobotics.ftclib.geometry.Pose2d;
import com.arcrobotics.ftclib.geometry.Rotation2d;
import com.arcrobotics.ftclib.geometry.Twist2d;

import java.util.function.DoubleSupplier;

public class ConstantVeloMecanumOdometry extends Odometry {

    private double prevLeftEncoder, prevRightEncoder, prevHorizontalEncoder;
    private Rotation2d previousAngle, gyroOffset;
    private double centerWheelOffset;

    // the suppliers
    DoubleSupplier m_heading, m_left, m_right, m_horizontal;

    public ConstantVeloMecanumOdometry(DoubleSupplier headingSupplier,
                                       DoubleSupplier leftEncoder, DoubleSupplier rightEncoder,
                                       DoubleSupplier horizontalEncoder, double trackWidth, double centerWheelOffset) {
        this(new Rotation2d(headingSupplier.getAsDouble()), trackWidth, centerWheelOffset);
        m_heading = headingSupplier;
        m_left = leftEncoder;
        m_right = rightEncoder;
        m_horizontal = horizontalEncoder;
    }

    public ConstantVeloMecanumOdometry(Rotation2d gyroAngle, Pose2d initialPose, double trackwidth, double centerWheelOffset) {
        super(initialPose, trackwidth);
        gyroOffset = robotPose.getRotation().minus(gyroAngle);
        previousAngle = initialPose.getRotation();
        this.centerWheelOffset = centerWheelOffset;
    }

    public ConstantVeloMecanumOdometry(Rotation2d gyroAngle, double trackwidth, double centerWheelOffset) {
        this(gyroAngle, new Pose2d(), trackwidth, centerWheelOffset);
    }

    /**
     * This handles all the calculations for you.
     */
    @Override
    public void updatePose() {
        update(new Rotation2d(m_heading.getAsDouble()), m_left.getAsDouble(),
                              m_right.getAsDouble(), m_horizontal.getAsDouble());
    }

    @Override
    public void updatePose(Pose2d pose) {
        previousAngle = pose.getRotation();
        robotPose = pose;

        prevLeftEncoder = 0;
        prevRightEncoder = 0;
        prevHorizontalEncoder = 0;
    }

    public void update(Rotation2d gyroAngle, double leftEncoderPos, double rightEncoderPos, double horizontalEncoderPos) {
        double deltaLeftEncoder = leftEncoderPos - prevLeftEncoder;
        double deltaRightEncoder = rightEncoderPos - prevRightEncoder;
        double deltaHorizontalEncoder = horizontalEncoderPos - prevHorizontalEncoder;

        Rotation2d angle = gyroAngle.plus(gyroOffset);

        prevLeftEncoder = leftEncoderPos;
        prevRightEncoder = rightEncoderPos;
        prevHorizontalEncoder = horizontalEncoderPos;
        
        // Averaging encoder method with gyro method
        double dw = (angle.minus(previousAngle).getRadians());
        
        double dx = (deltaLeftEncoder + deltaRightEncoder) / 2;
        double dy = deltaHorizontalEncoder - (centerWheelOffset * dw);
        
        Twist2d twist2d = new Twist2d(dx, dy, dw);

        Pose2d newPose = robotPose.exp(twist2d);

        previousAngle = angle;

        robotPose = new Pose2d(newPose.getTranslation(), angle);
    }

}
