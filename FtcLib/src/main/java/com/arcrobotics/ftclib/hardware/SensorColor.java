package com.arcrobotics.ftclib.hardware;

import android.graphics.Color;


import com.qualcomm.robotcore.hardware.ColorSensor;
import com.qualcomm.robotcore.hardware.HardwareMap;

public class SensorColor implements HardwareDevice {

    private ColorSensor colorSensor;

    /**Constructs a color sensor, defaults to ARGB */
    public SensorColor(ColorSensor colorSensor){
        this.colorSensor = colorSensor;
    }

    /** Constructs a color sensor using the given hardware map and name, defaults to ARGB */
    public SensorColor(HardwareMap hardwareMap, String name){
        this.colorSensor = hardwareMap.get(ColorSensor.class, "name");
    }

    /** Convert HSV value to an ARGB one. Includes alpha.*/
    public int[] HSVtoARGB( int alpha, float[] hsv){
        int color = Color.HSVToColor(alpha, hsv);
        return new int[] {Color.alpha(color), Color.red(color), Color.green(color), Color.blue(color)};
    }

    /** Converts an RGB value to an HSV value. Provide the float[] to be used. */
    public float[] RGBtoHSV(int red, int green, int blue, float[] hsv){
        Color.RGBToHSV(red, green, blue, hsv);
        return hsv;
    }

    /** Get all the ARGB values in an array from the sensor **/
    public int[] getARGB(){
        return new int[] {alpha(), red(), green(), blue()};
    }

    /** Gets the alpha value from the sensor */
    public int alpha() {
        return colorSensor.alpha();
    }

    /** Gets the red value from the sensor */
    public int red(){
        return colorSensor.red();
    }

    /** Gets the green value from the sensor */
    public int green() {
        return colorSensor.green();
    }

    /** Gets the blue value from the sensor */
    public int blue() {
        return colorSensor.blue();
    }

    @Override
    public void disable() {
        // somehow disable the color sensor..?
    }

    @Override
    public String getDeviceType() {
        return "Color Sensor";
    }

    /*
    alpha(int) to extract the alpha component
    red(int) to extract the red component
    green(int) to extract the green component
    blue(int) to extract the blue component

     */

    //public static Color valueOf (float r,
    //                float g,
    //                float b,
    //                float a)

}