package com.arcrobotics.ftclib.gamepad;

import com.qualcomm.robotcore.hardware.Gamepad;

/**
 * An extended gamepad for more advanced toggles, key events,
 * and other control processors.
 */
public class GamepadEx {

    /** The retrievable gamepad object */
    public Gamepad gamepad;

    /**
     * The constructor, that contains the gamepad object from the
     * opmode.
     *
     * @param gamepad the gamepad object from the opmode
     */
    public GamepadEx(Gamepad gamepad) {
        this.gamepad = gamepad;
    }

    /**
     * @param button    the button object
     * @return          the boolean value as to whether the button is active or not
     */
    public boolean getButton(GamepadKeys.Button button) {

        boolean buttonValue = false;
        switch (button) {
            case A:
                buttonValue = gamepad.a;
                break;
            case B:
                buttonValue = gamepad.b;
                break;
            case X:
                buttonValue = gamepad.x;
                break;
            case Y:
                buttonValue = gamepad.y;
                break;
            case LEFT_BUMPER:
                buttonValue = gamepad.left_bumper;
                break;
            case RIGHT_BUMPER:
                buttonValue = gamepad.right_bumper;
                break;
            case DPAD_UP:
                buttonValue = gamepad.dpad_up;
                break;
            case DPAD_DOWN:
                buttonValue = gamepad.dpad_down;
                break;
            case DPAD_LEFT:
                buttonValue = gamepad.dpad_left;
                break;
            case DPAD_RIGHT:
                buttonValue = gamepad.dpad_right;
                break;
            case BACK:
                buttonValue = gamepad.back;
                break;
            case START:
                buttonValue = gamepad.start;
                break;
            case LEFT_STICK_BUTTON:
                buttonValue = gamepad.left_stick_button;
                break;
            case RIGHT_STICK_BUTTON:
                buttonValue = gamepad.right_stick_button;
                break;
            default:
                buttonValue = false;
                break;
        }
        return buttonValue;
    }

    /**
     * @param trigger   the trigger object
     * @return          the value returned by the trigger in question
     */
    public double getTrigger(GamepadKeys.Trigger trigger) {
        double triggerValue = 0;
        switch (trigger) {
            case LEFT_TRIGGER:
                triggerValue = gamepad.left_trigger;
                break;
            case RIGHT_TRIGGER:
                triggerValue = gamepad.right_trigger;
                break;
            default:
                break;
        }
        return triggerValue;
    }

    /**
     * @return the y-value on the left analog stick
     */
    public double getLeftY() {
        return -gamepad.left_stick_y;
    }

    /**
     * @return the y-value on the right analog stick
     */
    public double getRightY() {
        return gamepad.right_stick_y;
    }

    /**
     * @return the x-value on the left analog stick
     */
    public double getLeftX() {
        return gamepad.left_stick_x;
    }

    /**
     * @return the x-value on the right analog stick
     */
    public double getRightX() {
        return gamepad.right_stick_x;
    }

}