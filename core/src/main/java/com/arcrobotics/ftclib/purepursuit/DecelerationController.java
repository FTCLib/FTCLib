package com.arcrobotics.ftclib.purepursuit;

/**
 * This class is utility class that is used by Path to decelerate the robot as it approaches
 * a destination. Users can use this class to create a custom deceleration profile.
 *
 * @version 1.0
 * @see Path
 */
public abstract class DecelerationController {

    // Keeps track of previous method calls.
    private double lastDistanceToTarget;
    private long lastCallTimeStamp;

    /**
     * Constructs a DecelerationController object. The user just implement the decelerateMotorSpeeds() method.
     */
    public DecelerationController() {
        lastCallTimeStamp = -1;
        lastDistanceToTarget = -1;
    }

    /**
     * Adjusts decelerates the motor speeds. This is used to slow the robot down when it approaches a target point. This method is called by Path
     * to adjust it's motor speeds while decelerating. Calling this method will relay the given information to the decelerateMotorSpeeds() method.
     *
     * @param motorSpeeds             Raw motor speeds.
     * @param distanceToTarget        Distance the robot is from it's target.
     * @param configuredMovementSpeed Configured movement speed.
     * @param configuredTurnSpeed     Configured turn speed.
     */
    public void process(double[] motorSpeeds, double distanceToTarget, double configuredMovementSpeed, double configuredTurnSpeed) {
        // Call decelerateMotorSpeeds().
        decelerateMotorSpeeds(motorSpeeds, distanceToTarget, lastDistanceToTarget, System.nanoTime() - lastCallTimeStamp, configuredMovementSpeed, configuredTurnSpeed);
        // Update fields.
        lastDistanceToTarget = distanceToTarget;
        lastCallTimeStamp = System.nanoTime();
    }

    /**
     * Adjusts decelerates the motor speeds. This is used to slow the robot down when it approaches a target point.
     * <p>
     * Note: It is suggested when estimating speed, verify the speed is possible.
     *
     * @param motorSpeeds             Raw motor speeds.
     * @param distanceToTarget        Distance the robot is from the target destination.
     * @param lastDistanceToTarget    Previous distance to target, can be used to estimate speed. This value is -1 if this is the first time this method is called.
     * @param timeSinceLastCallNano   Time since the last time with method was called (in nanoseconds). This value is -1 if this is the first time this method is called.
     * @param configuredMovementSpeed Configured movement speed.
     * @param configuredTurnSpeed     Configured turn speed.
     */
    public abstract void decelerateMotorSpeeds(double[] motorSpeeds, double distanceToTarget, double lastDistanceToTarget, long timeSinceLastCallNano, double configuredMovementSpeed, double configuredTurnSpeed);

}
