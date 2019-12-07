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
package com.qualcomm.hardware.lynx.commands.core;

import com.qualcomm.hardware.lynx.LynxModuleIntf;
import com.qualcomm.hardware.lynx.commands.LynxInterface;
import com.qualcomm.hardware.lynx.commands.LynxInterfaceCommand;
import com.qualcomm.hardware.lynx.commands.LynxMessage;

/**
 * Created by bob on 2016-03-06.
 */
public abstract class LynxDekaInterfaceCommand<RESPONSE extends LynxMessage> extends LynxInterfaceCommand<RESPONSE>
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String theInterfaceName = "DEKA";

    public static LynxInterface theInterface
            = new LynxInterface(theInterfaceName,
                LynxGetBulkInputDataCommand.class,      // 0
                LynxSetSingleDIOOutputCommand.class,    // 1
                LynxSetAllDIOOutputsCommand.class,      // 2
                LynxSetDIODirectionCommand.class,       // 3
                LynxGetDIODirectionCommand.class,       // 4
                LynxGetSingleDIOInputCommand.class,     // 5
                LynxGetAllDIOInputsCommand.class,       // 6
                LynxGetADCCommand.class,                // 7
                LynxSetMotorChannelModeCommand.class,   // 8
                LynxGetMotorChannelModeCommand.class,   // 9
                LynxSetMotorChannelEnableCommand.class, // 10
                LynxGetMotorChannelEnableCommand.class, // 11
                LynxSetMotorChannelCurrentAlertLevelCommand.class,      // 12
                LynxGetMotorChannelCurrentAlertLevelCommand.class,      // 13
                LynxResetMotorEncoderCommand.class,                     // 14
                LynxSetMotorConstantPowerCommand.class,                 // 15
                LynxGetMotorConstantPowerCommand.class,                 // 16
                LynxSetMotorTargetVelocityCommand.class,                // 17
                LynxGetMotorTargetVelocityCommand.class,                // 18
                LynxSetMotorTargetPositionCommand.class,                // 19
                LynxGetMotorTargetPositionCommand.class,                // 20
                LynxIsMotorAtTargetCommand.class,                       // 21
                LynxGetMotorEncoderPositionCommand.class,               // 22
                LynxSetMotorPIDControlLoopCoefficientsCommand.class,    // 23 SetMotorPIDControlLoopCoefficients
                LynxGetMotorPIDControlLoopCoefficientsCommand.class,    // 24 GetMotorPIDControlLoopCoefficients
                LynxSetPWMConfigurationCommand.class,                   // 25 SetPWMConfiguration
                LynxGetPWMConfigurationCommand.class,                   // 26 GetPWMConfiguration
                LynxSetPWMPulseWidthCommand.class,                      // 27 SetPWMPulseWidth
                LynxGetPWMPulseWidthCommand.class,                      // 28 GetPWMPulseWidth
                LynxSetPWMEnableCommand.class,                          // 29 SetPWMEnable
                LynxGetPWMEnableCommand.class,                          // 30 GetPWMEnable
                LynxSetServoConfigurationCommand.class,                 // 31 SetServoConfiguration
                LynxGetServoConfigurationCommand.class,                 // 32 GetServoConfiguration
                LynxSetServoPulseWidthCommand.class,                    // 33 SetServoPulseWidth
                LynxGetServoPulseWidthCommand.class,                    // 34 GetServoPulseWidth
                LynxSetServoEnableCommand.class,                        // 35 SetServoEnable
                LynxGetServoEnableCommand.class,                        // 36 GetServoEnable
                LynxI2cWriteSingleByteCommand.class,                    // 37 I2cWriteSingleByte
                LynxI2cWriteMultipleBytesCommand.class,                 // 38 I2cWriteMultipleBytes
                LynxI2cReadSingleByteCommand.class,                     // 39 I2cReadSingleByte
                LynxI2cReadMultipleBytesCommand.class,                  // 40 I2cReadMultipleBytes
                LynxI2cReadStatusQueryCommand.class,                    // 41 I2cReadStatusQuery
                LynxI2cWriteStatusQueryCommand.class,                   // 42 I2cWriteStatusQuery
                LynxI2cConfigureChannelCommand.class,                   // 43 I2cConfigureChannel
                LynxPhoneChargeControlCommand.class,                    // 44 PhoneChargeControl
                LynxPhoneChargeQueryCommand.class,                      // 45 PhoneChargeQuery
                LynxInjectDataLogHintCommand.class,                     // 46 InjectDataLogHint
                LynxI2cConfigureQueryCommand.class,                     // 47 I2cConfigureQuery
                LynxReadVersionStringCommand.class,                     // 48 ReadVersionString
                LynxFtdiResetControlCommand.class,                      // 49 FtdiResetControl
                LynxFtdiResetQueryCommand.class,                        // 50 FtdiResetQuery
                LynxSetMotorPIDFControlLoopCoefficientsCommand.class,   // 51 SetMotorPIDFControlLoopCoefficients
                LynxI2cWriteReadMultipleBytesCommand.class,             // 52 I2cWriteReadMultipleBytes
                LynxGetMotorPIDFControlLoopCoefficientsCommand.class    // 53 GetMotorPIDFControlLoopCoefficients
            );

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public LynxDekaInterfaceCommand(LynxModuleIntf module)
        {
        super(module);
        }

    //----------------------------------------------------------------------------------------------
    // Accessing
    //----------------------------------------------------------------------------------------------

    @Override
    public LynxInterface getInterface()
        {
        return theInterface;
        }
    }
