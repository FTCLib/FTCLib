package com.arcrobotics.ftclib.hardware;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

/**
 * An extended servo interface.
 */
public interface ServoEx extends HardwareDevice {

    /**
     * Rotates the servo a certain number of degrees.
     *
     * @param degrees The desired degrees of rotation
     */
    void rotateAngle(double angle);

    /**
     * Turns the servo position to a set angle.
     *
     * @param angle The desired set position of the servo
     */
    void turnToAngle(double angle);

    /**
     * Rotates by a given positional factor.
     */
    void rotate(double position);

    /**
     * Sets the position of the servo to the specified location.
     *
     * @param position The location of the servo, which ranges from 0 to 1
     */
    void setPosition(double position);

    /**
     * Sets the angle unit to be used in all of the methods of this servo.
     *
     * @param angleUnit The angle unit, either RADIANS or DEGREES
     */
    void setAngleUnit(AngleUnit angleUnit);

    /**
     * Sets the range of the servo.
     *
     * @param min The minimum value. Setting the servo position to 0 will bring it
     *            to this specified minimum.
     * @param max The maximum value. Setting the servo position to 1 will bring it
     *            to this specified maximum.
     */
    void setRange(double min, double max);

    /**
     * Sets the inversion factor of the servo.
     *
     * <p>By default, the inversion is false.</p>
     *
     * @param isInverted the desired inversion factor
     */
    void setInverted(boolean isInverted);

    /**
     * @return true if the servo is inverted, false otherwise
     */
    boolean getInverted();

    /**
     * @return The current position of the servo from 0 to 1.
     */
    double getPosition();

    /**
     * @return The angle the servo's current makes with the 0 position.
     */
    double getAngle();
}
