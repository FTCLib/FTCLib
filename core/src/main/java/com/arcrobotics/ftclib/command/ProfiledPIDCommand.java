/*----------------------------------------------------------------------------*/
/* Copyright (c) 2019-2020 FIRST. All Rights Reserved.                        */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package com.arcrobotics.ftclib.command;

import androidx.annotation.NonNull;

import com.arcrobotics.ftclib.controller.wpilibcontroller.ProfiledPIDController;

import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

import static com.arcrobotics.ftclib.trajectory.TrapezoidProfile.State;

/**
 * A command that controls an output with a {@link ProfiledPIDController}.  Runs forever by
 * default - to add
 * exit conditions and/or other behavior, subclass this class.  The controller calculation and
 * output are performed synchronously in the command's execute() method.
 */
public class ProfiledPIDCommand extends CommandBase {

    protected final ProfiledPIDController m_controller;
    protected DoubleSupplier m_measurement;
    protected Supplier<State> m_goal;
    protected BiConsumer<Double, State> m_useOutput;

    /**
     * Creates a new PIDCommand, which controls the given output with a ProfiledPIDController.
     * Goal velocity is specified.
     *
     * @param controller        the controller that controls the output.
     * @param measurementSource the measurement of the process variable
     * @param goalSource        the controller's goal
     * @param useOutput         the controller's output
     * @param requirements      the subsystems required by this command
     */
    public ProfiledPIDCommand(@NonNull ProfiledPIDController controller, @NonNull DoubleSupplier measurementSource,
                              @NonNull Supplier<State> goalSource, @NonNull BiConsumer<Double, State> useOutput,
                              Subsystem... requirements) {

        m_controller = controller;
        m_useOutput = useOutput;
        m_measurement = measurementSource;
        m_goal = goalSource;
        m_requirements.addAll(Arrays.asList(requirements));
    }

    /**
     * Creates a new PIDCommand, which controls the given output with a ProfiledPIDController.
     * Goal velocity is implicitly zero.
     *
     * @param controller        the controller that controls the output.
     * @param measurementSource the measurement of the process variable
     * @param goalSource        the controller's goal
     * @param useOutput         the controller's output
     * @param requirements      the subsystems required by this command
     */
    public ProfiledPIDCommand(@NonNull ProfiledPIDController controller, @NonNull DoubleSupplier measurementSource,
                              @NonNull DoubleSupplier goalSource, @NonNull BiConsumer<Double, State> useOutput,
                              Subsystem... requirements) {

        m_controller = controller;
        m_useOutput = useOutput;
        m_measurement = measurementSource;
        m_goal = () -> new State(goalSource.getAsDouble(), 0);
        m_requirements.addAll(Arrays.asList(requirements));
    }

    /**
     * Creates a new PIDCommand, which controls the given output with a ProfiledPIDController. Goal
     * velocity is specified.
     *
     * @param controller        the controller that controls the output.
     * @param measurementSource the measurement of the process variable
     * @param goal              the controller's goal
     * @param useOutput         the controller's output
     * @param requirements      the subsystems required by this command
     */
    public ProfiledPIDCommand(ProfiledPIDController controller, DoubleSupplier measurementSource,
                              State goal, BiConsumer<Double, State> useOutput,
                              Subsystem... requirements) {
        this(controller, measurementSource, () -> goal, useOutput, requirements);
    }

    /**
     * Creates a new PIDCommand, which controls the given output with a ProfiledPIDController. Goal
     * velocity is implicitly zero.
     *
     * @param controller        the controller that controls the output.
     * @param measurementSource the measurement of the process variable
     * @param goal              the controller's goal
     * @param useOutput         the controller's output
     * @param requirements      the subsystems required by this command
     */
    public ProfiledPIDCommand(ProfiledPIDController controller, DoubleSupplier measurementSource,
                              double goal, BiConsumer<Double, State> useOutput,
                              Subsystem... requirements) {
        this(controller, measurementSource, () -> goal, useOutput, requirements);
    }

    @Override
    public void initialize() {
        m_controller.reset(m_measurement.getAsDouble());
    }

    @Override
    public void execute() {
        m_useOutput.accept(m_controller.calculate(m_measurement.getAsDouble(), m_goal.get()),
                m_controller.getSetpoint());
    }

    @Override
    public void end(boolean interrupted) {
        m_useOutput.accept(0.0, new State());
    }

    /**
     * Returns the ProfiledPIDController used by the command.
     *
     * @return The ProfiledPIDController
     */
    public ProfiledPIDController getController() {
        return m_controller;
    }

}