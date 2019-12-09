package com.arcrobotics.ftclib;

/**
 * This is the Robot class. This is where you will store your
 * subsystems. This will make your command-based robot code a lot smoother
 * and easier to understand.
 */
public class Robot {

    /**
     * The enabled safety of the robot. The default safety
     * mode is DEFAULT.
     */
    public Safety m_safety = Safety.DEFAULT;

    /**
     * The Robot class makes use of four safety modes:
     * swift, ease_off, default, and break. SWIFT performs actions quickly.
     * EASE_OFF performs actions swiftly, but as soon as the action ends, the
     * system continues to go for a while longer until deactivating. DEFAULT
     * does not have any special features and performs actions the way they normally would.
     * BREAK causes mechanisms to stop immediately after deactivation.
     */
    public enum Safety {
        SWIFT, EASE_OFF, DEFAULT, BREAK
    }

    /**
     * Sets the safety mode for the robot.
     *
     * @param safety The requested mode of safety for the robot.
     */
    public void setSafetyMode(Safety safety) {
        m_safety = safety;
    }

}
