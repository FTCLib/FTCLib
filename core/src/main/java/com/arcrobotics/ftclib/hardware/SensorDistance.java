package com.arcrobotics.ftclib.hardware;

import com.technototes.logger.Log;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

public interface SensorDistance extends HardwareDevice {




    /**
     * Gets the current distance from the sensor.
     *
     * @param unit The distance unit to return in.
     * @return the distance in the indicated unit.
     *
     **/
    @Log
    double getDistance(DistanceUnit unit);



}
