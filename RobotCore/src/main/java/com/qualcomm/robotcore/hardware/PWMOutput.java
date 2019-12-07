package com.qualcomm.robotcore.hardware;

/**
 * Created by bob on 2016-03-12.
 */
public interface PWMOutput extends HardwareDevice
    {
    void setPulseWidthOutputTime(int usDuration);
    int getPulseWidthOutputTime();
    void setPulseWidthPeriod(int usFrame);
    int getPulseWidthPeriod();
    }
