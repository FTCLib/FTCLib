package com.arcrobotics.ftclib.hardware.motors;

import com.arcrobotics.ftclib.controller.PController;
import com.arcrobotics.ftclib.controller.PIDController;
import com.arcrobotics.ftclib.controller.PIDFController;
import com.arcrobotics.ftclib.controller.wpilibcontroller.SimpleMotorFeedforward;

/**
 * An extended motor class that utilizes more features than the
 * regular motor. This is basically a rewrite of DcMotor in the basic
 * SDK, but has more features and a more powerful PIDF control.
 *
 * In order to create an extended motor, you will need to have already
 * created either a custom motor class or use {@link SimpleMotor}.
 *
 * Please note that this is the <b>abstract class file</b>, so
 * the purpose of this class is for creating your own custom motor class.
 * If you want to use the simple one we have created for you, please use
 * {@link SimpleMotorEx}.
 *
 * @author Jackson
 */
public abstract class MotorEx implements Motor {

    /**
     * The PID controller for the extended motor.
     */
    protected PIDController velocityController;

    /**
     * The feed forward output for the motor.
     */
    protected SimpleMotorFeedforward motorFeedforward;
    
    /**
    * The P controller for position.
    */
    protected PController positionController;

    /**
     * The motor in question.
     */
    protected Motor motor;

    /**
     * The counts per revolution of the output shaft, usually listed in the specs for
     * the motor.
     */
    public final double COUNTS_PER_REV;

    public ZeroPowerBehavior zeroBehavior = ZeroPowerBehavior.UNKNOWN;

    /**
     * The behavior of the motor when a speed of 0 is passed.
     *
     * <p>
     *     BREAK causes the motor to run into itself and immediately sets the speed to 0.
     * </p>
     * <p>
     *     FLOAT causes the motor to continue to spin with slowly decreasing speed when 0 is passed.
     * </p>
     * <p>
     *     UNKNOWN is the default behavior, which causes the motor to decelerate quickly.
     * </p>
     */
    public enum ZeroPowerBehavior {
        BREAK, FLOAT, UNKNOWN
    }

    /**
     * The constructor for the motor without a PID controller.
     *
     * @param mot   The motor in question.
     * @param cpr   The counts per revolution of said motor.
     */
    public MotorEx(Motor mot, double cpr) {
        this(mot, cpr, new PIDController(new double[]{1,0,0}));
    }

    /**
     * The constructor for the extended motor that includes a {@link PIDFController}.
     *
     * @param mot           The motor in question.
     * @param cpr           The counts per revolution of said motor.
     * @param controller    The PID controller.
     */
    public MotorEx(Motor mot, double cpr, PIDController controller) {
        this(mot, cpr, controller, new PController(1));
    }
    
    /**
     * The constructor for the extended motor which includes a
     * {@link PIDController} and a {@link PController}
     */
   public MotorEx(Motor mot, double cpr,
                  PIDController veloController,
                  PController positionController) {

        this(mot, cpr, veloController, positionController,
                new SimpleMotorFeedforward(0,0));

    }

    /**
     * The constructor for the extended motor which includes a
     * {@link PIDController} and a {@link PController} along
     * with {@link SimpleMotorFeedforward}.
     */
    public MotorEx(Motor mot, double cpr,
                   PIDController veloController,
                   PController positionController,
                   SimpleMotorFeedforward feedforward) {

        motor = mot;
        COUNTS_PER_REV = cpr;

        velocityController = veloController;

        this.positionController = positionController;

        motorFeedforward = feedforward;

    }

    /**
     * The constructor for the extended motor which includes a
     * {@link PIDController} and {@link SimpleMotorFeedforward}.
     */
    public MotorEx(Motor mot, double cpr,
                   PIDController veloController,
                   SimpleMotorFeedforward feedforward) {

        this(mot, cpr, veloController, new PController(1), feedforward);

    }

    /**
     * Set the zero power behavior of the motor.
     *
     * @param behavior The MotorEx.ZeroPowerBehavior of the motor.
     */
    public void setZeroPowerBehavior(ZeroPowerBehavior behavior) {
        zeroBehavior = behavior;
    }

    /**
     * @return the current position of the motor in ticks
     */
    public abstract int getCurrentPosition();

    /**
     * Resets the controlelrs running the robot
     */
    public void resetControllers() {
        velocityController.reset();
        positionController.reset();
    }

    /**
     * Sets the speed of the motor. This method needs to be overridden in your
     * {@link Motor} object.
     *
     * @param speed The speed to set. Value should be between -1.0 and 1.0.
     */
    public void set(double speed) {
        motor.set(speed);

        if (Math.abs(speed) <= 10E-2) {
            if (zeroBehavior == ZeroPowerBehavior.FLOAT) {
                positionController.control(motor, 0, motor.get(), 0.5);
            } else if (zeroBehavior == ZeroPowerBehavior.BREAK) {
                motor.stopMotor();
            } else positionController.control(motor, 0, motor.get());
        }
    }

    /**
     * Disables the motor.
     */
    @Override
    public void disable() {
        motor.disable();
    }

    /**
     * Stops the motor.
     */
    @Override
    public void stopMotor() {
        motor.stopMotor();
    }

    @Override
    public double get() {
        return motor.get();
    }

    /**
     * This is the method that does the magic for the extended motor.
     * It uses a {@link PIDController} and {@link SimpleMotorFeedforward}.
     *
     * @param output the desired output from the motor, from -1.0 to 1.0
     */
    @Override
    public void pidWrite(double output) {
        motor.set(velocityController.calculate(get(), output) + motorFeedforward.calculate(output));
    }

    @Override
    public void setInverted(boolean isInverted) {
        motor.setInverted(isInverted);
    }

    @Override
    public boolean getInverted() {
        return motor.getInverted();
    }

    @Override
    public String getDeviceType() {
        return "Extended " + motor.getDeviceType();
    }

}