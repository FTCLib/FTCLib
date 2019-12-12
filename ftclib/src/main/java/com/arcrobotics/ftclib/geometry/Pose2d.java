package com.arcrobotics.ftclib.geometry;

/**
 * A two-dimensional position that includes a {@link Vector2d} and
 * a heading angle. Look at {@link Vector2d} for information on what
 * some of these methods do.
 */
public class Pose2d {

    private final Vector2d m_pos;
    private final double m_heading;

    /**
     * Default constructor, no params.
     * Initializes to x, y, and heading components of 0
     */
    public Pose2d() {
        this(0, 0, 0);
    }

    /**
     * The constructor that sets up the values for the 2d position.
     *
     * @param x         the x value of the vector
     * @param y         the y value of the vector
     * @param heading   the heading of the position
     */
    public Pose2d(double x, double y, double heading) {
        this(new Vector2d(x, y), heading);
    }

    /**
     * The constructor that sets up a position vector and heading.
     *
     * @param pos       A 2d vector that contains the x and y values.
     * @param heading   The heading of the 2d position.
     */
    public Pose2d(Vector2d pos, double heading) {
        m_pos = pos;
        m_heading = heading;
    }

    /**
     * Sets up a 2d position with a 2d vector without an instantiated heading.
     * Initializes the heading to 0.
     *
     * @param pos   The 2d vector that represents the Cartesian position.
     */
    public Pose2d(Vector2d pos) {
        this(pos, 0);
    }

    public Pose2d(Pose2d other) {
        m_pos = other.getPos();
        m_heading = other.getHeading();
    }

    /**
     * @return The Cartesian vector of the 2d position.
     */
    public Vector2d getPos() {
        return m_pos;
    }

    /**
     * @return The heading of the 2d position.
     */
    public double getHeading() {
        return m_heading;
    }

    public Pose2d unaryMinus() { return new Pose2d(m_pos.unaryMinus(), -m_heading); }


    public Pose2d plus(Pose2d other) {
        return new Pose2d(m_pos.plus(other.getPos()), m_heading + other.getHeading());
    }

    public Pose2d minus(Pose2d other) {
        return plus(other.unaryMinus());
    }

    public Pose2d times(double scalar) {
        return new Pose2d(m_pos.times(scalar), m_heading * scalar);
    }

    public Pose2d div(double scalar) {
        return times(1 / scalar);
    }

    public Pose2d rotate(double deltaTheta) {
        return new Pose2d(m_pos, m_heading + deltaTheta);
    }

    /**
     * Checks equality between this Pose2d and another object
     *
     * @param obj the other object
     * @return whether the two objects are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Pose2d) {
            return m_pos.equals(((Pose2d) obj).getPos()) &&
                    Math.abs(((Pose2d) obj).getHeading() - m_heading) < 1E-9;
        }
        return false;
    }
}