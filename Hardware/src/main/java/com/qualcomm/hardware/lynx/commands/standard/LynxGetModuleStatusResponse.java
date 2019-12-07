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
package com.qualcomm.hardware.lynx.commands.standard;

import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.hardware.lynx.commands.LynxResponse;
import com.qualcomm.robotcore.hardware.configuration.LynxConstants;
import com.qualcomm.robotcore.util.TypeConversion;

/**
 * Created by bob on 2016-03-06.
 */
@SuppressWarnings("WeakerAccess")
public class LynxGetModuleStatusResponse extends LynxStandardResponse
    {
    //----------------------------------------------------------------------------------------------
    // Constants
    //----------------------------------------------------------------------------------------------

    public static final int bitKeepAliveTimeout   = (1<<0);
    public static final int bitDeviceReset        = (1<<1);
    public static final int bitFailSafe           = (1<<2);
    public static final int bitControllerOverTemp = (1<<3);
    public static final int bitBatteryLow         = (1<<4);
    public static final int bitHIBFault           = (1<<5);

    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    byte status;
    byte motorAlerts;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public LynxGetModuleStatusResponse(LynxModule module)
        {
        super(module);
        }

    @Override public String toString()
        {
        StringBuilder builder = new StringBuilder();
        appendBit(builder, bitKeepAliveTimeout,     "KeepAliveTimeout");
        appendBit(builder, bitDeviceReset,          "Reset");
        appendBit(builder, bitFailSafe,             "FailSafe");
        appendBit(builder, bitControllerOverTemp,   "Temp");
        appendBit(builder, bitBatteryLow,           "Battery");
        appendBit(builder, bitHIBFault,             "HIB Fault");
        String message = builder.toString();
        if (message.length() > 0) message = ": " + message;
        return String.format("LynxGetModuleStatusResponse(status=0x%02x alerts=0x%02x%s)", status, motorAlerts, message);
        }

    protected void appendBit(StringBuilder builder, int bit, String message)
        {
        if (testBitsOn(bit))
            {
            if (builder.length() > 0) builder.append("|");
            builder.append(message);
            }
        }

    //----------------------------------------------------------------------------------------------
    // Accessors
    //----------------------------------------------------------------------------------------------

    public boolean isKeepAliveTimeout()
        {
        return testBitsOn(bitKeepAliveTimeout);
        }
    public boolean isDeviceReset()
        {
        return testBitsOn(bitDeviceReset);
        }
    public boolean isFailSafe()
        {
        return testBitsOn(bitFailSafe);
        }
    public boolean isControllerOverTemp()
        {
        return testBitsOn(bitControllerOverTemp);
        }
    public boolean isBatteryLow()
        {
        return testBitsOn(bitBatteryLow);
        }
    public boolean isHIBFault()
        {
        return testBitsOn(bitHIBFault);
        }

    public int getStatus()
        {
        return TypeConversion.unsignedByteToInt(this.status);
        }
    public boolean testBitsOn(int bits)
        {
        return (getStatus() & bits) == bits;
        }
    public boolean testAnyBits(int bits)
        {
        return (getStatus() & bits) != 0;
        }

    public int getMotorAlerts()
        {
        return TypeConversion.unsignedByteToInt(this.motorAlerts);
        }
    public boolean hasMotorLostCounts(int motorZ)
        {
        LynxConstants.validateMotorZ(motorZ);
        int bit = (1<<motorZ);
        return (getMotorAlerts() & bit) == bit;
        }
    public boolean isMotorBridgeOverTemp(int motorZ)
        {
        LynxConstants.validateMotorZ(motorZ);
        int bit = (1<<(motorZ+4));
        return (getMotorAlerts() & bit) == bit;
        }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    public static int getStandardCommandNumber()
        {
        return LynxGetModuleStatusCommand.getStandardCommandNumber() | LynxResponse.RESPONSE_BIT;
        }

    @Override
    public int getCommandNumber()
        {
        return getStandardCommandNumber();
        }

    @Override
    public byte[] toPayloadByteArray()
        {
        return new byte[] { this.status, this.motorAlerts };
        }

    @Override
    public void fromPayloadByteArray(byte[] rgb)
        {
        this.status      = rgb[0];
        this.motorAlerts = rgb[1];
        }
    }
