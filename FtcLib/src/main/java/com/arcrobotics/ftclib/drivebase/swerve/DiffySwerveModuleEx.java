package com.arcrobotics.ftclib.drivebase.swerve;

import com.arcrobotics.ftclib.controller.PController;
import com.arcrobotics.ftclib.geometry.Vector2d;
import com.arcrobotics.ftclib.hardware.motors.MotorEx;

import java.util.function.DoubleSupplier;

public class DiffySwerveModuleEx extends DiffySwerveModule {

    /**
     * This is the constant that corresponds to the
     * revolutions of the module; this value must be tuned
     */
    public double kRevConstant;

    /**
     * This is the constant that corresponds to the revolution
     * of the wheel rather than the rotation of the module;
     * this value must be tuned
     */
    public double kWheelConstant;

    private double distanceTravelled;
    private double lastMotor1EncoderCount;
    private double lastMotor2EncoderCount;

    /**
     * This is the heading of the module. You can use it
     * to determine the heading.
     */
    public DoubleSupplier moduleHeading;

    private PController angleController;

    /**
     * Make sure the motors are already reversed if needed
     *
     * @param one the first motor
     * @param two the second motor
     */
    public DiffySwerveModuleEx(MotorEx one, MotorEx two) {
        this(one, two, 1, 1, 0.05);
    }

    /**
     * Make sure the motors are already reversed if needed
     *
     * @param one       the first motor
     * @param two       the second motor
     * @param kAngle    the module constant that is used for heading;
     *                  this is a value that needs to be tuned;
     *                  it takes into consideration the gear ratios
     *                  and counts per revolution of the motors
     * @param kWheel    the module constant that is used for distance;
     *                  this is a value that needs to be tuned;
     *                  it takes into consideration the gear ratios
     *                  and counts per revolution of the motors
     * @param kP        the P value for the P controller that limits the freedom
     *                  of rotation for the module
     */
    public DiffySwerveModuleEx(MotorEx one, MotorEx two, double kAngle, double kWheel,
                               double kP) {
        super(one, two);

        kRevConstant = kAngle;
        kWheelConstant = kWheel;

        angleController = new PController(kP);
    }

    /**
     * Resets the encoder values of the motors
     */
    public void resetEncoders() {
        ((MotorEx)m_motorOne).encoder.resetEncoderCount();
        ((MotorEx)m_motorTwo).encoder.resetEncoderCount();
    }

    /**
     * Returns the encoder counts of the motors in the module
     *
     * @return  the encoder counts as an array
     */
    public double[] getRawEncoderCounts() {
        double[] counts = new double[] {
                ((MotorEx)m_motorOne).encoder.getCurrentTicks(),
                ((MotorEx)m_motorTwo).encoder.getCurrentTicks()
            };
        return counts;
    }

    /**
     * Allows you to set the threshold value of the PController
     *
     * @param kThreshold    the desired threshold for the controller
     */
    public void setThreshold(double kThreshold) {
        angleController.setTolerance(kThreshold);
    }

    public double[] getLastEncoderCounts() {
        lastMotor1EncoderCount = getRawEncoderCounts()[0];
        lastMotor2EncoderCount = getRawEncoderCounts()[1];

        return new double[]{lastMotor1EncoderCount, lastMotor2EncoderCount};
    }

    /**
     * Returns the raw heading of the module in radians
     *
     * @return the heading of the module
     */
    public double getRawHeading() {
        return Math.toRadians(kRevConstant * (getRawEncoderCounts()[0] + getRawEncoderCounts()[1]));
    }

    /**
     * Updates the distance travelled
     */
    public void updateTracking() {
        double deltaOne = getRawEncoderCounts()[0] - lastMotor1EncoderCount;
        double deltaTwo = getRawEncoderCounts()[1] - lastMotor2EncoderCount;

        distanceTravelled += (deltaOne - deltaTwo)/2.0 * kWheelConstant;

        getLastEncoderCounts();
    }

    /**
     * You can set this to use the value returned by getRawHeading()
     * or from an external sensor.
     *
     * @param headingReader the heading pointer
     */
    public void setHeadingInterpol(DoubleSupplier headingReader) {
        moduleHeading = headingReader;
    }

    /**
     * @return the distance travelled by the module wheel
     */
    public double getDistanceTravelled() {
        return distanceTravelled;
    }

    /**
     * Once the difference between the desired angle
     * and the current angle reaches 0, the vector becomes
     * (magnitude, 0), which means it will strictly move forward.
     * Note that it reads the value from the heading interpolator as
     * a value in degrees, so it is switched to raidans in the code.
     */
    @Override
    public void driveModule(Vector2d powerVec) {
        double[] angAndMag = optimalAngleAndDirection(powerVec);
        double angle = angAndMag[0];
        double magnitude = angAndMag[1] * powerVec.magnitude();

        double nextError = angleController.calculate(angle, Math.toRadians(moduleHeading.getAsDouble()));
        double oneSpeed = Math.cos(nextError) * magnitude;
        double twoSpeed = Math.sin(nextError) * magnitude;

        super.driveModule(new Vector2d(oneSpeed, twoSpeed));
    }

    private double[] optimalAngleAndDirection(Vector2d vec) {
        double rawAngle1 = vec.angle();
        double rawAngle2 = vec.angle() < 0 ? vec.angle() + Math.PI : vec.angle() - Math.PI;

        double angleApprox = Math.abs(rawAngle1 - Math.toRadians(moduleHeading.getAsDouble())) >
                             Math.abs(rawAngle2 - Math.toRadians(moduleHeading.getAsDouble())) ?
                                rawAngle2 : rawAngle1;

        double direction = vec.angle() < 0 ? -1 : 1;

        return new double[]{angleApprox, direction};
    }

    /**
     * Turns the module to a desired angle. Use this method in a control
     * loop.
     *
     * @param angle The desired set angle in degrees
     */
    public void turnToAngle(double angle) {
        double nextError = angleController.calculate(Math.toRadians(angle), getRawHeading());
        driveModule(new Vector2d(Math.cos(nextError), Math.sin(nextError)));
    }

}