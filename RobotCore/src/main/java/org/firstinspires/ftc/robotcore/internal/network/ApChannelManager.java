package org.firstinspires.ftc.robotcore.internal.network;

/**
 * Utility for changing the operating channel for AP mode.
 */
public interface ApChannelManager
{
    /**
     * setChannel
     *
     * Sets the operating channel of the AP
     */
    boolean setChannel(String channel);

    /**
     * resetChannel
     *
     * Reset the operating channel to the Factory default
     */
    boolean resetChannel();
}
