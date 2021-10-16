package com.arcrobotics.ftclib.vision;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.openftc.easyopencv.OpenCvCamera;
import org.openftc.easyopencv.OpenCvCameraFactory;
import org.openftc.easyopencv.OpenCvCameraRotation;
import org.openftc.easyopencv.OpenCvInternalCamera;

public class UGRectDetector {

    private OpenCvCamera camera;
    private boolean isUsingWebcam;
    private String webcamName;
    private final HardwareMap hardwareMap;
    private UGRectRingPipeline ftclibPipeline;

    public static int CAMERA_WIDTH = 320, CAMERA_HEIGHT = 240;
    public static OpenCvCameraRotation ORIENTATION = OpenCvCameraRotation.UPRIGHT;

    private DetectorState detectorState = DetectorState.NOT_CONFIGURED;

    //Lock for the camera device opening callback
    private final Object sync = new Object();

    // The constructor is overloaded to allow the use of webcam instead of the phone camera
    public UGRectDetector(HardwareMap hMap) {
        hardwareMap = hMap;
    }

    public UGRectDetector(HardwareMap hMap, String webcamName) {
        hardwareMap = hMap;
        isUsingWebcam = true;
        this.webcamName = webcamName;
    }

    public DetectorState getDetectorState(){
        synchronized (sync){
            return detectorState;
        }
    }


    public void init() {
        synchronized (sync) {
            if(detectorState == DetectorState.NOT_CONFIGURED) {
                //This will instantiate an OpenCvCamera object for the camera we'll be using
                int cameraMonitorViewId = hardwareMap
                        .appContext.getResources()
                        .getIdentifier("cameraMonitorViewId", "id", hardwareMap.appContext.getPackageName());
                if (isUsingWebcam) {
                    camera = OpenCvCameraFactory.getInstance()
                            .createWebcam(hardwareMap.get(WebcamName.class, webcamName), cameraMonitorViewId);
                } else {
                    camera = OpenCvCameraFactory.getInstance()
                            .createInternalCamera(OpenCvInternalCamera.CameraDirection.BACK, cameraMonitorViewId);
                }

                //Set the pipeline the camera should use and start streaming
                camera.setPipeline(ftclibPipeline = new UGRectRingPipeline());


                detectorState = DetectorState.INITIALIZING;

                camera.openCameraDeviceAsync(new OpenCvCamera.AsyncCameraOpenListener() {
                    @Override
                    public void onOpened() {

                        camera.startStreaming(CAMERA_WIDTH, CAMERA_HEIGHT, ORIENTATION);

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

    public void setTopRectangle(double topRectHeightPercentage, double topRectWidthPercentage) {
        ftclibPipeline.setTopRectHeightPercentage(topRectHeightPercentage);
        ftclibPipeline.setTopRectWidthPercentage(topRectWidthPercentage);
    }

    public void setBottomRectangle(double bottomRectHeightPercentage, double bottomRectWidthPercentage) {
        ftclibPipeline.setBottomRectHeightPercentage(bottomRectHeightPercentage);
        ftclibPipeline.setBottomRectWidthPercentage(bottomRectWidthPercentage);
    }

    public void setRectangleSize(int rectangleWidth, int rectangleHeight) {
        ftclibPipeline.setRectangleHeight(rectangleHeight);
        ftclibPipeline.setRectangleWidth(rectangleWidth);
    }

    public Stack getStack() {
        if (Math.abs(ftclibPipeline.getTopAverage() - ftclibPipeline.getBottomAverage()) < ftclibPipeline.getThreshold()
                && (ftclibPipeline.getTopAverage() <= 100 && ftclibPipeline.getBottomAverage() <= 100)) {
            return Stack.FOUR;
        } else if (Math.abs(ftclibPipeline.getTopAverage() - ftclibPipeline.getBottomAverage()) < ftclibPipeline.getThreshold()
                && (ftclibPipeline.getTopAverage() >= 100 && ftclibPipeline.getBottomAverage() >= 100)) {
            return Stack.ZERO;
        } else {
            return Stack.ONE;
        }
    }

    public void setThreshold(int threshold) {
        ftclibPipeline.setThreshold(threshold);
    }

    public double getTopAverage() {
        return ftclibPipeline.getTopAverage();
    }

    public double getBottomAverage() {
        return ftclibPipeline.getBottomAverage();
    }

    public enum Stack {
        ZERO,
        ONE,
        FOUR,
    }

}
