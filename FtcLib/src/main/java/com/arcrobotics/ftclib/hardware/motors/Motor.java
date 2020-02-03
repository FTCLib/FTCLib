package com.arcrobotics.ftclib.hardware.motors;

import com.arcrobotics.ftclib.hardware.HardwareDevice;

/**
 * Interface for motor devices. Create your own custom motors
 * through the following tutorial:
 *
 * <ol>
 *     <li>
 *         Create a classfile that implements the Motor interface. Name it something
 *         that represents the type of motor you are using. For instance, if you are using a
 *         435 rpm goBILDA Yellow Jacket, then you can name the class: YellowJacket435.
 *         This can be done through using: {@code public class YellowJacket435 implements Motor}.
 *     </li>
 *     <li>
 *         Add the motor object as an instance variable. For the regular FTC SDK, this would be
 *         DcMotor from {@code com.qualcomm.robotcore.hardware.DcMotor}. If you are using re2 from
 *         OpenFTC, then you can use something like ExpansionHubMotor
 *         {@code org.openftc.revextensions2.ExpansionHubMotor}. e.g.
 *         {@code private DcMotor motor}.
 *     </li>
 *     <li>
 *          Create the constructor so that it takes in the same type of motor
 *          as the object you declared as the parameter. This will have the instance
 *          variable reference that object when methods are called.
 *     </li>
 *     <li>
 *         Implement the methods and create your own if you would like!
 *     </li>
 * </ol>
 */
public interface Motor extends HardwareDevice {

    /**
     * Common interface for setting the speed of a motor. If motor is inverted
     * {@link #getInverted()}, multiply the speed by -1.
     *
     * @param speed The speed to set. Value should be between -1.0 and 1.0.
     */
    void set(double speed);

    /**
     * Common interface for getting the current set speed of a motor.
     *
     * @return The current set speed. Value is between -1.0 and 1.0.
     */
    double get();

    /**
     * Common interface for inverting direction of a motor.
     *
     * @param isInverted The state of inversion true is inverted.
     */
    void setInverted(boolean isInverted);

    /**
     * Common interface for returning if a motor is in the inverted state or not.
     *
     * @return isInverted The state of the inversion true is inverted.
     */
    boolean getInverted();

    /**
     * Disable the motor.
     */
    @Override
    void disable();

    /**
     * Set power to the given output that has been calculated using pid calculations.
     */
    void pidWrite(double output);

    /**
     * Stops motor movement. Motor can be moved again by calling set without having to re-enable the
     * motor.
     */
    void stopMotor();

}
