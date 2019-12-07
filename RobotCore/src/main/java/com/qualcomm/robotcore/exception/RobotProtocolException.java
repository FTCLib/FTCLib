package com.qualcomm.robotcore.exception;

public class RobotProtocolException extends Exception {

    public RobotProtocolException(String message)
    {
        super(message);
    }

    public RobotProtocolException(String format, Object... args)
    {
        super(String.format(format, args));
    }
}
