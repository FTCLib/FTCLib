package com.arcrobotics.ftclib.hardware.motors;

import com.arcrobotics.ftclib.controller.PController;
import com.arcrobotics.ftclib.controller.PIDFController;

/**
 * An extended motor class that utilizes more features than the
 * regular motor. This is basically a rewrite of DcMotor in the basic
 * SDK, but has more features and a more powerful PIDF control.
 */
public abstract class MotorEx implements Motor {

    /**
     * The PIDF controller for the extended motor.
     */
    private PIDFController pidfController;
    
    /**
    * Hotfix! Very bad but works
    *
    * The P controller for position
    */
    private PController pController;
    /**
     * The motor in question.
     */
    private Motor motor;

    /**
     * The target position of the motor.
     *
     * <p>
     *     This only works if the motor is in RUN_TO_POSITION.
     * </p>
     */
    private double targetPos;

    /**
     * The counts per revolution of the output shaft, usually listed in the specs for
     * the motor.
     */
    public final double COUNTS_PER_REV;

    public String runMode = "rwe";
    public String zeroBehavior = "null";

    /**
     * The internal encoder for the motor.
     */
    public EncoderEx encoder;

    /**
     * The mode of the motor.
     *
     * <p>
     *     RUN_TO_POSITION uses a P controller to bring the motor shaft to a
     *     designated target.
     * </p>
     * <p>
     *     RUN_USING_ENCODER uses the encoder to have the motor accelerate to the
     *     chosen output speed using a PIDF controller.
     * </p>
     * <p>
     *     RUN_WITHOUT_ENCODER is the default run mode, which causes the motor to run
     *     like a normal {@link Motor}.
     * </p>
     * <p>
     *     STOP_AND_RESET_ENCODER stops the motor and resets the encoder value to 0.
     * </p>
     */
    public enum RunMode {
        RUN_TO_POSITION("rtp"), RUN_USING_ENCODER("rue"), RUN_WITHOUT_ENCODER("rwe"),
        STOP_AND_RESET_ENCODER("sare");

        private final String mode;

        RunMode(String mode) {
            this.mode = mode;
        }

        public String getMode() {
            return mode;
        }
    }

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
        BREAK("break"), FLOAT("float"), UNKNOWN("null");

        private final String behavior;

        ZeroPowerBehavior(String behavior) {
            this.behavior = behavior;
        }

        public String getBehavior() {
            return behavior;
        }
    }

    /**
     * The constructor for the motor without a PIDF controller.
     *
     * @param mot   The motor in question.
     * @param cpr   The counts per revolution of said motor.
     */
    public MotorEx(Motor mot, double cpr) {
        this(mot, cpr, new PIDFController(new double[]{1,0,0,0}));
    }

    /**
     * The constructor for the extended motor that includes a {@link PIDFController}.
     *
     * @param mot           The motor in question.
     * @param cpr           The counts per revolution of said motor.
     * @param controller    The PIDF controller.
     */
    public MotorEx(Motor mot, double cpr, PIDFController controller) {
        this(mot, cpr, controller, new PController(1));
    }
    
    /**
     * The constructor for the extended motor which includes a
     * {@link PIDFController} and a {@link PController}
     */
   public MotorEx(Motor mot, double cpr, PIDFController veloController, PController positionController) {
        motor = mot;
        COUNTS_PER_REV = cpr;

        pidfController = veloController;
       
        //HOTFIX Stores the P value
        pController = positionController;

        encoder = new EncoderEx(this);
    }
    
    
    /**
     * @return The current tick count of the output shaft.
     */
    abstract double getCurrentPosition();

    /**
     * Set the run mode of the motor.
     *
     * @param mode  The MotorEx.RunMode of the motor.
     */
    public void setMode(RunMode mode) {
        runMode = mode.getMode();
        if (runMode.equals("sare")) {
            encoder.stopAndReset();
        }
    }

    /**
     * Set the zero power behavior of the motor.
     *
     * @param behavior The MotorEx.ZeroPowerBehavior of the motor.
     */
    public void setZeroPowerBehavior(ZeroPowerBehavior behavior) {
        zeroBehavior = behavior.getBehavior();
    }

    /**
     * Sets the target position of the motor.
     *
     * <p>If the motor is not in RUN_TO_POSITION, this will throw a {@code NullPointerException}.</p>
     *
     * @param target The specified target of the motor.
     */
    public void setTargetPosition(double target) {
        if (runMode.equals("rtp")) {
            targetPos = target;
        } else throw null;
    }

    /**
     * @return The target position of the motor.
     */
    public double getTargetPosition() {
        return (targetPos != 0) ? targetPos : null;
    }

    public void resetController() {
        pidfController.reset();
        pController.reset();
    }

    /**
     * Sets the speed of the motor based on the current run mode.
     *
     * @param speed The speed to set. Value should be between -1.0 and 1.0.
     * 
     * HOTFIX: AHHH
     */
    public void set(double speed) {
        if (runMode.equals("rtp")) {
            pController.pControl(motor, targetPos, encoder.getCurrentTicks(), speed);
        } else if (runMode.equals("rue")) {
            motor.set(motor.get() + pidfController.calculate(speed, motor.get()));
        } else if (runMode.equals("sare")) {
            throw null;
        } else {
            motor.set(speed);
        }

        if (speed == 0) {
            if (zeroBehavior.equals("float")) {
                pController.pControl(motor, 0, motor.get(), 0.5);
            } else if (zeroBehavior.equals("break")) {
                motor.set(0);
            } else pController.pControl(motor, 0, motor.get());
        }
    }

    /**
     * Stops the motor, of course.
     */
    public void stopMotor() {
        set(0);
    }

    /**
     * Resets the encoder value to 0 without stopping the motor, of course.
     */
    public void resetEncoder() {
        encoder.resetEncoderCount();
    }

}
