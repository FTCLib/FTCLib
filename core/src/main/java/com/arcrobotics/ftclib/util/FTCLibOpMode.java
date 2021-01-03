package com.arcrobotics.ftclib.util;

import com.arcrobotics.ftclib.gamepad.GamepadEx;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import java.util.List;

public abstract class FTCLibOpMode extends LinearOpMode {

    protected GamepadEx driverOp, toolOp;

    @Override
    public final void runOpMode() throws InterruptedException {
        driverOp = new GamepadEx(gamepad1);
        toolOp = new GamepadEx(gamepad2);

        initialize();

        List<LynxModule> hubs = hardwareMap.getAll(LynxModule.class);
        hubs.forEach(hub -> hub.setBulkCachingMode(LynxModule.BulkCachingMode.MANUAL));

        waitForStart();

        while (!isStopRequested() && opModeIsActive()) {
            hubs.forEach(LynxModule::clearBulkCache);
            run();
            driverOp.readButtons();
            toolOp.readButtons();
        }
    }

    /**
     * This method is like {@link OpMode#init()}
     */
    public abstract void initialize();

    /**
     * This method is like {@link OpMode#loop()}
     */
    public abstract void run();

}