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

    /**
     * addSequential takes in a new command and runs it, delaying any code until the command isFinished. Then, it runs its initialize function.
     * After that, it runs the command's execute function every 20 ms.
     * After each iteration of the loop, it checks the command's isFinished method.
     * If the isFinished method is true, it exits out of the loop and runs the command's end method.
     * @param newCommand new Command to run.
     */
    public void addSequential(Command newCommand) {
        addSequential(newCommand, 20);
    }

    /**
     * Runs addSequential with a user-specified time interval (in ms)
     * @param newCommand Command to run
     * @param dt Time interval of loop iterations
     */
    public void addSequential(Command newCommand, double dt) {
        final long timeInterval = (long) dt;
        final Command command = newCommand;
        command.initialize();
        final ScheduledExecutorService scheduledExecutorService =
                Executors.newScheduledThreadPool(3);

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
            scheduledExecutorService
                    .scheduleAtFixedRate(updateMethod, 0,timeInterval, TimeUnit.MILLISECONDS);
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



