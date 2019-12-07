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
import org.firstinspires.ftc.robotcore.internal.ftdi.FtDeviceIOException;
import org.firstinspires.ftc.robotcore.internal.usb.exception.RobotUsbException;

/**
 * Created by bob on 3/18/2017.
 */
@SuppressWarnings("WeakerAccess")
public class FT_EE_232H_Ctrl extends FT_EE_Ctrl
    {
    private static final byte EEPROM_SIZE_LOCATION = 15;
    private static final String DEFAULT_PID = "6014";
    private static final int AL_DRIVE_CURRENT = 3;
    private static final int BL_DRIVE_CURRENT = 768;
    private static final int AL_FAST_SLEW = 4;
    private static final int AL_SCHMITT_INPUT = 8;
    private static final int BL_FAST_SLEW = 1024;
    private static final int BL_SCHMITT_INPUT = 2048;
    private static FtDevice ft_device;

    public FT_EE_232H_Ctrl(FtDevice usbc) throws FtDeviceIOException, RobotUsbException
        {
        super(usbc);
        this.getEepromSize(EEPROM_SIZE_LOCATION);
        }

    @Override public short programEeprom(FT_EEPROM ee)
        {
        int[] dataToWrite = new int[this.mEepromSize];
        if (ee.getClass() != FT_EEPROM_232H.class)
            {
            return 1;
            }
        else
            {
            FT_EEPROM_232H eeprom = (FT_EEPROM_232H) ee;

            try
                {
                if (eeprom.FIFO)
                    {
                    dataToWrite[0] |= 1;
                    }
                else if (eeprom.FIFOTarget)
                    {
                    dataToWrite[0] |= 2;
                    }
                else if (eeprom.FastSerial)
                    {
                    dataToWrite[0] |= 4;
                    }

                if (eeprom.FT1248)
                    {
                    dataToWrite[0] |= 8;
                    }

                if (eeprom.LoadVCP)
                    {
                    dataToWrite[0] |= 16;
                    }

                if (eeprom.FT1248ClockPolarity)
                    {
                    dataToWrite[0] |= 256;
                    }

                if (eeprom.FT1248LSB)
                    {
                    dataToWrite[0] |= 512;
                    }

                if (eeprom.FT1248FlowControl)
                    {
                    dataToWrite[0] |= 1024;
                    }

                if (eeprom.PowerSaveEnable)
                    {
                    dataToWrite[0] |= '耀';
                    }

                dataToWrite[1] = eeprom.VendorId;
                dataToWrite[2] = eeprom.ProductId;
                dataToWrite[3] = 2304;
                dataToWrite[4] = this.setUSBConfig(ee);
                dataToWrite[5] = this.setDeviceControl(ee);
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

                byte driveC = eeprom.BL_DriveCurrent;
                if (driveC == -1)
                    {
                    driveC = 0;
                    }

                dataToWrite[6] |= (short) (driveC << 8);
                if (eeprom.BL_SlowSlew)
                    {
                    dataToWrite[6] |= 1024;
                    }

                if (eeprom.BL_SchmittInput)
                    {
                    dataToWrite[6] |= 2048;
                    }

                byte offset = 80;
                int offset1 = this.setStringDescriptor(eeprom.Manufacturer, dataToWrite, offset, 7, false);
                offset1 = this.setStringDescriptor(eeprom.Product, dataToWrite, offset1, 8, false);
                if (eeprom.SerNumEnable)
                    {
                    this.setStringDescriptor(eeprom.SerialNumber, dataToWrite, offset1, 9, false);
                    }

                dataToWrite[10] = 0;
                dataToWrite[11] = 0;
                dataToWrite[12] = 0;
                byte c0 = eeprom.CBus0;
                byte c1 = eeprom.CBus1;
                int c11 = c1 << 4;
                byte c2 = eeprom.CBus2;
                int c21 = c2 << 8;
                byte c3 = eeprom.CBus3;
                int c31 = c3 << 12;
                dataToWrite[12] = c0 | c11 | c21 | c31;
                dataToWrite[13] = 0;
                byte c4 = eeprom.CBus4;
                byte c5 = eeprom.CBus5;
                int c51 = c5 << 4;
                byte c6 = eeprom.CBus6;
                int c61 = c6 << 8;
                byte c7 = eeprom.CBus7;
                int c71 = c7 << 12;
                dataToWrite[13] = c4 | c51 | c61 | c71;
                dataToWrite[14] = 0;
                byte c8 = eeprom.CBus8;
                byte c9 = eeprom.CBus9;
                int c91 = c9 << 4;
                dataToWrite[14] = c8 | c91;
                dataToWrite[15] = this.mEepromType;
                dataToWrite[69] = 72;
                if (this.mEepromType == 70)
                    {
                    return 1;
                    }
                else if (dataToWrite[1] != 0 && dataToWrite[2] != 0)
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
            catch (Exception var18)
                {
                var18.printStackTrace();
                return 0;
                }
            }
        }

    @Override public FT_EEPROM readEeprom()
        {
        FT_EEPROM_232H eeprom = new FT_EEPROM_232H();
        int[] data = new int[this.mEepromSize];
        if (this.mEepromBlank)
            {
            return eeprom;
            }
        else
            {
            try
                {
                for (short e = 0; e < this.mEepromSize; ++e)
                    {
                    data[e] = this.readWord(e);
                    }

                eeprom.UART = false;
                switch (data[0] & 15)
                    {
                    case 0:
                        eeprom.UART = true;
                        break;
                    case 1:
                        eeprom.FIFO = true;
                        break;
                    case 2:
                        eeprom.FIFOTarget = true;
                        break;
                    case 3:
                    case 5:
                    case 6:
                    case 7:
                    default:
                        eeprom.UART = true;
                        break;
                    case 4:
                        eeprom.FastSerial = true;
                        break;
                    case 8:
                        eeprom.FT1248 = true;
                    }

                if ((data[0] & 16) > 0)
                    {
                    eeprom.LoadVCP = true;
                    eeprom.LoadD2XX = false;
                    }
                else
                    {
                    eeprom.LoadVCP = false;
                    eeprom.LoadD2XX = true;
                    }

                if ((data[0] & 256) > 0)
                    {
                    eeprom.FT1248ClockPolarity = true;
                    }
                else
                    {
                    eeprom.FT1248ClockPolarity = false;
                    }

                if ((data[0] & 512) > 0)
                    {
                    eeprom.FT1248LSB = true;
                    }
                else
                    {
                    eeprom.FT1248LSB = false;
                    }

                if ((data[0] & 1024) > 0)
                    {
                    eeprom.FT1248FlowControl = true;
                    }
                else
                    {
                    eeprom.FT1248FlowControl = false;
                    }

                if ((data[0] & '耀') > 0)
                    {
                    eeprom.PowerSaveEnable = true;
                    }

                eeprom.VendorId = (short) data[1];
                eeprom.ProductId = (short) data[2];
                this.getUSBConfig(eeprom, data[4]);
                this.getDeviceControl(eeprom, data[5]);
                int var17 = data[6] & 3;
                switch (var17)
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

                if ((data[6] & 4) > 0)
                    {
                    eeprom.AL_SlowSlew = true;
                    }
                else
                    {
                    eeprom.AL_SlowSlew = false;
                    }

                if ((data[6] & 8) > 0)
                    {
                    eeprom.AL_SchmittInput = true;
                    }
                else
                    {
                    eeprom.AL_SchmittInput = false;
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

                if ((data[6] & 1024) > 0)
                    {
                    eeprom.BL_SlowSlew = true;
                    }
                else
                    {
                    eeprom.BL_SlowSlew = false;
                    }

                if ((data[6] & 2048) > 0)
                    {
                    eeprom.BL_SchmittInput = true;
                    }
                else
                    {
                    eeprom.BL_SchmittInput = false;
                    }

                short cbus0 = (short) (data[12] >> 0 & 15);
                eeprom.CBus0 = (byte) cbus0;
                short cbus1 = (short) (data[12] >> 4 & 15);
                eeprom.CBus1 = (byte) cbus1;
                short cbus2 = (short) (data[12] >> 8 & 15);
                eeprom.CBus2 = (byte) cbus2;
                short cbus3 = (short) (data[12] >> 12 & 15);
                eeprom.CBus3 = (byte) cbus3;
                short cbus4 = (short) (data[13] >> 0 & 15);
                eeprom.CBus4 = (byte) cbus4;
                short cbus5 = (short) (data[13] >> 4 & 15);
                eeprom.CBus5 = (byte) cbus5;
                short cbus6 = (short) (data[13] >> 8 & 15);
                eeprom.CBus6 = (byte) cbus6;
                short cbus7 = (short) (data[13] >> 12 & 15);
                eeprom.CBus7 = (byte) cbus7;
                short cbus8 = (short) (data[14] >> 0 & 15);
                eeprom.CBus8 = (byte) cbus8;
                short cbus9 = (short) (data[14] >> 4 & 15);
                eeprom.CBus9 = (byte) cbus9;
                int addr = data[7] & 255;
                addr /= 2;
                eeprom.Manufacturer = this.getStringDescriptor(addr, data);
                addr = data[8] & 255;
                addr /= 2;
                eeprom.Product = this.getStringDescriptor(addr, data);
                addr = data[9] & 255;
                addr /= 2;
                eeprom.SerialNumber = this.getStringDescriptor(addr, data);
                return eeprom;
                }
            catch (Exception var16)
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
        ++ptr;
        int length = (data & '\uff00') >> 8;
        length /= 2;
        ++length;
        return (this.mEepromSize - ptr - 1 - length) * 2;
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
