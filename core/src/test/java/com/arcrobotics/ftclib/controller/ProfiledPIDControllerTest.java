package com.arcrobotics.ftclib.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.arcrobotics.ftclib.controller.wpilibcontroller.ProfiledPIDController;
import com.arcrobotics.ftclib.trajectory.TrapezoidProfile;
import org.junit.jupiter.api.Test;

public class ProfiledPIDControllerTest {
  @Test
  void testStartFromNonZeroPosition() {
    ProfiledPIDController controller =
        new ProfiledPIDController(1.0, 0.0, 0.0, new TrapezoidProfile.Constraints(1.0, 1.0));

    controller.reset(20);

    assertEquals(0, controller.calculate(20, 20), 0.05);
  }
}
