package com.arcrobotics.ftclib;

import com.arcrobotics.ftclib.util.Safety;

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
     * Sets the safety mode for the robot.
     *
     * @param safety The requested mode of safety for the robot.
     */
    public void setSafetyMode(Safety safety) {
        m_safety = safety;
    }

}
