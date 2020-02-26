package com.arcrobotics.ftclib.controller;

/**
 * This is a PID controller (https://en.wikipedia.org/wiki/PID_controller)
 * for your robot. Internally, it performs all the calculations for you.
 * You need to tune your values to the appropriate amounts in order
 * to properly utilize these calculations.
 *
 * The equation we will use is:
 * u(t) = kP * e(t) + kI * int(0,t)[e(t')dt'] + kD * e'(t) + kF
 * where e(t) = r(t) - y(t) and r(t) is the setpoint and y(t) is the
 * measured value. If we consider e(t) the positional error, then
 * int(0,t)[e(t')dt'] is the total error and e'(t) is the velocity error.
 */
public class PIDFController {

    private double kP, kI, kD, kF;
    private double setPoint;
    private double measuredValue;

    private double errorVal_p;
    private double errorVal_v;

    private double totalError;
    private double prevErrorVal;

    private double errorTolerance_p = 0.05;
    private double errorTolerance_v = Double.POSITIVE_INFINITY;

    private double period;

    /**
     * Pre-Condition: coeff is a four-element array: {kP, kI, kD, kF}
     *
     * <p>The {@link #period} is automatically set to 0.02.</p>
     */
    public PIDFController(double[] coeff) {
        this(coeff, 0, 0, 0.02);
    }

    /**
     * This is the full constructor for the PIDF controller. Our PIDF controller
     * includes a feed-forward value which is useful for fighting friction and gravity.
     * Our errorVal represents the return of e(t) and prevErrorVal is the previous error.
     *
     * @param coeff     The coefficient array as specified in the pre-condition of the basic constructor.
     * @param sp        The setpoint of the pid control loop.
     * @param pv        The measured value of he pid control loop. We want sp = pv, or to the degree
     *                  such that sp - pv, or e(t) < tolerance.
     * @param period    The interval of time between the iteration of the control loop.
     */
    public PIDFController(double[] coeff, double sp, double pv, double period) {
        kP = coeff[0];
        kI = coeff[1];
        kD = coeff[2];
        kF = coeff[3];

        setPoint = sp;
        measuredValue = pv;

        errorVal_p = setPoint - measuredValue;
        this.period = period;
        reset();
    }

    public void reset() {
        totalError = 0;
        prevErrorVal = 0;
    }

    /**
     * Sets the error which is considered tolerable for use with {@link #atSetPoint()}.
     *
     * @param positionTolerance Position error which is tolerable.
     */
    public void setTolerance(double positionTolerance) {
        setTolerance(positionTolerance, Double.POSITIVE_INFINITY);
    }

    /**
     * Sets the error which is considered tolerable for use with {@link #atSetPoint()}.
     *
     * @param positionTolerance Position error which is tolerable.
     * @param velocityTolerance Velocity error which is tolerable.
     */
    public void setTolerance(double positionTolerance, double velocityTolerance) {
        errorTolerance_p = positionTolerance;
        errorTolerance_v = velocityTolerance;
    }

    /**
     * Returns the current setpoint of the PIDFController.
     *
     * @return The current setpoint.
     */
    public double getSetPoint() {
        return setPoint;
    }

    /**
     * Sets the setpoint for the PIDFController
     *
     * @param sp The desired setpoint.
     */
    public void setSetPoint(double sp) {
        setPoint = sp;
    }

    /**
     * Returns true if the error is within the percentage of the total input range, determined by
     * {@link #setTolerance}.
     *
     * @return Whether the error is within the acceptable bounds.
     */
    public boolean atSetPoint() {
        return Math.abs(errorVal_p) < errorTolerance_p
                && Math.abs(errorVal_v) < errorTolerance_v;
    }

    /**
     * @return the period of time between the interval
     */
    public double getPeriod() {
        return period;
    }

    /**
     * @return the PIDF coefficients
     */
    public double[] getCoefficients() {
        return new double[]{kP, kI, kD, kF};
    }

    /**
     * @return the positional error e(t)
     */
    public double getPositionError() {
        return errorVal_p;
    }

    /**
     * @return the tolerances of the controller
     */
    public double[] getTolerance() {
        return new double[]{errorTolerance_p, errorTolerance_v};
    }

    /**
     * @return the velocity error e'(t)
     */
    public double getVelocityError() {
        return errorVal_v;
    }

    /**
     * Calculates the next output of the PIDF controller.
     *
     * @return the next output using the current measured value via
     * {@link #calculate(double)}.
     */
    public double calculate() {
        return calculate(measuredValue);
    }

    /**
     * Calculates the next output of the PIDF controller.
     *
     * @param pv The given measured value.
     * @param sp The given setpoint.
     * @return the next output using the given measurd value via
     * {@link #calculate(double)}.
     */
    public double calculate(double pv, double sp) {
        // set the setpoint to the provided value
        setSetPoint(sp);
        return calculate(pv);
    }

    /**
     * Calculates the control value, u(t).
     *
     * @param pv The current measurement of the process variable.
     * @return the value produced by u(t).
     */
    public double calculate(double pv) {
        prevErrorVal = errorVal_p;

        if (measuredValue == pv) {
            errorVal_p = setPoint - measuredValue;
        } else {
            errorVal_p = setPoint - pv;
            measuredValue = pv;
        }

        if (period != 0) {
            errorVal_v = (errorVal_p - prevErrorVal) / period;
        } else {
            errorVal_v = 0;
        }

        /*
        if total error is the integral from 0 to t of e(t')dt', and
        e(t) = sp - pv, then the total error, E(t), equals sp*t - pv*t.
         */
        totalError = period * (setPoint - measuredValue);

        // returns u(t)
        return kP * errorVal_p + kI * totalError + kD * errorVal_v + kF * setPoint;
    }

}
