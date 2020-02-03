package com.arcrobotics.ftclib.vision;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.openftc.easyopencv.OpenCvPipeline;

import java.util.ArrayList;

public class SkystoneDetector extends OpenCvPipeline {

    enum SkystonePosition {
        LEFT_STONE, CENTER_STONE, RIGHT_STONE
    }
    // These are the mats we need, I will be explaining them as we go
    private Mat matYCrCb = new Mat();

    private ArrayList<Scalar> means = new ArrayList();

    private double firstStonePosition;
    private double secondStonePosition;
    private double thirdStonePosition;

    SkystonePosition position;

    ArrayList<Rect> blocks;

    public SkystoneDetector(int width, int height) {
        this(width, height, 25, 25, 50, 50);
    }

    public SkystoneDetector(int width, int height, double firstSkystonePositionPercentage,
                            double percentSpacing, double stoneWidth, double stoneHeight){

        double spacing = (percentSpacing * width) / 100;
        firstStonePosition  = (firstSkystonePositionPercentage / 100) * width;
        secondStonePosition  = firstStonePosition + spacing;
        thirdStonePosition  = secondStonePosition + spacing;
        blocks = new ArrayList<Rect>();

        blocks.add(
                new Rect(
                        new Point(
                                firstStonePosition - (stoneWidth / 2),
                                0.50 * height - (stoneHeight / 2)
                        ),
                        new Point(
                                firstStonePosition + (stoneWidth / 2),
                                0.50 * height + (stoneHeight / 2)
                        )
                )
        );

        blocks.add(
                new Rect(
                        new Point(
                                secondStonePosition - (stoneWidth / 2),
                                0.50 * height - (stoneHeight / 2)
                        ),
                        new Point(
                                secondStonePosition + (stoneWidth / 2),
                                0.50 * height + (stoneHeight / 2)
                        )
                )
        );

        blocks.add(
                new Rect(
                        new Point(
                                thirdStonePosition - (stoneWidth / 2),
                                0.50 * height - (stoneHeight / 2)
                        ),
                        new Point(
                                thirdStonePosition + (stoneWidth / 2),
                                0.50 * height + (stoneHeight / 2)
                        )
                )
        );

        position = null;

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
        /**
         *input which is in RGB is the frame the camera gives
         *We convert the input frame to the color space matYCrCb
         *Then we store this converted color space in the mat matYCrCb
         *For all the color spaces go to
         *https://docs.opencv.org/3.4/d8/d01/group__imgproc__color__conversions.html
         */
        Imgproc.cvtColor(input, matYCrCb, Imgproc.COLOR_RGB2YCrCb);

        for (Rect stone: blocks) {
            Mat currentMat = new Mat();
            Core.extractChannel(drawRectangle(matYCrCb, stone, new Scalar (255, 0, 255), 2), currentMat, 2);
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
        
        means.clear();
        return input;
    }

    public SkystonePosition getSkystonePosition() {
        return position;
    }

}
