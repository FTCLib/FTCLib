package com.arcrobotics.ftclib.command;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class CommandOpMode extends LinearOpMode {
    /**
     * Initialize all objects, set up subsystems, etc...
     */
    public abstract void initialize();

    /**
     * Run Op Mode. Is called after user presses play button
     */
    public abstract void run();

    @Override
    public void runOpMode() throws InterruptedException {
        initialize();
        waitForStart();
        run();
    }

    public void addSequential(Command newCommand) {
        final Command command = newCommand;
        command.initialize();
        final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(3);


        Runnable updateMethod = new Runnable() {
            @Override
            public void run() {
                try {
                    telemetry.addData("Running: ", true);
                    command.execute();
                    telemetry.update();
                } catch(Exception e) {
                    telemetry.addData("Running: ", false);
                    telemetry.addData("Exception: ", e);

                    telemetry.update();
                }
            }
        };
        try {
            scheduledExecutorService.scheduleAtFixedRate(updateMethod, 0,20, TimeUnit.MILLISECONDS);
            while(!command.isFinished() && this.opModeIsActive()) {
                //telemetry.update();
            }
            scheduledExecutorService.shutdownNow();

        } catch (Exception e) {
            command.end();
            throw e;
        }
        command.end();
    }

    public void addSequential(Command newCommand, double dt) {
        final long timeInterval = (long) dt;
        final Command command = newCommand;
        command.initialize();
        final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(3);


        Runnable updateMethod = new Runnable() {
            @Override
            public void run() {
                try {
                    telemetry.addData("Running: ", true);
                    command.execute();
                    telemetry.update();
                } catch(Exception e) {
                    telemetry.addData("Running: ", false);
                    telemetry.addData("Exception: ", e);

                    telemetry.update();
                }
            }
        };
        try {
            scheduledExecutorService.scheduleAtFixedRate(updateMethod, 0,timeInterval, TimeUnit.MILLISECONDS);
            while(!command.isFinished() && this.opModeIsActive()) {
                //telemetry.update();
            }
            scheduledExecutorService.shutdownNow();

        } catch (Exception e) {
            command.end();
            throw e;
        }
        command.end();
    }
}



