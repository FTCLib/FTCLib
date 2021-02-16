package com.arcrobotics.ftclib.vision;

import org.opencv.core.Mat;

public class UGAdvancedHighGoalPipeline extends UGAngleHighGoalPipeline {

    private double centerOfLogoHeight;
    private double cameraHeight;
    private double cameraPitchOffset;
    private double cameraYawOffset;

    private double distanceOfClosestPuck = 16.5;
    private double puckSpacing = 7.5;

    public enum Powershot {
        LeftShot, CenterShot, RightShot
    }

    public UGAdvancedHighGoalPipeline(double fov, double cameraHeight) {
        this(fov, cameraHeight, 40.625, 0, 0);  // inches
    }

    public UGAdvancedHighGoalPipeline(double fov, double cameraHeight, double centerOfLogoHeight,
                                      double cameraPitchOffset, double cameraYawOffset) {
        super(fov);
        this.cameraHeight = cameraHeight;
        this.centerOfLogoHeight = centerOfLogoHeight;
        this.cameraPitchOffset = cameraPitchOffset;
        this.cameraYawOffset = cameraYawOffset;
    }

    @Override
    public Mat processFrame(Mat input) {
        super.processFrame(input);
        return input;
    }

    public void setCameraHeight(double cameraHeight) {
        this.cameraHeight = cameraHeight;
    }

    public double getDistanceToGoal(Target color) {
        Target baseline;
        if (Math.abs(calculateYaw(Target.BLUE)) < Math.abs(calculateYaw(Target.RED))) {
            baseline = Target.BLUE;
        } else {
            baseline = Target.RED;
        }
        return getDistanceToGoalWall(baseline) / Math.cos(Math.toRadians(calculateYaw(color)));
    }

    public double getDistanceToGoalWall(Target color) {
        return (centerOfLogoHeight - cameraHeight) / Math.tan(Math.toRadians(calculatePitch(color)));
    }

    public double getPowershotAngle(Target color, Powershot shot) {
        double offset = 0;
        double angle = calculateYaw(color);
        if (shot == Powershot.LeftShot) {
            offset = distanceOfClosestPuck;
        } else if (shot == Powershot.CenterShot) {
            offset = distanceOfClosestPuck + puckSpacing;
        } else if (shot == Powershot.RightShot) {
            offset = distanceOfClosestPuck + puckSpacing * 2;
        }

        // Gives the offset a direction
        if (color == Target.RED) {
            offset *= -1;
        }

        double distanceFromWallToGoal = Math.tan(Math.toRadians(angle)) * getDistanceToGoalWall(color);

        // If robot is between Powershot and Highgoal
        if (Math.abs(offset) > Math.abs(distanceFromWallToGoal) && (offset * distanceFromWallToGoal < 0)) {
            return Math.toDegrees(Math.atan((offset + distanceFromWallToGoal) / getDistanceToGoalWall(color)));
        }

        if (angle > 0) {
            return Math.toDegrees(Math.atan((offset + distanceFromWallToGoal) / getDistanceToGoalWall(color)));
        } else {
            return Math.toDegrees(Math.atan((offset - distanceFromWallToGoal) / getDistanceToGoalWall(color)));
        }
    }

    public double getPowerShotDistance(Target color, Powershot shot) {
        return getDistanceToGoalWall(color) / Math.cos(Math.toRadians(getPowershotAngle(color, shot)));
    }

}
