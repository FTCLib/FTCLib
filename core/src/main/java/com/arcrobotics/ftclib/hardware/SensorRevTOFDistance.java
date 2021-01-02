package com.arcrobotics.ftclib.hardware;

import com.qualcomm.robotcore.hardware.DistanceSensor;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Class for a time-of-flight distance sensor
 */
public class SensorRevTOFDistance implements SensorDistanceEx {

    /**
     * Our distance sensor object.
     *
     * @see DistanceSensor
     */
    private final DistanceSensor distanceSensor;

    /**
     * The List that holds the {@code DistanceTarget} (s) associated with this device.
     *
     * @see DistanceTarget
     */
    private final List<DistanceTarget> targetList;

    /**
     * Makes a distance sensor from an FTC DistanceSensor device.
     *
     * @param distanceSensor the FTC DistanceSensor device
     */
    public SensorRevTOFDistance(DistanceSensor distanceSensor) {
        this.distanceSensor = distanceSensor;
        targetList = new ArrayList<>();
    }

    /**
     * Makes a distance sensor object from a given HardwareMap and name.
     *
     * @param hardwareMap the hardware map the DistanceSensor is registered to
     * @param name        the name of the DistanceSensor
     */
    public SensorRevTOFDistance(HardwareMap hardwareMap, String name) {
        this.distanceSensor = hardwareMap.get(DistanceSensor.class, name);
        targetList = new ArrayList<>();
    }

    /**
     * Makes a distance sensor from an FTC DistanceSensor device and a given list of DistanceTargets
     *
     * @param distanceSensor the DistanceSensor object
     * @param targetList     an ArrayList of DistanceTargets for the SensorTOFDistance
     */
    public SensorRevTOFDistance(DistanceSensor distanceSensor, List<DistanceTarget> targetList) {
        this.distanceSensor = distanceSensor;
        this.targetList = new ArrayList<>(targetList);
    }

    /**
     * Makes a distance sensor from a given HardwareMap and name and a given list of DistanceTargets
     *
     * @param hardwareMap the HardwareMap the DistanceSensor is registered to
     * @param name        the name of the DistanceSensor on the hardwareMap
     * @param targetList  the ArrayList of DistanceTargets for the SensorTFODistance
     */
    public SensorRevTOFDistance(HardwareMap hardwareMap, String name, List<DistanceTarget> targetList) {
        this.distanceSensor = hardwareMap.get(DistanceSensor.class, name);
        this.targetList = new ArrayList<>(targetList);
    }

    @Override
    public double getDistance(DistanceUnit unit) {
        return distanceSensor.getDistance(unit);
    }

    @Override
    public boolean targetReached(DistanceTarget target) {
        return target.atTarget(getDistance(target.getUnit()));
    }

    @Override
    public void addTarget(DistanceTarget target) {
        if (!targetList.contains(target)) targetList.add(target);
    }

    @Override
    public void addTargets(List<DistanceTarget> targets) {

        for (DistanceTarget target : targets) {
            if (!targetList.contains(target)) targetList.add(target);
        }
    }

    @Override
    public HashMap<DistanceTarget, Boolean> checkAllTargets() {
        HashMap<DistanceTarget, Boolean> results = new HashMap<>();
        for (DistanceTarget target : targetList) {
            results.put(target, target.atTarget(getDistance(target.getUnit())));
        }
        return results;
    }

    @Override
    public void disable() {
        distanceSensor.close();
    }

    @Override
    public String getDeviceType() {
        return "TOF Rev 2m Distance Sensor";
    }

}
