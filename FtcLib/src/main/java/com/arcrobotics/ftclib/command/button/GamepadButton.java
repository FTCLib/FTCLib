package com.arcrobotics.ftclib.command.button;

import com.arcrobotics.ftclib.gamepad.GamepadEx;
import com.arcrobotics.ftclib.gamepad.GamepadKeys;

/**
 * A {@link Button} that gets its state from a {@link GamepadEx}.
 *
 * @author Jackson
 */
public class GamepadButton extends Button {

    private final GamepadEx m_gamepad;
    private final GamepadKeys.Button m_button;

    /**
     * Creates a gamepad button for triggering commands.
     *
     * @param gamepad   the gamepad with the buttons
     * @param button    the specified button
     */
    public GamepadButton(GamepadEx gamepad, GamepadKeys.Button button) {
        m_gamepad = gamepad;
        m_button = button;
    }

    /**
     * Gets the value of the joystick button.
     *
     * @return The value of the joystick button
     */
    @Override
    public boolean get() {
        return m_gamepad.getButton(m_button);
    }

}
