package com.example.ftclibexamples;

import com.arcrobotics.ftclib.command.CommandOpMode;
import com.arcrobotics.ftclib.vision.UGRectDetector;

public class UGRectRingVisionSample extends CommandOpMode {
    UGRectDetector UGRectDetector;
    @Override
    public void initialize() {
        UGRectDetector = new UGRectDetector(hardwareMap);
        UGRectDetector.init();
    }

    @Override
    public void run() {
        // Assuming threaded. It hopefully found the stack at the end of init.
        UGRectDetector.Stack stack = UGRectDetector.getStack();

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
