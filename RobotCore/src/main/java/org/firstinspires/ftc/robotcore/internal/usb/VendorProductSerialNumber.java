/*
Copyright (c) 2018 Robert Atkinson

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
package org.firstinspires.ftc.robotcore.internal.usb;

import android.text.TextUtils;

import com.qualcomm.robotcore.util.SerialNumber;

import org.firstinspires.ftc.robotcore.internal.system.Misc;

/**
 * A {@link VendorProductSerialNumber} is a made-up USB serial number derived from vendor and
 * product identifiers together with the USB connection path (concatenation of USB port numbers
 * through possibly various USB hubs).
 *
 * The general rule we have for devices with this kind of serial number is that if there's only
 * one of them attached with a given (vid,pid) pair, then we allow that to move around from USB
 * port to USB port, but if there's more than one with the same (vid,pid) identification then
 * we require that all of them only be used on the USB phyical ports / connection paths on which
 * they were originally configured / detected.
 *
 * @see SerialNumber#equals
 * @see SerialNumber#matches
 */
@SuppressWarnings("WeakerAccess")
public class VendorProductSerialNumber extends SerialNumber
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected final int vendorId;
    protected final int productId;
    protected final String connectionPath;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public VendorProductSerialNumber(String initializer)
        {
        super(initializer.trim());
        String[] parts = serialNumberString.split("\\|");
        vendorId = Integer.decode(parts[0].split("=")[1]);
        productId = Integer.decode(parts[1].split("=")[1]);
        String[] connectionParts = parts[2].split("=");
        connectionPath = connectionParts.length > 1 ? connectionParts[1] : "";
        }

    public VendorProductSerialNumber(int vid, int pid, String connectionPath)
        {
        this(Misc.formatInvariant("%svendor=0x%04x|product=0x%04x|connection=%s", vendorProductPrefix, vid, pid, connectionPath==null?"":connectionPath));
        }

    //----------------------------------------------------------------------------------------------
    // Comparision
    //----------------------------------------------------------------------------------------------

    @Override public boolean matches(Object oPattern)
        {
        if (oPattern instanceof VendorProductSerialNumber)
            {
            VendorProductSerialNumber pattern = (VendorProductSerialNumber)oPattern;
            return vendorId==pattern.vendorId
                    && productId==pattern.productId
                    && (TextUtils.isEmpty(pattern.connectionPath) || connectionPath.equals(pattern.connectionPath));
            }
        return false;
        }

    //----------------------------------------------------------------------------------------------
    // Accessing
    //----------------------------------------------------------------------------------------------

    @Override public String toString()
        {
        // We avoid hex on the idea decimal is more friendly for humans
        return Misc.formatForUser("%d:%d%s%s", vendorId, productId, TextUtils.isEmpty(connectionPath)?"":":", connectionPath);
        }

    @Override public boolean isVendorProduct()
        {
        return true;
        }

    public int getVendorId()
        {
        return vendorId;
        }

    public int getProductId()
        {
        return productId;
        }

    public String getConnectionPath()
        {
        return connectionPath;
        }
    }
