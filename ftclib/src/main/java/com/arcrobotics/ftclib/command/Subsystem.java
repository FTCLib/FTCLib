package com.arcrobotics.ftclib.command;

public interface Subsystem {
    public void initialize();
    public void reset();
    public void loop();
    public void stop();
    public void disable();
}
