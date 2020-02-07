package com.arcrobotics.ftclib.util;

/**
 * SWIFT performs actions quickly. EASE_OFF performs actions swiftly,
 * but as soon as the action ends, the
 * system continues to go for a while longer until deactivating. DEFAULT
 * does not have any special features and performs actions the way they normally would.
 * BREAK causes mechanisms to stop immediately after deactivation.
 */
public enum Safety {
    SWIFT(0), EASE_OFF(1), DEFAULT(2), BREAK(3);

    public final int value;

    Safety(int value) {
        this.value = value;
    }
}