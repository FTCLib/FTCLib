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

import org.firstinspires.ftc.robotcore.internal.ftdi.FtDeviceIOException;
import org.firstinspires.ftc.robotcore.internal.ftdi.FtDevice;
import org.firstinspires.ftc.robotcore.internal.usb.exception.RobotUsbException;

/**
 * Created by bob on 3/18/2017.
 */
@SuppressWarnings("WeakerAccess")
public class FT_EE_2232_Ctrl extends FT_EE_Ctrl
    {
    private static final byte EEPROM_SIZE_LOCATION = 10;
    private static final short CHECKSUM_LOCATION = 63;
    private static final String DEFAULT_PID = "6010";

    public FT_EE_2232_Ctrl(FtDevice usbC) throws FtDeviceIOException, RobotUsbException
        {
        super(usbC);
        this.getEepromSize(EEPROM_SIZE_LOCATION);
        }

    @Override public short programEeprom(FT_EEPROM ee)
        {
        int[] data = new int[this.mEepromSize];
        if (ee.getClass() != FT_EEPROM_2232D.class)
            {
            return 1;
            }
        else
            {
            FT_EEPROM_2232D eeprom = (FT_EEPROM_2232D) ee;

            try
                {
                data[0] = 0;
                if (eeprom.A_FIFO)
                    {
                    data[0] |= 1;
                    }
                else if (eeprom.A_FIFOTarget)
                    {
                    data[0] |= 2;
                    }
                else
                    {
                    data[0] |= 4;
                    }

                if (eeprom.A_HighIO)
                    {
                    data[0] |= 16;
                    }

                if (eeprom.A_LoadVCP)
                    {
                    data[0] |= 8;
                    }
                else if (eeprom.B_FIFO)
                    {
                    data[0] |= 256;
                    }
                else if (eeprom.B_FIFOTarget)
                    {
                    data[0] |= 512;
                    }
                else
                    {
                    data[0] |= 1024;
                    }

                if (eeprom.B_HighIO)
                    {
                    data[0] |= 4096;
                    }

                if (eeprom.B_LoadVCP)
                    {
                    data[0] |= 2048;
                    }

                data[1] = eeprom.VendorId;
                data[2] = eeprom.ProductId;
                data[3] = 1280;
                data[4] = this.setUSBConfig(ee);
                data[4] = this.setDeviceControl(ee);
                boolean e = false;
                byte offset = 75;
                if (this.mEepromType == 70)
                    {
                    offset = 11;
                    e = true;
                    }

                int offset1 = this.setStringDescriptor(eeprom.Manufacturer, data, offset, 7, e);
                offset1 = this.setStringDescriptor(eeprom.Product, data, offset1, 8, e);
                if (eeprom.SerNumEnable)
                    {
                    this.setStringDescriptor(eeprom.SerialNumber, data, offset1, 9, e);
                    }

                data[10] = this.mEepromType;
                if (data[1] != 0 && data[2] != 0)
                    {
                    boolean returnCode = false;
                    returnCode = this.programEeprom(data, this.mEepromSize - 1);
                    return (short) (returnCode ? 0 : 1);
                    }
                else
                    {
                    return 2;
                    }
                }
            catch (Exception var7)
                {
                var7.printStackTrace();
                return 0;
                }
            }
        }

    @Override public FT_EEPROM readEeprom()
        {
        FT_EEPROM_2232D eeprom = new FT_EEPROM_2232D();
        int[] dataRead = new int[this.mEepromSize];

        try
            {
            for (int e = 0; e < this.mEepromSize; ++e)
                {
                dataRead[e] = this.readWord((short) e);
                }

            short var11 = (short) (dataRead[0] & 7);
            switch (var11)
                {
                case 0:
                    eeprom.A_UART = true;
                    break;
                case 1:
                    eeprom.A_FIFO = true;
                    break;
                case 2:
                    eeprom.A_FIFOTarget = true;
                case 3:
                default:
                    break;
                case 4:
                    eeprom.A_FastSerial = true;
                }

            short data3x00 = (short) ((dataRead[0] & 8) >> 3);
            if (data3x00 == 1)
                {
                eeprom.A_LoadVCP = true;
                }
            else
                {
                eeprom.A_HighIO = true;
                }

            short data4x00 = (short) ((dataRead[0] & 16) >> 4);
            if (data4x00 == 1)
                {
                eeprom.A_HighIO = true;
                }

            short data810x00 = (short) ((dataRead[0] & 1792) >> 8);
            switch (data810x00)
                {
                case 0:
                    eeprom.B_UART = true;
                    break;
                case 1:
                    eeprom.B_FIFO = true;
                    break;
                case 2:
                    eeprom.B_FIFOTarget = true;
                case 3:
                default:
                    break;
                case 4:
                    eeprom.B_FastSerial = true;
                }

            short data11x00 = (short) ((dataRead[0] & 2048) >> 11);
            if (data11x00 == 1)
                {
                eeprom.B_LoadVCP = true;
                }
            else
                {
                eeprom.B_LoadD2XX = true;
                }

            short data12x00 = (short) ((dataRead[0] & 4096) >> 12);
            if (data12x00 == 1)
                {
                eeprom.B_HighIO = true;
                }

            eeprom.VendorId = (short) dataRead[1];
            eeprom.ProductId = (short) dataRead[2];
            this.getUSBConfig(eeprom, dataRead[4]);
            int addr = dataRead[7] & 255;
            if (this.mEepromType == 70)
                {
                addr -= 128;
                addr /= 2;
                eeprom.Manufacturer = this.getStringDescriptor(addr, dataRead);
                addr = dataRead[8] & 255;
                addr -= 128;
                addr /= 2;
                eeprom.Product = this.getStringDescriptor(addr, dataRead);
                addr = dataRead[9] & 255;
                addr -= 128;
                addr /= 2;
                eeprom.SerialNumber = this.getStringDescriptor(addr, dataRead);
                }
            else
                {
                addr /= 2;
                eeprom.Manufacturer = this.getStringDescriptor(addr, dataRead);
                addr = dataRead[8] & 255;
                addr /= 2;
                eeprom.Product = this.getStringDescriptor(addr, dataRead);
                addr = dataRead[9] & 255;
                addr /= 2;
                eeprom.SerialNumber = this.getStringDescriptor(addr, dataRead);
                }

            return eeprom;
            }
        catch (Exception var10)
            {
            return null;
            }
        }

    @Override public int getUserSize() throws RobotUsbException
        {
        int data = this.readWord((short) 9);
        int ptr = data & 255;
        int length = (data & '\uff00') >> 8;
        ptr += length / 2;
        return (this.mEepromSize - 1 - 1 - ptr) * 2;
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
            int[] eeprom = new int[this.mEepromSize];

            for (short returnCode = 0; returnCode < this.mEepromSize; ++returnCode)
                {
                eeprom[returnCode] = this.readWord(returnCode);
                }

            short var7 = (short) (this.mEepromSize - this.getUserSize() / 2 - 1 - 1);

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
                var9 = this.programEeprom(eeprom, this.mEepromSize - 1);
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
            short offset = (short) (this.mEepromSize - this.getUserSize() / 2 - 1 - 1);

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
