package com.qualcomm.robotcore.hardware;

/**
 * Created by bob on 2016-03-12.
 */
public interface PWMOutputControllerEx
    {
    void setPwmEnable(int port);
    void setPwmDisable(int port);
    boolean isPwmEnabled(int port);
    }
