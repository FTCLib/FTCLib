package com.arcrobotics.ftclib.purepursuit.waypoints;

import com.arcrobotics.ftclib.geometry.Pose2d;
import com.arcrobotics.ftclib.geometry.Rotation2d;
import com.arcrobotics.ftclib.geometry.Translation2d;
import com.arcrobotics.ftclib.purepursuit.types.WaypointType;

/**
 * A point turn waypoint is a special type of waypoint where instead of "curving" around it, the
 * robot will travel to it, make a complete stop, turn towards the next waypoint, and continue.
 *
 * @author Michael Baljet, Team 14470
 * @version 1.1
 */
public class PointTurnWaypoint extends GeneralWaypoint {

    // The expected level of error, the robot will consider itself at the waypoint when it is within the buffer. The buffers must be > 0.
    private double positionBuffer;
    private double rotationBuffer;

    // True if the robot has already "passed" this waypoint.
    private boolean traversed;

    /**
     * Constructs a PointTurnWaypoint. All values are set to their default.
     */
    public PointTurnWaypoint() {
        positionBuffer = 0;
        rotationBuffer = 0;
        traversed = false;
    }

    /**
     * Constructs a PointTurnWaypoint with the provided values.
     *
     * @param translation    The (x, y) translation of this waypoint.
     * @param rotation       The rotation (preferred angle) of this waypoint.
     * @param movementSpeed  The speed in which the robot moves at while traversing this waypoint, in the range [0, 1].
     * @param turnSpeed      The speed in which the robot turns at while traversing this waypoint, in the range [0, 1].
     * @param followRadius   The distance in which the robot traverses this waypoint. Please see guides to learn more about this value.
     * @param positionBuffer The expected level of error, the robot will consider itself at the waypoint when it is within the buffer. The buffer must be > 0.
     * @param rotationBuffer The expected level of error (in radians), the robot will consider itself at the waypoint when it is within the buffer. The buffer must be > 0.
     */
    public PointTurnWaypoint(Translation2d translation, Rotation2d rotation, double movementSpeed, double turnSpeed, double followRadius, double positionBuffer, double rotationBuffer) {
        super(translation, rotation, movementSpeed, turnSpeed, followRadius);
        this.positionBuffer = verifyBuffer(positionBuffer);
        this.rotationBuffer = verifyBuffer(rotationBuffer);
        traversed = false;
    }

    /**
     * Constructs a PointTurnWaypoint with the provided values.
     *
     * @param pose           Position and rotation (preferred angle) of this waypoint.
     * @param movementSpeed  The speed in which the robot moves at while traversing this waypoint, in the range [0, 1].
     * @param turnSpeed      The speed in which the robot turns at while traversing this waypoint, in the range [0, 1].
     * @param followRadius   The distance in which the robot traverses this waypoint. Please see guides to learn more about this value.
     * @param positionBuffer The expected level of error, the robot will consider itself at the waypoint when it is within the buffer. The buffer must be > 0.
     * @param rotationBuffer The expected level of error (in radians), the robot will consider itself at the waypoint when it is within the buffer. The buffer must be > 0.
     */
    public PointTurnWaypoint(Pose2d pose, double movementSpeed, double turnSpeed, double followRadius, double positionBuffer, double rotationBuffer) {
        super(pose, movementSpeed, turnSpeed, followRadius);
        this.positionBuffer = verifyBuffer(positionBuffer);
        this.rotationBuffer = verifyBuffer(rotationBuffer);
        traversed = false;
    }

    /**
     * Constructs a PointTurnWaypoint with the provided values.
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
    public PointTurnWaypoint(double x, double y, double rotationRadians, double movementSpeed, double turnSpeed, double followRadius, double positionBuffer, double rotationBuffer) {
        super(x, y, rotationRadians, movementSpeed, turnSpeed, followRadius);
        this.positionBuffer = verifyBuffer(positionBuffer);
        this.rotationBuffer = verifyBuffer(rotationBuffer);
        traversed = false;
    }

    /**
     * Constructs a PointTurnWaypoint with the provided values.
     *
     * @param x              The x position of this waypoint.
     * @param y              The y position of this waypoint.
     * @param movementSpeed  The speed in which the robot moves at while traversing this waypoint, in the range [0, 1].
     * @param turnSpeed      The speed in which the robot turns at while traversing this waypoint, in the range [0, 1].
     * @param followRadius   The distance in which the robot traverses this waypoint. Please see guides to learn more about this value.
     * @param positionBuffer The expected level of error, the robot will consider itself at the waypoint when it is within the buffer. The buffer must be > 0.
     * @param rotationBuffer The expected level of error (in radians), the robot will consider itself at the waypoint when it is within the buffer. The buffer must be > 0.
     */
    public PointTurnWaypoint(double x, double y, double movementSpeed, double turnSpeed, double followRadius, double positionBuffer, double rotationBuffer) {
        super(x, y, movementSpeed, turnSpeed, followRadius);
        this.positionBuffer = verifyBuffer(positionBuffer);
        this.rotationBuffer = verifyBuffer(rotationBuffer);
        traversed = false;
    }

    /**
     * Returns this waypoint's position buffer.
     *
     * @return this waypoint's position buffer.
     */
    public double getPositionBuffer() {
        return positionBuffer;
    }

    /**
     * Returns this waypoint's rotation buffer.
     *
     * @return this waypoint's rotation buffer.
     */
    public double getRotationBuffer() {
        return rotationBuffer;
    }

    /**
     * Sets this waypoint's position buffer.
     *
     * @param buffer Position buffer to be set.
     * @return This PointTurnWaypoint, used for chaining methods.
     */
    public PointTurnWaypoint setPositionBuffer(double buffer) {
        positionBuffer = verifyBuffer(buffer);
        return this;
    }

    /**
     * Sets this waypoint's rotation buffer.
     *
     * @param buffer Rotation buffer to be set.
     * @return This PointTurnWaypoint, used for chaining methods.
     */
    public PointTurnWaypoint setRotationBuffer(double buffer) {
        rotationBuffer = verifyBuffer(buffer);
        return this;
    }

    /**
     * Returns true if this waypoint has already been traversed, false otherwise.
     *
     * @return true if this waypoint has already been traversed, false otherwise.
     */
    public boolean hasTraversed() {
        return traversed;
    }

    /**
     * Tells the waypoint that it has been traversed.
     */
    public void setTraversed() {
        traversed = true;
    }

    /**
     * Verified the buffer it valid. The buffer is valid if it is >1.
     *
     * @param buffer Buffer to be checked.
     * @return True if the buffer is valid, false otherwise.
     */
    private double verifyBuffer(double buffer) {
        if (buffer <= 0)
            throw new IllegalArgumentException("The buffer must be > 0");
        return buffer;
    }

    @Override
    public void reset() {
        traversed = false;
    }

    @Override
    public WaypointType getType() {
        return WaypointType.POINT_TURN;
    }

    @Override
    public String toString() {
        return String.format("PointTurnWaypoint(%s, %s)", getTranslation().getX(), getTranslation().getY());
    }

}
