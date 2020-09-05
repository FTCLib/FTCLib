package com.arcrobotics.ftclib.hardware.motors;

import com.arcrobotics.ftclib.controller.PController;
import com.arcrobotics.ftclib.controller.PIDController;
import com.arcrobotics.ftclib.controller.PIDFController;
import com.arcrobotics.ftclib.controller.wpilibcontroller.SimpleMotorFeedforward;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;

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
    public DcMotorEx motor;

    /**
     * Constructs the instance motor for the
     *
     * @param hMap the hardware map from the OpMode
     * @param id   the device id from the RC config
     */
    public MotorEx(HardwareMap hMap, String id) {
        motor = hMap.get(DcMotorEx.class, id);
        runmode = RunMode.RawPower;
        ACHIEVABLE_MAX_TICKS_PER_SECOND = motor.getMotorType().getAchieveableMaxTicksPerSecond();
    }

    @Override
    public void set(double output) {

    }

    @Override
    public double getVelocity() {
        return motor.getVelocity();
    }

    @Override
    public String getDeviceType() {
        return "Extended " + super.getDeviceType();
    }

}