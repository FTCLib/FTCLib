package com.arcrobotics.ftclib.vision;

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
        } else if (shot == Powershot.RightShot) {
            offset = distanceOfClosestPuck + puckSpacing * 2;
        }

        if (color == Target.Blue) {
            offset *= -1;
        }
        return Math.toDegrees(Math.toRadians(calculateYaw(color) - Math.asin(offset / getDistanceToGoal(color))));
    }

    public double getPowerShotDistance(Target color, Powershot shot) {
        return getDistanceToGoalWall(color) / Math.cos(Math.toRadians(getPowershotAngle(color, shot)));
    }

}
