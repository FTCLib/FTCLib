package com.example.ftclibexamples.VisionSample;

import com.arcrobotics.ftclib.vision.AprilTagDetector;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import org.openftc.easyopencv.OpenCvCamera;
import org.openftc.easyopencv.OpenCvCameraRotation;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

public class AprilTagVisionSample extends LinearOpMode {

    AprilTagDetector aprilTagDetector;
    OpenCvCamera camera;

    @Override
    public void runOpMode() throws InterruptedException {

        DecimalFormat df = new DecimalFormat("0.00");
        List<Integer> targets;

        aprilTagDetector = new AprilTagDetector(hardwareMap);
        /* Custom camera example:
         * new AprilTagDetector(hardwareMap, "camera")
         */

        // Make sure the settings below are configured before initializing the detector
        aprilTagDetector.WIDTH = 1280;
        aprilTagDetector.HEIGHT = 720;
        aprilTagDetector.ORIENTATION = OpenCvCameraRotation.SIDEWAYS_LEFT;
        aprilTagDetector.GPU_ENABLED = true;

        aprilTagDetector.init();
        aprilTagDetector.setTargets(0, 1, 2);
        camera = aprilTagDetector.getCamera();

        waitForStart();

        while (opModeIsActive() && !isStopRequested()) {
            Map<String, Integer> detection = aprilTagDetector.getDetection();
            targets = aprilTagDetector.getTargets();

            telemetry.addLine("Camera FPS: " + df.format(camera.getFps()));
            telemetry.addLine("Max theoretical FPS: " + df.format(camera.getCurrentPipelineMaxFps()));
            telemetry.addLine("Current Targets: " + (targets != null ? targets.toString() : "None"));

            if (detection != null) {
                telemetry.addLine("Detection ID: " + detection.get("id"));
                telemetry.addLine("Detection center X: " + detection.get("x"));
                telemetry.addLine("Detection center Y: " + detection.get("y"));
            }

            telemetry.update();
        }
        aprilTagDetector.close();
    }
}
