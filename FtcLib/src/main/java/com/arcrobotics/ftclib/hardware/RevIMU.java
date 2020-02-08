package com.arcrobotics.ftclib.hardware;

import com.arcrobotics.ftclib.geometry.Rotation2d;
import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.hardware.bosch.JustLoggingAccelerationIntegrator;
import com.qualcomm.robotcore.hardware.HardwareMap;


public class RevIMU extends GyroEx {

    private BNO055IMU revIMU;
    double globalHeading;
    double relativeHeading;
    double offset;
    private int multiplier;

    public RevIMU(HardwareMap hw, String imuName) {
        revIMU = hw.get(BNO055IMU.class, imuName);
        multiplier = 1;
    }

    public RevIMU(HardwareMap hw) {
        this(hw, "imu");
    }

    public void init() {
        BNO055IMU.Parameters parameters = new BNO055IMU.Parameters();
        parameters.angleUnit           = BNO055IMU.AngleUnit.DEGREES;
        parameters.accelUnit           = BNO055IMU.AccelUnit.METERS_PERSEC_PERSEC;
        parameters.calibrationDataFile = "BNO055IMUCalibration.json"; // see the calibration sample opmode
        parameters.loggingEnabled      = true;
        parameters.loggingTag          = "IMU";
        parameters.accelerationIntegrationAlgorithm = new JustLoggingAccelerationIntegrator();

        revIMU.initialize(parameters);

        globalHeading = 0;
        relativeHeading = 0;
        offset = 0;
    }

    public void init(BNO055IMU.Parameters parameters) {
        revIMU.initialize(parameters);

        globalHeading = 0;
        relativeHeading = 0;
        offset = 0;
    }

    public void invertGyro() {
        multiplier *= -1;
    }

    public double getHeading() {
        globalHeading = revIMU.getAngularOrientation().firstAngle;
        relativeHeading = globalHeading + offset;
        // Return yaw
        return relativeHeading * multiplier;
    }

    @Override
    public double getAbsoluteHeading() {
        return revIMU.getAngularOrientation().firstAngle * multiplier;
    }

    // TODO Find the order of these angles
    public double[] getAngles() {

        double[] angles = new double[4];

        angles[0] = (double) revIMU.getAngularOrientation().firstAngle;
        angles[1] = (double) revIMU.getAngularOrientation().secondAngle;
        angles[2] = (double) revIMU.getAngularOrientation().thirdAngle;

        return angles;
    }

    @Override
    public Rotation2d getRotation2d() {
        return Rotation2d.fromDegrees(getHeading());
    }

    @Override
    /**
     * Don't need to call this function hardly ever.
     */
    public void disable() {
        revIMU.close();
    }

    @Override
    public void reset() {
        offset = -getHeading();
    }

    @Override
    public String getDeviceType() {
        return "Rev Expansion Hub IMU";
    }

    public BNO055IMU getRevIMU() {
        return revIMU;
    }
}
