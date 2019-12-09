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

        /**The distance.  */
        private double target;

        /**The threshold for the actual distance to be in for the target to be reached. */
        private double threshold;

        /** User-defined name for the target. Optional. */
        private String name;

        /** User-defined unit of distance. This unit applies to both the threshold and the target */
        private DistanceUnit unit;

        /** Target <b>must</b> be within the range of the Rev 2m Sensor, which is 2 meters */
        public DistanceTarget(DistanceUnit unit, double target) {
            if(unit.toMeters(target) >= 2) throw new IllegalArgumentException("Target value:" + target +" over 2 meters!");
            this.unit = unit;
            this.target = target;
            threshold = 5;
            name = "Distance Target";
        }

        public DistanceTarget(DistanceUnit unit , double target, double threshold){
            if(unit.toMeters(target) >= 2) throw new IllegalArgumentException("Target value:" + target +" over 2 meters!");
            this.unit = unit;
            this.target = target;
            this.threshold = threshold;
            name = "Distance Target";
        }

        public DistanceTarget(DistanceUnit unit, double target, double threshold, String name){
            if(unit.toMeters(target) >= 2) throw new IllegalArgumentException("Target value:" + target +" over 2 meters!");
            this.unit = unit;
            this.target = target;
            this.threshold = threshold;
            this.name = name;
        }

        public boolean atTarget( double currentDistance){
            currentDistance = unit.fromUnit(unit, currentDistance);
            boolean withinRange;
            if ((Range.clip(currentDistance, currentDistance-threshold, currentDistance+threshold)) >= currentDistance+threshold) withinRange = false;
            else if(((Range.clip(currentDistance, currentDistance-threshold, currentDistance+threshold)) <= currentDistance+threshold)) withinRange = false;
            else  withinRange = true;
            return withinRange;
        }


        public void setTarget(double target){
            this.target = target;
        }

        public void setUnit(DistanceUnit unit){
            this.target = unit.fromUnit(this.unit, target);
            this.threshold = unit.fromUnit(this.unit, threshold);
            this.unit = unit;
        }

        public void setName(String name){
            this.name = name;
        }

        public DistanceUnit getUnit() {
            return unit;
        }

        public double getThreshold() {
            return threshold;
        }

        public double getTarget() {
            return target;
        }
        public String getName() {
            return this.name;
        }
    }


    /**Our distance sensor object.
     *
     * @see DistanceSensor
     *
     */


    private DistanceSensor distanceSensor;

    /** The List that holds the {@code DistanceTarget} (s) associated with this device.
     *
     * @see DistanceTarget
     * */
    private ArrayList<DistanceTarget> targetList;


    /** Makes a distance sensor from an FTC DistanceSensor device. */
    public SensorTOFDistance(DistanceSensor distanceSensor){
        this.distanceSensor = distanceSensor;
        targetList = new ArrayList<>();
    }

    /**Makes a distance sensor object from a given HardwareMap and name. */
    public SensorTOFDistance(HardwareMap hardwareMap, String name){
        this.distanceSensor = hardwareMap.get(DistanceSensor.class, name);
        targetList = new ArrayList<>();
    }


    /** Makes a distance sensor from an FTC DistanceSensor device and a given list of DistanceTargets */
    public SensorTOFDistance(DistanceSensor distanceSensor, ArrayList<DistanceTarget> targetList){
        this.distanceSensor = distanceSensor;
        this.targetList = new ArrayList<>(targetList);
    }

    /** Makes a distance sensor from a given HardwareMap and name and a given list of DistanceTargets */
    public SensorTOFDistance(HardwareMap hardwareMap, String name, ArrayList<DistanceTarget> targetList){
        this.distanceSensor = hardwareMap.get(DistanceSensor.class, name);
        this.targetList = new ArrayList<>(targetList);
    }


    /**Returns the current distance */
    public double getDistance(DistanceUnit unit){
        return distanceSensor.getDistance(unit);
    }

    /**Returns whether a given DistanceTarget has been reached */
    public boolean targetReached(DistanceTarget target){
        return target.atTarget( getDistance(target.getUnit()));
    }

    /** Adds a DistanceTarget. */
    public void addTarget(DistanceTarget target){
        if(!targetList.contains(target)) targetList.add(target);
    }

    /**Adds an ArrayList of DistanceTargets to the targets associated with this device. */
    public void addTargets(ArrayList<DistanceTarget> targets){

        for(DistanceTarget target : targets){
            if(!targetList.contains(target)) targetList.add(target);
        }
    }


    public ArrayList<Boolean> checkAllTargets(){
        ArrayList<Boolean> results = new ArrayList<>();
        for(DistanceTarget target : targetList){
            if (target.atTarget(getDistance(target.unit))) results.add(true);
            else results.add(false);
        }
        return results;
    }





    @Override
    public void disable(){

    }

    @Override
    public String getDeviceType(){
        return "TOF Distance Sensor";
    }

}
