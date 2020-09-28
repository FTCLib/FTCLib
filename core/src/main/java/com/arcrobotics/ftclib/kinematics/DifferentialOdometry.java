package com.arcrobotics.ftclib.kinematics;

import com.arcrobotics.ftclib.drivebase.DifferentialDrive;
import com.arcrobotics.ftclib.geometry.Pose2d;
import com.arcrobotics.ftclib.geometry.Rotation2d;
import com.arcrobotics.ftclib.geometry.Transform2d;
import com.arcrobotics.ftclib.geometry.Translation2d;
import com.arcrobotics.ftclib.geometry.Twist2d;

import java.util.function.DoubleSupplier;

/**
 * The classfile that performs odometry calculations for a differential drivetrain.
 * For more information on the differential drivebase, see {@link DifferentialDrive}.
 *
 * FIXME: @JarnaChao09
 * Change this to pose exponential.
 */
public class DifferentialOdometry extends Odometry {

    private double prevLeftEncoder, prevRightEncoder;
    private Rotation2d previousAngle, gyroOffset;

    // the suppliers
    DoubleSupplier m_left, m_right;

    public DifferentialOdometry(Rotation2d gyroAngle,
                                DoubleSupplier leftEncoder, DoubleSupplier rightEncoder,
                                double trackWidth) {
        this(gyroAngle, trackWidth);
        m_left = leftEncoder;
        m_right = rightEncoder;
    }

    /**
     * The constructor that specifies the track width of the robot but defaults
     * the position to (0, 0, 0).
     *
     * @param gyroAngle  the starting heading of the robot
     * @param trackWidth the track width of the robot in inches.
     */
    public DifferentialOdometry(Rotation2d gyroAngle, double trackWidth) {
        this(gyroAngle, new Pose2d(), trackWidth);
    }

    /**
     * The constructor that specifies the starting position and the track width.
     *
     * @param gyroAngle   the starting heading of the robot
     * @param initialPose the starting position of the robot
     * @param trackWidth  the track width of the robot in inches
     */
    public DifferentialOdometry(Rotation2d gyroAngle, Pose2d initialPose, double trackWidth) {
        super(initialPose, trackWidth);
        gyroOffset = robotPose.getRotation().minus(gyroAngle);
        previousAngle = initialPose.getRotation();
    }

    /**
     * Updates the position of the robot.
     *
     * @param newPose the new {@link Pose2d}
     */
    @Override
    public void updatePose(Pose2d newPose) {
        previousAngle = newPose.getRotation();
        robotPose = newPose;

        prevLeftEncoder = 0;
        prevRightEncoder = 0;
    }

    /**
     * This does everything for you.
     */
    @Override
    public void updatePose() {
        updatePosition(
                new Rotation2d((m_left.getAsDouble() - m_right.getAsDouble()) / trackWidth),
                m_left.getAsDouble(),
                m_right.getAsDouble()
        );
    }


    /**
     * The update method that updates the robot's position over a relatively small amount of time.
     * The update frequency can range in a value of Hz. However many updates that are called per
     * second is the representation of the feedback frequency in Hertz (1/s). If we have
     * a frequency of 60 Hz, then the update method is called 60 times in 1 second.
     * If you have no heading, pass in a value of 0 for each update. If you have no horizontal odometers,
     * pass in a value of 0 for horizontalOdoInches.
     *
     * @param gyroAngle       the current heading of the robot, which might be de-synced
     *                        with the heading in the robot position.
     * @param leftEncoderPos  the encoder position of the left encoder.
     * @param rightEncoderPos the encoder position of the right encoder.
     */
    public void updatePosition(Rotation2d gyroAngle, double leftEncoderPos, double rightEncoderPos) {
        double deltaLeftDistance = leftEncoderPos - prevLeftEncoder;
        double deltaRightDistance = rightEncoderPos - prevRightEncoder;

        prevLeftEncoder = leftEncoderPos;
        prevRightEncoder = rightEncoderPos;

        double dx = (deltaLeftDistance + deltaRightDistance) / 2.0;
        Rotation2d angle = gyroAngle.plus(gyroOffset);

        Pose2d newPose = robotPose.exp(
                new Twist2d(dx, 0.0, angle.minus(previousAngle).getRadians())
        );

        previousAngle = angle;

        robotPose = new Pose2d(newPose.getTranslation(), angle);
    }
}