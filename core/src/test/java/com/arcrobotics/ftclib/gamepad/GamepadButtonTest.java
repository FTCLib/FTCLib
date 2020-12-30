package com.arcrobotics.ftclib.gamepad;

import com.arcrobotics.ftclib.command.CommandScheduler;
import com.arcrobotics.ftclib.command.InstantCommand;
import com.qualcomm.robotcore.hardware.Gamepad;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GamepadButtonTest {

    public static int x = 3;
    private Gamepad myGamepad;
    private GamepadEx gamepadEx;

    @BeforeEach
    public void setup() {
        myGamepad = new Gamepad();
        gamepadEx = new GamepadEx(myGamepad);
    }

    @Test
    public void simpleTest() {
        myGamepad.a = false;
        assertFalse(gamepadEx.getButton(GamepadKeys.Button.A));
        myGamepad.a = true;
        assertTrue(gamepadEx.getButton(GamepadKeys.Button.A));
    }

    @Test
    public void oneButtonTest() {
        myGamepad.a = false;
        assertFalse(gamepadEx.getButton(GamepadKeys.Button.A));
        gamepadEx.getGamepadButton(GamepadKeys.Button.A)
                .whenPressed(new InstantCommand(() -> x = 5));
        CommandScheduler.getInstance().run();
        assertEquals(3, x);
        myGamepad.a = true;
        CommandScheduler.getInstance().run();
        assertEquals(5, x);
        CommandScheduler.getInstance().reset();
        x = 3;
    }

    @Test
    public void whenPressedTest() {
        myGamepad.a = false;
        BooleanSupplier wasJustPressed = () -> gamepadEx.wasJustPressed(GamepadKeys.Button.A);
        assertFalse(wasJustPressed.getAsBoolean());
        myGamepad.a = true;
        assertFalse(wasJustPressed.getAsBoolean());
        gamepadEx.readButtons();
        assertTrue(wasJustPressed.getAsBoolean());
        myGamepad.a = true;
        assertTrue(wasJustPressed.getAsBoolean());
        gamepadEx.readButtons();
        assertFalse(wasJustPressed.getAsBoolean());
        myGamepad.a = false;
        assertFalse(wasJustPressed.getAsBoolean());
        gamepadEx.readButtons();
        assertFalse(wasJustPressed.getAsBoolean());
    }

    @Test
    public void twoButtonCommandTest() {
        myGamepad.a = false;
        myGamepad.b = false;
        assertFalse(gamepadEx.getButton(GamepadKeys.Button.B));
        gamepadEx.getGamepadButton(GamepadKeys.Button.A)
                .and(gamepadEx.getGamepadButton(GamepadKeys.Button.B).negate())
                .whenActive(new InstantCommand(() -> x = 5));
        gamepadEx.getGamepadButton(GamepadKeys.Button.B)
                .and(gamepadEx.getGamepadButton(GamepadKeys.Button.A).negate())
                .whenActive(new InstantCommand(() -> x = 3));
        CommandScheduler.getInstance().run();
        assertEquals(3, x);
        myGamepad.a = true;
        myGamepad.b = true;
        CommandScheduler.getInstance().run();
        assertEquals(3, x);
        myGamepad.a = true;
        myGamepad.b = false;
        CommandScheduler.getInstance().run();
        assertEquals(5, x);
        myGamepad.a = false;
        myGamepad.b = true;
        CommandScheduler.getInstance().run();
        assertEquals(3, x);
        myGamepad.a = false;
        myGamepad.b = false;
        CommandScheduler.getInstance().run();
        assertEquals(3, x);
        CommandScheduler.getInstance().reset();
    }

}
