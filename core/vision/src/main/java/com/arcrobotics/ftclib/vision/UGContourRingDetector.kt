package com.arcrobotics.ftclib.vision

import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.Telemetry
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName
import org.openftc.easyopencv.OpenCvCamera
import org.openftc.easyopencv.OpenCvCameraFactory
import org.openftc.easyopencv.OpenCvCameraRotation
import org.openftc.easyopencv.OpenCvInternalCamera


class UGContourRingDetector(
        private var hardwareMap: HardwareMap,
        private val telemetry: Telemetry? = null,
        private var debug: Boolean = false,
) {
    
    companion object PipelineConfiguration {
        var CAMERA_WIDTH = 320
        var CAMERA_HEIGHT = 240

        var HORIZON = 100
    }

    lateinit var camera: OpenCvCamera
    private var isUsingWebcam = false
    private lateinit var webcamName: String
    private lateinit var ftcLibPipeline: UGContourRingPipeline

    constructor(hMap: HardwareMap, webcamName: String): this(hMap) {
        this.webcamName = webcamName
        this.isUsingWebcam = true
    }

    constructor(hMap: HardwareMap, webcamName: String, telemetry: Telemetry, debug: Boolean): this(hMap, telemetry, debug) {
        this.webcamName = webcamName
        this.isUsingWebcam = true
    }

    val height: UGContourRingPipeline.Height
        get() = ftcLibPipeline.height

    fun init() {
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
                            OpenCvInternalCamera.CameraDirection.BACK, cameraMonitorViewId,
                    )
        }

        camera.setPipeline(
                UGContourRingPipeline(
                        telemetry = telemetry,
                        debug = debug,
                ).apply {
                    UGContourRingPipeline.CAMERA_WIDTH = CAMERA_WIDTH
                    UGContourRingPipeline.HORIZON = HORIZON
                    ftcLibPipeline = this
                }
        )

        camera.openCameraDeviceAsync {
            camera.startStreaming(320,
                    240,
                    OpenCvCameraRotation.UPRIGHT,
            )
        }
    }

}