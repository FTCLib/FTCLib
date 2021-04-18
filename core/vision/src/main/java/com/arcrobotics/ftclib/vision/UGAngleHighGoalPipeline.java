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

    // Camera Settings
    protected int imageWidth;
    protected int imageHeight;

    private double cameraPitchOffset;
    private double cameraYawOffset;

    private double fov;
    private double horizontalFocalLength;
    private double verticalFocalLength;

    public enum Target {
        RED, BLUE
    }

    public UGAngleHighGoalPipeline(double fov, double cameraPitchOffset, double cameraYawOffset) {
        super();
        this.fov = fov;
        this.cameraPitchOffset = cameraPitchOffset;
        this.cameraYawOffset = cameraYawOffset;
    }

    public UGAngleHighGoalPipeline(double fov) {
        this(fov, 0, 0);
    }

    @Override
    public void init(Mat mat) {
        super.init(mat);

        imageWidth = mat.width();
        imageHeight = mat.height();

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
    }

    @Override
    public Mat processFrame(Mat input) {
        input = super.processFrame(input);
        return input;
    }

    /**
     * @param color Alliance Color
     */
    public double calculateYaw(Target color) {
        return calculateYaw(color, centerX) + cameraYawOffset;
    }

    /**
     * @param color Alliance Color
     */
    public double calculatePitch(Target color) {
        return calculatePitch(color, centerY) + cameraPitchOffset;
    }

    /**
     * @param color         Alliance color
     * @param offsetCenterX centerX
     */
    public double calculateYaw(Target color, double offsetCenterX) {
        Rect currentRect = color == Target.RED ? getRedRect() : getBlueRect();
        double targetCenterX = getCenterofRect(currentRect).x;

        return Math.toDegrees(
                Math.atan((targetCenterX - offsetCenterX) / horizontalFocalLength)
        );
    }

    /**
     * @param color         Alliance color
     * @param offsetCenterY centerY
     */
    public double calculatePitch(Target color, double offsetCenterY) {
        Rect currentRect = color == Target.RED ? getRedRect() : getBlueRect();
        double targetCenterY = getCenterofRect(currentRect).y;

        return -Math.toDegrees(
                Math.atan((targetCenterY - offsetCenterY) / verticalFocalLength)
        );
    }

}