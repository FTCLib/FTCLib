package com.arcrobotics.ftclib.kinematics.wpilibkinematics;

import com.arcrobotics.ftclib.geometry.Translation2d;

import org.ejml.simple.SimpleMatrix;

public class MecanumOdoKinematics {
    private SimpleMatrix m_inverseKinematics;
    private final SimpleMatrix m_forwardKinematics;

    private final Translation2d m_frontLeftWheelMeters;
    private final Translation2d m_frontRightWheelMeters;
    private final Translation2d m_rearLeftWheelMeters;
    private final Translation2d m_rearRightWheelMeters;
    private final double auxDistance;

    private Translation2d m_prevCoR = new Translation2d();

    private double wheelbaseRadius;

    /**
     * Constructs a mecanum drive kinematics object.
     *
     * @param frontLeftWheelMeters  The location of the front-left wheel relative to the
     *                              physical center of the robot.
     * @param frontRightWheelMeters The location of the front-right wheel relative to
     *                              the physical center of the robot.
     * @param rearLeftWheelMeters   The location of the rear-left wheel relative to the
     *                              physical center of the robot.
     * @param rearRightWheelMeters  The location of the rear-right wheel relative to the
     *                              physical center of the robot.
     */
    public MecanumOdoKinematics(Translation2d frontLeftWheelMeters,
                                  Translation2d frontRightWheelMeters,
                                  Translation2d rearLeftWheelMeters,
                                  Translation2d rearRightWheelMeters, double auxDistance, double wheelbaseWidth) {
        m_frontLeftWheelMeters = frontLeftWheelMeters;
        m_frontRightWheelMeters = frontRightWheelMeters;
        m_rearLeftWheelMeters = rearLeftWheelMeters;
        m_rearRightWheelMeters = rearRightWheelMeters;
        this.auxDistance = auxDistance;
        m_inverseKinematics = new SimpleMatrix(4, 3);

        setInverseKinematics(frontLeftWheelMeters, frontRightWheelMeters,
                rearLeftWheelMeters, rearRightWheelMeters);
        m_forwardKinematics = m_inverseKinematics.pseudoInverse();

        wheelbaseRadius = wheelbaseWidth / 2;

    }

    /**
     * Performs inverse kinematics to return the wheel speeds from a desired chassis velocity. This
     * method is often used to convert joystick values into wheel speeds.
     *
     * <p>This function also supports variable centers of rotation. During normal
     * operations, the center of rotation is usually the same as the physical
     * center of the robot; therefore, the argument is defaulted to that use case.
     * However, if you wish to change the center of rotation for evasive
     * manuevers, vision alignment, or for any other use case, you can do so.
     *
     * @param chassisSpeeds    The desired chassis speed.
     * @param centerOfRotationMeters The center of rotation. For example, if you set the
     *                         center of rotation at one corner of the robot and provide
     *                         a chassis speed that only has a dtheta component, the robot
     *                         will rotate around that corner.
     * @return  The wheel speeds. Use caution because they are not normalized. Sometimes, a user
     *          input may cause one of the wheel speeds to go above the attainable max velocity. Use
     *          the {@link MecanumDriveWheelSpeeds#normalize(double)} function to rectify this issue.
     */
    public MecanumDriveWheelSpeeds toWheelSpeeds(ChassisSpeeds chassisSpeeds,
                                                 Translation2d centerOfRotationMeters) {
        // We have a new center of rotation. We need to compute the matrix again.
        if (!centerOfRotationMeters.equals(m_prevCoR)) {
            Translation2d fl = m_frontLeftWheelMeters.minus(centerOfRotationMeters);
            Translation2d fr = m_frontRightWheelMeters.minus(centerOfRotationMeters);
            Translation2d rl = m_rearLeftWheelMeters.minus(centerOfRotationMeters);
            Translation2d rr = m_rearRightWheelMeters.minus(centerOfRotationMeters);

            setInverseKinematics(fl, fr, rl, rr);
            m_prevCoR = centerOfRotationMeters;
        }

        SimpleMatrix chassisSpeedsVector = new SimpleMatrix(3, 1);
        chassisSpeedsVector.setColumn(0, 0,
                chassisSpeeds.vxMetersPerSecond, chassisSpeeds.vyMetersPerSecond,
                chassisSpeeds.omegaRadiansPerSecond);

        SimpleMatrix wheelsMatrix = m_inverseKinematics.mult(chassisSpeedsVector);
        return new MecanumDriveWheelSpeeds(
                wheelsMatrix.get(0, 0),
                wheelsMatrix.get(1, 0),
                wheelsMatrix.get(2, 0),
                wheelsMatrix.get(3, 0)
        );
    }

    /**
     * Performs inverse kinematics. See {@link #toWheelSpeeds(ChassisSpeeds, Translation2d)} for more
     * information.
     *
     * @param chassisSpeeds The desired chassis speed.
     * @return The wheel speeds.
     */
    public MecanumDriveWheelSpeeds toWheelSpeeds(ChassisSpeeds chassisSpeeds) {
        return toWheelSpeeds(chassisSpeeds, new Translation2d());
    }

    /**
     * Performs forward kinematics to return the resulting chassis state from the given wheel speeds.
     * This method is often used for odometry -- determining the robot's position on the field using
     * data from the real-world speed of each wheel on the robot.
     *
     * @param wheelSpeeds The current mecanum drive wheel speeds.
     * @return The resulting chassis speed.
     */
    public ChassisSpeeds toChassisSpeeds(OdoWheelSpeeds wheelSpeeds) {
        double omega = (wheelSpeeds.rightMetersPerSecond - wheelSpeeds.leftMetersPerSecond)
                        / (wheelbaseRadius * 2);
        return new ChassisSpeeds(
                (wheelSpeeds.leftMetersPerSecond + wheelSpeeds.rightMetersPerSecond) / 2, wheelSpeeds.centerMetersPerSecond - auxDistance * omega,
                (omega));
    }

    /**
     * Construct inverse kinematics matrix from wheel locations.
     *
     * @param fl The location of the front-left wheel relative to the physical center of the robot.
     * @param fr The location of the front-right wheel relative to the physical center of the robot.
     * @param rl The location of the rear-left wheel relative to the physical center of the robot.
     * @param rr The location of the rear-right wheel relative to the physical center of the robot.
     */
    private void setInverseKinematics(Translation2d fl, Translation2d fr,
                                      Translation2d rl, Translation2d rr) {
        m_inverseKinematics.setRow(0, 0, 1, -1, -(fl.getX() + fl.getY()));
        m_inverseKinematics.setRow(1, 0, 1, 1, fr.getX() - fr.getY());
        m_inverseKinematics.setRow(2, 0, 1, 1, rl.getX() - rl.getY());
        m_inverseKinematics.setRow(3, 0, 1, -1, -(rr.getX() + rr.getY()));
        m_inverseKinematics = m_inverseKinematics.scale(1.0 / Math.sqrt(2));
    }
}
