package com.arcrobotics.ftclib.vision;

import org.firstinspires.ftc.robotcore.external.Telemetry;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.openftc.easyopencv.OpenCvPipeline;

import java.util.ArrayList;

@SuppressWarnings("unchecked")
public class SkystoneDetector extends OpenCvPipeline {
    public enum SkystonePosition {
        LEFT_STONE, CENTER_STONE, RIGHT_STONE
    }
    // These are the mats we need, I will be explaining them as we go
    private Mat matYCrCb;

    private ArrayList<Scalar> means;

    private double firstStonePosition;
    private double secondStonePosition;
    private double thirdStonePosition;
    private double firstSkystonePercentage;
    private double percentSpacing;
    private double stoneWidth, stoneHeight;
    private boolean defaultValues;
    private double spacing;
    private Telemetry tl;
    volatile SkystonePosition position;


    ArrayList<Rect> blocks;

    public SkystoneDetector(Telemetry tl) {
        this.tl = tl;
        defaultValues = true;
        blocks = null;
        means = new ArrayList();
        matYCrCb  = new Mat();
    }
    public SkystoneDetector() {
        this(null);
    }

    public SkystoneDetector(double firstSkystonePositionPercentage, double percentSpacing, double stoneWidth, double stoneHeight, Telemetry tl){
        defaultValues = false;

        this.firstSkystonePercentage = firstSkystonePositionPercentage;
        this.percentSpacing = percentSpacing;
        this.stoneWidth = stoneWidth;
        this.stoneHeight = stoneHeight;

        this.tl = tl;

        position = null;
        blocks = null;
        means = new ArrayList();
        matYCrCb  = new Mat();

    }
    public SkystoneDetector(double firstSkystonePositionPercentage, double percentSpacing, double stoneWidth, double stoneHeight){
        this(firstSkystonePositionPercentage, percentSpacing, stoneWidth, stoneHeight, null);

    }

    //These will be the points for our rectangle

    /**
     * This will create the rectangles
     * @param frame the input mat
     * @param rect the rectangle
     * @param color the color of the rectangle when it is displayed on screen
     * @param thickness the thickness of the rectangle
     */
    public Mat drawRectangle(Mat frame,Rect rect,Scalar color,int thickness){
        Imgproc.rectangle(frame, rect, color, thickness);
        //submat simply put is cropping the mat
        return frame.submat(rect);
    }

    @Override
    public Mat processFrame(Mat input) {
        setValues(input.width(), input.height());
        try {
            /**
             *input which is in RGB is the frame the camera gives
             *We convert the input frame to the color space matYCrCb
             *Then we store this converted color space in the mat matYCrCb
             *For all the color spaces go to
             *https://docs.opencv.org/3.4/d8/d01/group__imgproc__color__conversions.html
             */
            Imgproc.cvtColor(input, matYCrCb, Imgproc.COLOR_RGB2YCrCb);

            for (Rect stone : blocks) {
                Mat currentMat = new Mat();
                Core.extractChannel(drawRectangle(matYCrCb, stone, new Scalar(255, 0, 255), 2), currentMat, 2);
                means.add(Core.mean(currentMat));
                currentMat.release();
            }

            Scalar max = means.get(0);
            int biggestIndex = 0;

            for (Scalar k : means) {
                if (k.val[0] > max.val[0]) {
                    max = k;
                    biggestIndex = means.indexOf(k);
                }
            }

            switch (biggestIndex) {
                case 0:
                    position = SkystonePosition.LEFT_STONE;
                    Imgproc.rectangle(input, blocks.get(0), new Scalar(0, 255, 0), 30);
                    Imgproc.rectangle(input, blocks.get(1), new Scalar(255, 0, 0), 30);
                    Imgproc.rectangle(input, blocks.get(2), new Scalar(255, 0, 0), 30);

                    break;
                case 1:
                    position = SkystonePosition.CENTER_STONE;
                    Imgproc.rectangle(input, blocks.get(0), new Scalar(255, 0, 0), 30);
                    Imgproc.rectangle(input, blocks.get(1), new Scalar(0, 255, 0), 30);
                    Imgproc.rectangle(input, blocks.get(2), new Scalar(255, 0, 0), 30);
                    break;
                case 2:
                    Imgproc.rectangle(input, blocks.get(0), new Scalar(255, 0, 0), 30);
                    Imgproc.rectangle(input, blocks.get(1), new Scalar(255, 0, 0), 30);
                    Imgproc.rectangle(input, blocks.get(2), new Scalar(0, 255, 0), 30);
                    position = SkystonePosition.RIGHT_STONE;
                    break;
                default:
                    position = SkystonePosition.RIGHT_STONE;
                    Imgproc.rectangle(input, blocks.get(0), new Scalar(255, 0, 0), 30);
                    Imgproc.rectangle(input, blocks.get(1), new Scalar(255, 0, 0), 30);
                    Imgproc.rectangle(input, blocks.get(2), new Scalar(255, 0, 0), 30);
                    // Default go for right stone;
                    break;

            }

            if(tl != null) {
                tl.addData("Skystone Position", position);
                tl.update();
            }
            means.clear();
        } catch (Exception e) {
            if(tl != null) {
                tl.addData("Exception", e);
                tl.update();
            }
        }
        return input;
    }

    public SkystonePosition getSkystonePosition() {
        return position;
    }

    /**
     * Sets the target rectangles only once using input's width and height.
     * @param width Width of Frame
     * @param height Height of Frame
     */
    private void setValues(double width, double height) {
        if (blocks == null) {
            if (defaultValues) {
                // Set default values
                firstSkystonePercentage = 25;
                percentSpacing = 25;
                stoneHeight = 50;
                stoneWidth = 50;
            }
            spacing = (percentSpacing * width) / 100;
            firstStonePosition = (firstSkystonePercentage / 100) * width;
            secondStonePosition = firstStonePosition + spacing;
            thirdStonePosition = secondStonePosition + spacing;
            blocks = new ArrayList<Rect>();

            blocks.add(new Rect(new Point(firstStonePosition - (stoneWidth / 2), 0.50 * height - (stoneHeight / 2)),
                    new Point(firstStonePosition + (stoneWidth / 2), 0.50 * height + (stoneHeight / 2))));
            blocks.add(new Rect(new Point(secondStonePosition - (stoneWidth / 2), 0.50 * height - (stoneHeight / 2)),
                    new Point(secondStonePosition + (stoneWidth / 2), 0.50 * height + (stoneHeight / 2))));
            blocks.add(new Rect(new Point(thirdStonePosition - (stoneWidth / 2), 0.50 * height - (stoneHeight / 2)),
                    new Point(thirdStonePosition + (stoneWidth / 2), 0.50 * height + (stoneHeight / 2))));
        }
    }
}
