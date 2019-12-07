package com.qualcomm.robotcore.exception;

public class TargetPositionNotSetException extends RuntimeException {
    public TargetPositionNotSetException() {
        super("Failed to enable motor. You must set a target position before switching to RUN_TO_POSITION mode");
    }
}
