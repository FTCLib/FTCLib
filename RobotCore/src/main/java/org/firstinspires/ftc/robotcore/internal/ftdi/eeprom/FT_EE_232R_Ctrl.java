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
public class FT_EE_232R_Ctrl extends FT_EE_Ctrl
    {
    private static final short EEPROM_SIZE = 80;
    private static final short ENDOFUSERLOCATION = 63;
    private static final short EE_MAX_SIZE = 1024;
    private static final int EXTERNAL_OSCILLATOR = 2;
    private static final int HIGH_CURRENT_IO = 4;
    private static final int LOAD_D2XX_DRIVER = 8;
    private static final int INVERT_TXD = 256;
    private static final int INVERT_RXD = 512;
    private static final int INVERT_RTS = 1024;
    private static final int INVERT_CTS = 2048;
    private static final int INVERT_DTR = 4096;
    private static final int INVERT_DSR = 8192;
    private static final int INVERT_DCD = 16384;
    private static final int INVERT_RI = 32768;
    private FtDevice ftDevice;

    public FT_EE_232R_Ctrl(FtDevice usbC)
        {
        super(usbC);
        ftDevice = usbC;
        }

    @Override public void writeWord(short offset, short value) throws RobotUsbException
        {
        if (offset >= 1024)
            {
            throw new IllegalArgumentException(String.format("offset >= 1024: %d", offset));
            }
        else
            {
            byte latency = ftDevice.getLatencyTimer();
            ftDevice.setLatencyTimer((byte) 119);
            try {
                int status = ftDevice.getConnection().controlTransfer(64, 145, value, offset, (byte[]) null, 0, 0);
                FtDevice.throwIfStatus(status, "writeWord");
                }
            finally
                {
                ftDevice.setLatencyTimer(latency);
                }
            }
        }

    @Override public short programEeprom(FT_EEPROM ee)
        {
        if (ee.getClass() != FT_EEPROM_232R.class)
            {
            return 1;
            }
        else
            {
            int[] data = new int[80];
            FT_EEPROM_232R eeprom = (FT_EEPROM_232R) ee;

            try
                {
                for (short e = 0; e < 80; ++e)
                    {
                    data[e] = this.readWord(e);
                    }

                byte wordx00 = 0;
                int var17 = wordx00 | data[0] & '\uff00';
                if (eeprom.HighIO)
                    {
                    var17 |= 4;
                    }

                if (eeprom.LoadVCP)
                    {
                    var17 |= 8;
                    }

                if (eeprom.ExternalOscillator)
                    {
                    var17 |= 2;
                    }
                else
                    {
                    var17 &= '�';
                    }

                data[0] = var17;
                data[1] = eeprom.VendorId;
                data[2] = eeprom.ProductId;
                data[3] = 1536;
                data[4] = this.setUSBConfig(ee);
                int wordx05 = this.setDeviceControl(ee);
                if (eeprom.InvertTXD)
                    {
                    wordx05 |= 256;
                    }

                if (eeprom.InvertRXD)
                    {
                    wordx05 |= 512;
                    }

                if (eeprom.InvertRTS)
                    {
                    wordx05 |= 1024;
                    }

                if (eeprom.InvertCTS)
                    {
                    wordx05 |= 2048;
                    }

                if (eeprom.InvertDTR)
                    {
                    wordx05 |= 4096;
                    }

                if (eeprom.InvertDSR)
                    {
                    wordx05 |= 8192;
                    }

                if (eeprom.InvertDCD)
                    {
                    wordx05 |= 16384;
                    }

                if (eeprom.InvertRI)
                    {
                    wordx05 |= '耀';
                    }

                data[5] = wordx05;
                boolean wordx0A = false;
                byte var19 = eeprom.CBus0;
                byte c1 = eeprom.CBus1;
                int var20 = c1 << 4;
                byte c2 = eeprom.CBus2;
                int var21 = c2 << 8;
                byte c3 = eeprom.CBus3;
                int var22 = c3 << 12;
                int var18 = var19 | var20 | var21 | var22;
                data[10] = var18;
                boolean wordx0B = false;
                byte c4 = eeprom.CBus4;
                data[11] = c4;
                byte saddr = 12;
                int var23 = this.setStringDescriptor(eeprom.Manufacturer, data, saddr, 7, true);
                var23 = this.setStringDescriptor(eeprom.Product, data, var23, 8, true);
                if (eeprom.SerNumEnable)
                    {
                    this.setStringDescriptor(eeprom.SerialNumber, data, var23, 9, true);
                    }

                if (data[1] != 0 && data[2] != 0)
                    {
                    boolean returnCode = false;
                    byte latencyTimer = ftDevice.getLatencyTimer();
                    ftDevice.setLatencyTimer((byte) 119);
                    try {
                        returnCode = this.programEeprom(data, 63);
                        }
                    finally
                        {
                        ftDevice.setLatencyTimer(latencyTimer);
                        }
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

    @Override public FT_EEPROM readEeprom()
        {
        FT_EEPROM_232R eeprom = new FT_EEPROM_232R();
        int[] data = new int[80];

        try
            {
            int e;
            for (e = 0; e < 80; ++e)
                {
                data[e] = this.readWord((short) e);
                }

            if ((data[0] & 4) == 4)
                {
                eeprom.HighIO = true;
                }
            else
                {
                eeprom.HighIO = false;
                }

            if ((data[0] & 8) == 8)
                {
                eeprom.LoadVCP = true;
                }
            else
                {
                eeprom.LoadVCP = false;
                }

            if ((data[0] & 2) == 2)
                {
                eeprom.ExternalOscillator = true;
                }
            else
                {
                eeprom.ExternalOscillator = false;
                }

            eeprom.VendorId = (short) data[1];
            eeprom.ProductId = (short) data[2];
            this.getUSBConfig(eeprom, data[4]);
            this.getDeviceControl(eeprom, data[5]);
            if ((data[5] & 256) == 256)
                {
                eeprom.InvertTXD = true;
                }
            else
                {
                eeprom.InvertTXD = false;
                }

            if ((data[5] & 512) == 512)
                {
                eeprom.InvertRXD = true;
                }
            else
                {
                eeprom.InvertRXD = false;
                }

            if ((data[5] & 1024) == 1024)
                {
                eeprom.InvertRTS = true;
                }
            else
                {
                eeprom.InvertRTS = false;
                }

            if ((data[5] & 2048) == 2048)
                {
                eeprom.InvertCTS = true;
                }
            else
                {
                eeprom.InvertCTS = false;
                }

            if ((data[5] & 4096) == 4096)
                {
                eeprom.InvertDTR = true;
                }
            else
                {
                eeprom.InvertDTR = false;
                }

            if ((data[5] & 8192) == 8192)
                {
                eeprom.InvertDSR = true;
                }
            else
                {
                eeprom.InvertDSR = false;
                }

            if ((data[5] & 16384) == 16384)
                {
                eeprom.InvertDCD = true;
                }
            else
                {
                eeprom.InvertDCD = false;
                }

            if ((data[5] & '耀') == '耀')
                {
                eeprom.InvertRI = true;
                }
            else
                {
                eeprom.InvertRI = false;
                }

            e = data[10];
            int cbus0 = e & 15;
            eeprom.CBus0 = (byte) cbus0;
            int cbus1 = e & 240;
            eeprom.CBus1 = (byte) (cbus1 >> 4);
            int cbus2 = e & 3840;
            eeprom.CBus2 = (byte) (cbus2 >> 8);
            int cbus3 = e & '\uf000';
            eeprom.CBus3 = (byte) (cbus3 >> 12);
            int cbus4 = data[11] & 255;
            eeprom.CBus4 = (byte) cbus4;
            int addr = data[7] & 255;
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
            return eeprom;
            }
        catch (Exception var10)
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
        int ptr = 12 + ptr07 + ptr08 + 1;
        data = this.readWord((short) 9);
        int length = (data & '\uff00') >> 8;
        length /= 2;
        return (63 - ptr - length - 1) * 2;
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
            int[] eeprom = new int[80];

            for (short latency = 0; latency < 80; ++latency)
                {
                eeprom[latency] = this.readWord(latency);
                }

            short var8 = (short) (63 - this.getUserSize() / 2 - 1);
            var8 = (short) (var8 & '\uffff');

            for (int var9 = 0; var9 < data.length; var9 += 2)
                {
                int var7;
                if (var9 + 1 < data.length)
                    {
                    var7 = data[var9 + 1] & 255;
                    }
                else
                    {
                    var7 = 0;
                    }

                var7 <<= 8;
                var7 |= data[var9] & 255;
                eeprom[var8++] = var7;
                }

            if (eeprom[1] != 0 && eeprom[2] != 0)
                {
                boolean returnCode = false;
                byte latencyTimer = ftDevice.getLatencyTimer();
                ftDevice.setLatencyTimer((byte) 119);
                try {
                    returnCode = this.programEeprom(eeprom, 63);
                    }
                finally
                    {
                    ftDevice.setLatencyTimer(latencyTimer);
                    }
                if (!returnCode)
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
