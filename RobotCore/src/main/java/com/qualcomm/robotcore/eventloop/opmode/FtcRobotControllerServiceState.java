package com.qualcomm.robotcore.eventloop.opmode;

import com.qualcomm.robotcore.eventloop.EventLoopManager;


/**
 * Created by David on 7/7/2017.
 */

public interface FtcRobotControllerServiceState extends EventLoopManagerClient {
    EventLoopManager getEventLoopManager();
}
