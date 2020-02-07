package com.arcrobotics.ftclib.command;

/**
 * The interface for a custom susbsystem. A subsystem is a mechanism
 * that acts as its own unit on the robot. For example, an elevator consisting
 * of linear slides and a motor connected to a spool is a subsystem on the robot,
 * performing a unique action.
 */
public interface Subsystem {

    /**
     * The initilizer method. This prepares the hardware for the
     * actual movement or activation of the mechanism.
     */
    public void initialize();

    /**
     * The reset method. Returns the subsystem back to its original
     * position and resets any saved data.
     */
    public void reset();

    /**
     * Loops the subsystem until {@link #stop()} is called.
     */
    public void loop();

    /**
     * Halts the performance of the subsystem, bringing all
     * hardware devices to a stop.
     */
    public void stop();

    /**
     * Deactivates the subsystem, rendering it unusable until the
     * next initialization.
     */
    public void disable();
}
