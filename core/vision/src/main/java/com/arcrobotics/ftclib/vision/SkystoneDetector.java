package com.arcrobotics.ftclib.vision;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.openftc.easyopencv.OpenCvCamera;
import org.openftc.easyopencv.OpenCvCameraFactory;
import org.openftc.easyopencv.OpenCvCameraRotation;
import org.openftc.easyopencv.OpenCvInternalCamera;

/**
 * This is an example of how to use the ExamplePipeLine
 * For more examples such as using a webcam and switching between different cameras
 * visit https://github.com/OpenFTC/EasyOpenCV/tree/master/examples/src/main/java/org/openftc/easyopencv/examples
 */
public class SkystoneDetector extends LinearOpMode
{
  OpenCvCamera phoneCam;
  SkystoneDetectorPipeline visionPipeLine;

  @Override
  public void runOpMode()
  {
    /*
     * Instantiate an OpenCvCamera object for the camera we'll be using.
     * In this sample, we're using the phone's internal camera. We pass it a
     * CameraDirection enum indicating whether to use the front or back facing
     * camera, as well as the view that we wish to use for camera monitor (on
     * the RC phone). If no camera monitor is desired, use the alternate
     * single-parameter constructor instead (commented out below)
     */
    int cameraMonitorViewId = hardwareMap.appContext.getResources().getIdentifier("cameraMonitorViewId", "id", hardwareMap.appContext.getPackageName());
    phoneCam = OpenCvCameraFactory.getInstance().createInternalCamera(OpenCvInternalCamera.CameraDirection.BACK, cameraMonitorViewId);

    // OR...  Do Not Activate the Camera Monitor View
    //phoneCam = OpenCvCameraFactory.getInstance().createInternalCamera(OpenCvInternalCamera.CameraDirection.BACK);

    /*
     * Open the connection to the camera device
     */
    phoneCam.openCameraDevice();

    /*
     * Specify the image processing SkystoneDetectorPipeline we wish to invoke upon receipt
     * of a frame from the camera. Note that switching pipelines on-the-fly
     * (while a streaming session is in flight) *IS* supported.
     */
    visionPipeLine = new SkystoneDetectorPipeline();

    phoneCam.setPipeline(visionPipeLine);

    /*
     * Tell the camera to start streaming images to us! Note that you must make sure
     * the resolution you specify is supported by the camera. If it is not, an exception
     * will be thrown.
     *
     * Also, we specify the rotation that the camera is used in. This is so that the image
     * from the camera sensor can be rotated such that it is always displayed with the image upright.
     * For a front facing camera, rotation is defined assuming the user is looking at the screen.
     * For a rear facing camera or a webcam, rotation is defined assuming the camera is facing
     * away from the user.
     */
    phoneCam.startStreaming(320, 240, OpenCvCameraRotation.UPRIGHT);

    /*
     * Wait for the user to press start on the Driver Station
     */
    waitForStart();

    while (opModeIsActive())
    {

      //print out which stone is the skystone
      telemetry.addData("Skystone Position", visionPipeLine.getSkystonePosition());
      telemetry.update();


    }
  }

}
