/*
Copyright (c) 2017 Robert Atkinson

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

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.qualcomm.robotcore.util.SerialNumber;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

/**
 * A simple utility class holding the serial number of a USB accessible lynx module and (optionally) its module address
 */
@SuppressWarnings("WeakerAccess")
public final class USBAccessibleLynxModule
    {
    protected SerialNumber serialNumber = null;
    protected int          moduleAddress = 0;
    protected boolean      moduleAddressChangeable = true;
    protected String       firmwareVersionString = "";

    public USBAccessibleLynxModule(SerialNumber serialNumber)
        {
        this.setSerialNumber(serialNumber);
        }

    public USBAccessibleLynxModule(SerialNumber serialNumber, boolean moduleAddressChangeable)
        {
        this.setSerialNumber(serialNumber);
        this.setModuleAddressChangeable(moduleAddressChangeable);
        }

    public SerialNumber getSerialNumber()
        {
        return serialNumber;
        }

    public void setSerialNumber(SerialNumber serialNumber)
        {
        this.serialNumber = serialNumber;
        }

    public int getModuleAddress()
        {
        return moduleAddress;
        }

    public void setModuleAddress(int moduleAddress)
        {
        this.moduleAddress = moduleAddress;
        }

    public boolean isModuleAddressChangeable()
        {
        return moduleAddressChangeable;
        }

    public void setModuleAddressChangeable(boolean moduleAddressChangeable)
        {
        this.moduleAddressChangeable = moduleAddressChangeable;
        }

    public String getFirmwareVersionString()
        {
        return firmwareVersionString;
        }

    public String getFinishedFirmwareVersionString()
        {
        String result = getFirmwareVersionString();
        if (TextUtils.isEmpty(result))
            {
            result = "(" + AppUtil.getDefContext().getString(R.string.lynxUnavailableFWVersionString) + ")";
            }
        return result;
        }

    public void setFirmwareVersionString(@NonNull String firmwareVersionString)
        {
        this.firmwareVersionString = firmwareVersionString;
        }

    }
