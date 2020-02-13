package org.firstinspires.ftc.teamcode.TestProject;

import com.arcrobotics.ftclib.command.CommandOpMode;
import com.arcrobotics.ftclib.gamepad.GamepadEx;
import com.arcrobotics.ftclib.hardware.SimpleServo;
import com.arcrobotics.ftclib.hardware.motors.MotorImplEx;
import com.arcrobotics.ftclib.vision.SkystoneDetector;

import org.openftc.easyopencv.OpenCvCamera;
import org.openftc.easyopencv.OpenCvCameraFactory;
import org.openftc.easyopencv.OpenCvCameraRotation;
import org.openftc.easyopencv.OpenCvInternalCamera;

@com.qualcomm.robotcore.eventloop.opmode.Autonomous(name="Test Project")  // @Autonomous(...) is the other common choice
public class Auto extends CommandOpMode {

    SkystoneDetector pipeline;
    OpenCvCamera camera;
    DriveSubsystem driveSubsystem;
    GamepadEx driverGamepad;


    @Override
    public void initialize() {
        driverGamepad = new GamepadEx(gamepad1);
        driveSubsystem = new DriveSubsystem(driverGamepad, hardwareMap, telemetry);

        driveSubsystem.initialize();

        int cameraMonitorViewId = hardwareMap.appContext.getResources().getIdentifier("cameraMonitorViewId", "id", hardwareMap.appContext.getPackageName());
        camera = OpenCvCameraFactory.getInstance().createInternalCamera(OpenCvInternalCamera.CameraDirection.BACK, cameraMonitorViewId);
        camera.openCameraDevice();

        pipeline = new SkystoneDetector(10, 40, 50, 50);

        camera.setPipeline(pipeline);
        camera.startStreaming(640, 480, OpenCvCameraRotation.UPSIDE_DOWN);
    }

    @Override
    public void run() {
        SkystoneDetector.SkystonePosition position = pipeline.getSkystonePosition();

        switch (position) {
            case LEFT_STONE:
                addSequential(new DriveForwardCommand(driveSubsystem, 4, 0.2), 1);
                break;
            case CENTER_STONE:
                break;
            case RIGHT_STONE:
                addSequential(new DriveForwardCommand(driveSubsystem, -4, 0.2), 1);
                break;
            default:
                break;
        }
        // Turn 90 degrees with a timeout of 2 seconds
        addSequential(new TurnAngleCommand(driveSubsystem, 90, telemetry), 10);

    }
}
