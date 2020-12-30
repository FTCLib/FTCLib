package com.arcrobotics.ftclib.purepursuit.waypoints;

import com.arcrobotics.ftclib.geometry.Pose2d;
import com.arcrobotics.ftclib.geometry.Rotation2d;
import com.arcrobotics.ftclib.geometry.Translation2d;
import com.arcrobotics.ftclib.purepursuit.Waypoint;
import com.arcrobotics.ftclib.purepursuit.types.WaypointType;

/**
 * A general waypoint is the most common type of Waypoint. This waypoint acts like a
 * conventional pure pursuit waypoint, with the robot simply traversing it. Most other
 * types of waypoints are sub classes of this.
 *
 * @author Michael Baljet, Team 14470
 * @version 1.1
 * @see Waypoint
 */
public class GeneralWaypoint extends Pose2d implements Waypoint {

    // If the robot moves towards this waypoint for longer than the timeout period, the path is aborted.
    private long timeoutMiliseconds;

    // Speed at which the robot moves, within the range [0, 1].
    private double movementSpeed;

    // Speed at which the robot turns, within the range [0, 1].
    private double turnSpeed;

    // The pure pursuit follow radius.
    private double followRadius;

    // True if this waypoint uses a preferred angle.
    private boolean usePreferredAngle;
    private double preferredAngle;

    // True if this waypoint is to inherit the previous node's configuration.
    private boolean copyMode;

    /**
     * Constructs a GeneralWaypoint. All values are set to their default.
     */
    public GeneralWaypoint() {
        // Set values to default.
        movementSpeed = 0;
        turnSpeed = 0;
        followRadius = 0;
        timeoutMiliseconds = -1;
        usePreferredAngle = false;
        copyMode = false;
    }

    public GeneralWaypoint(double x, double y) {
        this(x, y, 0, 0, 0, 0);
        copyMode = true;
    }

    /**
     * Constructs a GeneralWaypoint with the provided values.
     *
     * @param translation   The (x, y) translation of this waypoint.
     * @param rotation      The rotation (preferred angle) of this waypoint.
     * @param movementSpeed The speed in which the robot moves at while traversing this waypoint, in the range [0, 1].
     * @param turnSpeed     The speed in which the robot turns at while traversing this waypoint, in the range [0, 1].
     * @param followRadius  The distance in which the robot traverses this waypoint. Please see guides to learn more about this value.
     */
    public GeneralWaypoint(Translation2d translation, Rotation2d rotation, double movementSpeed, double turnSpeed, double followRadius) {
        super(translation, rotation);
        this.movementSpeed = normalizeSpeed(movementSpeed);
        this.turnSpeed = normalizeSpeed(turnSpeed);
        this.followRadius = followRadius;
        timeoutMiliseconds = -1;
        usePreferredAngle = true;
        preferredAngle = rotation.getRadians();
        copyMode = false;
    }

    /**
     * Constructs a GeneralWaypoint with the provided values.
     *
     * @param pose          Position and rotation (preferred angle) of this waypoint.
     * @param movementSpeed The speed in which the robot moves at while traversing this waypoint, in the range [0, 1].
     * @param turnSpeed     The speed in which the robot turns at while traversing this waypoint, in the range [0, 1].
     * @param followRadius  The distance in which the robot traverses this waypoint. Please see guides to learn more about this value.
     */
    public GeneralWaypoint(Pose2d pose, double movementSpeed, double turnSpeed, double followRadius) {
        super(pose.getTranslation(), pose.getRotation());
        this.movementSpeed = normalizeSpeed(movementSpeed);
        this.turnSpeed = normalizeSpeed(turnSpeed);
        this.followRadius = followRadius;
        timeoutMiliseconds = -1;
        usePreferredAngle = false;
        preferredAngle = 0;
        copyMode = false;
    }

    /**
     * Constructs a GeneralWaypoint with the provided values.
     *
     * @param x             The x position of this waypoint.
     * @param y             The y position of this waypoint.
     * @param movementSpeed The speed in which the robot moves at while traversing this waypoint, in the range [0, 1].
     * @param turnSpeed     The speed in which the robot turns at while traversing this waypoint, in the range [0, 1].
     * @param followRadius  The distance in which the robot traverses this waypoint. Please see guides to learn more about this value.
     */
    public GeneralWaypoint(double x, double y, double movementSpeed, double turnSpeed, double followRadius) {
        super(x, y, new Rotation2d(0));
        this.movementSpeed = normalizeSpeed(movementSpeed);
        this.turnSpeed = normalizeSpeed(turnSpeed);
        this.followRadius = followRadius;
        timeoutMiliseconds = -1;
        usePreferredAngle = false;
        preferredAngle = 0;
        copyMode = false;
    }

    /**
     * Constructs a GeneralWaypoint with the provided values.
     *
     * @param x               The x position of this waypoint.
     * @param y               The y position of this waypoint.
     * @param rotationRadians The rotation (preferred angle) of this waypoint (in radians).
     * @param movementSpeed   The speed in which the robot moves at while traversing this waypoint, in the range [0, 1].
     * @param turnSpeed       The speed in which the robot turns at while traversing this waypoint, in the range [0, 1].
     * @param followRadius    The distance in which the robot traverses this waypoint. Please see guides to learn more about this value.
     */
    public GeneralWaypoint(double x, double y, double rotationRadians, double movementSpeed, double turnSpeed, double followRadius) {
        super(x, y, new Rotation2d(rotationRadians));
        this.movementSpeed = normalizeSpeed(movementSpeed);
        this.turnSpeed = normalizeSpeed(turnSpeed);
        this.followRadius = followRadius;
        timeoutMiliseconds = -1;
        usePreferredAngle = true;
        preferredAngle = rotationRadians;
        copyMode = false;
    }

    /**
     * Returns the movement speed of this waypoint.
     *
     * @return the movement speed of this waypoint.
     */
    public double getMovementSpeed() {
        return movementSpeed;
    }

    /**
     * Returns the turn speed of this waypoint.
     *
     * @return the turn speed of this waypoint.
     */
    public double getTurnSpeed() {
        return turnSpeed;
    }

    /**
     * Returns the follow radius of this waypoint.
     *
     * @return the follow radius of this waypoint.
     */
    public double getFollowRadius() {
        return followRadius;
    }

    /**
     * Returns this waypoint's preferred angle (in radians).
     *
     * @return this waypoint's preferred angle (in radians).
     * @throws IllegalStateException If this waypoint is not using a preferred angle.
     */
    public double getPreferredAngle() {
        if (!usePreferredAngle)
            throw new IllegalStateException("This waypoint is not using a preferredAngle");
        return preferredAngle;
    }

    /**
     * Returns true if this waypoint is using a preferred angle.
     *
     * @return true if this waypoint is using a preferred angle, false otherwise.
     */
    public boolean usingPreferredAngle() {
        return usePreferredAngle;
    }

    /**
     * Sets the movement speed of this waypoint.
     *
     * @param movementSpeed Speed to set.
     * @return This GeneralWaypoint, used for chaining methods.
     */
    public GeneralWaypoint setMovementSpeed(double movementSpeed) {
        this.movementSpeed = movementSpeed;
        return this;
    }

    /**
     * Sets the turn speed of this waypoint.
     *
     * @param turnSpeed Speed to be set.
     * @return This GeneralWaypoint, used for chaining methods.
     */
    public GeneralWaypoint setTurnSpeed(double turnSpeed) {
        this.turnSpeed = turnSpeed;
        return this;
    }

    /**
     * Sets the follow radius of this waypoint.
     *
     * @param followRadius Radius to be set.
     * @return This GeneralWaypoint, used for chaining methods.
     */
    public GeneralWaypoint setFollowRadius(double followRadius) {
        this.followRadius = followRadius;
        return this;
    }

    /**
     * Sets and enables this waypoint's preferred angle.
     *
     * @param angle Angle to be set.
     * @return This GeneralWaypoint, used for chaining methods.
     */
    public GeneralWaypoint setPreferredAngle(double angle) {
        usePreferredAngle = true;
        preferredAngle = angle;
        return this;
    }

    /**
     * Sets the timeout period of this waypoint. This is optional.
     *
     * @param timeoutMiliseconds The timeout period of this waypoint.
     * @return This GeneralWaypoint, used for chaining methods.
     */
    public GeneralWaypoint setTimeout(long timeoutMiliseconds) {
        this.timeoutMiliseconds = timeoutMiliseconds;
        return this;
    }

    /**
     * Disables this waypoint's preferredAngle. This is disabled by default.
     *
     * @return This GeneralWaypoint, used for chaining methods.
     */
    public GeneralWaypoint disablePreferredAngle() {
        usePreferredAngle = false;
        preferredAngle = 0;
        return this;
    }

    /**
     * Resets this waypoint. This is called by Path.
     */
    public void reset() {
        // GeneralWaypoints don't have anything to reset.
    }

    /**
     * Normalizes the given double to in the range [0, 1].
     *
     * @param raw Value to be normalized.
     * @return Normalized value.
     */
    protected double normalizeSpeed(double raw) {
        if (raw > 1)
            return 1;
        if (raw < 0)
            return 0;
        return raw;
    }

    /**
     * Copies configuration from the given waypoint. Does nothing if copy mode is disabled.
     *
     * @param waypoint Waypoint to copy.
     */
    public void inherit(Waypoint waypoint) {
        if (!copyMode)
            return;
        if (!(waypoint instanceof GeneralWaypoint))
            throw new IllegalArgumentException("A " + getType() + " waypoint cannot inherit the configuration of a " + waypoint.getType() + " waypoint.");
        GeneralWaypoint w = (GeneralWaypoint) waypoint;
        setMovementSpeed(w.getMovementSpeed());
        setTurnSpeed(w.getTurnSpeed());
        setFollowRadius(w.getFollowRadius());
        setTimeout(w.getTimeout());
        if (w.usingPreferredAngle())
            setPreferredAngle(w.getPreferredAngle());
        else
            usePreferredAngle = false;
    }

    @Override
    public WaypointType getType() {
        return WaypointType.GENERAL;
    }

    @Override
    public Pose2d getPose() {
        return this;
    }

    @Override
    public double getFollowDistance() {
        return followRadius;
    }

    @Override
    public long getTimeout() {
        return timeoutMiliseconds;
    }

    @Override
    public String toString() {
        return String.format("GeneralWaypoint(%s, %s)", getTranslation().getX(), getTranslation().getY());
    }

}
