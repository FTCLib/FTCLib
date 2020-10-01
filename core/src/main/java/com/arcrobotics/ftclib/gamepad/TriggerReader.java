package com.arcrobotics.ftclib.gamepad;

import org.firstinspires.ftc.robotcore.external.Telemetry;

public class TriggerReader implements KeyReader {
    private GamepadEx gamepad;
    private GamepadKeys.Trigger trigger;
    /** Last state of the button **/
    private boolean lastState;
    /** Current state of the button **/
    private boolean currState;
    private Telemetry telemetry;
    /*** Description of Button ***/
    private String triggerName;
    /** Initializes controller variables
     * @param gamepad The controller joystick
     * @param trigger The controller button
     **/
    public TriggerReader(GamepadEx gamepad, GamepadKeys.Trigger trigger) {
        this.gamepad = gamepad;
        this.trigger = trigger;
        if(this.gamepad.getTrigger(trigger) > 0.5) {
            currState = true;
        } else {
            currState = false;
        }
        lastState = currState;
        triggerName = "";
    }

    public TriggerReader(GamepadEx gamepad, GamepadKeys.Trigger trigger, String triggerName, Telemetry telemetry) {
        this.gamepad = gamepad;
        this.trigger = trigger;
        if(this.gamepad.getTrigger(trigger) > 0.5) {
            currState = true;
        } else {
            currState = false;
        }
        lastState = currState;
        this.telemetry = telemetry;
        this.triggerName = triggerName;
    }


    /** Reads button value **/
    public void readValue() {
        if(this.gamepad.getTrigger(trigger) > 0.5) {
            currState = true;
        } else {
            currState = false;
        }
        lastState = currState;
    }
    /** Checks if the button is down **/
    public boolean isDown() {
        if(triggerName != "") {
            telemetry.addData(triggerName, currState);
            telemetry.update();
        }
        return currState;
    }
    /** Checks if the button was just pressed **/
    public boolean wasJustPressed() {
        return (lastState == false && currState == true);
    }
    /** Checks if the button was just released **/
    public boolean wasJustReleased() {
        return (lastState == true && currState == false);
    }
    /** Checks if the button state has changed **/
    public boolean stateJustChanged() {
        return (lastState != currState);
    }
}
