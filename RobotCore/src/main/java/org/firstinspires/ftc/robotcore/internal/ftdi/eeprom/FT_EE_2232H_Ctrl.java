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
public class FT_EE_2232H_Ctrl extends FT_EE_Ctrl
    {
    private static final byte EEPROM_SIZE_LOCATION = 12;
    private static final String DEFAULT_PID = "6010";
    private static final int AL_DRIVE_CURRENT = 3;
    private static final int AH_DRIVE_CURRENT = 48;
    private static final int BL_DRIVE_CURRENT = 768;
    private static final int BH_DRIVE_CURRENT = 12288;
    private static final int AL_FAST_SLEW = 4;
    private static final int AL_SCHMITT_INPUT = 8;
    private static final int AH_FAST_SLEW = 64;
    private static final int AH_SCHMITT_INPUT = 128;
    private static final int BL_FAST_SLEW = 1024;
    private static final int BL_SCHMITT_INPUT = 2048;
    private static final int BH_FAST_SLEW = 16384;
    private static final int BH_SCHMITT_INPUT = 32768;
    private static final int TPRDRV = 24;
    private static final int A_UART_RS232 = 0;
    private static final int A_245_FIFO = 1;
    private static final int A_245_FIFO_TARGET = 2;
    private static final int A_FAST_SERIAL = 4;
    private static final int A_LOAD_VCP_DRIVER = 8;
    private static final int INVERT_TXD = 256;
    private static final int INVERT_RXD = 512;
    private static final int INVERT_RTS = 1024;
    private static final int INVERT_CTS = 2048;
    private static final int INVERT_DTR = 4096;
    private static final int INVERT_DSR = 8192;
    private static final int INVERT_DCD = 16384;
    private static final int INVERT_RI = 32768;

    public FT_EE_2232H_Ctrl(FtDevice usbC) throws FtDeviceIOException, RobotUsbException
        {
        super(usbC);
        this.getEepromSize(EEPROM_SIZE_LOCATION);
        }

    @Override public short programEeprom(FT_EEPROM ee)
        {
        int[] dataToWrite = new int[this.mEepromSize];
        if (ee.getClass() != FT_EEPROM_2232H.class)
            {
            return 1;
            }
        else
            {
            FT_EEPROM_2232H eeprom = (FT_EEPROM_2232H) ee;

            try
                {
                if (!eeprom.A_UART)
                    {
                    if (eeprom.A_FIFO)
                        {
                        dataToWrite[0] |= 1;
                        }
                    else if (eeprom.A_FIFOTarget)
                        {
                        dataToWrite[0] |= 2;
                        }
                    else
                        {
                        dataToWrite[0] |= 4;
                        }
                    }

                if (eeprom.A_LoadVCP)
                    {
                    dataToWrite[0] |= 8;
                    }

                if (!eeprom.B_UART)
                    {
                    if (eeprom.B_FIFO)
                        {
                        dataToWrite[0] |= 256;
                        }
                    else if (eeprom.B_FIFOTarget)
                        {
                        dataToWrite[0] |= 512;
                        }
                    else
                        {
                        dataToWrite[0] |= 1024;
                        }
                    }

                if (eeprom.B_LoadVCP)
                    {
                    dataToWrite[0] |= 2048;
                    }

                if (eeprom.PowerSaveEnable)
                    {
                    dataToWrite[0] |= '耀';
                    }

                dataToWrite[1] = eeprom.VendorId;
                dataToWrite[2] = eeprom.ProductId;
                dataToWrite[3] = 1792;
                dataToWrite[4] = this.setUSBConfig(ee);
                dataToWrite[5] = this.setDeviceControl(ee);
                dataToWrite[6] = 0;
                byte e = eeprom.AL_DriveCurrent;
                if (e == -1)
                    {
                    e = 0;
                    }

                dataToWrite[6] |= e;
                if (eeprom.AL_SlowSlew)
                    {
                    dataToWrite[6] |= 4;
                    }

                if (eeprom.AL_SchmittInput)
                    {
                    dataToWrite[6] |= 8;
                    }

                byte driveB = eeprom.AH_DriveCurrent;
                if (driveB == -1)
                    {
                    driveB = 0;
                    }

                short driveB1 = (short) (driveB << 4);
                dataToWrite[6] |= driveB1;
                if (eeprom.AH_SlowSlew)
                    {
                    dataToWrite[6] |= 64;
                    }

                if (eeprom.AH_SchmittInput)
                    {
                    dataToWrite[6] |= 128;
                    }

                byte driveC = eeprom.BL_DriveCurrent;
                if (driveC == -1)
                    {
                    driveC = 0;
                    }

                short driveC1 = (short) (driveC << 8);
                dataToWrite[6] |= driveC1;
                if (eeprom.BL_SlowSlew)
                    {
                    dataToWrite[6] |= 1024;
                    }

                if (eeprom.BL_SchmittInput)
                    {
                    dataToWrite[6] |= 2048;
                    }

                byte driveD = eeprom.BH_DriveCurrent;
                short driveD1 = (short) (driveD << 12);
                dataToWrite[6] |= driveD1;
                if (eeprom.BH_SlowSlew)
                    {
                    dataToWrite[6] |= 16384;
                    }

                if (eeprom.BH_SchmittInput)
                    {
                    dataToWrite[6] |= '耀';
                    }

                boolean eeprom46 = false;
                byte offset = 77;
                if (this.mEepromType == 70)
                    {
                    offset = 13;
                    eeprom46 = true;
                    }

                int offset1 = this.setStringDescriptor(eeprom.Manufacturer, dataToWrite, offset, 7, eeprom46);
                offset1 = this.setStringDescriptor(eeprom.Product, dataToWrite, offset1, 8, eeprom46);
                if (eeprom.SerNumEnable)
                    {
                    this.setStringDescriptor(eeprom.SerialNumber, dataToWrite, offset1, 9, eeprom46);
                    }

                switch (eeprom.TPRDRV)
                    {
                    case 0:
                        dataToWrite[11] = 0;
                        break;
                    case 1:
                        dataToWrite[11] = 8;
                        break;
                    case 2:
                        dataToWrite[11] = 16;
                        break;
                    case 3:
                        dataToWrite[11] = 24;
                        break;
                    default:
                        dataToWrite[11] = 0;
                    }

                dataToWrite[12] = this.mEepromType;
                if (dataToWrite[1] != 0 && dataToWrite[2] != 0)
                    {
                    boolean returnCode = false;
                    returnCode = this.programEeprom(dataToWrite, this.mEepromSize - 1);
                    return (short) (returnCode ? 0 : 1);
                    }
                else
                    {
                    return 2;
                    }
                }
            catch (Exception var11)
                {
                var11.printStackTrace();
                return 0;
                }
            }
        }

    @Override public FT_EEPROM readEeprom()
        {
        FT_EEPROM_2232H eeprom = new FT_EEPROM_2232H();
        int[] data = new int[this.mEepromSize];
        if (this.mEepromBlank)
            {
            return eeprom;
            }
        else
            {
            try
                {
                short e;
                for (e = 0; e < this.mEepromSize; ++e)
                    {
                    data[e] = this.readWord(e);
                    }

                boolean wordx00 = false;
                int var24 = data[0];
                e = (short) (var24 & 7);
                switch (e)
                    {
                    case 0:
                        eeprom.A_UART = true;
                        break;
                    case 1:
                        eeprom.A_FIFO = true;
                        break;
                    case 2:
                        eeprom.A_FIFOTarget = true;
                        break;
                    case 3:
                    default:
                        eeprom.A_UART = true;
                        break;
                    case 4:
                        eeprom.A_FastSerial = true;
                    }

                short data3x00 = (short) ((var24 & 8) >> 3);
                if (data3x00 == 1)
                    {
                    eeprom.A_LoadVCP = true;
                    eeprom.A_LoadD2XX = false;
                    }
                else
                    {
                    eeprom.A_LoadVCP = false;
                    eeprom.A_LoadD2XX = true;
                    }

                short data810x00 = (short) ((var24 & 1792) >> 8);
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
                        break;
                    case 3:
                    default:
                        eeprom.B_UART = true;
                        break;
                    case 4:
                        eeprom.B_FastSerial = true;
                    }

                short data11x00 = (short) ((var24 & 2048) >> 11);
                if (data11x00 == 1)
                    {
                    eeprom.B_LoadVCP = true;
                    eeprom.B_LoadD2XX = false;
                    }
                else
                    {
                    eeprom.B_LoadVCP = false;
                    eeprom.B_LoadD2XX = true;
                    }

                short data15x00 = (short) ((var24 & '耀') >> 15);
                if (data15x00 == 1)
                    {
                    eeprom.PowerSaveEnable = true;
                    }
                else
                    {
                    eeprom.PowerSaveEnable = false;
                    }

                eeprom.VendorId = (short) data[1];
                eeprom.ProductId = (short) data[2];
                this.getUSBConfig(eeprom, data[4]);
                this.getDeviceControl(eeprom, data[5]);
                short data01x06 = (short) (data[6] & 3);
                switch (data01x06)
                    {
                    case 0:
                        eeprom.AL_DriveCurrent = 0;
                        break;
                    case 1:
                        eeprom.AL_DriveCurrent = 1;
                        break;
                    case 2:
                        eeprom.AL_DriveCurrent = 2;
                        break;
                    case 3:
                        eeprom.AL_DriveCurrent = 3;
                    }

                short data2x06 = (short) (data[6] & 4);
                if (data2x06 == 4)
                    {
                    eeprom.AL_SlowSlew = true;
                    }
                else
                    {
                    eeprom.AL_SlowSlew = false;
                    }

                short data3x06 = (short) (data[6] & 8);
                if (data3x06 == 8)
                    {
                    eeprom.AL_SchmittInput = true;
                    }
                else
                    {
                    eeprom.AL_SchmittInput = false;
                    }

                short data45x06 = (short) ((data[6] & 48) >> 4);
                switch (data45x06)
                    {
                    case 0:
                        eeprom.AH_DriveCurrent = 0;
                        break;
                    case 1:
                        eeprom.AH_DriveCurrent = 1;
                        break;
                    case 2:
                        eeprom.AH_DriveCurrent = 2;
                        break;
                    case 3:
                        eeprom.AH_DriveCurrent = 3;
                    }

                short data6x06 = (short) (data[6] & 64);
                if (data6x06 == 64)
                    {
                    eeprom.AH_SlowSlew = true;
                    }
                else
                    {
                    eeprom.AH_SlowSlew = false;
                    }

                short data7x06 = (short) (data[6] & 128);
                if (data7x06 == 128)
                    {
                    eeprom.AH_SchmittInput = true;
                    }
                else
                    {
                    eeprom.AH_SchmittInput = false;
                    }

                short data89X06 = (short) ((data[6] & 768) >> 8);
                switch (data89X06)
                    {
                    case 0:
                        eeprom.BL_DriveCurrent = 0;
                        break;
                    case 1:
                        eeprom.BL_DriveCurrent = 1;
                        break;
                    case 2:
                        eeprom.BL_DriveCurrent = 2;
                        break;
                    case 3:
                        eeprom.BL_DriveCurrent = 3;
                    }

                short data10x06 = (short) (data[6] & 1024);
                if (data10x06 == 1024)
                    {
                    eeprom.BL_SlowSlew = true;
                    }
                else
                    {
                    eeprom.BL_SlowSlew = false;
                    }

                short data11x06 = (short) (data[6] & 2048);
                if (data7x06 == 2048)
                    {
                    eeprom.BL_SchmittInput = true;
                    }
                else
                    {
                    eeprom.BL_SchmittInput = false;
                    }

                short data1213X06 = (short) ((data[6] & 12288) >> 12);
                switch (data1213X06)
                    {
                    case 0:
                        eeprom.BH_DriveCurrent = 0;
                        break;
                    case 1:
                        eeprom.BH_DriveCurrent = 1;
                        break;
                    case 2:
                        eeprom.BH_DriveCurrent = 2;
                        break;
                    case 3:
                        eeprom.BH_DriveCurrent = 3;
                    }

                short data14x06 = (short) (data[6] & 16384);
                if (data14x06 == 16384)
                    {
                    eeprom.BH_SlowSlew = true;
                    }
                else
                    {
                    eeprom.BH_SlowSlew = false;
                    }

                short data15x06 = (short) (data[6] & '耀');
                if (data15x06 == '耀')
                    {
                    eeprom.BH_SchmittInput = true;
                    }
                else
                    {
                    eeprom.BH_SchmittInput = false;
                    }

                short datax0B = (short) ((data[11] & 24) >> 3);
                if (datax0B < 4)
                    {
                    eeprom.TPRDRV = datax0B;
                    }
                else
                    {
                    eeprom.TPRDRV = 0;
                    }

                int addr = data[7] & 255;
                if (this.mEepromType == 70)
                    {
                    addr -= 128;
                    addr /= 2;
                    eeprom.Manufacturer = this.getStringDescriptor(addr, data);
                    addr = data[8] & 255;
                    addr -= 128;
                    addr /= 2;
                    eeprom.Product = this.getStringDescriptor(addr, data);
                    addr = data[9] & 255;
                    addr -= 128;
                    addr /= 2;
                    eeprom.SerialNumber = this.getStringDescriptor(addr, data);
                    }
                else
                    {
                    addr /= 2;
                    eeprom.Manufacturer = this.getStringDescriptor(addr, data);
                    addr = data[8] & 255;
                    addr /= 2;
                    eeprom.Product = this.getStringDescriptor(addr, data);
                    addr = data[9] & 255;
                    addr /= 2;
                    eeprom.SerialNumber = this.getStringDescriptor(addr, data);
                    }

                return eeprom;
                }
            catch (Exception var23)
                {
                return null;
                }
            }
        }

    @Override public int getUserSize() throws RobotUsbException
        {
        int data = this.readWord((short) 9);
        int ptr = data & 255;
        ptr /= 2;
        int length = (data & '\uff00') >> 8;
        ptr += length / 2;
        ++ptr;
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
