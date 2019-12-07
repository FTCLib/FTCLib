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

/**
 * Created by bob on 3/18/2017.
 */
@SuppressWarnings("WeakerAccess")
public class FT_EEPROM_232H extends FT_EEPROM
    {
    public boolean AL_SlowSlew = false;
    public boolean AL_SchmittInput = false;
    public byte AL_DriveCurrent = 0;
    public boolean BL_SlowSlew = false;
    public boolean BL_SchmittInput = false;
    public byte BL_DriveCurrent = 0;
    public byte CBus0 = 0;
    public byte CBus1 = 0;
    public byte CBus2 = 0;
    public byte CBus3 = 0;
    public byte CBus4 = 0;
    public byte CBus5 = 0;
    public byte CBus6 = 0;
    public byte CBus7 = 0;
    public byte CBus8 = 0;
    public byte CBus9 = 0;
    public boolean UART = false;
    public boolean FIFO = false;
    public boolean FIFOTarget = false;
    public boolean FastSerial = false;
    public boolean FT1248 = false;
    public boolean FT1248ClockPolarity = false;
    public boolean FT1248LSB = false;
    public boolean FT1248FlowControl = false;
    public boolean PowerSaveEnable = false;
    public boolean LoadVCP = false;
    public boolean LoadD2XX = false;

    public FT_EEPROM_232H()
        {
        }

    public static final class CBUS
        {
        static final int TRISTATE = 0;
        static final int TXLED = 1;
        static final int RXLED = 2;
        static final int TXRXLED = 3;
        static final int PWREN = 4;
        static final int SLEEP = 5;
        static final int DRIVE_0 = 6;
        static final int DRIVE_1 = 7;
        static final int GPIO_MODE = 8;
        static final int TXDEN = 9;
        static final int CLK30MHz = 10;
        static final int CLK15MHz = 11;
        static final int CLK7_5MHz = 12;

        public CBUS()
            {
            }
        }

    public static final class DRIVE_STRENGTH
        {
        static final byte DRIVE_4mA = 0;
        static final byte DRIVE_8mA = 1;
        static final byte DRIVE_12mA = 2;
        static final byte DRIVE_16mA = 3;

        public DRIVE_STRENGTH()
            {
            }
        }
    }
