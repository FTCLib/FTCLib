package com.arcrobotics.ftclib.vision;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.openftc.apriltag.AprilTagDetection;
import org.openftc.easyopencv.OpenCvCamera;
import org.openftc.easyopencv.OpenCvCameraFactory;
import org.openftc.easyopencv.OpenCvCameraRotation;
import org.openftc.easyopencv.OpenCvInternalCamera;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AprilTagDetector {

    private OpenCvCamera camera;
    private AprilTag2dPipeline apriltagPipeline;
    private final HardwareMap hardwareMap;

    private int WIDTH = 640;
    private int HEIGHT = 480;

    private boolean isUsingWebcam;
    private String cameraName;

    private DetectorState detectorState = DetectorState.NOT_CONFIGURED;
    private final Object sync = new Object();

    private List<Integer> targetIds = new ArrayList<>();

    public AprilTagDetector(HardwareMap hMap, String camName, int width, int height) {
        hardwareMap = hMap;
        cameraName = camName;
        WIDTH = width;
        HEIGHT = height;
        isUsingWebcam = true;
    }

    public AprilTagDetector(HardwareMap hMap, int width, int height) {
        hardwareMap = hMap;
        WIDTH = width;
        HEIGHT = height;
    }

    public AprilTagDetector(HardwareMap hMap) {
        hardwareMap = hMap;
    }

    public AprilTagDetector(HardwareMap hMap, String camName) {
        hardwareMap = hMap;
        cameraName = camName;
        isUsingWebcam = true;
    }

    public DetectorState getDetectorState() {
        synchronized (sync) {
            return detectorState;
        }
    }

    public void init(OpenCvCameraRotation orientation) {
        synchronized (sync) {

            // Get camera instance
            if (detectorState == DetectorState.NOT_CONFIGURED) {
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

                // Instantiate pipeline and start streaming
                apriltagPipeline = new AprilTag2dPipeline();
                camera.setPipeline(apriltagPipeline);
                detectorState = DetectorState.INITIALIZING;

                camera.openCameraDeviceAsync(new OpenCvCamera.AsyncCameraOpenListener() {
                    @Override
                    public void onOpened() {
                        camera.startStreaming(WIDTH, HEIGHT, orientation);
                        synchronized (sync) {
                            detectorState = DetectorState.RUNNING;
                        }
                    }

                    @Override
                    public void onError(int errorCode) {
                        synchronized (sync) {
                            detectorState = DetectorState.INIT_FAILURE_NOT_RUNNING;
                        }
                        RobotLog.addGlobalWarningMessage("WARNING: Camera device failed to open with OpenCV error: " + errorCode);
                    }
                });
            }
        }
    }

    public void init() {
        this.init(OpenCvCameraRotation.UPRIGHT);
    }

    public void setTargets(@NonNull Integer... targets) {
        targetIds = Arrays.asList(targets);
    }

    @Nullable
    public List<Integer> getTargets() {
        if (targetIds.isEmpty()) {
            return null;
        } else {
            return targetIds;
        }
    }

    @Nullable
    public Map<String, Integer> getDetection() {
        ArrayList<AprilTagDetection> detections = apriltagPipeline.getLatestDetections();
        Map<String, Integer> detection_data = new HashMap<>();

        if (targetIds.isEmpty()) {
            RobotLog.setGlobalErrorMsg("AprilTag Error: No target AprilTags have been set.");
            return null;
        }

        if (detections.isEmpty()) {
            return null;
        }

        for (AprilTagDetection tag : detections)
            if (targetIds.contains(tag.id)) {
                detection_data.put("id", tag.id);
                detection_data.put("x", (int) tag.center.x);
                detection_data.put("y", (int) tag.center.y);

                return detection_data;
            }

        return null;
    }

    public OpenCvCamera getCamera() {
        return camera;
    }
}
