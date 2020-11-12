package com.arcrobotics.ftclib.command;

import com.arcrobotics.ftclib.command.button.Trigger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CommandSchedulerTests {

    public static int x = 3;
    private boolean val = false;

    @Test
    public void testAddCommand() {
        x = 3;
        Robot.enable();
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
        Robot.disable();
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
        Robot.disable();
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
        Robot.enable();
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

    @Test
    public void pollButtons() {
        x = 3;
        Robot.enable();
        Trigger button = new Trigger(this::getValue).whenActive(new CommandBase(){
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
        updateValue();
        CommandScheduler.getInstance().run();
        assertEquals(5, x);
        CommandScheduler.getInstance().reset();
    }

    public boolean getValue() {
        return val;
    }

    private void updateValue() {
        val = !val;
    }

}
