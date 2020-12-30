package com.arcrobotics.ftclib.purepursuit.waypoints;

import com.arcrobotics.ftclib.geometry.Pose2d;
import com.arcrobotics.ftclib.geometry.Rotation2d;
import com.arcrobotics.ftclib.geometry.Translation2d;
import com.arcrobotics.ftclib.purepursuit.actions.InterruptAction;
import com.arcrobotics.ftclib.purepursuit.types.WaypointType;

/**
 * An end waypoint is an InterruptWaypoint used to represent the end of a path. Every path must end with one
 * of these. This waypoint's action cannot be changed.
 *
 * @author Michael Baljet, Team 14470
 * @version 1.1
 * @see InterruptWaypoint
 */
public class EndWaypoint extends InterruptWaypoint {

    // True if the robot has reached this point, false otherwise.
    private boolean isFinished;

    /**
     * Constructs an EndWaypoint. All values are set to their default.
     */
    public EndWaypoint() {
        super.setAction(generateEndAction());
        isFinished = false;
    }

    /**
     * Constructs an EndWaypoint with the provided values.
     *
     * @param translation    The (x, y) translation of this waypoint.
     * @param rotation       The rotation (preferred angle) of this waypoint.
     * @param movementSpeed  The speed in which the robot moves at while traversing this waypoint, in the range [0, 1].
     * @param turnSpeed      The speed in which the robot turns at while traversing this waypoint, in the range [0, 1].
     * @param followRadius   The distance in which the robot traverses this waypoint. Please see guides to learn more about this value.
     * @param positionBuffer The expected level of error, the robot will consider itself at the waypoint when it is within the buffer. The buffer must be > 0.
     * @param rotationBuffer The expected level of error (in radians), the robot will consider itself at the waypoint when it is within the buffer. The buffer must be > 0.
     */
    public EndWaypoint(Translation2d translation, Rotation2d rotation, double movementSpeed, double turnSpeed, double followRadius, double positionBuffer, double rotationBuffer) {
        super(translation, rotation, movementSpeed, turnSpeed, followRadius, positionBuffer, rotationBuffer, null);
        super.setAction(generateEndAction());
        isFinished = false;
    }

    /**
     * Constructs an EndWaypoint with the provided values.
     *
     * @param pose           Position and rotation (preferred angle) of this waypoint.
     * @param movementSpeed  The speed in which the robot moves at while traversing this waypoint, in the range [0, 1].
     * @param turnSpeed      The speed in which the robot turns at while traversing this waypoint, in the range [0, 1].
     * @param followRadius   The distance in which the robot traverses this waypoint. Please see guides to learn more about this value.
     * @param positionBuffer The expected level of error, the robot will consider itself at the waypoint when it is within the buffer. The buffer must be > 0.
     * @param rotationBuffer The expected level of error (in radians), the robot will consider itself at the waypoint when it is within the buffer. The buffer must be > 0.
     */
    public EndWaypoint(Pose2d pose, double movementSpeed, double turnSpeed, double followRadius, double positionBuffer, double rotationBuffer) {
        super(pose, movementSpeed, turnSpeed, followRadius, positionBuffer, rotationBuffer, null);
        super.setAction(generateEndAction());
        isFinished = false;
    }

    /**
     * Constructs an EndWaypoint with the provided values.
     *
     * @param x               The x position of this waypoint.
     * @param y               The y position of this waypoint.
     * @param rotationRadians The rotation (preferred angle) of this waypoint (in radians).
     * @param movementSpeed   The speed in which the robot moves at while traversing this waypoint, in the range [0, 1].
     * @param turnSpeed       The speed in which the robot turns at while traversing this waypoint, in the range [0, 1].
     * @param followRadius    The distance in which the robot traverses this waypoint. Please see guides to learn more about this value.
     * @param positionBuffer  The expected level of error, the robot will consider itself at the waypoint when it is within the buffer. The buffer must be > 0.
     * @param rotationBuffer  The expected level of error (in radians), the robot will consider itself at the waypoint when it is within the buffer. The buffer must be > 0.
     */
    public EndWaypoint(double x, double y, double rotationRadians, double movementSpeed, double turnSpeed, double followRadius, double positionBuffer, double rotationBuffer) {
        super(x, y, rotationRadians, movementSpeed, turnSpeed, followRadius, positionBuffer, rotationBuffer, null);
        super.setAction(generateEndAction());
        isFinished = false;
    }

    /**
     * Sets this endpoint as traversed.
     */
    @Override
    public void setTraversed() {
        isFinished = true;
    }

    /**
     * Returns true if the robot has reached this point and the path is finished, false otherwise.
     *
     * @return true if the robot has reached this point and the path is finished, false otherwise.
     */
    public boolean isFinished() {
        return isFinished;
    }

    @Override
    public InterruptWaypoint setAction(InterruptAction action) {
        // You cannot change the action of an end waypoint.
        throw new IllegalArgumentException("You cannot change the action of an end waypoint.");
    }

    /**
     * Creates and returns the InterruptAction for an end point.
     *
     * @return the InterruptAction for an end point.
     */
    private InterruptAction generateEndAction() {
        return new InterruptAction() {
            @Override
            public void doAction() {
                // When the robot reaches the end, isFinished is set to true.
                isFinished = true;
            }
        };
    }

    @Override
    public void reset() {
        super.reset();
        isFinished = false;
    }

    @Override
    public WaypointType getType() {
        return WaypointType.END;
    }

    @Override
    public String toString() {
        return String.format("EndWaypoint(%s, %s)", getTranslation().getX(), getTranslation().getY());
    }

}
