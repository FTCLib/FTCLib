package com.example.ftclibexamples.VisionSample

import com.arcrobotics.ftclib.vision.UGContourRingPipeline
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName
import org.openftc.easyopencv.*

class UGContourRingPipelineKtExample : LinearOpMode() {
    companion object {
        val CAMERA_WIDTH = 320 // width  of wanted camera resolution
        val CAMERA_HEIGHT = 240 // height of wanted camera resolution

        val HORIZON = 100 // horizon value to tune

        val DEBUG = false // if debug is wanted, change to true

        val USING_WEBCAM = false // change to true if using webcam
        val WEBCAM_NAME = "" // insert webcam name from configuration if using webcam
    }

    private lateinit var pipeline: UGContourRingPipeline
    private var camera: OpenCvCamera = if (USING_WEBCAM)
        configureWebCam()
    else
        configurePhoneCamera()

    private val cameraMonitorViewId: Int = hardwareMap
            .appContext
            .resources
            .getIdentifier(
                    "cameraMonitorViewId",
                    "id",
                    hardwareMap.appContext.packageName,
            )

    private fun configurePhoneCamera(): OpenCvInternalCamera = OpenCvCameraFactory.getInstance()
            .createInternalCamera(
                    OpenCvInternalCamera.CameraDirection.BACK, cameraMonitorViewId,
            )

    private fun configureWebCam(): OpenCvWebcam = OpenCvCameraFactory.getInstance().createWebcam(
            hardwareMap.get(
                    WebcamName::class.java,
                    WEBCAM_NAME
            ),
            cameraMonitorViewId,
    )

    override fun runOpMode() {
        camera.setPipeline(UGContourRingPipeline(telemetry, DEBUG).apply { pipeline = this })

        UGContourRingPipeline.Config.CAMERA_WIDTH = CAMERA_WIDTH

        UGContourRingPipeline.Config.HORIZON = HORIZON

        camera.openCameraDeviceAsync(object : OpenCvCamera.AsyncCameraOpenListener {
            override fun onOpened() {
                camera.startStreaming(
                        CAMERA_WIDTH,
                        CAMERA_HEIGHT,
                        OpenCvCameraRotation.UPRIGHT,
                )
            }

            override fun onError(errorCode: Int) {

            }
        })

        waitForStart()

        while (opModeIsActive()) {
            telemetry.addData("[Ring Stack] >>", "[HEIGHT] ${pipeline.height}")
            telemetry.update()
        }
    }

}