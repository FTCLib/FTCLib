package com.arcrobotics.ftclib.hardware.motors;

import android.support.annotation.NonNull;

import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;

import java.util.function.Supplier;

/**
 * An extended motor class that utilizes more features than the
 * regular motor.
 *
 * @author Jackson
 */
public class MotorEx extends Motor {

    /**
     * The motor for the MotorEx class.
     */
    public DcMotorEx motorEx;

    public class EncoderEx extends Encoder {

        /**
         * The encoder object for the motor.
         *
         * @param position the position supplier which just points to the
         *                 current position of the motor in ticks
         */
        public EncoderEx(Supplier<Integer> position) {
            super(position);
        }

        public double getRawVelocity() {
            return motorEx.getVelocity();
        }

        private final static int CPS_STEP = 0x10000;

        /**
         * Corrects for velocity overflow
         *
         * @return the corrected velocity
         */
        public double getCorrectedVelocity() {
            double real = getRawVelocity();
            while (Math.abs(veloEstimate - real) > CPS_STEP / 2.0) {
                real += Math.signum(veloEstimate - real) * CPS_STEP;
            }
            return real;
        }

    }

    /**
     * Constructs the instance motor for the wrapper
     *
     * @param hMap the hardware map from the OpMode
     * @param id   the device id from the RC config
     */
    public MotorEx(@NonNull HardwareMap hMap, String id) {
        this(hMap, id, GoBILDA.NONE);
        ACHIEVABLE_MAX_TICKS_PER_SECOND = motorEx.getMotorType().getAchieveableMaxTicksPerSecond();
    }

    /**
     * Constructs the instance motor for the wrapper
     *
     * @param hMap        the hardware map from the OpMode
     * @param id          the device id from the RC config
     * @param gobildaType the type of gobilda 5202 series motor being used
     */
    public MotorEx(@NonNull HardwareMap hMap, String id, @NonNull GoBILDA gobildaType) {
        motorEx = hMap.get(DcMotorEx.class, id);
        runmode = RunMode.RawPower;
        type = gobildaType;
        ACHIEVABLE_MAX_TICKS_PER_SECOND = gobildaType.getAchievableMaxTicksPerSecond();
        encoder = new EncoderEx(motorEx::getCurrentPosition);
    }

    @Override
    public void set(double output) {
        if (runmode == RunMode.VelocityControl) {
            motorEx.setVelocity(output * ACHIEVABLE_MAX_TICKS_PER_SECOND);
        } else {
            super.set(output);
        }
    }

    @Override
    public double getVelocity() {
        return ((EncoderEx) encoder).getCorrectedVelocity();
    }

    @Override
    public String getDeviceType() {
        return "Extended " + super.getDeviceType();
    }

}