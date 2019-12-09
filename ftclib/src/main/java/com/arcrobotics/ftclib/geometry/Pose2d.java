package com.arcrobotics.ftclib.geometry;

public class Pose2d {

    private final Vector2d m_pos;
    private final double m_heading;

    public Pose2d() {
        this(0, 0, 0);
    }

    public Pose2d(double x, double y, double heading) {
        this(new Vector2d(x, y), heading);
    }

    public Pose2d(Vector2d pos, double heading) {
        m_pos = pos;
        m_heading = heading;
    }

    public Pose2d(Pose2d other) {
        m_pos = other.getPos();
        m_heading = other.getHeading();
    }

    public Vector2d getPos() {
        return m_pos;
    }

    public double getHeading() {
        return m_heading;
    }

    public Pose2d unaryMinus() { return new Pose2d(m_pos.unaryMinus(), -m_heading); }

}