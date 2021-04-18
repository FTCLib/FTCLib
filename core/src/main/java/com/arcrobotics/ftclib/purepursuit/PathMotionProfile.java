package com.arcrobotics.ftclib.purepursuit;

/**
 * This class is utility class that is used by Path to adjust the robot speed as it approaches
 * or leaves a destination. Users can use this class to create a custom motion profile.
 *
 * @author Michael Baljet, Team 14470
 * @version 1.1
 * @see Path
 */
public abstract class PathMotionProfile {

    // Keeps track of previous method calls.
    private double lastDistanceToTarget;
    private long lastCallTimeStamp;

    // True if last call was decelerate, false if last call was accelerate
    private boolean lastCallType;

    /**
     * Constructs a PathMotionProfile object. The user just implement the adjustMotorSpeeds() method.
     */
    public PathMotionProfile() {
        lastCallTimeStamp = -1;
        lastDistanceToTarget = -1;
        lastCallType = true;
    }

    /**
     * Adjusts the motor speeds to decelerate the robot based on this motion profile. This is used to slow the robot when it approaches a target point. This method is called by Path
     * to adjust it's motor speeds while decelerating. Calling this method will relay the given information to the decelerate() method.
     *
     * @param motorSpeeds             Raw motor speeds.
     * @param distanceToTarget        Distance the robot is from it's target.
     * @param configuredMovementSpeed Configured movement speed.
     * @param configuredTurnSpeed     Configured turn speed.
     */
    public void processDecelerate(double[] motorSpeeds, double distanceToTarget, double configuredMovementSpeed, double configuredTurnSpeed) {
        if (lastCallType == true)
            // Call decelerate().
            decelerate(motorSpeeds, distanceToTarget, (lastDistanceToTarget - distanceToTarget) / ((System.nanoTime() - lastCallTimeStamp) * 1e9), configuredMovementSpeed, configuredTurnSpeed);
        else
            // If the last call was not a decelerate, then skip the first call.
            lastCallType = true;
        // Update fields.
        lastDistanceToTarget = distanceToTarget;
        lastCallTimeStamp = System.nanoTime();
    }

    /**
     * Adjusts the motor speeds to accelerate the robot based on this motion profile. This is used to speed up the robot when it leaves a target point. This method is called by Path
     * to adjust it's motor speeds while accelerating. Calling this method will relay the given information to the accelerate() method.
     *
     * @param motorSpeeds             Raw motor speeds.
     * @param distanceFromTarget      Distance the robot is from it's target.
     * @param configuredMovementSpeed Configured movement speed.
     * @param configuredTurnSpeed     Configured turn speed.
     */
    public void processAccelerate(double[] motorSpeeds, double distanceFromTarget, double configuredMovementSpeed, double configuredTurnSpeed) {
        if (lastCallType == false)
            // Call accelerate().
            accelerate(motorSpeeds, distanceFromTarget, (distanceFromTarget - lastDistanceToTarget) / ((System.nanoTime() - lastCallTimeStamp) * 1e9), configuredMovementSpeed, configuredTurnSpeed);
        else
            // If the last call was not a decelerate, then skip the first call.
            lastCallType = false;
        // Update fields.
        lastDistanceToTarget = distanceFromTarget;
        lastCallTimeStamp = System.nanoTime();
    }

    /**
     * Decelerates the motor speeds. This is used to slow the robot down when it approaches a target point.
     *
     * @param motorSpeeds             Raw motor speeds.
     * @param distanceToTarget        Distance the robot is from the target destination.
     * @param speed                   The robot's calculated speed, in units/second.	 * @param configuredMovementSpeed Configured movement speed.
     * @param configuredMovementSpeed Configured movement speed.
     * @param configuredTurnSpeed     Configured turn speed.
     */
    public abstract void decelerate(double[] motorSpeeds, double distanceToTarget, double speed, double configuredMovementSpeed, double configuredTurnSpeed);

    /**
     * Accelerates the motor speeds. This is used to speed the robot up when it approaches a target point.
     *
     * @param motorSpeeds             Raw motor speeds.
     * @param distanceFromTarget      Distance the robot is from the target.
     * @param speed                   The robot's calculated speed, in units/second.
     * @param configuredMovementSpeed Configured movement speed.
     * @param configuredTurnSpeed     Configured turn speed.
     */
    public abstract void accelerate(double[] motorSpeeds, double distanceFromTarget, double speed, double configuredMovementSpeed, double configuredTurnSpeed);


}
