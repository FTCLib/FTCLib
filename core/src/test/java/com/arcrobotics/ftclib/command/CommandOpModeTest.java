package com.arcrobotics.ftclib.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class CommandOpModeTest extends CommandOpMode {

  public static int x = 3;

  @Override
  public void initialize() {
    x = 3;
    Robot.enable();
    schedule(
        new CommandBase() {
          @Override
          public void execute() {
            x = 5;
          }
        });
  }

  @Test
  public void testRunOpMode() {
    initialize();
    run();
    assertEquals(5, x);
    reset();
  }
}
