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
 * Created by bob on 3/25/2017.
 */
@SuppressWarnings("WeakerAccess")
public class FtConstants
    {
    // Notable resources include the following:
    //
    // https://felhr85.net/2014/11/11/usbserial-a-serial-port-driver-library-for-android-v2-0/
    // https://github.com/felHR85/UsbSerial/blob/master/usbserial/src/main/java/com/felhr/usbserial/FTDISerialDevice.java
    // https://github.com/freebsd/freebsd/blob/70b396ca9c54a94c3fad73c3ceb0a76dffbde635/sys/dev/usb/serial/uftdi_reg.h
    // http://lxr.free-electrons.com/source/drivers/usb/serial/ftdi_sio.h
    // https://github.com/mik3y/usb-serial-for-android/blob/master/usbSerialForAndroid/src/main/java/com/hoho/android/usbserial/driver/FtdiSerialDriver.java
    // https://hackage.haskell.org/package/ftdi-0.2.0.1/docs/System-FTDI.html
    // https://www.codeproject.com/Articles/824708/Let-s-dig-into-an-issue-of-the-FT-chip
    // http://ftdi-usb-sio.sourceforge.net/
    // https://www.intra2net.com/en/developer/libftdi/index.php

    /*
     * "Definitions for the FTDI USB Single Port Serial Converter -
     * known as FTDI_SIO (Serial Input/Output application of the chipset)
     * The device is based on the FTDI FT8U100AX chip. It has a DB25 on one side,
     * USB on the other."
     */
    public static final int FTDI_BREAK_OFF          = 0;
    public static final int FTDI_BREAK_ON           = 0x4000;
    public static final int	FTDI_SIO_RESET 		    = 0;	/* Reset the port */
    public static final int	FTDI_SIO_MODEM_CTRL 	= 1;	/* Set the modem control register */
    public static final int	FTDI_SIO_SET_FLOW_CTRL	= 2;	/* Set flow control register */
    public static final int FTDI_SIO_SET_BAUDRATE   = 3;	/* Set baud rate */
    public static final int	FTDI_SIO_SET_DATA	    = 4;	/* Set the data characteristics of the port */
    public static final int	FTDI_SIO_GET_STATUS	    = 5;	/* Retrieve current value of status reg */
    public static final int	FTDI_SIO_SET_EVENT_CHAR	= 6;	/* Set the event character */
    public static final int	FTDI_SIO_SET_ERROR_CHAR	= 7;	/* Set the error character */
    public static final int	FTDI_SIO_SET_LATENCY	= 9;	/* Set the latency timer */
    public static final int	FTDI_SIO_GET_LATENCY	= 10;	/* Read the latency timer */
    public static final int	FTDI_SIO_SET_BITMODE	= 11;	/* Set the bit bang I/O mode */
    public static final int	FTDI_SIO_GET_BITMODE	= 12;	/* Read pin states from any mode */
    public static final int	FTDI_SIO_READ_EEPROM	= 144;	/* Read eeprom word */
    public static final int	FTDI_SIO_WRITE_EEPROM	= 145;	/* Write eeprom word */
    public static final int	FTDI_SIO_ERASE_EEPROM	= 146;	/* Erase entire eeprom */

    /* Port Identifier Table */
    public static final int FTDI_PIT_DEFAULT 	    = 0;	/* SIOA */
    public static final int FTDI_PIT_SIOA		    = 1;	/* SIOA */
    public static final int FTDI_PIT_SIOB		    = 2;	/* SIOB */
    public static final int FTDI_PIT_PARALLEL	    = 3;	/* Parallel */

    /* Values for driver_info */
    //public static final int UFTDI_JTAG_IFACE(i)	(1 << i)	/* Flag interface as jtag */
    public static final int UFTDI_JTAG_IFACES_MAX	= 8;		/* Allow up to 8 jtag intfs */
    public static final int UFTDI_JTAG_CHECK_STRING	= 0xff;	    /* Check product names table */
    public static final int UFTDI_JTAG_MASK		    = 0xff;

    /*
     * BmRequestType:  0100 0000B
     * bRequest:       FTDI_SIO_RESET
     * wValue:         Control Value
     *                   0 = Reset SIO
     *                   1 = Purge RX buffer
     *                   2 = Purge TX buffer
     * wIndex:         Port
     * wLength:        0
     * Data:           None
     *
     * The Reset SIO command has this effect:
     *
     *    Sets flow control set to 'none'
     *    Event char = 0x0d
     *    Event trigger = disabled
     *    Purge RX buffer
     *    Purge TX buffer
     *    Clear DTR
     *    Clear RTS
     *    baud and data format not reset
     *
     * The Purge RX and TX buffer commands affect nothing except the buffers
     */
    /* FTDI_SIO_RESET */
    public static final int FTDI_SIO_RESET_SIO      = 0;
    public static final int FTDI_SIO_RESET_PURGE_RX = 1;
    public static final int FTDI_SIO_RESET_PURGE_TX = 2;

    /*
     * BmRequestType:  0100 0000B
     * bRequest:       FTDI_SIO_SET_BAUDRATE
     * wValue:         BaudRate low bits
     * wIndex:         Port and BaudRate high bits
     * wLength:        0
     * Data:           None
     */
    /*
     * BmRequestType:  0100 0000B
     * bRequest:       FTDI_SIO_SET_BAUDRATE
     * wValue:         BaudDivisor value - see below
     * wIndex:         Port
     * wLength:        0
     * Data:           None
     * The BaudDivisor values are calculated as follows:
     * - BaseClock is either 12000000 or 48000000 depending on the device.
     *   FIXME: I wish I knew how to detect old chips to select proper base clock!
     * - BaudDivisor is a fixed point number encoded in a funny way.
     *   (--WRONG WAY OF THINKING--)
     *   BaudDivisor is a fixed point number encoded with following bit weighs:
     *   (-2)(-1)(13..0). It is a radical with a denominator of 4, so values
     *   end with 0.0 (00...), 0.25 (10...), 0.5 (01...), and 0.75 (11...).
     *   (--THE REALITY--)
     *   The both-bits-set has quite different meaning from 0.75 - the chip
     *   designers have decided it to mean 0.125 instead of 0.75.
     *   This info looked up in FTDI application note "FT8U232 DEVICES \ Data Rates
     *   and Flow Control Consideration for USB to RS232".
     * - BaudDivisor = (BaseClock / 16) / BaudRate, where the (=) operation should
     *   automagically re-encode the resulting value to take fractions into
     *   consideration.
     * As all values are integers, some bit twiddling is in order:
     *   BaudDivisor = (BaseClock / 16 / BaudRate) |
     *   (((BaseClock / 2 / BaudRate) & 4) ? 0x4000    // 0.5
     *    : ((BaseClock / 2 / BaudRate) & 2) ? 0x8000  // 0.25
     *    : ((BaseClock / 2 / BaudRate) & 1) ? 0xc000  // 0.125
     *    : 0)
     *
     * For the FT232BM, a 17th divisor bit was introduced to encode the multiples
     * of 0.125 missing from the FT8U232AM.  Bits 16 to 14 are coded as follows
     * (the first four codes are the same as for the FT8U232AM, where bit 16 is
     * always 0):
     *   000 - add .000 to divisor
     *   001 - add .500 to divisor
     *   010 - add .250 to divisor
     *   011 - add .125 to divisor
     *   100 - add .375 to divisor
     *   101 - add .625 to divisor
     *   110 - add .750 to divisor
     *   111 - add .875 to divisor
     * Bits 15 to 0 of the 17-bit divisor are placed in the urb value.  Bit 16 is
     * placed in bit 0 of the urb index.
     *
     * Note that there are a couple of special cases to support the highest baud
     * rates.  If the calculated divisor value is 1, this needs to be replaced with
     * 0.  Additionally for the FT232BM, if the calculated divisor value is 0x4001
     * (1.5), this needs to be replaced with 0x0001 (1) (but this divisor value is
     * not supported by the FT8U232AM).
     */
    /* FTDI_SIO_SET_BAUDRATE */

    /*
     * BmRequestType:  0100 0000B
     * bRequest:       FTDI_SIO_SET_DATA
     * wValue:         Data characteristics (see below)
     * wIndex:         Port
     * wLength:        0
     * Data:           No
     *
     * Data characteristics
     *
     *   B0..7   Number of data bits
     *   B8..10  Parity
     *           0 = None
     *           1 = Odd
     *           2 = Even
     *           3 = Mark
     *           4 = Space
     *   B11..13 Stop Bits
     *           0 = 1
     *           1 = 1.5
     *           2 = 2
     *   B14..15 Reserved
     *
     */
    /* FTDI_SIO_SET_DATA */
    // public static final int FTDI_SIO_SET_DATA_BITS(n)    (n)
    public static final int FTDI_SIO_SET_DATA_PARITY_NONE   = (0x0 << 8);
    public static final int FTDI_SIO_SET_DATA_PARITY_ODD    = (0x1 << 8);
    public static final int FTDI_SIO_SET_DATA_PARITY_EVEN   = (0x2 << 8);
    public static final int FTDI_SIO_SET_DATA_PARITY_MARK   = (0x3 << 8);
    public static final int FTDI_SIO_SET_DATA_PARITY_SPACE  = (0x4 << 8);
    public static final int FTDI_SIO_SET_DATA_STOP_BITS_1   = (0x0 << 11);
    public static final int FTDI_SIO_SET_DATA_STOP_BITS_15  = (0x1 << 11);
    public static final int FTDI_SIO_SET_DATA_STOP_BITS_2   = (0x2 << 11);
    public static final int FTDI_SIO_SET_BREAK              = (0x1 << 14);

    /*
     * BmRequestType:   0100 0000B
     * bRequest:        FTDI_SIO_MODEM_CTRL
     * wValue:          ControlValue (see below)
     * wIndex:          Port
     * wLength:         0
     * Data:            None
     *
     * NOTE: If the device is in RTS/CTS flow control, the RTS set by this
     * command will be IGNORED without an error being returned
     * Also - you can not set DTR and RTS with one control message
     *
     * ControlValue
     * B0    DTR state
     *          0 = reset
     *          1 = set
     * B1    RTS state
     *          0 = reset
     *          1 = set
     * B2..7 Reserved
     * B8    DTR state enable
     *          0 = ignore
     *          1 = use DTR state
     * B9    RTS state enable
     *          0 = ignore
     *          1 = use RTS state
     * B10..15 Reserved
     */
    /* FTDI_SIO_MODEM_CTRL */
    public static final int FTDI_SIO_SET_DTR_MASK   = 0x1;
    public static final int FTDI_SIO_SET_DTR_HIGH   = (1 | (FTDI_SIO_SET_DTR_MASK << 8));
    public static final int FTDI_SIO_SET_DTR_LOW    = (0 | (FTDI_SIO_SET_DTR_MASK << 8));
    public static final int FTDI_SIO_SET_RTS_MASK   = 0x2;
    public static final int FTDI_SIO_SET_RTS_HIGH   = (2 | (FTDI_SIO_SET_RTS_MASK << 8));
    public static final int FTDI_SIO_SET_RTS_LOW    = (0 | (FTDI_SIO_SET_RTS_MASK << 8));


    /*
     *   BmRequestType:  0100 0000b
     *   bRequest:       FTDI_SIO_SET_FLOW_CTRL
     *   wValue:         Xoff/Xon
     *   wIndex:         Protocol/Port - hIndex is protocol / lIndex is port
     *   wLength:        0
     *   Data:           None
     *
     * hIndex protocol is:
     *   B0 Output handshaking using RTS/CTS
     *       0 = disabled
     *       1 = enabled
     *   B1 Output handshaking using DTR/DSR
     *       0 = disabled
     *       1 = enabled
     *   B2 Xon/Xoff handshaking
     *       0 = disabled
     *       1 = enabled
     *
     * A value of zero in the hIndex field disables handshaking
     *
     * If Xon/Xoff handshaking is specified, the hValue field should contain the
     * XOFF character and the lValue field contains the XON character.
     */
    /* FTDI_SIO_SET_FLOW_CTRL */
    public static final int FTDI_SIO_DISABLE_FLOW_CTRL  = 0x0;
    public static final int FTDI_SIO_RTS_CTS_HS         = 0x1;
    public static final int FTDI_SIO_DTR_DSR_HS         = 0x2;
    public static final int FTDI_SIO_XON_XOFF_HS        = 0x4;

    /*
     *  BmRequestType:   0100 0000b
     *  bRequest:        FTDI_SIO_SET_EVENT_CHAR
     *  wValue:          Event Char
     *  wIndex:          Port
     *  wLength:         0
     *  Data:            None
     *
     * wValue:
     *   B0..7   Event Character
     *   B8      Event Character Processing
     *             0 = disabled
     *             1 = enabled
     *   B9..15  Reserved
     *
     * FTDI_SIO_SET_EVENT_CHAR
     *
     * Set the special event character for the specified communications port.
     * If the device sees this character it will immediately return the
     * data read so far - rather than wait 40ms or until 62 bytes are read
     * which is what normally happens.
     */

    /*
     *  BmRequestType:  0100 0000b
     *  bRequest:       FTDI_SIO_SET_ERROR_CHAR
     *  wValue:         Error Char
     *  wIndex:         Port
     *  wLength:        0
     *  Data:           None
     *
     *  Error Char
     *  B0..7  Error Character
     *  B8     Error Character Processing
     *           0 = disabled
     *           1 = enabled
     *  B9..15 Reserved
     * FTDI_SIO_SET_ERROR_CHAR
     * Set the parity error replacement character for the specified communications
     * port.
     */

    /*
     *   BmRequestType:   1100 0000b
     *   bRequest:        FTDI_SIO_GET_MODEM_STATUS
     *   wValue:          zero
     *   wIndex:          Port
     *   wLength:         1
     *   Data:            Status
     *
     * One byte of data is returned
     * B0..3 0
     * B4    CTS
     *         0 = inactive
     *         1 = active
     * B5    DSR
     *         0 = inactive
     *         1 = active
     * B6    Ring Indicator (RI)
     *         0 = inactive
     *         1 = active
     * B7    Receive Line Signal Detect (RLSD)
     *         0 = inactive
     *         1 = active
     *
     * FTDI_SIO_GET_MODEM_STATUS
     * Retrieve the current value of the modem status register.
     */
    public static final int FTDI_SIO_CTS_MASK   = 0x10;
    public static final int FTDI_SIO_DSR_MASK   = 0x20;
    public static final int FTDI_SIO_RI_MASK    = 0x40;
    public static final int FTDI_SIO_RLSD_MASK  = 0x80;

    /*
     * DATA FORMAT
     *
     * IN Endpoint
     *
     * The device reserves the first two bytes of data on this endpoint to contain
     * the current values of the modem and line status registers. In the absence of
     * data, the device generates a message consisting of these two status bytes
     * every 40 ms.
     *
     * Byte 0: Modem Status
     *   NOTE: 4 upper bits have same layout as the MSR register in a 16550
     *
     * Offset	Description
     * B0..3	Port
     * B4		Clear to Send (CTS)
     * B5		Data Set Ready (DSR)
     * B6		Ring Indicator (RI)
     * B7		Receive Line Signal Detect (RLSD)
     *
     * Byte 1: Line Status
     *   NOTE: same layout as the LSR register in a 16550
     *
     * Offset	Description
     * B0	Data Ready (DR)
     * B1	Overrun Error (OE)
     * B2	Parity Error (PE)
     * B3	Framing Error (FE)
     * B4	Break Interrupt (BI)
     * B5	Transmitter Holding Register (THRE)
     * B6	Transmitter Empty (TEMT)
     * B7	Error in RCVR FIFO
     * OUT Endpoint
     *
     * This device reserves the first bytes of data on this endpoint contain the
     * length and port identifier of the message. For the FTDI USB Serial converter
     * the port identifier is always 1.
     *
     * Byte 0: Port & length
     *
     * Offset	Description
     * B0..1	Port
     * B2..7	Length of message - (not including Byte 0)
     */
    // public static final int FTDI_PORT_MASK              0x0f
    // public static final int FTDI_MSR_MASK               0xf0
    // public static final int FTDI_GET_MSR(p)             (((p)[0]) & FTDI_MSR_MASK)
    // public static final int FTDI_GET_LSR(p)             ((p)[1])
    // public static final int FTDI_LSR_MASK               (~0x60)		/* interesting bits */
    // public static final int FTDI_OUT_TAG(len, port)     (((len) << 2) | (port))

    //----------------------------------------------------------------------------------------------

    public static final byte DATA_BITS_7 = 7;
    public static final byte DATA_BITS_8 = 8;
    public static final byte STOP_BITS_1 = 0;
    public static final byte STOP_BITS_2 = 2;
    public static final byte PARITY_NONE = 0;
    public static final byte PARITY_ODD = 1;
    public static final byte PARITY_EVEN = 2;
    public static final byte PARITY_MARK = 3;
    public static final byte PARITY_SPACE = 4;
    public static final short FLOW_NONE = 0;
    public static final short FLOW_RTS_CTS = 256;
    public static final short FLOW_DTR_DSR = 512;
    public static final short FLOW_XON_XOFF = 1024;
    public static final byte PURGE_RX = 1;
    public static final byte PURGE_TX = 2;
    public static final byte CTS = 16;
    public static final byte DSR = 32;
    public static final byte RI = 64;
    public static final byte DCD = -128;
    public static final byte OE = 2;
    public static final byte PE = 4;
    public static final byte FE = 8;
    public static final byte BI = 16;
    public static final byte EVENT_RXCHAR = 1;
    public static final byte EVENT_MODEM_STATUS = 2;
    public static final byte EVENT_LINE_STATUS = 4;
    public static final byte EVENT_REMOVED = 8;
    public static final byte FLAGS_OPENED = 1;
    public static final byte FLAGS_HI_SPEED = 2;
    public static final int DEVICE_232B = 0;
    public static final int DEVICE_8U232AM = 1;
    public static final int DEVICE_UNKNOWN = 3;
    public static final int DEVICE_2232 = 4;
    public static final int DEVICE_232R = 5;
    public static final int DEVICE_245R = 5;
    public static final int DEVICE_2232H = 6;
    public static final int DEVICE_4232H = 7;
    public static final int DEVICE_232H = 8;
    public static final int DEVICE_X_SERIES = 9;
    public static final int DEVICE_4222_0 = 10;
    public static final int DEVICE_4222_1_2 = 11;
    public static final int DEVICE_4222_3 = 12;
    public static final byte BITMODE_RESET = 0;
    public static final byte BITMODE_ASYNC_BITBANG = 1;
    public static final byte BITMODE_MPSSE = 2;
    public static final byte BITMODE_SYNC_BITBANG = 4;
    public static final byte BITMODE_MCU_HOST = 8;
    public static final byte BITMODE_FAST_SERIAL = 16;
    public static final byte BITMODE_CBUS_BITBANG = 32;
    public static final byte BITMODE_SYNC_FIFO = 64;

    public static final byte PACKET_SIZE = 64;
    public static final int PACKET_SIZE_HI = 512;
    public static final byte MODEM_STATUS_SIZE = 2;
    
    public static final int CLOCK_RATE = 3000000;
    public static final int CLOCK_RATE_HI = 12000000;
    public static final int SUB_INT_0_0     = 0;
    public static final int SUB_INT_0_125   = /*49152*/ 0xC000;
    public static final int SUB_INT_0_25    = /*32768*/ 0x8000;
    public static final int SUB_INT_0_5     = /*16384*/ 0x4000;
    public static final int SUB_INT_MASK    = /*49152*/ 0xC000;
    public static final int SUB_INT_0_375   = 0;
    public static final int SUB_INT_0_625   = /*16384*/ 0x4000;
    public static final int SUB_INT_0_75    = /*32768*/ 0x8000;
    public static final int SUB_INT_0_875   = /*49152*/ 0xC000;
    }
