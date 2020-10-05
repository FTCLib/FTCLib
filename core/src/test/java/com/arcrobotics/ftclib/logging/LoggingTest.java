package com.arcrobotics.ftclib.logging;

import com.arcrobotics.ftclib.hardware.motors.CRServo;
import com.technototes.logger.Log;

public class LoggingTest {
    @Log(name = "servo")
    public CRServo servo;
}
