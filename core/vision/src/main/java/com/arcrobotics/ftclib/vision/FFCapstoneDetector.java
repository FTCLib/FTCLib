package com.arcrobotics.ftclib.vision;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.opencv.core.Scalar;
import org.openftc.easyopencv.OpenCvCamera;
import org.openftc.easyopencv.OpenCvCameraFactory;
import org.openftc.easyopencv.OpenCvCameraRotation;
import org.openftc.easyopencv.OpenCvInternalCamera;

public class FFCapstoneDetector {

    private OpenCvCamera camera;
    private boolean isUsingWebcam;
    private String cameraName;
    private FFCapstonePipeline capstonePipeline;
    private final HardwareMap hardwareMap;
    private int WIDTH = 432;
    private int HEIGHT = 240;

    private DetectorState detectorState = DetectorState.NOT_CONFIGURED;

    private final Object sync = new Object();

    public FFCapstoneDetector(HardwareMap hMap) {
        hardwareMap = hMap;
    }

    public FFCapstoneDetector(HardwareMap hMap, String camName) {
        hardwareMap = hMap;
        cameraName = camName;
        isUsingWebcam = true;
    }

    public FFCapstoneDetector(HardwareMap hMap, String camName, int width, int height) {
        hardwareMap = hMap;
        cameraName = camName;
        WIDTH = width;
        HEIGHT = height;
        isUsingWebcam = true;
    }

    public DetectorState getDetectorState() {
        synchronized (sync) {
            return detectorState;
        }
    }

    public void init() {
        synchronized (sync) {
            if (detectorState == DetectorState.NOT_CONFIGURED) {
                //This will instantiate an OpenCvCamera object for the camera we'll be using
                int cameraMonitorViewId = hardwareMap
                        .appContext.getResources()
                        .getIdentifier("cameraMonitorViewId", "id", hardwareMap.appContext.getPackageName());
                if (isUsingWebcam) {
                    camera = OpenCvCameraFactory.getInstance()
                            .createWebcam(hardwareMap.get(WebcamName.class, cameraName), cameraMonitorViewId);
                } else {
                    camera = OpenCvCameraFactory.getInstance()
                            .createInternalCamera(OpenCvInternalCamera.CameraDirection.BACK, cameraMonitorViewId);
                }

                //Set the pipeline the camera should use and start streaming
                camera.setPipeline(capstonePipeline = new FFCapstonePipeline());


                detectorState = DetectorState.INITIALIZING;

                camera.openCameraDeviceAsync(new OpenCvCamera.AsyncCameraOpenListener() {
                    @Override
                    public void onOpened() {

                        camera.startStreaming(WIDTH, HEIGHT, OpenCvCameraRotation.UPRIGHT);

                        synchronized (sync) {
                            detectorState = DetectorState.RUNNING;
                        }
                    }

                    @Override
                    public void onError(int errorCode) {

                        synchronized (sync) {
                            detectorState = DetectorState.INIT_FAILURE_NOT_RUNNING; //Set our state
                        }

                        RobotLog.addGlobalWarningMessage("Warning: Camera device failed to open with EasyOpenCv error: " +
                                ((errorCode == -1) ? "CAMERA_OPEN_ERROR_FAILURE_TO_OPEN_CAMERA_DEVICE" : "CAMERA_OPEN_ERROR_POSTMORTEM_OPMODE")
                        ); //Warn the user about the issue
                    }
                });
            }
        }
    }

    public OpenCvCamera getCamera() {
        return camera;
    }

    public void setLowerBound(Scalar low) {
        capstonePipeline.setLowerBound(low);
    }

    public void setUpperBound(Scalar high) {
        capstonePipeline.setUpperBound(high);
    }

    public void setLowerAndUpperBounds(Scalar low, Scalar high) {
        capstonePipeline.setLowerAndUpperBounds(low, high);
    }

    public Placement getPlacement() {
        if (capstonePipeline.getCentroid() != null) {
            if (capstonePipeline.getCentroid().x > WIDTH * 2 / 3.0)
                return Placement.RIGHT;
            else if (capstonePipeline.getCentroid().x < WIDTH / 3.0)
                return Placement.LEFT;
        }
        return Placement.CENTER;
    }

    public enum Placement {
        LEFT,
        RIGHT,
        CENTER
    }

}