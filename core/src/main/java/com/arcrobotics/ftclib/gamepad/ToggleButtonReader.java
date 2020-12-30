package com.arcrobotics.ftclib.gamepad;

import java.util.function.BooleanSupplier;

/**
 * Class gets the current state of a toggle button
 */
public class ToggleButtonReader extends ButtonReader {

    private boolean currToggleState;

    /**
     * The constructor that uses the gamepad and button to refer to a certain state toggler.
     *
     * @param gamepad the gamepad object that contains the buttonn
     * @param button  the button on the oject
     */
    public ToggleButtonReader(GamepadEx gamepad, GamepadKeys.Button button) {
        super(gamepad, button);

        currToggleState = false;
    }

    /**
     * The constructor that checks the values returned by a boolean supplier
     * object.
     *
     * @param buttonValue the value supplier
     */
    public ToggleButtonReader(BooleanSupplier buttonValue) {
        super(buttonValue);

        currToggleState = false;
    }

    /**
     * @return the current state of the toggler
     */
    public boolean getState() {
        if (wasJustReleased()) {
            currToggleState = !currToggleState;
        }
        return (currToggleState);
    }

}
