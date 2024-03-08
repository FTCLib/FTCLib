package com.arcrobotics.ftclib.hardware.motors;

import androidx.annotation.NonNull;

import com.arcrobotics.ftclib.math.controller.PController;
import com.arcrobotics.ftclib.math.controller.PIDController;
import com.arcrobotics.ftclib.math.controller.wpilibcontroller.SimpleMotorFeedforward;
import com.arcrobotics.ftclib.hardware.HardwareDevice;
import com.arcrobotics.ftclib.util.MathUtils;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.configuration.typecontainers.MotorConfigurationType;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * This is the common wrapper for the {@link DcMotor} object in the
 * FTC SDK.
 *
 * @author Jackson
 */
public class Motor implements HardwareDevice {

    public enum GoBILDA {
        RPM_30(5281.1, 30), RPM_43(3895.9, 43), RPM_60(2786.2, 60), RPM_84(1993.6, 84),
        RPM_117(1425.1, 117), RPM_223(751.8, 223), RPM_312(537.7, 312), RPM_435(384.5, 435),
        RPM_1150(145.1, 1150), RPM_1620(103.8, 1620), BARE(28, 6000), NONE(0, 0);

        private final double cpr, rpm;

        GoBILDA(double cpr, double rpm) {
            this.cpr = cpr;
            this.rpm = rpm;
        }

        public double getCPR() {
            return cpr;
        }

        public double getRPM() {
            return rpm;
        }
    }

    public enum Direction {
        FORWARD(1), REVERSE(-1);

        private final int val;

        Direction(int multiplier) {
            val = multiplier;
        }

        public int getMultiplier() {
            return val;
        }
    }

    public class Encoder {

        private final Supplier<Integer> m_position;
        private int resetVal, lastPosition;
        private Direction direction = Direction.FORWARD;
        private double lastTimeStamp, veloEstimate, lastVelo, accel, dpp = 1;

        /**
         * The encoder object for the motor.
         *
         * @param position the position supplier which just points to the
         *                 current position of the motor in ticks
         */
        public Encoder(Supplier<Integer> position) {
            m_position = position;
            lastTimeStamp = (double) System.nanoTime() / 1E9;
        }

        /**
         * @return the current position in ticks of the encoder
         */
        public int getPosition() {
            int currentPosition = m_position.get();

            double currentTime = (double) System.nanoTime() / 1E9;
            double dt = currentTime - lastTimeStamp;

            veloEstimate = (currentPosition - lastPosition) / dt;
            lastPosition = currentPosition;
            lastTimeStamp = currentTime;

            return direction.getMultiplier() * currentPosition - resetVal;
        }

        /**
         * @return the position of the encoder adjusted to account for the distance per pulse
         */
        public double getDistance() {
            return dpp * getPosition();
        }

        /**
         * @return the velocity of the encoder adjusted to account for the distance per pulse
         */
        public double getRate() {
            return dpp * getVelocity();
        }

        /**
         * Resets the encoder without having to stop the motor.
         */
        public void reset() {
            resetVal += getPosition();

            lastVelo = getVelocity();
            lastPosition = getPosition();
        }

        /**
         * Sets the distance per pulse of the encoder.
         *
         * @param distancePerPulse the desired distance per pulse (in units per tick)
         */
        public Encoder setDistancePerPulse(double distancePerPulse) {
            dpp = distancePerPulse;
            return this;
        }

        /**
         * Sets the direction of the encoder to forward or reverse
         *
         * @param direction the desired direction
         */
        public void setDirection(Direction direction) {
            this.direction = direction;
        }

        /**
         * @return the number of revolutions turned by the encoder
         */
        public double getRevolutions() {
            return getPosition() / getCPR();
        }

        /**
         * @return the raw velocity of the motor reported by the encoder
         */
        public double getRawVelocity() {
            double velo = getVelocity();
            double currentTime = (double) System.nanoTime() / 1E9;
            double dt = currentTime - lastTimeStamp;

            accel = (velo - lastVelo) / dt;
            lastVelo = velo;
            lastTimeStamp = currentTime;

            return velo;
        }

        /**
         * @return the estimated acceleration of the motor in ticks per second squared
         */
        public double getAcceleration() {
            this.getRawVelocity(); // Update the acceleration
            return accel;
        }

        private final static int CPS_STEP = 0x10000;

        /**
         * Corrects for velocity overflow
         *
         * @return the corrected velocity
         */
        public double getCorrectedVelocity() {
            this.getPosition(); // Update velocity estimate
            double real = getRawVelocity();
            while (Math.abs(veloEstimate - real) > CPS_STEP / 2.0) {
                real += Math.signum(veloEstimate - real) * CPS_STEP;
            }

            return real;
        }
    }

    /**
     * The RunMode of the motor.
     */
    public enum RunMode {
        VelocityControl, PositionControl, RawPower
    }

    public enum ZeroPowerBehavior {
        UNKNOWN(DcMotor.ZeroPowerBehavior.UNKNOWN),
        BRAKE(DcMotor.ZeroPowerBehavior.BRAKE),
        FLOAT(DcMotor.ZeroPowerBehavior.FLOAT);

        private final DcMotor.ZeroPowerBehavior m_behavior;

        ZeroPowerBehavior(DcMotor.ZeroPowerBehavior behavior) {
            m_behavior = behavior;
        }

        public DcMotor.ZeroPowerBehavior getBehavior() {
            return m_behavior;
        }
    }

    public DcMotor motor;
    public Encoder encoder;

    /**
     * The runmode of the motor
     */
    protected RunMode runmode = RunMode.RawPower;

    /**
     * The achievable ticks per second velocity of the motor
     */
    public double ACHIEVABLE_MAX_TICKS_PER_SECOND;

    protected PIDController veloController = new PIDController(1, 0, 0);

    protected PController positionController = new PController(1);

    protected SimpleMotorFeedforward feedforward = new SimpleMotorFeedforward(0, 1, 0);

    protected double bufferFraction = 0.9;

    public Motor() {
    }

    /**
     * Constructs the instance motor for the wrapper using the SDK object
     *
     * @param motor the DcMotor object
     */
    public Motor(@NonNull DcMotor motor) {
        this(motor, GoBILDA.NONE);
    }

    /**
     * Constructs the instance motor for the wrapper using the SDK object
     *
     * @param motor       the DcMotor object
     * @param gobildaType the type of GoBilda 5204 series motor being used
     */
    public Motor(@NonNull DcMotor motor, @NonNull GoBILDA gobildaType) {
        this(motor, gobildaType.getCPR(), gobildaType.getRPM());
    }

    /**
     * Constructs an instance motor for the wrapper using the SDK object
     *
     * @param motor the DcMotor object
     * @param cpr   the counts per revolution of the motor
     * @param rpm   the revolutions per minute of the motor
     */
    public Motor(@NonNull DcMotor motor, double cpr, double rpm) {
        this.motor = motor;
        encoder = new Encoder(motor::getCurrentPosition);

        if (cpr != 0 && rpm != 0) {
            MotorConfigurationType type = motor.getMotorType().clone();

            type.setMaxRPM(rpm);
            type.setTicksPerRev(cpr);
            type.setAchieveableMaxRPMFraction(1.0);

            this.motor.setMotorType(type);
        }

        ACHIEVABLE_MAX_TICKS_PER_SECOND = this.motor.getMotorType().getAchieveableMaxTicksPerSecond();
    }

    /**
     * Constructs the instance motor for the wrapper
     *
     * @param hMap the hardware map from the OpMode
     * @param id   the device id from the RC config
     */
    public Motor(@NonNull HardwareMap hMap, String id) {
        this(hMap, id, GoBILDA.NONE);
    }

    /**
     * Constructs the instance motor for the wrapper
     *
     * @param hMap        the hardware map from the OpMode
     * @param id          the device id from the RC config
     * @param gobildaType the type of GoBilda 5204 series motor being used
     */
    public Motor(@NonNull HardwareMap hMap, String id, @NonNull GoBILDA gobildaType) {
        this(hMap, id, gobildaType.getCPR(), gobildaType.getRPM());
    }

    /**
     * Constructs an instance motor for the wrapper
     *
     * @param hMap the hardware map from the OpMode
     * @param id   the device id from the RC config
     * @param cpr  the counts per revolution of the motor
     * @param rpm  the revolutions per minute of the motor
     */
    public Motor(@NonNull HardwareMap hMap, String id, double cpr, double rpm) {
        motor = hMap.get(DcMotor.class, id);
        encoder = new Encoder(motor::getCurrentPosition);

        if (cpr != 0 && rpm != 0) {
            MotorConfigurationType type = motor.getMotorType().clone();

            type.setMaxRPM(rpm);
            type.setTicksPerRev(cpr);
            type.setAchieveableMaxRPMFraction(1.0);

            motor.setMotorType(type);
        }

        ACHIEVABLE_MAX_TICKS_PER_SECOND = motor.getMotorType().getAchieveableMaxTicksPerSecond();
    }

    /**
     * Common method for setting the speed of a motor.
     *
     * @param output The percentage of power to set. Value should be between -1.0 and 1.0.
     */
    public void set(double output) {
        output = MathUtils.clamp(output, -1.0, 1.0);

        if (runmode == RunMode.VelocityControl) {
            double speed = bufferFraction * output * ACHIEVABLE_MAX_TICKS_PER_SECOND;
            double velocity = veloController.calculate(getVelocity(), speed) + feedforward.calculate(speed, encoder.getAcceleration());
            motor.setPower(velocity / ACHIEVABLE_MAX_TICKS_PER_SECOND);
        }
        else if (runmode == RunMode.PositionControl) {
            double error = positionController.calculate(getDistance());
            motor.setPower(output * error);
        }
        else motor.setPower(output);
    }

    /**
     * Sets the distance per pulse of the encoder in units per tick.
     *
     * @param distancePerPulse the desired distance per pulse
     * @return an encoder an object with the specified distance per pulse
     */
    public Encoder setDistancePerPulse(double distancePerPulse) {
        return encoder.setDistancePerPulse(distancePerPulse);
    }

    /**
     * @return the distance traveled by the encoder
     */
    public double getDistance() {
        return encoder.getDistance();
    }

    /**
     * @return the rate of the encoder
     */
    public double getRate() {
        return encoder.getRate();
    }

    /**
     * @return if the motor is at the target position or distance
     */
    public boolean atTargetPosition() {
        return positionController.atSetPoint();
    }

    /**
     * Resets the external encoder wrapper value.
     */
    public void resetEncoder() {
        encoder.reset();
    }

    /**
     * Resets the internal position of the motor.
     */
    public void stopAndResetEncoder() {
        encoder.resetVal = 0;
        motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
    }

    /**
     * @return the velocity coefficients
     */
    public double[] getVeloCoefficients() {
        return veloController.getCoefficients();
    }

    /**
     * @return the position coefficient
     */
    public double getPositionCoefficient() {
        return positionController.getP();
    }

    /**
     * @return the feedforward coefficients
     */
    public double[] getFeedforwardCoefficients() {
        return new double[]{feedforward.ks, feedforward.kv, feedforward.ka};
    }

    /**
     * A wrapper method for the zero power behavior
     *
     * @param behavior the behavior desired
     */
    public void setZeroPowerBehavior(ZeroPowerBehavior behavior) {
        motor.setZeroPowerBehavior(behavior.getBehavior());
    }

    /**
     * @return the current position of the motor in ticks
     */
    public int getCurrentPosition() {
        return encoder.getPosition();
    }

    /**
     * @return the corrected velocity for overflow
     */
    public double getCorrectedVelocity() {
        return encoder.getCorrectedVelocity();
    }

    /**
     * @return the counts per revolution of the motor
     */
    public double getCPR() {
        return motor.getMotorType().getTicksPerRev();
    }

    /**
     * @return the max RPM of the motor
     */
    public double getMaxRPM() {
        return motor.getMotorType().getMaxRPM();
    }

    /**
     * Set the buffer for the motor. This adds a fractional value to the velocity control.
     *
     * @param fraction a fractional value between (0, 1].
     */
    public void setBuffer(double fraction) {
        assert fraction > 0 && fraction <= 1: "Buffer must be between 0 and 1, exclusive to 0";
        bufferFraction = fraction;
    }

    /**
     * Sets the {@link RunMode} of the motor
     *
     * @param runmode the desired runmode
     */
    public void setRunMode(RunMode runmode) {
        this.runmode = runmode;
        veloController.reset();
        positionController.reset();

        if (runmode == RunMode.PositionControl)
            setTargetDistance(getDistance());
    }

    protected double getVelocity() {
        return ((DcMotorEx) motor).getVelocity();
    }

    /**
     * Common method for getting the current set speed of a motor.
     *
     * @return The current set speed. Value is between -1.0 and 1.0.
     */
    public double get() {
        return motor.getPower();
    }

    /**
     * Sets the target position for the motor to the desired target.
     * Once {@link #set(double)} is called, the motor will attempt to move in the direction
     * of said target.
     *
     * @param target the target position in ticks
     */
    public void setTargetPosition(int target) {
        setTargetDistance(target * encoder.dpp);
    }

    /**
     * Sets the target distance for the motor to the desired target.
     * Once {@link #set(double)} is called, the motor will attempt to move in the direction
     * of said target.
     *
     * @param target the target position in units of distance
     */
    public void setTargetDistance(double target) {
        positionController.setSetPoint(target);
    }

    /**
     * Sets the target tolerance
     *
     * @param tolerance the specified tolerance
     */
    public void setPositionTolerance(double tolerance) {
        positionController.setTolerance(tolerance);
    }

    /**
     * Common method for inverting direction of a motor.
     *
     * @param isInverted The state of inversion true is inverted.
     */
    public void setInverted(boolean isInverted) {
        motor.setDirection(isInverted ? DcMotor.Direction.REVERSE : DcMotor.Direction.FORWARD);
    }

    /**
     * Common method for returning if a motor is in the inverted state or not.
     *
     * @return isInverted The state of the inversion true is inverted.
     */
    public boolean getInverted() {
        return DcMotor.Direction.REVERSE == motor.getDirection();
    }

    /**
     * Set the velocity pid coefficients for the motor.
     *
     * @param kp the proportional gain
     * @param ki the integral gain
     * @param kd the derivative gain
     */
    public void setVeloCoefficients(double kp, double ki, double kd) {
        veloController.setPIDF(kp, ki, kd, 0);
    }

    /**
     * Set the feedforward coefficients for the motor.
     *
     * @param ks the static gain
     * @param kv the velocity gain
     */
    public void setFeedforwardCoefficients(double ks, double kv) {
        feedforward = new SimpleMotorFeedforward(ks, kv);
    }

    /**
     * Set the feedforward coefficients for the motor.
     *
     * @param ks the static gain
     * @param kv the velocity gain
     * @param ka the acceleration gain
     */
    public void setFeedforwardCoefficients(double ks, double kv, double ka) {
        feedforward = new SimpleMotorFeedforward(ks, kv, ka);
    }

    /**
     * Set the proportional gain for the position controller.
     *
     * @param kp the proportional gain
     */
    public void setPositionCoefficient(double kp) {
        positionController.setP(kp);
    }

    /**
     * Disable the motor.
     */
    @Override
    public void disable() {
        motor.close();
    }

    @Override
    public String getDeviceType() {
        return String.format(Locale.ROOT,
                "Motor %s from %s in port %d",
                motor.getDeviceName(), motor.getManufacturer(), motor.getPortNumber());
    }

    /**
     * Stops motor movement. Motor can be moved again by calling set without having to re-enable the
     * motor.
     */
    public void stopMotor() {
        motor.setPower(0);
    }
}
