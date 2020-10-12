package com.arcrobotics.ftclib.vision;

import org.opencv.core.Mat;

public class AdvancedHighGoalPipeline extends AngleHighGoalPipeline {
    private double centerOfLogoHeight;
    private double cameraHeight;
    private double cameraPitchOffset;
    private double cameraYawOffset;

    private double distanceOfClosestPuck = 16.5;
    private double puckSpacing = 7.5;

    enum Powershot {
        LeftShot, CenterShot, RightShot
    }


    public AdvancedHighGoalPipeline(double fov, double cameraHeight) {
        this(fov, cameraHeight, 40.625, 0, 0);  // inches

    }

    public AdvancedHighGoalPipeline(double fov, double cameraHeight, double centerOfLogoHeight, double cameraPitchOffset, double cameraYawOffset) {
        super(fov);
        this.cameraHeight = cameraHeight;
        this.centerOfLogoHeight = centerOfLogoHeight;
        this.cameraPitchOffset = cameraPitchOffset;
        this.cameraYawOffset = cameraYawOffset;
    }

    public void setCameraHeight(double cameraHeight) {
        this.cameraHeight = cameraHeight;
    }

    public double getDistanceToGoal(Target color) {

        return getDistanceToGoalWall(color) / Math.cos(Math.toRadians(calculateYaw(color)));
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
        } else if (shot == Powershot.RightShot){
            offset = distanceOfClosestPuck + puckSpacing * 2;
        }

        // Gives the offset a direction
        if (color == Target.RED) {
            offset *= -1;
        }

        double distanceFromWallToGoal = Math.tan(Math.toRadians(angle)) * getDistanceToGoalWall(color);

        // If robot is inbetween Powershot and Highgoal
        if (Math.abs(offset) > Math.abs(distanceFromWallToGoal)) {
            return Math.toRadians(Math.atan((offset - distanceFromWallToGoal) / getDistanceToGoalWall(color)));
        }

        // if the angle to the powershot is embedded in the angle to the color goal
        boolean inclusiveAngle = angle * offset < 0;
        // triangle made from distangeToGoalWall and angle
        double hypotenuse = getDistanceToGoalWall(color) / Math.cos(Math.toRadians(angle));
        if (inclusiveAngle) {
            return angle - Math.toDegrees(Math.asin(offset / hypotenuse));
        } else {
            double alpha = Math.toDegrees(Math.atan((Math.abs(offset) + Math.abs(distanceFromWallToGoal))/getDistanceToGoalWall(color))) - angle;
            return Math.signum(angle) * alpha;
        }



        // If both the offset and alpha are the same sign, the angle to the powershot is alpha + yaw. else just alpha


    }

    public double getPowerShotDistance(Target color, Powershot shot) {
        return getDistanceToGoalWall(color) / Math.cos(Math.toRadians(getPowershotAngle(color, shot)));
    }

}
