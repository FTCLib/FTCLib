package com.arcrobotics.ftclib.vision;

import org.opencv.core.Mat;

import static com.arcrobotics.ftclib.vision.VisionTestHelper.loadMatFromBGR;




class TestCase
{

    public static class TestCaseRings
    {
        public String imagePath;
        public String imageName;
//        public int ringsPresent;
        public UGContourRingPipeline.Height heightEnum;

        public TestCaseRings() {
        }

        public TestCaseRings(String imagePath, String imageName, UGContourRingPipeline.Height heightEnum) {
            this.imagePath = imagePath;
            this.imageName = imageName;
            this.heightEnum = heightEnum;
        }

        public Mat getMat() {
            return loadMatFromBGR(this.imagePath + this.imageName);
        }
    }

}