package com.arcrobotics.ftclib.hardware;

import com.qualcomm.robotcore.hardware.DistanceSensor;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

import com.qualcomm.robotcore.util.Range;

import java.util.ArrayList;
import java.util.List;

/**
 * Class for a time-of-flight distance sensor
 */
public class SensorTOFDistance implements HardwareDevice {

    /**
     * Represents a target distance
     */
    public class DistanceTarget {

        /**
         * The distance.
         */
        private double target;

        /**
         * The threshold for the actual distance to be in for the target to be reached.
         * Essentially, this is the acceptable error range.
         */
        private double threshold;

        /**
         * User-defined name for the target. Optional.
         */
        private String name;

        /**
         * User-defined unit of distance. This unit applies to both the threshold and the target
         */
        private DistanceUnit unit;

        /**
         * Target <b>must</b> be within the range of the Rev 2m Sensor, which is 2 meters
         *
         * @param unit   user-defined unit of distance
         * @param target the target distance
         */
        public DistanceTarget(DistanceUnit unit, double target) {
            this(unit, target, 5);
        }

        /**
         * @param unit      user-defined unit of distance
         * @param target    the target distance
         * @param threshold the acceptable error range
         */
        public DistanceTarget(DistanceUnit unit, double target, double threshold) {
            this(unit, target, threshold, "SensorTOFDistance");
        }

        /**
         * @param unit      the user-defined unit of distance
         * @param target    the target distance
         * @param threshold the acceptable error range
         * @param name      the name of the sensor
         */
        public DistanceTarget(DistanceUnit unit, double target, double threshold, String name) {
            if (unit.toMeters(target) >= 2)
                throw new IllegalArgumentException("Target value:" + target + " over 2 meters!");
            this.unit = unit;
            this.target = target;
            this.threshold = threshold;
            this.name = name;
        }

        /**
         * Determines if the sensor reached the target value (within the threshold error range)
         *
         * @param currentDistance the current distance to the target
         * @return whether the the sensor reached the the target value (within the threshold error range)
         */
        public boolean atTarget(double currentDistance) {
            currentDistance = unit.fromUnit(unit, currentDistance);
            boolean withinRange;
            if ((Range.clip(currentDistance, currentDistance - threshold, currentDistance + threshold)) >= currentDistance + threshold)
                withinRange = false;
            else if (((Range.clip(currentDistance, currentDistance - threshold, currentDistance + threshold)) <= currentDistance + threshold))
                withinRange = false;
            else withinRange = true;
            return withinRange;
        }


        /**
         * Change the target value
         *
         * @param target the new target
         */
        public void setTarget(double target) {
            this.target = target;
        }

        /**
         * Changes the unit of measurement
         *
         * @param unit the new unit value
         */
        public void setUnit(DistanceUnit unit) {
            this.target = unit.fromUnit(this.unit, target);
            this.threshold = unit.fromUnit(this.unit, threshold);
            this.unit = unit;
        }

        /**
         * Changes the name of the sensor
         *
         * @param name the new name of the sensor
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Gets the unit of distance
         *
         * @return the unit of distance
         */
        public DistanceUnit getUnit() {
            return unit;
        }

        /**
         * Gets the acceptable error range
         *
         * @return the threshold (acceptable error range)
         */
        public double getThreshold() {
            return threshold;
        }

        /**
         * @return the target distance
         */
        public double getTarget() {
            return target;
        }

        /**
         * @return the name of the sensor
         */
        public String getName() {
            return this.name;
        }
    }


    /**
     * Our distance sensor object.
     *
     * @see DistanceSensor
     */
    private DistanceSensor distanceSensor;

    /**
     * The List that holds the {@code DistanceTarget} (s) associated with this device.
     *
     * @see DistanceTarget
     */
    private ArrayList<DistanceTarget> targetList;

    /**
     * Makes a distance sensor from an FTC DistanceSensor device.
     *
     * @param distanceSensor the FTC DistanceSensor device
     */
    public SensorTOFDistance(DistanceSensor distanceSensor) {
        this.distanceSensor = distanceSensor;
        targetList = new ArrayList<>();
    }

    /**
     * Makes a distance sensor object from a given HardwareMap and name.
     *
     * @param hardwareMap the hardware map the DistanceSensor is registered to
     * @param name        the name of the DistanceSensor
     */
    public SensorTOFDistance(HardwareMap hardwareMap, String name) {
        this.distanceSensor = hardwareMap.get(DistanceSensor.class, name);
        targetList = new ArrayList<>();
    }


    /**
     * Makes a distance sensor from an FTC DistanceSensor device and a given list of DistanceTargets
     *
     * @param distanceSensor the DistanceSensor object
     * @param targetList     an ArrayList of DistanceTargets for the SensorTOFDistance
     */
    public SensorTOFDistance(DistanceSensor distanceSensor, ArrayList<DistanceTarget> targetList) {
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
    public SensorTOFDistance(HardwareMap hardwareMap, String name, ArrayList<DistanceTarget> targetList) {
        this.distanceSensor = hardwareMap.get(DistanceSensor.class, name);
        this.targetList = new ArrayList<>(targetList);
    }

// TODO: complete docs for the remainder of these methods below

    /**
     * Returns the current distance
     */
    public double getDistance(DistanceUnit unit) {
        return distanceSensor.getDistance(unit);
    }

    /**
     * Returns whether a given DistanceTarget has been reached
     */
    public boolean targetReached(DistanceTarget target) {
        return target.atTarget(getDistance(target.getUnit()));
    }

    /**
     * Adds a DistanceTarget.
     */
    public void addTarget(DistanceTarget target) {
        if (!targetList.contains(target)) targetList.add(target);
    }

    /**
     * Adds an ArrayList of DistanceTargets to the targets associated with this device.
     */
    public void addTargets(ArrayList<DistanceTarget> targets) {

        for (DistanceTarget target : targets) {
            if (!targetList.contains(target)) targetList.add(target);
        }
    }


    public ArrayList<Boolean> checkAllTargets() {
        ArrayList<Boolean> results = new ArrayList<>();
        for (DistanceTarget target : targetList) {
            if (target.atTarget(getDistance(target.unit))) results.add(true);
            else results.add(false);
        }
        return results;
    }


    @Override
    public void disable() {

    }

    @Override
    public String getDeviceType() {
        return "TOF Distance Sensor";
    }

}
