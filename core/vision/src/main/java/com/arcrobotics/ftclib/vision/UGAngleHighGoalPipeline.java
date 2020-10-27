package com.arcrobotics.ftclib.vision;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;
import org.openftc.easyopencv.OpenCvPipeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;


public class UGAngleHighGoalPipeline extends BasicHighGoalPipeline {

    class Fraction {
        private int numerator, denominator;
        Fraction(long a, long b) {
            numerator = (int) (a / gcd(a, b));
            denominator = (int) (b / gcd(a, b));
        }

        /** @return the greatest common denominator */
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

    public AngleHighGoalPipeline(double fov, double cameraPitchOffset, double cameraYawOffset) {
        super();
        this.fov = fov;
        this.cameraPitchOffset = cameraPitchOffset;
        this.cameraYawOffset = cameraYawOffset;
    }

    public AngleHighGoalPipeline(double fov) {
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
            horizontalFocalLength = this.imageWidth / (2 * Math.tan(horizontalView /2));
            verticalFocalLength = this.imageHeight / (2 * Math.tan(verticalView /2));
        }

        return super.processFrame(input);

    }

    public double calculateYaw(Target color) {
        return calculateYaw(color, centerX) + cameraYawOffset;
    }

    public double calculatePitch(Target color) {
        return calculatePitch(color, centerY) + cameraPitchOffset;
    }

    public double calculateYaw(Target color, double offsetCenterX) {
        double targetCenterX = 0;
        if (color == Target.RED) {
            targetCenterX = AngleHighGoalPipeline.getCenterofRect(getRedRect()).x;
        } else if (color == Target.Blue) {
            targetCenterX = AngleHighGoalPipeline.getCenterofRect(getBlueRect()).x;
        }

        return Math.toDegrees(
                Math.atan((offsetCenterX - targetCenterX) / horizontalFocalLength));
    }

    public double calculatePitch(Target color, double offsetCenterY) {
        double targetCenterY = 0;
        if (color == Target.RED) {
            targetCenterY = AngleHighGoalPipeline.getCenterofRect(getRedRect()).y;
        } else if (color == Target.Blue) {
            targetCenterY = AngleHighGoalPipeline.getCenterofRect(getBlueRect()).y;
        }

        return -Math.toDegrees(
                Math.atan((offsetCenterY - targetCenterY) / verticalFocalLength));
    }



}
