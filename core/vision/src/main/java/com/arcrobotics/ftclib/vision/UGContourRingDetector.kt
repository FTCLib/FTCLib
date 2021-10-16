package com.arcrobotics.ftclib.vision

import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.util.RobotLog
import org.firstinspires.ftc.robotcore.external.Telemetry
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName
import org.openftc.easyopencv.OpenCvCamera
import org.openftc.easyopencv.OpenCvCamera.AsyncCameraOpenListener
import org.openftc.easyopencv.OpenCvCameraFactory
import org.openftc.easyopencv.OpenCvCameraRotation
import org.openftc.easyopencv.OpenCvInternalCamera

/**
 * UGContourRingPipeline Detector Class
 *
 * @author Jaran Chao
 *
 * @constructor Constructs the Vision Detector
 *
 * @param hardwareMap variable being passed into the detector to access hardwareMap, no default value
 * @param cameraDirection direction of the camera if using phone camera (FRONT, BACK), default value
 * to BACK
 * @param telemetry If wanted, provide a telemetry object to the constructor to get stacktrace if an
 * error occurs (mainly for debug purposes), default value of null
 * @param debug If true, all intermediate calculation results (except showing mat operations) will
 * be printed to telemetry (mainly for debug purposes), default value of false
 */
class UGContourRingDetector(
        // primary constructor
        private var hardwareMap: HardwareMap, // hardwareMap variable being passed into the detector. no default value
        private var cameraDirection: OpenCvInternalCamera.CameraDirection = OpenCvInternalCamera.CameraDirection.BACK,
        private val telemetry: Telemetry? = null,
        private var debug: Boolean = false,
) {
    /**
     * Companion Object to hold all Configuration needed for the Pipeline
     *
     * @see UGContourRingPipeline.Config
     */
    companion object PipelineConfiguration {
        /** Width and Height of the camera **/
        var CAMERA_WIDTH = 320
        var CAMERA_HEIGHT = 240

        /** Horizon value in use, anything above this value (less than the value) since
         * (0, 0) is the top left of the camera frame **/
        var HORIZON = 100

        /** Value storing whether or not the orientation of the camera is in portrait mode **/
        var IS_PORTRAIT_MODE = false

        /** Value storing the orientation of the camera **/
        var CAMERA_ORIENTATION: OpenCvCameraRotation = OpenCvCameraRotation.UPRIGHT
    }

    private var detectorState = DetectorState.NOT_CONFIGURED

    private val sync: Any = Any()

    // camera variable, lateinit, initialized in init() function
    lateinit var camera: OpenCvCamera

    // flag to tell init() function whether or not to set up webcam
    private var isUsingWebcam = false

    // name of webcam, lateinit, initialized if webcam constructor is called otherwise will error is accessed
    private lateinit var webcamName: String

    // pipeline, lateinit, initialized in the init() function after creating of UGContourPipeline instance
    private lateinit var ftcLibPipeline: UGContourRingPipeline

    /**
     * @param hMap hardwareMap
     * @param webcamName name of webcam in use
     */
    constructor(hMap: HardwareMap, webcamName: String) : this(hardwareMap = hMap) {
        this.webcamName = webcamName
        this.isUsingWebcam = true
    }

    /**
     * @param hMap hardwareMap
     * @param webcamName name of webcam in use
     * @param telemetry If wanted, provide a telemetry object to the constructor to get stacktrace if an
     * error occurs (mainly for debug purposes), default value of null
     * @param debug If true, all intermediate calculation results (except showing mat operations) will
     * be printed to telemetry (mainly for debug purposes), default value of false
     */
    constructor(hMap: HardwareMap, webcamName: String, telemetry: Telemetry, debug: Boolean) : this(hardwareMap = hMap, telemetry = telemetry, debug = debug) {
        this.webcamName = webcamName
        this.isUsingWebcam = true
    }

    // returns the height detected by the pipeline
    val height: UGContourRingPipeline.Height
        get() = ftcLibPipeline.height

    fun getDetectorState(): DetectorState {
        synchronized(sync) {
            return detectorState
        }
    }

    // init function to initialize camera and pipeline
    fun init() {
        synchronized(sync, ::init)
        if(detectorState == DetectorState.NOT_CONFIGURED) {
            val cameraMonitorViewId = hardwareMap
                    .appContext
                    .resources
                    .getIdentifier(
                            "cameraMonitorViewId",
                            "id",
                            hardwareMap.appContext.packageName,
                    )
            camera = if (isUsingWebcam) {
                OpenCvCameraFactory
                        .getInstance()
                        .createWebcam(
                                hardwareMap.get(
                                        WebcamName::class.java,
                                        webcamName
                                ),
                                cameraMonitorViewId,
                        )
            } else {
                OpenCvCameraFactory
                        .getInstance()
                        .createInternalCamera(
                                cameraDirection, cameraMonitorViewId,
                        )
            }

            camera.setPipeline(
                    UGContourRingPipeline(
                            telemetry = telemetry,
                            debug = debug,
                    ).apply {
                        UGContourRingPipeline.CAMERA_WIDTH = if (IS_PORTRAIT_MODE) CAMERA_HEIGHT else CAMERA_WIDTH
                        UGContourRingPipeline.HORIZON = HORIZON
                        ftcLibPipeline = this
                    }
            )


            detectorState = DetectorState.INITIALIZING

            camera.openCameraDeviceAsync(object : AsyncCameraOpenListener {
                override fun onOpened() {
                    camera.startStreaming(
                            CAMERA_WIDTH,
                            CAMERA_HEIGHT,
                            CAMERA_ORIENTATION,
                    )

                    synchronized(sync){
                        detectorState = DetectorState.RUNNING;
                    }
                }

                override fun onError(errorCode: Int) {
                    synchronized(sync) {
                        detectorState = DetectorState.INIT_FAILURE_NOT_RUNNING //Set our state
                    }

                        RobotLog.addGlobalWarningMessage("Warning: Camera device failed to open with EasyOpenCv error: " +
                                if (errorCode == -1) "CAMERA_OPEN_ERROR_FAILURE_TO_OPEN_CAMERA_DEVICE" else "CAMERA_OPEN_ERROR_POSTMORTEM_OPMODE"
                        ) //Warn the user about the issue

                }
            })
        }
    }

}