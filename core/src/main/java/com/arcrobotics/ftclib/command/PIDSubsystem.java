package com.arcrobotics.ftclib.command;
// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.


import com.arcrobotics.ftclib.math.controller.PIDController;

/**
 * A subsystem that uses a {@link PIDController} to control an output. The controller is run
 * synchronously from the subsystem's periodic() method.
 *
 * <p>This class is provided by the NewCommands VendorDep
 */
public abstract class PIDSubsystem extends SubsystemBase {
    protected final PIDController m_controller;
    protected boolean m_enabled;

    /**
     * Creates a new PIDSubsystem.
     *
     * @param controller the PIDController to use
     * @param initialPosition the initial setpoint of the subsystem
     */
    public PIDSubsystem(PIDController controller, double initialPosition) {
        m_controller = controller;
        setSetpoint(initialPosition);
    }

    /**
     * Creates a new PIDSubsystem. Initial setpoint is zero.
     *
     * @param controller the PIDController to use
     */
    public PIDSubsystem(PIDController controller) {
        this(controller, 0);
    }

    @Override
    public void periodic() {
        if (m_enabled) {
            useOutput(m_controller.calculate(getMeasurement()), getSetpoint());
        }
    }

    public PIDController getController() {
        return m_controller;
    }

    /**
     * Sets the setpoint for the subsystem.
     *
     * @param setpoint the setpoint for the subsystem
     */
    public void setSetpoint(double setpoint) {
        m_controller.setSetPoint(setpoint);
    }

    /**
     * Returns the current setpoint of the subsystem.
     *
     * @return The current setpoint
     */
    public double getSetpoint() {
        return m_controller.getSetPoint();
    }

    /**
     * Uses the output from the PIDController.
     *
     * @param output the output of the PIDController
     * @param setpoint the setpoint of the PIDController (for feedforward)
     */
    protected abstract void useOutput(double output, double setpoint);

    /**
     * Returns the measurement of the process variable used by the PIDController.
     *
     * @return the measurement of the process variable
     */
    protected abstract double getMeasurement();

    /** Enables the PID control. Resets the controller. */
    public void enable() {
        m_enabled = true;
        m_controller.reset();
    }

    /** Disables the PID control. Sets output to zero. */
    public void disable() {
        m_enabled = false;
        useOutput(0, 0);
    }

    /**
     * Returns whether the controller is enabled.
     *
     * @return Whether the controller is enabled.
     */
    public boolean isEnabled() {
        return m_enabled;
    }
}

