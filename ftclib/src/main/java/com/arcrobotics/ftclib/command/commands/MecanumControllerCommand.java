package com.arcrobotics.ftclib.command.commands;


import com.arcrobotics.ftclib.command.Command;
import com.arcrobotics.ftclib.command.Subsystem;
import com.arcrobotics.ftclib.controller.wpilibcontroller.PIDController;
import com.arcrobotics.ftclib.controller.wpilibcontroller.ProfiledPIDController;
import com.arcrobotics.ftclib.controller.wpilibcontroller.SimpleMotorFeedforward;
import com.arcrobotics.ftclib.geometry.Pose2d;
import com.arcrobotics.ftclib.kinematics.wpilibkinematics.ChassisSpeeds;
import com.arcrobotics.ftclib.kinematics.wpilibkinematics.MecanumDriveKinematics;
import com.arcrobotics.ftclib.kinematics.wpilibkinematics.MecanumDriveMotorVoltages;
import com.arcrobotics.ftclib.kinematics.wpilibkinematics.MecanumDriveWheelSpeeds;
import com.arcrobotics.ftclib.trajectory.Trajectory;
import com.qualcomm.robotcore.util.ElapsedTime;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A command that uses two PID controllers ({@link PIDController}) and a
 * ProfiledPIDController ({@link ProfiledPIDController}) to follow a trajectory
 * {@link Trajectory} with a mecanum drive.
 *
 * <p>The command handles trajectory-following,
 * Velocity PID calculations, and feedforwards internally. This
 * is intended to be a more-or-less "complete solution" that can be used by teams without a great
 * deal of controls expertise.
 *
 * <p>Advanced teams seeking more flexibility (for example, those who wish to use the onboard
 * PID functionality of a "smart" motor controller) may use the secondary constructor that omits
 * the PID and feedforward functionality, returning only the raw wheel speeds from the PID
 * controllers.
 *
 * <p>The robot angle controller does not follow the angle given by
 * the trajectory but rather goes to the angle given in the final state of the trajectory.
 */

@SuppressWarnings({"PMD.TooManyFields", "MemberName"})
public class MecanumControllerCommand implements Command {
    private final ElapsedTime m_timer;
    private MecanumDriveWheelSpeeds m_prevSpeeds;
    private double m_prevTime;
    private Pose2d m_finalPose;
    private final boolean m_usePID;

    private final Trajectory m_trajectory;
    private final Supplier<Pose2d> m_pose;
    private final SimpleMotorFeedforward m_feedforward;
    private final MecanumDriveKinematics m_kinematics;
    private final PIDController m_xController;
    private final PIDController m_yController;
    private final ProfiledPIDController m_thetaController;
    private final double m_maxWheelVelocityMetersPerSecond;
    private final PIDController m_frontLeftController;
    private final PIDController m_rearLeftController;
    private final PIDController m_frontRightController;
    private final PIDController m_rearRightController;
    private final Supplier<MecanumDriveWheelSpeeds> m_currentWheelSpeeds;
    private final Consumer<MecanumDriveMotorVoltages> m_outputDriveVoltages;
    private final Consumer<MecanumDriveWheelSpeeds> m_outputWheelSpeeds;

    /**
     * Constructs a new MecanumControllerCommand that when executed will follow the provided
     * trajectory. PID control and feedforward are handled internally. Outputs are scaled from -12 to
     * 12 as a voltage output to the motor.
     *
     * <p>Note: The controllers will *not* set the outputVolts to zero upon completion of the path
     * this is left to the user, since it is not appropriate for paths with nonstationary endstates.
     *
     * <p>Note 2: The rotation controller will calculate the rotation based on the final pose in the
     * trajectory, not the poses at each time step.
     *
     * @param trajectory                      The trajectory to follow.
     * @param pose                            A function that supplies the robot pose - use one of
     *                                        the odometry classes to provide this.
     * @param feedforward                     The feedforward to use for the drivetrain.
     * @param kinematics                      The kinematics for the robot drivetrain.
     * @param xController                     The Trajectory Tracker PID controller
     *                                        for the robot's x position.
     * @param yController                     The Trajectory Tracker PID controller
     *                                        for the robot's y position.
     * @param thetaController                 The Trajectory Tracker PID controller
     *                                        for angle for the robot.
     * @param maxWheelVelocityMetersPerSecond The maximum velocity of a drivetrain wheel.
     * @param frontLeftController             The front left wheel velocity PID.
     * @param rearLeftController              The rear left wheel velocity PID.
     * @param frontRightController            The front right wheel velocity PID.
     * @param rearRightController             The rear right wheel velocity PID.
     * @param currentWheelSpeeds              A MecanumDriveWheelSpeeds object containing
     *                                        the current wheel speeds.
     * @param outputDriveVoltages             A MecanumDriveMotorVoltages object containing
     *                                        the output motor voltages.
     */

    @SuppressWarnings({"PMD.ExcessiveParameterList", "ParameterName"})
    public MecanumControllerCommand(Trajectory trajectory,
                                    Supplier<Pose2d> pose,
                                    SimpleMotorFeedforward feedforward,
                                    MecanumDriveKinematics kinematics,

                                    PIDController xController,
                                    PIDController yController,
                                    ProfiledPIDController thetaController,

                                    double maxWheelVelocityMetersPerSecond,

                                    PIDController frontLeftController,
                                    PIDController rearLeftController,
                                    PIDController frontRightController,
                                    PIDController rearRightController,

                                    Supplier<MecanumDriveWheelSpeeds> currentWheelSpeeds,

                                    Consumer<MecanumDriveMotorVoltages> outputDriveVoltages) {
        m_trajectory = trajectory;
        m_pose = pose;
        m_feedforward = feedforward;
        m_kinematics = kinematics;

        m_xController = xController;

        m_yController = yController;
        m_thetaController = thetaController;

        m_maxWheelVelocityMetersPerSecond = maxWheelVelocityMetersPerSecond;

        m_frontLeftController = frontLeftController;
        m_rearLeftController = rearLeftController;

        m_frontRightController = frontRightController;
        m_rearRightController = rearRightController;

        m_currentWheelSpeeds = currentWheelSpeeds;

        m_outputDriveVoltages = outputDriveVoltages;

        m_outputWheelSpeeds = null;

        m_usePID = true;


        m_timer = new ElapsedTime();
    }

    /**
     * Constructs a new MecanumControllerCommand that when executed will follow the provided
     * trajectory. The user should implement a velocity PID on the desired output wheel velocities.
     *
     * <p>Note: The controllers will *not* set the outputVolts to zero upon completion of the path -
     * this is left to the user, since it is not appropriate for paths with non-stationary end-states.
     *
     * <p>Note2: The rotation controller will calculate the rotation based on the final pose
     * in the trajectory, not the poses at each time step.
     *
     * @param trajectory                      The trajectory to follow.
     * @param pose                            A function that supplies the robot pose - use one of
     *                                        the odometry classes to provide this.
     * @param kinematics                      The kinematics for the robot drivetrain.
     * @param xController                     The Trajectory Tracker PID controller
     *                                        for the robot's x position.
     * @param yController                     The Trajectory Tracker PID controller
     *                                        for the robot's y position.
     * @param thetaController                 The Trajectory Tracker PID controller
     *                                        for angle for the robot.
     * @param maxWheelVelocityMetersPerSecond The maximum velocity of a drivetrain wheel.
     * @param outputWheelSpeeds               A MecanumDriveWheelSpeeds object containing
     *                                        the output wheel speeds.
     */

    @SuppressWarnings({"PMD.ExcessiveParameterList", "ParameterName"})
    public MecanumControllerCommand(Trajectory trajectory,
                                    Supplier<Pose2d> pose,
                                    MecanumDriveKinematics kinematics,
                                    PIDController xController,
                                    PIDController yController,
                                    ProfiledPIDController thetaController,

                                    double maxWheelVelocityMetersPerSecond,

                                    Consumer<MecanumDriveWheelSpeeds> outputWheelSpeeds) {

        m_trajectory = trajectory;
        m_pose = pose;
        m_feedforward = new SimpleMotorFeedforward(0, 0, 0);
        m_kinematics = kinematics;

        m_xController = xController;

        m_yController = yController;
        m_thetaController = thetaController;

        m_maxWheelVelocityMetersPerSecond = maxWheelVelocityMetersPerSecond;


        m_frontLeftController = null;
        m_rearLeftController = null;
        m_frontRightController = null;
        m_rearRightController = null;

        m_currentWheelSpeeds = null;

        m_outputWheelSpeeds = outputWheelSpeeds;

        m_outputDriveVoltages = null;

        m_usePID = false;

        m_timer = new ElapsedTime();
    }

    @Override
    public void initialize() {
        Trajectory.State initialState = m_trajectory.sample(0);

        // Sample final pose to get robot rotation
        m_finalPose = m_trajectory.sample(m_trajectory.getTotalTimeSeconds()).poseMeters;

        double initialXVelocity = initialState.velocityMetersPerSecond
                * initialState.poseMeters.getRotation().getCos();
        double initialYVelocity = initialState.velocityMetersPerSecond
                * initialState.poseMeters.getRotation().getSin();

        m_prevSpeeds = m_kinematics.toWheelSpeeds(
                new ChassisSpeeds(initialXVelocity, initialYVelocity, 0.0));

        // Resets and starts the timer
        m_timer.reset();
    }

    @Override
    @SuppressWarnings("LocalVariableName")
    public void execute() {
        double curTime = m_timer.seconds();
        double dt = curTime - m_prevTime;

        Trajectory.State desiredState = m_trajectory.sample(curTime);
        Pose2d desiredPose = desiredState.poseMeters;

        Pose2d poseError = desiredPose.relativeTo(m_pose.get());

        double targetXVel = m_xController.calculate(
                m_pose.get().getTranslation().getX(),
                desiredPose.getTranslation().getX());

        double targetYVel = m_yController.calculate(
                m_pose.get().getTranslation().getY(),
                desiredPose.getTranslation().getY());

        // The robot will go to the desired rotation of the final pose in the trajectory,
        // not following the poses at individual states.
        double targetAngularVel = m_thetaController.calculate(
                m_pose.get().getRotation().getRadians(),
                m_finalPose.getRotation().getRadians());

        double vRef = desiredState.velocityMetersPerSecond;

        targetXVel += vRef * poseError.getRotation().getCos();
        targetYVel += vRef * poseError.getRotation().getSin();

        ChassisSpeeds targetChassisSpeeds = new ChassisSpeeds(targetXVel, targetYVel, targetAngularVel);

        MecanumDriveWheelSpeeds targetWheelSpeeds = m_kinematics.toWheelSpeeds(targetChassisSpeeds);

        targetWheelSpeeds.normalize(m_maxWheelVelocityMetersPerSecond);

        double frontLeftSpeedSetpoint = targetWheelSpeeds.frontLeftMetersPerSecond;
        double rearLeftSpeedSetpoint = targetWheelSpeeds.rearLeftMetersPerSecond;
        double frontRightSpeedSetpoint = targetWheelSpeeds.frontRightMetersPerSecond;
        double rearRightSpeedSetpoint =  targetWheelSpeeds.rearRightMetersPerSecond;

        double frontLeftOutput;
        double rearLeftOutput;
        double frontRightOutput;
        double rearRightOutput;

        if (m_usePID) {
            final double frontLeftFeedforward = m_feedforward.calculate(frontLeftSpeedSetpoint,
                    (frontLeftSpeedSetpoint - m_prevSpeeds.frontLeftMetersPerSecond) / dt);

            final double rearLeftFeedforward = m_feedforward.calculate(rearLeftSpeedSetpoint,
                    (rearLeftSpeedSetpoint - m_prevSpeeds.rearLeftMetersPerSecond) / dt);

            final double frontRightFeedforward = m_feedforward.calculate(frontRightSpeedSetpoint,
                    (frontRightSpeedSetpoint - m_prevSpeeds.frontRightMetersPerSecond) / dt);

            final double rearRightFeedforward = m_feedforward.calculate(rearRightSpeedSetpoint,
                    (rearRightSpeedSetpoint - m_prevSpeeds.rearRightMetersPerSecond) / dt);

            frontLeftOutput = frontLeftFeedforward + m_frontLeftController.calculate(
                    m_currentWheelSpeeds.get().frontLeftMetersPerSecond,
                    frontLeftSpeedSetpoint);

            rearLeftOutput = rearLeftFeedforward + m_rearLeftController.calculate(
                    m_currentWheelSpeeds.get().rearLeftMetersPerSecond,
                    rearLeftSpeedSetpoint);

            frontRightOutput = frontRightFeedforward + m_frontRightController.calculate(
                    m_currentWheelSpeeds.get().frontRightMetersPerSecond,
                    frontRightSpeedSetpoint);

            rearRightOutput = rearRightFeedforward + m_rearRightController.calculate(
                    m_currentWheelSpeeds.get().rearRightMetersPerSecond,
                    rearRightSpeedSetpoint);

            m_outputDriveVoltages.accept(new MecanumDriveMotorVoltages(
                    frontLeftOutput,
                    frontRightOutput,
                    rearLeftOutput,
                    rearRightOutput));

        } else {
            m_outputWheelSpeeds.accept(new MecanumDriveWheelSpeeds(
                    frontLeftSpeedSetpoint,
                    frontRightSpeedSetpoint,
                    rearLeftSpeedSetpoint,
                    rearRightSpeedSetpoint));
        }

        m_prevTime = curTime;
        m_prevSpeeds = targetWheelSpeeds;
    }

    @Override
    public void end() {
    }

    @Override
    public boolean isFinished() {
        return m_trajectory.getTotalTimeSeconds() > m_timer.seconds();
    }
}