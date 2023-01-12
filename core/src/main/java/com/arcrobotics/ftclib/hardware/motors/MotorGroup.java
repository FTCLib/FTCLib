package com.arcrobotics.ftclib.hardware.motors;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Allows multiple {@link Motor} objects to be linked together
 * as a single group. Multiple motors will act together.
 *
 * @author Jackson
 */
public class MotorGroup extends Motor implements Iterable<Motor> {

    private final Motor[] group;

    /**
     * Create a new MotorGroup with the provided Motors.
     *
     * @param leader    The leader motor.
     * @param followers The follower motors which follow the leader motor's protocols.
     */
    public MotorGroup(@NonNull Motor leader, Motor... followers) {
        group = new Motor[followers.length + 1];
        group[0] = leader;
        System.arraycopy(followers, 0, group, 1, followers.length);
    }

    /**
     * Set the speed for each motor in the group
     *
     * @param speed The speed to set. Value should be between -1.0 and 1.0.
     */
    @Override
    public void set(double speed) {
        group[0].set(speed);
        for (int i = 1; i < group.length; i++) {
            group[i].set(group[0].get());
        }
    }

    /**
     * @return The speed as a percentage of output
     */
    @Override
    public double get() {
        return group[0].get();
    }

    /**
     * @return All motor target speeds as a percentage of output
     */
    public List<Double> getSpeeds() {
        return Arrays.stream(group)
                .map(Motor::get)
                .collect(Collectors.toList());
    }

    @Override
    public double getVelocity() {
        return group[0].getCorrectedVelocity();
    }

    /**
     * @return All current velocities of the motors in the group in units of distance
     * per second which is by default ticks / second
     */
    public List<Double> getVelocities() {
        return Arrays.stream(group)
                .map(Motor::getRate)
                .collect(Collectors.toList());
    }

    @NonNull
    @Override
    public Iterator<Motor> iterator() {
        return Arrays.asList(group).iterator();
    }

    @Override
    public Encoder setDistancePerPulse(double distancePerPulse) {
        Encoder leaderEncoder = group[0].setDistancePerPulse(distancePerPulse);
        for (int i = 1; i < group.length; i++) {
            group[i].setDistancePerPulse(distancePerPulse);
        }
        return leaderEncoder;
    }

    /**
     * @return The position of every motor in the group in units of distance
     * which is by default ticks
     */
    public List<Double> getPositions() {
        return Arrays.stream(group)
                .map(Motor::getDistance)
                .collect(Collectors.toList());
    }

    @Override
    public void setRunMode(RunMode runmode) {
        group[0].setRunMode(runmode);
    }

    @Override
    public void setZeroPowerBehavior(ZeroPowerBehavior behavior) {
        for (Motor motor : group) {
            motor.setZeroPowerBehavior(behavior);
        }
    }

    @Override
    public void resetEncoder() {
        group[0].resetEncoder();
    }

    @Override
    public void stopAndResetEncoder() {
        group[0].stopAndResetEncoder();
    }

    @Override
    public void setPositionCoefficient(double kp) {
        group[0].setPositionCoefficient(kp);
    }

    @Override
    public boolean atTargetPosition() {
        return group[0].atTargetPosition();
    }

    @Override
    public void setTargetPosition(int target) {
        group[0].setTargetPosition(target);
    }

    @Override
    public void setTargetDistance(double target) {
        group[0].setTargetDistance(target);
    }

    @Override
    public void setPositionTolerance(double tolerance) {
        group[0].setPositionTolerance(tolerance);
    }

    @Override
    public void setVeloCoefficients(double kp, double ki, double kd) {
        group[0].setVeloCoefficients(kp, ki, kd);
    }

    @Override
    public void setFeedforwardCoefficients(double ks, double kv) {
        group[0].setFeedforwardCoefficients(ks, kv);
    }

    @Override
    public void setFeedforwardCoefficients(double ks, double kv, double ka) {
        group[0].setFeedforwardCoefficients(ks, kv, ka);
    }

    /**
     * @return true if the motor group is inverted
     */
    @Override
    public boolean getInverted() {
        return group[0].getInverted();
    }

    /**
     * Set the motor group to the inverted direction or forward direction.
     * This directly affects the speed rather than the direction.
     *
     * @param isInverted The state of inversion true is inverted.
     */
    @Override
    public void setInverted(boolean isInverted) {
        for (Motor motor : group) {
            motor.setInverted(isInverted);
        }
    }

    /**
     * Disables all the motor devices.
     */
    @Override
    public void disable() {
        for (Motor x : group) {
            x.disable();
        }
    }

    /**
     * @return a string characterizing the device type
     */
    @Override
    public String getDeviceType() {
        return "Motor Group";
    }

    /**
     * Stops all motors in the group.
     */
    @Override
    public void stopMotor() {
        for (Motor x : group) {
            x.stopMotor();
        }
    }

}
