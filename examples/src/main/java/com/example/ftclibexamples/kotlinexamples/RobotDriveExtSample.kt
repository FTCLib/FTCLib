package com.example.ftclibexamples.kotlinexamples

import com.arcrobotics.ftclib.drivebase.MecanumDrive
import com.arcrobotics.ftclib.geometry.Vector2d
import com.arcrobotics.ftclib.hardware.motors.Motor
import com.arcrobotics.ftclib.hardware.motors.MotorEx
import com.arcrobotics.ftclib.kotlin.extensions.drivebase.setMaxSpeed
import com.arcrobotics.ftclib.kotlin.extensions.drivebase.setRange
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import kotlin.math.absoluteValue

class RobotDriveExtSample : LinearOpMode() {
    private lateinit var driveTrain: MecanumDrive

    // using motors goBILDA Yellow Jackets, 435 rpm
    // all motors are type inferred to be of type MotorEx
    private var frontLeft = MotorEx(hardwareMap, "frontLeft", Motor.GoBILDA.RPM_435)
    private var frontRight = MotorEx(hardwareMap, "frontRight", Motor.GoBILDA.RPM_435)
    private var backLeft = MotorEx(hardwareMap, "backLeft", Motor.GoBILDA.RPM_435)
    private var backRight = MotorEx(hardwareMap, "backRight", Motor.GoBILDA.RPM_435)

    init {
        driveTrain = MecanumDrive(
                frontLeft, frontRight, backLeft, backRight
        )
    }

    override fun runOpMode() {
        driveTrain setMaxSpeed 0.8 // setting max possible speed to 0.8

        driveTrain setRange (-0.8 to 0.8) // setting range for clip function to be -0.8 to 0.8

        waitForStart()

        driveWithVector(vector2dOf(x = 12, y = 3))

        sleep(1000)

        driveWithVector(vector2dOf(x = 0, y = 0))
    }

    /**
     * Helper function to allow for generic number input
     * and not call .toDouble() in Vector2d constructor
     */
    private fun vector2dOf(x: Number, y: Number) = Vector2d(x.toDouble(), y.toDouble())

    private fun driveWithVector(vector: Vector2d) {
        val (xSpeed, ySpeed) = normalize(doubleArrayOf(vector.x, vector.y))
        driveTrain.driveRobotCentric(xSpeed, ySpeed, 0.0)
    }

    /**
     * Normalize the wheel speeds if any value is greater than 0.8
     */
    private fun normalize(wheelSpeeds: DoubleArray): DoubleArray {
        var maxMagnitude = wheelSpeeds[0].absoluteValue
        for (i in 1 until wheelSpeeds.size) {
            val temp = wheelSpeeds[i].absoluteValue
            if (maxMagnitude < temp) {
                maxMagnitude = temp
            }
        }
        if (maxMagnitude > 0.8) {
            for (i in wheelSpeeds.indices) {
                wheelSpeeds[i] = wheelSpeeds[i] / maxMagnitude
            }
        }
        return wheelSpeeds
    }
}