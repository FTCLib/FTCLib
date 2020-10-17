package com.example.ftclibexamples;

import com.arcrobotics.ftclib.command.CommandOpMode;
import com.arcrobotics.ftclib.vision.UltimateGoalDetector;

public class UltimateGoalVisionSample extends CommandOpMode {
    UltimateGoalDetector ultimateGoalDetector;
    @Override
    public void initialize() {
        ultimateGoalDetector = new UltimateGoalDetector(hardwareMap);
        ultimateGoalDetector.init();
    }

    @Override
    public void run() {
        // Assuming threaded. It hopefully found the stack at the end of init.
        UltimateGoalDetector.Stack stack = ultimateGoalDetector.getStack();

        switch (stack) {
            case ZERO:
                break;
            case ONE:
                break;
            case FOUR:
                break;
            default:
                break;
        }
    }
}
