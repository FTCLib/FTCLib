package com.arcrobotics.ftclib.hardware;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

/**
 * An extended servo interface.
 */
public interface ServoEx extends HardwareDevice {

    /**
     * Rotates the servo by a certain angle.
     *
     * @param angle     The desired angle of rotation
     * @param angleUnit The unit of the angle parameter
     */
    void rotateByAngle(double angle, AngleUnit angleUnit);

    /**
     * Rotates the servo by a certain angle in degrees.
     *
     * @param degrees The desired angle of rotation in degrees
     */
    void rotateByAngle(double degrees);

    /**
     * Turns the servo position to a set angle.
     *
     * @param angle     The desired set position of the servo
     * @param angleUnit The unit of the angle parameter
     */
    void turnToAngle(double angle, AngleUnit angleUnit);

    /**
     * Turns the servo position to a set angle in degrees.
     *
     * @param degrees The desired set position of the servo in degrees
     */
    void turnToAngle(double degrees);

    /**
     * Rotates by a given positional factor.
     */
    void rotateBy(double position);

    /**
     * Sets the position of the servo to the specified location.
     *
     * @param position The location of the servo, which ranges from 0 to 1
     */
    void setPosition(double position);

    /**
     * Sets the range of the servo at specified angles.
     *
     * @param min       The minimum value. Setting the servo position to 0 will bring it
     *                  to this specified minimum.
     * @param max       The maximum value. Setting the servo position to 1 will bring it
     *                  to this specified maximum.
     * @param angleUnit The unit of the range parameters
     */
    void setRange(double min, double max, AngleUnit angleUnit);

    /**
     * Sets the range of the servo at specified angles in degrees.
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
     * @param angleUnit Angle unit for the result to be returned in
     * @return The angle the servo's current makes with the 0 position.
     */
    double getAngle(AngleUnit angleUnit);

    /**
     * @return The angle the servo's current makes with the 0 position, in degrees.
     */
    double getAngle();

}
