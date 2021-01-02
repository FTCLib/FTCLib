package com.example.ftclibexamples.kotlinexamples

import com.arcrobotics.ftclib.geometry.*
import com.arcrobotics.ftclib.kotlin.extensions.geometry.*
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode

class GeometryExtSample : LinearOpMode() {
    /**
     * random math using geometry classes
     */
    override fun runOpMode() {
        val vec1 = Vector2d(10.0, 2.0)
        val vec2 = Vector2d(-2.0, 3.0)

        val rotation1 = Rotation2d(2.0, 3.0)
        val rotation2 = Rotation2d(-2.0, -3.0)

        val translation1 = Translation2d(10.0, 10.0)

        val pose1 = Pose2d(translation1, rotation1)
        val pose2 = Pose2d(translation1, rotation1)

        val twist1 = Twist2d(2.0, 0.0, -2.0)

        val transform1 = Transform2d(translation1, rotation1)

        val vec1Angle = vec1.angle
        val vec2Mag = vec2.magnitude
        val newVec1 = vec1 rotateBy (Math.PI / 2)
        val vec1DotVec2 = vec1 dot vec2
        val vec1Scaled = vec1 scale 10
        val vec1ScaleProject = vec1 scalarProject vec2

        val translationRotated = translation1 rotateBy rotation1

        val transformInverse = transform1.inverse

        val poseTransformed = pose1 transformBy transform1
        val poseRelative = pose1 relativeTo pose2
        val poseExponential = pose1 exp twist1
        val poseLog = pose1 log pose2
        val poseRotate = pose1 rotate -(Math.PI / 2)

        val rotationRotate = rotation1 rotateBy rotation2

        waitForStart()
    }
}