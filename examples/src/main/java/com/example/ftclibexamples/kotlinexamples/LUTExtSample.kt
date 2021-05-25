package com.example.ftclibexamples.kotlinexamples

import com.arcrobotics.ftclib.geometry.Pose2d
import com.arcrobotics.ftclib.geometry.Rotation2d
import com.arcrobotics.ftclib.geometry.Translation2d
import com.arcrobotics.ftclib.hardware.motors.Motor
import com.arcrobotics.ftclib.hardware.motors.MotorEx
import com.arcrobotics.ftclib.kinematics.HolonomicOdometry
import com.arcrobotics.ftclib.kotlin.extensions.util.toLUT
import com.arcrobotics.ftclib.util.LUT
import com.qualcomm.robotcore.eventloop.opmode.Disabled
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp


@TeleOp(name = "LUTExt Sample")
@Disabled
class LUTExtSample : LinearOpMode() {
    // our lookup table of distances from the goal and respective speeds of the shooter
    var speeds: LUT<Double, Double> = mapOf(
            5.0 to 1.0,
            4.0 to 0.9,
            3.0 to 0.75,
            2.0 to 0.5,
            1.0 to 0.2,
    ).toLUT() // extension function to allow for conversion of Map<Double, Double> to LUT<Double, Double>

    private var odometry: HolonomicOdometry? = null
    private val leftEncoder: MotorEx = MotorEx(hardwareMap, "left")
    private val rightEncoder: MotorEx = MotorEx(hardwareMap, "right")
    private val perpEncoder: MotorEx = MotorEx(hardwareMap, "perp")
    private val shooter = Motor(hardwareMap, "shooter")

    @Throws(InterruptedException::class)
    override fun runOpMode() {
        // REVcoders
        // the values we are setting here is the circumference of the
        // 2 inch odometer wheels in inches divided by 8192 (the CPR)
        leftEncoder.setDistancePerPulse(2 * Math.PI / 8192.toDouble())
        rightEncoder.setDistancePerPulse(2 * Math.PI / 8192.toDouble())
        perpEncoder.setDistancePerPulse(2 * Math.PI / 8192.toDouble())

        // The last two values are trackwidth and center_wheel_offset
        odometry = HolonomicOdometry(
                leftEncoder::getDistance,
                rightEncoder::getDistance,
                perpEncoder::getDistance,
                14.0,
                2.1
        )
        odometry!!.updatePose(
                Pose2d(
                        3.0,
                        4.0,
                        Rotation2d(0.0)
                )
        )

        waitForStart()

        // let's say our goal is at (5, 10) in our global field coordinates
        while (opModeIsActive() && !isStopRequested) {
            if (gamepad1.a) {
                val distance = odometry!!.pose.translation.getDistance(Translation2d(5.0, 10.0))
                shooter.set(speeds.getClosest(distance))
            }
            odometry!!.updatePose()
        }
    }
}