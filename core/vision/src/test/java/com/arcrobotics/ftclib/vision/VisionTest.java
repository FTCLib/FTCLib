package com.arcrobotics.ftclib.vision;

import static com.arcrobotics.ftclib.vision.UGRectDetector.getStack;
import static com.google.common.truth.Truth.assertThat;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.arcrobotics.ftclib.vision.VisionTestHelper.*;
import static com.arcrobotics.ftclib.vision.TestCase.*;
import com.arcrobotics.ftclib.vision.UGContourRingPipeline.Height;

public class VisionTest {

    // Load x64 OpenCV Library dll
    static {
        try {
            System.load("C:/opencv/build/java/x64/opencv_java412.dll");
            // https://sourceforge.net/projects/opencvlibrary/files/opencv-win/3.4.3/
            // https://sourceforge.net/projects/opencvlibrary/files/4.1.2/opencv-4.1.2-vc14_vc15.exe/
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Native code library failed to load.\n" + e);
            System.err.println("For windows 10, download OpenCV Library from:");
            //System.err.println("https://sourceforge.net/projects/opencvlibrary/files/opencv-win/3.4.3/");
            System.err.println("https://sourceforge.net/projects/opencvlibrary/files/4.1.2/opencv-4.1.2-vc14_vc15.exe/");
            // Just the 50 MB dll from google docs
			System.err.println("https://drive.google.com/file/d/1vgO4UNozY0Zq2aSPP1nH90zFbTrPlg1s/view?usp=sharing");			
            System.err.println("https://opencv.org/releases/");
            System.err.println("And extract to your C:\\ drive");

            System.exit(1);
        }
    }


    String IMAGE_READ_PATH = "./TestData/openCV_input/ug1/";
    String IMAGE_WRITE_PATH = "./TestData/openCV_output/ug1/";


    Mat inputMat = new Mat();
    ArrayList<TestCaseRings> testCaseRings = new ArrayList<TestCaseRings>();


    @Before
    public void initialize() {
//        this.inputMat = loadMatFromBGR(IMAGE_READ_PATH + "blue_ring_4.jpg" );
        this.testCaseRings.add(new TestCaseRings(IMAGE_READ_PATH,"blue_ring_0.jpg", Height.ZERO));
        this.testCaseRings.add(new TestCaseRings(IMAGE_READ_PATH,"blue_ring_1.jpg",Height.ONE));
        this.testCaseRings.add(new TestCaseRings(IMAGE_READ_PATH,"blue_ring_4.jpg",Height.FOUR));
        this.inputMat = this.testCaseRings.get(0).getMat();
    }

    @Test
    public void imageRead() {
        System.out.println("Input Width: " + inputMat.width() + "   Input Height: " + inputMat.height());
        assertThat(inputMat.width()).isAtLeast(1);
        assertThat(inputMat.height()).isAtLeast(1);
    }

    @Test
    public void imageWrite() {
        String TEST_TYPE = "WriteTest";
        String writePath = IMAGE_WRITE_PATH + TEST_TYPE + "_" + "input.jpg";
        saveMatAsRGB(writePath, inputMat);
        File outputFile = new File(writePath);
        assertThat(outputFile.exists()).isTrue();
    }

    @Test
    public void colorThresholding() {
        String TEST_TYPE = "ColorThresholding";
        Mat yCbCrChan2Mat = new Mat();
        Mat thresholdMat = new Mat();
        Mat all = new Mat();
        List<MatOfPoint> contoursList = new ArrayList<>();

        Imgproc.cvtColor(inputMat, yCbCrChan2Mat, Imgproc.COLOR_RGB2YCrCb);//converts rgb to ycrcb
        Core.extractChannel(yCbCrChan2Mat, yCbCrChan2Mat, 2);//takes cb difference and stores

        //b&w
        Imgproc.threshold(yCbCrChan2Mat, thresholdMat, 100, 255, Imgproc.THRESH_BINARY_INV);

        //outline/contour
        Imgproc.findContours(thresholdMat, contoursList, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        yCbCrChan2Mat.copyTo(all);//copies mat object
        Imgproc.drawContours(all, contoursList, -1, new Scalar(255, 0, 0), 3, 8);//draws blue contours

        Imgproc.rectangle(
                all,
                new Point(
                        inputMat.cols()*0.25,
                        inputMat.rows()*0.25),
                new Point(
                        inputMat.cols()*0.75,
                        inputMat.rows()*0.75),
                new Scalar(0, 255, 0), 3);


        Imgcodecs.imwrite(IMAGE_WRITE_PATH + TEST_TYPE + "_" + "yCbCr.jpg", yCbCrChan2Mat);
        Imgcodecs.imwrite(IMAGE_WRITE_PATH + TEST_TYPE + "_" + "threshold.jpg", thresholdMat);
        Imgcodecs.imwrite(IMAGE_WRITE_PATH + TEST_TYPE + "_" + "all.jpg", all);
    }


    @Test
    public void testExamplePipeline() {
        String TEST_TYPE = "ExamplePipeline";
        ExamplePipeLine examplePipeLine = new ExamplePipeLine();
        Mat outputMat = examplePipeLine.processFrame(inputMat);
        saveMatAsRGB(IMAGE_WRITE_PATH + TEST_TYPE + "_" + "pipeline_default.jpg",outputMat);
    }

    @Test
    public void testUGContourRingPipeline() {
        String TEST_TYPE = "ContourRing";
        UGContourRingPipeline ugContourRingPipeline = new UGContourRingPipeline();
        Mat testMat, outputMat;
        UGContourRingPipeline.Height detectedHeight;
        for (TestCaseRings ringsTest: this.testCaseRings) {
            testMat = ringsTest.getMat();
            outputMat = ugContourRingPipeline.processFrame(testMat);
            detectedHeight =  ugContourRingPipeline.getHeight();
            saveMatAsRGB(IMAGE_WRITE_PATH + TEST_TYPE + "_" + detectedHeight.toString() + "_" +
                ringsTest.imageName,outputMat);
//            System.out.println( detectedHeight.toString());
            Assert.assertEquals(ringsTest.heightEnum,detectedHeight);
        }
    }

    @Test
    public void testUGRectRingPipeline() {
        String TEST_TYPE = "RectRing";
        Mat testMat, outputMat;
        UGRectDetector.Stack stackHeight;
        UGRectRingPipeline ugRectRingPipeline = new UGRectRingPipeline();
        // Position Detection Rectangles for Pipeline
        ugRectRingPipeline.setRectangleHeight(10);
        ugRectRingPipeline.setRectangleWidth(50);
        ugRectRingPipeline.setThreshold(50);
        ugRectRingPipeline.setTopRectHeightPercentage(0.5);
        ugRectRingPipeline.setTopRectWidthPercentage(0.5);
        ugRectRingPipeline.setBottomRectHeightPercentage(0.58);
        ugRectRingPipeline.setBottomRectWidthPercentage(0.5);

        for (TestCaseRings ringsTest: this.testCaseRings) {
            testMat = ringsTest.getMat();
            outputMat = ugRectRingPipeline.processFrame(testMat);
            stackHeight = UGRectDetector.getStack(ugRectRingPipeline);
            saveMatAsRGB(IMAGE_WRITE_PATH + TEST_TYPE + "_" + //stackHeight.toString() + "_" +
                    ringsTest.imageName,outputMat);
//            System.out.println( detectedHeight.toString());
            Assert.assertEquals(ringsTest.heightEnum.toString(),stackHeight.toString());
        }
    }



//    @Test
    public void createFolders() {
        Integer imageNumber = 0;
        File directory = new File(IMAGE_WRITE_PATH);
        if(!directory.exists()) {
            directory.mkdir();
        }

        File writeLocation = new File(directory.getPath() + "/" + "img_" + imageNumber.toString() + ".jpg");
        while(writeLocation.exists()) {
            ++imageNumber;
            writeLocation = new File(directory.getPath() + "/" + "img_" + imageNumber.toString() + ".jpg");

        }
        System.out.println(directory.getPath());
        System.out.println(writeLocation.getPath());
        saveMatAsRGB(writeLocation.getPath(), inputMat);

    }

//    @Test
    public void swapColorChannels() {
        String INPUT = "./TestData/colorSwap/input";
        String OUTPUT = "./TestData/colorSwap/output";
        File dir = new File(INPUT);
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            for(File child: directoryListing) {
                Mat imageMat = Imgcodecs.imread(child.getPath());
                saveMatAsRGB(OUTPUT + "/" + child.getName(), imageMat);
            }
        }
    }


}


