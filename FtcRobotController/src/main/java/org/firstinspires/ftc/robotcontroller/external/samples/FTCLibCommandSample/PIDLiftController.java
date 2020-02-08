package org.firstinspires.ftc.robotcontroller.external.samples.FTCLibCommandSample;

public class PIDLiftController {

    private SimpleLinearLift m_lift;

    public final double STAGE_CONSTANT;

    public PIDLiftController(SimpleLinearLift lift) {
        m_lift = lift;

        STAGE_CONSTANT = lift.m_liftMotor.COUNTS_PER_REV;
    }

    // let's say that one stage is 3.2 rotations of the motor
    // but, further stages require further ticks because of gravity

    public void setStageOne() {
        m_lift.moveToPosition(STAGE_CONSTANT);
    }

    public void setStageTwo() {
        m_lift.moveToPosition( 2 * STAGE_CONSTANT + 8);
    }

    public void setStageThree() {
        m_lift.moveToPosition(3 * STAGE_CONSTANT + 14);
    }

    public void resetStage() {
        m_lift.moveToPosition(0);
    }

    public void power(double speed) {
        m_lift.moveLift(speed);
    }

}
