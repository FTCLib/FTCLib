package com.arcrobotics.ftclib.hardware;


import com.arcrobotics.ftclib.geometry.Rotation2d;
import com.technototes.logger.Log;

public abstract class GyroEx implements HardwareDevice {




    public abstract void init();

    // Gyro
    @Log
    public abstract double getHeading();
    public abstract double getAbsoluteHeading();
    public abstract double[] getAngles();
    public abstract Rotation2d getRotation2d();
    public abstract void reset();

}
