/*----------------------------------------------------------------------------*/
/* Copyright (c) 2018-2019 FIRST. All Rights Reserved.                        */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package com.arcrobotics.ftclib.command;

import android.util.Log;

/**
 * A command that logs a message when initialized.
 */
public class LogCatCommand extends InstantCommand {

    public LogCatCommand(String tag, String message, int priority) {
        super(() -> Log.println(priority, tag, message));
    }

    public LogCatCommand(String tag, String message) {
        this(tag, message, Log.DEBUG);
    }

    public LogCatCommand(String message) {
        this("LogCatCommand", message);
    }


    public LogCatCommand(String message, int priority) {
        this("LogCatCommand", message, priority);
    }

    @Override
    public boolean runsWhenDisabled() {
        return true;
    }

}