package com.arcrobotics.ftclib.vision;

import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;

public class VisionTestHelper {


    public static Mat loadMatFromBGR(String filePath) {
        Mat loadedMat = Imgcodecs.imread(filePath);
        loadedMat = cropAndResize(loadedMat,640,480);
        Imgproc.cvtColor(loadedMat, loadedMat, Imgproc.COLOR_RGB2BGR); //converts rgb to bgr
        return loadedMat;
    }


    public static void saveMatAsRGB(String filePath, Mat output) {
        File file = new File(filePath);
        File directory = new File(file.getParent());
        if(!directory.exists()) {
            directory.mkdir();
        }
        Imgproc.cvtColor(output, output, Imgproc.COLOR_BGR2RGB);
        Imgcodecs.imwrite(file.getPath(), output);
    }


    public static Mat cropAndResize(Mat input, int fx, int fy) {
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
