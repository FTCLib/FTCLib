package com.example.ftclibexamples.PowerPlaySample;

import com.arcrobotics.ftclib.command.Command;
import com.arcrobotics.ftclib.command.FunctionalCommand;
import com.arcrobotics.ftclib.command.SubsystemBase;
import com.arcrobotics.ftclib.hardware.motors.MotorEx;
import com.arcrobotics.ftclib.math.controller.PIDController;

public class LiftSubsystem extends SubsystemBase {
    private final MotorEx left, right;

    private final double kP = 1;
    private final double kI = 0;
    private final double kD = 0;
    private final PIDController controller = new PIDController(kP, kI, kD);

    private int currentTarget;
    private int threshold = 20;

    public LiftSubsystem(MotorEx left, MotorEx right) {
        this.left = left;
        this.right = right;
    }

    public int getCurrentTarget() {
        return currentTarget;
    }

    /**
     * Sets the target position in ticks for the lift controller
     * @param junction junction to set target to
     */
    public void setCurrentTarget(Junction junction) {
        currentTarget = junction.getTick();
        controller.setSetPoint(currentTarget);
    }

    /**
     * @return whether the lift has reached its target position
     */
    public boolean atTarget() {
        return left.getCurrentPosition() < currentTarget + threshold &&
                left.getCurrentPosition() > currentTarget - threshold;
    }

    /**
     * Alternatively, an InstantCommand could be used since setCurrentTarget is run on initialization,
     * but, in reality, the lift does not move to its new location instantly and so when chaining commands,
     * especially in command groups, it is important to set a condition at which the command ends.
     *
     * @param junction the junction to move the lift to
     * @return a command that assigns the lift a new target
     */
    public Command goTo(Junction junction) {
        return new FunctionalCommand(
                () -> setCurrentTarget(junction),
                () -> {},
                interrupted -> {},
                this::atTarget,
                this
        );
    }

    @Override
    public void periodic() {
        double output = controller.calculate(left.getCurrentPosition());
        left.set(output);
        right.set(output);
    }
}
