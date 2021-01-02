package com.example.ftclibexamples.OldCommandSample;

public class PIDLiftController {

    private SimpleLinearLift m_lift;

    public final int STAGE_CONSTANT;

    public PIDLiftController(SimpleLinearLift lift) {
        m_lift = lift;

        STAGE_CONSTANT = (int) (lift.m_liftMotor.getCPR());
    }

    // let's say that one stage is 3.2 rotations of the motor
    // but, further stages require further ticks because of gravity

    public void setStageOne() {
        m_lift.moveToPosition(STAGE_CONSTANT);
    }

    public void setStageTwo() {
        m_lift.moveToPosition(2 * STAGE_CONSTANT + 8);
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
