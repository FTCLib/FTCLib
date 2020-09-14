package com.arcrobotics.ftclib.vision;

import android.support.annotation.NonNull;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.imgproc.Imgproc;
import org.openftc.easyopencv.OpenCvPipeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;


public class BasicHighGoalPipeline extends OpenCvPipeline {

    private Mat blueThreshold;
    private Mat redThreshold;

    private Mat matYCrCb;
    private Mat redChannel;
    private Mat blueChannel;

    private ArrayList<MatOfPoint> redContours;
    private ArrayList<MatOfPoint> blueContours;
    private MatOfPoint biggestBlueContour;
    private MatOfPoint biggestRedContour;
    private Rect blueRect, redRect;

    private boolean isBlueVisible, isRedVisible;

    public BasicHighGoalPipeline() {

        matYCrCb = new Mat();
        redChannel = new Mat();
        blueChannel = new Mat();

        blueThreshold = new Mat();
        redThreshold = new Mat();

        blueContours = new ArrayList<MatOfPoint>();
        redContours = new ArrayList<MatOfPoint>();

        biggestBlueContour = new MatOfPoint();
        biggestRedContour = new MatOfPoint();

        blueRect = new Rect();
        redRect = new Rect();
    }

    @Override
    public Mat processFrame(Mat input) {


        Imgproc.cvtColor(input, matYCrCb, Imgproc.COLOR_RGB2YCrCb);

        Core.extractChannel(matYCrCb, redChannel, 1);
        Core.extractChannel(matYCrCb, blueChannel, 2);

        // Blue threshold
        Imgproc.threshold(blueChannel, blueThreshold, 155, 255, Imgproc.THRESH_BINARY);
        // Red threshold
        Imgproc.threshold(redChannel, redThreshold, 155, 255, Imgproc.THRESH_BINARY);

        Imgproc.findContours(blueThreshold, blueContours, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        Imgproc.findContours(redThreshold, redContours, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        if (blueContours.size() != 0) {
            biggestBlueContour = Collections.max(blueContours, new Comparator<MatOfPoint>() {
                @Override
                public int compare(MatOfPoint t0, MatOfPoint t1) {
                    return Double.compare(Imgproc.contourArea(t0), Imgproc.contourArea(t1)) ;
                }
            });

            blueRect = Imgproc.boundingRect(biggestBlueContour);
        }

        if (redContours.size() != 0) {
            biggestRedContour = Collections.max(redContours, new Comparator<MatOfPoint>() {
                @Override
                public int compare(MatOfPoint t0, MatOfPoint t1) {
                    return Double.compare(Imgproc.contourArea(t0), Imgproc.contourArea(t1)) ;
                }
            });

            redRect = Imgproc.boundingRect(biggestRedContour);
        }

        return null;
    }

    public Rect getRedRect() {
        return redRect;
    }

    public Rect getBlueRect() {
        return blueRect;
    }

    public static Point getCenterofRect(Rect rect) {
        return new Point(rect.x + rect.width / 2, rect.y + rect.height / 2);
    }
}
