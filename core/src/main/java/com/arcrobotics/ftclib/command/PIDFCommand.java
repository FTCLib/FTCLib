package com.arcrobotics.ftclib.command;

import androidx.annotation.NonNull;

import com.arcrobotics.ftclib.controller.PIDFController;

import java.util.function.BiFunction;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

public class PIDFCommand extends CommandBase {

    private final BiFunction<Double, Double, Double> m_function;
    private final PIDFController m_controller;
    protected DoubleSupplier pv, sp;
    protected DoubleConsumer m_output;

    /**
     * Constructs a PIDFCommand
     *
     * @param controller        a pidf controller
     * @param measurementSource the source of measurement for the controller
     * @param output            the desired output usage for the pidcontroller
     * @param subsystems        the subsystems required by the command
     */
    public PIDFCommand(@NonNull PIDFController controller, DoubleSupplier measurementSource,
                       DoubleConsumer output, Subsystem... subsystems) {
        m_controller = controller;
        m_output = output;
        m_function = m_controller::calculate;

        addRequirements(subsystems);
    }

    /**
     * Constructs a PIDFCommand
     *
     * @param measurementSource the source of measurement for the controller
     * @param output            the desired output usage for the pidcontroller
     * @param subsystems        the subsystems required by the command
     */
    public PIDFCommand(DoubleSupplier measurementSource, DoubleConsumer output,
                       Subsystem... subsystems) {
        this(new PIDFController(1, 0, 0, 0), measurementSource, output, subsystems);
    }

    /**
     * Sets the setpoint for the PIDFCommand
     *
     * @param sp a double representing the setpoint
     */
    public PIDFCommand setSetPoint(double sp) {
        this.sp = () -> sp;
        return this;
    }

    /**
     * Sets the setpoint for the PIDFCommand
     *
     * @param sp a supplier containing the setpoint
     */
    public PIDFCommand setSetPoint(DoubleSupplier sp) {
        this.sp = sp;
        return this;
    }

    public PIDFCommand setP(double kP) {
        m_controller.setP(kP);
        return this;
    }

    public PIDFCommand setI(double kI) {
        m_controller.setI(kI);
        return this;
    }

    public PIDFCommand setD(double kD) {
        m_controller.setD(kD);
        return this;
    }

    public PIDFCommand setF(double kF) {
        m_controller.setF(kF);
        return this;
    }

    @Override
    public void execute() {
        m_output.accept(m_function.apply(pv.getAsDouble(), sp.getAsDouble()));
    }

    @Override
    public void end(boolean interrupted) {
        m_controller.reset();
        m_output.accept(0);
    }

    @Override
    public boolean isFinished() {
        return m_controller.atSetPoint();
    }

    public PIDFController getController() {
        return m_controller;
    }

}
