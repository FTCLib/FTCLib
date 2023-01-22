package com.example.ftclibexamples.VisionSample;

import com.arcrobotics.ftclib.vision.AprilTagDetector;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import org.openftc.easyopencv.OpenCvCamera;

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

        aprilTagDetector = new AprilTagDetector(hardwareMap, 1280, 720);
        /* Custom camera example:
         * new AprilTagDetector(hardwareMap, "camera")
         * or new AprilTagDetector(hardwareMap, "camera", 432, 240)
         */

        aprilTagDetector.init();
        /* NOTE: Default orientation is UPRIGHT.
         * Custom orientation example:
         * aprilTagDetector.init(OpenCvCameraRotation.SIDEWAYS_LEFT)
         */
        aprilTagDetector.setTargets(0, 1, 2);
        camera = aprilTagDetector.getCamera();

        waitForStart();

        while (opModeIsActive() && !isStopRequested()) {
            Map<String, Integer> detection = aprilTagDetector.getDetection();
            targets = aprilTagDetector.getTargets();

            telemetry.addLine("Camera FPS: " + df.format(camera.getFps()));
            telemetry.addLine("Current Targets: " + (targets != null ? targets.toString() : "None"));

            if (detection != null) {
                telemetry.addLine("Detection ID: " + detection.get("id"));
                telemetry.addLine("Detection center X: " + detection.get("x"));
                telemetry.addLine("Detection center Y: " + detection.get("y"));
            }

            telemetry.update();
        }
    }
}
