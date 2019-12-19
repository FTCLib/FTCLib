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
        final Command command = newCommand;
        command.initialize();

        // Start a ScheduledExecutorService to get precise loop timing.
        final ScheduledExecutorService scheduledExecutorService =
                Executors.newScheduledThreadPool(3);

        Runnable updateMethod = new Runnable() {
            @Override
            public void run() {
                // Runs the command's execute function in a 20 ms loop
                // If it throws an exception, then print that exception to telemtry
                try {
                    telemetry.addData("Running: ", true);
                    command.execute();
                    telemetry.update();
                } catch(Exception e) {
                    telemetry.addData("Exception: ", e.getMessage());
                    telemetry.update();
                    throw e;
                }
            }
        };

        try {
            scheduledExecutorService
                    .scheduleAtFixedRate(updateMethod, 0,20, TimeUnit.MILLISECONDS);
            // Start the loop thread
            scheduledExecutorService.scheduleAtFixedRate(updateMethod, 0,20, TimeUnit.MILLISECONDS);
            // Delay the execution of future if the command isn't finished and the opMode isn't stopped
            while(!command.isFinished() && this.opModeIsActive()) {

            }
            // Once command is finished or opMode is stopped, then shutdown the service
            scheduledExecutorService.shutdownNow();

        } catch (Exception e) {
            // If a problem happens, end the command, print the error to telemetry, and throw an exception
            command.end();
            telemetry.addData("Exception: ", e.getMessage());
            telemetry.update();
            throw e;
        }

        // If the command has run command successfully, then end the command
        command.end();
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



