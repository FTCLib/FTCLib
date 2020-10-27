package com.arcrobotics.ftclib.command;

import com.arcrobotics.ftclib.geometry.Pose2d;
import com.arcrobotics.ftclib.kinematics.Odometry;

public class OdometrySubsystem extends SubsystemBase {

    protected Odometry m_odometry;

    /**
     * Make sure you are using the supplier version of the constructor
     *
     * @param odometry the odometry on the robot
     */
    public OdometrySubsystem(Odometry odometry) {
        m_odometry = odometry;
    }

    public Pose2d getPose() {
        return m_odometry.getPose();
    }

    /**
     * Call this at the end of every loop
     */
    public void update() {
        m_odometry.updatePose();
    }

    /**
     * Updates the pose every cycle
     */
    @Override
    public void periodic() {
        m_odometry.updatePose();
    }

}
