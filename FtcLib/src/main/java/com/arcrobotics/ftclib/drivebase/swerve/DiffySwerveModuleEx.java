package com.arcrobotics.ftclib.drivebase.swerve;

import com.arcrobotics.ftclib.controller.PController;
import com.arcrobotics.ftclib.geometry.Vector2d;
import com.arcrobotics.ftclib.hardware.motors.MotorEx;

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

    private PController angleController;

    /**
     * Make sure the motors are already reversed if needed
     *
     * @param one the first motor
     * @param two the second motor
     */
    public DiffySwerveModuleEx(MotorEx one, MotorEx two) {
        this(one, two, 1, 1);
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
     */
    public DiffySwerveModuleEx(MotorEx one, MotorEx two, double kAngle, double kWheel) {
        super(one, two);

        kRevConstant = kAngle;
        kWheelConstant = kWheel;

        //TODO: tune this p-value
        angleController = new PController(0.05);
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
     * @return the distance travelled by the module wheel
     */
    public double getDistanceTravelled() {
        return distanceTravelled;
    }

    /**
     * Once the difference between the desired angle
     * and the current angle reaches 0, the vector becomes
     * (magnitude, 0), which means it will strictly move forward.
     */
    @Override
    public void driveModule(Vector2d powerVec) {
        double angle = powerVec.angle();
        double magnitude = powerVec.magnitude();

        double nextError = angleController.calculate(angle, getRawHeading());
        double oneSpeed = Math.cos(nextError) * magnitude;
        double twoSpeed = Math.sin(nextError) * magnitude;

        super.driveModule(new Vector2d(oneSpeed, twoSpeed));
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