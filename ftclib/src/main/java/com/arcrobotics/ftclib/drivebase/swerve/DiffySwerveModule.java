package com.arcrobotics.ftclib.drivebase.swerve;

import com.arcrobotics.ftclib.geometry.Vector2d;
import com.arcrobotics.ftclib.hardware.motors.Motor;

/**
 * This is a differential swerve module.
 * There are two motors that act together to produce either
 * a rotation or a translation within the module.
 */
public class DiffySwerveModule {

    protected Motor m_motorOne, m_motorTwo;

    public final Vector2d MOTOR_1_POWER = new Vector2d(1/Math.sqrt(2), 1/Math.sqrt(2));
    public final Vector2d MOTOR_2_POWER = new Vector2d(-1/Math.sqrt(2),1/Math.sqrt(2));

    /**
     * Make sure the motors are already reversed if needed
     *
     * @param one   the first motor
     * @param two   the second motor
     */
    public DiffySwerveModule(Motor one, Motor two) {
        m_motorOne = one;
        m_motorTwo = two;
    }

    /**
     * If both motors in the module spin in the same direction
     * with the same power, this will result in strict module rotation.
     * Powering the motors in opposite directions with the same power
     * results in a strict translation. Consider the fact that spinning
     * one motor with a power -0.5 and another with 0.5 power results in
     * a strict translation with 50% speed, while setting speeds to 0 and 1
     * will result in 50% rotation and 50% translation.
     *
     * This is some tricky math. Let's consider the fact that we can never have
     * 100% rotation and 100% translation. Let's break this relationship down into
     * our old friend, the unit circle. Let's represent the y-axis as the rotational power
     * and the translational power on the x-axis. The power of each motor is represented with a
     * vector given two components: translational speed and rotational speed.
     *
     * @param speed1  the speed of the first motor
     * @param speed2  the speed of the second motor
     */
    public void driveModule(double speed1, double speed2) {
        m_motorOne.set(speed1);
        m_motorTwo.set(speed2);
    }

    public void driveModule(Vector2d powerVec) {
        Vector2d motorOneUnscaled = powerVec.project(MOTOR_1_POWER);
        Vector2d motorTwoUnscaled = powerVec.project(MOTOR_2_POWER);

        Vector2d[] motorPowersScaled = new Vector2d[]{motorOneUnscaled, motorTwoUnscaled};
        double motorOnePower =
                motorPowersScaled[0].angle() != MOTOR_1_POWER.angle() ? -motorPowersScaled[0].magnitude()
                        : motorPowersScaled[0].magnitude();
        double motorTwoPower =
                motorPowersScaled[1].angle() != MOTOR_2_POWER.angle() ? -motorPowersScaled[1].magnitude()
                        : motorPowersScaled[1].magnitude();

        double[] newSpeed = normalize(new double[]{motorOnePower, motorTwoPower});

        driveModule(newSpeed[0], newSpeed[1]);
    }

    /**
     * Normalize the wheel speeds if any value is greater than 1
     */
    protected double[] normalize(double[] wheelSpeeds) {
        double maxMagnitude = Math.abs(wheelSpeeds[0]);
        for (int i = 1; i < wheelSpeeds.length; i++) {
            double temp = Math.abs(wheelSpeeds[i]);
            if (maxMagnitude < temp) {
                maxMagnitude = temp;
            }
        }
        if (maxMagnitude > 1.0) {
            for (int i = 0; i < wheelSpeeds.length; i++) {
                wheelSpeeds[i] = wheelSpeeds[i] / maxMagnitude;
            }
        }

        return wheelSpeeds;
    }

    /**
     * Disables the module.
     */
    public void disable() {
        m_motorOne.disable();
        m_motorTwo.disable();
    }

    /**
     * Stops the motion of the module
     */
    public void stopMotor() {
        m_motorTwo.stopMotor();
        m_motorOne.stopMotor();
    }

    /**
     * Reverses the module
     */
    public void setInverted(boolean isInverted) {
        m_motorOne.setInverted(isInverted);
        m_motorTwo.setInverted(isInverted);
    }

}
