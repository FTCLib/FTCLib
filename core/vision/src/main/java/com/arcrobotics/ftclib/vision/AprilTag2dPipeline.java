package com.arcrobotics.ftclib.vision;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.openftc.apriltag.AprilTagDetection;
import org.openftc.apriltag.AprilTagDetectorJNI;
import org.openftc.easyopencv.OpenCvPipeline;

import java.util.ArrayList;

public class AprilTag2dPipeline extends OpenCvPipeline
{
    private long nativeApriltagPtr;
    private Mat grey = new Mat();
    private ArrayList<AprilTagDetection> detections = new ArrayList<>();

    private ArrayList<AprilTagDetection> detectionsUpdate = new ArrayList<>();
    private final Object detectionsUpdateSync = new Object();

    Scalar blue = new Scalar(7,197,235,255);
    Scalar red = new Scalar(255,0,0,255);
    Scalar green = new Scalar(0,255,0,255);
    Scalar white = new Scalar(255,255,255,255);

    double fx;
    double fy;
    double cx;
    double cy;

    double tagsize;
    double tagsizeX;
    double tagsizeY;

    private float decimation;
    private boolean needToSetDecimation;
    private final Object decimationSync = new Object();

    public AprilTag2dPipeline(double tagsize, double fx, double fy, double cx, double cy)
    {
        this.tagsize = tagsize;
        this.tagsizeX = tagsize;
        this.tagsizeY = tagsize;
        this.fx = fx;
        this.fy = fy;
        this.cx = cx;
        this.cy = cy;

        // Allocate a native context object. See the corresponding deletion in the finalizer
        nativeApriltagPtr = AprilTagDetectorJNI.createApriltagDetector(AprilTagDetectorJNI.TagFamily.TAG_36h11.string, 3, 3);
    }

    public AprilTag2dPipeline() {
        this(0.166, 578.272, 578.272, 402.145, 221.506);
    }

    @Override
    public void finalize()
    {
        // Might be null if createApriltagDetector() threw an exception
        if(nativeApriltagPtr != 0)
        {
            // Delete the native context we created in the constructor
            AprilTagDetectorJNI.releaseApriltagDetector(nativeApriltagPtr);
            nativeApriltagPtr = 0;
        }
        else
        {
            System.out.println("AprilTagDetectionPipeline.finalize(): nativeApriltagPtr was NULL");
        }
    }

    @Override
    public Mat processFrame(Mat input)
    {
        // Convert to greyscale
        Imgproc.cvtColor(input, grey, Imgproc.COLOR_RGBA2GRAY);

        synchronized (decimationSync)
        {
            if(needToSetDecimation)
            {
                AprilTagDetectorJNI.setApriltagDetectorDecimation(nativeApriltagPtr, decimation);
                needToSetDecimation = false;
            }
        }

        // Run AprilTag
        detections = AprilTagDetectorJNI.runAprilTagDetectorSimple(nativeApriltagPtr, grey, tagsize, fx, fy, cx, cy);
        synchronized (detectionsUpdateSync)
        {
            detectionsUpdate = detections;
        }

        for(AprilTagDetection detection : detections)
        {
            draw2dSquare(input, detection.corners);
        }

        return input;
    }

    public void setDecimation(float decimation)
    {
        synchronized (decimationSync)
        {
            this.decimation = decimation;
            needToSetDecimation = true;
        }
    }

    public ArrayList<AprilTagDetection> getLatestDetections()
    {
        return detections;
    }

    public ArrayList<AprilTagDetection> getDetectionsUpdate()
    {
        synchronized (detectionsUpdateSync)
        {
            ArrayList<AprilTagDetection> ret = detectionsUpdate;
            detectionsUpdate = null;
            return ret;
        }
    }


    void draw2dSquare(Mat buf, Point[] points) {
        Imgproc.line(buf, points[0], points[1], blue);
        Imgproc.line(buf, points[1], points[2], blue);
        Imgproc.line(buf, points[2], points[3], blue);
        Imgproc.line(buf, points[3], points[0], blue);
    }
}
