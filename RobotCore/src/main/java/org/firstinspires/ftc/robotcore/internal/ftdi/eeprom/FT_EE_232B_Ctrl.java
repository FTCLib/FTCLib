/*
Copyright (c) 2017 Robert Atkinson

All rights reserved.

Derived in part from information in various resources, including FTDI, the
Android Linux implementation, FreeBsc, UsbSerial, and others.

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
package org.firstinspires.ftc.robotcore.internal.ftdi.eeprom;

import org.firstinspires.ftc.robotcore.internal.ftdi.FtDevice;
import org.firstinspires.ftc.robotcore.internal.usb.exception.RobotUsbException;

/**
 * Created by bob on 3/18/2017.
 */
@SuppressWarnings("WeakerAccess")
public class FT_EE_232B_Ctrl extends FT_EE_Ctrl
    {
    private static final short CHECKSUM_LOCATION = 63;
    private static final short EEPROM_SIZE = 64;
    private static FtDevice ft_device;

    public FT_EE_232B_Ctrl(FtDevice usbC)
        {
        super(usbC);
        ft_device = usbC;
        }

    @Override public short programEeprom(FT_EEPROM ee)
        {
        int[] data = new int[64];
        if (ee.getClass() != FT_EEPROM.class)
            {
            return 1;
            }
        else
            {
            FT_EEPROM eeprom = ee;

            try
                {
                for (short e = 0; e < 64; ++e)
                    {
                    data[e] = this.readWord(e);
                    }

                data[1] = eeprom.VendorId;
                data[2] = eeprom.ProductId;
                data[3] = ft_device.getDeviceInfo().bcdDevice;
                data[4] = this.setUSBConfig(ee);
                byte var7 = 10;
                int var8 = this.setStringDescriptor(eeprom.Manufacturer, data, var7, 7, true);
                var8 = this.setStringDescriptor(eeprom.Product, data, var8, 8, true);
                if (eeprom.SerNumEnable)
                    {
                    this.setStringDescriptor(eeprom.SerialNumber, data, var8, 9, true);
                    }

                if (data[1] != 0 && data[2] != 0)
                    {
                    boolean returnCode = false;
                    returnCode = this.programEeprom(data, 63);
                    return (short) (returnCode ? 0 : 1);
                    }
                else
                    {
                    return 2;
                    }
                }
            catch (Exception var6)
                {
                var6.printStackTrace();
                return 0;
                }
            }
        }

    @Override public FT_EEPROM readEeprom()
        {
        FT_EEPROM eeprom = new FT_EEPROM();
        int[] data = new int[64];

        try
            {
            int e;
            for (e = 0; e < 64; ++e)
                {
                data[e] = this.readWord((short) e);
                }

            eeprom.VendorId = (short) data[1];
            eeprom.ProductId = (short) data[2];
            this.getUSBConfig(eeprom, data[4]);
            byte var5 = 10;
            eeprom.Manufacturer = this.getStringDescriptor(var5, data);
            e = var5 + eeprom.Manufacturer.length() + 1;
            eeprom.Product = this.getStringDescriptor(e, data);
            e += eeprom.Product.length() + 1;
            eeprom.SerialNumber = this.getStringDescriptor(e, data);
            return eeprom;
            }
        catch (Exception var4)
            {
            return null;
            }
        }

    @Override public int getUserSize() throws RobotUsbException
        {
        int data = this.readWord((short) 7);
        int ptr07 = (data & '\uff00') >> 8;
        ptr07 /= 2;
        data = this.readWord((short) 8);
        int ptr08 = (data & '\uff00') >> 8;
        ptr08 /= 2;
        int ptr = 10 + ptr07 + ptr08 + 1;
        data = this.readWord((short) 9);
        int length = (data & '\uff00') >> 8;
        length /= 2;
        return (63 - ptr - 1 - length) * 2;
        }

    @Override public int writeUserData(byte[] data) throws RobotUsbException
        {
        boolean dataWrite = false;
        boolean offset = false;
        if (data.length > this.getUserSize())
            {
            return 0;
            }
        else
            {
            int[] eeprom = new int[64];

            for (short returnCode = 0; returnCode < 64; ++returnCode)
                {
                eeprom[returnCode] = this.readWord(returnCode);
                }

            short var7 = (short) (63 - this.getUserSize() / 2 - 1);
            var7 = (short) (var7 & '\uffff');

            for (int var8 = 0; var8 < data.length; var8 += 2)
                {
                int var6;
                if (var8 + 1 < data.length)
                    {
                    var6 = data[var8 + 1] & 255;
                    }
                else
                    {
                    var6 = 0;
                    }

                var6 <<= 8;
                var6 |= data[var8] & 255;
                eeprom[var7++] = var6;
                }

            if (eeprom[1] != 0 && eeprom[2] != 0)
                {
                boolean var9 = false;
                var9 = this.programEeprom(eeprom, 63);
                if (!var9)
                    {
                    return 0;
                    }
                else
                    {
                    return data.length;
                    }
                }
            else
                {
                return 0;
                }
            }
        }

    @Override public byte[] readUserData(int length) throws RobotUsbException
        {
        boolean Hi = false;
        boolean Lo = false;
        boolean dataRead = false;
        byte[] data = new byte[length];
        if (length != 0 && length <= this.getUserSize())
            {
            short offset = (short) (63 - this.getUserSize() / 2 - 1);
            offset = (short) (offset & '\uffff');

            for (int i = 0; i < length; i += 2)
                {
                int var10 = this.readWord(offset++);
                if (i + 1 < data.length)
                    {
                    byte var8 = (byte) (var10 & 255);
                    data[i + 1] = var8;
                    }
                else
                    {
                    Lo = false;
                    }

                byte var9 = (byte) ((var10 & '\uff00') >> 8);
                data[i] = var9;
                }

            return data;
            }
        else
            {
            return null;
            }
        }
    }
