package com.arcrobotics.ftclib.vision;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.openftc.easyopencv.OpenCvPipeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class UGBasicHighGoalPipeline extends OpenCvPipeline {

    protected double centerX;
    protected double centerY;

    public int minThreshold, maxThreshold;
    private Mat blueThreshold;
    private Mat redThreshold;

    private Mat matYCrCb;
    private Mat redChannel;
    private Mat blueChannel;

    private List<MatOfPoint> redContours;
    private List<MatOfPoint> blueContours;
    private MatOfPoint biggestBlueContour;
    private MatOfPoint biggestRedContour;
    private Rect blueRect, redRect;

    public UGBasicHighGoalPipeline() {
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

        minThreshold = 155;
        maxThreshold = 200;
    }

    @Override
    public void init(Mat mat) {
        super.init(mat);
        int imageWidth = mat.width();
        int imageHeight = mat.height();

        centerX = ((double) imageWidth / 2) - 0.5;
        centerY = ((double) imageHeight / 2) - 0.5;
    }

    public boolean filterContours(MatOfPoint contour) {
        return Imgproc.contourArea(contour) > 30;
    }

    @Override
    public Mat processFrame(Mat input) {
        Imgproc.cvtColor(input, matYCrCb, Imgproc.COLOR_RGB2YCrCb);

        Core.extractChannel(matYCrCb, redChannel, 1);
        Core.extractChannel(matYCrCb, blueChannel, 2);

        // Blue threshold
        Imgproc.threshold(blueChannel, blueThreshold, minThreshold, maxThreshold, Imgproc.THRESH_BINARY);
        // Red threshold
        Imgproc.threshold(redChannel, redThreshold, minThreshold, maxThreshold, Imgproc.THRESH_BINARY);

        blueContours.clear();
        redContours.clear();

        Imgproc.findContours(blueThreshold, blueContours, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        Imgproc.findContours(redThreshold, redContours, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        blueContours = blueContours.stream().filter(i -> {
            boolean appropriateAspect = ((double) Imgproc.boundingRect(i).width / Imgproc.boundingRect(i).height > 1)
                    && ((double) Imgproc.boundingRect(i).width / Imgproc.boundingRect(i).height < 2);
            return filterContours(i) && appropriateAspect;
        }).collect(Collectors.toList());
        redContours = redContours.stream().filter(i -> {
            boolean appropriateAspect = ((double) Imgproc.boundingRect(i).width / Imgproc.boundingRect(i).height > 1)
                    && ((double) Imgproc.boundingRect(i).width / Imgproc.boundingRect(i).height < 2);
            return filterContours(i) && appropriateAspect;
        }).collect(Collectors.toList());

        Imgproc.drawContours(input, redContours, -1, new Scalar(255, 255, 0));
        Imgproc.drawContours(input, blueContours, -1, new Scalar(255, 255, 0));

        if (!blueContours.isEmpty()) {
            // Comparing width instead of area because wobble goals that are close to the camera tend to have a large area
            biggestBlueContour = Collections.max(blueContours, (t0, t1) -> {
                return Double.compare(Imgproc.boundingRect(t0).width, Imgproc.boundingRect(t1).width);
            });
            blueRect = Imgproc.boundingRect(biggestBlueContour);
            Imgproc.rectangle(input, blueRect, new Scalar(0, 0, 255), 3);
        } else {
            blueRect = null;
        }

        if (!redContours.isEmpty()) {
            // Comparing width instead of area because wobble goals that are close to the camera tend to have a large area
            biggestRedContour = Collections.max(redContours, (t0, t1) -> {
                return Double.compare(Imgproc.boundingRect(t0).width, Imgproc.boundingRect(t1).width);
            });
            redRect = Imgproc.boundingRect(biggestRedContour);
            Imgproc.rectangle(input, redRect, new Scalar(255, 0, 0), 3);
        } else {
            redRect = null;
        }

        return input;
    }

    public Rect getRedRect() {
        return redRect;
    }

    public Rect getBlueRect() {
        return blueRect;
    }

    public boolean isRedVisible() {
        return (redRect != null);
    }

    public boolean isBlueVisible() {
        return (blueRect != null);
    }

    public Point getCenterofRect(Rect rect) {
        if (rect == null) {
            return new Point(centerX, centerY);
        }
        return new Point(rect.x + rect.width / 2.0, rect.y + rect.height / 2.0);
    }

}
