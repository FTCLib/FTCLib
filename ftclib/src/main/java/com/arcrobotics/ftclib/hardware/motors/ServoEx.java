package com.arcrobotics.ftclib.hardware.motors;

public interface ServoEx {
    void rotateDegrees(double degrees);
    void turnToAngle(double angle);
    void rotate(double position);
    void setPosition(double position);
    void setRange(double min, double max);
    void setInverted(boolean isInverted);
    boolean getInverted();
    double getPosition();
    double getAngle();
}
