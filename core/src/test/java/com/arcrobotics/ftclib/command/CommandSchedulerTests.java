package com.arcrobotics.ftclib.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CommandSchedulerTests {

    public static int x = 3;

    @Test
    public void testAddCommand() {
        x = 3;
        Robot.isDisabled = false;
        CommandScheduler.getInstance().schedule(new CommandBase(){
            @Override
            public void execute() {
                x = 5;
            }

            @Override
            public boolean runsWhenDisabled() {
                return false;
            }
        });

        CommandScheduler.getInstance().run();
        assertEquals(5, x);
        CommandScheduler.getInstance().reset();
    }

    @Test
    public void testNotRunWhenDisabled() {
        x = 3;
        Robot.isDisabled = true;
        CommandScheduler.getInstance().schedule(new CommandBase(){
            @Override
            public void execute() {
                x = 5;
            }

            @Override
            public boolean runsWhenDisabled() {
                return false;
            }
        });

        CommandScheduler.getInstance().run();
        assertEquals(3, x);
        CommandScheduler.getInstance().reset();
    }

    @Test
    public void testRunWhenDisabled() {
        x = 3;
        Robot.isDisabled = true;
        CommandScheduler.getInstance().schedule(new CommandBase(){
            @Override
            public void execute() {
                x = 5;
            }

            @Override
            public boolean runsWhenDisabled() {
                return true;
            }
        });

        CommandScheduler.getInstance().run();
        assertEquals(5, x);
        CommandScheduler.getInstance().reset();
    }

    @Test
    public void testSubsystemPeriodic() {
        x = 3;
        Robot.isDisabled = false;
        CommandScheduler.getInstance().registerSubsystem(new SubsystemBase() {
            @Override
            public void periodic() {
                x = 5;
            }
        });

        CommandScheduler.getInstance().run();
        assertEquals(5, x);
        CommandScheduler.getInstance().reset();
    }

}
