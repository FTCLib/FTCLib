package com.qualcomm.robotcore.exception;

/**
 * Created by bob on 2016-04-20.
 */
public class DuplicateNameException extends RuntimeException
    {
    public DuplicateNameException(String message)
        {
        super(message);
        }

    public DuplicateNameException(String format, Object... args)
        {
        super(String.format(format, args));
        }

    }
