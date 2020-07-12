package org.firstinspires.ftc.teamcode.OpModes;


import com.arcrobotics.ftclib.command.old.CommandOpMode;
import com.arcrobotics.ftclib.vision.SkystoneDetector;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.openftc.easyopencv.OpenCvCamera;
import org.openftc.easyopencv.OpenCvCameraFactory;
import org.openftc.easyopencv.OpenCvCameraRotation;

@Autonomous(name="Sample Skystone Detection OpMode")
public class SkystoneTest extends CommandOpMode {

    OpenCvCamera camera;
    SkystoneDetector pipeline;
    @Override
    public void initialize() {

        int cameraMonitorViewId = hardwareMap.appContext.getResources().getIdentifier("cameraMonitorViewId", "id", hardwareMap.appContext.getPackageName());
        camera = OpenCvCameraFactory.getInstance().createWebcam(hardwareMap.get(WebcamName.class, "Webcam 1"), cameraMonitorViewId);
        camera.openCameraDevice();

        pipeline = new SkystoneDetector(10, 30, 50, 50);

        camera.setPipeline(pipeline);
        camera.startStreaming(640, 480, OpenCvCameraRotation.UPRIGHT);
    }

    @Override
    public void run() {
        // Assuming threaded. It hopefully found the skystone at the end of init.
        SkystoneDetector.SkystonePosition position = pipeline.getSkystonePosition();

        switch (position) {
            case LEFT_STONE:
                break;
            case CENTER_STONE:
                break;
            case RIGHT_STONE:
                break;
            default:
                break;
        }
    }
}

