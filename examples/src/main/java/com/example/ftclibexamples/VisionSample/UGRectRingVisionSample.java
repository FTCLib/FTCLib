package com.example.ftclibexamples.VisionSample;

import com.arcrobotics.ftclib.vision.UGRectDetector;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

public class UGRectRingVisionSample extends LinearOpMode {

    UGRectDetector UGRectDetector;

    @Override
    public void runOpMode() {
        UGRectDetector = new UGRectDetector(hardwareMap);
        UGRectDetector.init();

        waitForStart();

        while (!isStopRequested() && opModeIsActive()) {
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
            telemetry.addData("Rings", stack);
        }
    }

}
