package com.arcrobotics.ftclib.purepursuit.actions;

import com.arcrobotics.ftclib.purepursuit.Path;

/**
 * This is an optional feature of paths. A triggered action is an action that called
 * when some condition is met. For example, you might have a triggered action that
 * triggers when the robot move above some Y value on the field.
 *
 * @author Michael Baljet, Team 14470
 * @version 1.1
 * @see Path
 */
public abstract class TriggeredAction {

    // True if doAction() has already been called.
    private boolean alreadyPerformed = false;

    /**
     * Called regularly by the path it is apart of. If the trigger condition is met this will call doAction().
     */
    public void loop() {
        if (isTriggered()) {
            doAction(alreadyPerformed);
            alreadyPerformed = true;
        }
    }

    /**
     * Resets this actions.
     */
    public void reset() {
        alreadyPerformed = false;
    }

    /**
     * Returns true if the trigger condition is met and the action should be performed.
     *
     * @return true if the trigger condition is met and the action should be performed, false otherwise.
     */
    public abstract boolean isTriggered();

    /**
     * Perform the triggered action. Automatically called when the trigger condition is met.
     *
     * @param alreadyPerformed True if the action has already been performed, false otherwise.
     */
    public abstract void doAction(boolean alreadyPerformed);

}
