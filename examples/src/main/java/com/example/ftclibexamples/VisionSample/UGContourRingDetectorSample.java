package com.example.ftclibexamples.VisionSample;

import com.arcrobotics.ftclib.vision.UGContourRingDetector;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import org.openftc.easyopencv.OpenCvInternalCamera;

public class UGContourRingDetectorSample extends LinearOpMode {

    private UGContourRingDetector detector;

    private static final boolean USING_WEBCAM = false;

    @Override
    public void runOpMode() throws InterruptedException {
        detector = new UGContourRingDetector(
                hardwareMap, OpenCvInternalCamera.CameraDirection.BACK,
                telemetry, true
        );
    }

}
