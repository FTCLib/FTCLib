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
public class FT_EE_X_Ctrl extends FT_EE_Ctrl
    {
    private static final String DEFAULT_PID = "6015";
    private static final byte UART = 0;
    private static final byte FIFO = 1;
    private static final byte FT1248 = 2;
    private static final byte I2C = 3;
    private static final int DEVICE_TYPE_EE_LOC = 73;
    private static final int BCD_ENABLE = 1;
    private static final int FORCE_POWER_ENABLE = 2;
    private static final int DEACTIVATE_SLEEP = 4;
    private static final int RS485_ECHO = 8;
    private static final int VBUS_SUSPEND = 64;
    private static final int LOAD_DRIVER = 128;
    private static final int FT1248_CLK_POLARITY = 16;
    private static final int FT1248_BIT_ORDER = 32;
    private static final int FT1248_FLOW_CTRL = 64;
    private static final int I2C_DISABLE_SCHMITT = 128;
    private static final int INVERT_TXD = 256;
    private static final int INVERT_RXD = 512;
    private static final int INVERT_RTS = 1024;
    private static final int INVERT_CTS = 2048;
    private static final int INVERT_DTR = 4096;
    private static final int INVERT_DSR = 8192;
    private static final int INVERT_DCD = 16384;
    private static final int INVERT_RI = 32768;
    private static final int DBUS_DRIVE = 3;
    private static final int CBUS_DRIVE = 48;
    private static final int DBUS_SLEW = 4;
    private static final int DBUS_SCHMITT = 8;
    private static final int CBUS_SLEW = 64;
    private static final int CBUS_SCHMITT = 128;
    private static FtDevice ft_device;
    private static final short EE_MAX_SIZE = 1024;

    public FT_EE_X_Ctrl(FtDevice usbC)
        {
        super(usbC);
        ft_device = usbC;
        this.mEepromSize = 128;
        this.mEepromType = 1;
        }

    @Override public short programEeprom(FT_EEPROM ee) throws RobotUsbException
        {
        int[] dataToWrite = new int[this.mEepromSize];
        short counter = 0;
        if (ee.getClass() != FT_EEPROM_X_Series.class)
            {
            return 1;
            }
        else
            {
            FT_EEPROM_X_Series eeprom = (FT_EEPROM_X_Series) ee;

            do
                {
                dataToWrite[counter] = this.readWord(counter);
                } while (++counter < this.mEepromSize);

            try
                {
                dataToWrite[0] = 0;
                if (eeprom.BCDEnable)
                    {
                    dataToWrite[0] |= 1;
                    }

                if (eeprom.BCDForceCBusPWREN)
                    {
                    dataToWrite[0] |= 2;
                    }

                if (eeprom.BCDDisableSleep)
                    {
                    dataToWrite[0] |= 4;
                    }

                if (eeprom.RS485EchoSuppress)
                    {
                    dataToWrite[0] |= 8;
                    }

                if (eeprom.A_LoadVCP)
                    {
                    dataToWrite[0] |= 128;
                    }

                if (eeprom.PowerSaveEnable)
                    {
                    boolean e = false;
                    if (eeprom.CBus0 == 17)
                        {
                        e = true;
                        }

                    if (eeprom.CBus1 == 17)
                        {
                        e = true;
                        }

                    if (eeprom.CBus2 == 17)
                        {
                        e = true;
                        }

                    if (eeprom.CBus3 == 17)
                        {
                        e = true;
                        }

                    if (eeprom.CBus4 == 17)
                        {
                        e = true;
                        }

                    if (eeprom.CBus5 == 17)
                        {
                        e = true;
                        }

                    if (eeprom.CBus6 == 17)
                        {
                        e = true;
                        }

                    if (!e)
                        {
                        return 1;
                        }

                    dataToWrite[0] |= 64;
                    }

                dataToWrite[1] = eeprom.VendorId;
                dataToWrite[2] = eeprom.ProductId;
                dataToWrite[3] = 4096;
                dataToWrite[4] = this.setUSBConfig(ee);
                dataToWrite[5] = this.setDeviceControl(ee);
                if (eeprom.FT1248ClockPolarity)
                    {
                    dataToWrite[5] |= 16;
                    }

                if (eeprom.FT1248LSB)
                    {
                    dataToWrite[5] |= 32;
                    }

                if (eeprom.FT1248FlowControl)
                    {
                    dataToWrite[5] |= 64;
                    }

                if (eeprom.I2CDisableSchmitt)
                    {
                    dataToWrite[5] |= 128;
                    }

                if (eeprom.InvertTXD)
                    {
                    dataToWrite[5] |= 256;
                    }

                if (eeprom.InvertRXD)
                    {
                    dataToWrite[5] |= 512;
                    }

                if (eeprom.InvertRTS)
                    {
                    dataToWrite[5] |= 1024;
                    }

                if (eeprom.InvertCTS)
                    {
                    dataToWrite[5] |= 2048;
                    }

                if (eeprom.InvertDTR)
                    {
                    dataToWrite[5] |= 4096;
                    }

                if (eeprom.InvertDSR)
                    {
                    dataToWrite[5] |= 8192;
                    }

                if (eeprom.InvertDCD)
                    {
                    dataToWrite[5] |= 16384;
                    }

                if (eeprom.InvertRI)
                    {
                    dataToWrite[5] |= '耀';
                    }

                dataToWrite[6] = 0;
                byte var19 = eeprom.AD_DriveCurrent;
                if (var19 == -1)
                    {
                    var19 = 0;
                    }

                dataToWrite[6] |= var19;
                if (eeprom.AD_SlowSlew)
                    {
                    dataToWrite[6] |= 4;
                    }

                if (eeprom.AD_SchmittInput)
                    {
                    dataToWrite[6] |= 8;
                    }

                byte driveC = eeprom.AC_DriveCurrent;
                if (driveC == -1)
                    {
                    driveC = 0;
                    }

                short var17 = (short) (driveC << 4);
                dataToWrite[6] |= var17;
                if (eeprom.AC_SlowSlew)
                    {
                    dataToWrite[6] |= 64;
                    }

                if (eeprom.AC_SchmittInput)
                    {
                    dataToWrite[6] |= 128;
                    }

                byte offset = 80;
                int var18 = this.setStringDescriptor(eeprom.Manufacturer, dataToWrite, offset, 7, false);
                var18 = this.setStringDescriptor(eeprom.Product, dataToWrite, var18, 8, false);
                if (eeprom.SerNumEnable)
                    {
                    this.setStringDescriptor(eeprom.SerialNumber, dataToWrite, var18, 9, false);
                    }

                dataToWrite[10] = eeprom.I2CSlaveAddress;
                dataToWrite[11] = eeprom.I2CDeviceID & '\uffff';
                dataToWrite[12] = eeprom.I2CDeviceID >> 16;
                byte c0 = eeprom.CBus0;
                if (c0 == -1)
                    {
                    c0 = 0;
                    }

                byte c1 = eeprom.CBus1;
                if (c1 == -1)
                    {
                    c1 = 0;
                    }

                int var20 = c1 << 8;
                dataToWrite[13] = (short) (c0 | var20);
                byte c2 = eeprom.CBus2;
                if (c2 == -1)
                    {
                    c2 = 0;
                    }

                byte c3 = eeprom.CBus3;
                if (c3 == -1)
                    {
                    c3 = 0;
                    }

                int var21 = c3 << 8;
                dataToWrite[14] = (short) (c2 | var21);
                byte c4 = eeprom.CBus4;
                if (c4 == -1)
                    {
                    c4 = 0;
                    }

                byte c5 = eeprom.CBus5;
                if (c5 == -1)
                    {
                    c5 = 0;
                    }

                int var22 = c5 << 8;
                dataToWrite[15] = (short) (c4 | var22);
                byte c6 = eeprom.CBus6;
                if (c6 == -1)
                    {
                    c6 = 0;
                    }

                dataToWrite[16] = (short) c6;
                if (dataToWrite[1] != 0 && dataToWrite[2] != 0)
                    {
                    boolean returnCode = false;
                    returnCode = this.programXeeprom(dataToWrite, this.mEepromSize - 1);
                    return (short) (returnCode ? 0 : 1);
                    }
                else
                    {
                    return 2;
                    }
                }
            catch (Exception var16)
                {
                var16.printStackTrace();
                return 0;
                }
            }
        }

    boolean programXeeprom(int[] dataToWrite, int ee_size) throws RobotUsbException
        {
        int checksumLocation = ee_size;
        int Checksum = 'ꪪ';
        boolean TempChecksum = false;
        int addressCounter = 0;
        boolean a = false;
        boolean b = false;
        boolean data = false;

        do
            {
            int var13 = dataToWrite[addressCounter];
            var13 &= '\uffff';
            this.writeWord((short) addressCounter, (short) var13);
            int var10 = var13 ^ Checksum;
            var10 &= '\uffff';
            int var11 = var10 << 1;
            var11 &= '\uffff';
            byte var12;
            if ((var10 & '耀') > 0)
                {
                var12 = 1;
                }
            else
                {
                var12 = 0;
                }

            Checksum = var11 | var12;
            Checksum &= '\uffff';
            ++addressCounter;
            if (addressCounter == 18)
                {
                addressCounter = 64;
                }
            } while (addressCounter != checksumLocation);

        this.writeWord((short) checksumLocation, (short) Checksum);
        return true;
        }

    @Override public FT_EEPROM readEeprom()
        {
        FT_EEPROM_X_Series eeprom = new FT_EEPROM_X_Series();
        int[] dataRead = new int[this.mEepromSize];

        try
            {
            short e;
            for (e = 0; e < this.mEepromSize; ++e)
                {
                dataRead[e] = this.readWord(e);
                }

            if ((dataRead[0] & 1) > 0)
                {
                eeprom.BCDEnable = true;
                }
            else
                {
                eeprom.BCDEnable = false;
                }

            if ((dataRead[0] & 2) > 0)
                {
                eeprom.BCDForceCBusPWREN = true;
                }
            else
                {
                eeprom.BCDForceCBusPWREN = false;
                }

            if ((dataRead[0] & 4) > 0)
                {
                eeprom.BCDDisableSleep = true;
                }
            else
                {
                eeprom.BCDDisableSleep = false;
                }

            if ((dataRead[0] & 8) > 0)
                {
                eeprom.RS485EchoSuppress = true;
                }
            else
                {
                eeprom.RS485EchoSuppress = false;
                }

            if ((dataRead[0] & 64) > 0)
                {
                eeprom.PowerSaveEnable = true;
                }
            else
                {
                eeprom.PowerSaveEnable = false;
                }

            if ((dataRead[0] & 128) > 0)
                {
                eeprom.A_LoadVCP = true;
                eeprom.A_LoadD2XX = false;
                }
            else
                {
                eeprom.A_LoadVCP = false;
                eeprom.A_LoadD2XX = true;
                }

            eeprom.VendorId = (short) dataRead[1];
            eeprom.ProductId = (short) dataRead[2];
            this.getUSBConfig(eeprom, dataRead[4]);
            this.getDeviceControl(eeprom, dataRead[5]);
            if ((dataRead[5] & 16) > 0)
                {
                eeprom.FT1248ClockPolarity = true;
                }
            else
                {
                eeprom.FT1248ClockPolarity = false;
                }

            if ((dataRead[5] & 32) > 0)
                {
                eeprom.FT1248LSB = true;
                }
            else
                {
                eeprom.FT1248LSB = false;
                }

            if ((dataRead[5] & 64) > 0)
                {
                eeprom.FT1248FlowControl = true;
                }
            else
                {
                eeprom.FT1248FlowControl = false;
                }

            if ((dataRead[5] & 128) > 0)
                {
                eeprom.I2CDisableSchmitt = true;
                }
            else
                {
                eeprom.I2CDisableSchmitt = false;
                }

            if ((dataRead[5] & 256) == 256)
                {
                eeprom.InvertTXD = true;
                }
            else
                {
                eeprom.InvertTXD = false;
                }

            if ((dataRead[5] & 512) == 512)
                {
                eeprom.InvertRXD = true;
                }
            else
                {
                eeprom.InvertRXD = false;
                }

            if ((dataRead[5] & 1024) == 1024)
                {
                eeprom.InvertRTS = true;
                }
            else
                {
                eeprom.InvertRTS = false;
                }

            if ((dataRead[5] & 2048) == 2048)
                {
                eeprom.InvertCTS = true;
                }
            else
                {
                eeprom.InvertCTS = false;
                }

            if ((dataRead[5] & 4096) == 4096)
                {
                eeprom.InvertDTR = true;
                }
            else
                {
                eeprom.InvertDTR = false;
                }

            if ((dataRead[5] & 8192) == 8192)
                {
                eeprom.InvertDSR = true;
                }
            else
                {
                eeprom.InvertDSR = false;
                }

            if ((dataRead[5] & 16384) == 16384)
                {
                eeprom.InvertDCD = true;
                }
            else
                {
                eeprom.InvertDCD = false;
                }

            if ((dataRead[5] & '耀') == '耀')
                {
                eeprom.InvertRI = true;
                }
            else
                {
                eeprom.InvertRI = false;
                }

            e = (short) (dataRead[6] & 3);
            switch (e)
                {
                case 0:
                    eeprom.AD_DriveCurrent = 0;
                    break;
                case 1:
                    eeprom.AD_DriveCurrent = 1;
                    break;
                case 2:
                    eeprom.AD_DriveCurrent = 2;
                    break;
                case 3:
                    eeprom.AD_DriveCurrent = 3;
                }

            short data2x06 = (short) (dataRead[6] & 4);
            if (data2x06 == 4)
                {
                eeprom.AD_SlowSlew = true;
                }
            else
                {
                eeprom.AD_SlowSlew = false;
                }

            short data3x06 = (short) (dataRead[6] & 8);
            if (data3x06 == 8)
                {
                eeprom.AD_SchmittInput = true;
                }
            else
                {
                eeprom.AD_SchmittInput = false;
                }

            short data45x06 = (short) ((dataRead[6] & 48) >> 4);
            switch (data45x06)
                {
                case 0:
                    eeprom.AC_DriveCurrent = 0;
                    break;
                case 1:
                    eeprom.AC_DriveCurrent = 1;
                    break;
                case 2:
                    eeprom.AC_DriveCurrent = 2;
                    break;
                case 3:
                    eeprom.AC_DriveCurrent = 3;
                }

            short data6x06 = (short) (dataRead[6] & 64);
            if (data6x06 == 64)
                {
                eeprom.AC_SlowSlew = true;
                }
            else
                {
                eeprom.AC_SlowSlew = false;
                }

            short data7x06 = (short) (dataRead[6] & 128);
            if (data7x06 == 128)
                {
                eeprom.AC_SchmittInput = true;
                }
            else
                {
                eeprom.AC_SchmittInput = false;
                }

            eeprom.I2CSlaveAddress = dataRead[10];
            eeprom.I2CDeviceID = dataRead[11];
            eeprom.I2CDeviceID |= (dataRead[12] & 255) << 16;
            eeprom.CBus0 = (byte) (dataRead[13] & 255);
            eeprom.CBus1 = (byte) (dataRead[13] >> 8 & 255);
            eeprom.CBus2 = (byte) (dataRead[14] & 255);
            eeprom.CBus3 = (byte) (dataRead[14] >> 8 & 255);
            eeprom.CBus4 = (byte) (dataRead[15] & 255);
            eeprom.CBus5 = (byte) (dataRead[15] >> 8 & 255);
            eeprom.CBus6 = (byte) (dataRead[16] & 255);
            this.mEepromType = (short) (dataRead[73] >> 8);
            int addr = dataRead[7] & 255;
            addr /= 2;
            eeprom.Manufacturer = this.getStringDescriptor(addr, dataRead);
            addr = dataRead[8] & 255;
            addr /= 2;
            eeprom.Product = this.getStringDescriptor(addr, dataRead);
            addr = dataRead[9] & 255;
            addr /= 2;
            eeprom.SerialNumber = this.getStringDescriptor(addr, dataRead);
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
                var9 = this.programXeeprom(eeprom, this.mEepromSize - 1);
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
