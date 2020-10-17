package com.arcrobotics.ftclib.vision;

import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.openftc.easyopencv.OpenCvCamera;
import org.openftc.easyopencv.OpenCvCameraFactory;
import org.openftc.easyopencv.OpenCvCameraRotation;
import org.openftc.easyopencv.OpenCvInternalCamera;


public class UltimateGoalDetector {

    OpenCvCamera camera;
    private boolean isUsingWebcam;
    private String webcamName;
    HardwareMap hardwareMap;
    UltimateGoalPipeline ftclibPipeline;

    public UltimateGoalDetector(HardwareMap hMap) {
        hardwareMap = hMap;
    }

    public UltimateGoalDetector(HardwareMap hMap, String webcamName) {
        hardwareMap = hMap;
        isUsingWebcam = true;
        this.webcamName = webcamName;
    }

    public void init() {
        if (isUsingWebcam) {
            int cameraMonitorViewId = hardwareMap.appContext.getResources().getIdentifier("cameraMonitorViewId", "id", hardwareMap.appContext.getPackageName());
            camera = OpenCvCameraFactory.getInstance().createWebcam(hardwareMap.get(WebcamName.class, webcamName), cameraMonitorViewId);
        } else {
            int cameraMonitorViewId = hardwareMap.appContext.getResources().getIdentifier("cameraMonitorViewId", "id", hardwareMap.appContext.getPackageName());
            camera = OpenCvCameraFactory.getInstance().createInternalCamera(OpenCvInternalCamera.CameraDirection.BACK, cameraMonitorViewId);
        }

        camera.setPipeline(ftclibPipeline = new UltimateGoalPipeline());
        camera.openCameraDeviceAsync(new OpenCvCamera.AsyncCameraOpenListener() {
            @Override
            public void onOpened() {
                camera.startStreaming(320, 240, OpenCvCameraRotation.UPRIGHT);
            }
        });
    }

    public Stack getStack() {
        if (Math.abs(ftclibPipeline.getTopAverage() - ftclibPipeline.getBottomAverage()) < ftclibPipeline.getThreshold() && (ftclibPipeline.getTopAverage() <= 100 && ftclibPipeline.getBottomAverage() <= 100)) {
            return Stack.FOUR;
        } else if (Math.abs(ftclibPipeline.getTopAverage() - ftclibPipeline.getBottomAverage()) < ftclibPipeline.getThreshold() && (ftclibPipeline.getTopAverage() >= 100 && ftclibPipeline.getBottomAverage() >= 100)) {
            return Stack.ZERO;
        } else {
            return Stack.ONE;
        }
    }

   public enum Stack {
        ZERO,
        ONE,
        FOUR,
    }

}
