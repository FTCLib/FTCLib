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

/**
 * <p>Detection wrapper for the AprilTag2dPipeline.</p>
 * <p>Customizable parameters include: camera width, height, orientation, and whether to use the GPU.</p>
 */
public class AprilTagDetector {

    private OpenCvCamera camera;
    private AprilTag2dPipeline apriltagPipeline;
    private final HardwareMap hardwareMap;

    public int WIDTH = 640;
    public int HEIGHT = 480;
    public boolean GPU_ENABLED = false;
    public OpenCvCameraRotation ORIENTATION = OpenCvCameraRotation.UPRIGHT;

    private boolean isUsingWebcam;
    private String cameraName;

    private DetectorState detectorState = DetectorState.NOT_CONFIGURED;
    private final Object sync = new Object();

    private List<Integer> targetIds = new ArrayList<>();

    /**
     * Creates a new AprilTagDetector instance.
     *
     * @param hMap The hardware map
     */
    public AprilTagDetector(HardwareMap hMap) {
        hardwareMap = hMap;
    }

    /**
     * Creates a new AprilTagDetector instance.
     *
     * @param hMap    The hardware map
     * @param camName The name of the webcam, if using one
     */
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

    /**
     * <p>Initializes the camera and loads the pipeline.</p>
     * <p>The customizations of the camera must be set before calling this method.</p>
     */
    public void init() {

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

                if (GPU_ENABLED) {
                    camera.setViewportRenderer(OpenCvCamera.ViewportRenderer.GPU_ACCELERATED);
                }
                camera.showFpsMeterOnViewport(false);
                camera.openCameraDeviceAsync(new OpenCvCamera.AsyncCameraOpenListener() {

                    @Override
                    public void onOpened() {
                        synchronized (sync) {
                            detectorState = DetectorState.RUNNING;
                            sync.notifyAll();
                        }

                        camera.startStreaming(WIDTH, HEIGHT, ORIENTATION);
                    }

                    @Override
                    public void onError(int errorCode) {
                        synchronized (sync) {
                            detectorState = DetectorState.INIT_FAILURE_NOT_RUNNING;
                            sync.notifyAll();
                        }
                        RobotLog.setGlobalErrorMsg("Camera device failed to open with OpenCV error " + errorCode);
                    }
                });
            }
        }
    }

    /**
     * <p>Closes the camera device and stops the detector. Runs by default at the end of the OpMode.</p>
     * <p>This method is <strong>synchronous</strong>, meaning it will block the thread until the camera is closed.</p>
     */

    public void close() {
        synchronized (sync) {
            if (detectorState != DetectorState.RUNNING) {
                return;
            }
        }

        camera.closeCameraDevice();
        synchronized (sync) {
            detectorState = DetectorState.NOT_CONFIGURED;
        }
    }

    /**
     * <p>Closes the camera device and stops the detector.</p>
     * <p>This method is <strong>asynchronous</strong>, meaning it will not block the thread until the camera is closed.</p>
     */
    public void closeAsync() {
        synchronized (sync) {
            if (detectorState != DetectorState.RUNNING) {
                return;
            }
        }

        camera.closeCameraDeviceAsync(new OpenCvCamera.AsyncCameraCloseListener() {
            @Override
            public void onClose() {
                synchronized (sync) {
                    detectorState = DetectorState.NOT_CONFIGURED;
                }
            }
        });
    }

    /**
     * Gets the target AprilTags.
     *
     * @return A list of the target AprilTag IDs. Null if no target AprilTags have been set.
     */
    @Nullable
    public List<Integer> getTargets() {
        synchronized (sync) {
            if (targetIds.isEmpty()) {
                return null;
            } else {
                return targetIds;
            }
        }
    }

    /**
     * Sets the target AprilTags.
     *
     * @param targets The IDs of the target AprilTags
     */
    public void setTargets(@NonNull Integer... targets) {
        targetIds = Arrays.asList(targets);
    }

    /**
     * <p>Gets the latest detection data from the AprilTagDetector.</p>
     * <p>Shows a warning if no target AprilTags have been set.</p>
     *
     * @return A map containing the ID, x, and y coordinates of the first target AprilTag detected. Null if no AprilTags are detected.
     */
    @Nullable
    public Map<String, Integer> getDetection() {
        synchronized (sync) {
            if (detectorState != DetectorState.RUNNING) {
                try {
                    sync.wait();
                } catch (InterruptedException e) {
                    RobotLog.setGlobalErrorMsg("AprilTag Error: Camera is not running.");
                    return null;
                }
            }
        }

        ArrayList<AprilTagDetection> detections = apriltagPipeline.getLatestDetections();
        Map<String, Integer> detection_data = new HashMap<>();

        if (targetIds.isEmpty()) {
            RobotLog.addGlobalWarningMessage("AprilTag Warning: No target AprilTags have been set.");
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

    /**
     * Gets the camera used by the AprilTagDetector.
     *
     * @return The camera object. Null if the detector is not running.
     */
    @Nullable
    public OpenCvCamera getCamera() {
        synchronized (sync) {
            if (detectorState != DetectorState.RUNNING) {
                try {
                    sync.wait();
                } catch (InterruptedException e) {
                    RobotLog.setGlobalErrorMsg("AprilTag Error: Camera is not running.");
                    return null;
                }
            }
            return camera;
        }
    }
}