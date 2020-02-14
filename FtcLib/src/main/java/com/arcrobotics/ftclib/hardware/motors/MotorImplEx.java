package com.arcrobotics.ftclib.hardware.motors;

import com.arcrobotics.ftclib.controller.PController;
import com.arcrobotics.ftclib.controller.PIDFController;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;
import com.qualcomm.robotcore.hardware.HardwareMap;

/**
 * An extended implemented motor. Uses a {@link MotorImpl} along
 * with the methods of {@link MotorEx}.
 */
public class MotorImplEx extends MotorEx {

    private MotorImpl motor;
    private double distancePerPulse;

    /**
     * The constructor or the object.
     *
     * @param ex    the implemented motor
     */
    public MotorImplEx(MotorImpl ex) {
        super(ex.mot, ex.getCPR(),
                new PIDFController(new double[]
                        {ex.mot.getP(),ex.mot.getI(),ex.mot.getD(),ex.mot.getF()}));
        
        motor = ex;
        distancePerPulse = -1; // not set yet
    }

    /**
     * The constructor for the motor that includes an internal
     * PIDF controller.
     *
     * @param ex                the motor in question
     * @param pidfController    the PIDF controller that controls the output of the motor
     */
    public MotorImplEx(MotorImpl ex, PIDFController pidfController) {
        super(ex.mot, ex.getCPR(), pidfController);

        motor = ex;
        distancePerPulse = -1; // not set yet
    }

    /**
     * The constructor for the motor that includes an internal
     * PIDF controller.
     *
     * @param ex                the motor in question
     * @param pidfController    the PIDF controller that controls the output of the motor
     * @param pController       the P controller that controls the position of the motor
     */
    public MotorImplEx(MotorImpl ex, PIDFController pidfController, PController pController) {
        super(ex.mot, ex.getCPR(), pidfController, pController);

        motor = ex;
        distancePerPulse = -1; // not set yet
    }
    
    public MotorImplEx(HardwareMap hMap, String name, double cpr) {
        this(new MotorImpl(hMap, name, cpr));
        
        distancePerPulse = -1; // not set yet
    }
    
    public MotorImplEx(HardwareMap hMap, String name, double cpr, PIDFController pidfController) {
        this(new MotorImpl(hMap, name, cpr), pidfController);

        distancePerPulse = -1; // not set yet
    }

    public MotorImplEx(HardwareMap hMap, String name, double cpr, PIDFController pidfController,
                       PController pController) {
        this(new MotorImpl(hMap, name, cpr), pidfController, pController);

        distancePerPulse = -1; // not set yet
    }

    @Override
    double getCurrentPosition() {
        return motor.getCurrentPosition();
    }

    /**
     * sets the distance per encoder tick
     *
     * @param distancePerPulse  the distance travelled after one tick
     */
    public void setDistancePerPulse(double distancePerPulse) {
        this.distancePerPulse = Math.abs(distancePerPulse);
    }

    /**
     * @return the number of encoder ticks
     */
    public double getEncoderPulses() {
        return getCurrentPosition();
    }

    public double getDistance() throws Exception {
        if(distancePerPulse == -1) {
            throw new Exception("Must set distance per pulse or call" +
                    " methods that set distance per pulse before getting distance!");
        }

        return distancePerPulse * getCurrentPosition();
    }

    public void setTargetDistance(double distance) throws Exception {
        if (distancePerPulse == -1) {
            throw new Exception("Must set distance per pulse or call" +
                    " methods that set distance per pulse before setting distance!");
        }

        setTargetPosition(distance / distancePerPulse);
    }

    @Override
    public double get() {
        return motor.getPower();
    }

    public boolean isBusy() { return motor.isBusy(); }

    public double getVelocity() { return motor.getVelocity(); }

    public double getPower() { return get(); }

    public void setPower(double power) {
        motor.setPower(power);
    }

    public void setVelocity(double velocity) {
        motor.setVelocity(velocity);
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
    public void disable() {
        motor.disable();
    }

    @Override
    public String getDeviceType() {
        return motor.getDeviceType();
    }

    @Override
    public void pidWrite(double output) {
        motor.pidWrite(output);
    }


}
