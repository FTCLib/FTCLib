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
package org.firstinspires.ftc.robotcore.internal.ftdi;

import com.qualcomm.robotcore.util.TypeConversion;

import org.firstinspires.ftc.robotcore.internal.system.Assert;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by bob on 3/27/2017.
 */
@SuppressWarnings("WeakerAccess")
public class UsbDeviceDescriptor extends UsbDescriptorHeader
    {
    public int bcdUSB;
    public int bDeviceClass;
    public int bDeviceSubClass;
    public int bDeviceProtocol;

    public int bMaxPacketSize0;
    public int idVendor;
    public int idProduct;
    public int bcdDevice;

    public int iManufacturer;
    public int iProduct;
    public int iSerialNumber;
    public int bNumConfigurations;

    public UsbDeviceDescriptor(byte[] bytes)
        {
        this(ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN));
        }
    
    public UsbDeviceDescriptor(ByteBuffer descriptor)
        {
        super(descriptor);                                                          // 0,1
        Assert.assertTrue(bLength >= 18);

        bcdUSB          = TypeConversion.unsignedShortToInt(descriptor.getShort()); // 2,3
        bDeviceClass    = TypeConversion.unsignedByteToInt(descriptor.get());       // 4
        bDeviceSubClass = TypeConversion.unsignedByteToInt(descriptor.get());       // 5
        bDeviceProtocol = TypeConversion.unsignedByteToInt(descriptor.get());       // 6

        bMaxPacketSize0 = TypeConversion.unsignedByteToInt(descriptor.get());       // 7
        idVendor        = TypeConversion.unsignedShortToInt(descriptor.getShort()); // 8,9
        idProduct       = TypeConversion.unsignedShortToInt(descriptor.getShort()); // 10,11
        bcdDevice       = TypeConversion.unsignedShortToInt(descriptor.getShort()); // 12,13

        iManufacturer       = TypeConversion.unsignedByteToInt(descriptor.get());   // 14
        iProduct            = TypeConversion.unsignedByteToInt(descriptor.get());   // 15
        iSerialNumber       = TypeConversion.unsignedByteToInt(descriptor.get());   // 16
        bNumConfigurations  = TypeConversion.unsignedByteToInt(descriptor.get());   // 17
        }
    }
