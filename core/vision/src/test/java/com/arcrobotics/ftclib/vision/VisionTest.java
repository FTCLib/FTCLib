package com.arcrobotics.ftclib.vision;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.openftc.easyopencv.OpenCvPipeline;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

    String IMAGE_READ_PATH = "./TestData/openCV_input/skystone/";
    String IMAGE_WRITE_PATH = "./TestData/openCV_output/skystone/";


    Mat input = new Mat();
    Mat inputBlue = new Mat();
    Mat inputRed = new Mat();

    @Before
    public void initialize() {
        this.input = loadMatFromBGR(IMAGE_READ_PATH + "iphone7_27inches_by_2.5_up_inches_left.jpg" );
//        this.inputBlue = loadMatFromBGR(IMAGE_READ_PATH + "blue_right_1.jpg" );
//        this.inputRed = loadMatFromBGR(IMAGE_READ_PATH + "red_center_1.jpg" );
    }

    @Test
    public void imageRead() {
        System.out.println("Input Width: " + input.width() + "   Input Height: " + input.height());
        assertThat(input.width()).isAtLeast(1);
        assertThat(input.height()).isAtLeast(1);
    }

    @Test
    public void imageWrite() {
        String writePath = IMAGE_WRITE_PATH + "writeTestImage.jpg";
        saveMatAsRGB(writePath, input);
        File outputFile = new File(writePath);
        assertThat(outputFile.exists()).isTrue();
    }

    @Test
    public void colorThresholding() {
        Mat yCbCrChan2Mat = new Mat();
        Mat thresholdMat = new Mat();
        Mat all = new Mat();
        List<MatOfPoint> contoursList = new ArrayList<>();

        Imgproc.cvtColor(input, yCbCrChan2Mat, Imgproc.COLOR_RGB2YCrCb);//converts rgb to ycrcb
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
                        input.cols()*0.25,
                        input.rows()*0.25),
                new Point(
                        input.cols()*0.75,
                        input.rows()*0.75),
                new Scalar(0, 255, 0), 3);


        Imgcodecs.imwrite(IMAGE_WRITE_PATH + "yCbCr.jpg", yCbCrChan2Mat);
        Imgcodecs.imwrite(IMAGE_WRITE_PATH + "threshold.jpg", thresholdMat);
        Imgcodecs.imwrite(IMAGE_WRITE_PATH + "all.jpg", all);
    }


//    @Test
//    public void testDefaultPipeline() {
//        AveragingPipeline testPipeline = new AveragingPipeline();
//        Mat outputMat = testPipeline.processFrame(input);
//        saveMatAsRGB(IMAGE_WRITE_PATH + "pipeline_default.jpg",outputMat);
//        testPipeline.getStatus();

//        testPipeline.saveInputImage(IMAGE_WRITE_PATH + "anotherFolder/");
//    }

//    @Test
//    public void testBluePipeline() {
//        AveragingPipeline testPipeline = SkystoneDetector.getAveragingPipelineForBlue();
//        Mat outputMat = testPipeline.processFrame(inputBlue);
//        saveMatAsRGB(IMAGE_WRITE_PATH + "pipeline_blue.jpg",outputMat);
//        testPipeline.getStatus();
//        System.out.println(SkystoneDetector.getSkystoneRelativeLocation(testPipeline, AllianceColor.BLUE));
//    }

//    @Test
//    public void testRedPipeline() {
//        AveragingPipeline testPipeline = SkystoneDetector.getAveragingPipelineForRed();
//        Mat outputMat = testPipeline.processFrame(inputRed);
//        saveMatAsRGB(IMAGE_WRITE_PATH + "pipeline_red.jpg",outputMat);
//        testPipeline.getStatus();
//        System.out.println(SkystoneDetector.getSkystoneRelativeLocation(testPipeline, AllianceColor.RED));
//    }

//    @Test
//    public void testCustomPipeline() {
//        ArrayList<AveragingPipeline.NormalizedRectangle> scanRegions = new ArrayList<>();
//        double yPosition = 0.55;
//        double[] normalizedSize = {0.08, 0.10};
//        scanRegions.add(new NormalizedRectangle(0.07,yPosition,normalizedSize[0],normalizedSize[1]));
//        scanRegions.add(new NormalizedRectangle(0.18,yPosition,normalizedSize[0],normalizedSize[1]));
//        scanRegions.add(new NormalizedRectangle(0.3,yPosition-0.1,normalizedSize[0],normalizedSize[1]));
//        scanRegions.add(new NormalizedRectangle(0.35,yPosition+0.1,normalizedSize[0],normalizedSize[1]));
//        scanRegions.add(new NormalizedRectangle(0.5,yPosition,normalizedSize[0],normalizedSize[1]));
//        scanRegions.add(new NormalizedRectangle(0.75,yPosition,normalizedSize[0],normalizedSize[1]));
//        AveragingPipeline testPipeline = new AveragingPipeline(scanRegions);
//        Mat outputMat = testPipeline.processFrame(input);
//        saveMatAsRGB(IMAGE_WRITE_PATH + "pipeline_custom.jpg",outputMat);
//        testPipeline.getStatus();
//
//        System.out.println(testPipeline.getSkystoneRelativeLocation());
//        testPipeline.saveInputImage(IMAGE_WRITE_PATH + "anotherFolder/");
//    }

//    @Test
//    public void testPositionCalculation() {
//        double[] fovXY_degrees = {78,78};
//        double[] relativePositionXY_inches = {36,11};
//        double normalizedPositionX = SkystoneDetector.getNormalizedPositionX(fovXY_degrees,relativePositionXY_inches);
//        System.out.print("FOV: " + fovXY_degrees[0] + " , ");
//        System.out.print("RelativePositionXY: " + relativePositionXY_inches[0] + " , " + relativePositionXY_inches[1] +" , ");
//        System.out.println("NormalizedPositionX: " + normalizedPositionX);
//    }

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
        saveMatAsRGB(writeLocation.getPath(),input);

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


    private Mat loadMatFromBGR(String filePath) {
        Mat loadedMat = Imgcodecs.imread(filePath);
        loadedMat = cropAndResize(loadedMat,640,480);
        Imgproc.cvtColor(loadedMat, loadedMat, Imgproc.COLOR_RGB2BGR); //converts rgb to bgr
        return loadedMat;
    }


    private void saveMatAsRGB(String filePath, Mat output) {
        File file = new File(filePath);
        File directory = new File(file.getParent());
        if(!directory.exists()) {
            directory.mkdir();
        }
        Imgproc.cvtColor(output, output, Imgproc.COLOR_BGR2RGB);
        Imgcodecs.imwrite(file.getPath(), output);
    }


    private Mat cropAndResize(Mat input, int fx, int fy) {
        // First, crop the original image so it scales to the final dimensions.
        int requiredWidthFromInputHeight =  Math.round(input.height() * fx / fy);
        Mat croppedInput;
        if ( requiredWidthFromInputHeight == input.width() ) {
            // No cropping needed
            croppedInput = new Mat();
            input.copyTo(croppedInput);
        } else if ( requiredWidthFromInputHeight < input.width() ) {
            // Trim the width
            int trimEachSideBy = Math.round((input.width() - requiredWidthFromInputHeight)/2);
            Rect cropRect = new Rect(trimEachSideBy,0,requiredWidthFromInputHeight,input.height());
            croppedInput = new Mat(input,cropRect);
        } else {
            // Trim the height
            int requiredHeightFromInputWidth = Math.round(input.width() * fy / fx);
            int trimEachSideBy = Math.round((input.height() - requiredHeightFromInputWidth)/2);
            Rect cropRect = new Rect(0,trimEachSideBy,input.width(),requiredHeightFromInputWidth);
            croppedInput = new Mat(input,cropRect);
        }

        // Now that the proportions are the same, perform the scaling and return:
        Imgproc.resize(croppedInput, croppedInput, new Size(fx,fy),0,0, Imgproc.INTER_AREA);
        return croppedInput;
    }
}
