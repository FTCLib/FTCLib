package com.arcrobotics.ftclib.hardware;

import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface SensorDistanceEx extends SensorDistance {


    /**
     * Represents a target distance
     */
     class DistanceTarget {

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
            this(unit, target, threshold, "Distance Target");
        }

        /**
         * @param unit      the user-defined unit of distance
         * @param target    the target distance
         * @param threshold the acceptable error range
         * @param name      the name of the sensor
         */
        public DistanceTarget(DistanceUnit unit, double target, double threshold, String name) {
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
     * Returns whether a given DistanceTarget has been reached
     */
    boolean targetReached(SensorRevTOFDistance.DistanceTarget target);


    /**
     * Adds a DistanceTarget.
     */
     void addTarget(SensorRevTOFDistance.DistanceTarget target);


    /**
     * Adds an List of DistanceTargets to the targets associated with this device.
     */
     void addTargets(List<SensorRevTOFDistance.DistanceTarget> targets);


    /**
     * Checks all targets currently associated with this device and returns a {@code Map}
     * with the results.
     *
     * @return The results of the checking.
     */
    Map<SensorRevTOFDistance.DistanceTarget, Boolean> checkAllTargets();


}
