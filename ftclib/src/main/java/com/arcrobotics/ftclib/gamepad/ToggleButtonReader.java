package com.arcrobotics.ftclib.gamepad;


import android.os.Build;
import android.support.annotation.RequiresApi;

import java.util.function.BooleanSupplier;

/**
 * Class gets the current state of a toggle button
 * You must call "readValue();" in a loop to get accurate values.
 */
@RequiresApi(api = Build.VERSION_CODES.N)
public class ToggleButtonReader extends ButtonReader {
    private boolean currToggleState;


    public ToggleButtonReader(GamepadEx gamepad, GamepadKeys.Button button) {
        super(gamepad, button);

        currToggleState = false;
    }

    public ToggleButtonReader(BooleanSupplier buttonValue) {
        super(buttonValue);

        currToggleState = false;
    }

    public boolean getState() {
        if (wasJustReleased()) {
            currToggleState = !currToggleState;
        }
        return (currToggleState);
    }
}
