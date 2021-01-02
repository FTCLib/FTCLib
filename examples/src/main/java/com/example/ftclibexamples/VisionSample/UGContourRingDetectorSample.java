package com.example.ftclibexamples.VisionSample;

import com.arcrobotics.ftclib.vision.UGContourRingDetector;
import com.arcrobotics.ftclib.vision.UGContourRingPipeline;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import org.openftc.easyopencv.OpenCvInternalCamera;

@Autonomous
@Disabled
public class UGContourRingDetectorSample extends LinearOpMode {

    private UGContourRingDetector detector;

    private static final boolean USING_WEBCAM = false;
    private static final double CAMERA_WIDTH = 320;
    private static final double CAMERA_HEIGHT = 240;

    @Override
    public void runOpMode() throws InterruptedException {
        if (!USING_WEBCAM) {
            detector = new UGContourRingDetector(
                    hardwareMap, OpenCvInternalCamera.CameraDirection.BACK,
                    telemetry, true
            );
        } else {
            detector = new UGContourRingDetector(
                    hardwareMap, "webcam", telemetry, true
            );
        }

        detector.init();
        waitForStart();

        UGContourRingPipeline.Height height = detector.getHeight();

        while (!isStopRequested() && opModeIsActive()) {
            switch (height) {
                case ZERO:
                    break;
                case ONE:
                    break;
                case FOUR:
                    break;
            }
            height = detector.getHeight();
        }
    }

}
