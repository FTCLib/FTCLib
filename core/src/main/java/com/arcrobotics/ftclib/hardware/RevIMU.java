package com.arcrobotics.ftclib.hardware;

import com.arcrobotics.ftclib.geometry.Rotation2d;
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.IMU;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles;

public class RevIMU {

    private final IMU revIMU;

    /**
     * Offset between global heading and relative heading
     */
    double offset;

    private int multiplier = 1;

    /**
     * Create a new object for the built-in gyro/imu in the Rev Expansion Hub
     *
     * @param hw      Hardware map
     * @param imuName Name of sensor in configuration
     */
    public RevIMU(HardwareMap hw, String imuName) {
        revIMU = hw.get(IMU.class, imuName);
    }

    /**
     * Create a new object for the built-in gyro/imu in the Rev Expansion Hub with the default configuration name of "imu"
     *
     * @param hw Hardware map
     */
    public RevIMU(HardwareMap hw) {
        this(hw, "imu");
    }

    /**
     * Initializes gyro with custom parameters.
     */
    public void init(RevHubOrientationOnRobot orientation) {
        revIMU.initialize(new IMU.Parameters(orientation));
    }

    /**
     * Inverts the output of gyro
     */
    public void invertGyro() {
        multiplier *= -1;
    }

    /**
     * @param unit Unit to output the heading in
     * @return Relative heading of the robot
     */
    public double getHeading(AngleUnit unit) {
        return revIMU.getRobotYawPitchRollAngles().getYaw(unit) * multiplier;
    }

    /**
     * @return Relative heading of the robot in degrees
     */
    public double getHeading() {
        return getHeading(AngleUnit.DEGREES);
    }

    /**
     * @param unit Unit to output the heading in
     * @return Absolute heading of the robot
     */
    public double getAbsoluteHeading(AngleUnit unit) {
        return getHeading(unit) + unit.fromDegrees(offset) * multiplier;
    }

    /**
     * @return Absolute heading of the robot in degrees
     */
    public double getAbsoluteHeading() {
        return getAbsoluteHeading(AngleUnit.DEGREES);
    }

    /**
     * @return Yaw, Pitch, Roll angles of gyro
     */
    public YawPitchRollAngles getAngles() {
        return revIMU.getRobotYawPitchRollAngles();
    }

    public double[] getAngles(AngleUnit unit) {
        YawPitchRollAngles angles = revIMU.getRobotYawPitchRollAngles();
        return new double[]{
                angles.getYaw(unit),
                angles.getPitch(unit),
                angles.getRoll(unit)
        };
    }

    /**
     * @return Transforms heading into {@link Rotation2d}
     */
    public Rotation2d getRotation2d() {
        return Rotation2d.fromDegrees(getHeading());
    }

    public void disable() {
        revIMU.close();
    }

    public void reset() {
        offset += getHeading();
        revIMU.resetYaw();
    }

    public String getDeviceType() {
        return "Rev IMU";
    }

    /**
     * @return the internal sensor being wrapped
     */
    public IMU getRevIMU() {
        return revIMU;
    }

}
