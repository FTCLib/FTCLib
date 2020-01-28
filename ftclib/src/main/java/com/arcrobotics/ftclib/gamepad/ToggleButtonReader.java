package com.arcrobotics.ftclib.gamepad;

public class ToggleButtonReader extends ButtonReader {
    private boolean currToggleState;


    public ToggleButtonReader(GamepadEx gamepad, GamepadKeys.Button button) {
        super(gamepad, button);

        currToggleState = false;

    }

    public boolean getState() {
        if (wasJustReleased()) {
            currToggleState = !currToggleState;
        }
        return (currToggleState);
    }
}
