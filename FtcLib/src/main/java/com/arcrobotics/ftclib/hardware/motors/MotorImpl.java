package com.arcrobotics.ftclib.hardware.motors;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;
import com.qualcomm.robotcore.hardware.VoltageSensor;

import static com.arcrobotics.ftclib.util.MathUtils.clamp;

/**
 * An implementation of the motor. Utilizes the {@link DcMotorEx}
 * object from the SDK.
 */
public class MotorImpl {

    CustomMotor mot;

    public class CustomMotor implements Motor {

        private DcMotorEx motor;
        private double counts_per_rev;
        private HardwareMap hMap;
        public CustomMotor(HardwareMap hMap, String motorName, double cpr) {
            motor = (DcMotorEx)hMap.get(DcMotor.class, motorName);
            this.hMap = hMap;
            counts_per_rev = cpr;
        }

        public boolean isBusy() { return motor.isBusy(); }


        @Override
        public void set(double speed) {
            motor.setPower(speed);
        }

        /**
         * Set the power of the motor.
         *
         * @param power The percentage of power running through the motor.
         */
        public void setPower(double power) {
            set(power);
        }

        /**
         * @return the internal PIDF coefficients of the motor
         */
        public PIDFCoefficients getPIDFCoefficients() {
            return motor.getPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER);
        }

        /**
         * @return the internal kP value
         */
        public double getP() {
            return getPIDFCoefficients().p;
        }

        /**
         * @return the internal kI value
         */
        public double getI() {
            return getPIDFCoefficients().i;
        }

        /**
         * @return the internal kD value
         */
        public double getD() {
            return getPIDFCoefficients().d;
        }

        /**
         * @return the internal kF value
         */
        public double getF() {
            return getPIDFCoefficients().f;
        }

        /**
         * Sets the velocity of the motor in terms of ticks per second.
         *
         * @param velocity the desired velocity of the motor.
         */
        public void setVelocity(double velocity) { motor.setVelocity(velocity); }

        /**
         * returns the maximum speed of the motor given its rotations per minute.
         *
         * @param rpm   The number of revolutions per minute of the motor.
         * @return the maximum velocity of the motor in ticks per second.
         */
        public double getMaxVelocity(double rpm) {
            return counts_per_rev * rpm / 60;
        }

        @Override
        public double get() {
            return motor.getPower();
        }

        /**
         * @return the percentage of the max speed of the motor and its direction.
         */
        public double getPower() {
            return get();
        }

        /**
         * @return the velocity of the motor in ticks / second.
         */
        public double getVelocity() {
            return motor.getVelocity();
        }

        @Override
        public void setInverted(boolean isInverted) {
            motor.setDirection(isInverted ? DcMotor.Direction.REVERSE : DcMotor.Direction.FORWARD);
        }

        @Override
        public boolean getInverted() {
            return motor.getDirection() == DcMotor.Direction.REVERSE;
        }

        @Override
        public void disable() {
            motor.setMotorDisable();
        }

        @Override
        public String getDeviceType() {
            String port = Integer.toString(motor.getPortNumber());
            String controller = motor.getController().toString();
            return "Motor: " + port + "; " + controller;
        }

        @Override
        public void pidWrite(double output) {
            motor.setVelocity(output);
        }

        @Override
        public void stopMotor() {
            set(0);
        }

        /**
         * @return the current position of the motor
         */
        public double getCurrentPosition() {
            return motor.getCurrentPosition();
        }

    }

    /**
     * References a dcMotor object in the hardware map (located in your
     * local configuration) and constructs an implemented motor.
     *
     * @param hMap      the hardware map
     * @param motorName the name of the motor in the local configuration
     * @param cpr       the counts per revolution of the motor
     */
    public MotorImpl(HardwareMap hMap, String motorName, double cpr) {
        mot = new CustomMotor(hMap, motorName, cpr);
    }

    /**
     * Sets the power of the motor to a percentage of the maximum speed.
     *
     * @param speed the percentage of the maximum speed
     */
    public void setPower(double speed) {
        mot.setPower(speed);
    }

    /**
     * Sets the velocity of the motor to a set number of ticks per second.
     *
     * @param velocity the desired speed in ticks per second
     */
    public void setVelocity(double velocity) {
        mot.setVelocity(velocity);
    }

    /**
     * Returns the maximum velocity of the motor.
     *
     * @param rpm The rotations per minute of the output shaft.
     * @return the maximum velocity
     */
    public double getMaxVelocity(double rpm) {
        return mot.getMaxVelocity(rpm);
    }

    /**
     * The current position of the motor.
     *
     * @return The current position of the output shaft in ticks
     */
    double getCurrentPosition() {
        return mot.getCurrentPosition();
    }

    /**
     * @return the percentage of the max speed of the output shaft
     */
    public double getPower() {
        return mot.getPower();
    }

    /**
     * @return the velocity of the motor in ticks per second
     */
    public double getVelocity() {
        return mot.getVelocity();
    }

    public void setInverted(boolean isInverted) {
        mot.setInverted(isInverted);
    }

    public boolean getInverted() {
        return mot.getInverted();
    }

    public void disable() {
        mot.disable();
    }

    public double getCPR() {
        return mot.counts_per_rev;
    }

    public void pidWrite(double output) {
        mot.pidWrite(output);
    }

    public String getDeviceType() {
        return mot.getDeviceType();
    }

    public boolean isBusy() { return mot.isBusy(); }


}
