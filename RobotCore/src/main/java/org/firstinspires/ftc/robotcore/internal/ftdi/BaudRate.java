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
package org.firstinspires.ftc.robotcore.internal.ftdi;

/**
 * Does baud rate calculations for FTDI devices. Needs more cleanup.
 */
@SuppressWarnings("WeakerAccess")
public final class BaudRate extends FtConstants
    {
    private BaudRate()
        {
        }

    /** @hide */
    public static byte getDivisor(int rate, int[] divisors, boolean isBitModeDevice)
        {
        byte divisor;
        if ((divisor = calcDivisor(rate, divisors, isBitModeDevice)) == -1)
            {
            return -1;
            }
        else
            {
            if (divisor == 0)
                {
                divisors[0] = (divisors[0] & -49153 /*0x3fff?*/) + 1;
                }

            int temp_actual = calcBaudRate(divisors[0], divisors[1], isBitModeDevice);
            int temp_accuracy;
            int temp_mod;
            if (rate > temp_actual)
                {
                temp_accuracy = rate * 100 / temp_actual - 100;
                temp_mod = rate % temp_actual * 100 % temp_actual;
                }
            else
                {
                temp_accuracy = temp_actual * 100 / rate - 100;
                temp_mod = temp_actual % rate * 100 % rate;
                }

            byte rval1;
            if (temp_accuracy < 3)
                {
                rval1 = 1;
                }
            else if (temp_accuracy == 3 && temp_mod == 0)
                {
                rval1 = 1;
                }
            else
                {
                rval1 = 0;
                }

            return rval1;
            }
        }

    private static byte calcDivisor(int rate, int[] divisors, boolean bm)
        {
        byte ok = 1;
        if (rate == 0)
            {
            return -1;
            }
        else if ((CLOCK_RATE / rate & -16384) > 0)
            {
            return -1;
            }
        else
            {
            divisors[0] = CLOCK_RATE / rate;
            divisors[1] = 0;
            int t;
            if (divisors[0] == 1)
                {
                t = CLOCK_RATE % rate * 100 / rate;
                if (t <= 3)
                    {
                    divisors[0] = 0;
                    }
                }

            if (divisors[0] == 0)
                {
                return ok;
                }
            else
                {
                t = CLOCK_RATE % rate * 100 / rate;
                char modifier;
                if (!bm)
                    {
                    if (t <= 6)
                        {
                        modifier = 0;
                        }
                    else if (t <= 18)
                        {
                        modifier = '쀀';
                        }
                    else if (t <= 37)
                        {
                        modifier = '耀';
                        }
                    else if (t <= 75)
                        {
                        modifier = 16384;
                        }
                    else
                        {
                        modifier = 0;
                        ok = 0;
                        }
                    }
                else if (t <= 6)
                    {
                    modifier = 0;
                    }
                else if (t <= 18)
                    {
                    modifier = '쀀';
                    }
                else if (t <= 31)
                    {
                    modifier = '耀';
                    }
                else if (t <= 43)
                    {
                    modifier = 0;
                    divisors[1] = 1;
                    }
                else if (t <= 56)
                    {
                    modifier = 16384;
                    }
                else if (t <= 68)
                    {
                    modifier = 16384;
                    divisors[1] = 1;
                    }
                else if (t <= 81)
                    {
                    modifier = '耀';
                    divisors[1] = 1;
                    }
                else if (t <= 93)
                    {
                    modifier = '쀀';
                    divisors[1] = 1;
                    }
                else
                    {
                    modifier = 0;
                    ok = 0;
                    }

                divisors[0] |= modifier;
                return ok;
                }
            }
        }
    /** @hide */
    private static int calcBaudRate(int divisor, int extdiv, boolean isBitModeDevice)
        {
        if (divisor == 0)
            {
            return CLOCK_RATE;
            }
        else
            {
            int rate = (divisor & -49153) * 100;
            if (!isBitModeDevice)
                {
                switch (divisor & /*49152*/0xC000)
                    {
                    case 0x4000:
                        rate += 50;
                        break;
                    case 0x8000:
                        rate += 25;
                        break;
                    case 0xC000:
                        rate += 12;
                    }
                }
            else if (extdiv == 0)
                {
                switch (divisor & 0xC000)
                    {
                    case 0x4000:
                        rate += 50;
                        break;
                    case 0x8000:
                        rate += 25;
                        break;
                    case 0xC000:
                        rate += 12;
                    }
                }
            else
                {
                switch (divisor & 0xC000)
                    {
                    case 0:
                        rate += 37;
                        break;
                    case 0x4000:
                        rate += 62;
                        break;
                    case 0x8000:
                        rate += 75;
                        break;
                    case 0xC000:
                        rate += 87;
                    }
                }

            rate = CLOCK_RATE * 100 / rate;
            return rate;
            }
        }

    public static byte getDivisorHi(int rate, int[] divisors)
        {
        byte rval;
        if ((rval = calcDivisorHi(rate, divisors)) == -1)
            {
            return -1;
            }
        else
            {
            if (rval == 0)
                {
                divisors[0] = (divisors[0] & -49153) + 1;
                }

            int temp_actual = calcBaudRateHi(divisors[0], divisors[1]);
            int temp_accuracy;
            int temp_mod;
            if (rate > temp_actual)
                {
                temp_accuracy = rate * 100 / temp_actual - 100;
                temp_mod = rate % temp_actual * 100 % temp_actual;
                }
            else
                {
                temp_accuracy = temp_actual * 100 / rate - 100;
                temp_mod = temp_actual % rate * 100 % rate;
                }

            byte rval1;
            if (temp_accuracy < 3)
                {
                rval1 = 1;
                }
            else if (temp_accuracy == 3 && temp_mod == 0)
                {
                rval1 = 1;
                }
            else
                {
                rval1 = 0;
                }

            return rval1;
            }
        }

    private static byte calcDivisorHi(int rate, int[] divisors)
        {
        byte ok = 1;
        if (rate == 0)
            {
            return -1;
            }
        else if ((CLOCK_RATE_HI / rate & -16384) > 0)
            {
            return -1;
            }
        else
            {
            divisors[1] = 2;
            if (rate >= 11640000 && rate <= 12360000)
                {
                divisors[0] = 0;
                return ok;
                }
            else if (rate >= 7760000 && rate <= 8240000)
                {
                divisors[0] = 1;
                return ok;
                }
            else
                {
                divisors[0] = CLOCK_RATE_HI / rate;
                divisors[1] = 2;
                int t;
                if (divisors[0] == 1)
                    {
                    t = CLOCK_RATE_HI % rate * 100 / rate;
                    if (t <= 3)
                        {
                        divisors[0] = 0;
                        }
                    }

                if (divisors[0] == 0)
                    {
                    return ok;
                    }
                else
                    {
                    t = CLOCK_RATE_HI % rate * 100 / rate;
                    char modifier;
                    if (t <= 6)
                        {
                        modifier = 0;
                        }
                    else if (t <= 18)
                        {
                        modifier = '쀀';
                        }
                    else if (t <= 31)
                        {
                        modifier = '耀';
                        }
                    else if (t <= 43)
                        {
                        modifier = 0;
                        divisors[1] |= 1;
                        }
                    else if (t <= 56)
                        {
                        modifier = 16384;
                        }
                    else if (t <= 68)
                        {
                        modifier = 16384;
                        divisors[1] |= 1;
                        }
                    else if (t <= 81)
                        {
                        modifier = '耀';
                        divisors[1] |= 1;
                        }
                    else if (t <= 93)
                        {
                        modifier = '쀀';
                        divisors[1] |= 1;
                        }
                    else
                        {
                        modifier = 0;
                        ok = 0;
                        }

                    divisors[0] |= modifier;
                    return ok;
                    }
                }
            }
        }

    private static int calcBaudRateHi(int divisor, int extdiv)
        {
        if (divisor == 0)
            {
            return CLOCK_RATE_HI;
            }
        else if (divisor == 1)
            {
            return 8000000;
            }
        else
            {
            int rate = (divisor & -49153) * 100;
            extdiv &= '�';
            if (extdiv == 0)
                {
                switch (divisor & 49152)
                    {
                    case 16384:
                        rate += 50;
                        break;
                    case 32768:
                        rate += 25;
                        break;
                    case 49152:
                        rate += 12;
                    }
                }
            else
                {
                switch (divisor & 49152)
                    {
                    case 0:
                        rate += 37;
                        break;
                    case 16384:
                        rate += 62;
                        break;
                    case 32768:
                        rate += 75;
                        break;
                    case 49152:
                        rate += 87;
                    }
                }

            rate = CLOCK_RATE_HI * 100 / rate;
            return rate;
            }
        }
    }
