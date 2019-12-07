/*
Copyright (c) 2016 Robert Atkinson

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of Robert Atkinson nor the names of his contributors may be used to
endorse or promote products derived from this software without specific prior
written permission.

NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.qualcomm.ftccommon;

import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.usb.RobotUsbModule;

/**
 * UsbModuleAttachmentHandler is a notification interface through which policies for dealing
 * with the attachment and detachment can be provided.
 *
 * @see FtcEventLoop#setUsbModuleAttachmentHandler(UsbModuleAttachmentHandler)
 */
public interface UsbModuleAttachmentHandler
    {
    /**
     * One of the hardware modules in the current robot configuration (such as a Modern Robotics
     * DC Motor Controller, a Modern Robotics Legacy Module, etc) is now newly attached to the
     * system (or so we think). The code in this method should deal with this event by, e.g.,
     * putting the module into a correctly operating state.
     *
     * @param module the module which is newly attached
     * @throws RobotCoreException
     * @throws InterruptedException
     */
    void handleUsbModuleAttach(RobotUsbModule module) throws RobotCoreException, InterruptedException;

    /**
     * One of the hardware modules in the current robot configuration (such as a Modern Robotics
     * DC Motor Controller, a Modern Robotics Legacy Module, etc) has been disconnected from the
     * system. The code in this method should deal with this event by, e.g., putting the module in
     * a state where it merely pretends to function.
     *
     * @param module the module which has become disconnected
     * @throws RobotCoreException
     * @throws InterruptedException
     */
    void handleUsbModuleDetach(RobotUsbModule module) throws RobotCoreException, InterruptedException;
    }
