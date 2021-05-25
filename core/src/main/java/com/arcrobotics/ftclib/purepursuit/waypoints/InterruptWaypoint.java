package com.arcrobotics.ftclib.purepursuit.waypoints;

import com.arcrobotics.ftclib.geometry.Pose2d;
import com.arcrobotics.ftclib.geometry.Rotation2d;
import com.arcrobotics.ftclib.geometry.Translation2d;
import com.arcrobotics.ftclib.purepursuit.actions.InterruptAction;
import com.arcrobotics.ftclib.purepursuit.types.WaypointType;

/**
 * An InterruptWaypoint is a PointTurnWaypoint. After the robot stops at this waypoint, it will turn
 * to face this waypoint's preferred angle, perform the supplied action, then turn towards the next
 * waypoint and continue. This is useful for having the robot perform actions "mid path".
 *
 * @author Michael Baljet, Team 14470
 * @version 1.1
 */
public class InterruptWaypoint extends PointTurnWaypoint {

    // The action the robot performs.
    private InterruptAction action;

    // True if the robot has already performed the action, false otherwise.
    private boolean actionPerformed;

    /**
     * Constructs an InterruptWaypoint. All values are set to their default.
     */
    public InterruptWaypoint() {
        action = new InterruptAction() {
            @Override
            public void doAction() {
                // The default action is doing nothing.
            }
        };
        actionPerformed = false;
    }

    /**
     * Constructs an InterruptWaypoint with the provided values.
     *
     * @param translation    The (x, y) translation of this waypoint.
     * @param rotation       The rotation (preferred angle) of this waypoint.
     * @param movementSpeed  The speed in which the robot moves at while traversing this waypoint, in the range [0, 1].
     * @param turnSpeed      The speed in which the robot turns at while traversing this waypoint, in the range [0, 1].
     * @param followRadius   The distance in which the robot traverses this waypoint. Please see guides to learn more about this value.
     * @param positionBuffer The expected level of error, the robot will consider itself at the waypoint when it is within the buffer. The buffer must be > 0.
     * @param rotationBuffer The expected level of error (in radians), the robot will consider itself at the waypoint when it is within the buffer. The buffer must be > 0.
     * @param action         The action the robot performs at this point.
     */
    public InterruptWaypoint(Translation2d translation, Rotation2d rotation, double movementSpeed, double turnSpeed, double followRadius, double positionBuffer, double rotationBuffer, InterruptAction action) {
        super(translation, rotation, movementSpeed, turnSpeed, followRadius, positionBuffer, rotationBuffer);
        this.action = action;
        actionPerformed = false;
    }

    /**
     * Constructs an InterruptWaypoint with the provided values.
     *
     * @param pose           Position and rotation (preferred angle) of this waypoint.
     * @param movementSpeed  The speed in which the robot moves at while traversing this waypoint, in the range [0, 1].
     * @param turnSpeed      The speed in which the robot turns at while traversing this waypoint, in the range [0, 1].
     * @param followRadius   The distance in which the robot traverses this waypoint. Please see guides to learn more about this value.
     * @param positionBuffer The expected level of error, the robot will consider itself at the waypoint when it is within the buffer. The buffer must be > 0.
     * @param rotationBuffer The expected level of error (in radians), the robot will consider itself at the waypoint when it is within the buffer. The buffer must be > 0.
     * @param action         The action the robot performs at this point.
     */
    public InterruptWaypoint(Pose2d pose, double movementSpeed, double turnSpeed, double followRadius, double positionBuffer, double rotationBuffer, InterruptAction action) {
        super(pose, movementSpeed, turnSpeed, followRadius, positionBuffer, rotationBuffer);
        this.action = action;
        actionPerformed = false;
    }

    /**
     * Constructs an InterruptWaypoint with the provided values.
     *
     * @param x               The x position of this waypoint.
     * @param y               The y position of this waypoint.
     * @param rotationRadians The rotation (preferred angle) of this waypoint (in radians).
     * @param movementSpeed   The speed in which the robot moves at while traversing this waypoint, in the range [0, 1].
     * @param turnSpeed       The speed in which the robot turns at while traversing this waypoint, in the range [0, 1].
     * @param followRadius    The distance in which the robot traverses this waypoint. Please see guides to learn more about this value.
     * @param positionBuffer  The expected level of error, the robot will consider itself at the waypoint when it is within the buffer. The buffer must be > 0.
     * @param rotationBuffer  The expected level of error (in radians), the robot will consider itself at the waypoint when it is within the buffer. The buffer must be > 0.
     * @param action          The action the robot performs at this point.
     */
    public InterruptWaypoint(double x, double y, double rotationRadians, double movementSpeed, double turnSpeed, double followRadius, double positionBuffer, double rotationBuffer, InterruptAction action) {
        super(x, y, rotationRadians, movementSpeed, turnSpeed, followRadius, positionBuffer, rotationBuffer);
        this.action = action;
        actionPerformed = false;
    }

    /**
     * Constructs an InterruptWaypoint with the provided values.
     *
     * @param x              The x position of this waypoint.
     * @param y              The y position of this waypoint.
     * @param movementSpeed  The speed in which the robot moves at while traversing this waypoint, in the range [0, 1].
     * @param turnSpeed      The speed in which the robot turns at while traversing this waypoint, in the range [0, 1].
     * @param followRadius   The distance in which the robot traverses this waypoint. Please see guides to learn more about this value.
     * @param positionBuffer The expected level of error, the robot will consider itself at the waypoint when it is within the buffer. The buffer must be > 0.
     * @param rotationBuffer The expected level of error (in radians), the robot will consider itself at the waypoint when it is within the buffer. The buffer must be > 0.
     * @param action         The action the robot performs at this point.
     */
    public InterruptWaypoint(double x, double y, double movementSpeed, double turnSpeed, double followRadius, double positionBuffer, double rotationBuffer, InterruptAction action) {
        super(x, y, movementSpeed, turnSpeed, followRadius, positionBuffer, rotationBuffer);
        this.action = action;
        actionPerformed = false;
    }

    /**
     * Sets the action of this InterruptWaypoint.
     *
     * @param action Action to be set.
     * @return This InterruptWaypoint, used for chaining methods.
     */
    public InterruptWaypoint setAction(InterruptAction action) {
        this.action = action;
        return this;
    }

    /**
     * If the action has not already been performed, performs the action.
     */
    public void performAction() {
        if (!actionPerformed && action != null) {
            action.doAction();
            actionPerformed = true;
        }
    }

    /**
     * Returns true if the action has already been performed, false otherwise.
     *
     * @return true if the action has already been performed, false otherwise.
     */
    public boolean actionPerformed() {
        return actionPerformed;
    }

    @Override
    public void reset() {
        super.reset();
        actionPerformed = false;
    }

    @Override
    public WaypointType getType() {
        return WaypointType.INTERRUPT;
    }

    @Override
    public String toString() {
        return String.format("InterruptWaypoint(%s, %s)", getTranslation().getX(), getTranslation().getY());
    }

}
