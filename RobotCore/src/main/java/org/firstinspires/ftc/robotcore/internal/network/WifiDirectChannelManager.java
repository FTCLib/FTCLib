package org.firstinspires.ftc.robotcore.internal.network;

//@Todo implement these methods to support wifi direct channel change.
public class WifiDirectChannelManager implements ApChannelManager
{
    @Override
    public boolean setChannel(String channel)
    {
        return false;
    }

    @Override
    public boolean resetChannel()
    {
        return false;
    }
}
