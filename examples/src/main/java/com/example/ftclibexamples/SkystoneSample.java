package com.example.ftclibexamples;

import com.arcrobotics.ftclib.command.old.CommandOpMode;
import com.arcrobotics.ftclib.vision.SkystoneDetectorPipeline;

import org.openftc.easyopencv.OpenCvCamera;
import org.openftc.easyopencv.OpenCvCameraFactory;
import org.openftc.easyopencv.OpenCvCameraRotation;
import org.openftc.easyopencv.OpenCvInternalCamera;

public class SkystoneSample extends CommandOpMode {

    OpenCvCamera camera;
    SkystoneDetectorPipeline pipeline;
    @Override
    public void initialize() {

        int cameraMonitorViewId = hardwareMap.appContext.getResources().getIdentifier("cameraMonitorViewId", "id", hardwareMap.appContext.getPackageName());
        camera = OpenCvCameraFactory.getInstance().createInternalCamera(OpenCvInternalCamera.CameraDirection.BACK, cameraMonitorViewId);
        camera.openCameraDevice();

        pipeline = new SkystoneDetectorPipeline();

        camera.setPipeline(pipeline);
        camera.startStreaming(640, 480, OpenCvCameraRotation.UPRIGHT);
    }

    @Override
    public void run() {
        // Assuming threaded. It hopefully found the skystone at the end of init.
        SkystoneDetectorPipeline.SkystonePosition position = pipeline.getSkystonePosition();

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
