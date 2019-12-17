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
package com.qualcomm.hardware.lynx;

import android.support.annotation.NonNull;

import com.qualcomm.hardware.lynx.commands.LynxCommand;
import com.qualcomm.hardware.lynx.commands.LynxMessage;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.Engagable;
import com.qualcomm.robotcore.hardware.HardwareDevice;
import com.qualcomm.robotcore.hardware.RobotCoreLynxModule;
import com.qualcomm.robotcore.util.SerialNumber;

/**
 * LynxModuleIntf is an interface to LynxModule so as to allow for an alternate
 * substitution of PretendLynxModule when necessary.
 */
public interface LynxModuleIntf extends RobotCoreLynxModule, HardwareDevice, Engagable
    {
    SerialNumber getSerialNumber();
    <T> T acquireI2cLockWhile(Supplier<T> supplier)                     throws InterruptedException, RobotCoreException, LynxNackException;
    void acquireNetworkTransmissionLock(@NonNull LynxMessage message)   throws InterruptedException;
    void releaseNetworkTransmissionLock(@NonNull LynxMessage message)   throws InterruptedException;
    void validateCommand(LynxMessage lynxMessage)               throws LynxUnsupportedCommandException;
    boolean isCommandSupported(Class<? extends LynxCommand> command);
    void sendCommand(LynxMessage message)                       throws InterruptedException, LynxUnsupportedCommandException;
    void resetPingTimer(@NonNull LynxMessage message);
    void retransmit(LynxMessage message)                        throws InterruptedException;
    void finishedWithMessage(LynxMessage message)               throws InterruptedException;
    void noteAttentionRequired();
    int getInterfaceBaseCommandNumber(String interfaceName);
    }
