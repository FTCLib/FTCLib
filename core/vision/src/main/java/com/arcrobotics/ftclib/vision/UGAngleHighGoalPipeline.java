package com.arcrobotics.ftclib.vision;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

public class UGAngleHighGoalPipeline extends UGBasicHighGoalPipeline {

    class Fraction {
        private int numerator, denominator;
        Fraction(long a, long b) {
            numerator = (int) (a / gcd(a, b));
            denominator = (int) (b / gcd(a, b));
        }
        /**
         * @return the greatest common denominator
         */
        private long gcd(long a, long b) {
            return b == 0 ? a : gcd(b, a % b);
        }
        public int getNumerator() {
            return numerator;
        }
        public int getDenominator() {
            return denominator;
        }
    }

    private boolean isFirstFrame;

    // Camera Settings
    private int imageWidth;
    private int imageHeight;

    private double cameraPitchOffset;
    private double cameraYawOffset;

    private double fov;
    private double imageArea;
    private double centerX;
    private double centerY;
    private double horizontalFocalLength;
    private double verticalFocalLength;

    public enum Target {
        RED, Blue
    }

    public UGAngleHighGoalPipeline(double fov, double cameraPitchOffset, double cameraYawOffset) {
        super();
        this.fov = fov;
        this.cameraPitchOffset = cameraPitchOffset;
        this.cameraYawOffset = cameraYawOffset;
        this.isFirstFrame = true;
    }

    public UGAngleHighGoalPipeline(double fov) {
        this(fov, 0, 0);
    }

    @Override
    public Mat processFrame(Mat input) {
        if (isFirstFrame) {
            imageWidth = input.width();
            imageHeight = input.height();

            imageArea = this.imageWidth * this.imageHeight;
            centerX = ((double) this.imageWidth / 2) - 0.5;
            centerY = ((double) this.imageHeight / 2) - 0.5;

            // pinhole model calculations
            double diagonalView = Math.toRadians(this.fov);
            Fraction aspectFraction = new Fraction(this.imageWidth, this.imageHeight);
            int horizontalRatio = aspectFraction.getNumerator();
            int verticalRatio = aspectFraction.getDenominator();
            double diagonalAspect = Math.hypot(horizontalRatio, verticalRatio);
            double horizontalView = Math.atan(Math.tan(diagonalView / 2) * (horizontalRatio / diagonalAspect)) * 2;
            double verticalView = Math.atan(Math.tan(diagonalView / 2) * (verticalRatio / diagonalAspect)) * 2;
            horizontalFocalLength = this.imageWidth / (2 * Math.tan(horizontalView / 2));
            verticalFocalLength = this.imageHeight / (2 * Math.tan(verticalView / 2));
            isFirstFrame = false;
        }

        return super.processFrame(input);
    }
    /**
     * @param color Alliance Color
     * @param defaultAngle Angle to display if no target found
    */
    public double calculateYaw(Target color, double defaultAngle) {
        return calculateYaw(color, centerX, defaultAngle) + cameraYawOffset;
    }

    /**
     * @param color Alliance Color
     * @param defaultAngle Angle to display if no target found
     */
    public double calculatePitch(Target color, double defaultAngle) {
        return calculatePitch(color, centerY, defaultAngle) + cameraPitchOffset;
    }

    /**
     *
     * @param color Allaince color
     * @param offsetCenterX centerX
     * @param defaultAngle Angle to return if no target found
     * @return
     */
    public double calculateYaw(Target color, double offsetCenterX, double defaultAngle) {


        double targetCenterX = 0;
        Rect currentRect;
        if (color == Target.RED) {
            currentRect = getRedRect();
        } else {
            currentRect = getBlueRect();
        }
        if(getCenterofRect(currentRect).isPresent()) {
            targetCenterX = getCenterofRect(currentRect).get().x;
        } else {
            return defaultAngle;
        }

        return Math.toDegrees(
                Math.atan((offsetCenterX - targetCenterX) / horizontalFocalLength));
    }

    /**
     *
     * @param color Allaince color
     * @param offsetCenterY centerY
     * @param defaultAngle Angle to return if no target found
     * @return
     */
    public double calculatePitch(Target color, double offsetCenterY, double defaultAngle) {
        double targetCenterY = 0;

        Rect currentRect;
        if (color == Target.RED) {
            currentRect = getRedRect();
        } else {
            currentRect = getBlueRect();
        }
        if(getCenterofRect(currentRect).isPresent()) {
            targetCenterY = getCenterofRect(currentRect).get().y;
        } else {
            return defaultAngle;
        }

        return -Math.toDegrees(
                Math.atan((offsetCenterY - targetCenterY) / verticalFocalLength));
    }


}
