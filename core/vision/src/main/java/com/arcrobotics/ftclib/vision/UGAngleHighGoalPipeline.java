package com.arcrobotics.ftclib.vision;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

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
    private double imageArea;
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

        imageArea = this.imageWidth * this.imageHeight;

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
        input =  super.processFrame(input);
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
     *
     * @param color Allaince color
     * @param offsetCenterX centerX
     * @return
     */
    public double calculateYaw(Target color, double offsetCenterX) {


        double targetCenterX = 0;
        Rect currentRect;
        if (color == Target.RED) {
            currentRect = getRedRect();
        } else {
            currentRect = getBlueRect();
        }
        targetCenterX = getCenterofRect(currentRect).x;

        return Math.toDegrees(
                Math.atan((targetCenterX - offsetCenterX) / horizontalFocalLength));
    }

    /**
     *
     * @param color Allaince color
     * @param offsetCenterY centerY
     * @return
     */
    public double calculatePitch(Target color, double offsetCenterY) {
        double targetCenterY = 0;

        Rect currentRect;
        if (color == Target.RED) {
            currentRect = getRedRect();
        } else {
            currentRect = getBlueRect();
        }
        targetCenterY = getCenterofRect(currentRect).y;

        return -Math.toDegrees(
                Math.atan((targetCenterY - offsetCenterY ) / verticalFocalLength));
    }


}