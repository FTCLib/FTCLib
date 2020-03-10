package com.arcrobotics.ftclib.drivebase;

public abstract class RobotDrive {

    public static final double kDefaultRangeMin = -1.0;
    public static final double kDefaultRangeMax = 1.0;
    public static final double kDefaultMaxSpeed = 1.0;

    protected double rangeMin = kDefaultRangeMin;
    protected double rangeMax = kDefaultRangeMax;
    protected double maxOutput = kDefaultMaxSpeed;

    /**
     * The location of the motor on the robot.
     * We assume the drivebase is made of at least two
     * and at most four motors.
     */
    public enum MotorType {
        kBackLeft(2),
        kBackRight(3),
        kFrontLeft(0),
        kFrontRight(1),
        kLeft(0),
        kRight(1),
        kSlide(2);

        public final int value;

        MotorType(int value) {
            this.value = value;
        }
    }

    public RobotDrive() {
        // constructor uwu
    }

    /**
     * Scale the output speed to the specified maxOutput value.
     * The drivespeed is multiplied by this value.
     *
     * <p>The default value is {@value #kDefaultMaxSpeed}.</p>
     *
     * @param maxOutput Multiplied with the output percentage computed by the drive functions.
     */
    public void setMaxSpeed(double maxOutput) {
        this.maxOutput = maxOutput;
    }

    /**
     * Sets the clipped range for the drive inputs.
     *
     * <p>The default clip range is {@value #kDefaultRangeMin} to {@value #kDefaultRangeMax}.
     * Inputs smaller than the minimum are set to -1 and inputs greater
     * than 1 are set to 1. See {@link #clipRange}</p>
     *
     * @param min The minimum value of the range.
     * @param max The maximum value of the range.
     */
    public void setRange(double min, double max) {
        rangeMin = min;
        rangeMax = max;
    }

    /**
     * Returns minimum range value if the given value is less than
     * the set minimum. If the value is greater than the set maximum,
     * then the method returns the maximum value.
     *
     * @param value The value to clip.
     */
    public double clipRange(double value) {
        return value <= rangeMin ? rangeMin
                : value >= rangeMax ? rangeMax
                : value;
    }

    public abstract void stopMotor();

    /**
     * Normalize the wheel speeds
     */
    protected void normalize(double[] wheelSpeeds) {
        double maxMagnitude = Math.abs(wheelSpeeds[0]);
        for (int i = 1; i < wheelSpeeds.length; i++) {
            double temp = Math.abs(wheelSpeeds[i]);
            if (maxMagnitude < temp) {
                maxMagnitude = temp;
            }
        }
        for (int i = 0; i < wheelSpeeds.length; i++) {
            wheelSpeeds[i] = wheelSpeeds[i] / maxMagnitude;
        }

    }

    /**
     * Square magnitude of number while keeping the sign.
     */
    protected double squareInput(double input) {
        return input * Math.abs(input);
    }

}